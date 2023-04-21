// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.ui.util.css;

public enum TextOverflow {

	CLIP("clip"),
	ELLIPSIS("ellipsis");

	private String value;

	TextOverflow(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
