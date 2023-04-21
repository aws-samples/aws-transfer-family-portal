package com.example.application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.upload.receivers.FileBuffer;

public class UploadBuffer extends FileBuffer {
    private File tmpFile;
    private final static Logger logger = LogManager.getLogger(UploadBuffer.class);

    @Override
    protected FileOutputStream createFileOutputStream(String fileName) {
        try {
            tmpFile = createFile(fileName); // store reference
            logger.info("Done createFileOutputStream");
            logger.info("Tmp file  path=" + tmpFile.getAbsolutePath());
            return new FileOutputStream(tmpFile);
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            return null;
        }
    }

    private File createFile(String fileName) throws IOException {
        String tempFileName = "upload_tmpfile_" + fileName + "_" + System.currentTimeMillis();
        return File.createTempFile(tempFileName, "tmp");
    }

    public File getTmpFile() {
        return tmpFile;
    }

}
