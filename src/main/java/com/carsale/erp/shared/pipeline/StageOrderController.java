package com.carsale.erp.shared.pipeline;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.carsale.erp.shared.pipeline.StageReorderRequest;
import com.carsale.erp.shared.pipeline.StageReorderResult;
import com.carsale.erp.shared.pipeline.PipelineStageService;

@Controller
public class StageOrderController {

    private final PipelineStageService pipelineStageService;

    public StageOrderController(PipelineStageService pipelineStageService) {
        this.pipelineStageService = pipelineStageService;
    }

    @GetMapping("/stages")
    public String page(Model model) {
        model.addAttribute("pageTitle", "Stage order");
        model.addAttribute("activeMenu", "stages");
        model.addAttribute("flowGroups", pipelineStageService.flowGroups());
        return "shared/stage-order";
    }

    @PostMapping("/stages/reorder")
    @ResponseBody
    public StageReorderResult reorder(@RequestBody StageReorderRequest request) {
        try {
            if (request == null || request.getFlowId() == null) {
                return StageReorderResult.fail("Stage order is required.");
            }
            return StageReorderResult.ok(
                    "Stage order saved.",
                    pipelineStageService.reorder(request.getFlowId(), request.getStageKeys())
            );
        } catch (IllegalArgumentException ex) {
            return StageReorderResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/stages/reset")
    @ResponseBody
    public StageReorderResult reset(@RequestParam("flowId") Long flowId) {
        try {
            return StageReorderResult.ok(
                    "Default stage order restored.",
                    pipelineStageService.reset(flowId)
            );
        } catch (IllegalArgumentException ex) {
            return StageReorderResult.fail(ex.getMessage());
        }
    }
}
