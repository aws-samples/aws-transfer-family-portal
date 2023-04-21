// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.ui.util.css;

public enum FlexWrap {

	NO_WRAP("nowrap"), WRAP("wrap"), WRAP_REVERSE("wrap-reverse");

	private String value;

	FlexWrap(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
