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
        "scheduleType", "emissionCo", "emissionNmhc", "emissionNox", "emissionPm",
        "emissionHc", "emissionHcNox", "emissionThc", "emissionCh4", "emissionSmoke",
        "threePointSeatBelts", "twoPointSeatBelts", "driverAirbag", "passengerAirbag", "absFitted",
        "make", "model", "chassisVin", "placeOfInspection", "inspectionDate", "remarks"
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
        return "/standards/documents/" + encodeURIComponent(storedName);
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
        markDocumentReady(name || "Standards certificate");
        if (previewEmpty) {
            previewEmpty.hidden = true;
        }
        hideSavedPreviewElements();
        previewIsPdf = isPdfType(contentType, name);
        previewUrl = url;
        previewName = name || "Standards certificate";
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
            setStatus("Upload a standards certificate first to preview it.", false);
            return;
        }
        if (window.DocumentViewer) {
            DocumentViewer.open({
                url: previewUrl,
                title: previewName || "Standards certificate",
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
        xhr.open("POST", "/standards/upload-document");
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
            setStatus("Document saved. Click Auto fill to read fields from the standards certificate.", true);
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
        setStatus("Saved standards certificate loaded. Use Auto fill to read fields from the document.", true);
        updateActionButtons();
    }

    function isTruthy(value) {
        return /^(true|yes|y|1|✓|✔)$/i.test(String(value).trim());
    }

    function applyValue(el, value) {
        if (!el) {
            return false;
        }
        if (el.type === "checkbox") {
            el.checked = isTruthy(value);
            return true;
        }
        if (el.type === "radio") {
            el.checked = String(el.value) === String(value);
            return el.checked;
        }
        el.value = value;
        return true;
    }

    function clearField(key) {
        if (key === "scheduleType") {
            var radios = document.querySelectorAll('input[name="scheduleType"]');
            Array.prototype.forEach.call(radios, function (radio) {
                radio.checked = false;
            });
            return;
        }
        var el = document.getElementById(key);
        if (!el) {
            return;
        }
        if (el.type === "checkbox") {
            el.checked = false;
            return;
        }
        el.value = "";
    }

    function fillField(key, value) {
        if (!value) {
            return false;
        }
        if (key === "scheduleType") {
            var radios = document.querySelectorAll('input[name="scheduleType"]');
            var matched = false;
            Array.prototype.forEach.call(radios, function (radio) {
                if (applyValue(radio, value)) {
                    matched = true;
                }
            });
            return matched;
        }
        return applyValue(document.getElementById(key), value);
    }

    function fillForm(result) {
        var fields = result.fields || {};
        var filled = 0;

        FIELD_ORDER.forEach(function (key) {
            clearField(key);
        });

        FIELD_ORDER.forEach(function (key) {
            if (fillField(key, fields[key])) {
                filled++;
            }
        });

        Object.keys(fields).forEach(function (key) {
            if (FIELD_ORDER.indexOf(key) >= 0) {
                return;
            }
            if (fillField(key, fields[key])) {
                filled++;
            }
        });

        var ocr = document.getElementById("ocrText");
        if (ocr && result.rawText) {
            ocr.value = result.rawText;
        }
        return filled;
    }

    function withParseFile(callback, onError) {
        if (selectedFile) {
            callback(selectedFile);
            return;
        }
        if (!hasSavedDocument()) {
            onError("Upload a standards certificate first.");
            return;
        }
        var url = buildDocumentUrl(documentStoredNameInput.value);
        var name = documentOriginalNameInput && documentOriginalNameInput.value
            ? documentOriginalNameInput.value
            : "standards-certificate";
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
            setStatus("Upload a standards certificate first.", false);
            return;
        }
        showLoader(true);
        setStatus("Reading standards certificate...", true);
        withParseFile(function (file) {
            var data = new FormData();
            data.append("file", file);
            data.append("provider", window.selectedOcrProvider ? window.selectedOcrProvider("") : "ocrspace");
            var token = document.querySelector('meta[name="_csrf"]');
            var header = document.querySelector('meta[name="_csrf_header"]');
            var xhr = new XMLHttpRequest();
            xhr.open("POST", "/standards/parse-document");
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

    function resolveField(fieldId) {
        if (fieldId === "scheduleType") {
            return document.querySelector('input[name="scheduleType"]:checked')
                || document.getElementById("scheduleTypeIII");
        }
        return document.getElementById(fieldId);
    }

    function isFieldEmpty(fieldId) {
        if (fieldId === "scheduleType") {
            return !document.querySelector('input[name="scheduleType"]:checked');
        }
        var el = document.getElementById(fieldId);
        if (!el) {
            return true;
        }
        if (el.type === "checkbox") {
            return !el.checked;
        }
        return !el.value || el.value.trim().length === 0;
    }

    function highlightField(fieldId) {
        Array.prototype.forEach.call(document.querySelectorAll(".sheet-field-target"), function (node) {
            node.classList.remove("sheet-field-target");
        });
        var el = resolveField(fieldId);
        if (el) {
            el.classList.add("sheet-field-target");
        }
    }

    function setCurrentIndexFromActiveElement() {
        var active = document.activeElement;
        if (!active) {
            return;
        }
        if (active.name === "scheduleType") {
            currentFieldIndex = FIELD_ORDER.indexOf("scheduleType");
            return;
        }
        if (!active.id) {
            return;
        }
        var idx = FIELD_ORDER.indexOf(active.id);
        if (idx >= 0) {
            currentFieldIndex = idx;
        }
    }

    function focusInput(fieldId) {
        var el = resolveField(fieldId);
        if (!el) {
            return false;
        }
        highlightField(fieldId);
        el.focus();
        try {
            if (el.setSelectionRange && el.type !== "checkbox" && el.type !== "radio") {
                var v = el.value || "";
                el.setSelectionRange(v.length, v.length);
            }
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
