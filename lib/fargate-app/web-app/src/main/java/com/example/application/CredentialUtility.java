// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Random;

//import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import com.example.application.data.AppUser;

import java.security.SecureRandom;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public class CredentialUtility {
	private final static Logger logger = LogManager.getLogger(CredentialUtility.class);
	private static final String VALID_PW_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*?";
	private static final int DEFAULT_PASSWORD_LENGTH = 12;
	private static final Random RANDOM = new SecureRandom();
	private static final String region = System.getenv("AWS_REGION");
	private static Region REGION = Region.of(region);

	private static String getPublicKeyAsString(KeyPair keyPair) throws NullPointerException {
		RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
		String publicKeyAsString = null;
		try {
			publicKeyAsString = encode(pub);
		} catch (UnsupportedEncodingException uee) {
			logger.error(uee.getMessage());
		}

		catch (IOException io) {
			logger.error(io.getMessage());
		}

		if (publicKeyAsString == null) {
			throw new NullPointerException("Something went wrong creating a public key");
		} else {
			return publicKeyAsString;
		}
	}

	private static String getPrivateKeyAsString(KeyPair keyPair) {
		StringWriter privateWrite = new StringWriter();
		JcaPEMWriter privatePemWriter = new JcaPEMWriter(privateWrite);
		try {
			privatePemWriter.writeObject(keyPair.getPrivate());
			privatePemWriter.close();
		} catch (IOException io) {
			logger.error(io.getMessage());
		}
		return privateWrite.toString();
	}

	public static KeyPair generateKeyPair() {
		java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		KeyPair keyPair = null;
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			keyPair = kpg.generateKeyPair();

		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
		}
		return keyPair;
	}


	
	private static String encode(RSAPublicKey key) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte[] name = "ssh-rsa".getBytes("US-ASCII");
		write(name, buf);
		write(key.getPublicExponent().toByteArray(), buf);
		write(key.getModulus().toByteArray(), buf);
		// String s = new String(buf.toByteArray(), StandardCharsets.UTF_8);
		String s2 = "ssh-rsa " + Base64.getEncoder().encodeToString(buf.toByteArray());
		return s2;
	}

	private static void write(byte[] str, OutputStream os) throws IOException {
		for (int shift = 24; shift >= 0; shift -= 8)
			os.write((str.length >>> shift) & 0xFF);
		os.write(str);
	}

	
	/*
	public static URL getPrivateKeyPresignedUrl(String s3Bucket, String s3Path) {

		S3Presigner presigner = S3Presigner.builder().region(REGION).build();
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(Toolkit.RESOURCE_BUCKET)
				.key(s3Path)
				.build();
		GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofDays(7))
				.getObjectRequest(getObjectRequest)
				.build();
		PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);

		URL url = presignedGetObjectRequest.url();
		presigner.close();
		return url;
	}*/

	public static String[] uploadKeysToS3(KeyPair keyPair, AppUser appUser) {

		String[] keyPairObjectPaths = new String[2];

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		String timestamp = formatter.format(Toolkit.getSanDiegoTime());
		String publicKeyFileName = appUser.getUsername() + "/public-keys/" + appUser.getUsername() + "_" + timestamp
				+ ".pub";
		S3Client s3 = Toolkit.getS3Client();
		String privateKeyFileName = appUser.getUsername() + "/private-keys/" + appUser.getUsername() + "_" + timestamp
				+ ".pem";
		keyPairObjectPaths[0] = privateKeyFileName;
		keyPairObjectPaths[1] = publicKeyFileName;
		logger.info("Uploading public key " + publicKeyFileName + " to resource bucket");
		logger.info("Uploading private key " + privateKeyFileName + " to resource bucket");
		byte[] publicKeyArray = getPublicKeyAsString(keyPair).getBytes();
		PutObjectRequest objectRequest = PutObjectRequest.builder()
				.bucket(Toolkit.RESOURCE_BUCKET)
				.key(publicKeyFileName)
				.build();
		ByteBuffer byteBuffer = ByteBuffer.wrap(publicKeyArray);
		s3.putObject(objectRequest, RequestBody.fromByteBuffer(byteBuffer));

		byte[] privateKeyArray = getPrivateKeyAsString(keyPair).getBytes();
		PutObjectRequest objectRequest2 = PutObjectRequest.builder()
				.bucket(Toolkit.RESOURCE_BUCKET)
				.key(privateKeyFileName)
				.build();
		ByteBuffer privateKeyByteBuffer = ByteBuffer.wrap(privateKeyArray);
		s3.putObject(objectRequest2, RequestBody.fromByteBuffer(privateKeyByteBuffer));

		return keyPairObjectPaths;
	}

	/******/
	/*
	 * public static String generateCommonLangPassword() {
	 * String upperCaseLetters = RandomStringUtils.random(2, 65, 90, true, true);
	 * String lowerCaseLetters = RandomStringUtils.random(2, 97, 122, true, true);
	 * String numbers = RandomStringUtils.randomNumeric(2);
	 * String specialChar = RandomStringUtils.random(2, 33, 47, false, false);
	 * String totalChars = RandomStringUtils.randomAlphanumeric(2);
	 * String combinedChars = upperCaseLetters.concat(lowerCaseLetters)
	 * .concat(numbers)
	 * .concat(specialChar)
	 * .concat(totalChars);
	 * List<Character> pwdChars = combinedChars.chars()
	 * .mapToObj(c -> (char) c)
	 * .collect(Collectors.toList());
	 * Collections.shuffle(pwdChars);
	 * String password = pwdChars.stream()
	 * .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
	 * .toString();
	 * return password;
	 * }
	 */

	// main class
	public static String generatePassword() {

		StringBuilder pw = new StringBuilder();

		// generate password
		for (int i = 0; i < DEFAULT_PASSWORD_LENGTH; i++) {
			int index = RANDOM.nextInt(VALID_PW_CHARS.length());
			pw.append(VALID_PW_CHARS.charAt(index));
		}

		return pw.toString();
	}

}
