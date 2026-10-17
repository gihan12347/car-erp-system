(function () {
    var loader = document.getElementById("fillLoader");
    var viewerClose = document.getElementById("sheetViewerClose");
    var prevEmptyBtn = document.getElementById("sheetPrevEmptyField");
    var nextEmptyBtn = document.getElementById("sheetNextEmptyField");
    var FIELD_ORDER = [
        "buyerFullName",
        "buyerNicNo",
        "buyerAddress",
        "buyerDateOfBirth"
    ];
    var PARSE_FIELDS = {
        fullName: "buyerFullName",
        nicNo: "buyerNicNo",
        address: "buyerAddress",
        dateOfBirth: "buyerDateOfBirth"
    };
    var currentFieldIndex = 0;

    function byId(id) {
        return document.getElementById(id);
    }

    function bindSide(suffix, label, originalId, storedId, typeId, ocrId) {
    var uploadLabel = byId("uploadLabel" + suffix);
    var input = byId("sheetFile" + suffix);
    var status = byId("uploadStatus" + suffix);
    var preview = byId("sheetPreview" + suffix);
    var previewFrame = byId("sheetPreviewFrame" + suffix);
    var previewEmpty = byId("sheetPreviewEmpty" + suffix);
    var previewImage = byId("sheetPreviewImage" + suffix);
    var previewPdf = byId("sheetPreviewPdf" + suffix);
    var fileNameLabel = byId("sheetFileName" + suffix);
    var fillBtn = byId("autoFillBtn" + suffix);
    var previewBtn = byId("previewSheetBtn" + suffix);
    var documentOriginalNameInput = byId(originalId);
    var documentStoredNameInput = byId(storedId);
    var documentContentTypeInput = byId(typeId);
    var selectedFile = null;
    var previewUrl = null;
    var previewIsPdf = false;
    var previewName = "";
    var objectUrl = null;

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
        return "/sell/documents/" + encodeURIComponent(storedName);
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
        markDocumentReady(name || label);
        if (previewEmpty) {
            previewEmpty.hidden = true;
        }
        hideSavedPreviewElements();
        previewIsPdf = isPdfType(contentType, name);
        previewUrl = url;
        previewName = name || label;
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
            setStatus("Upload the " + label.toLowerCase() + " first to preview it.", false);
            return;
        }
        if (window.DocumentViewer) {
            DocumentViewer.open({
                url: previewUrl,
                title: previewName || label,
                isPdf: previewIsPdf
            });
        }
    }

    function uploadDocument(file, onSuccess, onError) {
        var data = new FormData();
        data.append("file", file);
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        var xhr = new XMLHttpRequest();
        xhr.open("POST", "/sell/upload-document");
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
            setStatus("Document saved.", true);
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
        setStatus("Saved " + label.toLowerCase() + " loaded.", true);
        updateActionButtons();
    }

    function toDateInputValue(value) {
        if (!value) {
            return "";
        }
        var s = String(value).trim();
        if (/^\d{4}-\d{2}-\d{2}$/.test(s)) {
            return s;
        }
        var iso = s.match(/^(\d{4}-\d{2}-\d{2})(?:[T\s].*)?$/);
        if (iso) {
            return iso[1];
        }
        var m = s.match(/^(\d{4})[\/.\-](\d{1,2})[\/.\-](\d{1,2})/);
        if (m) {
            return m[1] + "-" + ("0" + m[2]).slice(-2) + "-" + ("0" + m[3]).slice(-2);
        }
        m = s.match(/^(\d{1,2})[\/.\-](\d{1,2})[\/.\-](\d{4})/);
        if (m) {
            return m[3] + "-" + ("0" + m[2]).slice(-2) + "-" + ("0" + m[1]).slice(-2);
        }
        return "";
    }

    function toMonthInputValue(value) {
        if (!value) {
            return "";
        }
        var text = String(value).trim();
        var isoDate = text.match(/^(\d{4}-\d{2})(?:-\d{2})?(?:[T\s].*)?$/);
        if (isoDate) {
            return isoDate[1];
        }
        var yearMonth = text.match(/^(\d{4})[/. -](\d{1,2})$/);
        if (yearMonth) {
            return yearMonth[1] + "-" + ("0" + yearMonth[2]).slice(-2);
        }
        var monthYear = text.match(/^(\d{1,2})[/. -](\d{4})$/);
        if (monthYear) {
            return monthYear[2] + "-" + ("0" + monthYear[1]).slice(-2);
        }
        return text;
    }

    function applyValue(el, value) {
        if (!el) {
            return false;
        }
        if (el.type === "date") {
            el.value = toDateInputValue(value);
            return !!el.value;
        }
        if (el.type === "month") {
            el.value = toMonthInputValue(value);
            return !!el.value;
        }
        el.value = value;
        return true;
    }

    function clearField(key) {
        var el = document.getElementById(key);
        if (el && !el.readOnly) {
            el.value = "";
        }
    }

    function fillField(key, value) {
        if (!value) {
            return false;
        }
        return applyValue(document.getElementById(key), value);
    }

    function fillForm(result) {
        var fields = result.fields || {};
        var filled = 0;

        Object.keys(PARSE_FIELDS).forEach(function (key) {
            if (fillField(PARSE_FIELDS[key], fields[key])) {
                filled++;
            }
        });

        var ocr = document.getElementById(ocrId);
        if (ocr && result.rawText) {
            ocr.value = result.rawText;
        }
        var chassisNoEl = document.getElementById("chassisNo");
        if (chassisNoEl && !chassisNoEl.readOnly && !chassisNoEl.value.trim() && fields.chassisNo) {
            chassisNoEl.value = String(fields.chassisNo).trim();
        }
        return filled;
    }

    function withParseFile(callback, onError) {
        if (selectedFile) {
            callback(selectedFile);
            return;
        }
        if (!hasSavedDocument()) {
            onError("Upload a identity card first.");
            return;
        }
        var url = buildDocumentUrl(documentStoredNameInput.value);
        var name = documentOriginalNameInput && documentOriginalNameInput.value
            ? documentOriginalNameInput.value
            : "identity-card";
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
            setStatus("Upload a identity card first.", false);
            return;
        }
        showLoader(true);
        setStatus("Reading identity card...", true);
        withParseFile(function (file) {
            var data = new FormData();
            data.append("file", file);
            var token = document.querySelector('meta[name="_csrf"]');
            var header = document.querySelector('meta[name="_csrf_header"]');
            var xhr = new XMLHttpRequest();
            xhr.open("POST", "/sell/parse-id");
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
                    var message = result.message || ("Filled " + filled + " fields from the document.");
                    setStatus(message, result.success !== false);
                } catch (e) {
                    setStatus("Could not read the document. Fill the form manually.", false);
                }
                showLoader(false);
            };
            xhr.onerror = function () {
                showLoader(false);
                setStatus("Could not read the document. Fill the form manually.", false);
            };
            xhr.ontimeout = function () {
                showLoader(false);
                setStatus("Reading the document is taking too long. Please wait and try again.", false);
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

    initExistingDocument();
    }

    function closeViewer() {
        if (window.DocumentViewer) {
            DocumentViewer.close();
        }
    }

    function resolveField(fieldId) {
        return document.getElementById(fieldId);
    }

    function isFieldEmpty(fieldId) {
        var el = document.getElementById(fieldId);
        if (!el) {
            return true;
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
        if (!active || !active.id) {
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
            if (el.setSelectionRange) {
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

    bindSide("", "Front", "documentOriginalName", "documentStoredName", "documentContentType", "ocrText");
    bindSide("Back", "Back", "documentOriginalNameBack", "documentStoredNameBack", "documentContentTypeBack", "ocrTextBack");
    bindSaveLock();

    function bindSaveLock() {
        var saveBtn = document.getElementById("sellSaveBtn");
        var form = document.getElementById("sellForm");
        if (!saveBtn || !form) {
            return;
        }
        var ids = [
            "buyerFullName",
            "buyerNicNo",
            "buyerDateOfBirth",
            "buyerAddress",
            "sellingPrice",
            "listedOn",
            "saleCode",
            "saleLocationValue"
        ];
        function filled(id) {
            var el = document.getElementById(id);
            return !!(el && String(el.value || "").trim());
        }
        function refresh() {
            var ready = true;
            for (var i = 0; i < ids.length; i++) {
                if (!filled(ids[i])) {
                    ready = false;
                    break;
                }
            }
            saveBtn.disabled = !ready;
        }
        refresh();
        form.addEventListener("input", refresh);
        form.addEventListener("change", refresh);
    }
})();
