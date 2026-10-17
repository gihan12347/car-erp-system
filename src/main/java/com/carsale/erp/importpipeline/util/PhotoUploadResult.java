package com.carsale.erp.importpipeline.util;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhotoUploadResult {

    private boolean success;
    private String message;
    private Long photoId;
    private String originalName;
    private String storedName;
    private String contentType;
    private String previewUrl;
    private int count;
    private int remaining;
}
