// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

public enum LineHeight {

	XS("var(--lumo-line-height-xs)"),
	S("var(--lumo-line-height-s)"),
	M("var(--lumo-line-height-m)");

	private String value;

	LineHeight(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
