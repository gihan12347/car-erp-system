package com.carsale.erp.customer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.carsale.erp.customer.CustomerService.CustomerRecord;

class CustomerIdentityTest {

    @Test
    void identityCountsWhenNameNicOrCardIsSaved() {
        assertTrue(CustomerService.hasUploadedIdentity("Amal Perera", null, null, null));
        assertTrue(CustomerService.hasUploadedIdentity(null, "199012345678", null, null));
        assertTrue(CustomerService.hasUploadedIdentity(null, null, "front.jpg", null));
        assertTrue(CustomerService.hasUploadedIdentity(" ", " ", " ", "back.jpg"));
        assertFalse(CustomerService.hasUploadedIdentity(null, "  ", null, null));
    }

    @Test
    void searchMatchesNameNicOrChassis() {
        CustomerRecord row = new CustomerRecord(
                "buyer",
                "Buyer",
                "M900A-1122703",
                "Amal Perera",
                "199012345678",
                null,
                null,
                null,
                "HONDA Insight",
                "V001",
                null,
                null,
                null,
                null,
                "/sell/documents/",
                null,
                false
        );

        assertTrue(CustomerService.matches(row, "amal"));
        assertTrue(CustomerService.matches(row, "m900a"));
        assertTrue(CustomerService.matches(row, "199012345678"));
        assertFalse(CustomerService.matches(row, "suzuki"));
    }
}
