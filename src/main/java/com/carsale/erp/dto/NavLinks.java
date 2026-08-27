package com.carsale.erp.dto;

public class NavLinks {
    private final int stageIndex;
    private final String stageLabel;
    private final String prevUrl;
    private final String nextUrl;
    private final String nextLabel;
    private final String editUrl;

    public NavLinks(int stageIndex, String stageLabel, String prevUrl, String nextUrl, String nextLabel, String editUrl) {
        this.stageIndex = stageIndex;
        this.stageLabel = stageLabel;
        this.prevUrl = prevUrl;
        this.nextUrl = nextUrl;
        this.nextLabel = nextLabel;
        this.editUrl = editUrl;
    }

    public int getStageIndex() {
        return stageIndex;
    }

    public String getStageLabel() {
        return stageLabel;
    }

    public String getPrevUrl() {
        return prevUrl;
    }

    public String getNextUrl() {
        return nextUrl;
    }

    public String getNextLabel() {
        return nextLabel;
    }

    public String getEditUrl() {
        return editUrl;
    }
}
