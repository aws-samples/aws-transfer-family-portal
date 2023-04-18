// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.SucceededEvent;

public class FileUploader implements Receiver {
    private final static Logger logger = LogManager.getLogger(FileUploader.class);
    private File file;
    private String BASE_PATH = "C:\\temp\\";

    public OutputStream receiveUpload(String filename,
            String mimeType) {
        FileOutputStream fos = null; // Stream to write to
        try {
            // Open the file for writing.
            file = new File(BASE_PATH + filename);
            System.out.println(BASE_PATH + filename);
            fos = new FileOutputStream(file);
        } catch (final java.io.FileNotFoundException e) {
            logger.error(e.getMessage());
            return null;
        }
        return fos; // Return the output stream to write to
    }

    public void uploadSucceeded(SucceededEvent event) {
        // Do some cool stuff here with the file
    }
};