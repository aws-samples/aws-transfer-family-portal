package com.example.application.views;

import java.time.LocalDateTime;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.application.Toolkit;
import com.example.application.dao.AppUserDAO;
import com.example.application.dao.CloudWatchService;
import com.example.application.data.AppUser;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@SuppressWarnings("serial")

@PageTitle("Password Mgmt")
@Route(value = "Password", layout = MainLayout.class)
public class ChangePassword extends VerticalLayout {
	private AppUser user;
	private VerticalLayout form = new VerticalLayout();
	private FormLayout CheckboxContainer = new FormLayout();
	private PasswordField pf1 = new PasswordField("Enter Password");
	private PasswordField pf2 = new PasswordField("Confirm Password");
	private Checkbox cbLength = new Checkbox("7+ Characters");
	private Checkbox cbUpper = new Checkbox("1+ Uppercase");
	private Checkbox cbLower = new Checkbox("1+ Lower");
	private Checkbox cbNumber = new Checkbox("1+ Number");
	private Checkbox cbChar = new Checkbox("1+ Special Character");
	private Checkbox cbMatch = new Checkbox("Passwords match");
	private Checkbox[] Checkboxes;
	private Button btnSave = new Button("Save");
	private HorizontalLayout buttonContainer = new HorizontalLayout();
	private AppUserDAO appUserDAO;

	public ChangePassword(@Autowired CloudWatchService sessionData, @Autowired AppUserDAO appUserDAO) {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = ((UserDetails) principal).getUsername();
		this.appUserDAO = appUserDAO;
		user = appUserDAO.getAppUser(username);
		initLayout();
	}

	private void initLayout() {
		btnSave.setEnabled(false);
		btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		add(new H2("Change Password"), form, CheckboxContainer, buttonContainer);
		buttonContainer.add(btnSave);
		CheckboxContainer.setMaxWidth("800px");
		form.add(pf1, pf2);
		Checkboxes = new Checkbox[] { cbLength, cbUpper, cbLower, cbNumber, cbChar, cbMatch };
		for (int i = 0; i < Checkboxes.length; i++) {
			Checkbox cb = Checkboxes[i];
			cb.setReadOnly(true);
			CheckboxContainer.add(cb);
			cb.addValueChangeListener(e -> {
				validate();
			});
		}
		pf1.addValueChangeListener(e -> {
			String pwd = pf1.getValue();
			cbLength.setValue(pwd.length() >= 7);
			cbUpper.setValue(!pwd.equals(pwd.toLowerCase()));
			cbLower.setValue(!pwd.equals(pwd.toUpperCase()));
			Pattern numPattern = Pattern.compile("[0-9]");
			cbNumber.setValue(numPattern.matcher(pwd).find());
			Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
			cbChar.setValue(p.matcher(pwd).find());
			cbMatch.setValue(pf1.getValue().equals(pf2.getValue()));
		});
		pf1.setValueChangeMode(ValueChangeMode.EAGER);
		pf2.addValueChangeListener(e -> {
			cbMatch.setValue(pf1.getValue().equals(pf2.getValue()));
		});
		btnSave.addClickListener(e -> {
			this.user.setPassword(Toolkit.hashPassword(pf1.getValue()));
			this.user.setPasswordExpiration(LocalDateTime.now().plusYears(1));
			this.appUserDAO.update(user);
			Notification n = Notification.show("Password successfully updated", 2000, Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		});
		pf2.setValueChangeMode(ValueChangeMode.EAGER);
	}

	private void validate() {
		boolean valid = true;
		for (int j = 0; j < Checkboxes.length; j++) {
			valid = valid && Checkboxes[j].getValue();
		}
		btnSave.setEnabled(valid);
	}
}
