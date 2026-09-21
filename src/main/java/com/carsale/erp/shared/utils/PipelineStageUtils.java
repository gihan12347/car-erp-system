package com.carsale.erp.shared.utils;

import com.carsale.erp.shared.pipeline.FlowPipeline;
import org.springframework.web.util.UriUtils;

import java.util.List;

public class PipelineStageUtils {

    public static String redirectAfterStageSave(
            String chassisNo,
            String basePath,
            List<String> keys,
            String currentKey
    ) {
        String encoded = encode(chassisNo);
        return redirectToNext(basePath, keys, currentKey, encoded);
    }

    public static String redirectToNext(
            String basePath,
            List<String> keys,
            String currentKey,
            String encoded
    ) {
        int index = keys.indexOf(currentKey);
        if (index < keys.size() - 1) {
            String nextKey = keys.get(index + 1);
            return viewUrl(viewBase(basePath,encoded), nextKey);
        } else if (index == keys.size() - 1) {
            FlowPipeline currentFlowPipeline = FlowPipeline.findPipelineByBasePath(basePath);
            if (currentFlowPipeline == null) {
                return viewBase(basePath, encoded);
            }
            if (currentFlowPipeline == FlowPipeline.PREP) {
                return "/yards";
            }
            FlowPipeline nextFlowPipeline = FlowPipeline.findPipelineBySortOrder(currentFlowPipeline.getSortOrder() + 1);
            if (nextFlowPipeline == null) {
                return viewUrl(viewBase(basePath, encoded), currentKey);
            }
            return viewBase(nextFlowPipeline.getCurrentBase(), encoded);
        }
        return viewBase(basePath, encoded);
    }

    public static String viewBase(String basePath, String encoded) {
        return basePath + encoded;
    }

    public static String encode(String chassisNo) {
        return UriUtils.encodePathSegment(chassisNo, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static String viewUrl(String viewBase, String stageKey) {
        if (stageKey == null || stageKey.trim().isEmpty()) {
            return viewBase;
        }
        return viewBase + "?stage=" + stageKey;
    }
}
