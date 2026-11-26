package com.carsale.erp.importpipeline.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carsale.erp.customspipeline.model.CustomsDocument;
import com.carsale.erp.customspipeline.repository.CustomsDocumentRepository;
import com.carsale.erp.customspipeline.service.CustomsDocumentService;
import com.carsale.erp.importpipeline.model.ExportCertificate;
import com.carsale.erp.importpipeline.model.InspectionCertificate;
import com.carsale.erp.importpipeline.repository.ExportCertificateRepository;
import com.carsale.erp.importpipeline.repository.InspectionCertificateRepository;
import com.carsale.erp.importpipeline.service.CertificateOfInspectionService;
import com.carsale.erp.importpipeline.service.ExportCertificateService;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuctionLotFieldSyncTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CustomsDocumentRepository customsDocumentRepository;

    @Mock
    private ExportCertificateRepository exportCertificateRepository;

    @Mock
    private InspectionCertificateRepository inspectionCertificateRepository;

    @Mock
    private SheetDocumentStorageService documentStorageService;

    private CustomsDocumentService customsDocumentService;
    private ExportCertificateService exportCertificateService;
    private CertificateOfInspectionService certificateOfInspectionService;

    @BeforeEach
    void setUp() {
        customsDocumentService = new CustomsDocumentService(
                vehicleRepository, customsDocumentRepository, documentStorageService);
        exportCertificateService = new ExportCertificateService(
                vehicleRepository, exportCertificateRepository, documentStorageService);
        certificateOfInspectionService = new CertificateOfInspectionService(
                vehicleRepository, inspectionCertificateRepository, documentStorageService);
    }

    @Test
    void odometerMileageUpdatesAuctionLotMileage() {
        Vehicle vehicle = vehicle("SYNC-ODO");
        vehicle.setMileage("1 km");
        when(vehicleRepository.findById("SYNC-ODO")).thenReturn(Optional.of(vehicle));
        when(customsDocumentRepository.findById("SYNC-ODO")).thenReturn(Optional.empty());
        when(customsDocumentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CustomsDocument certificate = new CustomsDocument();
        certificate.setChassisNo("SYNC-ODO");
        certificate.setJevicCurrentOdometer("  86,420 km  ");
        customsDocumentService.saveOdometerCertificate(certificate);

        assertThat(vehicle.getMileage()).isEqualTo("86,420 km");
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void blankOdometerMileageLeavesAuctionLotMileage() {
        Vehicle vehicle = vehicle("SYNC-ODO-BLANK");
        vehicle.setMileage("12,000 km");
        when(vehicleRepository.findById("SYNC-ODO-BLANK")).thenReturn(Optional.of(vehicle));
        when(customsDocumentRepository.findById("SYNC-ODO-BLANK")).thenReturn(Optional.empty());
        when(customsDocumentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CustomsDocument certificate = new CustomsDocument();
        certificate.setChassisNo("SYNC-ODO-BLANK");
        certificate.setJevicCurrentOdometer("   ");
        customsDocumentService.saveOdometerCertificate(certificate);

        assertThat(vehicle.getMileage()).isEqualTo("12,000 km");
    }

    @Test
    void exportCertificateUpdatesFuelAndDimensions() {
        Vehicle vehicle = vehicle("SYNC-EXPORT");
        vehicle.setFuel("Diesel");
        vehicle.setLengthCm("1");
        vehicle.setWidthCm("2");
        vehicle.setHeightCm("3");
        when(vehicleRepository.findById("SYNC-EXPORT")).thenReturn(Optional.of(vehicle));
        when(exportCertificateRepository.findById("SYNC-EXPORT")).thenReturn(Optional.empty());
        when(exportCertificateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExportCertificate certificate = new ExportCertificate();
        certificate.setChassisNo("SYNC-EXPORT");
        certificate.setFuelType("Petrol");
        certificate.setLengthCm("339cm");
        certificate.setWidthCm("147cm");
        certificate.setHeightCm("164cm");
        exportCertificateService.save(certificate);

        assertThat(vehicle.getFuel()).isEqualTo("Petrol");
        assertThat(vehicle.getLengthCm()).isEqualTo("339cm");
        assertThat(vehicle.getWidthCm()).isEqualTo("147cm");
        assertThat(vehicle.getHeightCm()).isEqualTo("164cm");
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void exportCertificateUpdatesSeatingCapacity() {
        Vehicle vehicle = vehicle("SYNC-SEATS");
        vehicle.setSeats("2 people");
        when(vehicleRepository.findById("SYNC-SEATS")).thenReturn(Optional.of(vehicle));
        when(exportCertificateRepository.findById("SYNC-SEATS")).thenReturn(Optional.empty());
        when(exportCertificateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExportCertificate certificate = new ExportCertificate();
        certificate.setChassisNo("SYNC-SEATS");
        certificate.setSeatingCapacity("  5  ");
        exportCertificateService.save(certificate);

        assertThat(vehicle.getSeats()).isEqualTo("5");
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void blankSeatingCapacityLeavesAuctionLotSeats() {
        Vehicle vehicle = vehicle("SYNC-SEATS-BLANK");
        vehicle.setSeats("5 people");
        when(vehicleRepository.findById("SYNC-SEATS-BLANK")).thenReturn(Optional.of(vehicle));
        when(exportCertificateRepository.findById("SYNC-SEATS-BLANK")).thenReturn(Optional.empty());
        when(exportCertificateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExportCertificate certificate = new ExportCertificate();
        certificate.setChassisNo("SYNC-SEATS-BLANK");
        certificate.setSeatingCapacity("   ");
        exportCertificateService.save(certificate);

        assertThat(vehicle.getSeats()).isEqualTo("5 people");
    }

    @Test
    void worksheetManufactureDateUpdatesFirstRegistration() {
        Vehicle vehicle = vehicle("SYNC-YEAR");
        vehicle.setYear("January 2020");
        when(vehicleRepository.findById("SYNC-YEAR")).thenReturn(Optional.of(vehicle));
        when(customsDocumentRepository.findById("SYNC-YEAR")).thenReturn(Optional.empty());
        when(customsDocumentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CustomsDocument worksheet = new CustomsDocument();
        worksheet.setChassisNo("SYNC-YEAR");
        worksheet.setWorksheetManufactureDate("  2024-11-11  ");
        customsDocumentService.saveWorksheet(worksheet);

        assertThat(vehicle.getYear()).isEqualTo("2024-11-11");
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void blankManufactureDateLeavesFirstRegistration() {
        Vehicle vehicle = vehicle("SYNC-YEAR-BLANK");
        vehicle.setYear("January 2020");
        when(vehicleRepository.findById("SYNC-YEAR-BLANK")).thenReturn(Optional.of(vehicle));
        when(customsDocumentRepository.findById("SYNC-YEAR-BLANK")).thenReturn(Optional.empty());
        when(customsDocumentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CustomsDocument worksheet = new CustomsDocument();
        worksheet.setChassisNo("SYNC-YEAR-BLANK");
        worksheet.setWorksheetManufactureDate("   ");
        customsDocumentService.saveWorksheet(worksheet);

        assertThat(vehicle.getYear()).isEqualTo("January 2020");
    }

    @Test
    void blankExportFieldsLeaveAuctionLotValues() {
        Vehicle vehicle = vehicle("SYNC-EXPORT-BLANK");
        vehicle.setFuel("Petrol");
        vehicle.setLengthCm("339cm");
        vehicle.setWidthCm("147cm");
        vehicle.setHeightCm("164cm");
        when(vehicleRepository.findById("SYNC-EXPORT-BLANK")).thenReturn(Optional.of(vehicle));
        when(exportCertificateRepository.findById("SYNC-EXPORT-BLANK")).thenReturn(Optional.empty());
        when(exportCertificateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExportCertificate certificate = new ExportCertificate();
        certificate.setChassisNo("SYNC-EXPORT-BLANK");
        certificate.setFuelType("");
        certificate.setLengthCm("440cm");
        exportCertificateService.save(certificate);

        assertThat(vehicle.getFuel()).isEqualTo("Petrol");
        assertThat(vehicle.getLengthCm()).isEqualTo("440cm");
        assertThat(vehicle.getWidthCm()).isEqualTo("147cm");
        assertThat(vehicle.getHeightCm()).isEqualTo("164cm");
    }

    @Test
    void inspectionCertificateUpdatesEngineNameAndModel() {
        Vehicle vehicle = vehicle("SYNC-COI");
        vehicle.setEngineSize("1000");
        vehicle.setModel("OLD");
        vehicle.setModelCode("OLD-CODE");
        when(vehicleRepository.findById("SYNC-COI")).thenReturn(Optional.of(vehicle));
        when(inspectionCertificateRepository.findById("SYNC-COI")).thenReturn(Optional.empty());
        when(inspectionCertificateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InspectionCertificate certificate = new InspectionCertificate();
        certificate.setChassisNo("SYNC-COI");
        certificate.setMake("Suzuki");
        certificate.setModel("Every");
        certificate.setEngineCapacity("650");
        certificateOfInspectionService.save(certificate);

        assertThat(vehicle.getEngineSize()).isEqualTo("650");
        assertThat(vehicle.getModel()).isEqualTo("Suzuki Every");
        assertThat(vehicle.getModelCode()).isEqualTo("Every");
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void inspectionCertificateKeepsVehicleNameWhenModelAlreadyIncludesMake() {
        Vehicle vehicle = vehicle("SYNC-COI-NAME");
        vehicle.setModel("Suzuki Every");
        when(vehicleRepository.findById("SYNC-COI-NAME")).thenReturn(Optional.of(vehicle));
        when(inspectionCertificateRepository.findById("SYNC-COI-NAME")).thenReturn(Optional.empty());
        when(inspectionCertificateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InspectionCertificate certificate = new InspectionCertificate();
        certificate.setChassisNo("SYNC-COI-NAME");
        certificate.setMake("Suzuki");
        certificate.setModel("Suzuki Every");
        certificateOfInspectionService.save(certificate);

        assertThat(vehicle.getModel()).isEqualTo("Suzuki Every");
        assertThat(vehicle.getModelCode()).isEqualTo("Suzuki Every");
    }

    @Test
    void inspectionCertificateWithoutMakeDoesNotChangeVehicleName() {
        Vehicle vehicle = vehicle("SYNC-COI-MODEL");
        vehicle.setModel("Carry");
        vehicle.setModelCode("");
        when(vehicleRepository.findById("SYNC-COI-MODEL")).thenReturn(Optional.of(vehicle));
        when(inspectionCertificateRepository.findById("SYNC-COI-MODEL")).thenReturn(Optional.empty());
        when(inspectionCertificateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InspectionCertificate certificate = new InspectionCertificate();
        certificate.setChassisNo("SYNC-COI-MODEL");
        certificate.setModel("DA16T");
        certificateOfInspectionService.save(certificate);

        assertThat(vehicle.getModel()).isEqualTo("Carry");
        assertThat(vehicle.getModelCode()).isEqualTo("DA16T");
        assertThat(vehicle.getEngineSize()).isNull();
    }

    private static Vehicle vehicle(String chassisNo) {
        Vehicle vehicle = new Vehicle();
        vehicle.setChassisNo(chassisNo);
        return vehicle;
    }
}
