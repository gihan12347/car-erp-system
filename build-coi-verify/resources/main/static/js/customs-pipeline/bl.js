(function () {
    var COST_ID = "landingCostUsd";
    var RATE_ID = "blExchangeRate";
    var VALUE_ID = "landingCostLkr";
    var updating = false;

    function parseNumber(value) {
        if (value == null) {
            return null;
        }
        var cleaned = String(value).replace(/,/g, "").replace(/[^\d.-]/g, "").trim();
        if (!cleaned) {
            return null;
        }
        var num = Number(cleaned);
        return isFinite(num) ? num : null;
    }

    function formatAmount(value, decimals) {
        if (value == null || !isFinite(value)) {
            return "";
        }
        var fixed = value.toFixed(decimals);
        var parts = fixed.split(".");
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        return parts.join(".");
    }

    function setFieldValue(id, value) {
        var el = document.getElementById(id);
        if (el) {
            el.value = value;
        }
    }

    function updateLandingCostLkr() {
        var costEl = document.getElementById(COST_ID);
        var rateEl = document.getElementById(RATE_ID);
        var cost = costEl ? parseNumber(costEl.value) : null;
        var rate = rateEl ? parseNumber(rateEl.value) : null;
        if (cost == null || rate == null) {
            return;
        }
        setFieldValue(VALUE_ID, formatAmount(cost * rate, 2));
    }

    function refreshBreakdown() {
        if (updating) {
            return;
        }
        updating = true;
        try {
            updateLandingCostLkr();
        } finally {
            updating = false;
        }
    }

    function bindBreakdown() {
        [COST_ID, RATE_ID].forEach(function (id) {
            var el = document.getElementById(id);
            if (!el) {
                return;
            }
            el.addEventListener("input", refreshBreakdown);
            el.addEventListener("change", refreshBreakdown);
        });
    }

    ClearanceStage({
        title: "Bill of lading",
        uploadUrl: "/bl/upload-document",
        parseUrl: "/bl/parse-document",
        docUrlPrefix: "/bl/documents/",
        originalNameId: "page5OriginalName",
        storedNameId: "page5StoredName",
        contentTypeId: "page5ContentType",
        ocrTextId: "ocrTextPage5",
        fallbackFileName: "bill-of-lading",
        fieldOrder: ["blNo", "dateOfBlIssue"],
        onFilled: function () {
            var valueEl = document.getElementById(VALUE_ID);
            if (!parseNumber((valueEl || {}).value)) {
                refreshBreakdown();
            }
        }
    });

    bindBreakdown();
})();
