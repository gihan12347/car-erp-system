package com.carsale.erp.shared.document;

import java.io.IOException;

public final class DocumentUploads {

    private static final String DOCUMENT_FAILURE = "Could not save the document. ";

    private DocumentUploads() {
    }

    public static SheetUploadResult storeDocument(StoreAction action) {
        return store(action, DOCUMENT_FAILURE);
    }

    public static SheetUploadResult store(StoreAction action, String failurePrefix) {
        try {
            return action.save();
        } catch (IOException ex) {
            SheetUploadResult failed = new SheetUploadResult();
            failed.setSuccess(false);
            failed.setMessage(failurePrefix + ex.getMessage());
            return failed;
        }
    }

    @FunctionalInterface
    public interface StoreAction {
        SheetUploadResult save() throws IOException;
    }
}
