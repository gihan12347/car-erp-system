package com.carsale.erp.shared.document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SheetDocumentStorageService {

    private final Path auctionRoot;
    private final Path reshipRoot;
    private final Path clearanceRoot;
    private final Path equipmentRoot;
    private final Path coiRoot;
    private final Path standardsRoot;
    private final Path exportRoot;
    private final Path photoRoot;

    public SheetDocumentStorageService(
            @Value("${app.documents.uploadDir:uploads/auction-sheets}") String uploadDir,
            @Value("${app.documents.preshipUploadDir:uploads/pre-shipment-docs}") String preshipUploadDir,
            @Value("${app.documents.clearanceUploadDir:uploads/clearance-docs}") String clearanceUploadDir,
            @Value("${app.documents.equipmentUploadDir:uploads/equipment-docs}") String equipmentUploadDir,
            @Value("${app.documents.coiUploadDir:uploads/coi-docs}") String coiUploadDir,
            @Value("${app.documents.standardsUploadDir:uploads/standards-docs}") String standardsUploadDir,
            @Value("${app.documents.exportUploadDir:uploads/export-docs}") String exportUploadDir,
            @Value("${app.documents.photoUploadDir:uploads/vehicle-photos}") String photoUploadDir
    ) throws IOException {
        this.auctionRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.reshipRoot = Paths.get(preshipUploadDir).toAbsolutePath().normalize();
        this.clearanceRoot = Paths.get(clearanceUploadDir).toAbsolutePath().normalize();
        this.equipmentRoot = Paths.get(equipmentUploadDir).toAbsolutePath().normalize();
        this.coiRoot = Paths.get(coiUploadDir).toAbsolutePath().normalize();
        this.standardsRoot = Paths.get(standardsUploadDir).toAbsolutePath().normalize();
        this.exportRoot = Paths.get(exportUploadDir).toAbsolutePath().normalize();
        this.photoRoot = Paths.get(photoUploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.auctionRoot);
        Files.createDirectories(this.reshipRoot);
        Files.createDirectories(this.clearanceRoot);
        Files.createDirectories(this.equipmentRoot);
        Files.createDirectories(this.coiRoot);
        Files.createDirectories(this.standardsRoot);
        Files.createDirectories(this.exportRoot);
        Files.createDirectories(this.photoRoot);
    }

    public SheetUploadResult store(MultipartFile file) throws IOException {
        return store(file, auctionRoot, "/auction/documents/", "auction sheet");
    }

    public SheetUploadResult storePreShipment(MultipartFile file) throws IOException {
        return store(file, reshipRoot, "/shipping/documents/", "pre-shipment certificate");
    }

    public SheetUploadResult storeClearance(MultipartFile file) throws IOException {
        return store(file, clearanceRoot, "/customs/documents/", "clearance document page");
    }

    public SheetUploadResult storeEquipment(MultipartFile file) throws IOException {
        return store(file, equipmentRoot, "/equipment/documents/", "equipment condition document");
    }

    public SheetUploadResult storeCoi(MultipartFile file) throws IOException {
        return store(file, coiRoot, "/coi/documents/", "certificate of inspection");
    }

    public SheetUploadResult storeStandards(MultipartFile file) throws IOException {
        return store(file, standardsRoot, "/standards/documents/", "standards certificate");
    }

    public SheetUploadResult storeExport(MultipartFile file) throws IOException {
        return store(file, exportRoot, "/export/documents/", "export certificate");
    }

    public SheetUploadResult storePhoto(MultipartFile file) throws IOException {
        SheetUploadResult rejected = new SheetUploadResult();
        if (file == null || file.isEmpty()) {
            rejected.setSuccess(false);
            rejected.setMessage("Please choose a JPG, PNG, or WEBP image.");
            return rejected;
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
        if (!isAllowedImageExtension(extensionOf(originalName))) {
            rejected.setSuccess(false);
            rejected.setMessage("Only JPG, PNG, or WEBP images are allowed.");
            return rejected;
        }
        SheetUploadResult result = store(file, photoRoot, "/photos/documents/", "vehicle image");
        if (result.isSuccess()) {
            result.setMessage("Image uploaded.");
        }
        return result;
    }

    private SheetUploadResult store(MultipartFile file, Path uploadRoot, String previewPrefix, String label) throws IOException {
        SheetUploadResult result = new SheetUploadResult();
        if (file == null || file.isEmpty()) {
            result.setSuccess(false);
            result.setMessage("Please choose a " + label + " image or PDF.");
            return result;
        }

        String originalName = file.getOriginalFilename() == null ? label : file.getOriginalFilename().trim();
        String extension = extensionOf(originalName);
        if (!isAllowedExtension(extension)) {
            result.setSuccess(false);
            result.setMessage("Only JPG, PNG, WEBP, or PDF files are allowed.");
            return result;
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = uploadRoot.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new IOException("Invalid storage path.");
        }

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.trim().isEmpty()) {
            contentType = guessContentType(extension);
        }

        result.setSuccess(true);
        result.setMessage("Document uploaded.");
        result.setOriginalName(originalName);
        result.setStoredName(storedName);
        result.setContentType(contentType);
        result.setPreviewUrl(previewPrefix + storedName);
        result.setPdf(isPdf(extension, contentType));
        return result;
    }

    public Resource loadAsResource(String storedName) throws IOException {
        return loadAsResource(storedName, auctionRoot);
    }

    public Resource loadPreShipmentAsResource(String storedName) throws IOException {
        return loadAsResource(storedName, reshipRoot);
    }

    public Resource loadClearanceAsResource(String storedName) throws IOException {
        return loadAsResource(storedName, clearanceRoot);
    }

    public Resource loadEquipmentAsResource(String storedName) throws IOException {
        return loadAsResource(storedName, equipmentRoot);
    }

    public Resource loadCoiAsResource(String storedName) throws IOException {
        return loadAsResource(storedName, coiRoot);
    }

    public Resource loadStandardsAsResource(String storedName) throws IOException {
        return loadAsResource(storedName, standardsRoot);
    }

    public Resource loadExportAsResource(String storedName) throws IOException {
        return loadAsResource(storedName, exportRoot);
    }

    public Resource loadPhotoAsResource(String storedName) throws IOException {
        return loadAsResource(storedName, photoRoot);
    }

    private Resource loadAsResource(String storedName, Path uploadRoot) throws IOException {
        String safeName = sanitizeStoredName(storedName);
        Path file = uploadRoot.resolve(safeName).normalize();
        if (!file.startsWith(uploadRoot) || !Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IOException("Document not found: " + safeName);
        }
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("Document not readable: " + safeName);
        }
        return resource;
    }

    public String resolveContentType(String storedName, String fallback) {
        String extension = extensionOf(storedName);
        String guessed = guessContentType(extension);
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback;
        }
        return guessed;
    }

    public void deleteIfExists(String storedName) {
        deleteIfExists(storedName, auctionRoot);
    }

    public void deletePreShipmentIfExists(String storedName) {
        deleteIfExists(storedName, reshipRoot);
    }

    public void deleteClearanceIfExists(String storedName) {
        deleteIfExists(storedName, clearanceRoot);
    }

    public void deleteEquipmentIfExists(String storedName) {
        deleteIfExists(storedName, equipmentRoot);
    }

    public void deleteCoiIfExists(String storedName) {
        deleteIfExists(storedName, coiRoot);
    }

    public void deleteStandardsIfExists(String storedName) {
        deleteIfExists(storedName, standardsRoot);
    }

    public void deleteExportIfExists(String storedName) {
        deleteIfExists(storedName, exportRoot);
    }

    public void deletePhotoIfExists(String storedName) {
        deleteIfExists(storedName, photoRoot);
    }

    private void deleteIfExists(String storedName, Path uploadRoot) {
        if (storedName == null || storedName.trim().isEmpty()) {
            return;
        }
        try {
            Path file = uploadRoot.resolve(sanitizeStoredName(storedName)).normalize();
            if (file.startsWith(uploadRoot)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ignored) {
        }
    }

    private String sanitizeStoredName(String storedName) throws IOException {
        if (storedName == null || storedName.trim().isEmpty()) {
            throw new IOException("Missing document name.");
        }
        String name = storedName.trim();
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            throw new IOException("Invalid document name.");
        }
        return name;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static boolean isAllowedImageExtension(String extension) {
        return ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".png".equals(extension)
                || ".webp".equals(extension);
    }

    private static boolean isAllowedExtension(String extension) {
        return isAllowedImageExtension(extension) || ".pdf".equals(extension);
    }

    private static boolean isPdf(String extension, String contentType) {
        return ".pdf".equals(extension)
                || (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("pdf"));
    }

    private static String guessContentType(String extension) {
        if (".pdf".equals(extension)) {
            return "application/pdf";
        }
        if (".png".equals(extension)) {
            return "image/png";
        }
        if (".webp".equals(extension)) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
