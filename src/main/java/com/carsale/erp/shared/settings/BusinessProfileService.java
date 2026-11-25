package com.carsale.erp.shared.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BusinessProfileService {

    static final String NAME_KEY = "business_name";
    static final String LOGO_KEY = "business_logo";
    static final int NAME_MAX = 80;
    static final int MAX_LOGO_BYTES = 2 * 1024 * 1024;

    private static final Pattern LOGO_NAME = Pattern.compile("^[a-f0-9]{32}\\.(jpg|jpeg|png|webp|gif)$");

    private final AppSettingRepository appSettingRepository;
    private final Path logoRoot;

    public BusinessProfileService(
            AppSettingRepository appSettingRepository,
            @Value("${app.documents.logoUploadDir:uploads/business-logo}") String logoDir
    ) throws IOException {
        this.appSettingRepository = appSettingRepository;
        this.logoRoot = Paths.get(logoDir).toAbsolutePath().normalize();
        Files.createDirectories(this.logoRoot);
    }

    public BusinessProfile current() {
        return new BusinessProfile(read(NAME_KEY), safeLogoName(read(LOGO_KEY)));
    }

    @Transactional
    public void save(String name, MultipartFile logo, boolean removeLogo) throws IOException {
        String cleanedName = cleanName(name);
        PreparedLogo prepared = prepareLogo(logo);
        write(NAME_KEY, cleanedName);
        if (prepared != null) {
            storeLogo(prepared);
            return;
        }
        if (removeLogo) {
            deleteLogo();
        }
    }

    public Resource loadLogo(BusinessProfile profile) {
        if (profile == null || !profile.isHasLogo()) {
            return null;
        }
        try {
            Path file = logoRoot.resolve(profile.getLogoStoredName()).normalize();
            if (!file.startsWith(logoRoot) || !Files.isRegularFile(file)) {
                return null;
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return null;
            }
            return resource;
        } catch (IOException ex) {
            log.warn("Could not read business logo: {}", ex.getMessage());
            return null;
        }
    }

    public static String contentTypeFor(String storedName) {
        if (storedName != null && storedName.endsWith(".png")) {
            return "image/png";
        }
        if (storedName != null && storedName.endsWith(".webp")) {
            return "image/webp";
        }
        if (storedName != null && storedName.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }

    static String cleanName(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > NAME_MAX) {
            throw new IllegalArgumentException("Enter a business name of 80 characters or fewer.");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 0x20 || c == 0x7F || c == '<' || c == '>') {
                throw new IllegalArgumentException("Use letters, numbers, and normal punctuation in the business name.");
            }
        }
        return trimmed;
    }

    static String safeLogoName(String storedName) {
        if (storedName == null) {
            return null;
        }
        String trimmed = storedName.trim().toLowerCase(Locale.ROOT);
        return LOGO_NAME.matcher(trimmed).matches() ? trimmed : null;
    }

    private PreparedLogo prepareLogo(MultipartFile logo) throws IOException {
        if (logo == null || logo.isEmpty()) {
            return null;
        }
        if (logo.getSize() > MAX_LOGO_BYTES) {
            throw new IllegalArgumentException("Logo must be 2 MB or smaller.");
        }
        String contentType = logo.getContentType();
        if (contentType != null && !contentType.trim().isEmpty()
                && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Choose a JPG, PNG, WEBP, or GIF logo.");
        }
        String originalName = logo.getOriginalFilename() == null ? "" : logo.getOriginalFilename().trim();
        String extension = extensionOf(originalName);
        if (!isAllowedExtension(extension)) {
            throw new IllegalArgumentException("Choose a JPG, PNG, WEBP, or GIF logo.");
        }
        byte[] bytes = logo.getBytes();
        if (bytes.length == 0 || bytes.length > MAX_LOGO_BYTES || !matchesImage(bytes, extension)) {
            throw new IllegalArgumentException("Choose a JPG, PNG, WEBP, or GIF logo.");
        }
        return new PreparedLogo(bytes, extension);
    }

    private void storeLogo(PreparedLogo prepared) throws IOException {
        String previous = safeLogoName(read(LOGO_KEY));
        String storedName = UUID.randomUUID().toString().replace("-", "") + prepared.extension;
        Path target = logoRoot.resolve(storedName).normalize();
        if (!target.startsWith(logoRoot)) {
            throw new IOException("Invalid storage path.");
        }
        Files.write(target, prepared.bytes);
        try {
            write(LOGO_KEY, storedName);
        } catch (RuntimeException ex) {
            Files.deleteIfExists(target);
            throw ex;
        }
        if (previous != null && !previous.equals(storedName)) {
            deleteFile(previous);
        }
    }

    private void deleteLogo() {
        String existing = safeLogoName(read(LOGO_KEY));
        write(LOGO_KEY, null);
        deleteFile(existing);
    }

    private void deleteFile(String storedName) {
        if (storedName == null) {
            return;
        }
        try {
            Path file = logoRoot.resolve(storedName).normalize();
            if (file.startsWith(logoRoot)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ex) {
            log.warn("Could not delete business logo {}: {}", storedName, ex.getMessage());
        }
    }

    private String read(String key) {
        return appSettingRepository.findById(key)
                .map(setting -> blankToNull(setting.getValue()))
                .orElse(null);
    }

    private void write(String key, String value) {
        if (value == null) {
            if (appSettingRepository.existsById(key)) {
                appSettingRepository.deleteById(key);
            }
            return;
        }
        AppSetting setting = appSettingRepository.findById(key).orElseGet(AppSetting::new);
        setting.setKey(key);
        setting.setValue(value);
        appSettingRepository.save(setting);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static boolean isAllowedExtension(String extension) {
        return ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".png".equals(extension)
                || ".webp".equals(extension)
                || ".gif".equals(extension);
    }

    private static boolean matchesImage(byte[] bytes, String extension) {
        if (bytes.length < 12) {
            return false;
        }
        if (".png".equals(extension)) {
            return (bytes[0] & 0xFF) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47;
        }
        if (".gif".equals(extension)) {
            return bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
        }
        if (".webp".equals(extension)) {
            return bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
        }
        return (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
    }

    private static final class PreparedLogo {
        private final byte[] bytes;
        private final String extension;

        private PreparedLogo(byte[] bytes, String extension) {
            this.bytes = bytes;
            this.extension = extension;
        }
    }
}
