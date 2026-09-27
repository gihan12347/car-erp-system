(function () {
    ClearanceStage({
        title: "Working sheet",
        uploadUrl: "/worksheet/upload-document",
        parseUrl: "/worksheet/parse-document",
        docUrlPrefix: "/worksheet/documents/",
        originalNameId: "page4OriginalName",
        storedNameId: "page4StoredName",
        contentTypeId: "page4ContentType",
        ocrTextId: "ocrTextPage4",
        fallbackFileName: "working-sheet",
        fieldOrder: [
            "worksheetRef", "worksheetHsCode", "worksheetVehicleType", "worksheetReferenceNo",
            "worksheetVesselName", "worksheetChassisNo", "worksheetAgentsFob", "worksheetAgentsFobCalc",
            "worksheetAgentsFreight", "worksheetAgentsInsurance", "worksheetInvoicedFob",
            "worksheetInvoicedFreight", "worksheetInvoicedInsurance", "worksheetOptionsValue",
            "worksheetBlFreightCalc", "worksheetBlFreightAmount", "worksheetBlDate", "worksheetManufactureDate",
            "worksheetAgeDifference", "worksheetFirstRegistrationDate", "worksheetWebsiteValue",
            "worksheetLocalTaxes", "worksheetFifteenPercent", "worksheetFobValue85", "worksheetFobValue85Currency",
            "worksheetLcNo", "worksheetLcAmount", "worksheetLcBank", "worksheetLcImporter",
            "worksheetLcIssueDate", "worksheetLcExpiryDate", "worksheetLcAmendmentDate",
            "worksheetClearingAgent", "worksheetChaNo", "worksheetFiscalFob", "worksheetFiscalFreight",
            "worksheetFiscalInsurance", "worksheetFiscalOptions", "worksheetFiscalTotal",
            "worksheetFiscalTotalCurrency", "worksheetCheckedBy", "worksheetAppraiserName"
        ]
    });
})();
