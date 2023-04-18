// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.example.application.ui.util.css;

public enum BorderRadius {

	S("var(--lumo-border-radius-s)"), M("var(--lumo-border-radius-m)"), L(
			"var(--lumo-border-radius-l)"),

	_50("50%");

	private String value;

	BorderRadius(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
