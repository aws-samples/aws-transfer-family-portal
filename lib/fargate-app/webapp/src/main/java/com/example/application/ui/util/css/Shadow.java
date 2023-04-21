// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.ui.util.css;

public enum Shadow {

	XS("var(--lumo-box-shadow-xs)"), S("var(--lumo-box-shadow-s)"), M(
			"var(--lumo-box-shadow-m)"), L("var(--lumo-box-shadow-l)"), XL(
			"var(--lumo-box-shadow-xl)");

	private String value;

	Shadow(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
