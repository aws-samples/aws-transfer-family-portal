package com.socalcat.lambda.transferauth;

public enum Permission {

	ALLOW_WRITE("AllowWrite", "Allow", "s3:PutObject"),
	ALLOW_DELETE("AllowDelete", "Allow", "s3:DeleteObject"),
	DENY_WRITE("DenyWrite", "Deny", "s3:PutObject"),
	DENY_DELETE("DenyDelete", "Deny", "s3:DeleteObject");
	
	
	private String sid;
	private String effect;
	private String action;

	Permission(String sid, String effect, String action) {
		this.sid = sid;
		this.effect = effect;
		this.action = action;
	}

	public String getSid() {
		return sid;
	}

	public String getEffect() {
		return effect;
	}

	public String getAction() {
		return action;
	}

	;
}
