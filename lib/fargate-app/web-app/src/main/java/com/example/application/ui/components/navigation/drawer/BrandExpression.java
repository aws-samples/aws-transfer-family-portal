// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.ui.components.navigation.drawer;

import com.example.application.ui.util.UIUtils;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;

@CssImport("./styles/components/brand-expression.css")
public class BrandExpression extends Div {

	private String CLASS_NAME = "brand-expression";

	private Image logo;
	private Label title;

	/**
	 * Come back to this-- allow users to customize their icon.
	 * @param text
	 */
	public BrandExpression(String text) {
		setClassName(CLASS_NAME);
		/*
		logo = new Image(UIUtils.IMG_PATH + "logos/18.png", "");
		logo.setAlt(text + " logo");
		logo.setClassName(CLASS_NAME + "__logo");
		*/
		title = UIUtils.createH3Label(text);
		title.addClassName(CLASS_NAME + "__title");

		add(/*logo,*/ title);
	}

}
