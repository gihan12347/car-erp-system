package com.carsale.erp.shared.ocr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OcrImagePreparer {

    private final int maxImageSide;
    //private final int pdfRenderDpi;
    private final OcrClientRouter ocrClients;
    private final DocumentAiClient documentAiClient;

    public OcrImagePreparer(
            OcrClientRouter ocrClients,
            @Value("${app.ocr.ocrspace.maxImageSide:2200}") int maxImageSide,
            DocumentAiClient documentAiClient
    ) {
        this.ocrClients = ocrClients;
        this.maxImageSide = maxImageSide > 0 ? maxImageSide : 2200;
        //this.pdfRenderDpi = pdfRenderDpi > 0 ? pdfRenderDpi : 220;
        this.documentAiClient = documentAiClient;
    }

    public AuctionParseResult readDocumentText(File file, String originalName, String language, String provider, DocumentParser parser) throws Exception {
        OcrClient client = ocrClients.clientFor(provider);
        AuctionParseResult result = null;
        //TODO :: disabled this feature ---- if needed need to enable it with considering Google Document AI
//        String name = originalName == null ? "" : originalName.toLowerCase();
//        if (name.endsWith(".pdf")) {
//            log.info("Cloud OCR ({}) for PDF document: {}", client.displayName(), originalName);
//            StringBuilder text = new StringBuilder();
//            try (PDDocument document = PDDocument.load(file)) {
//                PDFRenderer renderer = new PDFRenderer(document);
//                int pages = document.getNumberOfPages();
//                for (int i = 0; i < pages; i++) {
//                    BufferedImage image = prepare(renderer.renderImageWithDPI(i, pdfRenderDpi));
//                    File imageTemp = writeUploadImage(image, "doc-page-", client);
//                    try {
//                        text.append(client.recognize(imageTemp, language)).append('\n');
//                    } finally {
//                        tryDelete(imageTemp);
//                    }
//                }
//            }
//            return parser.parsePage(text.toString());
//        }

        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Unsupported image format.");
        }
        File imageTemp = writeUploadImage(prepare(image), client);
        try {
            if (client instanceof GoogleVisionOcrClient) {
                result = parser.parsePage(documentAiClient.process(file, originalName));
            }
            if (result != null) {
                return result;
            }
            return parser.parsePage(client.recognize(imageTemp, language));
        } finally {
            tryDelete(imageTemp);
        }
    }

    private BufferedImage prepare(BufferedImage source) {
        BufferedImage rgb = toRgb(source);
        int width = rgb.getWidth();
        int height = rgb.getHeight();
        double scale = 1.0d;
        if (width > maxImageSide || height > maxImageSide) {
            scale = Math.min((double) maxImageSide / width, (double) maxImageSide / height);
        }
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, targetWidth, targetHeight);
        graphics.drawImage(rgb, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return enhanceHandwriting(scaled);
    }

    private BufferedImage enhanceHandwriting(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage enhanced = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int gray = ((((rgb >> 16) & 0xff) * 30) + (((rgb >> 8) & 0xff) * 59) + ((rgb & 0xff) * 11)) / 100;
                int value = (int) ((gray - 128) * 1.35d + 128);
                if (value < 0) {
                    value = 0;
                } else if (value > 255) {
                    value = 255;
                }
                if (value < 48) {
                    value = 0;
                } else if (value > 232) {
                    value = 255;
                }
                enhanced.setRGB(x, y, (value << 16) | (value << 8) | value);
            }
        }
        return enhanced;
    }

    private File writeUploadImage(BufferedImage source, OcrClient client) throws IOException {
        BufferedImage image = toRgb(source);
        File temp = File.createTempFile("doc-", ".jpg");
        float quality = 0.88f;
        double scale = 1.0d;

        for (int attempt = 0; attempt < 14; attempt++) {
            BufferedImage scaled = scaleImage(image, scale);
            writeJpeg(scaled, temp, quality);
            if (temp.length() <= client.maxUploadBytes()) {
                return temp;
            }
            if (quality > 0.45f) {
                quality -= 0.1f;
            } else {
                scale *= 0.85d;
                quality = 0.85f;
            }
        }

        throw new IOException(
                "Could not compress the document below the " + client.displayName()
                        + " size limit (" + formatMb(client.maxUploadBytes()) + ")."
        );
    }

    private BufferedImage scaleImage(BufferedImage source, double scale) {
        if (scale >= 0.999d) {
            return source;
        }
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rgb;
    }

    private void writeJpeg(BufferedImage image, File file, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available.");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        try (ImageOutputStream output = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private void tryDelete(File file) {
        if (file == null) {
            return;
        }
        try {
            java.nio.file.Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
        }
    }

    private static String formatMb(long bytes) {
        double mb = bytes / 1000000.0d;
        if (mb == Math.rint(mb)) {
            return (long) mb + " MB";
        }
        return String.format("%.1f MB", mb);
    }
}
