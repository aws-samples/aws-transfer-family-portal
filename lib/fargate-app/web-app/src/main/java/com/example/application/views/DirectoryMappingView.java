package com.example.application.views;

import com.example.application.Toolkit;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.example.application.data.*;
import com.example.application.ui.util.UIUtils;

public class DirectoryMappingView extends Dialog {
	private static String ENTRY_REGEX;
	private static String TARGET_REGEX;
	private static String ENTRY_VALIDATION_MESSAGE;
	private static String TARGET_VALIDATION_MESSAGE;

	private TextField tfEntry = new TextField("Entry");
	private TextField tfTarget = new TextField("Target");
	private Button btnDelete = new Button();
	private Checkbox cbWrite = new Checkbox("Write");
	private Binder<DirectoryMapping> binder = new Binder<>();
	private FormLayout form = new FormLayout();
	private Grid<DirectoryMapping> grid = new Grid<>();
	private AppUser user;
	private ListDataProvider<DirectoryMapping> dataProvider;
	private UserAdminView userAdminView;
	private Label lbl = new Label("Target mappings are relative to the bucket " + Toolkit.S3_BUCKET);
	private Button btnSave = new Button("Save");
	// private Button btnCancel = new Button("Cancel");
	private boolean createNew;
	private DirectoryMapping selectedMapping;
	private VerticalLayout right;
	private Button btnClose = new Button("Close");
	private Button btnAdd = new Button();
	private Button btnAddDefault = new Button("Add Default Mapping");
	static {
		ENTRY_REGEX = "(/[a-zA-Z0-9._$-]{1,}){1,}[a-zA-Z0-9]$";
		// TARGET_REGEX = "^/"+Toolkit.S3_BUCKET +
		// "(/[a-zA-Z0-9._$-]{1,}){1,}[a-zA-Z0-9]$";
		TARGET_REGEX = "(/[a-zA-Z0-9._$-]{1,}){1,}[a-zA-Z0-9]$";
		ENTRY_VALIDATION_MESSAGE = "Should have a leading but not trailing backslash.  Acceptable chars include '.', '_', '$', and '-'";
		TARGET_VALIDATION_MESSAGE = "Must have the prefix '/" + Toolkit.S3_BUCKET
				+ "/' and no trailing backslash.  Acceptable chars include '.', '_', '$', and '-'";
	}

	public DirectoryMappingView(AppUser user, UserAdminView userAdminView) {
		this.user = user;
		this.userAdminView = userAdminView;
		initLayout();
		initGrid();
		initListeners();
		initBinder();
	}

	private void initLayout() {
		HorizontalLayout layout = new HorizontalLayout();
		HorizontalLayout buttonContainer = new HorizontalLayout(btnAddDefault, btnAdd/* , btnCancel */);
		VerticalLayout left = new VerticalLayout(grid, buttonContainer, lbl);
		btnAdd.setIcon(VaadinIcon.PLUS.create());
		VerticalLayout layoutContainer = new VerticalLayout(new H3(this.user.getFullName() + "'s Directory Mappings"),
				layout, btnClose);
		form.add(tfEntry, tfTarget, cbWrite);
		right = new VerticalLayout(form, btnSave);
		right.setVisible(false);

		add(layoutContainer);
		layout.setSizeFull();
		btnDelete.setIcon(VaadinIcon.TRASH.create());
		tfEntry.setValue("/");
		tfTarget.setValue("/");
		tfTarget.setWidth("500px");
		btnAdd.setIcon(VaadinIcon.PLUS.create());
		btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		layout.add(left, right);
		left.setWidth("65%");
		right.setWidth("30%");
		left.setHeightFull();
		right.setHeightFull();
		btnClose.setIcon(VaadinIcon.EXIT.create());
		layoutContainer.setHorizontalComponentAlignment(Alignment.CENTER, btnClose);
		setSizeFull();
	}

	private void initBinder() {
		binder.forField(tfEntry).withValidator(new RegexpValidator(ENTRY_VALIDATION_MESSAGE, ENTRY_REGEX))
				.bind(DirectoryMapping::getEntry, DirectoryMapping::setEntry);
		binder.forField(tfTarget).withValidator(new RegexpValidator(TARGET_VALIDATION_MESSAGE, TARGET_REGEX))
				.bind(DirectoryMapping::getRelativePath, DirectoryMapping::setAbsolutePath);
		binder.forField(cbWrite).bind(DirectoryMapping::isWrite, DirectoryMapping::setWrite);
	}

	private Component renderBoolean(boolean allow) {
		return allow ? UIUtils.createPrimaryIcon(VaadinIcon.CHECK) : UIUtils.createDisabledIcon(VaadinIcon.CLOSE);
	}

	private void initListeners() {

		btnClose.addClickListener(e -> close());

		btnAddDefault.addClickListener(e -> {
			if (user.getDirectoryMappings().values().stream().filter(dm -> dm.getEntry().equals("/personal"))
					.count() > 0) {
				Notification n = Notification.show("Default mapping already defined for this user.", 2500,
						Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				return;
			}
			right.setVisible(true);

			selectedMapping = new DirectoryMapping(user.getId());
			selectedMapping.setEntry("/personal");
			selectedMapping.setTarget("/" + Toolkit.S3_BUCKET + "/" + user.getUsername());
			binder.readBean(selectedMapping);
			createNew = true;
			btnSave.setEnabled(true);
		});

		btnAdd.addClickListener(e -> {
			right.setVisible(true);
			selectedMapping = new DirectoryMapping(user.getId());
			binder.readBean(selectedMapping);
			createNew = true;
			btnSave.setEnabled(true);
		});

		// btnCancel.addClickListener(e-> {});
		btnSave.addClickListener(e -> {
			/* Validation */

			/* Does a mapping with this entry already exist? Avoid collisions */
			//if (createNew) {
				if ((createNew && entryAlreadyExists(tfEntry.getValue()))
						|| (!createNew && entryAlreadyExists(tfEntry.getValue(), selectedMapping.getDirectoryMappingId()))) {
					Notification n = Notification.show(
							"A mapping with entry " + tfEntry.getValue() + " already exists.", 2500, Position.MIDDLE);
					n.addThemeVariants(NotificationVariant.LUMO_ERROR);
					return;
				}
			//}
				
				if ((createNew && targetAlreadyExists(tfTarget.getValue()))
						|| (!createNew && targetAlreadyExists(tfTarget.getValue(), selectedMapping.getDirectoryMappingId()))) {
					Notification n = Notification.show(
							"A mapping with target " + tfTarget.getValue() + " already exists.", 2500, Position.MIDDLE);
					n.addThemeVariants(NotificationVariant.LUMO_ERROR);
					return;
				}
				

			boolean valid = binder.writeBeanIfValid(selectedMapping);
			if (!valid) {
				Notification n = Notification.show("Please check validation errors", 2500, Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				return;
			}
			if (createNew) {
				userAdminView.getAppUserDAO().insertToDb(selectedMapping);
				selectedMapping.createS3FolderIfDoesNotExist();
				user.getDirectoryMappings().put(selectedMapping.getDirectoryMappingId(), selectedMapping);
				dataProvider.refreshAll();
				Notification n = Notification.show("New mapping created successfully.", 2500, Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				btnSave.setEnabled(false);
			} else {
				userAdminView.getAppUserDAO().update(selectedMapping);
				selectedMapping.createS3FolderIfDoesNotExist();
				dataProvider.refreshAll();
				Notification n = Notification.show("Mapping updated successfully.", 2500, Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				btnSave.setEnabled(false);
			}
		});
	}

	
	private boolean targetAlreadyExists(String target) {
		System.out.println("Testing for existence of target=" + target);
		user.getDirectoryMappings().values()
		.stream()
		.forEach(dm->System.out.println(dm.getTarget().replace("/" + Toolkit.S3_BUCKET, "")));
		
		return user.getDirectoryMappings().values()
				.stream()
				.filter(dm->dm.getTarget().replace("/"+Toolkit.S3_BUCKET, "").equals(target)).count() > 0;
	}
	
	private boolean targetAlreadyExists(String target, long directoryMappingId) {
		return user.getDirectoryMappings().values()
				.stream()
				.filter(dm -> dm.getTarget().replace("/" + Toolkit.S3_BUCKET, "").equals(target) && dm.getDirectoryMappingId() != directoryMappingId)
				.count() > 0;

	}
	
	private boolean entryAlreadyExists(String entry) {
		return user.getDirectoryMappings().values()
				.stream()
				.filter(dm -> dm.getEntry().equals(entry)).count() > 0;

	}

	private boolean entryAlreadyExists(String entry, long directoryMappingId) {
		return user.getDirectoryMappings().values()
				.stream()
				.filter(dm -> dm.getEntry().equals(entry) && dm.getDirectoryMappingId() != directoryMappingId)
				.count() > 0;

	}

	private void initGrid() {
		grid.setWidthFull();
		grid.addColumn(dm -> dm.getEntry()).setHeader("Entry").setFlexGrow(1);
		grid.addColumn(dm -> dm.getRelativePath()).setHeader("Target").setFlexGrow(2);
		grid.addComponentColumn(dm -> renderBoolean(dm.isWrite())).setHeader("Write");
		grid.addComponentColumn(a -> getDeletionButton(a)).setHeader("Remove").setWidth("125px").setFlexGrow(0);
		grid.addItemClickListener(e -> {
			right.setVisible(true);
			selectedMapping = e.getItem();
			binder.readBean(selectedMapping);
			createNew = false;
			btnSave.setEnabled(true);
		});
		dataProvider = (ListDataProvider<DirectoryMapping>) DataProvider
				.ofCollection(user.getDirectoryMappings().values());
		grid.setDataProvider(dataProvider);
	}

	private Button getDeletionButton(DirectoryMapping directoryMapping) {
		Button btn = new Button();
		btn.setIcon(VaadinIcon.TRASH.create());
		btn.addClickListener(e -> launchConfirmDelete(directoryMapping));
		return btn;
	}

	private void launchConfirmDelete(DirectoryMapping directoryMapping) {
		Dialog dlg = new Dialog();
		VerticalLayout layout = new VerticalLayout();
		HorizontalLayout buttonContainer = new HorizontalLayout();
		Button btnConfirm = new Button("Confirm");
		Button btnCancel = new Button("Cancel");
		String confirmation = "<p>Are you sure you want to delete the directory mapping below?<br><br>";
		confirmation += "Entry: " + directoryMapping.getEntry();
		confirmation += "<br>Target: " + directoryMapping.getTarget() + "</p>";
		Html html = new Html(confirmation);
		dlg.add(layout);
		layout.add(html, buttonContainer);
		buttonContainer.add(btnConfirm, btnCancel);
		btnConfirm.addClickListener(e -> {
			userAdminView.getAppUserDAO().deleteUserDirectoryMapping(user, directoryMapping.getDirectoryMappingId());
			dataProvider.refreshAll();
			dlg.close();
			Notification n = Notification.show("Directory mapping successfully deleted.", 2000, Position.MIDDLE);
			n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		});
		btnCancel.addClickListener(e -> dlg.close());
		dlg.setModal(true);
		btnConfirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dlg.open();
	}
}
