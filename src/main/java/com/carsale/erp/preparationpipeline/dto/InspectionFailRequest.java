package com.carsale.erp.preparationpipeline.dto;

import lombok.Data;

@Data
public class InspectionFailRequest {

    private Long lineId;
    private String itemKey;
    private String itemTitle;
    private String notes;
    private boolean catalogItem;
    private String result;
}
