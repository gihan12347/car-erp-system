package com.carsale.erp.importpipeline.controller;

import com.carsale.erp.shared.web.ListSearchRedirect;
import com.carsale.erp.shared.document.InlineDocumentResponse;
import com.carsale.erp.importpipeline.util.ImportStageSave;
import com.carsale.erp.importpipeline.util.ImportStageUrls;
import java.io.IOException;
import java.util.List;

import com.carsale.erp.importpipeline.util.PhotoUploadResult;
import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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

import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.customspipeline.service.CustomsProgressService;
import com.carsale.erp.importpipeline.service.ImportProgressService;
import com.carsale.erp.shared.dto.ImportProgress;
import com.carsale.erp.shared.service.PipelineStageService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/photos")
public class VehicleImagesController {

    private final VehiclePhotoService photoService;
    private final VehicleService vehicleService;
    private final SheetDocumentStorageService documentStorageService;
    private final ImportProgressService importProgressService;
    private final CustomsProgressService customsProgressService;
    private final PipelineStageService pipelineStageService;

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query) {
        return ListSearchRedirect.to("/import", query);
    }

    @GetMapping("/documents/{storedName:.+}")
    public ResponseEntity<Resource> serveDocument(@PathVariable String storedName) throws IOException {
        Resource resource = documentStorageService.loadPhotoAsResource(storedName);
        String contentType = documentStorageService.resolveContentType(storedName, null);
        return InlineDocumentResponse.of(resource, storedName, contentType);
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
        String missingChassis = ImportStageSave.requireChassis(trimmed, null, redirectAttributes);
        if (missingChassis != null) {
            return missingChassis;
        }
        return ImportStageSave.openExistingOrCreate(
                trimmed,
                null,
                redirectAttributes,
                vehicleService,
                importProgressService,
                customsProgressService,
                pipelineStageService,
                () -> {
                    if (storedNames == null || storedNames.isEmpty()) {
                        throw new IllegalArgumentException("Upload at least one vehicle image.");
                    }
                },
                null,
                () -> photoService.attachPending(trimmed, storedNames, originalNames, contentTypes),
                "Vehicle images saved.",
                keys -> ImportStageUrls.redirectAfterPhotosSave(trimmed, keys)
        );
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
        PipelineStageUtils.addPipelineFlags(model, status);
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
        return ImportStageSave.saveAndSync(
                chassisNo,
                hub,
                "/photos/",
                redirectAttributes,
                () -> {
                    if (!photoService.hasPhotos(chassisNo)) {
                        throw new IllegalArgumentException("Upload at least one vehicle image.");
                    }
                },
                importProgressService,
                customsProgressService,
                "Vehicle images saved.",
                () -> ImportStageUrls.redirectAfterPhotosSave(
                        chassisNo,
                        pipelineStageService.keys(PipelineStageService.FLOW_IMPORT)
                )
        );
    }

    private void addPhotoModel(Model model, String chassisNo) {
        List<VehiclePhoto> photos = photoService.list(chassisNo);
        model.addAttribute("photos", photos);
        model.addAttribute("photoCount", photos.size());
        model.addAttribute("maxPhotos", VehiclePhotoService.MAX_PHOTOS);
        model.addAttribute("photosRemaining", Math.max(0, VehiclePhotoService.MAX_PHOTOS - photos.size()));
    }
}
