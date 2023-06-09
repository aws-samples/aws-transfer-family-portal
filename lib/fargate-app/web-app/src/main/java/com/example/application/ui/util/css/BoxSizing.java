// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.ui.util.css;

public enum BoxSizing {

	BORDER_BOX("border-box"), CONTENT_BOX("content-box");

	private String value;

	BoxSizing(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
