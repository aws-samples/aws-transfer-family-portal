// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.ui.util.css;

public enum AlignSelf {

	BASLINE("baseline"), CENTER("center"), END("end"), START("start"), STRETCH(
			"stretch");

	private String value;

	AlignSelf(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
