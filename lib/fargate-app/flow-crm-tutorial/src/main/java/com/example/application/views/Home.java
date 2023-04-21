package com.example.application.views;

import com.example.application.views.FlexBoxLayout;

import com.example.application.ui.layout.size.Horizontal;
import com.example.application.ui.layout.size.Right;
import com.example.application.ui.layout.size.Uniform;
import com.example.application.ui.util.UIUtils;


import com.example.application.ui.util.UIUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
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
		
		Html welcome = new Html("<p>Welcome to the <b>AWS File Transfer Admin Portal</b>  This web app let's you " +
				"easily manager users for your Transfer Family instance.  You can add, edit, and update users,"
				+ "provision authentication keys for them, and configure their rights to directory mappings. " +
				" The application also includes a basic client where users can upload, download and delete files " +
				" from the Transfer Family instance, although we recommend a more robust third-party solution like " +
				" CyberDuck or FileZilla for production deployments</p>");
		Html paragraph2=new Html("<p>We will continue to update the documentation on this page, and also " +
				"in the GitHub Repo.  Feedback and pull requests are welcome.  You may also reach out to the project " +
				"owners directly. </p>");
		Anchor github = new Anchor("https://github.com/aws-samples/aws-transfer-family-portal", UIUtils.createButton("GitHub Repo", VaadinIcon.EXTERNAL_LINK));
		String gabeEmail = "<a href = \"mailto: gmerton@amazon.com\">Gabe Merton, Ph.D., Developer and Architect</a>";
		String russEmail = "<a href = \"mailto: russboye@amazon.com\">Russ Boyer, Transfer Family Expert</a>";
		
		Html gabeEmailLink = new Html("<p>" + gabeEmail + "</p>");
		Html russEmailLink = new Html("<p>" + russEmail + "</p>");
		
		Html intro = new Html("<p>A responsive application template with some dummy data. Loosely based on " +
				"the <b>responsive layout grid</b> guidelines set by " +
				"<a href=\"https://material.io/design/layout/responsive-layout-grid.html\">Material Design</a>. " +
				"Utilises the <a href=\"https://vaadin.com/themes/lumo\">Lumo</a> theme.</p>");

		Html productivity = new Html("<p>The starter gives you a productivity boost and a head start. " +
				"You get an app shell with a typical hierarchical left-hand menu. The shell, the views and the " +
				"components are all responsive and touch friendly, which makes them great for desktop and mobile" +
				"use. The views are built with Java, which enhances Java developers' productivity by allowing them to" +
				"do all in one language.</p>");

		Html features = new Html("<p>The app comes with multiple list views to edit master-detail data. " +
				"Views can be divided horizontally or vertically to open up the details, and the details can " +
				"also be split into multiple tabs for extra space. The details can also be opened fullscreen " +
				"to maximize the use of space. Additionally there is an opt-in option for opening multiple " +
				"application views in tabs within the app, for quick comparison or navigation between data. " +
				"You enable this feature by setting <code>MainLayout.navigationTabs</code> to true.</p>");

		Anchor documentation = new Anchor("https://vaadin.com/docs/business-app/overview.html", UIUtils.createButton("Read the documentation", VaadinIcon.EXTERNAL_LINK));
		Anchor starter = new Anchor("https://vaadin.com/start/lts/business-app", UIUtils.createButton("Start a new project with Business App", VaadinIcon.EXTERNAL_LINK));

		FlexBoxLayout links = new FlexBoxLayout(documentation, starter);
		links.setFlexWrap(FlexWrap.WRAP);
		links.setSpacing(Right.S);

		FlexBoxLayout content = new FlexBoxLayout(welcome, paragraph2, github, gabeEmailLink, russEmailLink);
		//FlexBoxLayout content = new FlexBoxLayout(intro, productivity, features, links);
		content.setFlexDirection(FlexDirection.COLUMN);
		content.setMargin(Horizontal.AUTO);
		content.setMaxWidth("840px");
		content.setPadding(Uniform.RESPONSIVE_L);
		return content;
	}

}
