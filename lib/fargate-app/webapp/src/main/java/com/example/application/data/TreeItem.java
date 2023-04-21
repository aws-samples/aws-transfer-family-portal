// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.data;


import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.application.Toolkit;

import software.amazon.awssdk.services.s3.model.S3Object;

public class TreeItem {
	private final static Logger logger = LogManager.getLogger(TreeItem.class);

	/**
	 * Label is what is displayed in the UI.
	 */

	private final String label;
	private final Map<String, TreeItem> children = new HashMap<>();
	/**
	 * Indicates whether the object is a folder or a file.
	 */
	private final boolean isFolder;
	/**
	 * Depth is only used for troubleshooting. Can be removed.
	 */
	private final String s3ObjectKey;
	private int depth;

	private DirectoryMapping directoryMapping;

	private TreeItem(DirectoryMapping directoryMapping, String label, boolean isFolder, int depth, String s3ObjectKey) {
		this.directoryMapping = directoryMapping;
		this.label = label;
		this.isFolder = isFolder;
		this.depth = depth;
		this.s3ObjectKey = s3ObjectKey;
		logger.info("Constructor 1, s3ObjectKey=" + s3ObjectKey);
	}

	public TreeItem(DirectoryMapping directoryMapping, String label, boolean isFolder, String s3ObjectKey) {
		logger.info("Constructing tree item for directoryMapping " + directoryMapping.getEntry() + ", label=" + label + ", folder?=" + (isFolder));
		this.depth = 0;
		this.directoryMapping = directoryMapping;
		this.label = label;
		this.isFolder = isFolder;
		this.s3ObjectKey = s3ObjectKey;
		logger.info("Constructor 2, s3ObjectKey=" + s3ObjectKey);

	}

	public void removeChild2(String s3ObjectKeyForRemoval) {
		String rootObjectKey = this.s3ObjectKey;
		
	}
	
	public void removeChild(String s3ObjectKeyForRemoval) {
		String rootObjectKey = this.s3ObjectKey; //top level.
		//String remaining = s3ObjectKey.replaceFirst(s3ObjectKeyForRemoval + "/", "");
		//String remaining = rootObjectKey.replaceFirst(s3ObjectKeyForRemoval + "/", "");
		String remaining = s3ObjectKeyForRemoval.replaceFirst(rootObjectKey, "");

		logger.info("Root object key=" + rootObjectKey);
		logger.info("S3Object for removal key=" + s3ObjectKeyForRemoval);
		logger.info("Remaining=" + remaining);
		
		LinkedList<String> pathArray = new LinkedList<String>(Arrays.asList(remaining.split("/")));
		pathArray.push(rootObjectKey);

		TreeItem parent = this;
		for (int i = 1; i < pathArray.size() - 1; i++) {
			logger.info("path array " + i + " is " + pathArray.get(i));
			parent = parent.getChild(pathArray.get(i));
		}
		
		
		if (!parent.getChildren().containsKey(s3ObjectKeyForRemoval)) {
			logger.info(s3ObjectKeyForRemoval + " not found");
			logger.info("Children:");
			parent.getChildren().values().stream().forEach(x-> {
				logger.info(x.getS3ObjectKey());
			});
		}
		//parent.getChildren().remove(s3ObjectKey);
		parent.getChildren().remove(s3ObjectKeyForRemoval);
	}

	public List<TreeItem> getSubTreeItems(boolean folders) {

		List<TreeItem> items = children.values().stream().filter(child -> child.isFolder() == folders)
				.collect(Collectors.toList());
		return items;
	}

	@Override
	public String toString() {
		String repeated = StringUtils.repeat("\t", depth);
		String s = "\n" + repeated + "Root: " + label + ", depth=" + depth + " children: " + children.size()
				+ (isFolder ? " folder" : " file") + " key: " + this.s3ObjectKey;
		for (TreeItem child : children.values()) {
			s += "\n" + repeated + "child: " + child.getLabel();
		}
		for (TreeItem child : children.values()) {
			s += child.toString();
		}
		return s;
	}

	public void createDirectoryStructure(S3Object s3Object, String target) {
		String relativeRoot = target.replaceFirst("/" + Toolkit.S3_BUCKET + "/", "");

		/* Extract target from s3Object key */
		String relativePath = s3Object.key().replace(relativeRoot + "/", "");

		/* Remove final slash to avoid an extraneous empty space when split */
		if (relativePath.length() > 0 && relativePath.charAt(relativePath.length() - 1) == '/') {
			relativePath = relativePath.substring(0, relativePath.length() - 1);
		}

		String[] draftArray = relativePath.split("/");
		LinkedList<String> draftList = new LinkedList<>();
		if (draftArray.length == 1 && draftArray[0].trim().length() == 0) {

		} else {
			draftList = new LinkedList<String>(Arrays.asList(draftArray));
		}
		draftList.addFirst(relativeRoot);
		boolean isFolder = s3Object.key().endsWith("/");
		
		logger.info("Draft list " + String.join("/", draftList));
		
		
		draftList.pop();
		createDirectoryStructureFromPath(draftList, !isFolder);
	}

	private void createDirectoryStructureFromPath(LinkedList<String> folderPath, boolean thisIsAFile) {
		if (folderPath.size() == 0) {
			return;
		}
		
		logger.info("Creating directory structure for " + String.join("/", folderPath));
		
		String label = folderPath.pop();
		
		logger.info(" label=" + label);
		
		TreeItem child = null;

		if (hasChild(label)) {
			child = getChild(label);
		} else {
			//String childS3Path = this.s3ObjectKey + "/" + label;
			String childS3Path = this.s3ObjectKey + (!s3ObjectKey.endsWith("/") ? "/" : "") + label;
			logger.info("Child S3 Path=" + childS3Path);
			if (folderPath.size() == 0 && thisIsAFile) {
				child = new TreeItem(this.directoryMapping,label, false, getDepth() + 1,childS3Path);
			} else {
				child = new TreeItem(this.directoryMapping,  label, true, getDepth() + 1,childS3Path);
			}
			children.put(childS3Path, child);
		}
		child.createDirectoryStructureFromPath(folderPath, thisIsAFile);
	}

	public TreeItem getChild(String label) {
		return children.values().stream().filter(t -> t.getLabel().equals(label)).findFirst().orElse(null);
	}

	boolean hasChild(String label) {
		return children.values().stream().filter(t -> t.getLabel().equalsIgnoreCase(label)).count() > 0;
	}

	public int getDepth() {
		return depth;
	}

	public String getLabel() {
		return label;
	}

	public Map<String, TreeItem> getChildren() {
		return children;
	}

	public boolean isFolder() {
		return isFolder;
	}

	public String getS3ObjectKey() {
		return s3ObjectKey;
	}

	public DirectoryMapping getDirectoryMapping() {
		return this.directoryMapping;
	}

	public String getGridLabel() {
		if (getDepth() == 0) {
			DirectoryMapping dm = getDirectoryMapping();
			String label = getLabel();
			String result = label.replace(label, dm.getEntry());
			return result;
		} else {
			return getLabel();
		}
	}

	/**
	 * Retrieves the filename from a path.
	 * E.g.: /personal/documents/myfile.txt returns myfile.txt
	 * 
	 * @param obj
	 * @return
	 */
	public String getFilename() {
		int slashIndex = getS3ObjectKey().lastIndexOf("/");
		String filename = null;
		if (slashIndex == -1) {
			filename = getS3ObjectKey();
		} else {
			filename = getS3ObjectKey().substring(slashIndex + 1);
		}
		return filename;
	}

	public String getFtpPath() {
		// DirectoryMapping dm 
		// user.getDirectoryMappings().get(selectedFolder.getDirectoryMappingId());
		String cleanTarget = directoryMapping.getRelativePath();
		String cleanEntry = directoryMapping.getEntry().replaceFirst("/", "");
		String cleanPath = ("/" + this.s3ObjectKey).replaceFirst(cleanTarget, cleanEntry);

		logger.info("clean target =" + cleanTarget + ", cleanEntry=" + cleanEntry + ", cleanPath=" + cleanPath);
		return cleanPath;
	}

	/*
	 * public String getTarget() {
	 * return target;
	 * }
	 */
}

