// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.views;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.application.CredentialUtility;
import com.example.application.Toolkit;
import com.example.application.dao.EmailDAO;
import com.example.application.data.AddEditMode;
import com.example.application.data.Role;
import com.example.application.data.AppUser;
import com.example.application.data.DirectoryMapping;
import com.example.application.data.Email;
import com.example.application.data.Key;
import com.example.application.data.Organization;
import com.example.application.ui.util.LumoStyles;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.RegexpValidator;


@SuppressWarnings("serial")
public class UserForm extends VerticalLayout {
	private final static Logger logger = LogManager.getLogger(UserForm.class);
	private TextField tfFirstName = new TextField("First name");
	private TextField tfLastName = new TextField("Last name");
	private TextField tfUsername = new TextField("Username");
	private EmailField tfEmail = new EmailField("Email");
	private Checkbox issueCert = new Checkbox("Issue Key Pair");
	private Select<Role> slRole = new Select<>();
	private Select<Organization> slOrg = new Select<>();
	private Checkbox cbActive = new Checkbox("Active");
	private FormLayout form = new FormLayout();
	private BeanValidationBinder<AppUser> binder = new BeanValidationBinder<>(AppUser.class);
	private static final int minNameChars = 2;
	private static final int maxNameChars = 15;
	private static final int minUsernameChars = 4;
	private static int maxUsernameChars = 15;
	private static final String nameRegEx = "^[a-zA-Z,.'-]{" + minNameChars + "," + maxNameChars + "}";
	private static final String usernamePattern = "[a-zA-Z][a-zA-Z0-9]{" + (minUsernameChars - 1) + ","
			+ maxUsernameChars + "}";
	private final Map<Long, Organization> organizations;
	private Button btnCancel = new Button("Cancel");
	private AppUser user;
	private UserAdminView userAdminView;
	private AddEditMode mode;
	private ListDataProvider<DirectoryMapping> dataProvider;
	private Button btnDirectoryMapping = new Button("Directory Mappings");
	private Button btnPasswordReset = new Button("Reset Password");
	private EmailDAO emailDAO;

	public UserForm(EmailDAO emailDAO, UserAdminView userAdminView, AppUser user) {
		mode = user == null ? AddEditMode.ADD : AddEditMode.EDIT;
		this.user = user == null ? new AppUser() : user;
		this.userAdminView = userAdminView;
		if (mode==AddEditMode.EDIT) {
				this.organizations = userAdminView.getOrganizations();
				if (!organizations.containsKey(user.getOrganization().getId())) {
					logger.error("Can't find org for " + user.getUsername());
				}
				
		}
		/*Goal is to prevent new users from being assigned to inactive orgs*/
		else {
			organizations = new HashMap<>();
			userAdminView.getOrganizations()
				.values()
				.stream()
				.filter(org->org.isActive()).collect(Collectors.toList())
				.stream()
				.forEach(org->organizations.put(org.getId(), org));;
		}
			
		this.emailDAO = emailDAO;
		initLayout();
		initComponents();
		initBinder();
		initListeners();
		binder.readBean(this.user);
		
		
		
		btnDirectoryMapping.setEnabled(user != null);
	}
	

	private void initComponents() {
		slOrg.setLabel("Organization");
		List<Organization> organizationList = new ArrayList<Organization>(organizations.values());
		Comparator<Organization> nameCompare = (Organization o1, Organization o2) -> o1.getDescription()
				.compareTo(o2.getDescription());
		Collections.sort(organizationList, nameCompare);
		slOrg.setItems(organizationList);

		slOrg.setItems(organizationList);
		slOrg.setTextRenderer(r -> r.getDescription());
		slRole.setLabel("Role");
		slRole.setTextRenderer(r -> r.getFriendly());
		slRole.setItems(Role.values());
	}

	private void initBinder() {
		binder.forField(tfFirstName)
				.withValidator(new RegexpValidator(
						"Invalid first name: " + minNameChars + " to " + maxNameChars + " characters.", nameRegEx))
				.bind(AppUser::getFirstName, AppUser::setFirstName);
		binder.forField(tfLastName)
				.withValidator(new RegexpValidator(
						"Invalid last name: " + minNameChars + " to " + maxNameChars + " characters.", nameRegEx))
				.bind(AppUser::getLastName, AppUser::setLastName);
		binder.forField(tfUsername)
				.withValidator(new RegexpValidator("Invalid username: " + minUsernameChars + " to " + maxUsernameChars
						+ " characters starting with a letter.", usernamePattern))
				.bind(AppUser::getUsername, AppUser::setUsername);
		binder.forField(tfEmail).withValidator(new EmailValidator("Invalid Email Address")).bind(AppUser::getEmail,
				AppUser::setEmail);
		binder.forField(slRole).withValidator(r -> r != null, "Role is required").bind(AppUser::getRole,
				AppUser::setRole);
		binder.forField(slOrg).withValidator(r -> r != null, "Organization is required").bind(AppUser::getOrganization,
				AppUser::setOrganization);
		binder.forField(cbActive).bind(AppUser::isEnabled, AppUser::setEnabled);
	}

	
	
	private void initLayout() {
		form.add(
				tfFirstName, tfLastName,
				tfUsername,
				tfEmail,
				slRole, slOrg, issueCert, cbActive, this.btnDirectoryMapping, this.btnPasswordReset/* directoryMappingGrid, */ /*
																										 * mappingStripContainer
																										 */
		);

		btnDirectoryMapping.setIcon(VaadinIcon.BOOK.create());
		btnPasswordReset.setIcon(VaadinIcon.RECYCLE.create());
		addClassNames(LumoStyles.Padding.Bottom.L, LumoStyles.Padding.Horizontal.L, LumoStyles.Padding.Top.S);
		form.setResponsiveSteps(
				new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
				new FormLayout.ResponsiveStep("21em", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP));
		issueCert.setVisible(this.mode == AddEditMode.ADD);
		tfUsername.setReadOnly(this.mode == AddEditMode.EDIT);
		btnPasswordReset.setVisible(mode==AddEditMode.EDIT);
		add(form);
		btnDirectoryMapping.addClickListener(e -> {
			DirectoryMappingView view = new DirectoryMappingView(this.user, this.userAdminView);
			view.open();
		});
		btnPasswordReset.addClickListener(e-> resetPassword());
	}
	

	/**
	 * Save changes to the user.
	 */
	public boolean updateExistingUser() {
		logger.info("Updating user " + user.getUsername());
		boolean valid = this.binder.writeBeanIfValid(user);
		if (!valid) {
			logger.info("Invalid binder");
			return false;
		} else {
			userAdminView.getAppUserDAO().update(user);
			userAdminView.getUserGrid().getDataProvider().refreshAll();
			Notification n = Notification.show("User changes updated", 2500, Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			setButtonDefaultView(true);
			return true;
		}
	}

	public boolean saveNewUser() {
		binder.writeBeanIfValid(user);
		boolean valid = this.binder.writeBeanIfValid(user);
		if (!valid) {
			logger.info("Invalid binder");
			return false;
		}

		if (!validUser()) {
			return false;
		}
		String tempPassword = null;
		
		tempPassword = CredentialUtility.generatePassword();
		String pashword = Toolkit.hashPassword(tempPassword);
		user.setPasswordExpiration(Toolkit.getSanDiegoTime().plusDays(7));
		user.setPassword(pashword);

		/* Insert the user's info to the DB */
		userAdminView.getAppUserDAO().insert(user/* , directoryMappingSeeds */);
		user.getDirectoryMappings().values().forEach(m -> m.createS3FolderIfDoesNotExist());
		String privateKeyS3Path = null;
		Key key = null;
		if (issueCert.getValue()) {
			KeyPair keyPair = CredentialUtility.generateKeyPair();
			String[] keyPaths = CredentialUtility.uploadKeysToS3(keyPair, user);
			privateKeyS3Path = keyPaths[0];
			String publicKeyS3Path = keyPaths[1];
			key = new Key(
					user.getId(),
					privateKeyS3Path,
					publicKeyS3Path);
			userAdminView.getKeyDAO().insertToDb(key);
		}
		sendNewUserEmail(user, tempPassword, /*, presignedUrl*/  key);
		this.userAdminView.getUserMap().put(user.getId(), user);
		this.userAdminView.getUserGrid().getDataProvider().refreshAll();
		Notification n = Notification.show(
				"New user " + user.getUsername() + " successfully created, you may now add directory mappings.", 2000,
				Position.MIDDLE);
		this.btnDirectoryMapping.setEnabled(true);
		n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		
		
		return true;
	}

	private void sendNewUserEmail(AppUser newUser, String tempPassword , Key key) {
		String endpoint = Toolkit.CUSTOM_HOSTNAME == null ? Toolkit.SFTP_ENDPOINT : Toolkit.CUSTOM_HOSTNAME;
		StringBuilder sb = new StringBuilder();
		sb.append("<h2>Connection Instructions</h2>");
		sb.append("<p>Congratulations on enrolling in the File Transfer Service.  Please save this email  .");
		sb.append("You can upload and download files using the client of your choice.  ");
		sb.append("For information on connecting using WinSCP, FileZilla, Cyberduck or other applications, ");
		sb.append(
				"<a href=\"https://docs.aws.amazon.com/transfer/latest/userguide/transfer-file.html\"> click on this link</a>.  ");
		sb.append("Use the following parameters to connect. <br><br>");
		sb.append("<b>Username: </b>" + newUser.getUsername() + "<br>");
		sb.append("<strong>Endpoint: </strong>" + endpoint + "<br>");
		sb.append("<b>Temporary Password (expires in 7 days): </b>" + tempPassword + " <br><br>");
		if (issueCert.getValue()) {
			sb.append("You can also authenticate using the attached private key.<br><br>");
		}
		
		Email email = new Email(Toolkit.ENROLLMENT_EMAIL_SUBJECT, sb.toString(), newUser.getEmail());
		if (issueCert.getValue()) {
			File privateKeyFile = key.getPrivateKeyFile();
			email.setAttachment(privateKeyFile);
		}
		sb.append("Thank you for enrolling; please email " + Toolkit.SENDER +" with any questions.");

		try {
			EmailDAO.sendEmail(email);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	private void resetPassword() {
		Dialog dlg = new Dialog();
		Button btnConfirm = new Button("Confirm");
		Button btnCancel = new Button("Cancel");
		VerticalLayout layout = new VerticalLayout();
		Label title = new Label("Are you sure you want to reset " + this.user.getUsername() +"'s password?  They will receive a new password via email.  Their previous password will be disabled.");
		HorizontalLayout buttonContainer = new HorizontalLayout(btnConfirm, btnCancel);
		layout.add(title, buttonContainer);
		dlg.add(layout);
		btnCancel.addClickListener(e->dlg.close());
		btnConfirm.addClickListener(e-> {
			String tempPassword = CredentialUtility.generatePassword();
			String pashword = Toolkit.hashPassword(tempPassword);
			user.setPasswordExpiration(Toolkit.getSanDiegoTime().plusDays(7));
			user.setPassword(pashword);

			/* Insert the user's info to the DB */
			userAdminView.getAppUserDAO().update(user);
			String body = "<p>Please find your temporary password for the File Transfer Portal below. <br>";
			body += "Note that it will expire in 7 days.<br><br>";
			body += "<b>Username: </b>" + user.getUsername() + "<br>";
			body += "<b>Temporary Password: </b>" + tempPassword + "</p>";
			
			Email email = new Email("File TransPassword Reset", body, this.user.getEmail());
			try {
			EmailDAO.sendEmail(email);
 			} catch(MessagingException g) {
 				logger.error(g.getMessage());
 			}
			Notification n = Notification.show("New password successfully issued.",2000, Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			dlg.close();

		});
		btnConfirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dlg.setModal(true);
		dlg.open();
	}

	private void initListeners() {
		btnCancel.addClickListener(e -> {
			if (mode == AddEditMode.ADD) {
				this.user = new AppUser();
				this.dataProvider.refreshAll();
				this.binder.readBean(user);
			} else {
				binder.readBean(user);
			}
		});
	}

	private boolean validUser() {
		if (!binder.isValid()) {
			Notification n = Notification.show("Please fix field errors", 3000, Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return false;
		}
		if (userAdminView.getAppUserDAO().usernameExists(this.tfUsername.getValue())) {
			Notification n = Notification.show("There is already a user with username " + tfUsername.getValue(), 3000,
					Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return false;
		}

		/* Validate email address. Only necessary in SES Sandbox mode */
		String email = this.tfEmail.getValue();
		if (!emailDAO.validEmailAddress(email)) {
			String error = "The email address " + email
					+ " hasn't been verified. Note that email addresses are case sensitive.";
			Notification n = Notification.show(error, 2500, Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			return false;
		}

		return true;
	}

	public void setButtonDefaultView(boolean flag) {
		btnCancel.setVisible(!flag);
		tfFirstName.setReadOnly(flag);
		tfLastName.setReadOnly(flag);
		tfEmail.setReadOnly(flag);
		slOrg.setReadOnly(flag);
		cbActive.setReadOnly(flag);
	}

	public TextField getUsernameField() {
		return this.tfUsername;
	}

}
