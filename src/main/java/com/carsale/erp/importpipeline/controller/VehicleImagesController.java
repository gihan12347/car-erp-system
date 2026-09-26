package com.carsale.erp.importpipeline.controller;

import com.carsale.erp.importpipeline.util.ImportStageUrls;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.carsale.erp.importpipeline.util.PhotoUploadResult;
import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService.ImportProgress;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/photos")
public class VehicleImagesController {

    private final VehiclePhotoService photoService;
    private final VehicleService vehicleService;
    private final SheetDocumentStorageService documentStorageService;
    private final ImportProgressService importProgressService;
    private final CustomsProgressService customsProgressService;
    private final PipelineStageService pipelineStageService;

    public VehicleImagesController(
            VehiclePhotoService photoService,
            VehicleService vehicleService,
            SheetDocumentStorageService documentStorageService,
            ImportProgressService importProgressService,
            CustomsProgressService customsProgressService,
            PipelineStageService pipelineStageService
    ) {
        this.photoService = photoService;
        this.vehicleService = vehicleService;
        this.documentStorageService = documentStorageService;
        this.importProgressService = importProgressService;
        this.customsProgressService = customsProgressService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query) {
        if (query != null && !query.trim().isEmpty()) {
            return "redirect:/import?q=" + UriUtils.encodeQueryParam(query.trim(), StandardCharsets.UTF_8);
        }
        return "redirect:/import";
    }

    @GetMapping("/documents/{storedName:.+}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String storedName) throws IOException {
        Resource resource = documentStorageService.loadPhotoAsResource(storedName);
        String contentType = documentStorageService.resolveContentType(storedName, null);
        String encodedName = URLEncoder.encode(storedName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/new")
    public String newLot() {
        return "redirect:/auction/new";
    }

    @PostMapping("/new")
    public String saveNew(
            @RequestParam("chassisNo") String chassisNo,
            @RequestParam(value = "storedNames", required = false) List<String> storedNames,
            @RequestParam(value = "originalNames", required = false) List<String> originalNames,
            @RequestParam(value = "contentTypes", required = false) List<String> contentTypes,
            RedirectAttributes redirectAttributes
    ) {
        String trimmed = chassisNo == null ? "" : chassisNo.trim();
        if (trimmed.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Chassis number is required.");
            return "redirect:/auction/new";
        }
        Vehicle existing = vehicleService.findByChassisNo(trimmed);
        if (existing != null) {
            ImportProgress status = importProgressService.progressFor(existing);
            redirectAttributes.addFlashAttribute("notice",
                    "Chassis number " + trimmed + " is already in the import pipeline. "
                            + "No new vehicle was created — opened the existing record instead.");
            return "redirect:" + ImportStageUrls.pipelineHubUrl(
                    trimmed,
                    status,
                    pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
            );
        }
        try {
            if (storedNames == null || storedNames.isEmpty()) {
                throw new IllegalArgumentException("Upload at least one vehicle image.");
            }
            Vehicle vehicle = new Vehicle();
            vehicle.setChassisNo(trimmed);
            vehicleService.save(vehicle);
            photoService.attachPending(trimmed, storedNames, originalNames, contentTypes);
            importProgressService.syncVehicleStage(trimmed);
            customsProgressService.syncVehicleStage(trimmed);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle images saved.");
            return "redirect:" + ImportStageUrls.redirectAfterPhotosSave(
                    trimmed,
                    pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/auction/new";
        }
    }

    @GetMapping("/{chassisNo}")
    public String form(
            @PathVariable String chassisNo,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            Model model
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/import";
        }
        ImportProgress status = importProgressService.progressFor(vehicle);
        model.addAttribute("pageTitle", pipelineStageService.title(
                PipelineStageService.FLOW_IMPORT, FlowStage.PHOTOS.getStageKey()));
        model.addAttribute("activeMenu", "import");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        addPhotoModel(model, chassisNo);
        addPipelineFlags(model, status);
        model.addAttribute("stageNav", ImportStageUrls.editLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_IMPORT, FlowStage.PHOTOS.getStageKey()),
                status,
                pipelineStageService.list(PipelineStageService.FLOW_IMPORT)
        ));
        return "import-pipeline/photos/form";
    }

    @PostMapping("/upload")
    @ResponseBody
    public PhotoUploadResult upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chassisNo", required = false) String chassisNo
    ) {
        PhotoUploadResult result = new PhotoUploadResult();
        try {
            String trimmed = chassisNo == null ? "" : chassisNo.trim();
            if (!trimmed.isEmpty() && photoService.count(trimmed) >= VehiclePhotoService.MAX_PHOTOS) {
                result.setSuccess(false);
                result.setMessage("A vehicle can have at most " + VehiclePhotoService.MAX_PHOTOS + " images.");
                result.setCount(photoService.count(trimmed));
                result.setRemaining(0);
                return result;
            }
            SheetUploadResult stored = documentStorageService.storePhoto(file);
            if (!stored.isSuccess()) {
                result.setSuccess(false);
                result.setMessage(stored.getMessage());
                return result;
            }
            result.setSuccess(true);
            result.setOriginalName(stored.getOriginalName());
            result.setStoredName(stored.getStoredName());
            result.setContentType(stored.getContentType());
            result.setPreviewUrl(stored.getPreviewUrl());
            if (!trimmed.isEmpty() && vehicleService.findByChassisNo(trimmed) != null) {
                VehiclePhoto photo = photoService.add(trimmed, stored);
                result.setPhotoId(photo.getId());
                result.setCount(photoService.count(trimmed));
                result.setRemaining(Math.max(0, VehiclePhotoService.MAX_PHOTOS - result.getCount()));
                result.setMessage("Image " + result.getCount() + " of " + VehiclePhotoService.MAX_PHOTOS + " saved.");
                importProgressService.syncVehicleStage(trimmed);
            } else {
                result.setCount(0);
                result.setRemaining(VehiclePhotoService.MAX_PHOTOS);
                result.setMessage("Image uploaded. Save the vehicle to keep it.");
            }
            return result;
        } catch (Exception ex) {
            result.setSuccess(false);
            result.setMessage(ex.getMessage() == null
                    ? "Could not save the image."
                    : ex.getMessage());
            return result;
        }
    }

    @PostMapping("/{chassisNo}/photos/{photoId}/delete")
    @ResponseBody
    public PhotoUploadResult deletePhoto(
            @PathVariable String chassisNo,
            @PathVariable Long photoId
    ) {
        photoService.delete(chassisNo, photoId);
        importProgressService.syncVehicleStage(chassisNo);
        PhotoUploadResult result = new PhotoUploadResult();
        result.setSuccess(true);
        result.setCount(photoService.count(chassisNo));
        result.setRemaining(Math.max(0, VehiclePhotoService.MAX_PHOTOS - result.getCount()));
        result.setMessage("Image removed.");
        return result;
    }

    @PostMapping("/delete-stored")
    @ResponseBody
    public PhotoUploadResult deleteStored(@RequestParam("storedName") String storedName) {
        documentStorageService.deletePhotoIfExists(storedName);
        PhotoUploadResult result = new PhotoUploadResult();
        result.setSuccess(true);
        result.setMessage("Image removed.");
        return result;
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (!photoService.hasPhotos(chassisNo)) {
                throw new IllegalArgumentException("Upload at least one vehicle image.");
            }
            importProgressService.syncVehicleStage(chassisNo);
            customsProgressService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle images saved.");
            return "redirect:" + ImportStageUrls.redirectAfterPhotosSave(
                    chassisNo,
                    pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/photos/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private void addPhotoModel(Model model, String chassisNo) {
        List<VehiclePhoto> photos = photoService.list(chassisNo);
        model.addAttribute("photos", photos);
        model.addAttribute("photoCount", photos.size());
        model.addAttribute("maxPhotos", VehiclePhotoService.MAX_PHOTOS);
        model.addAttribute("photosRemaining", Math.max(0, VehiclePhotoService.MAX_PHOTOS - photos.size()));
    }

    private void addPipelineFlags(Model model, ImportProgress status) {
        model.addAttribute("auctionDocReady", status.isAuctionReady());
        model.addAttribute("preshipReady", status.isPreshipReady());
        model.addAttribute("equipmentReady", status.isEquipmentReady());
        model.addAttribute("jevicReady", status.isOdometerReady());
        model.addAttribute("coiReady", status.isCoiReady());
        model.addAttribute("standardsReady", status.isStandardsReady());
        model.addAttribute("exportReady", status.isExportReady());
        model.addAttribute("gradeReady", status.isGradeReady());
        model.addAttribute("photosReady", status.isPhotosReady());
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
