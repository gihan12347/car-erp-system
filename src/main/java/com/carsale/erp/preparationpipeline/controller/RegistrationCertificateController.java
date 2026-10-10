package com.carsale.erp.preparationpipeline.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.preparationpipeline.model.RegistrationCertificate;
import com.carsale.erp.preparationpipeline.controller.RegisteredPipelineController.RegisteredRow;
import com.carsale.erp.preparationpipeline.service.RegisteredProgressService;
import com.carsale.erp.preparationpipeline.service.RegistrationCertificateService;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.shared.document.document.RegistrationCertificateParser;
import com.carsale.erp.shared.ocr.OcrClient;
import com.carsale.erp.shared.ocr.OcrImagePreparer;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleIntake;
import com.carsale.erp.shared.vehicle.VehicleService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

@Controller
@RequestMapping("/workshop-yard")
public class RegistrationCertificateController {

    private final VehicleService vehicleService;
    private final RegistrationCertificateService certificateService;
    private final RegisteredProgressService registeredProgressService;
    private final SheetDocumentStorageService documentStorageService;
    private final RegistrationCertificateParser parser;
    private final OcrImagePreparer imagePreparer;

    public RegistrationCertificateController(
            VehicleService vehicleService,
            RegistrationCertificateService certificateService,
            RegisteredProgressService registeredProgressService,
            SheetDocumentStorageService documentStorageService,
            RegistrationCertificateParser parser,
            OcrImagePreparer imagePreparer
    ) {
        this.vehicleService = vehicleService;
        this.certificateService = certificateService;
        this.registeredProgressService = registeredProgressService;
        this.documentStorageService = documentStorageService;
        this.parser = parser;
        this.imagePreparer = imagePreparer;
    }

    @GetMapping("/documents/{storedName:.+}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String storedName) throws IOException {
        Resource resource = documentStorageService.loadRegistrationCertificateAsResource(storedName);
        String contentType = documentStorageService.resolveContentType(storedName, null);
        String encodedName = URLEncoder.encode(storedName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping("/upload-document")
    @ResponseBody
    public SheetUploadResult uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            return documentStorageService.storeRegistrationCertificate(file);
        } catch (IOException ex) {
            SheetUploadResult failed = new SheetUploadResult();
            failed.setSuccess(false);
            failed.setMessage("Could not save the document. " + ex.getMessage());
            return failed;
        }
    }

    @PostMapping("/parse-certificate")
    @ResponseBody
    public AuctionParseResult parseCertificate(@RequestParam("file") MultipartFile file) {
        return imagePreparer.parsePage(file, parser, OcrClient.GOOGLE);
    }

    @GetMapping("/{chassisNo}/certificate")
    public String edit(@PathVariable String chassisNo, Model model) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/registered";
        }
        if (vehicle.getIntake() != VehicleIntake.LOCAL_REGISTERED) {
            return "redirect:/registration/" + UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
        }
        RegistrationCertificate certificate = certificateService.prepareForm(chassisNo);
        model.addAttribute("pageTitle", "Registration certificate");
        model.addAttribute("activeMenu", "registered");
        model.addAttribute("certificate", certificate);
        model.addAttribute("editing", true);
        model.addAttribute("progress", new RegisteredRow(vehicle, registeredProgressService));
        return "preparation-pipeline/new";
    }

    @PostMapping("/{chassisNo}/certificate")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute("certificate") RegistrationCertificate certificate,
            RedirectAttributes redirectAttributes
    ) {
        certificate.setChassisNo(chassisNo);
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle != null && vehicle.getIntake() != VehicleIntake.LOCAL_REGISTERED) {
            return "redirect:/registration/" + UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
        }
        try {
            certificateService.save(certificate);
            redirectAttributes.addFlashAttribute("notice", "Registration certificate saved.");
            return "redirect:/workshop-yard/" + UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8)
                    + "/certificate";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("certificate", certificate);
            return "redirect:/workshop-yard/" + UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8)
                    + "/certificate";
        }
    }
}
