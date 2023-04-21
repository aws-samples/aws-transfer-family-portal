// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.transfer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.application.Toolkit;
import com.example.application.UploadBuffer;
import com.example.application.dao.CloudWatchService;
import com.example.application.data.TreeItem;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.UploadRequest;

public class TransferEngineS3 implements TransferEngine {
	private final static Logger logger = LogManager.getLogger(TransferEngineS3.class);
	private CloudWatchService cloudWatchService;
	private static final String region = System.getenv("AWS_REGION");
	private static Region REGION = Region.of(region);

	public TransferEngineS3(CloudWatchService cloudWatchService) {
		this.cloudWatchService = cloudWatchService;
	}

	@Override
	public InputStream getInputStream(TreeItem treeItem, long size) {
		cloudWatchDownloadStart(treeItem.getS3ObjectKey());
		S3Client s3Client = Toolkit.getS3Client();
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(Toolkit.S3_BUCKET)
				.key(treeItem.getS3ObjectKey())
				.build();
		ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);
		cloudWatchDownloadFinish(treeItem.getS3ObjectKey(), size);
		return response;
	}

	@Override
	public void delete(TreeItem treeItem) {
		S3Client s3 = Toolkit.getS3Client();
		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
				.bucket(Toolkit.S3_BUCKET)
				.key(treeItem.getS3ObjectKey())
				.build();
		s3.deleteObject(deleteObjectRequest);
		String deleteEvent = cloudWatchService.getLogstream() + " DELETE Path=" + treeItem.getS3ObjectKey()
				+ " Interface=Web";
		cloudWatchService.publishLogEvent(deleteEvent);
	}

	
	
	
	@Override
	public String upload(TreeItem selectedFolder, UploadBuffer buffer) {
		System.out.println(LocalDateTime.now());
		System.out.println(buffer.getFileName());
		String fileName = buffer.getFileName();
		String s3ParentFolder = selectedFolder.getS3ObjectKey();
		boolean needsBackslash = s3ParentFolder.charAt(s3ParentFolder.length() - 1) != '/';
		String key = s3ParentFolder + (needsBackslash ? "/" : "") + fileName;
		
		/*Validation-- does the key exist?*/
		/*
		if (Toolkit.objectExists(key)) {
			Dialog dlg = new Dialog();
			Button btnConfirm = new Button("Overwrite");
			Button btnCancel = new Button("Cancel");
			VerticalLayout layout = new VerticalLayout();
			HorizontalLayout buttonContainer = new HorizontalLayout();
			dlg.add(layout);
			buttonContainer.add(btnConfirm, btnCancel);
			btnConfirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			btnCancel.addClickListener(e->dlg.close());
			btnConfirm.addClickListener(e-> {
				
				dlg.close();
			});
			
			Label lbl = new Label("A file with this name already exists.  Do you want to overwrite it?");
			layout.add(lbl,buttonContainer);
			dlg.open();
		}*/
		
		logger.info("Starting upload of " + fileName + " with key = " + key);
		this.cloudWatchUploadStart(s3ParentFolder, fileName);
		S3TransferManager transferManager = S3TransferManager.builder()
				.s3ClientConfiguration(cfg -> cfg
						.region(REGION)
						.targetThroughputInGbps(20.0)
						.minimumPartSizeInBytes(10 * 1024L * 1024L))
				.build();
		UploadRequest uploadRequest = UploadRequest.builder()
				.requestBody(AsyncRequestBody.fromFile(Paths.get(buffer.getTmpFile().getAbsolutePath())))
				.putObjectRequest(p -> p.bucket(Toolkit.S3_BUCKET).key(key))
				.build();

		software.amazon.awssdk.transfer.s3.Upload transferUpload = transferManager.upload(uploadRequest);
		CompletableFuture<CompletedUpload> cu = transferUpload.completionFuture();
		cu.join();

		cloudWatchUploadFinish(s3ParentFolder, fileName, buffer.getTmpFile().length());

		boolean successfullyDeleted = buffer.getTmpFile().delete();
		if (successfullyDeleted) {
			logger.info("Deleted temp file " + buffer.getFileName());
		} else {
			logger.error("Error deleting temp file " + buffer.getFileName());
		}

		buffer.getTmpFile().deleteOnExit();
		return key;
	}

	
	
	@Override
	/**
	 * Zipping implementation thanks to this thread:
	 * https://vaadin.com/forum/thread/17562004/download-multiple-files
	 * 
	 * @return
	 * @throws IOException
	 */
	public byte[] zipFiles(TreeItem[] items, long[] sizes) throws IOException {
		S3Client s3 = Toolkit.getS3Client();
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final ZipOutputStream zos = new ZipOutputStream(baos)) {

			for (int i = 0; i < items.length; i++) {
				TreeItem s3Object = items[i];
				long size = sizes[i];
				String cloudwatchStartEvent = cloudWatchService.getLogstream() + " OPEN ";
				cloudwatchStartEvent += "Path=/" + Toolkit.S3_BUCKET + "/" + s3Object.getS3ObjectKey();
				cloudwatchStartEvent += " Mode=Read Interface=Web";
				cloudWatchService.publishLogEvent(cloudwatchStartEvent);

				String filename = s3Object.getS3ObjectKey();
				GetObjectRequest getObjectRequest = GetObjectRequest.builder()
						.bucket(Toolkit.S3_BUCKET)
						.key(s3Object.getS3ObjectKey())
						.build();
				ResponseInputStream<GetObjectResponse> response = s3.getObject(getObjectRequest);
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				int nRead;
				byte[] data = new byte[4];
				while ((nRead = response.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}
				buffer.flush();
				byte[] content = buffer.toByteArray();
				ZipEntry entry = new ZipEntry(filename);
				entry.setSize(content.length);
				zos.putNextEntry(entry);
				zos.write(content);
				zos.closeEntry();
				String cloudwatchCloseEvent = cloudWatchService.getLogstream() + " CLOSE ";
				cloudwatchCloseEvent += "Path=/" + Toolkit.S3_BUCKET + "/" + s3Object.getS3ObjectKey();
				cloudwatchCloseEvent += " BytesOut=" + size + " Interface=Web";
				cloudWatchService.publishLogEvent(cloudwatchCloseEvent);

			}
			zos.finish();
			zos.flush();
			return baos.toByteArray();
		}
	}

	@Override
	public void rename(TreeItem s3Object, String newFileName) {
		String currentFileName = s3Object.getFilename();

		/* A little naive, but a good start.... */
		String destinationKey = s3Object.getS3ObjectKey().replace(currentFileName, newFileName);
		logger.info("Renaming " + s3Object.getS3ObjectKey() + " TO " + destinationKey);

		S3Client s3 = Toolkit.getS3Client();
		CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
				.destinationBucket(Toolkit.S3_BUCKET)
				.destinationKey(destinationKey)
				.sourceBucket(Toolkit.S3_BUCKET)
				.sourceKey(s3Object.getS3ObjectKey()).build();
		CopyObjectResponse response = s3.copyObject(copyObjectRequest);

		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
				.bucket(Toolkit.S3_BUCKET)
				.key(s3Object.getS3ObjectKey())
				.build();
		s3.deleteObject(deleteObjectRequest);
		String event = cloudWatchService.getLogstream() + " RENAME Path=" + s3Object.getS3ObjectKey() + " NewPath="
				+ destinationKey + " Interface=Web";
		cloudWatchService.publishLogEvent(event);
	}

	private void cloudWatchDownloadStart(String path) {
		String cloudwatchStartEvent = cloudWatchService.getLogstream() + " OPEN ";
		cloudwatchStartEvent += "Path=/" + Toolkit.S3_BUCKET + "/" + path;
		cloudwatchStartEvent += " Mode=Read Interface=Web";
		cloudWatchService.publishLogEvent(cloudwatchStartEvent);
	}

	private void cloudWatchDownloadFinish(String path, long size) {
		String cloudwatchCloseEvent = cloudWatchService.getLogstream() + " CLOSE ";
		cloudwatchCloseEvent += "Path=/" + Toolkit.S3_BUCKET + "/" + path;
		cloudwatchCloseEvent += " BytesOut=" + size + " Interface=Web";
		cloudWatchService.publishLogEvent(cloudwatchCloseEvent);
	}

	private void cloudWatchUploadStart(String s3ParentFolder, String fileName) {
		String cloudwatchStartUploadEvent = cloudWatchService.getLogstream() + " OPEN ";
		cloudwatchStartUploadEvent += "Path=/" + Toolkit.S3_BUCKET + "/" + s3ParentFolder + "/" + fileName;
		cloudwatchStartUploadEvent += " Mode=CREATE|TRUNCATE|WRITE Interface=Web";
		cloudWatchService.publishLogEvent(cloudwatchStartUploadEvent);

	}

	private void cloudWatchUploadFinish(String s3ParentFolder, String fileName, long contentLength) {
		String cloudwatchCloseEvent = cloudWatchService.getLogstream() + " CLOSE ";
		cloudwatchCloseEvent += "Path=/" + Toolkit.S3_BUCKET + "/" + s3ParentFolder + "/" + fileName;
		cloudwatchCloseEvent += " BytesIn=" + contentLength + " Interface=Web";
		cloudWatchService.publishLogEvent(cloudwatchCloseEvent);
	}

}
