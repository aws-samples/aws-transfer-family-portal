// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.application.dao.AppUserDAO;
import com.example.application.dao.CloudWatchService;
import com.example.application.data.Role;
import com.example.application.ui.components.navigation.bar.AppBar;
import com.example.application.ui.components.navigation.drawer.NaviDrawer;
import com.example.application.ui.components.navigation.drawer.NaviMenu;
import com.example.application.ui.components.navigation.drawer.NaviItem;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;
import com.example.application.ui.util.UIUtils;
import com.example.application.ui.util.css.Overflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@CssImport(value = "./styles/components/charts.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value = "./styles/components/floating-action-button.css", themeFor = "vaadin-button")
@CssImport(value = "./styles/components/grid.css", themeFor = "vaadin-grid")
@CssImport("./styles/lumo/border-radius.css")
@CssImport("./styles/lumo/icon-size.css")
@CssImport("./styles/lumo/margin.css")
@CssImport("./styles/lumo/padding.css")
@CssImport("./styles/lumo/shadow.css")
@CssImport("./styles/lumo/spacing.css")
@CssImport("./styles/lumo/typography.css")
@CssImport("./styles/misc/box-shadow-borders.css")
@CssImport("./styles/components/navi-item.css")
@CssImport(value = "./styles/styles.css", include = "lumo-badge")
@JsModule("@vaadin/vaadin-lumo-styles/badge")

public class MainLayout extends FlexBoxLayout
		implements RouterLayout, AfterNavigationObserver {
	private static final Logger log = LoggerFactory.getLogger(MainLayout.class);
	private static final String CLASS_NAME = "root";
	private FlexBoxLayout row;
	private NaviDrawer naviDrawer;
	private FlexBoxLayout column;
	private Div appHeaderInner;
	private FlexBoxLayout viewContainer;
	private AppBar appBar;
	private Role role;
	
	public MainLayout(@Autowired AppUserDAO appUserDAO, @Autowired CloudWatchService cloudWatchService) {
		VaadinSession.getCurrent()
		.setErrorHandler((ErrorHandler) errorEvent -> {
			log.error("Uncaught UI exception",
					errorEvent.getThrowable());
			Notification.show(
					"We are sorry, but an internal error occurred");
		});
		String roleDescription = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .findFirst().get()
                .getAuthority();
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = ((UserDetails) principal).getUsername();
        role = Role.getAppRole(roleDescription);
        addClassName(CLASS_NAME);
		setFlexDirection(FlexDirection.COLUMN);
		setSizeFull();

		// Initialise the UI building blocks
		initStructure();

		// Populate the navigation drawer
		initNaviItems();

		// Configure the headers and footers (optional)
		initHeadersAndFooters();
    		cloudWatchService.init(username);
        	log.info("CloudWatch initialized.");
    }

	

	/**
	 * Initialise the required components and containers.
	 */
	private void initStructure() {
		naviDrawer = new NaviDrawer();

		viewContainer = new FlexBoxLayout();
		viewContainer.addClassName(CLASS_NAME + "__view-container");
		viewContainer.setOverflow(Overflow.HIDDEN);

		column = new FlexBoxLayout(viewContainer);
		column.addClassName(CLASS_NAME + "__column");
		column.setFlexDirection(FlexDirection.COLUMN);
		column.setFlexGrow(1, viewContainer);
		column.setOverflow(Overflow.HIDDEN);

		row = new FlexBoxLayout(naviDrawer, column);
		row.addClassName(CLASS_NAME + "__row");
		row.setFlexGrow(1, column);
		row.setOverflow(Overflow.HIDDEN);
		add(row);
		setFlexGrow(1, row);
	}

	private void logout() {
		SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
		logoutHandler.logout(
			VaadinServletRequest.getCurrent().getHttpServletRequest(), null,
		null);
	}
	
	/**
	 * Initialise the navigation items.
	 */
	private void initNaviItems() {
		NaviMenu menu = naviDrawer.getMenu();
		
		if (this.role == Role.ROLE_ADMIN) {
			menu.addNaviItem(VaadinIcon.HOME, "Home", Home.class);
			menu.addNaviItem(VaadinIcon.USER, "Users", UserAdminView.class);
			menu.addNaviItem(VaadinIcon.OFFICE, "Organizations", OrganizationView.class);
			menu.addNaviItem(VaadinIcon.ENVELOPE, "Email", EmailView.class);
			menu.addNaviItem(VaadinIcon.UPLOAD, "File Transfer", FileTransferView.class);
			menu.addNaviItem(VaadinIcon.KEY, "Keys", KeyView.class);
			menu.addNaviItem(VaadinIcon.LOCK, "Reset Password", ChangePassword.class);
			NaviItem logoutNaviItem = menu.addNaviItem(VaadinIcon.EXIT, "Logout", LoginView.class);
			logoutNaviItem.addClickListener(e->logout());
		}
		else {
			menu.addNaviItem(VaadinIcon.UPLOAD, "File Transfer", FileTransferView.class);
			menu.addNaviItem(VaadinIcon.KEY, "Keys", KeyView.class);
			menu.addNaviItem(VaadinIcon.LOCK, "Reset Password", ChangePassword.class);
			NaviItem logoutNaviItem = menu.addNaviItem(VaadinIcon.EXIT, "Logout", LoginView.class);
			logoutNaviItem.addClickListener(e->logout());
			
		}
		        }

	/**
	 * Configure the app's inner and outer headers and footers.
	 */
	private void initHeadersAndFooters() {
			appBar = new AppBar("");
			UIUtils.setTheme(Lumo.DARK, appBar);
			setAppHeaderInner(appBar);
	}

	

	private void setAppHeaderInner(Component... components) {
		if (appHeaderInner == null) {
			appHeaderInner = new Div();
			appHeaderInner.addClassName("app-header-inner");
			column.getElement().insertChild(0, appHeaderInner.getElement());
		}
		appHeaderInner.removeAll();
		appHeaderInner.add(components);
	}


	@Override
	public void showRouterLayoutContent(HasElement content) {
		this.viewContainer.getElement().appendChild(content.getElement());
	}

	public NaviDrawer getNaviDrawer() {
		return naviDrawer;
	}

	public static MainLayout get() {
		return (MainLayout) UI.getCurrent().getChildren()
				.filter(component -> component.getClass() == MainLayout.class)
				.findFirst().get();
	}

	public AppBar getAppBar() {
		return appBar;
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event) {
		NaviItem active = getActiveItem(event);
		if (active != null) {
			getAppBar().setTitle(active.getText());
			System.out.println("After navigation: " + active.getText());
		}
	}

	
	private NaviItem getActiveItem(AfterNavigationEvent e) {
		for (NaviItem item : naviDrawer.getMenu().getNaviItems()) {
			if (item.isHighlighted(e)) {
				return item;
			}
		}
		return null;
	}




}
