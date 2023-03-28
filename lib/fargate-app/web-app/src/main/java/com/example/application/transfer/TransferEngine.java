package com.example.application.transfer;

import java.io.IOException;
import java.io.InputStream;

import com.example.application.UploadBuffer;
import com.example.application.data.TreeItem;

public interface TransferEngine {
	InputStream getInputStream(TreeItem treeItem, long size);

	void delete(TreeItem treeItem);

	public String upload(TreeItem selectedFolder, UploadBuffer buffer);

	public byte[] zipFiles(TreeItem[] items, long[] sizes) throws IOException;

	public void rename(TreeItem s3Object, String newFileName);

}
