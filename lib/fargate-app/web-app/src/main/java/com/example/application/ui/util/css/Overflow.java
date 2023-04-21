// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.ui.util.css;

public enum Overflow {

	AUTO("auto"), HIDDEN("hidden"), SCROLL("scroll"), VISIBLE("visible");

	private String value;

	Overflow(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
