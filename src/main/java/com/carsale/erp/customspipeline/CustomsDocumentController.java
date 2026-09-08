package com.carsale.erp.customspipeline;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.pipeline.PipelineStageService;

/**
 * Legacy routes for customs documents and old edit URLs.
 * Stage edit/save/upload lives on declaration / assessment / worksheet controllers.
 */
@Controller
public class CustomsDocumentController {

    private final SheetDocumentStorageService documentStorageService;

    public CustomsDocumentController(SheetDocumentStorageService documentStorageService) {
        this.documentStorageService = documentStorageService;
    }

    @GetMapping("/customs/documents/{storedName:.+}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String storedName) throws IOException {
        Resource resource = documentStorageService.loadClearanceAsResource(storedName);
        String contentType = documentStorageService.resolveContentType(storedName, null);
        String encodedName = URLEncoder.encode(storedName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/customs/{chassisNo}/edit")
    public String legacyForm(
            @PathVariable String chassisNo,
            @RequestParam(value = "tab", required = false) Integer tab
    ) {
        if (tab != null && tab == 1) {
            return "redirect:/jevic/" + encodeChassis(chassisNo);
        }
        String stageKey = FlowStage.DECLARATION.getStageKey();
        if (tab != null && tab == 3) {
            stageKey = FlowStage.ASSESSMENT.getStageKey();
        } else if (tab != null && tab == 4) {
            stageKey = FlowStage.WORKSHEET.getStageKey();
        }
        return "redirect:" + PipelineStageService.editUrlFor(encodeChassis(chassisNo), stageKey);
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
