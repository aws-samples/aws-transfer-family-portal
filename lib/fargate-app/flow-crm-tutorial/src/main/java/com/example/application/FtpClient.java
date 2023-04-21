// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.example.application.Toolkit;

@VaadinSessionScope
@Component
public class FtpClient {
	private final static Logger logger = LogManager.getLogger(FtpClient.class);
	public static final int UNINITIALIZED = 0;
	public static final int INITIALIZING = 1;
	public static final int INITIALIZED = 2;

	private volatile int status = 0;
	private String password;
	private String username;

	private Session session;
	private ChannelSftp sftpChannel;
	private JSch jsch;

	public FtpClient() {
		if (Toolkit.CLIENT_MODE.equals("FTP")) {
			init();
		}

		else {
			logger.info("Not in FTP mode.  No need to initialize the FTP client.");
		}

	}

	private void init() {
		password = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		username = ((UserDetails) principal).getUsername();
		logger.info("Constructing User DAO");
		InitFtp initFtp = new InitFtp();
		initFtp.start();
	}

	public synchronized ChannelSftp getSftpChannel() {
		if (status == INITIALIZING) {
			logger.info("Entering while loop");

			while (status == INITIALIZING) {

			}
			logger.info("Exiting while loop");

		} else if (status == UNINITIALIZED || !sftpChannel.isConnected() || sftpChannel.isClosed()
				|| !session.isConnected()) {
			InitFtp initFtp = new InitFtp();
			initFtp.start();
		}
		return sftpChannel;
	}

	class InitFtp extends Thread {
		public InitFtp() {

		}

		public void run() {
			try {
				logger.info("Initializing SFTP");
				status = INITIALIZING;
				jsch = new JSch();
				// session = jsch.getSession(username, "s-5bd7bc00fc4a4d488.server.transfer.us-west-1.amazonaws.com");
				session = jsch.getSession(username, Toolkit.SFTP_ENDPOINT);
				session.setConfig("StrictHostKeyChecking", "no");
				session.setPassword(password);
				session.connect();
				logger.info("Connecting channel");
				sftpChannel = (ChannelSftp) session.openChannel("sftp");
				sftpChannel.connect();
				logger.info("SFTP is initialized");
				status = INITIALIZED;
				// notifyAll();
			} catch (JSchException e) {
				logger.info(e.getMessage());
				status = UNINITIALIZED;
				jsch = null;
				sftpChannel = null;
				session = null;
			}
		}
	}

}
