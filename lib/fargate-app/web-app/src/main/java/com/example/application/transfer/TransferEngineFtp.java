// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.transfer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.application.FtpClient;
import com.example.application.UploadBuffer;
import com.example.application.data.TreeItem;
import com.jcraft.jsch.SftpException;

public class TransferEngineFtp implements TransferEngine {
	private final static Logger logger = LogManager.getLogger(TransferEngineFtp.class);
	private FtpClient ftpClient;

	public TransferEngineFtp(FtpClient ftpClient) {
		this.ftpClient = ftpClient;
	}

	@Override
	public InputStream getInputStream(TreeItem treeItem, long size) {
		// public InputStream getInputStream(String path, long size) {

		InputStream is = null;

		try {
			logger.info("getting channel and file");
			is = ftpClient.getSftpChannel().get(treeItem.getFtpPath());
			logger.info("Input stream received");
		} catch (SftpException e) {
			logger.error(e.getMessage());
		}

		return is;
	}

	@Override
	public void delete(TreeItem treeItem) {
		try {
			ftpClient.getSftpChannel().rm(treeItem.getFtpPath());
		} catch (SftpException e) {
			logger.error(e.getMessage());
		}
	}

	@Override
	public String upload(TreeItem selectedFolder, UploadBuffer buffer) {
		// TODO Auto-generated method stub

		String destination = selectedFolder.getFtpPath() + "/" + buffer.getFileName();
		String key = selectedFolder.getS3ObjectKey() + "/" + buffer.getFileName();
		logger.info("FTP destination =" + destination + ", s3 key=" + key);
		try {
			ftpClient.getSftpChannel().put(buffer.getTmpFile().getAbsolutePath(), destination);
			// ftpClient.getSftpChannel().put(buffer.getInputStream(), destination);
		} catch (SftpException e) {
			logger.error(e.getMessage());
		}
		return key;
	}

	@Override
	public void rename(TreeItem s3Object, String newFileName) {
		String oldPath = s3Object.getFtpPath();
		String oldName = s3Object.getFilename();
		int index = oldPath.lastIndexOf(oldName);
		String pathExcludingFilename = oldPath.substring(0, index - 1);
		String newPath = pathExcludingFilename + "/" + newFileName;
		/*
		logger.info("Old path=" + s3Object.getFtpPath());
		logger.info("Old Name=" + s3Object.getFilename());
		logger.info("Index = " + index);
		logger.info("New file name=" + newFileName);
		*/
		logger.info(pathExcludingFilename + "  :  " + newPath);

		try {
			ftpClient.getSftpChannel().rename(oldPath, newPath);
		} catch (SftpException e) {
			logger.error(e.getMessage());
		}
	}

	@Override
	public byte[] zipFiles(TreeItem[] items, long[] sizes) throws IOException {
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final ZipOutputStream zos = new ZipOutputStream(baos)) {
			for (int i = 0; i < items.length; i++) {
				TreeItem s3Object = items[i];

				try {
					logger.info("getting channel and file");
					InputStream is = ftpClient.getSftpChannel().get(s3Object.getFtpPath());
					logger.info("Input stream received");
					String filename = s3Object.getS3ObjectKey();
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					int nRead;
					byte[] data = new byte[4];
					while ((nRead = is.read(data, 0, data.length)) != -1) {
						buffer.write(data, 0, nRead);
					}
					buffer.flush();
					byte[] content = buffer.toByteArray();
					ZipEntry entry = new ZipEntry(filename);
					entry.setSize(content.length);
					zos.putNextEntry(entry);
					zos.write(content);
					zos.closeEntry();
				} catch (SftpException e) {
					logger.error(e.getMessage());
				}

			}
			zos.finish();
			zos.flush();
			return baos.toByteArray();
		}
	}
}
