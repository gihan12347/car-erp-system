package com.carsale.erp.preparationpipeline.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

}
