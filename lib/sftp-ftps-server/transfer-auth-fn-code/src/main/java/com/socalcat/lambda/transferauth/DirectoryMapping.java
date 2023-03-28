package com.socalcat.lambda.transferauth;

public class DirectoryMapping {

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
