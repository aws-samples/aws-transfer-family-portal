// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import com.example.application.data.*;
import com.example.application.CredentialUtility;
import com.example.application.Toolkit;
import com.example.application.dao.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
@PageTitle("Keys")
@Route(value = "Keys", layout = MainLayout.class)
@PermitAll
public class KeyView extends VerticalLayout {
	private final static Logger logger = LogManager.getLogger(KeyView.class);
	private Grid<Key> grid = new Grid<>();
	private Map<Long, AppUser> users;
	private Map<Long, Key> keys;
	private Button btnIssueKey = new Button("Issue New Key");
	private AppUser user;
	private @Autowired KeyDAO keyDAO;
	private @Autowired AppUserDAO appUserDAO;
	public KeyView() {
		logger.info("Constructing");
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = ((UserDetails) principal).getUsername();
		user = AppUserDAO.getAppUserStatic(username);
		keys = KeyDAO.getKeysStatic(user);
		users = AppUserDAO.loadAppUsersStatic();
		ListDataProvider<Key> dp = (ListDataProvider<Key>) DataProvider.ofCollection(this.keys.values());
		dp.addSortOrder(k -> k.getCreated(), SortDirection.DESCENDING);
		grid.setDataProvider(dp);
		//grid.setHeightByRows(keys.size() < 8);
		btnIssueKey.setVisible(user.getRole() == Role.ROLE_ADMIN);
	
		initGrid();
		initLayout();
	}
	
	/*
	@PostConstruct
	private void init() {
		logger.info("Calling init");
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = ((UserDetails) principal).getUsername();
		user = appUserDAO.getAppUser(username);
		keys = keyDAO.getKeys(user);
		users = appUserDAO.getAppUsers(true);
		logger.info("user count=" + users.size());
		ListDataProvider<Key> dp = (ListDataProvider<Key>) DataProvider.ofCollection(this.keys.values());
		dp.addSortOrder(k -> k.getCreated(), SortDirection.DESCENDING);
		grid.setDataProvider(dp);
		grid.setHeightByRows(keys.size() < 8);
		btnIssueKey.setVisible(user.getRole() == Role.ROLE_ADMIN);

	}*/
	

	private void initGrid() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

		
		/*
		
		for (Key key : keys.values()) {
			if (!users.containsKey(key.getUserId())) {
				logger.error("The users table doesn't contain the userid " + key.getUserId() + " who is the owner of the key " +key.getId());
			}
		}*/
		
		grid.addColumn(k -> k.getId()).setHeader("Id").setFlexGrow(0).setWidth("75px");
		grid.addColumn(k -> this.users.get(k.getUserId()).getUsername()).setHeader("User").setSortable(true)
				.setFlexGrow(0).setWidth("200px");
		grid.addColumn(k -> k.getS3PrivateKeyPath()).setHeader("Private Key").setFlexGrow(1);
		grid.addColumn(k -> formatter.format(k.getCreated())).setHeader("Created").setSortable(true).setWidth("200px")
				.setFlexGrow(0);
		grid.addComponentColumn(k -> getDeletionButton(k)).setHeader("Delete").setFlexGrow(0).setWidth("125px");
		grid.addComponentColumn(k -> getResendButton(k)).setHeader("Re-Send").setFlexGrow(0).setWidth("125px");
		grid.setWidthFull();
	
	}

	private Button getResendButton(Key key) {
		Button btn = new Button();
		btn.setIcon(VaadinIcon.ENVELOPE_O.create());
		btn.addClickListener(e -> launchConfirmSend(key));
		return btn;
	}

	private Button getDeletionButton(Key key) {
		Button btn = new Button();
		btn.setIcon(VaadinIcon.TRASH.create());
		btn.addClickListener(e -> launchConfirmDelete(key));
		return btn;
	}

	private void initLayout() {
		H2 title = new H2("Key Library");
		add(title, grid, this.btnIssueKey);
		btnIssueKey.addClickListener(e -> issueNewKey());
	}

	private void launchConfirmSend(Key key) {

		Dialog dlg = new Dialog();
		VerticalLayout layout = new VerticalLayout();
		HorizontalLayout buttonContainer = new HorizontalLayout();
		Button btnSend = new Button("Send");
		Button btnCancel = new Button("Cancel");
		AppUser appUser = this.users.get(key.getUserId());
		dlg.add(layout);
		buttonContainer.add(btnSend, btnCancel);
		layout.add(new H3("Are you sure you want to resend key " + key.getId() + " to "
				+ this.users.get(key.getUserId()).getUsername() + "?"), buttonContainer);
		btnCancel.addClickListener(e -> dlg.close());
		btnSend.addClickListener(e -> {
			Email email = new Email("File Transfer Key Service", generateNewKeyBody(), appUser.getEmail());
			File privateKeyFile = key.getPrivateKeyFile();
			email.setAttachment(privateKeyFile);
			try {
				EmailDAO.sendEmail(email);
			} catch (MessagingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			dlg.close();
			Notification n = Notification.show("User has been resent their key.", 2000, Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

		});
		btnSend.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dlg.setModal(true);
		dlg.open();
	}

	private void launchConfirmDelete(Key key) {
		Dialog dlg = new Dialog();
		VerticalLayout layout = new VerticalLayout();
		HorizontalLayout buttonContainer = new HorizontalLayout();
		Button btnDelete = new Button("Delete");
		Button btnCancel = new Button("Cancel");
		dlg.add(layout);
		buttonContainer.add(btnDelete, btnCancel);
		layout.add(new H3("Are you sure you want to delete key " + key.getId() + "?"), buttonContainer);
		btnCancel.addClickListener(e -> dlg.close());
		btnDelete.addClickListener(e -> {
			logger.info("Delete key " + key.getS3PublicKeyPath() + " AWS Service");
			DeleteObjectRequest d = DeleteObjectRequest.builder()
					.bucket(Toolkit.RESOURCE_BUCKET)
					.key(key.getS3PublicKeyPath())
					.build();
			S3Client s3 = Toolkit.getS3Client();
			s3.deleteObject(d);
			logger.info("Deleting key from database");
			this.keyDAO.deleteFromDb(key);
			this.keys.remove(key.getId());
			this.grid.getDataProvider().refreshAll();
			dlg.close();
			Notification n = Notification.show("Key deleted: " + key.getId(), 2000, Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

		});
		btnDelete.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dlg.setModal(true);
		dlg.open();
	}

	private void issueNewKey() {
		Dialog dlg = new Dialog();
		VerticalLayout layout = new VerticalLayout();
		Select<AppUser> combo = new Select<AppUser>();
		Button btnIssue = new Button("Issue");
		Button btnCancel = new Button("Cancel");
		HorizontalLayout buttonContainer = new HorizontalLayout(btnIssue, btnCancel);
		btnIssue.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		ListDataProvider<AppUser> dp = (ListDataProvider<AppUser>) DataProvider.ofCollection(this.users.values().stream().filter(user->user.isEnabled()).collect(Collectors.toList()));
		dp.setSortOrder(AppUser::getUsername, SortDirection.ASCENDING);
		combo.setItems(this.users.values());
		//combo.setDataProvider(dp);
		combo.setItems(dp);
		
		combo.setTextRenderer(user -> user.getUsername());
		btnIssue.setEnabled(false);
		combo.addValueChangeListener(e -> {
			btnIssue.setEnabled(!combo.isEmpty());
		});
		btnCancel.addClickListener(e -> dlg.close());
		btnIssue.addClickListener(e -> {
			// KeyPair keyPair = KeyPairUtility.generateKeyPair();
			AppUser appUser = combo.getValue();
			// String s3ObjectPath = KeyPairUtility.uploadPrivateKeyToS3(keyPair, appUser);

			/* Create a key pair */
			KeyPair keyPair = CredentialUtility.generateKeyPair();
			String[] keyPaths = CredentialUtility.uploadKeysToS3(keyPair, appUser);
			String privateKeyS3Path = keyPaths[0];
			String publicKeyS3Path = keyPaths[1];
			
			Email email = new Email("File Transfer Key Service", generateNewKeyBody(), appUser.getEmail());
			
			Key key = new Key(

					appUser.getId(),
					privateKeyS3Path,
					publicKeyS3Path);

			keyDAO.insertToDb(key);
			keys.put(key.getId(), key);

			
			File privateKeyFile = key.getPrivateKeyFile();
			email.setAttachment(privateKeyFile);
			try {
				EmailDAO.sendEmail(email);
			} catch (MessagingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			
			
			
			grid.getDataProvider().refreshAll();
			dlg.close();
			Notification n = Notification.show(appUser.getUsername() + " has been sent their new private key.", 2000,
					Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		});

		dlg.add(layout);
		layout.add(new H3("Issue a new key to the selected user"), combo, buttonContainer);
		dlg.setModal(true);
		dlg.open();

	}

	
	private String generateNewKeyBody(/*String presignedUrl*/) {
		StringBuilder sb = new StringBuilder();
		sb.append("<h3>Key Services</h3>");
		sb.append("<p>Your private key for the Stargate service is attached.<br><br>");
		//sb.append("<b>Private Key: </b><a href=\"" + presignedUrl + "\">Click Me</a><br><br>");
		sb.append("Thank you!</p>");
		return sb.toString();
	}

}
