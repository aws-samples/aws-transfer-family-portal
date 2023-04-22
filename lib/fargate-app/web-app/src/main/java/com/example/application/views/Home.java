// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.views;

import com.example.application.ui.layout.size.Horizontal;
import com.example.application.ui.layout.size.Uniform;
import com.example.application.ui.util.UIUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;




@PageTitle("Welcome")
@Route(value = "", layout = MainLayout.class)
@PermitAll
public class Home extends ViewFrame {

	public Home() {
		setId("home");
		setViewContent(createContent());
	}

	private Component createContent() {
		
		Html welcome = new Html("<p>Welcome to the <b>AWS File Transfer Admin Portal</b>!  This web app lets you " +
				"easily manage users for your Transfer Family instance.  You can add, edit, and update users, "
				+ "provision authentication keys for them, and configure their rights to directory mappings. " +
				" The application also includes a basic client where users can upload, download and delete files " +
				" from the Transfer Family instance, although we recommend a more robust third-party solution like " +
				" CyberDuck or FileZilla for large file transfers.</p>");
		Html paragraph2=new Html("<p>We will continue to update the documentation on this page, and also " +
				"in the GitHub Repo.  Feedback and pull requests are welcome.  You may also reach out to the project " +
				"owners directly. </p>");
		Anchor github = new Anchor("https://github.com/aws-samples/aws-transfer-family-portal", UIUtils.createButton("GitHub Repo", VaadinIcon.EXTERNAL_LINK));
		String gabeEmail = "<a href = \"mailto: gmerton@amazon.com\">Gabe Merton, Ph.D., Software Developer and Solutions Architect</a>";
		String russEmail = "<a href = \"mailto: russboye@amazon.com\">Russ Boyer, Transfer Family Expert</a>";
		
		Html gabeEmailLink = new Html("<p>" + gabeEmail + "</p>");
		Html russEmailLink = new Html("<p>" + russEmail + "</p>");
		
			FlexBoxLayout content = new FlexBoxLayout(welcome, paragraph2, github, gabeEmailLink, russEmailLink);
		//FlexBoxLayout content = new FlexBoxLayout(intro, productivity, features, links);
		content.setFlexDirection(FlexDirection.COLUMN);
		content.setMargin(Horizontal.AUTO);
		content.setMaxWidth("840px");
		content.setPadding(Uniform.RESPONSIVE_L);
		return content;
	}

}
