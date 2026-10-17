package com.carsale.erp.preparationpipeline.controller;

import java.util.ArrayList;
import java.util.List;

import com.carsale.erp.preparationpipeline.service.PreparationProgressService;
import com.carsale.erp.shared.model.PipelineStage;
import com.carsale.erp.shared.pipeline.*;
import com.carsale.erp.shared.service.PipelineStageService;
import com.carsale.erp.shared.utils.PipelineStageUtils;
import com.carsale.erp.shared.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.preparationpipeline.model.RegistrationCertificate;
import com.carsale.erp.preparationpipeline.service.RegisteredProgressService;
import com.carsale.erp.preparationpipeline.service.RegistrationCertificateService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleIntake;
import com.carsale.erp.shared.vehicle.VehicleStage;
import com.carsale.erp.shared.dto.PreparationProgress;
import com.carsale.erp.preparationpipeline.service.VehicleInspectionService;
import com.carsale.erp.shared.vehicle.VehicleService;
import com.carsale.erp.preparationpipeline.service.WorkshopService;
import com.carsale.erp.preparationpipeline.service.YardService;
import com.carsale.erp.readypipeline.service.SaleListingService;

import static com.carsale.erp.shared.service.PipelineStageService.viewLinks;

@Controller
@RequiredArgsConstructor
public class PreparationPipelineListController {

    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final WorkshopService workshopService;
    private final YardService yardService;
    private final VehicleInspectionService vehicleInspectionService;
    private final SaleListingService saleListingService;
    private final PipelineStageService pipelineStageService;
    private final RegistrationCertificateService registrationCertificateService;
    private final RegisteredProgressService registeredProgressService;

    @GetMapping("/workshop-yard")
    public String list(@RequestParam(value = "q", required = false) String query, Model model) {
        List<Vehicle> vehicles = preparationProgressService.listEligible(query);
        List<PreparationListRow> rows = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            if (!registeredProgressService.readyForPreparation(vehicle)) {
                continue;
            }
            rows.add(new PreparationListRow(vehicle, preparationProgressService.progressFor(vehicle)));
        }
        model.addAttribute("pageTitle", "Preparation pipeline");
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("rows", rows);
        model.addAttribute("pipelineStages", pipelineStageService.list(PipelineStageService.FLOW_PREP));
        model.addAttribute("pipelineKeys", pipelineStageService.keys(PipelineStageService.FLOW_PREP));
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        return "preparation-pipeline/list";
    }

    @GetMapping("/workshop-yard/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "Vehicle book");
        model.addAttribute("activeMenu", "registered");
        if (!model.containsAttribute("certificate")) {
            model.addAttribute("certificate", new RegistrationCertificate());
        }
        model.addAttribute("editing", false);
        return "preparation-pipeline/new";
    }

    @PostMapping("/workshop-yard/new")
    public String create(
            @ModelAttribute("certificate") RegistrationCertificate certificate,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String chassisNo = certificate.getChassisNo() == null ? "" : certificate.getChassisNo().trim();
            certificate.setChassisNo(chassisNo);
            if (chassisNo.isEmpty()) {
                throw new IllegalArgumentException("Chassis number is required.");
            }
            Vehicle existing = vehicleService.findByChassisNo(chassisNo);
            if (existing != null) {
                if (existing.getIntake() == VehicleIntake.LOCAL_REGISTERED) {
                    redirectAttributes.addFlashAttribute("notice",
                            "This chassis is already in the registered vehicle pipeline.");
                    return "redirect:/workshop-yard/" + Utils.encodeChassis(chassisNo) + "/certificate";
                }
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Chassis number " + chassisNo + " is already in the import pipeline.");
                redirectAttributes.addFlashAttribute("certificate", certificate);
                return "redirect:/workshop-yard/new";
            }
            Vehicle vehicle = new Vehicle();
            registrationCertificateService.applyToVehicle(certificate, vehicle);
            vehicle.setIntake(VehicleIntake.LOCAL_REGISTERED);
            vehicle.setStage(VehicleStage.WORKSHOP);
            Vehicle saved = vehicleService.save(vehicle);
            certificate.setChassisNo(saved.getChassisNo());
            registrationCertificateService.save(certificate);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Vehicle book saved. Add the vehicle images next.");
            return "redirect:/registered/" + Utils.encodeChassis(saved.getChassisNo()) + "/photos";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("certificate", certificate);
            return "redirect:/workshop-yard/new";
        }
    }

    @PostMapping("/workshop-yard/{chassisNo}/skip")
    public String skip(
            @PathVariable String chassisNo,
            @RequestParam("stage") String stage,
            RedirectAttributes redirectAttributes
    ) {
        try {
            preparationProgressService.skipStage(chassisNo, stage);
            redirectAttributes.addFlashAttribute("successMessage", "Stage skipped.");
            return "redirect:" + PipelineStageUtils.redirectAfterStageSave(
                    chassisNo,
                    FlowPipeline.PREP.getCurrentBase(),
                    pipelineStageService.keys(PipelineStageService.FLOW_PREP),
                    stage.trim()
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            String editUrl = PipelineStageService.editUrlFor(Utils.encodeChassis(chassisNo), stage);
            return "redirect:" + (editUrl != null ? editUrl : "/workshop-yard");
        }
    }

    @PostMapping("/workshop-yard/{chassisNo}/delete")
    public String delete(@PathVariable String chassisNo, RedirectAttributes redirectAttributes) {
        if (vehicleService.deleteFromFlow(chassisNo, PipelineStageService.FLOW_PREP)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Preparation data deleted. Sale pipeline data was also removed.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Could not delete preparation data.");
        }
        return "redirect:/workshop-yard";
    }

    @GetMapping("/workshop-yard/{chassisNo}")
    public String view(
            @PathVariable String chassisNo,
            @RequestParam(value = "stage", required = false) String requestedStage,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/workshop-yard";
        }
        if (!registeredProgressService.readyForPreparation(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    "Finish vehicle book, images, and previous owner before preparation.");
            return "redirect:/registered";
        }
        if (!preparationProgressService.isEligible(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    preparationProgressService.ineligibleNotice(vehicle));
            return "redirect:" + preparationProgressService.redirectWhenNotEligible(vehicle);
        }

        preparationProgressService.syncVehicleStage(chassisNo);
        vehicle = vehicleService.findByChassisNo(chassisNo);

        PreparationProgress status = preparationProgressService.progressFor(vehicle);
        List<String> prepKeys = pipelineStageService.keys(PipelineStageService.FLOW_PREP);
        List<PipelineStage> prepStages = pipelineStageService.list(PipelineStageService.FLOW_PREP);
        int stageIndex = PipelineStageUtils.clampStageIndex(
                PipelineStageUtils.parseStageIndex(prepKeys, requestedStage),
                status,
                prepKeys
        );
        String stageKey = PipelineStageUtils.stageKeyAt(prepStages, stageIndex);
        if (stageKey == null) {
            return "redirect:/workshop-yard";
        }
        if (FlowStage.WORKSHOP.getStageKey().equals(stageKey) && workshopService.hasJobs(chassisNo)) {
            preparationProgressService.syncVehicleStage(chassisNo);
            redirectAttributes.addFlashAttribute("successMessage",
                    "No workshop jobs. Workshop is complete. Continue with yard.");
            return "redirect:/yard/" + UriUtils.encodePathSegment(chassisNo, java.nio.charset.StandardCharsets.UTF_8)
                    + "?hub=1";
        }
        NavLinks nav = viewLinks(chassisNo, stageIndex, status, prepStages, FlowPipeline.PREP);
        model.addAttribute("pageTitle", nav.getStageLabel());
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("workshop", workshopService.findByChassisNo(chassisNo));
        model.addAttribute("yard", yardService.findByChassisNo(chassisNo));
        model.addAttribute("inspection", vehicleInspectionService.findByChassisNo(chassisNo));
        model.addAttribute("listing", saleListingService.findByChassisNo(chassisNo));
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("inspectionReady", status.isInspectionReady());
        model.addAttribute("saleReady", status.isSaleReady());
        model.addAttribute("canEnterYard", status.isCanEnterYard());
        model.addAttribute("prepComplete", status.isPipelineCompleted());
        model.addAttribute("stageIndex", stageIndex);
        model.addAttribute("stageKey", stageKey);
        model.addAttribute("stageNav", nav);
        return "preparation-pipeline/detail";
    }

    @Getter
    @AllArgsConstructor
    public static final class PreparationListRow {
        private final Vehicle vehicle;
        private final PreparationProgress status;

    }
}
