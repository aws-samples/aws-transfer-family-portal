// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.views;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

import com.example.application.data.*;
import com.example.application.dao.*;

@PageTitle("Organization Admin")
@Route(value = "Organizations", layout = MainLayout.class)
@PermitAll
public class OrganizationView extends VerticalLayout {
	Tabs tabs = new Tabs();
	private Grid<Organization> grid = new Grid<>();
	private Button btnCreateOrganization = new Button();
	private TextField searchField = new TextField();;
	private ListDataProvider<Organization> dataProvider;
	private OrganizationDAO organizationDAO;
	private AppUserDAO appUserDAO ;
	private static final int minOrgChars = 3;
	private static int maxOrgChars = 30;
	private static final String orgRegEx = "[a-zA-Z][a-zA-Z0-9]{" + (minOrgChars - 1) + "," + maxOrgChars + "}";
	
	private Map<Long, Organization> organizations;
	
	public OrganizationView(@Autowired OrganizationDAO organizationDAO, @Autowired AppUserDAO appUserDAO) {
		this.appUserDAO= appUserDAO;
		Optional<? extends GrantedAuthority> grantedAuthority = SecurityContextHolder.getContext().getAuthentication()
				.getAuthorities().stream().findFirst();
		if (grantedAuthority.isPresent() && grantedAuthority.get().getAuthority().equals("ROLE_ADMIN")) {
			this.organizationDAO = organizationDAO;
			/*
			this.organizations = organizationDAO.getOrganizations();
		dataProvider = (ListDataProvider<Organization>) DataProvider
				.ofCollection(organizationDAO.getOrganizations().values());*/
		organizations = organizationDAO.getOrganizations();
		dataProvider = (ListDataProvider<Organization>) DataProvider.ofCollection(organizations.values());
			initLayout();
			initSearchBar();
			initGrid();
		}
		else {
			add(new Label("Unauthorized User"));
		}
	}

	private void initLayout() {
		HorizontalLayout top = new HorizontalLayout(searchField, btnCreateOrganization);
		add(top, grid);
		btnCreateOrganization.addClickListener(e -> editor(null));
		btnCreateOrganization.setIcon(VaadinIcon.PLUS.create());
		top.setWidth("100%");
		searchField.setWidth("80%");
		setWidth("60%");
		setHeightFull();
	}

	
	
	
	
	
	private void editor(Organization org) {
		boolean createNew = org == null;
		Organization organization = createNew ? new Organization() : org;
		if (createNew) {
			org = new Organization();
		}
		Dialog dlg = new Dialog();
		TextField tfDescription = new TextField("Description");
		Checkbox cbActive = new Checkbox("Active");
		FormLayout form = new FormLayout(tfDescription, cbActive);
		Button btnSave = new Button("Save");
		Button btnCancel = new Button("Cancel", e -> dlg.close());
		HorizontalLayout buttonContainer = new HorizontalLayout(btnSave, btnCancel);
		H2 title = new H2(createNew ? "New Organization" : "Edit Organization");
		VerticalLayout layout = new VerticalLayout(title, form, buttonContainer);
		Binder<Organization> binder = new Binder<>();
		dlg.add(layout);
		
		binder.forField(tfDescription).withValidator(new RegexpValidator("Invalid org name: " + minOrgChars + " to " + maxOrgChars + " characters, numbers or letters only, starting with a letter.", orgRegEx)).bind(Organization::getDescription, Organization::setDescription);
		binder.forField(cbActive).bind(Organization::isActive, Organization::setActive);
		binder.readBean(organization);
		
		/*
		binder.forField(tfUsername)
		.withValidator(new RegexpValidator("Invalid username: " + minUsernameChars + " to " + maxUsernameChars
				+ " characters starting with a letter.", usernamePattern))
		.bind(AppUser::getUsername, AppUser::setUsername);
	*/
		btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dlg.setModal(true);
		dlg.open();

		btnSave.addClickListener(e -> {
			boolean valid = binder.isValid();
			
			if (!valid) {
				Notification n = Notification.show("Invalid data.", 2500, Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				return;
			}
			
			/*
			boolean updateModeDuplicateName = !createNew && !tfDescription.getValue().equalsIgnoreCase(organization.getDescription());
			boolean createModeDuplicateName = createNew && organizationDAO.organizationExists(tfDescription.getValue());
			*/
			
			boolean duplicateNameOnNew = createNew && organizationDAO.organizationExists(tfDescription.getValue());
			boolean duplicateNameOnUpdate = !createNew && !tfDescription.getValue().equals(organization.getDescription()) && organizationDAO.organizationExists(tfDescription.getValue());
			
			boolean duplicateName = duplicateNameOnNew || duplicateNameOnUpdate;
			
			if (duplicateName) {
				Notification n = Notification.show("An organization with name " + tfDescription.getValue() + " already exists.",2500, Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				return;
			}
			/*
			if (!createNew) {
				if (!tfDescription.getValue().equalsIgnoreCase(organization.getDescription())) {
					if (organizationDAO.organizationExists(tfDescription.getValue())) {
						Notification n = Notification.show("An organization with name " + tfDescription.getValue() + " already exists.",2500, Position.MIDDLE);
						n.addThemeVariants(NotificationVariant.LUMO_ERROR);
						return;
					}
				}
			}
			*/
			binder.writeBeanIfValid(organization);
				
				if (createNew) {
					/*
					if (organizationDAO.organizationExists(tfDescription.getValue())) {
						Notification n = Notification.show("An organization with name " + tfDescription.getValue() + " already exists.",2500, Position.MIDDLE);
						n.addThemeVariants(NotificationVariant.LUMO_ERROR);
						return;
					}*/
					organizationDAO.insert(organization);
					this.organizations.put(organization.getId(), organization);
					Notification n = Notification.show(organization.getDescription() + " successfully created!", 2500, Position.MIDDLE);
					n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				} else {
					organizationDAO.update(organization);
					Notification n = Notification.show(organization.getDescription() + " successfully updated!", 2500,
							Position.MIDDLE);
					n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				}
				dataProvider.refreshAll();
				dlg.close();
		});

	}

	private void initGrid() {
		grid.addColumn(Organization::getDescription).setHeader("Description").setSortable(true);
		grid.addColumn(org->org.isActive() ? "Yes" : "No").setHeader("Active").setSortable(true);
		grid.addComponentColumn(org -> getEditButton(org)).setSortable(true).setHeader("Edit");
		grid.addComponentColumn(org->getDeactivateUsersButton(org)).setHeader("Deactivate All Users");
		grid.setHeight("100%");
		grid.setDataProvider(dataProvider);
	}

	
	private Button getDeactivateUsersButton(Organization org) {
		Button btn = new Button();
		btn.setEnabled(!org.isActive());
		btn.addClickListener(e->deactivateAllUsers(org));
		btn.setIcon(VaadinIcon.FILE_REMOVE.create());
		return btn;
	}
	
	private void deactivateAllUsers(Organization org) {
		Dialog dlg = new Dialog();
		Button btnConfirm = new Button("Deactivate Users");
		Button btnCancel = new Button("Cancel");
		Label lbl = new Label("Are you sure you want to disable all users in the organization " + org.getDescription() + "?");
		HorizontalLayout buttonContainer = new HorizontalLayout(btnConfirm, btnCancel);
		VerticalLayout layout = new VerticalLayout(lbl, buttonContainer);
		dlg.add(layout);
		dlg.setModal(true);
		
		btnConfirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnCancel.addClickListener(e-> dlg.close());
		btnConfirm.addClickListener(e-> {
			
			appUserDAO.deactivateAllUsers(org);
			dlg.close();
			Notification n = Notification.show("All users in the organization " +org.getDescription() + " have been deactivated.", 2000, Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
		});
		
		dlg.open();
	}
	
	private Button getEditButton(Organization org) {
		Button btn = new Button();
		btn.setIcon(VaadinIcon.EDIT.create());
		btn.addClickListener(e -> editor(org));
		return btn;
	}

	private void initSearchBar() {
		searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
		searchField.setValueChangeMode(ValueChangeMode.EAGER);
		searchField.addValueChangeListener(e -> {
			dataProvider.refreshAll();
			System.out.println("val change");
		});
		dataProvider.addFilter(org -> {
			String searchTerm = searchField.getValue().trim();
			if (searchTerm.isEmpty()) {
				return true;
			}
			boolean matchesDescription = matchesTerm(org.getDescription(), searchTerm);
			System.out.println(org.getDescription() + ", " + (matchesDescription));
			return matchesDescription;
		});
	}

	private boolean matchesTerm(String target, String searchTerm) {
		return target.toLowerCase().contains(searchTerm.toLowerCase());
	}
}
