// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.views;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.firitin.components.DynamicFileDownloader;
import org.vaadin.olli.FileDownloadWrapper;
import com.example.application.FtpClient;
import com.example.application.Toolkit;
import com.example.application.UploadBuffer;
import com.example.application.dao.AppUserDAO;
import com.example.application.dao.CloudWatchService;
import com.example.application.data.AppUser;
import com.example.application.transfer.TransferEngine;
import com.example.application.transfer.TransferEngineFtp;
import com.example.application.transfer.TransferEngineS3;
import com.vaadin.componentfactory.explorer.ExplorerTreeGrid;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;

import jakarta.annotation.security.PermitAll;

import com.example.application.data.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@PageTitle("Transfer")
@Route(value = "Transfer", layout = MainLayout.class)
//@RouteAlias(value = "", layout = MainLayout.class)
@PermitAll
public class FileTransferView extends VerticalLayout {
	private final static Logger logger = LogManager.getLogger(FileTransferView.class);
	private Grid<TreeItem> s3ObjectGrid = new Grid<>();
	private FileDownloadWrapper multiDownloadButtonWrapper;
	private Upload upload;
	private Button btnMultiDownload = new Button("Download Zip File");
	private ExplorerTreeGrid<TreeItem> treeGrid;
	private AppUser user;
	private TreeItem selectedFolder = null;
	private Map<String, S3Object> s3ObjectLibrary = new HashMap<>();
	private Map<Long, TreeItem> treeItems;
	private Label lblPermissions = new Label("");
	private TransferEngine transferEngine;

	@Autowired
	public FileTransferView(@Autowired CloudWatchService cloudWatchService, @Autowired AppUserDAO appUserDAO,
			@Autowired FtpClient ftpClient) {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = null;
		username = ((UserDetails) principal).getUsername();
		user = appUserDAO.getAppUser(username);
		initDownloadButtonWrappers();
		initUpload();
		initTreeGrid();
		initFileGrid();
		initLayout();
		if (Toolkit.CLIENT_MODE.equals("FTP")) {
			transferEngine = new TransferEngineFtp(ftpClient);
		}
		if (Toolkit.CLIENT_MODE.equals("S3")) {
			transferEngine = new TransferEngineS3(cloudWatchService);
		}
	}

	private void initUpload() {
		UploadBuffer buffer = new UploadBuffer();
		upload = new Upload(buffer);
		upload.setDropAllowed(false);
		upload.setVisible(false);
		
		/*
		String fileName = buffer.getFileName();
		String s3ParentFolder = selectedFolder.getS3ObjectKey();
		boolean needsBackslash = s3ParentFolder.charAt(s3ParentFolder.length() - 1) != '/';
		String key = s3ParentFolder + (needsBackslash ? "/" : "") + fileName;
		*/
		
		
		upload.addSucceededListener(event -> {
			
			String fileName = buffer.getFileName();
			String s3ParentFolder = selectedFolder.getS3ObjectKey();
			boolean needsBackslash = s3ParentFolder.charAt(s3ParentFolder.length() - 1) != '/';
			String key = s3ParentFolder + (needsBackslash ? "/" : "") + fileName;
			
			/*Validation-- does the key exist?*/
			
			if (Toolkit.objectExists(key)) {
				Dialog dlg = new Dialog();
				Button btnConfirm = new Button("Overwrite");
				Button btnCancel = new Button("Cancel");
				VerticalLayout layout = new VerticalLayout();
				HorizontalLayout buttonContainer = new HorizontalLayout();
				dlg.add(layout);
				buttonContainer.add(btnConfirm, btnCancel);
				btnConfirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
				btnCancel.addClickListener(e->dlg.close());
				btnConfirm.addClickListener(e-> {
					upload(event,buffer);
					dlg.close();
				});
				
				Label lbl = new Label("A file with this name already exists.  Do you want to overwrite it?");
				layout.add(lbl,buttonContainer);
				dlg.open();
			}
			
			else {
				upload(event, buffer);
			}	
		
		});
		
	}


	
	private void upload(SucceededEvent event, UploadBuffer buffer) {
		String key = this.transferEngine.upload(selectedFolder, buffer);
		ListObjectsRequest lor = ListObjectsRequest.builder()
				.bucket(Toolkit.S3_BUCKET)
				.prefix(key)
				.build();

		S3Client s3 = Toolkit.getS3Client();
		ListObjectsResponse listObjectsResponse = s3.listObjects(lor);

		if (listObjectsResponse.contents().size() != 1) {
			logger.info("I messed up.  This should just return the single object just uploaded");
		}

		else {
			S3Object newObject = listObjectsResponse.contents().stream().findFirst().get();
			TreeItem containingTree = treeItems.get(selectedFolder.getDirectoryMapping().getDirectoryMappingId());
			containingTree.createDirectoryStructure(newObject, selectedFolder.getDirectoryMapping().getTarget());
			this.s3ObjectLibrary.put(newObject.key(), newObject);
			this.updateFileGrid();
		}
	}

	private void showLargeFileHints() {
		Dialog dlg = new Dialog();
		Button btnOk = new Button("Close");
		VerticalLayout layout = new VerticalLayout();
		String s = "<ul>";
		s += "<li> The fastest way to transfer large files is using a client like Filezilla, WinSCP, or Cyberduck. Use the endpoint provided by your administrator,  along with your credentials to connect.";
		s += "<li> When using this app, there may be a lag between when the file is uploaded and when it appears listed in the folder, depending on the file size.";
		s += "</ul>";
		H2 title = new H2("Handling Large Files");
		Html html = new Html(s);
		btnOk.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		btnOk.addClickListener(e -> dlg.close());
		layout.add(title, html, btnOk);
		dlg.add(layout);
		dlg.setModal(true);
		dlg.open();
	}

	private String getPermissionsDescription() {
		DirectoryMapping dm = user.getDirectoryMappings()
				.get(selectedFolder.getDirectoryMapping().getDirectoryMappingId());
		String permissions = "Permissions: ";
		List<String> permissionsList = new LinkedList<String>();
		permissionsList.add("read");
		if (dm.isWrite())
			permissionsList.add("write");
		//if (dm.isDelete())
		//	permissionsList.add("delete");
		//if (dm.isWrite() && dm.isDelete())
		//	permissionsList.add("rename");
		permissions += String.join(", ", permissionsList);
		return permissions;
	}

	private void initLayout() {
		HorizontalLayout hl = new HorizontalLayout();
		HorizontalLayout buttonContainer = new HorizontalLayout();
		VerticalLayout rightLayout = new VerticalLayout();
		VerticalLayout leftLayout = new VerticalLayout();
		HorizontalLayout permissionsContainer = new HorizontalLayout(lblPermissions);
		Button btnLarge = new Button("Large File Tips");
		btnLarge.addClickListener(e -> this.showLargeFileHints());
		leftLayout.add(treeGrid, permissionsContainer);
		buttonContainer.add(multiDownloadButtonWrapper, upload, btnLarge);
		rightLayout.add(s3ObjectGrid, buttonContainer);
		hl.add(leftLayout, rightLayout);
		treeGrid.setSizeFull();
		leftLayout.setHeight("90%");
		rightLayout.setHeight("90%");
		hl.setSizeFull();
		s3ObjectGrid.setWidth("100%");
		s3ObjectGrid.setHeight("90%");
		leftLayout.setWidth("30%");
		rightLayout.setWidth("60%");
		treeGrid.setWidth("300px");
		setSizeFull();
		add(new H2("File Transfer"), hl);
	}

	private void initFileGrid() {
		btnMultiDownload.setEnabled(false);
		s3ObjectGrid.setSelectionMode(SelectionMode.MULTI);
		s3ObjectGrid.addComponentColumn(obj -> download(obj)).setHeader("Download").setFlexGrow(1)
				.setComparator(e -> e.getFilename().toLowerCase()).setSortable(true);
		s3ObjectGrid.addColumn(obj -> convertBytesToKb(obj) + " KB").setHeader("Size").setWidth("150px").setFlexGrow(0)
				.setSortable(true);
		s3ObjectGrid.addComponentColumn(obj -> delete(obj)).setHeader("Delete").setWidth("100px").setFlexGrow(0);
		s3ObjectGrid.addComponentColumn(obj -> rename(obj)).setHeader("Rename").setWidth("100px").setFlexGrow(0);
		s3ObjectGrid.addSelectionListener(e -> {
			int selectionCount = s3ObjectGrid.getSelectedItems().size();
			btnMultiDownload.setEnabled(selectionCount > 1);
		});
	}

	private void initTreeGrid() {
		treeGrid = new ExplorerTreeGrid<TreeItem>();
		treeGrid.addHierarchyColumn(x -> x.getGridLabel()).setHeader("Folders");

		treeItems = getTreeItems();
		treeGrid.setItems(treeItems.values(), x -> x.getSubTreeItems(true));
		treeGrid.addExpandListener(e -> {
			TreeItem item = e.getItems().stream().findFirst().get();
			reactToTreeSelection(item);
		});

		treeGrid.addCollapseListener(e -> {
			TreeItem item = e.getItems().stream().findFirst().get();
			reactToTreeSelection(item);
		});

		treeGrid.addItemClickListener(e -> {
			reactToTreeSelection(e.getItem());
		});
	}

	private void reactToTreeSelection(TreeItem item) {
		selectedFolder = item;
		boolean hasWritePermissions = selectedFolder.getDirectoryMapping().isWrite();
		lblPermissions.setText(getPermissionsDescription());
		upload.setVisible(hasWritePermissions);
		treeGrid.select(item);
		updateFileGrid();

	}

	private void initDownloadButtonWrappers() {
		this.multiDownloadButtonWrapper = new FileDownloadWrapper(
				new StreamResource("compressed.zip", () -> {
					ByteArrayInputStream stream = null;
					try {
						int count = s3ObjectGrid.getSelectedItems().size();
						TreeItem[] items = new TreeItem[count];
						long[] sizes = new long[count];
						int i = 0;
						for (TreeItem item : this.s3ObjectGrid.getSelectedItems()) {
							items[i] = item;
							sizes[i] = this.s3ObjectLibrary.get(item.getS3ObjectKey()).size();
							i++;
						}
						stream = new ByteArrayInputStream(this.transferEngine.zipFiles(items, sizes));
					} catch (IOException io) {
						logger.error(io.getMessage());
					}
					return stream;
				}));
		multiDownloadButtonWrapper.wrapComponent(btnMultiDownload);
	}

	/**
	 * Supports the grid column that allows single-file downloading.
	 * 
	 * @param s3Object
	 * @return
	 */
	private DynamicFileDownloader download(TreeItem s3Object) {
		@SuppressWarnings("serial")
		DynamicFileDownloader downloadButton = new DynamicFileDownloader(s3Object.getFilename(), s3Object.getFilename(),
				outputStream -> {

					try {
						InputStream is = null;
						long size = this.s3ObjectLibrary.get(s3Object.getS3ObjectKey()).size();
						is = this.transferEngine.getInputStream(s3Object, size);
						byte[] buf = new byte[8192];
						int length;
						while ((length = is.read(buf)) > 0) {
							outputStream.write(buf, 0, length);
						}
					} catch (IOException ex) {
						logger.error(ex.getMessage());
					}
				}) {
			@Override
			protected String getFileName(VaadinSession session, VaadinRequest request) {
				return s3Object.getFilename();
			}
		};
		return downloadButton;
	}

	private long convertBytesToKb(TreeItem item) {
		if (!s3ObjectLibrary.containsKey(item.getS3ObjectKey())) {
			logger.info("Can't find key: " + item.getS3ObjectKey());
			return 0L;
		} else {
			long sizeInBytes = this.s3ObjectLibrary.get(item.getS3ObjectKey()).size();
			return sizeInBytes / 1024;
		}
	}

	private Button delete(TreeItem s3Object) {
		Button btnDelete = new Button();
		btnDelete.setIcon(VaadinIcon.TRASH.create());
		boolean hasDeletePermissions = selectedFolder.getDirectoryMapping().isWrite();
		btnDelete.setEnabled(hasDeletePermissions);
		btnDelete.addClickListener(e -> {
			Dialog dlg = new Dialog();
			VerticalLayout layout = new VerticalLayout();
			Button btnConfirm = new Button("Confirm");
			Button btnCancel = new Button("Cancel");
			btnConfirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			HorizontalLayout buttonContainer = new HorizontalLayout(btnConfirm, btnCancel);
			Label lbl = new Label("Are you sure you want to delete " + s3Object.getFilename() + "?");

			btnCancel.addClickListener(f -> dlg.close());
			btnConfirm.addClickListener(f -> {
				this.transferEngine.delete(s3Object);
				s3ObjectLibrary.remove(s3Object.getS3ObjectKey());
				TreeItem containingTree = treeItems.get(selectedFolder.getDirectoryMapping().getDirectoryMappingId());
				containingTree.removeChild(s3Object.getS3ObjectKey());
				this.s3ObjectLibrary.remove(s3Object.getS3ObjectKey());
				this.updateFileGrid();
				Notification n = Notification.show("File deleted successfully!", 2000, Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

				dlg.close();
			});
			layout.add(lbl, buttonContainer);
			dlg.add(layout);
			dlg.setModal(true);
			dlg.open();
		});
		return btnDelete;
	}

	private Button rename(TreeItem item) {
		Button btnRename = new Button();
		btnRename.setIcon(VaadinIcon.EDIT.create());

		boolean hasWritePermissions = selectedFolder.getDirectoryMapping().isWrite();

		btnRename.setEnabled(hasWritePermissions);
		btnRename.addClickListener(e -> {
			Dialog dlg = new Dialog();
			TextField tf = new TextField("New Name");
			VerticalLayout vl = new VerticalLayout();
			Button btnConfirm = new Button("Rename");
			Button btnCancel = new Button("Cancel");
			HorizontalLayout buttonContainer = new HorizontalLayout();
			vl.add(new Label("Current Name: " + item.getFilename()), tf, buttonContainer);

			buttonContainer.add(btnConfirm, btnCancel);
			btnConfirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			btnCancel.addClickListener(g -> dlg.close());

			btnConfirm.addClickListener(g -> {
				String currentFileName = item.getFilename();
				String newFileName = tf.getValue();

				if (currentFileName.equals(newFileName)) {
					Notification n = Notification.show("Proposed and current filename are the same.", 1500, Position.MIDDLE);
					n.addThemeVariants(NotificationVariant.LUMO_ERROR);
					return;
				}
				
				/* A little naive, but a good start.... */
				String destinationKey = item.getS3ObjectKey().replace(currentFileName, newFileName);

				logger.info("Renaming " + item.getS3ObjectKey() + " TO " + destinationKey);
				transferEngine.rename(item, newFileName);
				TreeItem containingTree = treeItems.get(selectedFolder.getDirectoryMapping().getDirectoryMappingId());
				containingTree.removeChild(item.getS3ObjectKey());
				this.s3ObjectLibrary.remove(item.getS3ObjectKey());
				S3Client s3 = Toolkit.getS3Client();

				logger.info("Removing item " + item.getS3ObjectKey());

				ListObjectsRequest lor = ListObjectsRequest.builder()
						.bucket(Toolkit.S3_BUCKET)
						.prefix(destinationKey)
						.build();

				logger.info("Looking for item " + destinationKey);

				ListObjectsResponse listObjectsResponse = s3.listObjects(lor);
				S3Object newObject = listObjectsResponse.contents().stream().findFirst().orElse(null);
				if (newObject == null) {
					logger.info("this item is null");
				}
				containingTree.createDirectoryStructure(newObject, selectedFolder.getDirectoryMapping().getTarget());
				this.s3ObjectLibrary.put(newObject.key(), newObject);
				updateFileGrid();
				Notification n = Notification.show("File renamed successfully!", 2000, Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				dlg.close();
			});

			dlg.add(vl);
			dlg.setModal(true);
			dlg.open();
		});
		return btnRename;
	}

	private HashMap<Long, TreeItem> getTreeItems() {
		S3Client s3 = Toolkit.getS3Client();
		HashMap<Long, TreeItem> treeItems = new HashMap<>();
		for (DirectoryMapping directoryMapping : user.getDirectoryMappings().values()) {
			String target = directoryMapping.getTarget();
			/* Remove the bucket name from the target */
			String s3Prefix = target.substring(Toolkit.S3_BUCKET.length() + 2) + "/";
			
			TreeItem tree = new TreeItem(directoryMapping, s3Prefix, true, s3Prefix);

			ListObjectsRequest request = ListObjectsRequest.builder()
					.bucket(Toolkit.S3_BUCKET)
					.prefix(s3Prefix).build();
			
			
			ListObjectsResponse response = s3.listObjects(request);
			//logger.info("s3Prefix=" + s3Prefix + " response size=" + response.contents().size());
			for (S3Object s3Object : response.contents()) {
				boolean isFolder = s3Object.key().endsWith("/");
				if (!isFolder) {
					s3ObjectLibrary.put(s3Object.key(), s3Object);
				}
				//logger.info("Creating directory structure for s3 key=" + s3Object.key() +" target=" +  target);
				tree.createDirectoryStructure(s3Object, target);
			}
			treeItems.put(directoryMapping.getDirectoryMappingId(), tree);
		}
		return treeItems;
	}

	private void updateFileGrid() {
		List<TreeItem> files = selectedFolder.getSubTreeItems(false);
		this.s3ObjectGrid.setItems(files);
	}
}

