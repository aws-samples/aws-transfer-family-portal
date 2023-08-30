// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.socalcat.lambda.transferauth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.mindrot.jbcrypt.BCrypt;
import software.amazon.awssdk.services.ssm.*;
import software.amazon.awssdk.services.ssm.model.*;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.net.URI;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class LambdaFunctionHandler implements RequestHandler<Map<String, String>, Map<String, Object>> {
	private static S3Client s3;
	private static String S3_KEYPAIR_BUCKET_NAME;
	private static String S3_STORAGE_BUCKET_ARN;
	private static String S3_KEYPAIR_BUCKET_ARN;
	private List<DirectoryMapping> directoryMappings = new ArrayList<>();
	private static String TRANSFER_ROLE_ARN;
	private static String SSM_VPC_ENDPOINT;
	private static String DB_ENDPOINT;
	private String username;
	private static final String POLICY_HEADER = "\"Version\": \"2012-10-17\", \"Statement\":";
	private Map<String, Object> data_ret = new HashMap<>();
	private LambdaLogger logger;
	private static final String region = System.getenv("AWS_REGION");
	private static Region REGION = Region.of(region);
	private static final String ssm_vpc_endpoint = "https://ssm." + region + ".amazonaws.com";
	public static final String SSL_CERTIFICATE = "rds-ca-2019-root.pem";
	private static final String KEY_STORE_TYPE = "JKS";
	private static final String KEY_STORE_PROVIDER = "SUN";
	private static final String KEY_STORE_FILE_PREFIX = "sys-connect-via-ssl-test-cacerts";
	private static final String KEY_STORE_FILE_SUFFIX = ".jks";
	private static final String DEFAULT_KEY_STORE_PASSWORD = "delivery";
	
	static {
		SSM_VPC_ENDPOINT = System.getenv("SSM_VPC_ENDPOINT") == null
				? ssm_vpc_endpoint
				: System.getenv("SSM_VPC_ENDPOINT");
	}

	static {
		System.out.println("Initializing... MindrotV1");
		init();
	}
	
	@Override
	public Map<String, Object> handleRequest(Map<String, String> event, Context context) {
		data_ret =new HashMap<>();
		directoryMappings = new ArrayList<>();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		logger = context.getLogger();
		String eventGson = gson.toJson(event);
		
		/* Mask user's password, if one is received */
		JSONObject obj = new JSONObject(eventGson);
		if (obj.has("password")) {
			obj.put("password", "*******");
			eventGson = obj.toString();
		}
		
		username = event.get("username");

		/*
		 * Check that
		 * 1) This username exists
		 * 2) The user is active
		 */

		if (!activeUserExists()) {
			logger.log("No active user with username " + username);
			data_ret.put("Result", "Failed Authentication");
			data_ret.put("Reason", "Invalid User");
			data_ret.put("Username", username);
			return data_ret;
		}

		if (event.containsKey("password")) {
			String password = event.get("password");
			boolean valid = this.validUsernamePassword(username, password);
			if (!valid) {
				data_ret.put("Result", "Failed Authentication");
				data_ret.put("Reason", "Invalid Password");
				data_ret.put("Username", username);
				logger.log("Invalid Password");
				return data_ret;
			}
		}

		else {
			/*Case: SFTP and no password.  Search for a matching key pair*/
			if (event.get("protocol").equals("SFTP")) {
				List<String> publicKeyList = this.getPublicKeys();
				data_ret.put("PublicKeys", publicKeyList);
			}
			
			/*Case: FTPS and no password.  This is not acceptable*/
			if (event.get("protocol").equals("FTPS")) {
				logger.log("User " + username + " login failed: FTPS protocol, no passsword provided");
				return data_ret;
			}
			
		}

		String userMappings = getUserMappings();

		data_ret.put("Role", TRANSFER_ROLE_ARN);
		data_ret.put("HomeDirectoryType", "LOGICAL");
		data_ret.put("Policy", getPolicy());
		data_ret.put("HomeDirectoryDetails", userMappings);
		logger.log("RESPONSE: " + gson.toJson(data_ret));
		return data_ret;
	}

	/**
	 * Build S3 client and load class variables from parameter store.
	 * Construction of the S3 client is slow, so it's important for it to be static.
	 */
	private static void init() {
		SsmClient ssm = SsmClient.builder()
				.region(REGION)
				.endpointOverride(URI.create(SSM_VPC_ENDPOINT))
				.build();
		String nextToken = null;
		boolean stop = false;
		/*
		 * Slightly irritating... this API call returns 10 items at a time so need to
		 * paginate
		 */
		Map<String, String> parameterMap = new HashMap<>();

		while (!stop) {
			GetParametersByPathRequest parameterRequest = GetParametersByPathRequest.builder()
					.nextToken(nextToken)
					.path("/Applications/FileTransferAdminPortal/").build();

			GetParametersByPathResponse parameterResponse = ssm.getParametersByPath(parameterRequest);
			nextToken = parameterResponse.nextToken();
			List<Parameter> parameters = parameterResponse.parameters();
			parameters.forEach(p -> parameterMap.put(p.name(), p.value()));
			stop = (nextToken == null);
		}

		S3_KEYPAIR_BUCKET_NAME = parameterMap.get("/Applications/FileTransferAdminPortal/S3-Keypair-Bucket-Name");
		TRANSFER_ROLE_ARN = parameterMap.get("/Applications/FileTransferAdminPortal/TransferS3AccessRole");
		DB_ENDPOINT = parameterMap.get("/Applications/FileTransferAdminPortal/rds_endpoint");
		S3_STORAGE_BUCKET_ARN = parameterMap.get("/Applications/FileTransferAdminPortal/S3-Storage-Bucket-ARN");
		S3_KEYPAIR_BUCKET_ARN = parameterMap.get("/Applications/FileTransferAdminPortal/S3-Keypair-Bucket-ARN");
		s3 = S3Client.builder().region(REGION).build();
	}

	private String format(String target) {
		/* Remove leading backslash */
		String cleanTarget = target.substring(1);
		cleanTarget = "\"arn:aws:s3:::" + cleanTarget + "/*\"";
		return cleanTarget;
	}

	private String getPermissionBlock(Permission permission, List<String> directories) {
		String commaSeparatedDirectories = String.join(",", directories);
		String s = "{\"Sid\":\"" + permission.getSid() + "\",";
		s += "\"Effect\":\"" + permission.getEffect() + "\",";
		s += "\"Action\":\"" + permission.getAction() + "\",";
		s += "\"Resource\":[";
		s += commaSeparatedDirectories;
		s += "]}";
		return s;
	}

	private String getPolicy() {
		String policy = "{";
		policy += POLICY_HEADER;
		policy += "[";
		policy += getDataBucketPermissions();
		policy += ",";
		policy += getListBucketPermissions();
		policy += "]";
		policy += "}";
		
		return policy;
	}

	private String getListBucketPermissions() {
		String s = "{\"Sid\":\"ListDataBucket\",";
		s += "\"Effect\":\"Allow\",";
		s += "\"Action\":[\"s3:ListBucket\"],";
		s += "\"Resource\":[\"" + S3_STORAGE_BUCKET_ARN + "\",\"" + S3_KEYPAIR_BUCKET_ARN + "\"]";
		s += "},";
		s += "{\"Sid\":\"GetDataObjects\",";
		s += "\"Effect\":\"Allow\",";
		s += "\"Action\":[\"s3:GetObject*\"],";
		s += "\"Resource\":[\"" + S3_STORAGE_BUCKET_ARN + "/*\",\"" + S3_KEYPAIR_BUCKET_ARN + "/" + username + "/*\"]";
		s += "}";
		return s;
	}

	private String getDataBucketPermissions() {
		List<String> permissionBlocks = new LinkedList<>();
		Map<Permission, List<String>> directoryPermissions = new HashMap<>();
		directoryPermissions.put(
			Permission.ALLOW_WRITE, directoryMappings.stream().filter(
				d -> d.isWrite())
			.map(d -> format(d.getTarget()))
			.collect(Collectors.toList()));
		
		directoryPermissions.put(Permission.ALLOW_DELETE, directoryMappings.stream().filter(d -> d.isWrite())
				.map(d -> format(d.getTarget())).collect(Collectors.toList()));
		
		directoryPermissions.put(Permission.DENY_WRITE, directoryMappings.stream().filter(d -> !d.isWrite())
				.map(d -> format(d.getTarget())).collect(Collectors.toList()));
		
		
		directoryPermissions.put(Permission.DENY_DELETE, directoryMappings.stream().filter(d -> !d.isWrite())
				.map(d -> format(d.getTarget())).collect(Collectors.toList()));

		
		
		
		
		for (Permission permission : directoryPermissions.keySet()) {
			List<String> directories = directoryPermissions.get(permission);
			if (directories.size() > 0) {
				permissionBlocks.add(getPermissionBlock(permission, directories));
			}
		}

		String dataBucketPermissions = String.join(",", permissionBlocks);
		return dataBucketPermissions;
	}

	private List<String> getPublicKeys() {
		ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
				.bucket(S3_KEYPAIR_BUCKET_NAME)
				.prefix(username + "/public-keys")
				.build();
		ListObjectsResponse listObjectResponse = s3.listObjects(listObjectsRequest);
		List<String> keys = listObjectResponse.contents().stream()
				.filter(obj -> !obj.key().endsWith("/"))
				.map(obj -> obj.key()).collect(Collectors.toList());
		List<String> publicKeys = new ArrayList<>();
		for (String key : keys) {
			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(S3_KEYPAIR_BUCKET_NAME)
					.key(key)
					.build();
			ResponseInputStream<GetObjectResponse> inputStream = s3.getObject(getObjectRequest);
			String text = new BufferedReader(
					new InputStreamReader(inputStream, StandardCharsets.UTF_8))
							.lines()
							.collect(Collectors.joining());
			publicKeys.add(text);

		}
		return publicKeys;
	}

	/**
	 * Lambda is extremely fickle about formatting. The string concatenation below
	 * isn't that graceful.
	 * Improve on this.
	 * 
	 * @return
	 */
	private String getUserMappings() {
		String sql = "SELECT m.id as directoryMappingId, m.userId, m.write, m.entry, m.target ";
		sql += "FROM directoryMapping m inner join AppUser a on a.id = m.userId ";
		sql += "WHERE username = ?";

		List<String> mappingItems = new ArrayList<>();
		try (
				Connection con = this.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String entry = rs.getString("entry");
					String target = rs.getString("target");
					String mappingItem = "{\"Entry\":" + " \"" + entry + "\",";
					mappingItem += " \"Target\":" + " \"" + target + "\"}";
					mappingItems.add(mappingItem);

					DirectoryMapping dm = new DirectoryMapping(rs.getLong("directoryMappingId"), rs.getLong("userId"),
							entry, target,
							rs.getBoolean("write"));
					directoryMappings.add(dm);

				}
			}
		} catch (SQLException e) {
			logger.log("SQL ERROR: " + e.getMessage());
		}

		String mappingString = "[" + String.join(",", mappingItems) + "]";
		return mappingString;
	}

	private boolean activeUserExists() {
		String sql = "SELECT count(*) as recordCount FROM AppUser WHERE username = ? and enabled = 1;";
		try (
				Connection con = getConnection();
				PreparedStatement ps = con.prepareStatement(sql);) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getInt("recordCount") > 0;
			}

		} catch (SQLException e) {
			logger.log("SQL ERROR: " + e.getMessage());
			return false;
		}
	}

	private boolean validUsernamePassword(String username, String password) {
		LocalDateTime passwordExpiration = null;
		String sql = "SELECT username, password, passwordExpiration FROM AppUser WHERE username = ?";
		String correctPassword = null;

		try (
				Connection con = getConnection();
				PreparedStatement ps = con.prepareStatement(sql);) {
			ps.setString(1, username);
			try (
					ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					correctPassword = rs.getString("password");
					passwordExpiration = rs.getTimestamp("passwordExpiration").toLocalDateTime();
				}
			}

		} catch (SQLException e) {
			logger.log("SQL ERROR: " + e.getMessage());
		}

		boolean passwordExpired = LocalDateTime.now().isAfter(passwordExpiration);
		boolean validPassword = checkPassword(password, correctPassword);
		return validPassword && !passwordExpired;
	}

	public static boolean checkPassword(String loginPassword, String hashedPassword) {
		boolean valid = false;
		if (hashedPassword==null || !hashedPassword.startsWith("$2a$")) {
			return false;
		}
		valid = BCrypt.checkpw(loginPassword, hashedPassword);
		return valid;
	}

	
	public  Connection getConnection() throws SQLException {
		try {
    		 setSslProperties();
    		    
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (IOException e) {
			e.printStackTrace();
		} catch(ClassNotFoundException f) {
				f.printStackTrace();
		} catch(GeneralSecurityException g) {
			g.printStackTrace();
		}
    	
        String JDBC_URL = "jdbc:mysql://" + DB_ENDPOINT + ":3306/FileTransferAdminPortal";
        Connection con = null;
        try {
        	con = DriverManager.getConnection(JDBC_URL, setMySqlConnectionProperties());
        	con.setAutoCommit(true);
        } catch(SQLException e) {
        	e.printStackTrace();
        }
        return con;
    }
	 /**
	   * Sets the System's SSL properties which specify the key store file, its type and password.
	   *
	   * @throws GeneralSecurityException when creating the key in the key store fails
	   * @throws IOException when creating a temp file or reading a keystore file fails
	   */
	  private static void setSslProperties() throws GeneralSecurityException, IOException {
	    File keyStoreFile = createKeyStoreFile(createCertificate(SSL_CERTIFICATE));
	    System.setProperty("javax.net.ssl.trustStore", keyStoreFile.getPath());
	    System.setProperty("javax.net.ssl.trustStoreType", KEY_STORE_TYPE);
	    System.setProperty("javax.net.ssl.trustStorePassword", DEFAULT_KEY_STORE_PASSWORD);
	    
	  }

	  /**
	   * This method creates the Key Store File needed for the SSL verification <br/>
	   * during the IAM Database Authentication to the db instance.
	   *
	   * @param rootX509Certificate - the SSL certificate to be stored in the KeyStore
	   * @return the keystore file
	   * @throws GeneralSecurityException when creating the key in key store fails
	   * @throws IOException when creating temp file or reading a keystore file fails
	   */
	  private static File createKeyStoreFile(X509Certificate rootX509Certificate)
	      throws GeneralSecurityException, IOException {
	    File keyStoreFile = File.createTempFile(KEY_STORE_FILE_PREFIX, KEY_STORE_FILE_SUFFIX);

	    try (FileOutputStream fos = new FileOutputStream(keyStoreFile.getPath())) {
	      KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE, KEY_STORE_PROVIDER);
	      ks.load(null);
	      ks.setCertificateEntry("rootCaCertificate", rootX509Certificate);
	      ks.store(fos, DEFAULT_KEY_STORE_PASSWORD.toCharArray());
	    }

	    return keyStoreFile;
	  }
	  /**
	   * Creates the SSL certificate.
	   *
	   * @return X509Certificate certificate for SSL connection
	   * @throws GeneralSecurityException when creating the key in the key store fails
	   * @throws IOException when creating a temp file or reading a keystore file fails
	   */
	  public static X509Certificate createCertificate(String certFile) throws  GeneralSecurityException, IOException {
	    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
	    try (InputStream certInputStream = LambdaFunctionHandler.class.getResourceAsStream("/"+ certFile)) {
	    	X509Certificate cert = (X509Certificate) certFactory.generateCertificate(certInputStream);
	    	return cert;
	    }
	  }
	  /**
     * This method sets the mysql connection properties which includes the IAM Database Authentication token
     * as the password. It also specifies that SSL verification is required.
     * @return
     */
    public static Properties setMySqlConnectionProperties() {
        Properties mysqlConnectionProperties = new Properties();
        mysqlConnectionProperties.setProperty("verifyServerCertificate","true");
        mysqlConnectionProperties.setProperty("useSSL", "true");
        mysqlConnectionProperties.setProperty("user","svc_fap");
        mysqlConnectionProperties.setProperty("password",generateAuthToken());
        return mysqlConnectionProperties;
    }
    
    private static String generateAuthToken() {
		//init();
		 GenerateAuthenticationTokenRequest request = GenerateAuthenticationTokenRequest
				 .builder()
				 .hostname(DB_ENDPOINT)
				 .port(3306)
				 .username("svc_fap")
				 .credentialsProvider(DefaultCredentialsProvider.create())
				 .region(REGION)
				 .build();
		 	String token = RdsUtilities.builder().build().generateAuthenticationToken(request);
		 	return token;
	
	    }
	
}
