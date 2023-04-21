// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.example.application.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.application.Toolkit;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class Key {
	private final static Logger logger = LogManager.getLogger(Key.class);
	private long id;
	private final long userId;
	private String s3PrivateKeyPath;
	private String s3PublicKeyPath;
	private final LocalDateTime created;

	public Key(long userId, String s3PrivateKeyPath, String s3PublicKeyPath) {
		this.userId = userId;
		this.s3PrivateKeyPath = s3PrivateKeyPath;
		this.s3PublicKeyPath = s3PublicKeyPath;
		created = Toolkit.getSanDiegoTime();
	}

	public Key(long id, long userId, String s3PrivateKeyPath, String s3PublicKeyPath, LocalDateTime created) {
		super();
		this.id = id;
		this.userId = userId;
		this.s3PrivateKeyPath = s3PrivateKeyPath;
		this.s3PublicKeyPath = s3PublicKeyPath;
		this.created = created;
	}
	
	public File getPrivateKeyFile() {
		File file = null;
		String s3Path = getS3PrivateKeyPath();
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(Toolkit.RESOURCE_BUCKET)
				.key(s3Path)
				.build();
		
		S3Client s3Client = Toolkit.getS3Client();
		ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);
		
		int lastSlashIndex = s3Path.lastIndexOf("/");
		String tempFileName = s3Path.substring(lastSlashIndex+1);
		tempFileName = tempFileName.replace(".pem", "");
	     
		
				try {
					file = File.createTempFile(tempFileName, ".pem");
				     
				//file = new File(tempFileName);
				Files.copy(response, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch(IOException io) {
					logger.error(io.getMessage());
				}
				return file;
	}

	public long getUserId() {
		return userId;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getS3PrivateKeyPath() {
		return s3PrivateKeyPath;
	}

	public void setS3PrivateKeyPath(String s3PrivateKeyPath) {
		this.s3PrivateKeyPath = s3PrivateKeyPath;
	}

	public String getS3PublicKeyPath() {
		return s3PublicKeyPath;
	}

	public void setS3PublicKeyPath(String s3PublicKeyPath) {
		this.s3PublicKeyPath = s3PublicKeyPath;
	}

}
