package com.carsale.erp.shared.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesContactService {

    static final String PHONE = "sales_phone";
    static final String WHATSAPP = "sales_whatsapp";

    private final AppSettingRepository appSettingRepository;

    public SalesContact current() {
        return new SalesContact(read(PHONE), read(WHATSAPP));
    }

    @Transactional
    public void save(String phone, String whatsapp) {
        write(PHONE, clean(phone));
        write(WHATSAPP, clean(whatsapp));
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

    static String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 40 || !trimmed.matches("[0-9+()\\-\\s]{6,40}")) {
            throw new IllegalArgumentException("Enter a phone number using digits, spaces, and + only.");
        }
        return trimmed;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
