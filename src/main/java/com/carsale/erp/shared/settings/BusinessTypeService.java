package com.carsale.erp.shared.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessTypeService {

    static final String KEY = "business_type";

    private final AppSettingRepository appSettingRepository;

    public BusinessType current() {
        return appSettingRepository.findById(KEY)
                .map(setting -> {
                    BusinessType stored = BusinessType.fromStored(setting.getValue());
                    return stored == null ? BusinessType.BOTH : stored;
                })
                .orElse(BusinessType.BOTH);
    }

    @Transactional
    public void save(BusinessType type) {
        if (type == null) {
            throw new IllegalArgumentException("Choose a business type.");
        }
        AppSetting setting = appSettingRepository.findById(KEY).orElseGet(AppSetting::new);
        setting.setKey(KEY);
        setting.setValue(type.getCode());
        appSettingRepository.save(setting);
    }
}
