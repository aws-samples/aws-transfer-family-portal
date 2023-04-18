// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

public enum Position {

	ABSOLUTE("absolute"), FIXED("fixed"), RELATIVE("relative");

	private String value;

	Position(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
