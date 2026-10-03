(function () {
    ClearanceStage({
        title: "Assessment notice",
        uploadUrl: "/assessment/upload-document",
        parseUrl: "/assessment/parse-document",
        docUrlPrefix: "/assessment/documents/",
        originalNameId: "page3OriginalName",
        storedNameId: "page3StoredName",
        contentTypeId: "page3ContentType",
        ocrTextId: "ocrTextPage3",
        fallbackFileName: "assessment-notice",
        fieldOrder: [
            "assessmentOffice", "assessmentNoticeRef", "assessmentModel", "assessmentPackages",
            "assessmentCustomsReference", "assessmentDeclarantReference", "assessmentReference",
            "assessmentDeclarantId", "assessmentDeclarantName", "assessmentDeclarantAddress",
            "assessmentDeclarantChaExp", "assessmentConsigneeId", "assessmentConsigneeName",
            "assessmentConsigneeAddress", "assessmentTaxOtc", "assessmentTaxCom", "assessmentTaxExm",
            "assessmentTaxCid", "assessmentTaxSur", "assessmentTaxXid", "assessmentTaxVat",
            "assessmentTaxVel", "assessmentTotalAssessed", "assessmentTotalPaid"
        ]
    });
})();
