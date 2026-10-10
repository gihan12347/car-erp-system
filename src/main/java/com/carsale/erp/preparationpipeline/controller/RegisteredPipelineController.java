package com.carsale.erp.preparationpipeline.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.importpipeline.util.AuctionParseResult;
import com.carsale.erp.preparationpipeline.model.PreviousOwner;
import com.carsale.erp.preparationpipeline.service.PreviousOwnerService;
import com.carsale.erp.preparationpipeline.service.RegisteredProgressService;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.shared.document.document.IdCardParser;
import com.carsale.erp.shared.ocr.OcrClient;
import com.carsale.erp.shared.ocr.OcrImagePreparer;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.pipeline.PipelineStage;
import com.carsale.erp.shared.pipeline.PipelineStageService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

@Controller
public class RegisteredPipelineController {

    private final VehicleService vehicleService;
    private final VehiclePhotoService photoService;
    private final PreviousOwnerService previousOwnerService;
    private final RegisteredProgressService registeredProgressService;
    private final SheetDocumentStorageService documentStorageService;
    private final IdCardParser parser;
    private final OcrImagePreparer imagePreparer;
    private final PipelineStageService pipelineStageService;

    public RegisteredPipelineController(
            VehicleService vehicleService,
            VehiclePhotoService photoService,
            PreviousOwnerService previousOwnerService,
            RegisteredProgressService registeredProgressService,
            SheetDocumentStorageService documentStorageService,
            IdCardParser parser,
            OcrImagePreparer imagePreparer,
            PipelineStageService pipelineStageService
    ) {
        this.vehicleService = vehicleService;
        this.photoService = photoService;
        this.previousOwnerService = previousOwnerService;
        this.registeredProgressService = registeredProgressService;
        this.documentStorageService = documentStorageService;
        this.parser = parser;
        this.imagePreparer = imagePreparer;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping("/registered")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<RegisteredRow> rows = new ArrayList<RegisteredRow>();
        for (Vehicle vehicle : vehicleService.search(query)) {
            if (vehicle.getIntake() != VehicleIntake.LOCAL_REGISTERED) {
                continue;
            }
            rows.add(new RegisteredRow(vehicle, registeredProgressService));
        }
        model.addAttribute("pageTitle", "Registered vehicles");
        model.addAttribute("activeMenu", "registered");
        model.addAttribute("rows", rows);
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        return "registered/list";
    }

    @PostMapping("/registered/{chassisNo}/skip")
    public String skip(
            @PathVariable String chassisNo,
            @RequestParam("stage") String stage,
            RedirectAttributes redirectAttributes
    ) {
        if (requireRegistered(chassisNo) == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "That vehicle is not in the registered pipeline.");
            return "redirect:/registered";
        }
        try {
            registeredProgressService.skipStage(chassisNo, stage);
            redirectAttributes.addFlashAttribute("successMessage", "Stage skipped.");
            return "redirect:" + redirectAfterRegisteredSkip(chassisNo, stage);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + registeredStageUrl(chassisNo, stage);
        }
    }

    private String redirectAfterRegisteredSkip(String chassisNo, String stage) {
        String key = com.carsale.erp.shared.pipeline.SkippedStageService.canonical(stage);
        if (com.carsale.erp.shared.pipeline.SkippedStageService.REG_BOOK.equals(key)) {
            return "/registered/" + encode(chassisNo) + "/photos";
        }
        if (com.carsale.erp.shared.pipeline.SkippedStageService.REG_PHOTOS.equals(key)) {
            return "/registered/" + encode(chassisNo) + "/owner";
        }
        if (registeredProgressService.isComplete(chassisNo)) {
            return firstPreparationUrl(chassisNo);
        }
        return "/registered/" + encode(chassisNo) + "/owner";
    }

    private String registeredStageUrl(String chassisNo, String stage) {
        String key = com.carsale.erp.shared.pipeline.SkippedStageService.canonical(stage);
        if (com.carsale.erp.shared.pipeline.SkippedStageService.REG_PHOTOS.equals(key)) {
            return "/registered/" + encode(chassisNo) + "/photos";
        }
        if (com.carsale.erp.shared.pipeline.SkippedStageService.REG_OWNER.equals(key)) {
            return "/registered/" + encode(chassisNo) + "/owner";
        }
        return "/workshop-yard/" + encode(chassisNo) + "/certificate";
    }

    @PostMapping("/registered/{chassisNo}/delete")
    public String delete(@PathVariable String chassisNo, RedirectAttributes redirectAttributes) {
        Vehicle vehicle = requireRegistered(chassisNo);
        if (vehicle == null) {
            redirectAttributes.addFlashAttribute("error", "That vehicle is not in the registered pipeline.");
            return "redirect:/registered";
        }
        vehicleService.delete(chassisNo);
        redirectAttributes.addFlashAttribute("notice", "Registered vehicle deleted.");
        return "redirect:/registered";
    }

    @GetMapping("/registered/{chassisNo}/photos")
    public String photos(@PathVariable String chassisNo, Model model) {
        Vehicle vehicle = requireRegistered(chassisNo);
        if (vehicle == null) {
            return "redirect:/registered";
        }
        int count = photoService.count(chassisNo);
        model.addAttribute("pageTitle", "Images");
        model.addAttribute("activeMenu", "registered");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("photos", photoService.list(chassisNo));
        model.addAttribute("photoCount", count);
        model.addAttribute("maxPhotos", VehiclePhotoService.MAX_PHOTOS);
        model.addAttribute("photosRemaining", Math.max(0, VehiclePhotoService.MAX_PHOTOS - count));
        model.addAttribute("progress", new RegisteredRow(vehicle, registeredProgressService));
        return "registered/photos";
    }

    @PostMapping("/registered/{chassisNo}/photos")
    public String savePhotos(@PathVariable String chassisNo, RedirectAttributes redirectAttributes) {
        if (requireRegistered(chassisNo) == null) {
            return "redirect:/registered";
        }
        if (photoService.count(chassisNo) < 1) {
            redirectAttributes.addFlashAttribute("errorMessage", "Add at least one image.");
            return "redirect:/registered/" + encode(chassisNo) + "/photos";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Images saved.");
        return "redirect:/registered/" + encode(chassisNo) + "/owner";
    }

    @GetMapping("/registered/{chassisNo}/owner")
    public String owner(@PathVariable String chassisNo, Model model) {
        Vehicle vehicle = requireRegistered(chassisNo);
        if (vehicle == null) {
            return "redirect:/registered";
        }
        if (!model.containsAttribute("owner")) {
            model.addAttribute("owner", previousOwnerService.prepareForm(chassisNo));
        }
        model.addAttribute("pageTitle", "Previous owner");
        model.addAttribute("activeMenu", "registered");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("progress", new RegisteredRow(vehicle, registeredProgressService));
        return "registered/owner";
    }

    @PostMapping("/registered/{chassisNo}/owner")
    public String saveOwner(
            @PathVariable String chassisNo,
            @ModelAttribute("owner") PreviousOwner owner,
            RedirectAttributes redirectAttributes
    ) {
        if (requireRegistered(chassisNo) == null) {
            return "redirect:/registered";
        }
        owner.setChassisNo(chassisNo);
        try {
            if ((owner.getFullName() == null || owner.getFullName().trim().isEmpty())
                    && (owner.getNicNo() == null || owner.getNicNo().trim().isEmpty())) {
                throw new IllegalArgumentException("Enter the owner name or NIC number.");
            }
            previousOwnerService.save(owner);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Previous owner saved. Continue in the preparation pipeline.");
            return "redirect:" + firstPreparationUrl(chassisNo);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("owner", owner);
            return "redirect:/registered/" + encode(chassisNo) + "/owner";
        }
    }

    @GetMapping("/registered/documents/{storedName:.+}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String storedName) throws IOException {
        Resource resource = documentStorageService.loadIdCardAsResource(storedName);
        String contentType = documentStorageService.resolveContentType(storedName, null);
        String encodedName = URLEncoder.encode(storedName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping("/registered/upload-document")
    @ResponseBody
    public SheetUploadResult uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            return documentStorageService.storeIdCard(file);
        } catch (IOException ex) {
            SheetUploadResult failed = new SheetUploadResult();
            failed.setSuccess(false);
            failed.setMessage("Could not save the identity card. " + ex.getMessage());
            return failed;
        }
    }

    @PostMapping("/registered/parse-id")
    @ResponseBody
    public AuctionParseResult parseId(@RequestParam("file") MultipartFile file) {
        return imagePreparer.parsePage(file, parser, OcrClient.GOOGLE);
    }

    private Vehicle requireRegistered(String chassisNo) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null || vehicle.getIntake() != VehicleIntake.LOCAL_REGISTERED) {
            return null;
        }
        return vehicle;
    }

    private String firstPreparationUrl(String chassisNo) {
        List<PipelineStage> stages = pipelineStageService.list(PipelineStageService.FLOW_PREP);
        String stageKey = stages.isEmpty()
                ? FlowStage.INSPECTION.getStageKey()
                : stages.get(0).getStageKey();
        String url = PipelineStageService.editUrlFor(encode(chassisNo), stageKey);
        return url == null ? "/workshop-yard" : url;
    }

    private static String encode(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }

    public static final class RegisteredRow {
        private final Vehicle vehicle;
        private final boolean bookReady;
        private final boolean photosReady;
        private final boolean ownerReady;

        private final boolean bookFilled;
        private final boolean photosFilled;
        private final boolean ownerFilled;
        private final boolean bookSkipped;
        private final boolean photosSkipped;
        private final boolean ownerSkipped;

        public RegisteredRow(Vehicle vehicle, RegisteredProgressService progress) {
            this.vehicle = vehicle;
            String chassis = vehicle.getChassisNo();
            this.bookFilled = progress.isBookFilled(chassis);
            this.photosFilled = progress.isPhotosFilled(chassis);
            this.ownerFilled = progress.isOwnerFilled(chassis);
            this.bookSkipped = progress.isBookSkipped(chassis);
            this.photosSkipped = progress.isPhotosSkipped(chassis);
            this.ownerSkipped = progress.isOwnerSkipped(chassis);
            this.bookReady = bookFilled || bookSkipped;
            this.photosReady = photosFilled || photosSkipped;
            this.ownerReady = ownerFilled || ownerSkipped;
        }

        public Vehicle getVehicle() { return vehicle; }
        public boolean isBookReady() { return bookReady; }
        public boolean isPhotosReady() { return photosReady; }
        public boolean isOwnerReady() { return ownerReady; }
        public boolean isBookFilled() { return bookFilled; }
        public boolean isPhotosFilled() { return photosFilled; }
        public boolean isOwnerFilled() { return ownerFilled; }
        public boolean isBookSkipped() { return bookSkipped; }
        public boolean isPhotosSkipped() { return photosSkipped; }
        public boolean isOwnerSkipped() { return ownerSkipped; }
        public boolean isComplete() { return bookReady && photosReady && ownerReady; }
    }
}
