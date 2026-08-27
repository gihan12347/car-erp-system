package com.carsale.erp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EquipmentInspectionFieldsTest {

    @Test
    void standardChoicesMatchInspectionSheetValues() {
        assertTrue(EquipmentInspectionFields.isChoiceOption("YES"));
        assertTrue(EquipmentInspectionFields.isChoiceOption("no"));
        assertTrue(EquipmentInspectionFields.isChoiceOption("OK"));
        assertTrue(EquipmentInspectionFields.isChoiceOption("n/a"));
        assertFalse(EquipmentInspectionFields.isChoiceOption("SINGLE"));
        assertFalse(EquipmentInspectionFields.isChoiceOption("PLASTIC"));
        assertTrue(EquipmentInspectionFields.sameChoice("yes", "YES"));
        assertTrue(EquipmentInspectionFields.sameChoice("N/A", "n / a"));
        assertEquals("sunroof_NA", EquipmentInspectionFields.choiceDomId("sunroof", "N/A"));
    }
}
