// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

public enum TextAlign {

	CENTER("center"),
	JUSTIFY("justify"),
	LEFT("left"),
	RIGHT("right");

	private String value;

	TextAlign(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
