package com.carsale.erp.preparationpipeline.sale;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import com.carsale.erp.preparationpipeline.PreparationProgressService;
import com.carsale.erp.preparationpipeline.PreparationProgressService.PreparationProgress;
import com.carsale.erp.preparationpipeline.PreparationStageUrls;
import com.carsale.erp.readypipeline.SaleListing;
import com.carsale.erp.readypipeline.SaleListingService;
import com.carsale.erp.readypipeline.SaleLocationService;
import com.carsale.erp.shared.pipeline.FlowStage;
import com.carsale.erp.shared.pipeline.PipelineStageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleService;

@Controller
@RequestMapping("/sale")
public class PrepSaleController {

    private final SaleListingService saleListingService;
    private final SaleLocationService saleLocationService;
    private final VehicleService vehicleService;
    private final PreparationProgressService preparationProgressService;
    private final PipelineStageService pipelineStageService;

    public PrepSaleController(
            SaleListingService saleListingService,
            SaleLocationService saleLocationService,
            VehicleService vehicleService,
            PreparationProgressService preparationProgressService,
            PipelineStageService pipelineStageService
    ) {
        this.saleListingService = saleListingService;
        this.saleLocationService = saleLocationService;
        this.vehicleService = vehicleService;
        this.preparationProgressService = preparationProgressService;
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String query) {
        if (query != null && !query.trim().isEmpty()) {
            return "redirect:/workshop-yard?q=" + UriUtils.encodeQueryParam(query.trim(), StandardCharsets.UTF_8);
        }
        return "redirect:/workshop-yard";
    }

    @GetMapping("/{chassisNo}")
    public String form(
            @PathVariable String chassisNo,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Vehicle vehicle = vehicleService.findByChassisNo(chassisNo);
        if (vehicle == null) {
            return "redirect:/workshop-yard";
        }
        if (!preparationProgressService.isEligible(vehicle)) {
            redirectAttributes.addFlashAttribute("notice",
                    preparationProgressService.ineligibleNotice(vehicle));
            return "redirect:" + preparationProgressService.redirectWhenNotEligible(vehicle);
        }
        PreparationProgress status = preparationProgressService.progressFor(vehicle);
        SaleListing record = saleListingService.prepareForm(chassisNo);
        model.addAttribute("pageTitle", pipelineStageService.title(
                PipelineStageService.FLOW_PREP, FlowStage.SALE.getStageKey()));
        model.addAttribute("activeMenu", "workshop-yard");
        model.addAttribute("hubMode", hub);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("record", record);
        model.addAttribute("saleLocations", saleLocationService.listActive());
        addPrepStatus(model, chassisNo, status);
        return "preparation-pipeline/sale/form";
    }

    @PostMapping("/{chassisNo}")
    public String save(
            @PathVariable String chassisNo,
            @ModelAttribute SaleListing record,
            @RequestParam(value = "hub", defaultValue = "false") boolean hub,
            RedirectAttributes redirectAttributes
    ) {
        record.setChassisNo(chassisNo);
        try {
            saleListingService.saveAssignment(record);
            preparationProgressService.syncVehicleStage(chassisNo);
            PreparationProgress status = preparationProgressService.progressFor(chassisNo);
            if (status.isPipelineCompleted()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Preparation pipeline is complete. Continue in the sale pipeline.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Sale assignment saved.");
            }
            return "redirect:" + PreparationStageUrls.redirectAfterSaleSave(
                    chassisNo,
                    pipelineStageService.keys(PipelineStageService.FLOW_PREP)
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/sale/" + encodeChassis(chassisNo) + (hub ? "?hub=1" : "");
        }
    }

    private void addPrepStatus(Model model, String chassisNo, PreparationProgress status) {
        model.addAttribute("workshopReady", status.isWorkshopReady());
        model.addAttribute("yardReady", status.isYardReady());
        model.addAttribute("inspectionReady", status.isInspectionReady());
        model.addAttribute("saleReady", status.isSaleReady());
        model.addAttribute("canEnterYard", status.isCanEnterYard());
        model.addAttribute("prepComplete", status.isPipelineCompleted());
        model.addAttribute("stageNav", PreparationStageUrls.editLinks(
                chassisNo,
                pipelineStageService.indexOf(PipelineStageService.FLOW_PREP, FlowStage.SALE.getStageKey()),
                status,
                pipelineStageService.list(PipelineStageService.FLOW_PREP)
        ));
    }

    private String encodeChassis(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, StandardCharsets.UTF_8);
    }
}
