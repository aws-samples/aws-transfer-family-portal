package com.example.application.views;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.application.Toolkit;
import com.example.application.dao.CloudWatchService;
import com.example.application.security.AuthenticationFailureListener;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route("login")
@PageTitle("Login")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {
	private final static Logger logger = LogManager.getLogger(LoginView.class);
	private final LoginForm login = new LoginForm();

	public LoginView(@Autowired CloudWatchService bean, @Autowired AuthenticationFailureListener failureListener) {

		addClassName("login");
		setSizeFull();
		setAlignItems(Alignment.CENTER);
		setJustifyContentMode(JustifyContentMode.CENTER);
		//getElement().getStyle().set("background-image", "url('images/19_02_009 (2).jpg')");
		//getElement().getStyle().set("background-size", "cover");
		login.setAction("login");
		String ipAddress = VaadinSession.getCurrent().getBrowser().getAddress();
		failureListener.setIp(ipAddress);
		H1 title = new H1("File Transfer Portal");
		//title.getElement().getStyle().set("color", "white");
		add(title, login);
		/* Enable this and implement below to handle forgot passwords */
		login.setForgotPasswordButtonVisible(false);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
		if (beforeEnterEvent.getLocation()
				.getQueryParameters()
				.getParameters()
				.containsKey("error")) {
			login.setError(true);
		}
	}
}