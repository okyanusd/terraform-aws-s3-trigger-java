package com.mihsap.lambda;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class S3UploadHandler implements RequestHandler<S3Event, String> {
    private final String region = System.getenv("region");

    private final AmazonS3 amazonS3 = AmazonS3ClientBuilder
            .standard()
            .withRegion(Regions.fromName(region)).build();

    @Override
    public String handleRequest(S3Event s3Event, Context context) {

        String bucketName = s3Event.getRecords().get(0).getS3().getBucket().getName();
        String fileName = s3Event.getRecords().get(0).getS3().getObject().getUrlDecodedKey();
        LambdaLogger log = context.getLogger();
        log.log(String.format("%s %s", bucketName, fileName));
        S3Object s3Object = amazonS3.getObject(bucketName, fileName);

        try (InputStream is = s3Object.getObjectContent();
             PDDocument document = PDDocument.load(is)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);


            pdfRenderer.setSubsamplingAllowed(true);
            int pageNumber = document.getNumberOfPages();

            for (int page = 0; page < pageNumber; page++) {

                try {
                    log.log(page + "-" + fileName + " jpeg write");
                    write(bucketName, fileName, pdfRenderer, s3Object.getObjectMetadata(), page, 200);
                    log.log(page + "-" + fileName + " jpeg write ended");
                } catch (IllegalStateException e) {
                    log.log(String.format("Error %s %s %s", bucketName, fileName, e.getMessage()));
                    throw e;

                } catch (Exception e) {
                    log.log(page + "-" + fileName + " jpeg write 2");
                    write(bucketName, fileName, pdfRenderer, s3Object.getObjectMetadata(), page, 96);
                    log.log(page + "-" + fileName + " jpeg write 2 ended");
                }

            }

        } catch (IOException e) {
            log.log(String.format("Error %s %s %s", bucketName, fileName, e.getMessage()));
            return "Error reading contents of the file";
        }
        return null;
    }

    private void write(String bucketName,
                       String fileName,
                       PDFRenderer pdfRenderer,
                       ObjectMetadata objectMetadata,
                       int page,
                       int dpi) throws IOException {
        BufferedImage bim = pdfRenderer.renderImageWithDPI(page, dpi, ImageType.GRAY);
        Map<String, String> userMetadata = objectMetadata.getUserMetadata();
        byte[] array;

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(bim, "JPEG", os);
            array = os.toByteArray();
        }

        ObjectMetadata objectMetadataJpeg = new ObjectMetadata();
        objectMetadataJpeg.setContentLength(array.length);
        objectMetadataJpeg.setUserMetadata(userMetadata);

        try (InputStream isImage = new ByteArrayInputStream(array)) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    page  + fileName+ "-" + page + ".jpg",
                    isImage,
                    objectMetadataJpeg);
            amazonS3.putObject(putObjectRequest);
        }


    }

}
