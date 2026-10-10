package com.carsale.erp.shared.ocr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Prepares a Japanese auction sheet photo before Document AI:
 * upright the page, deskew it, and soften scan noise.
 */
@Component
public class AuctionSheetImagePreparer {

    private static final int MIN_SIDE = 1600;
    private static final int SKEW_SAMPLE = 420;
    private static final int DARK = 150;

    private final int maxSide;

    public AuctionSheetImagePreparer(@Value("${app.ocr.ocrspace.maxImageSide:2200}") int maxSide) {
        this.maxSide = maxSide > 0 ? maxSide : 2200;
    }

    public File prepare(File source) throws IOException {
        if (source == null || !source.isFile()) {
            return null;
        }
        int orientation = jpegOrientation(source);
        BufferedImage image = ImageIO.read(source);
        if (image == null) {
            return null;
        }
        BufferedImage processed = process(image, orientation, maxSide);
        File out = File.createTempFile("auction-sheet-", ".jpg");
        writeJpeg(processed, out, 0.92f);
        return out;
    }

    public static BufferedImage process(BufferedImage source, int orientation, int maxSide) {
        BufferedImage upright = applyOrientation(toRgb(source), orientation);
        double correction = skewDegrees(upright);
        BufferedImage straight = Math.abs(correction) >= 0.4d ? rotate(upright, correction) : upright;
        BufferedImage clean = denoise(straight);
        return scaleToRange(clean, MIN_SIDE, maxSide > 0 ? maxSide : 2200);
    }

    /**
     * Degrees to rotate the image so ruled lines sit horizontal.
     */
    public static double skewDegrees(BufferedImage source) {
        BufferedImage sample = scaleLongest(toRgb(source), SKEW_SAMPLE);
        double bestAngle = 0d;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int tenths = -80; tenths <= 80; tenths += 5) {
            double angle = tenths / 10.0d;
            BufferedImage rotated = angle == 0d ? sample : rotate(sample, angle);
            double score = horizontalInkVariance(rotated);
            if (score > bestScore) {
                bestScore = score;
                bestAngle = angle;
            }
        }
        return bestAngle;
    }

    static int jpegOrientation(File file) {
        try {
            byte[] header = readPrefix(file, 65536);
            int app1 = indexOf(header, new byte[] {(byte) 0xFF, (byte) 0xE1});
            if (app1 < 0 || app1 + 10 >= header.length) {
                return 1;
            }
            int exif = indexOf(header, "Exif\0\0".getBytes("US-ASCII"), app1, Math.min(header.length, app1 + 16));
            if (exif < 0) {
                return 1;
            }
            int tiff = exif + 6;
            if (tiff + 8 >= header.length) {
                return 1;
            }
            boolean little = header[tiff] == 'I' && header[tiff + 1] == 'I';
            int ifd = tiff + readInt(header, tiff + 4, little);
            if (ifd < 0 || ifd + 2 >= header.length) {
                return 1;
            }
            int count = readShort(header, ifd, little);
            int entry = ifd + 2;
            for (int i = 0; i < count; i++) {
                int at = entry + i * 12;
                if (at + 12 > header.length) {
                    break;
                }
                if (readShort(header, at, little) == 0x0112) {
                    int value = readShort(header, at + 8, little);
                    return value >= 1 && value <= 8 ? value : 1;
                }
            }
        } catch (IOException ignored) {
        }
        return 1;
    }

    public static BufferedImage rotate(BufferedImage source, double degrees) {
        double radians = Math.toRadians(degrees);
        int width = source.getWidth();
        int height = source.getHeight();
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int targetWidth = Math.max(1, (int) Math.floor(width * cos + height * sin));
        int targetHeight = Math.max(1, (int) Math.floor(height * cos + width * sin));
        BufferedImage rotated = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rotated.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, targetWidth, targetHeight);
        AffineTransform transform = new AffineTransform();
        transform.translate(targetWidth / 2.0d, targetHeight / 2.0d);
        transform.rotate(radians);
        transform.translate(-width / 2.0d, -height / 2.0d);
        graphics.drawImage(source, transform, null);
        graphics.dispose();
        return rotated;
    }

    private static BufferedImage applyOrientation(BufferedImage source, int orientation) {
        switch (orientation) {
            case 3:
                return rotate(source, 180d);
            case 6:
                return rotate(source, 90d);
            case 8:
                return rotate(source, -90d);
            default:
                return source;
        }
    }

    private static BufferedImage denoise(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] gray = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                gray[y * width + x] = luminance(source.getRGB(x, y));
            }
        }
        BufferedImage clean = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = median(gray, width, height, x, y);
                value = (int) ((value - 128) * 1.12d + 128);
                if (value < 0) {
                    value = 0;
                } else if (value > 255) {
                    value = 255;
                }
                clean.setRGB(x, y, (value << 16) | (value << 8) | value);
            }
        }
        return clean;
    }

    private static int median(int[] gray, int width, int height, int x, int y) {
        int[] window = new int[9];
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            int yy = y + dy;
            if (yy < 0 || yy >= height) {
                continue;
            }
            for (int dx = -1; dx <= 1; dx++) {
                int xx = x + dx;
                if (xx < 0 || xx >= width) {
                    continue;
                }
                window[count++] = gray[yy * width + xx];
            }
        }
        for (int i = 1; i < count; i++) {
            int value = window[i];
            int j = i - 1;
            while (j >= 0 && window[j] > value) {
                window[j + 1] = window[j];
                j--;
            }
            window[j + 1] = value;
        }
        return window[count / 2];
    }

    private static double horizontalInkVariance(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[] rows = new double[height];
        double sum = 0d;
        for (int y = 0; y < height; y++) {
            int ink = 0;
            for (int x = 0; x < width; x++) {
                if (luminance(image.getRGB(x, y)) < DARK) {
                    ink++;
                }
            }
            rows[y] = ink;
            sum += ink;
        }
        double mean = sum / height;
        double variance = 0d;
        for (int y = 0; y < height; y++) {
            double delta = rows[y] - mean;
            variance += delta * delta;
        }
        return variance / height;
    }

    private static BufferedImage scaleToRange(BufferedImage source, int minSide, int maxSide) {
        int longest = Math.max(source.getWidth(), source.getHeight());
        double scale = 1d;
        if (longest > maxSide) {
            scale = (double) maxSide / longest;
        } else if (longest < minSide && longest > 0) {
            scale = (double) minSide / longest;
        }
        if (Math.abs(scale - 1d) < 0.01d) {
            return source;
        }
        return scaleLongest(source, (int) Math.round(longest * scale));
    }

    private static BufferedImage scaleLongest(BufferedImage source, int longest) {
        int width = source.getWidth();
        int height = source.getHeight();
        int current = Math.max(width, height);
        if (current <= longest) {
            return source;
        }
        double scale = (double) longest / current;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, targetWidth, targetHeight);
        graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return scaled;
    }

    private static BufferedImage toRgb(BufferedImage source) {
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

    private static int luminance(int rgb) {
        return ((((rgb >> 16) & 0xff) * 30) + (((rgb >> 8) & 0xff) * 59) + ((rgb & 0xff) * 11)) / 100;
    }

    private static void writeJpeg(BufferedImage image, File file, float quality) throws IOException {
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

    private static byte[] readPrefix(File file, int limit) throws IOException {
        byte[] all = Files.readAllBytes(file.toPath());
        if (all.length <= limit) {
            return all;
        }
        byte[] prefix = new byte[limit];
        System.arraycopy(all, 0, prefix, 0, limit);
        return prefix;
    }

    private static int indexOf(byte[] data, byte[] needle) {
        return indexOf(data, needle, 0, data.length);
    }

    private static int indexOf(byte[] data, byte[] needle, int from, int to) {
        int last = Math.min(data.length, to) - needle.length;
        for (int i = from; i <= last; i++) {
            boolean match = true;
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    private static int readShort(byte[] data, int offset, boolean little) {
        int a = data[offset] & 0xff;
        int b = data[offset + 1] & 0xff;
        return little ? (b << 8) | a : (a << 8) | b;
    }

    private static int readInt(byte[] data, int offset, boolean little) {
        int a = data[offset] & 0xff;
        int b = data[offset + 1] & 0xff;
        int c = data[offset + 2] & 0xff;
        int d = data[offset + 3] & 0xff;
        return little
                ? (d << 24) | (c << 16) | (b << 8) | a
                : (a << 24) | (b << 16) | (c << 8) | d;
    }
}
