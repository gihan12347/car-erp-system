package com.carsale.erp.shared.pipeline;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NavLinks {
    private final int stageIndex;
    private final String stageLabel;
    private final String prevUrl;
    private final String nextUrl;
    private final String nextLabel;
    private final String editUrl;
}
