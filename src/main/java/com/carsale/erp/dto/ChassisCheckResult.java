package com.carsale.erp.dto;

public class ChassisCheckResult {

    private boolean exists;
    private String chassisNo;
    private String vehicleLabel;
    private String pipelineUrl;
    private String message;
    private int completedSteps;

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public String getChassisNo() {
        return chassisNo;
    }

    public void setChassisNo(String chassisNo) {
        this.chassisNo = chassisNo;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public void setVehicleLabel(String vehicleLabel) {
        this.vehicleLabel = vehicleLabel;
    }

    public String getPipelineUrl() {
        return pipelineUrl;
    }

    public void setPipelineUrl(String pipelineUrl) {
        this.pipelineUrl = pipelineUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(int completedSteps) {
        this.completedSteps = completedSteps;
    }
}
