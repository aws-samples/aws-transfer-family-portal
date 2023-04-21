// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.example.application.data;

import java.util.HashMap;
import java.util.Map;

public enum Role {
	ROLE_ADMIN("ROLE_ADMIN", "Admin"),
	ROLE_USER_PASSWORD("ROLE_USER", "User");

	private String description;
	private String friendly;

	Role(String description, String friendly) {
		this.description = description;
		this.friendly = friendly;
	}

	private static Map<String, Role> lookupByDescription;
	static {
		lookupByDescription = new HashMap<>();
		for (Role appRole : Role.values()) {
			lookupByDescription.put(appRole.description, appRole);
		}
	}

	public static Role getAppRole(String description) {
		return lookupByDescription.get(description);
	}

	public String getDescription() {
		return description;
	}

	public String getFriendly() {
		return friendly;
	}
}
