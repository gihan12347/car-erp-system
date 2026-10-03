(function () {
    var uploadLabel = document.getElementById("uploadLabel");
    var input = document.getElementById("sheetFile");
    var status = document.getElementById("uploadStatus");
    var preview = document.getElementById("sheetPreview");
    var previewEmpty = document.getElementById("sheetPreviewEmpty");
    var previewImage = document.getElementById("sheetPreviewImage");
    var previewPdf = document.getElementById("sheetPreviewPdf");
    var fileNameLabel = document.getElementById("sheetFileName");
    var fillBtn = document.getElementById("autoFillBtn");
    var previewBtn = document.getElementById("previewSheetBtn");
    var viewer = document.getElementById("sheetViewer");
    var viewerClose = document.getElementById("sheetViewerClose");
    var prevEmptyBtn = document.getElementById("sheetPrevEmptyField");
    var nextEmptyBtn = document.getElementById("sheetNextEmptyField");
    var loader = document.getElementById("fillLoader");
    var sheetOriginalNameInput = document.getElementById("sheetOriginalName");
    var sheetStoredNameInput = document.getElementById("sheetStoredName");
    var sheetContentTypeInput = document.getElementById("sheetContentType");
    var selectedFile = null;
    var previewUrl = null;
    var previewIsPdf = false;
    var previewName = "";
    var objectUrl = null;
    var FIELD_ORDER = [
        "chassisNo", "lotNo", "year", "model", "bodyStyle", "grade",
        "auctionGrade", "exteriorGrade", "interiorGrade",
        "mileage", "history", "engineSize", "fuel", "modelCode", "chassisNo",
        "seats", "color", "colorCode",
        "lengthCm", "widthCm", "heightCm",
        "transmission", "acType"
    ];
    var currentFieldIndex = 0;

    if (!input) {
        return;
    }

    function hasSavedDocument() {
        return !!(sheetStoredNameInput && sheetStoredNameInput.value.trim());
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

    function renderPreviewFrame(url, name, contentType) {
        if (!preview) {
            return;
        }
        preview.classList.add("has-file");
        if (previewEmpty) {
            previewEmpty.hidden = true;
        }
        if (fileNameLabel) {
            fileNameLabel.textContent = name || "Auction sheet";
        }
        hideSavedPreviewElements();
        previewUrl = url;
        previewName = name || "Auction sheet";
        previewIsPdf = isPdfType(contentType, name);
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

    function setSheetMetadata(originalName, storedName, contentType) {
        if (sheetOriginalNameInput) {
            sheetOriginalNameInput.value = originalName || "";
        }
        if (sheetStoredNameInput) {
            sheetStoredNameInput.value = storedName || "";
        }
        if (sheetContentTypeInput) {
            sheetContentTypeInput.value = contentType || "";
        }
    }

    function openViewer() {
        if (!previewUrl && hasSavedDocument()) {
            previewUrl = "/auction/documents/" + encodeURIComponent(sheetStoredNameInput.value);
            previewName = sheetOriginalNameInput
                ? sheetOriginalNameInput.value
                : sheetStoredNameInput.value;
            previewIsPdf = isPdfType(
                sheetContentTypeInput ? sheetContentTypeInput.value : "",
                previewName + " " + sheetStoredNameInput.value
            );
        }
        if (!previewUrl) {
            setStatus("Upload an auction sheet first to preview it.", false);
            return;
        }
        if (window.DocumentViewer) {
            DocumentViewer.open({
                url: previewUrl,
                title: previewName || "Auction sheet",
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
        xhr.open("POST", "/auction/upload-sheet");
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
            setSheetMetadata(result.originalName, result.storedName, result.contentType);
            renderPreviewFrame(objectUrl, previewName, result.contentType);
            if (result.storedName) {
                previewUrl = "/auction/documents/" + encodeURIComponent(result.storedName);
            }
            setStatus("Document saved. Preview it, use Auto fill, then Save vehicle.", true);
            updateActionButtons();
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
        previewUrl = "/auction/documents/" + encodeURIComponent(sheetStoredNameInput.value);
        previewName = sheetOriginalNameInput ? sheetOriginalNameInput.value : sheetStoredNameInput.value;
        renderPreviewFrame(
            previewUrl,
            previewName,
            sheetContentTypeInput ? sheetContentTypeInput.value : ""
        );
        setStatus("Saved document loaded. You can preview it or upload a new one.", true);
        updateActionButtons();
    }

    function fillForm(result) {
        var fields = result.fields || {};
        Object.keys(fields).forEach(function (key) {
            var value = fields[key];
            if (!value) {
                return;
            }
            var nodes = document.querySelectorAll("#" + key + ", [name=\"" + key + "\"]");
            if (!nodes.length) {
                var el = document.getElementById(key);
                if (el) {
                    el.value = value;
                }
                return;
            }
            Array.prototype.forEach.call(nodes, function (el) {
                el.value = value;
            });
        });
        var ocr = document.getElementById("ocrText");
        if (ocr && result.rawText) {
            ocr.value = result.rawText;
        }
        scheduleChassisCheck();
    }

    function withParseFile(callback, onError) {
        if (selectedFile) {
            callback(selectedFile);
            return;
        }
        if (!hasSavedDocument()) {
            onError("Upload an auction sheet first.");
            return;
        }
        var url = "/auction/documents/" + encodeURIComponent(sheetStoredNameInput.value);
        var name = sheetOriginalNameInput && sheetOriginalNameInput.value
            ? sheetOriginalNameInput.value
            : "auction-sheet";
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
            setStatus("Upload an auction sheet first.", false);
            return;
        }
        showLoader(true);
        setStatus("Reading Japanese auction sheet...", true);
        withParseFile(function (file) {
            var data = new FormData();
            data.append("file", file);
            data.append("provider", window.selectedOcrProvider ? window.selectedOcrProvider("") : "ocrspace");
            var token = document.querySelector('meta[name="_csrf"]');
            var header = document.querySelector('meta[name="_csrf_header"]');
            var xhr = new XMLHttpRequest();
            xhr.open("POST", "/auction/parse-sheet");
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
                    fillForm(result);
                    setStatus(result.message || "Fields filled from the auction sheet.", result.success);
                } catch (e) {
                    setStatus("Could not read the sheet. Fill the form manually.", false);
                }
                showLoader(false);
            };
            xhr.onerror = function () {
                showLoader(false);
                setStatus("Upload failed. Fill the form manually.", false);
            };
            xhr.ontimeout = function () {
                showLoader(false);
                setStatus("Reading the sheet is taking too long. Please wait a bit and try again.", false);
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

    function isFieldEmpty(fieldId) {
        var el = document.getElementById(fieldId);
        return !el || !el.value || el.value.trim().length === 0;
    }

    function getInputForFieldId(fieldId) {
        return document.getElementById(fieldId);
    }

    function highlightField(fieldId) {
        Array.prototype.forEach.call(document.querySelectorAll(".sheet-field-target"), function (node) {
            node.classList.remove("sheet-field-target");
        });
        var el = getInputForFieldId(fieldId);
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
        var el = getInputForFieldId(fieldId);
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

    var vehicleForm = document.getElementById("vehicleForm");
    var chassisInput = document.getElementById("chassisNo");
    var chassisNotice = document.getElementById("chassisDuplicateNotice");
    var chassisDuplicateActive = false;
    var chassisCheckTimer = null;
    var lastCheckedChassis = "";

    function isNewVehicleForm() {
        if (!vehicleForm) {
            return false;
        }
        var editMode = vehicleForm.querySelector('input[name="editMode"]');
        return editMode && editMode.value === "false";
    }

    function escapeHtml(text) {
        var div = document.createElement("div");
        div.textContent = text == null ? "" : String(text);
        return div.innerHTML;
    }

    function clearChassisDuplicateNotice() {
        chassisDuplicateActive = false;
        lastCheckedChassis = "";
        if (chassisNotice) {
            chassisNotice.hidden = true;
            chassisNotice.innerHTML = "";
        }
        if (chassisInput) {
            chassisInput.setCustomValidity("");
        }
    }

    function showChassisDuplicateNotice(result) {
        if (!chassisNotice || !result || !result.exists) {
            clearChassisDuplicateNotice();
            return;
        }
        chassisDuplicateActive = true;
        lastCheckedChassis = (result.chassisNo || "").trim();
        var msg = result.message || "This chassis number is already in the import pipeline.";
        var url = result.pipelineUrl || "#";
        chassisNotice.innerHTML =
            "<strong>Chassis already registered</strong>" +
            "<p>" + escapeHtml(msg) + "</p>" +
            "<p class=\"chassis-duplicate-actions\">" +
            "<a class=\"panel-btn\" href=\"" + escapeHtml(url) + "\">Open existing import pipeline</a>" +
            "<span class=\"chassis-duplicate-hint\">Do not save again — continue the existing record instead.</span>" +
            "</p>";
        chassisNotice.hidden = false;
        if (chassisInput) {
            chassisInput.setCustomValidity("This chassis number is already in the import pipeline.");
        }
    }

    function checkChassisDuplicate() {
        if (!isNewVehicleForm() || !chassisInput) {
            return;
        }
        var value = (chassisInput.value || "").trim();
        if (!value) {
            clearChassisDuplicateNotice();
            return;
        }
        if (value === lastCheckedChassis && chassisDuplicateActive) {
            return;
        }
        fetch("/auction/check-chassis?chassisNo=" + encodeURIComponent(value))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("check failed");
                }
                return response.json();
            })
            .then(function (result) {
                if ((chassisInput.value || "").trim() !== value) {
                    return;
                }
                if (result.exists) {
                    showChassisDuplicateNotice(result);
                } else {
                    clearChassisDuplicateNotice();
                }
            })
            .catch(function () {
            });
    }

    function scheduleChassisCheck() {
        if (!isNewVehicleForm()) {
            return;
        }
        if (chassisCheckTimer) {
            clearTimeout(chassisCheckTimer);
        }
        chassisCheckTimer = setTimeout(checkChassisDuplicate, 350);
    }

    if (isNewVehicleForm() && chassisInput) {
        chassisInput.addEventListener("blur", checkChassisDuplicate);
        chassisInput.addEventListener("input", function () {
            if (chassisDuplicateActive && (chassisInput.value || "").trim() !== lastCheckedChassis) {
                clearChassisDuplicateNotice();
            }
            scheduleChassisCheck();
        });
    }

    if (vehicleForm && isNewVehicleForm()) {
        vehicleForm.addEventListener("submit", function (e) {
            if (chassisDuplicateActive) {
                e.preventDefault();
                if (chassisNotice) {
                    chassisNotice.scrollIntoView({ behavior: "smooth", block: "nearest" });
                }
                if (chassisInput) {
                    chassisInput.reportValidity();
                    chassisInput.focus();
                }
            }
        });
    }
})();
