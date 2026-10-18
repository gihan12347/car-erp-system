package com.carsale.erp.readypipeline.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.carsale.erp.customer.Customer;
import com.carsale.erp.readypipeline.model.SaleListing;

class BuyerIdentityCopyTest {

    @Test
    void copiesBothSidesWithoutSellingFields() {
        Customer customer = new Customer();
        customer.setFullName("Amal Perera");
        customer.setNicNo("199012345678");
        customer.setAddress("12 Lake Road");
        customer.setDateOfBirth("1990-01-02");
        customer.setGender("Male");
        customer.setDocumentStoredName("front.jpg");
        customer.setDocumentOriginalName("front-id.jpg");
        customer.setDocumentContentType("image/jpeg");
        customer.setOcrText("front text");
        customer.setBackDocumentStoredName("back.jpg");
        customer.setBackDocumentOriginalName("back-id.jpg");
        customer.setBackDocumentContentType("image/jpeg");
        customer.setBackOcrText("back text");

        SaleListing listing = new SaleListing();
        listing.setAskingPrice("1500000");
        listing.setListedOn("2026-10-01");
        listing.setSaleCode("SALE-1");
        listing.setSaleLocation("Colombo");

        assertTrue(SaleListingService.copyBuyerIdentity(listing, customer));

        assertEquals("Amal Perera", listing.getBuyerFullName());
        assertEquals("199012345678", listing.getBuyerNicNo());
        assertEquals("12 Lake Road", listing.getBuyerAddress());
        assertEquals("1990-01-02", listing.getBuyerDateOfBirth());
        assertEquals("Male", listing.getBuyerGender());
        assertEquals("front.jpg", listing.getBuyerDocumentStoredName());
        assertEquals("back.jpg", listing.getBuyerBackDocumentStoredName());
        assertEquals("1500000", listing.getAskingPrice());
        assertEquals("2026-10-01", listing.getListedOn());
        assertEquals("SALE-1", listing.getSaleCode());
        assertEquals("Colombo", listing.getSaleLocation());
        assertFalse(listing.isSold());
    }

    @Test
    void leavesBuyerFieldsAlreadyOnTheListing() {
        Customer customer = new Customer();
        customer.setFullName("New Name");
        customer.setNicNo("200011112222");
        customer.setDocumentStoredName("new-front.jpg");

        SaleListing listing = new SaleListing();
        listing.setBuyerFullName("Kept Name");
        listing.setBuyerNicNo("111");
        listing.setBuyerDocumentStoredName("kept-front.jpg");

        assertFalse(SaleListingService.copyBuyerIdentity(listing, customer));
        assertEquals("Kept Name", listing.getBuyerFullName());
        assertEquals("111", listing.getBuyerNicNo());
        assertEquals("kept-front.jpg", listing.getBuyerDocumentStoredName());
    }
}
