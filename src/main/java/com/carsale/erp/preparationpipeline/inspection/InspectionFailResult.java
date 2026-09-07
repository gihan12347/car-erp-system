package com.carsale.erp.preparationpipeline.inspection;

public class InspectionFailResult {

    private boolean success;
    private boolean created;
    private String message;

    public static InspectionFailResult ok(boolean created, String message) {
        InspectionFailResult result = new InspectionFailResult();
        result.success = true;
        result.created = created;
        result.message = message;
        return result;
    }

    public static InspectionFailResult fail(String message) {
        InspectionFailResult result = new InspectionFailResult();
        result.success = false;
        result.message = message;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
