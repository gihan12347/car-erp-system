package com.carsale.erp.shared.vehicle;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChassisCheckResult {

    private boolean exists;
    private String chassisNo;
    private String vehicleLabel;
    private String pipelineUrl;
    private String message;
    private int completedSteps;
}
