package com.carsale.erp.shared.share;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.carsale.erp.importpipeline.repository.EquipmentInspectionRepository;
import com.carsale.erp.importpipeline.repository.VehiclePhotoRepository;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.repository.SaleListingRepository;
import com.carsale.erp.readypipeline.repository.VehicleRegistrationRepository;
import com.carsale.erp.readypipeline.service.SaleLocationService;
import com.carsale.erp.report.ReportService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import com.carsale.erp.shared.vehicle.VehicleStage;

@ExtendWith(MockitoExtension.class)
class VehicleShareServiceTest {

    @Mock
    private VehicleShareLinkRepository shareLinkRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private SaleListingRepository saleListingRepository;
    @Mock
    private VehiclePhotoRepository vehiclePhotoRepository;
    @Mock
    private EquipmentInspectionRepository equipmentInspectionRepository;
    @Mock
    private VehicleRegistrationRepository vehicleRegistrationRepository;
    @Mock
    private SaleLocationService saleLocationService;
    @Mock
    private ReportService reportService;

    @Test
    void salesReadyUsesTheExistingSalePlace() {
        Vehicle vehicle = vehicle("ABC");
        when(saleListingRepository.findById("ABC")).thenReturn(Optional.empty());
        when(reportService.placeOf(vehicle, null)).thenReturn(ReportService.READY);

        assertTrue(service().isShareable(vehicle));
    }

    @Test
    void saleStageCanBeSharedWhilePipelinesAreStillOpen() {
        Vehicle vehicle = vehicle("ABC");
        vehicle.setStage(VehicleStage.READY);
        when(saleListingRepository.findById("ABC")).thenReturn(Optional.empty());

        assertTrue(service().isShareable(vehicle));
        verify(reportService, never()).placeOf(any(), any());
    }

    @Test
    void soldVehicleCannotBeShared() {
        Vehicle vehicle = vehicle("ABC");
        vehicle.setStage(VehicleStage.READY);
        SaleListing listing = new SaleListing();
        listing.setSold(true);
        when(saleListingRepository.findById("ABC")).thenReturn(Optional.of(listing));

        assertFalse(service().isShareable(vehicle));
    }

    @Test
    void listedVehicleCanBeSharedAndEarlierStagesCannot() {
        Vehicle ready = vehicle("ABC");
        Vehicle workshop = vehicle("XYZ");
        when(saleListingRepository.findById("ABC")).thenReturn(Optional.empty());
        when(saleListingRepository.findById("XYZ")).thenReturn(Optional.empty());
        when(reportService.placeOf(ready, null)).thenReturn(ReportService.LISTED);
        when(reportService.placeOf(workshop, null)).thenReturn(ReportService.PREPARATION);

        VehicleShareService service = service();
        assertTrue(service.isShareable(ready));
        assertFalse(service.isShareable(workshop));
    }

    @Test
    void shareReusesTheActiveLink() {
        Vehicle vehicle = vehicle("ABC");
        VehicleShareLink existing = new VehicleShareLink();
        existing.setPublicToken("11111111-1111-1111-1111-111111111111");
        when(vehicleRepository.findById("ABC")).thenReturn(Optional.of(vehicle));
        when(saleListingRepository.findById("ABC")).thenReturn(Optional.of(new SaleListing()));
        when(reportService.placeOf(org.mockito.ArgumentMatchers.eq(vehicle), any(SaleListing.class)))
                .thenReturn(ReportService.READY);
        when(shareLinkRepository.findFirstByChassisNoAndActiveTrueOrderByIdDesc("ABC"))
                .thenReturn(Optional.of(existing));

        assertEquals(existing.getPublicToken(), service().ensureLink("ABC"));
        verify(shareLinkRepository, never()).save(any(VehicleShareLink.class));
    }

    @Test
    void shareRefusesAVehicleThatIsNotSalesReady() {
        Vehicle vehicle = vehicle("ABC");
        when(vehicleRepository.findById("ABC")).thenReturn(Optional.of(vehicle));
        when(saleListingRepository.findById("ABC")).thenReturn(Optional.empty());
        when(reportService.placeOf(vehicle, null)).thenReturn(ReportService.CUSTOMS);

        assertThrows(IllegalArgumentException.class, () -> service().ensureLink("ABC"));
    }

    @Test
    void unknownTokenDoesNotOpenAVehicle() {
        when(shareLinkRepository.findByPublicToken("11111111-1111-1111-1111-111111111111"))
                .thenReturn(Optional.empty());

        assertNull(service().publicView("11111111-1111-1111-1111-111111111111"));
        assertNull(service().publicView("not-a-token"));
    }

    @Test
    void equipmentChoicesOnlyKeepFittedOptions() {
        assertEquals("Air conditioning", VehicleShareService.featureLabel("Air conditioning", "YES"));
        assertEquals("Alloy wheels", VehicleShareService.featureLabel("Alloy wheels", "OK"));
        assertNull(VehicleShareService.featureLabel("ABS", "NO"));
        assertNull(VehicleShareService.featureLabel("Cab type", "N/A"));
    }

    @Test
    void returnPathStaysInsideTheApp() {
        assertEquals("/ready-for-sale/main/ABC", VehicleShareController.safeReturn("/ready-for-sale/main/ABC", "ABC"));
        assertEquals("/vehicle/ABC", VehicleShareController.safeReturn("https://evil.example/vehicle/ABC", "ABC"));
    }

    private VehicleShareService service() {
        return new VehicleShareService(
                shareLinkRepository,
                vehicleRepository,
                saleListingRepository,
                vehiclePhotoRepository,
                equipmentInspectionRepository,
                vehicleRegistrationRepository,
                saleLocationService,
                reportService
        );
    }

    private static Vehicle vehicle(String chassis) {
        Vehicle vehicle = new Vehicle();
        vehicle.setChassisNo(chassis);
        return vehicle;
    }
}
