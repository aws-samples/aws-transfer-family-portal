package com.example.application.data;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Email {
	private final static Logger logger = LogManager.getLogger(Email.class);

	private long id;
	private String subject;
	private String body;
	private Set<String> recipients;
	private LocalDateTime dateSent;
	//private String attachmentPath = null;
	private File attachment = null;
	public Email(long id, String subject, String body, Set<String> recipients, LocalDateTime dateSent) {
		super();
		this.id = id;
		this.subject = subject;
		this.body = body;
		this.recipients = recipients;
		this.dateSent = dateSent;
	}
	
	public void setAttachment(File attachment) {
		this.attachment = attachment;
	}
	
	public File getAttachment() {
		return attachment;
	}
	
	/**
	 * Constructor for multiple recipients
	 * 
	 * @param subject
	 * @param body
	 * @param recipients
	 */
	public Email(String subject, String body, Set<String> recipients) {
		super();
		this.subject = subject;
		this.body = body;
		this.recipients = recipients;
	}

	/**
	 * Constructor for a single recipient.
	 * 
	 * @param subject
	 * @param body
	 * @param recipient
	 */
	public Email(String subject, String body, String recipient) {
		super();
		this.subject = subject;
		this.body = body;
		recipients = new HashSet<>();
		recipients.add(recipient);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Set<String> getRecipients() {
		return recipients;
	}

	public void setRecipients(Set<String> recipients) {
		this.recipients = recipients;
	}

	public LocalDateTime getDateSent() {
		return dateSent;
	}

	public void setDateSent(LocalDateTime dateSent) {
		this.dateSent = dateSent;
	}

}
