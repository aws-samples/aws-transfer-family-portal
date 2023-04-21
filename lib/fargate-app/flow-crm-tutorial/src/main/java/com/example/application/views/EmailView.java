// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.application.dao.AppUserDAO;
import com.example.application.dao.EmailDAO;
import com.example.application.dao.OrganizationDAO;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.example.application.data.*;
@PageTitle("Email")
@Route(value = "Email", layout = MainLayout.class)
public class EmailView extends VerticalLayout {
	private final static Logger logger = LogManager.getLogger(EmailView.class);

	private TextField tfSubject = new TextField("Subject");
	private TextArea taArea = new TextArea("Message");
	private Button btnSend = new Button("Send");
	private Button btnClear = new Button("Clear");
	private Button btnVerify = new Button("Verify New Email Address");
	private HorizontalLayout buttonContainer = new HorizontalLayout(btnSend, btnClear, btnVerify);
	private AppUserDAO appUserDAO;
	private EmailDAO emailDAO;
	private static final String ALL_ACTIVE = "All Active Users";
	private static final String SINGLE_ORG = "Single Organization";
	private Select<Organization> slOrg = new Select<>();
	private Map<Long, Organization> organizations;
	private RadioButtonGroup<String> radioGroup = new RadioButtonGroup<>();

	public EmailView(@Autowired AppUserDAO appUserDAO, @Autowired EmailDAO emailDAO,
			@Autowired OrganizationDAO organizationDAO) {
		if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().findFirst().get()
				.getAuthority().equals("ROLE_ADMIN")) {
			organizations = organizationDAO.getOrganizations();
			initLayout();
			this.appUserDAO = appUserDAO;
			this.emailDAO = emailDAO;
		}

		else {
			add(new Label("Unauthorized User"));
		}
	}

	private void initLayout() {
		btnClear.addClickListener(e -> clear());
		btnSend.addClickListener(e -> confirm());
		btnVerify.addClickListener(e -> verify());
		
		/*Verification not necessary in SES production mode*/
		btnVerify.setVisible(false);
		
		btnSend.setIcon(VaadinIcon.ENVELOPE.create());
		btnSend.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		tfSubject.setWidth("500px");
		taArea.setWidth("500px");
		taArea.setHeight("200px");

		slOrg.setLabel("Organization");

		List<Organization> organizationList = new ArrayList<Organization>(organizations.values());
		Comparator<Organization> nameCompare = (Organization o1, Organization o2) -> o1.getDescription()
				.compareTo(o2.getDescription());
		Collections.sort(organizationList, nameCompare);
		slOrg.setItems(organizationList);

		slOrg.setTextRenderer(r -> r.getDescription());

		radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		radioGroup.setLabel("Recipients");
		radioGroup.setItems(ALL_ACTIVE, SINGLE_ORG);

		radioGroup.setValue(ALL_ACTIVE);
		slOrg.setVisible(false);
		radioGroup.addValueChangeListener(e -> {
			slOrg.setVisible(!radioGroup.getValue().equals(ALL_ACTIVE));
		});

		slOrg.setWidth("300px");
		add(radioGroup);
		add(new H3("Email all active users"), tfSubject, taArea, radioGroup, slOrg, buttonContainer);

	}

	private void clear() {
		tfSubject.clear();
		taArea.clear();
	}

	private void verify() {
		Dialog dlg = new Dialog();
		VerticalLayout layout = new VerticalLayout();
		Button btnVerify = new Button("Verify");
		Button btnCancel = new Button("Cancel");
		HorizontalLayout buttonContainer = new HorizontalLayout(btnVerify, btnCancel);
		Html html = new Html(
				"<p>A link will be sent to the recipient that they should click.  This step will NOT be necessary in production, it is just for the POC.</p>");
		EmailField field = new EmailField("Email Address");
		layout.add(field, html, buttonContainer);
		dlg.add(layout);
		btnCancel.addClickListener(e -> dlg.close());
		btnVerify.addClickListener(e -> {
			if (field.isInvalid()) {
				Notification n = Notification.show("Invalid Email Address", 2500, Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				return;
			}

			emailDAO.verifyEmail(field.getValue());
			Notification n = Notification.show("Verification email sent to " + field.getValue());
			n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			dlg.close();
		});

		dlg.open();

	}

	private void confirm() {
		if (slOrg.isEmpty() && radioGroup.getValue().equals(SINGLE_ORG)) {
			Notification n = Notification.show("Please select an organization", 2000, Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return;
		}
		Set<String> recipients = getRecipients();
		if (recipients == null || recipients.size() == 0) {
			Notification n = Notification.show("This selection has no active users.", 2000, Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return;
		}

		Dialog dlg = new Dialog();
		Button btnCancel = new Button("Cancel");
		Button btnConfirm = new Button("Confirm");
		HorizontalLayout buttonContainer = new HorizontalLayout(btnConfirm, btnCancel);
		VerticalLayout layout = new VerticalLayout(new H3("Are you sure you want to send this email?"),
				buttonContainer);
		dlg.add(layout);

		btnConfirm.addClickListener(e -> {
			Set<String> verifiedRecipients = getRecipients();
			Set<String> unverifiedEmailAddresses = new HashSet<>();
			for (String recipient : getRecipients()) {
				boolean valid = emailDAO.validEmailAddress(recipient);
				if (!valid) {
					unverifiedEmailAddresses.add(recipient);
					verifiedRecipients.remove(recipient);
				}
			}

			String unverified = "The following email address(es) are unverified and will not receive this email: ";
			if (unverifiedEmailAddresses.size() > 0) {
				unverified += String.join(",", unverifiedEmailAddresses);
			}

			if (verifiedRecipients.size() == 0) {
				Notification.show(unverified, 4000, Position.MIDDLE);
				dlg.close();
			}

			if (verifiedRecipients.size() > 0) {

				Email email = new Email(tfSubject.getValue(), taArea.getValue(), verifiedRecipients);
				try {
					EmailDAO.sendEmail(email);
				} catch (MessagingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				String message = "Message successfully sent!";
				if (unverifiedEmailAddresses.size() > 0) {
					message += unverified;
				}
				Notification.show(message, 2000, Position.MIDDLE);
				dlg.close();
				clear();
			}
		});

		btnConfirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnCancel.addClickListener(e -> dlg.close());
		dlg.setModal(true);
		dlg.setWidth("80%");
		dlg.open();
	}

	private Set<String> getRecipients() {
		Map<Long, AppUser> users = appUserDAO.getAppUsers(true);
		Set<String> recipients = null;
		if (radioGroup.getValue().equals(ALL_ACTIVE)) {
			recipients = users.values().stream().filter(x -> x.isEnabled())
					.map(user -> user.getEmail()).collect(Collectors.toSet());
		}

		else {
			recipients = users.values().stream()
					.filter(x -> x.isEnabled() && x.getOrganization().getId() == slOrg.getValue().getId())
					.map(user -> user.getEmail()).collect(Collectors.toSet());
		}
		return recipients;
	}
}
