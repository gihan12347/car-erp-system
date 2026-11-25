package com.carsale.erp.shared.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import com.carsale.erp.shared.utils.Utils;

class DocumentResponseSupportTest {

    @Test
    void servesTheStoredFileInlineWithAnEncodedName() throws IOException {
        ByteArrayResource resource = new ByteArrayResource(new byte[] {1});
        ResponseEntity<?> response = InlineDocumentResponse.of(resource, "a b.pdf", "application/pdf");

        assertSame(resource, response.getBody());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertEquals(
                "inline; filename=\"a%20b.pdf\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
    }

    @Test
    void reportsTheSameDocumentSaveFailure() {
        SheetUploadResult failed = DocumentUploads.storeDocument(() -> {
            throw new IOException("disk full");
        });

        assertFalse(failed.isSuccess());
        assertEquals("Could not save the document. disk full", failed.getMessage());
    }

    @Test
    void keepsADifferentFailurePrefix() {
        SheetUploadResult failed = DocumentUploads.store(() -> {
            throw new IOException("disk full");
        }, "Could not save the identity card. ");

        assertEquals("Could not save the identity card. disk full", failed.getMessage());
    }

    @Test
    void returnsTheStoredResultWhenTheSaveSucceeds() throws IOException {
        SheetUploadResult stored = new SheetUploadResult();
        stored.setSuccess(true);
        stored.setStoredName("file.pdf");

        assertSame(stored, DocumentUploads.storeDocument(() -> stored));
        assertTrue(stored.isSuccess());
    }

    @Test
    void mapsADocumentAiTypeToTheSameField() {
        Map<String, String> typeToField = new HashMap<String, String>();
        typeToField.put("chassis_no", "chassisNo");
        typeToField.put("date", "issueDate");

        assertEquals("chassisNo", Utils.mapDocumentTypeToField("Chassis No", typeToField));
        assertEquals("issueDate", Utils.mapDocumentTypeToField("custom/date", typeToField));
        assertEquals(null, Utils.mapDocumentTypeToField(" ", typeToField));
        assertEquals(null, Utils.mapDocumentTypeToField("missing", Collections.<String, String>emptyMap()));
    }
}
