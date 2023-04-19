// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.application.dao.AppUserDAO;
import com.example.application.dao.CloudWatchService;
import com.example.application.dao.EmailDAO;
import com.example.application.dao.KeyDAO;
import com.example.application.dao.OrganizationDAO;
import com.example.application.data.*;
import com.example.application.ui.components.FlexBoxLayout;
import com.example.application.ui.components.detailsdrawer.DetailsDrawer;
import com.example.application.ui.components.detailsdrawer.DetailsDrawerFooter;
import com.example.application.ui.components.detailsdrawer.DetailsDrawerHeader;
import com.example.application.ui.layout.size.Horizontal;
import com.example.application.ui.layout.size.Top;
import com.example.application.ui.util.UIUtils;
import com.example.application.ui.util.css.BoxSizing;

@PageTitle("User Admin")
@Route(value = "Users", layout = MainLayout.class)

public class UserAdminView extends SplitViewFrame {

	private final static Logger logger = LogManager.getLogger(UserAdminView.class);
	private Button btnClearAll = new Button("Clear All");
	private Grid<AppUser> userGrid = new Grid<>();
	private Map<Long, AppUser> userMap;
	private Button btnCreateUser = new Button("");
	private Button btnEditUser = new Button("Edit User");
	private DetailsDrawer detailsDrawer;
	private DetailsDrawerHeader detailsDrawerHeader;
	private AppUserDAO appUserDAO;
	private OrganizationDAO organizationDAO;
	private KeyDAO keyDAO;
	private ListDataProvider<AppUser> dataProvider;
	private UserForm userForm;
	private TextField searchField;
	private EmailDAO emailDAO;
	private Checkbox cbToggleActive = new Checkbox("Hide Inactive");
	
	public UserAdminView(@Autowired EmailDAO emailDAO, @Autowired CloudWatchService sessionData,
			@Autowired AppUserDAO appUserDAO, @Autowired OrganizationDAO organizationDAO, @Autowired KeyDAO keyDAO) {

		Optional<? extends GrantedAuthority> grantedAuthority = SecurityContextHolder.getContext().getAuthentication()
				.getAuthorities().stream().findFirst();
		if (grantedAuthority.isPresent() && grantedAuthority.get().getAuthority().equals("ROLE_ADMIN")) {
			this.organizationDAO = organizationDAO;
			this.keyDAO = keyDAO;
			this.emailDAO = emailDAO;
			userMap = appUserDAO.getAppUsers(true);
			dataProvider = (ListDataProvider<AppUser>) DataProvider.ofCollection(userMap.values());
			userForm = new UserForm(emailDAO, this, null);
			
			for (AppUser user : userMap.values()) {
				if (user.getOrganization()==null) {
					logger.error("Org null for user " + user.getUsername());
					System.exit(1);
				}
			}
			
			
			setViewContent(createContent());
			setViewDetails(createDetailsDrawer());
			initLayout();
			this.btnCreateUser.addClickListener(e -> this.showNewUserDetails());
		} else {
			setViewContent(new Label("Unauthorized User"));
		}

	}

	private Component createContent() {
		FlexBoxLayout content = new FlexBoxLayout(createGrid());
		content.setBoxSizing(BoxSizing.BORDER_BOX);
		content.setHeightFull();
		content.setPadding(Horizontal.RESPONSIVE_X, Top.RESPONSIVE_X);
		return content;
	}

	private DetailsDrawer createDetailsDrawer() {
		detailsDrawer = new DetailsDrawer(DetailsDrawer.Position.RIGHT);

		// Header
		detailsDrawerHeader = new DetailsDrawerHeader("");
		detailsDrawerHeader.addCloseListener(buttonClickEvent -> detailsDrawer.hide());
		detailsDrawer.setHeader(detailsDrawerHeader);

		// Footer
		DetailsDrawerFooter footer = new DetailsDrawerFooter();
		footer.addSaveListener(e -> {
			detailsDrawer.hide();
			UIUtils.showNotification("Changes saved.");
		});
		footer.addCancelListener(e -> detailsDrawer.hide());
		detailsDrawer.setFooter(footer);
		detailsDrawer.setHeightFull();
		return detailsDrawer;
	}

	private void initLayout() {
		// H2 title = new H2("Users");
		HorizontalLayout mainHorizontalLayout = new HorizontalLayout();
		// VerticalLayout right = new VerticalLayout();
		btnEditUser.setEnabled(false);
		btnClearAll.setIcon(VaadinIcon.REFRESH.create());
		btnCreateUser.setIcon(VaadinIcon.PLUS.create());
		mainHorizontalLayout.setHeight("100%");
	}

	private void showNewUserDetails() {
		detailsDrawerHeader.setTitle("New User");
		detailsDrawer.setContent(createDetails(null));
		DetailsDrawerFooter footer = new DetailsDrawerFooter();
		detailsDrawer.setFooter(footer);
		footer.addCancelListener(e -> detailsDrawer.hide());
		footer.addSaveListener(e -> {
			boolean valid = userForm.saveNewUser();
			footer.setSaveEnabled(false);
		});
		detailsDrawer.show();
	}

	private void showDetails(AppUser user) {
		detailsDrawerHeader.setTitle(user.getFirstName() + " " + user.getLastName() + " Organization=" + user.getOrganization().getId());
		detailsDrawer.setContent(createDetails(user));
		DetailsDrawerFooter footer = new DetailsDrawerFooter();
		detailsDrawer.setFooter(footer);
		footer.addCancelListener(e -> detailsDrawer.hide());
		footer.addSaveListener(e -> {
			boolean valid = userForm.updateExistingUser();
			if (valid) {
				detailsDrawer.hide();
			}
		});
		detailsDrawer.show();
	}

	private VerticalLayout createDetails(AppUser user) {
		this.userForm = new UserForm(emailDAO, this, user);
		return userForm;
	}

	private void initSearchBar() {
		searchField = new TextField();
		searchField.setValueChangeMode(ValueChangeMode.EAGER);
		searchField.addValueChangeListener(e -> dataProvider.refreshAll());
		dataProvider.addFilter(user -> {
			String searchTerm = searchField.getValue().trim();
			//boolean matchesActiveStatus = user.isEnabled()==!this.cbToggleActive.getValue();
			boolean matchesActiveStatus = !this.cbToggleActive.getValue() || (cbToggleActive.getValue() && user.isEnabled());
			if (searchTerm.isEmpty()) {
				return matchesActiveStatus;
				//return true;
			}
			boolean matchesUsername = matchesTerm(user.getUsername(), searchTerm);
			boolean matchesFullName = matchesTerm(user.getFullName(), searchTerm);
			boolean matchesEmail = matchesTerm(user.getEmail(), searchTerm);
			return (matchesFullName || matchesEmail || matchesUsername) && matchesActiveStatus;
		});
		
		cbToggleActive.addValueChangeListener(e-> {
			dataProvider.refreshAll();
		});
		
	}

	private boolean matchesTerm(String target, String searchTerm) {
		return target.toLowerCase().contains(searchTerm.toLowerCase());
	}

	/**
	 * Initialize the two grids.
	 */
	private /* Grid<AppUser> */ VerticalLayout createGrid() {
		initSearchBar();
		userGrid = new Grid<AppUser>();
		userGrid.addColumn(a -> a.getFirstName() + " " + a.getLastName()).setHeader("Name").setSortable(true);
		userGrid.addColumn(AppUser::getUsername).setHeader("Username").setWidth("200px").setFlexGrow(0)
				.setSortable(true);
		userGrid.addColumn(AppUser::getEmail).setHeader("Email").setHeader("Email").setFlexGrow(1).setSortable(true);
		userGrid.addColumn(a -> a.getRole().getFriendly()).setHeader("Role").setWidth("100px").setFlexGrow(0)
				.setSortable(true);
		userGrid.addColumn(a->a.isEnabled() ? "Yes" : "No").setHeader("Active").setSortable(true).setWidth("100px").setFlexGrow(0);
		userGrid.addColumn(a -> a.getOrganization().getDescription()).setHeader("Organization").setFlexGrow(1).setSortable(true);
		
		
		dataProvider.setSortOrder(AppUser::getUsername, SortDirection.ASCENDING);
		userGrid.setDataProvider(dataProvider);
		userGrid.addSelectionListener(event -> event.getFirstSelectedItem().ifPresent(this::showDetails));

		userGrid.setHeightFull();
		searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
		HorizontalLayout top = new HorizontalLayout(this.searchField, this.btnCreateUser);
		VerticalLayout layout = new VerticalLayout(top, cbToggleActive, userGrid);
		searchField.setWidth("87%");
		top.setWidth("100%");
		return layout;
	}

	public AppUserDAO getAppUserDAO() {
		return this.appUserDAO;
	}

	public Map<Long, Organization> getOrganizations() {
		return this.organizationDAO.getOrganizations();
	}

	public Map<Long, AppUser> getUserMap() {
		return userMap;
	}

	public Grid<AppUser> getUserGrid() {
		return userGrid;
	}

	public KeyDAO getKeyDAO() {
		return keyDAO;
	}

}
