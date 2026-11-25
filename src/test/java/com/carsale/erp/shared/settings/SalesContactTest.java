package com.carsale.erp.shared.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SalesContactTest {

    @Test
    void whatsappLinkUsesTheVehicleNameAndLocalNumber() {
        SalesContact contact = new SalesContact("077 123 4567", "077 123 4567");

        String link = contact.whatsappLink("March 2026 Suzuki Wagon R", "V003");

        assertTrue(link.startsWith("https://wa.me/94771234567?text="));
        assertTrue(link.contains("March%202026%20Suzuki%20Wagon%20R"));
        assertTrue(link.contains("Stock%3A%20V003"));
        assertEquals("tel:0771234567", contact.getTelHref());
    }

    @Test
    void blankNumbersStayHidden() {
        assertNull(SalesContact.whatsappDigits("  "));
        assertNull(SalesContactService.clean(""));
        assertNull(SalesContactService.clean("   "));
    }
}
