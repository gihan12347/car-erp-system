package com.carsale.erp.shared.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class BusinessProfileServiceTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
    );

    @TempDir
    Path tempDir;

    @Mock
    private AppSettingRepository appSettingRepository;

    private BusinessProfileService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new BusinessProfileService(appSettingRepository, tempDir.toString());
    }

    @Test
    void blankNameIsClearedAndLongNamesAreRejected() {
        assertNull(BusinessProfileService.cleanName("   "));
        assertEquals("Gihan Motors", BusinessProfileService.cleanName("  Gihan   Motors  "));
        assertThrows(IllegalArgumentException.class, () -> BusinessProfileService.cleanName(
                "A very long business name that should not fit inside the eighty character limit here"
        ));
        assertNull(BusinessProfileService.safeLogoName("../secret.png"));
    }

    @Test
    void saveStoresTheBusinessName() throws Exception {
        when(appSettingRepository.findById(BusinessProfileService.NAME_KEY)).thenReturn(Optional.empty());

        service.save("  Gihan Motors  ", null, false);

        ArgumentCaptor<AppSetting> saved = ArgumentCaptor.forClass(AppSetting.class);
        verify(appSettingRepository).save(saved.capture());
        assertEquals(BusinessProfileService.NAME_KEY, saved.getValue().getKey());
        assertEquals("Gihan Motors", saved.getValue().getValue());
    }

    @Test
    void saveStoresALogoAndReplacesThePreviousFile() throws Exception {
        String previousName = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png";
        Path previous = tempDir.resolve(previousName);
        Files.write(previous, PNG);
        AppSetting existing = new AppSetting();
        existing.setKey(BusinessProfileService.LOGO_KEY);
        existing.setValue(previousName);
        when(appSettingRepository.findById(BusinessProfileService.LOGO_KEY)).thenReturn(Optional.of(existing));
        when(appSettingRepository.findById(BusinessProfileService.NAME_KEY)).thenReturn(Optional.empty());

        MockMultipartFile logo = new MockMultipartFile("logo", "mark.png", "image/png", PNG);
        service.save("Yard One", logo, true);

        ArgumentCaptor<AppSetting> saved = ArgumentCaptor.forClass(AppSetting.class);
        verify(appSettingRepository, times(2)).save(saved.capture());
        String stored = null;
        for (AppSetting setting : saved.getAllValues()) {
            if (BusinessProfileService.LOGO_KEY.equals(setting.getKey())) {
                stored = setting.getValue();
            }
        }
        assertTrue(stored != null && stored.endsWith(".png"));
        assertTrue(Files.exists(tempDir.resolve(stored)));
        assertFalse(Files.exists(previous));
    }

    @Test
    void rejectsAFileThatIsNotAnImage() throws Exception {
        MockMultipartFile logo = new MockMultipartFile("logo", "notes.png", "image/png", "hello".getBytes("UTF-8"));

        assertThrows(IllegalArgumentException.class, () -> service.save("Yard One", logo, false));
    }
}
