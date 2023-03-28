package com.example.application.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.application.Toolkit;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class DirectoryMapping {
	private final static Logger logger = LogManager.getLogger(DirectoryMapping.class);

	/**
	 * id is the primary key of the corresponding table in the database.
	 */
	private long directoryMappingId;
	private String entry;
	private String target;
	private final long userId;
	private boolean write;
	
	public DirectoryMapping(long directoryMappingId, long userId, String entry, String target, boolean write) {
		this.directoryMappingId = directoryMappingId;
		this.entry = entry;
		this.target = target;
		this.userId = userId;
		this.write = write;
	}

	public DirectoryMapping(long userId) {
		this.entry = "/";
		this.target = "/";
		this.userId = userId;
		write = false;
	}

	public String getRelativePath() {
		return target.replace("/" + Toolkit.S3_BUCKET, "");
	}

	public void setAbsolutePath(String relativePath) {
		target = "/" + Toolkit.S3_BUCKET + relativePath;
	}

	public void createS3FolderIfDoesNotExist() {
		String truncatedTarget = target.replace("/" + Toolkit.S3_BUCKET + "/", "");
		S3Client s3Client = Toolkit.getS3Client();
		ListObjectsRequest baseLineRequest = ListObjectsRequest.builder().bucket(Toolkit.S3_BUCKET)
				.prefix(truncatedTarget+"/").build();
		boolean exists = s3Client.listObjects(baseLineRequest).contents().size() > 0;
		logger.info("Directory " + truncatedTarget + " of bucket " + Toolkit.S3_BUCKET
			 + (exists ? " exists" : " does not exist"));
		boolean folderExists = s3Client.listObjects(baseLineRequest).contents().size() > 0;
		if (!folderExists) {
			PutObjectRequest objectRequest = PutObjectRequest.builder()
					.bucket(Toolkit.S3_BUCKET)
					.key(truncatedTarget + "/")
					.build();
			logger.info("Building folder " + truncatedTarget);
			s3Client.putObject(objectRequest, RequestBody.empty());
		}
	}

	public long getDirectoryMappingId() {
		return directoryMappingId;
	}

	public void setDirectoryMappingId(long directoryMappingId) {
		this.directoryMappingId = directoryMappingId;
	}

	public String getEntry() {
		return entry;
	}

	public void setEntry(String entry) {
		this.entry = entry;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public long getUserId() {
		return userId;
	}

	public boolean isWrite() {
		return write;
	}

	public void setWrite(boolean write) {
		this.write = write;
	}

}
