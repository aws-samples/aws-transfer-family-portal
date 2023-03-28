package com.example.application.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteConfiguration;
import com.example.application.Toolkit;
import com.example.application.dao.AppUserDAO;
import com.example.application.dao.CloudWatchService;
import com.example.application.data.AppUser;
import com.example.application.data.Role;
import com.example.application.views.MainLayout;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;

/**
 * The main view is a top-level placeholder for other views.
 */
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
@CssImport(value = "./styles/styles.css", include = "lumo-badge")
@PWA(name = "My App", shortName = "My App", enableInstallPrompt = false)
@Theme(themeFolder = "myapp")
@PageTitle("Main")
@Push
public class MainLayout extends AppLayout implements BeforeEnterObserver {
    private final static Logger logger = LogManager.getLogger(MainLayout.class);
    private final Tabs menu;
    private H1 viewTitle;
    private Role role;
    private AppUser user;
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    public MainLayout(@Autowired AppUserDAO appUserDAO, @Autowired CloudWatchService cloudWatchService) {
        String roleDescription = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .findFirst().get()
                .getAuthority();
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = ((UserDetails) principal).getUsername();
        this.user = appUserDAO.getAppUser(username);
        role = Role.getAppRole(roleDescription);
        try {
            Class.forName(Toolkit.mysqldriver);
        } catch (ClassNotFoundException e) {
            logger.error("Can't find my sql driver class: " + Toolkit.mysqldriver);
        }
        setPrimarySection(Section.DRAWER);
        addToNavbar(true, createHeaderContent());
        menu = createMenu();
        addToDrawer(createDrawerContent(menu));
        logger.info("Initializing CloudWatch stream.  Make this a separate thread if it's too slow.");
        cloudWatchService.init(username);
        logger.info("CloudWatch initialized.");
    }

    private Component createHeaderContent() {
        HorizontalLayout layout = new HorizontalLayout();
        Avatar avatar = new Avatar(user.getFullName());
        Button logout = new Button("Log out", e -> UI.getCurrent().getPage().setLocation("/logout"));
        layout.setClassName("sidemenu-header");
        layout.getThemeList().set("dark", true);
        layout.setWidthFull();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        viewTitle = new H1("File Transfer Portal");
        avatar.addClassNames("ms-auto", "me-m");
        layout.add(new DrawerToggle());
        layout.add(viewTitle);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Label lbl = new Label(username + " ");
        layout.add(avatar);
        layout.add(lbl);
        layout.add(logout);
        return layout;
    }

    public void setPageTitle(String title) {
        viewTitle.setText(title);
    }

    private Component createDrawerContent(Tabs menu) {
        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("sidemenu-menu");
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getThemeList().set("spacing-s", true);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        HorizontalLayout logoLayout = new HorizontalLayout();
        logoLayout.setId("logo");
        logoLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.add(logoLayout, menu);
        return layout;
    }

    private Tabs createMenu() {
        final Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL);
        tabs.setId("tabs");
        for (Tab menuTab : createMenuItems()) {
            tabs.add(menuTab);
        }
        return tabs;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        RouteConfiguration configuration = RouteConfiguration.forSessionScope();
        if (configuration.isRouteRegistered(this.getContent().getClass())) {
            String target = configuration.getUrl(this.getContent().getClass());
            Optional<Component> tabToSelect = menu.getChildren().filter(tab -> {
                Component child = tab.getChildren().findFirst().get();
                return child instanceof RouterLink && ((RouterLink) child).getHref().equals(target);
            }).findFirst();
            tabToSelect.ifPresent(tab -> menu.setSelectedTab((Tab) tab));
        } else {
            menu.setSelectedTab(null);
        }
    }

    private List<Tab> createMenuItems() {
        MenuItemInfo[] menuItems = null;
        if (this.role == Role.ROLE_ADMIN) {
            menuItems = new MenuItemInfo[] {
                    new MenuItemInfo("Users", "la la-cloud-upload", UserAdminView.class),
                    new MenuItemInfo("Organizations", "la la-building", OrganizationView.class),
                    new MenuItemInfo("Email", "la la-envelope-o", EmailView.class),
                    new MenuItemInfo("Keys", "la la-key", KeyView.class),
                    new MenuItemInfo("Password Reset", "la la-user-secret", ChangePassword.class),
                    new MenuItemInfo("Transfer Files", "la la-exchange", FileTransferView.class)// ,
                    // new MenuItemInfo("About", "la la-exchange", AboutView.class)
            };

        }

        else {
            menuItems = new MenuItemInfo[] {
                    new MenuItemInfo("Keys", "la la-key", KeyView.class),
                    new MenuItemInfo("Password Reset", "la la-user-secret", ChangePassword.class),
                    new MenuItemInfo("Transfer Files", "la la-exchange", FileTransferView.class)
            };
        }

        List<Tab> tabs = new ArrayList<>();
        for (MenuItemInfo menuItemInfo : menuItems) {
            tabs.add(createTab(menuItemInfo));
        }
        return tabs;
    }

    private static Tab createTab(MenuItemInfo menuItemInfo) {
        Tab tab = new Tab();
        RouterLink link = new RouterLink();
        link.setRoute(menuItemInfo.getView());

        Span iconElement = new Span();
        iconElement.addClassNames("text-l", "pr-s");
        if (!menuItemInfo.getIconClass().isEmpty()) {
            iconElement.addClassNames(menuItemInfo.getIconClass());
        }
        link.add(iconElement, new Text(menuItemInfo.getText()));
        tab.add(link);

        return tab;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }

    public static class MenuItemInfo {

        private String text;
        private String iconClass;
        private Class<? extends Component> view;

        public MenuItemInfo(String text, String iconClass, Class<? extends Component> view) {
            this.text = text;
            this.iconClass = iconClass;
            this.view = view;
        }

        public String getText() {
            return text;
        }

        public String getIconClass() {
            return iconClass;
        }

        public Class<? extends Component> getView() {
            return view;
        }
    }
}
