(function () {
    ClearanceStage({
        title: "Customs declaration",
        uploadUrl: "/declaration/upload-document",
        parseUrl: "/declaration/parse-document",
        docUrlPrefix: "/declaration/documents/",
        originalNameId: "page2OriginalName",
        storedNameId: "page2StoredName",
        contentTypeId: "page2ContentType",
        ocrTextId: "ocrTextPage2",
        fallbackFileName: "customs-declaration",
        fieldOrder: [
            "customsReference", "declarationType", "totalPackages", "exporterName", "exporterAddress",
            "consigneeName", "consigneeTin", "consigneeAddress", "declarantName", "declarantTin", "declarantAddress",
            "countryExport", "countryDestination", "countryOrigin", "vesselFlight", "deliveryTerms", "voyageNoDate",
            "placeLoadingDischarging", "currencyInvoiced", "totalAmountInvoiced", "exchangeRate", "paymentTerms",
            "bankCode", "bankName", "bankBranch", "bankReference", "locationOfGoods",
            "hsCode", "grossMassKg", "netMassKg", "blAwbNo", "goodsDescription", "modelSpec",
            "yearOfManufacture", "dateOfRegistration", "clearanceChassisNo", "clearanceEngineNo",
            "itemPrice", "valueNcy", "engineCapacityCc",
            "taxCid", "taxSur", "taxVat", "taxXid", "taxVel", "taxOther", "totalTaxAmount",
            "invoiceFob", "invoiceFreight", "invoiceInsurance", "invoiceTotal", "declarantDate"
        ]
    });
})();
