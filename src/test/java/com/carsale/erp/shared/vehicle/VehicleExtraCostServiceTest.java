package com.carsale.erp.shared.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleExtraCostServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleExtraCostRepository extraCostRepository;

    @InjectMocks
    private VehicleExtraCostService service;

    @Test
    void addStoresDateReasonAndCost() {
        when(vehicleRepository.findById("JB64")).thenReturn(Optional.of(vehicle("JB64")));

        service.add("JB64", "2026-11-26", "  Towing  ", " 1,500.50 ");

        ArgumentCaptor<VehicleExtraCost> captor = ArgumentCaptor.forClass(VehicleExtraCost.class);
        verify(extraCostRepository).save(captor.capture());
        VehicleExtraCost saved = captor.getValue();
        assertThat(saved.getChassisNo()).isEqualTo("JB64");
        assertThat(saved.getCostDate()).isEqualTo("2026-11-26");
        assertThat(saved.getReason()).isEqualTo("Towing");
        assertThat(saved.getAmount()).isEqualTo("1,500.50");
    }

    @Test
    void addAcceptsTheSlashDateShownInTheForm() {
        when(vehicleRepository.findById("JB64")).thenReturn(Optional.of(vehicle("JB64")));

        service.add("JB64", "2026/11/26", "Air bag replace", "50,000");

        ArgumentCaptor<VehicleExtraCost> captor = ArgumentCaptor.forClass(VehicleExtraCost.class);
        verify(extraCostRepository).save(captor.capture());
        assertThat(captor.getValue().getCostDate()).isEqualTo("2026-11-26");
        assertThat(captor.getValue().getReason()).isEqualTo("Air bag replace");
    }

    @Test
    void addRejectsAMissingField() {
        when(vehicleRepository.findById("JB64")).thenReturn(Optional.of(vehicle("JB64")));

        assertThatThrownBy(() -> service.add("JB64", "", "Towing", "100"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Enter the date.");
        assertThatThrownBy(() -> service.add("JB64", "2026-11-26", "  ", "100"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Enter the reason.");
        assertThatThrownBy(() -> service.add("JB64", "2026-11-26", "Towing", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Enter the cost.");
    }

    @Test
    void totalAddsEveryExtraCost() {
        VehicleExtraCost first = new VehicleExtraCost();
        first.setAmount("1,000");
        VehicleExtraCost second = new VehicleExtraCost();
        second.setAmount("250.50");

        assertThat(VehicleExtraCostService.total(Arrays.asList(first, second)))
                .isEqualByComparingTo(new BigDecimal("1250.50"));
    }

    private static Vehicle vehicle(String chassisNo) {
        Vehicle vehicle = new Vehicle();
        vehicle.setChassisNo(chassisNo);
        return vehicle;
    }
}
