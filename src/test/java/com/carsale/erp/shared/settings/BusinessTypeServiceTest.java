package com.carsale.erp.shared.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessTypeServiceTest {

    @Mock
    private AppSettingRepository appSettingRepository;

    @Test
    void missingSettingKeepsEveryPipeline() {
        when(appSettingRepository.findById(BusinessTypeService.KEY)).thenReturn(Optional.empty());

        assertEquals(BusinessType.BOTH, new BusinessTypeService(appSettingRepository).current());
    }

    @Test
    void unknownStoredValueKeepsEveryPipeline() {
        AppSetting setting = new AppSetting();
        setting.setKey(BusinessTypeService.KEY);
        setting.setValue("dealer");
        when(appSettingRepository.findById(BusinessTypeService.KEY)).thenReturn(Optional.of(setting));

        assertEquals(BusinessType.BOTH, new BusinessTypeService(appSettingRepository).current());
    }

    @Test
    void saveStoresTheSelectedCode() {
        when(appSettingRepository.findById(BusinessTypeService.KEY)).thenReturn(Optional.empty());
        BusinessTypeService service = new BusinessTypeService(appSettingRepository);

        service.save(BusinessType.REGISTERED);

        ArgumentCaptor<AppSetting> saved = ArgumentCaptor.forClass(AppSetting.class);
        verify(appSettingRepository).save(saved.capture());
        assertEquals(BusinessTypeService.KEY, saved.getValue().getKey());
        assertEquals("registered", saved.getValue().getValue());
    }
}
