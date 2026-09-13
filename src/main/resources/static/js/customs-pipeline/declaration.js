(function () {
    var LINE_IDS = ["invoiceFob", "invoiceFreight", "invoiceInsurance", "invoiceOther"];
    var TOTAL_ID = "invoiceTotal";
    var RATE_ID = "exchangeRate";
    var VALUE_ID = "valueNcy";
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

    function sumLineItems() {
        var sum = 0;
        var hasAny = false;
        LINE_IDS.forEach(function (id) {
            var el = document.getElementById(id);
            var num = el ? parseNumber(el.value) : null;
            if (num != null) {
                hasAny = true;
                sum += num;
            }
        });
        return hasAny ? sum : null;
    }

    function updateInvoiceTotalFromLines() {
        var sum = sumLineItems();
        if (sum == null) {
            return;
        }
        setFieldValue(TOTAL_ID, formatAmount(sum, 2));
    }

    function updateValueNcy() {
        var totalEl = document.getElementById(TOTAL_ID);
        var rateEl = document.getElementById(RATE_ID);
        var total = totalEl ? parseNumber(totalEl.value) : null;
        var rate = rateEl ? parseNumber(rateEl.value) : null;
        if (total == null || rate == null) {
            return;
        }
        setFieldValue(VALUE_ID, formatAmount(total * rate, 2));
    }

    function refreshBreakdown(fromLines) {
        if (updating) {
            return;
        }
        updating = true;
        try {
            if (fromLines) {
                updateInvoiceTotalFromLines();
            }
            updateValueNcy();
        } finally {
            updating = false;
        }
    }

    function bindBreakdown() {
        LINE_IDS.forEach(function (id) {
            var el = document.getElementById(id);
            if (!el) {
                return;
            }
            el.addEventListener("input", function () {
                refreshBreakdown(true);
            });
            el.addEventListener("change", function () {
                refreshBreakdown(true);
            });
        });

        [TOTAL_ID, RATE_ID].forEach(function (id) {
            var el = document.getElementById(id);
            if (!el) {
                return;
            }
            el.addEventListener("input", function () {
                refreshBreakdown(false);
            });
            el.addEventListener("change", function () {
                refreshBreakdown(false);
            });
        });
    }

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
            "invoiceFob", "invoiceFreight", "invoiceInsurance", "invoiceOther",
            "invoiceTotal", "exchangeRate", "valueNcy"
        ],
        onFilled: function () {
            var totalEl = document.getElementById(TOTAL_ID);
            var valueEl = document.getElementById(VALUE_ID);
            if (!parseNumber((totalEl || {}).value)) {
                refreshBreakdown(true);
            } else if (!parseNumber((valueEl || {}).value)) {
                refreshBreakdown(false);
            }
        }
    });

    bindBreakdown();
})();
