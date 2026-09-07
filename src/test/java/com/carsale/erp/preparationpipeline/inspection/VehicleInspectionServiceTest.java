package com.carsale.erp.preparationpipeline.inspection;

import com.carsale.erp.preparationpipeline.workshop.WorkshopService;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.preparationpipeline.inspection.InspectionFailRequest;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.preparationpipeline.inspection.VehicleInspection;
import com.carsale.erp.preparationpipeline.inspection.VehicleInspectionLine;
import com.carsale.erp.preparationpipeline.workshop.WorkshopJob;
import com.carsale.erp.preparationpipeline.workshop.WorkshopJobLine;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@SpringBootTest
@Transactional
class VehicleInspectionServiceTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleInspectionService vehicleInspectionService;

    @Autowired
    private WorkshopService workshopService;

    @Test
    void prepareFormLoadsCatalogItems() {
        saveVehicle("TEST-INS-CAT");
        VehicleInspection form = vehicleInspectionService.prepareForm("TEST-INS-CAT");
        assertThat(form).isNotNull();
        assertThat(form.getLines()).isNotEmpty();
        assertThat(form.getLines()).extracting(VehicleInspectionLine::getItemTitle)
                .contains("Engine", "Brakes");
    }

    @Test
    void saveDoesNotCreateWorkshopJobForNo() {
        saveVehicle("TEST-INS-SAVE");
        VehicleInspection form = vehicleInspectionService.prepareForm("TEST-INS-SAVE");
        form.setInspector("Kasun");
        form.setInspectionDate("2026-08-30");
        for (VehicleInspectionLine line : form.getLines()) {
            line.setResult("OK");
        }
        lineNamed(form, "Brakes").setResult("NO");
        vehicleInspectionService.save(form);
        assertThat(workshopService.findByChassisNo("TEST-INS-SAVE")).isNull();
    }

    @Test
    void noClickCreatesWorkshopJobOnce() {
        saveVehicle("TEST-INS-NO");
        InspectionFailRequest request = new InspectionFailRequest();
        request.setItemKey("brakes");
        request.setItemTitle("Brakes");
        request.setCatalogItem(true);

        vehicleInspectionService.recordNoSelection("TEST-INS-NO", request);
        WorkshopJob job = workshopService.findByChassisNo("TEST-INS-NO");
        assertThat(job).isNotNull();
        assertThat(job.isCompleted()).isFalse();
        assertThat(job.getLines()).anySatisfy(line -> {
            assertThat(line.getInspectionItemKey()).isEqualTo("brakes");
            assertThat(line.getJobSummary()).isEqualTo("Inspection fail: Brakes");
        });

        int count = countInspectionJobs(job, "brakes");
        vehicleInspectionService.recordNoSelection("TEST-INS-NO", request);
        job = workshopService.findByChassisNo("TEST-INS-NO");
        assertThat(countInspectionJobs(job, "brakes")).isEqualTo(count);
        assertThat(workshopService.canEnterYard("TEST-INS-NO")).isFalse();
    }

    @Test
    void okResultDoesNotCreateWorkshopJob() {
        saveVehicle("TEST-INS-OK");
        VehicleInspection form = vehicleInspectionService.prepareForm("TEST-INS-OK");
        form.setInspector("Kasun");
        form.setInspectionDate("2026-08-30");
        for (VehicleInspectionLine line : form.getLines()) {
            line.setResult("OK");
        }
        vehicleInspectionService.save(form);
        assertThat(workshopService.findByChassisNo("TEST-INS-OK")).isNull();
    }

    private void saveVehicle(String chassisNo) {
        Vehicle vehicle = new Vehicle();
        vehicle.setChassisNo(chassisNo);
        vehicleRepository.save(vehicle);
    }

    private static VehicleInspectionLine lineNamed(VehicleInspection form, String title) {
        for (VehicleInspectionLine line : form.getLines()) {
            if (title.equals(line.getItemTitle())) {
                return line;
            }
        }
        return null;
    }

    private static int countInspectionJobs(WorkshopJob job, String itemKey) {
        int count = 0;
        for (WorkshopJobLine line : job.getLines()) {
            if (itemKey.equals(line.getInspectionItemKey())) {
                count++;
            }
        }
        return count;
    }
}
