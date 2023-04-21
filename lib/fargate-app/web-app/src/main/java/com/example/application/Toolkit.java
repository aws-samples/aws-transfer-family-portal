// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import org.mindrot.jbcrypt.BCrypt;

public class Toolkit {
	public static String SFTP_SERVER_ID;
	public static String S3_BUCKET;
	public static String SFTP_ENDPOINT;
	public static String CUSTOM_HOSTNAME;
	public static String RESOURCE_BUCKET;
	public static String RDS_ENDPOINT;
	//public static String RDS_USERNAME;
	//public static String RDS_PASSWORD;
	public static String CLOUDWATCH_VPC_ENDPOINT;
	private static int workload = 12;
	public static String TRANSFER_CLOUDWATCH_LOGGROUP_NAME;
	private static String SSM_VPC_ENDPOINT;
	public static final String ENROLLMENT_EMAIL_SUBJECT = "Welcome to the Transfer Family Portal";
	public static String SENDER;
	private final static Logger logger = LogManager.getLogger(Toolkit.class);
	private static Map<String, String> parameterMap = null;
	private static final String region = System.getenv("AWS_REGION");
	private static final Region REGION = Region.of(region);

	/**
	 * Valid values are FTP and S3. Selection determines which implementation of
	 * TransferEngine to use:
	 * either TransferEngineFtp or TransferEngineS3.
	 */
	 public static final String CLIENT_MODE = "S3";
	//public static final String CLIENT_MODE = "FTP";
	private static final String ssm_vpc_endpoint = "https://ssm." + region + ".amazonaws.com";

	static {
		SSM_VPC_ENDPOINT = System.getenv("SSM_VPC_ENDPOINT") == null
				? ssm_vpc_endpoint
				: System.getenv("SSM_VPC_ENDPOINT");
	}

	private static final SsmClient ssmClient = SsmClient.builder()
			.region(REGION)
			.endpointOverride(URI.create(SSM_VPC_ENDPOINT))
			.build();

	private static boolean initialized = false;
	public static final String mysqldriver = "com.mysql.cj.jdbc.Driver";

	public static void init() {
		if (!initialized) {
			logger.info("Initializing");
			loadParameters();
			initialized = true;
		}
	}
	

	
	public static boolean objectExists(String key) {
		ListObjectsRequest lor = ListObjectsRequest.builder()
				.bucket(Toolkit.S3_BUCKET)
				.prefix(key)
				.build();
		S3Client s3 = Toolkit.getS3Client();
		ListObjectsResponse listObjectsResponse = s3.listObjects(lor);

		return listObjectsResponse.contents().size() != 0; 
	}

	
	/**
	 * Returns current pacific time regardless of the server time.
	 * 
	 * @return
	 */
	public static LocalDateTime getSanDiegoTime() {
		ZoneId losAngeles = ZoneId.of("America/Los_Angeles");
		Instant now = Instant.now();
		return ZonedDateTime.ofInstant(now, losAngeles).toLocalDateTime();
	}

	public static long getSanDiegoTimeAsLong() {
		ZoneId losAngeles = ZoneId.of("America/Los_Angeles");
		LocalDateTime localDateTime = LocalDateTime.now();

		ZonedDateTime zdt = ZonedDateTime.of(localDateTime, losAngeles);
		long date = zdt.toInstant().toEpochMilli();
		return date;
	}

	public static S3Client getS3Client() {
		S3Client s3Client = S3Client.builder().region(REGION)
				.build();
		return s3Client;
	}

	public static String hashPassword(String password) {
		String salt = BCrypt.gensalt(workload);
		return BCrypt.hashpw(password, salt);
	}
	
	public static boolean checkPassword(String loginPassword, String hashedPassword) {
		boolean valid = false;
		if (hashedPassword==null || !hashedPassword.startsWith("$2a$")) {
			return false;
		}
		valid = BCrypt.checkpw(loginPassword, hashedPassword);
		return valid;
	}

	private static void loadParameters() {
		logger.info("Loading parameters");
		String ssmEndpoint = SSM_VPC_ENDPOINT;

		if (ssmEndpoint != null) {
			logger.info("SSM Endpoint = " + ssmEndpoint);
		} else {
			logger.info("SSM endpoint is null.  This is bad");
		}
		if (parameterMap == null) {
			parameterMap = new HashMap<>();

			String nextToken = null;
			boolean stop = false;
			/*
			 * Slightly irritating... this API call returns 10 items at a time so need to
			 * paginate
			 */

			while (!stop) {
				GetParametersByPathRequest parameterRequest = GetParametersByPathRequest.builder()
						.nextToken(nextToken)
						.path("/Applications/FileTransferAdminPortal/").build();

				GetParametersByPathResponse parameterResponse = ssmClient.getParametersByPath(parameterRequest);
				nextToken = parameterResponse.nextToken();
				List<Parameter> parameters = parameterResponse.parameters();
				parameters.forEach(p -> parameterMap.put(p.name(), p.value()));
				
				parameters.forEach(p -> System.out.println(p.name() + ", " + p.value()));
				
				
				stop = (nextToken == null);
			}

			SFTP_SERVER_ID = parameterMap.get("/Applications/FileTransferAdminPortal/SFTP-Server-Id");
			S3_BUCKET = parameterMap.get("/Applications/FileTransferAdminPortal/S3-Storage-Bucket-Name");
			SFTP_ENDPOINT = parameterMap.get("/Applications/FileTransferAdminPortal/SFTP-Endpoint");
			CUSTOM_HOSTNAME = parameterMap.get("/Applications/FileTransferAdminPortal/Custom-Hostname");
			RESOURCE_BUCKET = parameterMap.get("/Applications/FileTransferAdminPortal/S3-Keypair-Bucket-Name");
			RDS_ENDPOINT = parameterMap.get("/Applications/FileTransferAdminPortal/rds_endpoint");
			//RDS_USERNAME = parameterMap.get("/Applications/FileTransferAdminPortal/rds-lambda-username");
			//RDS_PASSWORD = parameterMap.get("/Applications/FileTransferAdminPortal/rds-lambda-password");
			CLOUDWATCH_VPC_ENDPOINT = parameterMap.get("/Applications/FileTransferAdminPortal/Cloudwatch-VPC-Endpoint");
			TRANSFER_CLOUDWATCH_LOGGROUP_NAME = parameterMap.get("/Applications/FileTransferAdminPortal/TransferLogGroupName");
			SENDER = parameterMap.get("/Applications/FileTransferAdminPortal/sender-email-address");

			
			
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
		 init();
		 GenerateAuthenticationTokenRequest request = GenerateAuthenticationTokenRequest
				 .builder()
				 .hostname(RDS_ENDPOINT)
				 .port(3306)
				 .username("svc_fap")
				 .credentialsProvider(DefaultCredentialsProvider.create())
				 .region(REGION)
				 .build();
		 	String token = RdsUtilities.builder().build().generateAuthenticationToken(request);
		 	return token;
	
	    }
    
}
