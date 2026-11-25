package com.carsale.erp.shared.document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SheetUploadResult {

    private boolean success;
    private String message;
    private String originalName;
    private String storedName;
    private String contentType;
    private String previewUrl;
    private boolean pdf;
}
