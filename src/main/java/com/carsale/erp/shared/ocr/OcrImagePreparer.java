package com.carsale.erp.shared.ocr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.shared.document.DocumentParser;
import com.carsale.erp.shared.utils.CustomsDocumentParserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OcrImagePreparer {
    private static final Logger log = LoggerFactory.getLogger(OcrImagePreparer.class);

    private final int maxImageSide;
    //private final int pdfRenderDpi;
    private final OcrClientRouter ocrClients;
    private final DocumentAiClient documentAiClient;
    private final String language;

    public OcrImagePreparer(
            OcrClientRouter ocrClients,
            @Value("${app.ocr.ocrspace.maxImageSide:2200}") int maxImageSide,
            @Value("${app.ocr.clearance.language:eng}") String language,
            DocumentAiClient documentAiClient
    ) {
        this.ocrClients = ocrClients;
        this.maxImageSide = maxImageSide > 0 ? maxImageSide : 2200;
        //this.pdfRenderDpi = pdfRenderDpi > 0 ? pdfRenderDpi : 220;
        this.language = language == null || language.trim().isEmpty() ? "eng" : language.trim();
        this.documentAiClient = documentAiClient;
    }

    public AuctionParseResult parsePage(MultipartFile file, DocumentParser documentParser, String provider) {
        return parsePage(file, documentParser, provider, resolveLanguage(documentParser));
    }

    public AuctionParseResult parsePage(MultipartFile file, DocumentParser documentParser, String provider, String language) {
        AuctionParseResult failed = new AuctionParseResult();
        String documentName = documentParser.getDocumentName();
        if (file == null || file.isEmpty()) {
            failed.setSuccess(false);
            failed.setMessage("Please upload " + documentName + ".");
            return failed;
        }
        File temp = null;
        try {
            String prefix = CustomsDocumentParserUtils.toSlug(documentName);
            temp = File.createTempFile(prefix, CustomsDocumentParserUtils.suffix(file.getOriginalFilename()));
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            String ocrLanguage = (language == null || language.trim().isEmpty())
                    ? resolveLanguage(documentParser)
                    : language.trim();
            AuctionParseResult parsed = readDocumentText(
                    temp, file.getOriginalFilename(), ocrLanguage, provider, documentParser);
            log.info("OCR text:\n{}", parsed.getRawText());
            return parsed;
        } catch (Throwable ex) {
            log.error("OCR failed", ex);
            failed.setSuccess(false);
            String detail = ex.getMessage();
            if (OcrClient.isUserFacingError(detail)) {
                failed.setMessage(detail + " You can fill the form manually.");
            } else {
                failed.setMessage("Could not read page. You can fill the form manually.");
            }
            return failed;
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp.toPath());
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String resolveLanguage(DocumentParser parser) {
        if (parser != null) {
            String fromParser = parser.getOcrLanguage();
            if (fromParser != null && !fromParser.trim().isEmpty()) {
                return fromParser.trim();
            }
        }
        return language;
    }

    public AuctionParseResult readDocumentText(File file, String originalName, String language, String provider, DocumentParser parser) throws Exception {
        AuctionParseResult documentAiResult = tryDocumentAi(file, originalName, parser);
        if (documentAiResult != null && documentAiResult.isSuccess()) {
            return documentAiResult;
        }

        OcrClient client = ocrClients.clientFor(provider);
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            if (documentAiResult != null) {
                return documentAiResult;
            }
            throw new IOException("Unsupported image format.");
        }
        File imageTemp = writeUploadImage(prepare(image), client);
        try {
            AuctionParseResult ocrResult = parser.parsePage(client.recognize(imageTemp, language));
            if (ocrResult != null && ocrResult.isSuccess()) {
                return ocrResult;
            }
            return documentAiResult != null ? documentAiResult : ocrResult;
        } finally {
            tryDelete(imageTemp);
        }
    }

    private AuctionParseResult tryDocumentAi(File file, String originalName, DocumentParser parser) {
        if (parser == null) {
            return null;
        }
        String processorId = parser.getProcessorId();
        if (processorId == null || processorId.trim().isEmpty()) {
            return null;
        }
        try {
            DocumentAiClient.DocumentAiResult documentAi = documentAiClient.process(file, originalName, parser);
            if (documentAi == null) {
                return null;
            }
            return parser.parsePage(documentAi);
        } catch (Exception ex) {
            log.warn("Document AI failed for {}: {}", parser.getDocumentName(), ex.getMessage());
            return null;
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
