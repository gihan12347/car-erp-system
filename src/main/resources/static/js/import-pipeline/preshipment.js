(function () {
    var uploadLabel = document.getElementById("uploadLabel");
    var input = document.getElementById("sheetFile");
    var status = document.getElementById("uploadStatus");
    var preview = document.getElementById("sheetPreview");
    var previewFrame = document.getElementById("sheetPreviewFrame");
    var previewEmpty = document.getElementById("sheetPreviewEmpty");
    var previewImage = document.getElementById("sheetPreviewImage");
    var previewPdf = document.getElementById("sheetPreviewPdf");
    var fileNameLabel = document.getElementById("sheetFileName");
    var fillBtn = document.getElementById("autoFillBtn");
    var previewBtn = document.getElementById("previewSheetBtn");
    var viewerClose = document.getElementById("sheetViewerClose");
    var prevEmptyBtn = document.getElementById("sheetPrevEmptyField");
    var nextEmptyBtn = document.getElementById("sheetNextEmptyField");
    var loader = document.getElementById("fillLoader");
    var documentOriginalNameInput = document.getElementById("documentOriginalName");
    var documentStoredNameInput = document.getElementById("documentStoredName");
    var documentContentTypeInput = document.getElementById("documentContentType");
    var selectedFile = null;
    var previewUrl = null;
    var previewIsPdf = false;
    var previewName = "";
    var objectUrl = null;
    var FIELD_ORDER = [
        "inspectionOrgName", "inspectionOrgAddress", "inspectionOrgTel", "inspectionOrgFax", "inspectionOrgEmail",
        "applicantName", "applicantAddress", "applicantTel", "applicantFax", "applicantEmail",
        "vehicleType", "make", "model", "commonName", "manufactureGrade", "preshipAuctionGrade",
        "bodyColour", "fuelType", "firstRegistration", "inspectionMileage", "engineCapacity", "engineModel",
        "chassisNo", "engineNo", "drivingSystem", "accidentMarksOnChassis", "chassisCondition",
        "tyreSize", "wheelBase", "grossVehicleMass"
    ];
    var currentFieldIndex = 0;

    if (!input) {
        return;
    }

    function hasSavedDocument() {
        return !!(documentStoredNameInput && documentStoredNameInput.value.trim());
    }

    function canAutoFill() {
        return !!selectedFile || hasSavedDocument();
    }

    function canPreview() {
        return !!previewUrl || hasSavedDocument();
    }

    function updateActionButtons() {
        if (fillBtn) {
            fillBtn.disabled = !canAutoFill();
        }
        if (previewBtn) {
            previewBtn.disabled = !canPreview();
        }
        if (uploadLabel) {
            uploadLabel.innerHTML = hasSavedDocument() || selectedFile
                ? '<i class="fa-solid fa-arrows-rotate"></i> Replace file'
                : '<i class="fa-solid fa-cloud-arrow-up"></i> Choose file';
        }
    }

    function setStatus(text, ok) {
        if (!status) {
            return;
        }
        status.textContent = text;
        status.className = "upload-status " + (ok ? "ok" : "err");
    }

    function showLoader(show) {
        if (!loader) {
            return;
        }
        loader.hidden = !show;
        document.body.classList.toggle("is-filling", show);
        if (fillBtn) {
            fillBtn.disabled = show || !canAutoFill();
        }
        if (previewBtn) {
            previewBtn.disabled = show || !canPreview();
        }
    }

    function clearObjectUrl() {
        if (objectUrl) {
            URL.revokeObjectURL(objectUrl);
            objectUrl = null;
        }
    }

    function isPdfType(contentType, name) {
        if (contentType && contentType.indexOf("pdf") >= 0) {
            return true;
        }
        return name && /\.pdf$/i.test(name);
    }

    function buildDocumentUrl(storedName) {
        if (!storedName) {
            return null;
        }
        return "/shipping/documents/" + encodeURIComponent(storedName);
    }

    function hideSavedPreviewElements() {
        var savedImage = document.getElementById("sheetPreviewSavedImage");
        var savedPdf = document.getElementById("sheetPreviewSavedPdf");
        if (savedImage) {
            savedImage.hidden = true;
            savedImage.removeAttribute("src");
        }
        if (savedPdf) {
            savedPdf.hidden = true;
            savedPdf.removeAttribute("src");
        }
    }

    function markDocumentReady(name) {
        if (preview) {
            preview.classList.add("has-file");
        }
        if (fileNameLabel && name) {
            fileNameLabel.textContent = name;
        }
        updateActionButtons();
    }

    function renderPreviewFrame(url, name, contentType) {
        if (!preview) {
            return;
        }
        markDocumentReady(name || "Pre-shipment certificate");
        if (previewEmpty) {
            previewEmpty.hidden = true;
        }
        hideSavedPreviewElements();
        previewIsPdf = isPdfType(contentType, name);
        previewUrl = url;
        previewName = name || "Pre-shipment certificate";
        if (previewIsPdf) {
            if (previewImage) {
                previewImage.hidden = true;
                previewImage.removeAttribute("src");
            }
            if (previewPdf) {
                previewPdf.hidden = false;
                previewPdf.src = url;
            }
        } else {
            if (previewPdf) {
                previewPdf.hidden = true;
                previewPdf.removeAttribute("src");
            }
            if (previewImage) {
                previewImage.hidden = false;
                previewImage.src = url;
            }
        }
        updateActionButtons();
        if (window.DocumentViewer) {
            DocumentViewer.refresh();
        }
    }

    function setDocumentMetadata(originalName, storedName, contentType) {
        if (documentOriginalNameInput) {
            documentOriginalNameInput.value = originalName || "";
        }
        if (documentStoredNameInput) {
            documentStoredNameInput.value = storedName || "";
        }
        if (documentContentTypeInput) {
            documentContentTypeInput.value = contentType || "";
        }
    }

    function openViewer() {
        if (!previewUrl && hasSavedDocument()) {
            previewUrl = buildDocumentUrl(documentStoredNameInput.value);
            previewName = documentOriginalNameInput
                ? documentOriginalNameInput.value
                : documentStoredNameInput.value;
            previewIsPdf = isPdfType(
                documentContentTypeInput ? documentContentTypeInput.value : "",
                previewName + " " + documentStoredNameInput.value
            );
        }
        if (!previewUrl) {
            setStatus("Upload a certificate first to preview it.", false);
            return;
        }
        if (window.DocumentViewer) {
            DocumentViewer.open({
                url: previewUrl,
                title: previewName || "Pre-shipment certificate",
                isPdf: previewIsPdf
            });
        }
    }

    function closeViewer() {
        if (window.DocumentViewer) {
            DocumentViewer.close();
        }
    }

    function uploadDocument(file, onSuccess, onError) {
        var data = new FormData();
        data.append("file", file);
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        var xhr = new XMLHttpRequest();
        xhr.open("POST", "/shipping/upload-document");
        xhr.timeout = 120000;
        if (token && header) {
            xhr.setRequestHeader(header.content, token.content);
        }
        xhr.onload = function () {
            if (xhr.status === 401 || xhr.status === 403) {
                onError("Please sign in again, then upload the document.");
                return;
            }
            try {
                var result = JSON.parse(xhr.responseText);
                if (!result.success) {
                    onError(result.message || "Could not save the document.");
                    return;
                }
                onSuccess(result);
            } catch (e) {
                onError("Could not save the document.");
            }
        };
        xhr.onerror = function () {
            onError("Upload failed.");
        };
        xhr.ontimeout = function () {
            onError("Saving the document took too long.");
        };
        xhr.send(data);
    }

    function selectFile(file) {
        selectedFile = file;
        updateActionButtons();
        setStatus("Saving document...", true);

        uploadDocument(file, function (result) {
            clearObjectUrl();
            objectUrl = URL.createObjectURL(file);
            previewName = result.originalName || file.name;
            previewIsPdf = !!result.pdf;

            setDocumentMetadata(result.originalName, result.storedName, result.contentType);
            renderPreviewFrame(objectUrl, previewName, result.contentType);
            previewUrl = buildDocumentUrl(result.storedName) || result.previewUrl || objectUrl;

            if (previewFrame) {
                previewFrame.scrollIntoView({ behavior: "smooth", block: "nearest" });
            }
            setStatus("Document saved. Click Auto fill to read fields from the certificate.", true);
        }, function (message) {
            selectedFile = null;
            updateActionButtons();
            setStatus(message, false);
        });
    }

    function initExistingDocument() {
        if (!hasSavedDocument()) {
            updateActionButtons();
            return;
        }
        previewName = documentOriginalNameInput
            ? documentOriginalNameInput.value
            : documentStoredNameInput.value;
        previewUrl = buildDocumentUrl(documentStoredNameInput.value);
        renderPreviewFrame(
            previewUrl,
            previewName,
            documentContentTypeInput ? documentContentTypeInput.value : ""
        );
        setStatus("Saved certificate loaded. Use Auto fill to read fields from the document.", true);
        updateActionButtons();
    }

    function normalizeYearMonth(value) {
        if (!value) {
            return value;
        }
        var text = String(value).trim().replace(/\s+/g, " ");
        var iso = text.match(/^(\d{4})-(\d{1,2})$/);
        if (iso) {
            return iso[1] + "-" + ("0" + iso[2]).slice(-2);
        }
        var numeric = text.match(/^(\d{1,2})[/. -](\d{4})$/);
        if (numeric) {
            return numeric[2] + "-" + ("0" + numeric[1]).slice(-2);
        }
        var yearFirst = text.match(/^(\d{4})[/. -](\d{1,2})$/);
        if (yearFirst) {
            return yearFirst[1] + "-" + ("0" + yearFirst[2]).slice(-2);
        }
        var named = text.match(/^(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)[.\-/ ]*(\d{4})$/i);
        if (named) {
            var months = {
                jan: "01", feb: "02", mar: "03", apr: "04", may: "05", jun: "06",
                jul: "07", aug: "08", sep: "09", oct: "10", nov: "11", dec: "12"
            };
            var key = named[1].toLowerCase().slice(0, 3);
            if (months[key]) {
                return named[2] + "-" + months[key];
            }
        }
        return text;
    }

    function setFieldValue(el, key, value) {
        if (!el) {
            return false;
        }
        if (key === "firstRegistration" || (el.type === "month")) {
            el.value = normalizeYearMonth(value);
        } else {
            el.value = value;
        }
        return true;
    }

    function fillForm(result) {
        var fields = result.fields || {};
        var filled = 0;

        FIELD_ORDER.forEach(function (key) {
            var value = fields[key];
            if (!value) {
                return;
            }
            if (key === "chassisNo") {
                var display = document.getElementById("chassisNoDisplay");
                if (display) {
                    display.value = value;
                    filled++;
                }
                return;
            }
            var el = document.getElementById(key);
            if (setFieldValue(el, key, value)) {
                filled++;
            }
        });

        Object.keys(fields).forEach(function (key) {
            if (FIELD_ORDER.indexOf(key) >= 0) {
                return;
            }
            var value = fields[key];
            if (!value) {
                return;
            }
            var el = document.getElementById(key);
            if (setFieldValue(el, key, value)) {
                filled++;
            }
        });

        var ocr = document.getElementById("ocrText");
        if (ocr && result.rawText) {
            ocr.value = result.rawText;
        }
        return filled;
    }

    function normalizeExistingMonthFields() {
        var firstRegistration = document.getElementById("firstRegistration");
        if (firstRegistration && firstRegistration.value) {
            firstRegistration.value = normalizeYearMonth(firstRegistration.value);
        }
    }

    function withParseFile(callback, onError) {
        if (selectedFile) {
            callback(selectedFile);
            return;
        }
        if (!hasSavedDocument()) {
            onError("Upload a certificate first.");
            return;
        }
        var url = buildDocumentUrl(documentStoredNameInput.value);
        var name = documentOriginalNameInput && documentOriginalNameInput.value
            ? documentOriginalNameInput.value
            : "certificate";
        fetch(url)
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("fetch failed");
                }
                return response.blob();
            })
            .then(function (blob) {
                callback(new File([blob], name, { type: blob.type || "application/octet-stream" }));
            })
            .catch(function () {
                onError("Could not load the saved document. Upload it again.");
            });
    }

    function autoFill() {
        if (!canAutoFill()) {
            setStatus("Upload a certificate first.", false);
            return;
        }
        showLoader(true);
        setStatus("Reading pre-shipment certificate...", true);
        withParseFile(function (file) {
            var data = new FormData();
            data.append("file", file);
            data.append("provider", window.selectedOcrProvider ? window.selectedOcrProvider("") : "google");
            var token = document.querySelector('meta[name="_csrf"]');
            var header = document.querySelector('meta[name="_csrf_header"]');
            var xhr = new XMLHttpRequest();
            xhr.open("POST", "/shipping/parse-document");
            xhr.timeout = 180000;
            if (token && header) {
                xhr.setRequestHeader(header.content, token.content);
            }
            xhr.onload = function () {
                if (xhr.status === 401 || xhr.status === 403) {
                    showLoader(false);
                    setStatus("Please sign in again, then try Auto fill.", false);
                    return;
                }
                try {
                    var result = JSON.parse(xhr.responseText);
                    var filled = fillForm(result);
                    var message = result.message || ("Filled " + filled + " fields from the certificate.");
                    setStatus(message, result.success !== false);
                } catch (e) {
                    setStatus("Could not read the certificate. Fill the form manually.", false);
                }
                showLoader(false);
            };
            xhr.onerror = function () {
                showLoader(false);
                setStatus("Could not read the certificate. Fill the form manually.", false);
            };
            xhr.ontimeout = function () {
                showLoader(false);
                setStatus("Reading the certificate is taking too long. Please wait and try again.", false);
            };
            xhr.send(data);
        }, function (message) {
            showLoader(false);
            setStatus(message, false);
        });
    }

    input.addEventListener("change", function () {
        if (input.files && input.files[0]) {
            var file = input.files[0];
            input.value = "";
            selectFile(file);
        }
    });
    if (fillBtn) {
        fillBtn.addEventListener("click", autoFill);
    }
    if (previewBtn) {
        previewBtn.addEventListener("click", openViewer);
    }
    if (viewerClose) {
        viewerClose.addEventListener("click", closeViewer);
    }
    if (nextEmptyBtn) {
        nextEmptyBtn.addEventListener("click", function () {
            focusNextEmptyField(1);
        });
    }
    if (prevEmptyBtn) {
        prevEmptyBtn.addEventListener("click", function () {
            focusNextEmptyField(-1);
        });
    }
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            closeViewer();
        }
    });

    initExistingDocument();
    normalizeExistingMonthFields();

    function isFieldEmpty(fieldId) {
        var el = document.getElementById(fieldId);
        return !el || !el.value || el.value.trim().length === 0;
    }

    function highlightField(fieldId) {
        Array.prototype.forEach.call(document.querySelectorAll(".sheet-field-target"), function (node) {
            node.classList.remove("sheet-field-target");
        });
        var el = document.getElementById(fieldId);
        if (el) {
            el.classList.add("sheet-field-target");
        }
    }

    function setCurrentIndexFromActiveElement() {
        var active = document.activeElement;
        if (!active || !active.id) {
            return;
        }
        var idx = FIELD_ORDER.indexOf(active.id);
        if (idx >= 0) {
            currentFieldIndex = idx;
        }
    }

    function focusInput(fieldId) {
        var el = document.getElementById(fieldId);
        if (!el) {
            return false;
        }
        highlightField(fieldId);
        el.focus();
        try {
            var v = el.value || "";
            el.setSelectionRange(v.length, v.length);
        } catch (e) {
        }
        return true;
    }

    function focusNextEmptyField(direction) {
        if (!FIELD_ORDER || !FIELD_ORDER.length) {
            return;
        }
        direction = direction || 1;
        setCurrentIndexFromActiveElement();
        var start = currentFieldIndex;
        for (var step = 0; step < FIELD_ORDER.length; step++) {
            var idx = start + step * direction;
            if (idx < 0) {
                idx = FIELD_ORDER.length - 1;
            }
            if (idx >= FIELD_ORDER.length) {
                idx = 0;
            }
            var fieldId = FIELD_ORDER[idx];
            if (isFieldEmpty(fieldId)) {
                currentFieldIndex = idx;
                focusInput(fieldId);
                return;
            }
        }
        currentFieldIndex = FIELD_ORDER.length - 1;
        focusInput(FIELD_ORDER[currentFieldIndex]);
    }
})();
