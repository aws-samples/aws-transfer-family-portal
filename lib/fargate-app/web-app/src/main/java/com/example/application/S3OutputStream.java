package com.example.application;

import java.io.File;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.progressbar.ProgressBar;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;

public class S3OutputStream extends OutputStream {
    private final static Logger logger = LogManager.getLogger(S3OutputStream.class);

    /** Default chunk size is 10MB */
    public static final int BUFFER_SIZE = 10000000;

    /** The bucket-name on Amazon S3 */
    private final String bucket;

    /** The path (key) name within the bucket */
    private final String path;

    /** The temporary buffer used for storing the chunks */
    private final byte[] buf;

    /** The position in the buffer */
    private int position;

    /** Amazon S3 client. */
    // TODO: support KMS
    private final S3Client s3Client;

    /** The unique id for this upload */
    private String uploadId;

    /** Collection of the etags for the parts that have been uploaded */
    private final List<String> etags;

    /** indicates whether the stream is still open / valid */
    private boolean open;
    File myObj;
    private UI ui;
    private ProgressBar progressBar;
    private int parts;
    private List<CompletedPart> completedParts = new ArrayList<>();

    /**
     * Creates a new S3 OutputStream
     * 
     * @param s3Client the AmazonS3 client
     * @param bucket   name of the bucket
     * @param path     path within the bucket
     */
    public S3OutputStream(UI ui, ProgressBar progressBar, int parts, S3Client s3Client, String bucket, String path) {
        System.out.println("Final try");
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.path = path;
        this.buf = new byte[BUFFER_SIZE];
        this.position = 0;
        this.etags = new ArrayList<>();
        this.open = true;
        this.ui = ui;
        this.progressBar = progressBar;
        this.parts = parts;
    }

    /**
     * Write an array to the S3 output stream.
     *
     * @param b the byte-array to append
     */
    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    /**
     * Writes an array to the S3 Output Stream
     *
     * @param byteArray the array to write
     * @param o         the offset into the array
     * @param l         the number of bytes to write
     */
    @Override
    public void write(final byte[] byteArray, final int o, final int l) {
        this.assertOpen();
        int ofs = o, len = l;
        int size;
        while (len > (size = this.buf.length - position)) {
            System.arraycopy(byteArray, ofs, this.buf, this.position, size);
            this.position += size;
            flushBufferAndRewind();
            ofs += size;
            len -= size;
        }
        System.arraycopy(byteArray, ofs, this.buf, this.position, len);
        this.position += len;

    }

    /**
     * Flushes the buffer by uploading a part to S3.
     */
    @Override
    public synchronized void flush() {
        this.assertOpen();
        System.out.println("Flush was called");
    }

    protected void flushBufferAndRewind() {
        if (uploadId == null) {
            System.out.println("Starting a multipart upload for " + bucket + "  " + this.path);
            CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                    .bucket(this.bucket)
                    .key(this.path)
                    .build();
            CreateMultipartUploadResponse initResponse = s3Client.createMultipartUpload(createMultipartUploadRequest);
            this.uploadId = initResponse.uploadId();
        }
        uploadPart();
        this.position = 0;
    }

    class UploadThread extends Thread {
        UploadPartRequest uploadPartRequest;

        UploadThread(UploadPartRequest uploadPartRequest) {
            this.uploadPartRequest = uploadPartRequest;
        }

        public void run() {

        }
    }

    protected void gabeUploadPart() {

    }

    protected void uploadPart() {
        int partNumber = etags.size() + 1;
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(this.bucket)
                .key(this.path)
                .uploadId(uploadId)
                .partNumber(partNumber).build();
        byte[] subset = Arrays.copyOf(buf, position);
        String etag = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(subset)).eTag();
        CompletedPart part = CompletedPart.builder().partNumber(partNumber).eTag(etag).build();
        double progress = (double) partNumber / parts;
        System.out.println("Uploaded part " + partNumber + ", " + progress);
        ui.access(() -> progressBar.setValue(progress));
        completedParts.add(part);
        etags.add(etag);
    }

    @Override
    public void close() {
        System.out.println("Calling close(), open = " + (open));
        if (this.open) {
            this.open = false;
            if (this.uploadId != null) {
                if (this.position > 0) {
                    uploadPart();
                }
                System.out.println("Completing multipart");

                // Finally call completeMultipartUpload operation to tell S3 to merge all
                // uploaded
                // parts and finish the multipart operation.

                completedParts.sort(Comparator.comparing(CompletedPart::partNumber));

                List<CompletedPart> sortedParts = completedParts.stream()
                        .sorted(Comparator.comparing(CompletedPart::partNumber))
                        .collect(Collectors.toList());

                CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                        .parts(sortedParts)
                        .build();

                CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                        .bucket(this.bucket)
                        .key(this.path)
                        .uploadId(uploadId)
                        .multipartUpload(completedMultipartUpload)
                        .build();

                CompleteMultipartUploadResponse resp = s3Client.completeMultipartUpload(completeMultipartUploadRequest);
                System.out.println(resp.toString());
                System.out.println(resp.key() + " " + resp.location());
                System.out.println("done logging");

                // this.s3Client.completeMultipartUpload(new
                // CompleteMultipartUploadRequest(bucket, path, uploadId, etags));
            }

            else {
                try {
                    System.out.println("uploading object at once");
                    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(path)
                            .build();

                    PutObjectResponse response = s3Client.putObject(putObjectRequest,
                            RequestBody.fromByteBuffer(getRandomByteBuffer(10_000)));
                    System.out.println("done logging request");
                    System.out.println(response.toString());
                    System.out.println(response.sdkHttpResponse().statusCode());
                    System.out.println("done logging");
                } catch (Exception e) {
                    logger.info(e.getMessage());
                }
            }
        }
    }

    private static ByteBuffer getRandomByteBuffer(int size) {
        byte[] b = new byte[size];
        // Random random = new Random();
        // random.nextBytes(b);
        // new Random().nextBytes(b);
        return ByteBuffer.wrap(b);
    }

    public void cancel() {
        this.open = false;
        if (this.uploadId != null) {
            // LOG.debug("Aborting multipart upload");
            System.out.println("Aborting");
            // this.s3Client.abortMultipartUpload(new
            // AbortMultipartUploadRequest(this.bucket, this.path, this.uploadId));
        }
    }

    @Override
    public void write(int b) {
        this.assertOpen();
        if (position >= this.buf.length) {
            flushBufferAndRewind();
        }
        this.buf[position++] = (byte) b;
    }

    private void assertOpen() {
        if (!this.open) {
            System.out.println("Closed");
            throw new IllegalStateException("Closed");
        }
    }
}