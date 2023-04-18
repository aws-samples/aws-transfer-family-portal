// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.example.application.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class LoginListener implements ApplicationListener<InteractiveAuthenticationSuccessEvent> {
	private final static Logger logger = LogManager.getLogger(LoginListener.class);
	// private SessionData sessionData;

	/*
	 * public LoginListener(@Autowired ApplicationConfiguration sessionData) {
	 * logger.info("Login Listener constructor.");
	 * this.sessionData = sessionData;
	 * }
	 */
	@Override
	public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event) {
		/*
		 * This method of initializing the parameters not really in the spirit of Spring
		 * Boot. Ok for now
		 */
		// Toolkit.loadParameters();
		// UserDetails userDetails = (UserDetails)
		// event.getAuthentication().getPrincipal();
		// logger.info(userDetails.getUsername());

		// sessionData.init(userDetails.getUsername());

	}
}