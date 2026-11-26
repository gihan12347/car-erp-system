package com.carsale.erp.readypipeline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carsale.erp.customer.CustomerRepository;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.model.SaleLocation;
import com.carsale.erp.readypipeline.repository.SaleListingRepository;
import com.carsale.erp.shared.service.SkippedStageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaleListingPriceSyncTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private SaleListingRepository saleListingRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SaleLocationService saleLocationService;

    @Mock
    private VehicleRegistrationService vehicleRegistrationService;

    @Mock
    private SkippedStageService skippedStageService;

    @InjectMocks
    private SaleListingService saleListingService;

    @Test
    void moveToSaleCopiesThePriceIntoActualAndAsking() {
        when(vehicleRepository.findById("PRICE-1")).thenReturn(Optional.of(vehicle("PRICE-1")));
        when(saleListingRepository.findById("PRICE-1")).thenReturn(Optional.empty());
        when(saleLocationService.requireAssignable("SL-01", "PRICE-1")).thenReturn(sale("SL-01"));

        SaleListing incoming = new SaleListing();
        incoming.setChassisNo("PRICE-1");
        incoming.setListedOn("2026-11-26");
        incoming.setSaleCode("SL-01");
        incoming.setAskingPrice("  7,209,327.09  ");
        incoming.setAssignmentComplete(true);
        saleListingService.saveAssignment(incoming);

        SaleListing saved = savedListing();
        assertThat(saved.getAskingPrice()).isEqualTo("7,209,327.09");
        assertThat(saved.getActualPrice()).isEqualTo("7,209,327.09");
    }

    @Test
    void moveToSaleKeepsDifferentActualAndAskingPrices() {
        when(vehicleRepository.findById("PRICE-2")).thenReturn(Optional.of(vehicle("PRICE-2")));
        when(saleListingRepository.findById("PRICE-2")).thenReturn(Optional.empty());
        when(saleLocationService.requireAssignable("SL-01", "PRICE-2")).thenReturn(sale("SL-01"));

        SaleListing incoming = new SaleListing();
        incoming.setChassisNo("PRICE-2");
        incoming.setListedOn("2026-11-26");
        incoming.setSaleCode("SL-01");
        incoming.setActualPrice("6,500,000");
        incoming.setAskingPrice("7,200,000");
        incoming.setAssignmentComplete(true);
        saleListingService.saveAssignment(incoming);

        SaleListing saved = savedListing();
        assertThat(saved.getActualPrice()).isEqualTo("6,500,000");
        assertThat(saved.getAskingPrice()).isEqualTo("7,200,000");
    }

    @Test
    void summaryPriceEditStoresEachValue() {
        SaleListing listing = new SaleListing();
        listing.setChassisNo("PRICE-3");
        listing.setSaleCode("SL-01");
        listing.setAskingPrice("7,200,000");
        listing.setActualPrice("7,200,000");
        when(saleListingRepository.findById("PRICE-3")).thenReturn(Optional.of(listing));

        saleListingService.updatePrice("PRICE-3", "actual", "6,100,000.50");

        assertThat(listing.getActualPrice()).isEqualTo("6,100,000.50");
        assertThat(listing.getAskingPrice()).isEqualTo("7,200,000");
        verify(saleListingRepository).save(listing);
    }

    @Test
    void summaryAskingEditLeavesActualPrice() {
        SaleListing listing = new SaleListing();
        listing.setChassisNo("PRICE-4");
        listing.setSaleCode("SL-01");
        listing.setAskingPrice("7,200,000");
        listing.setActualPrice("7,200,000");
        when(saleListingRepository.findById("PRICE-4")).thenReturn(Optional.of(listing));

        saleListingService.updatePrice("PRICE-4", "asking", "7,400,000");

        assertThat(listing.getAskingPrice()).isEqualTo("7,400,000");
        assertThat(listing.getActualPrice()).isEqualTo("7,200,000");
    }

    private SaleListing savedListing() {
        ArgumentCaptor<SaleListing> captor = ArgumentCaptor.forClass(SaleListing.class);
        verify(saleListingRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Vehicle vehicle(String chassisNo) {
        Vehicle vehicle = new Vehicle();
        vehicle.setChassisNo(chassisNo);
        return vehicle;
    }

    private static SaleLocation sale(String code) {
        SaleLocation sale = new SaleLocation();
        sale.setSaleCode(code);
        sale.setLocation("Colombo");
        return sale;
    }
}
