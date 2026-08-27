(function () {
    var loader = document.getElementById("fillLoader");
    var currentTab = 1;
    var chassisNoInput = document.querySelector("#clearanceForm [name='chassisNo']");
    var pageStates = {
        1: { previewUrl: null, previewIsPdf: false, previewName: "" },
        2: { previewUrl: null, previewIsPdf: false, previewName: "" },
        3: { previewUrl: null, previewIsPdf: false, previewName: "" }
    };

    function pageMetaId(page, suffix) {
        return "page" + page + suffix;
    }

    function getPageState(page) {
        return pageStates[page];
    }

    function setStatus(page, text, ok) {
        var status = document.getElementById("uploadStatus" + page);
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
    }

    function isPdfType(contentType, name) {
        if (contentType && contentType.indexOf("pdf") >= 0) {
            return true;
        }
        return name && /\.pdf$/i.test(name);
    }

    function resolvePreviewUrl(page) {
        var state = getPageState(page);
        var storedNameInput = document.getElementById(pageMetaId(page, "StoredName"));
        if (storedNameInput && storedNameInput.value) {
            state.previewUrl = "/customs/documents/" + encodeURIComponent(storedNameInput.value);
            var originalNameInput = document.getElementById(pageMetaId(page, "OriginalName"));
            var contentTypeInput = document.getElementById(pageMetaId(page, "ContentType"));
            state.previewName = originalNameInput ? originalNameInput.value : storedNameInput.value;
            state.previewIsPdf = isPdfType(
                contentTypeInput ? contentTypeInput.value : "",
                state.previewName + " " + storedNameInput.value
            );
        }
        return state.previewUrl;
    }

    function openViewer(page) {
        var state = getPageState(page);
        var url = resolvePreviewUrl(page);
        if (!url) {
            setStatus(page, "Upload a document first to preview it.", false);
            return;
        }
        if (window.DocumentViewer) {
            DocumentViewer.open({
                url: url,
                title: state.previewName || ("Clearance page " + page),
                isPdf: state.previewIsPdf
            });
        }
    }

    function closeViewer() {
        if (window.DocumentViewer) {
            DocumentViewer.close();
        }
    }

    function getChassisNo() {
        if (chassisNoInput && chassisNoInput.value) {
            return chassisNoInput.value.trim();
        }
        var match = window.location.pathname.match(/\/customs\/([^/]+)/);
        return match ? decodeURIComponent(match[1]) : "";
    }

    function updateTabBadge(page, uploaded) {
        var badge = document.getElementById("tabBadge" + page);
        if (badge) {
            badge.hidden = !uploaded;
        }
    }

    function switchTab(tab) {
        currentTab = tab;
        var tabs = document.querySelectorAll(".clearance-tab");
        var panels = document.querySelectorAll(".clearance-tab-panel");
        Array.prototype.forEach.call(tabs, function (btn) {
            var isActive = btn.getAttribute("data-tab") === String(tab);
            btn.classList.toggle("is-active", isActive);
        });
        Array.prototype.forEach.call(panels, function (panel) {
            var isActive = panel.getAttribute("data-tab-panel") === String(tab);
            panel.classList.toggle("is-active", isActive);
            panel.hidden = !isActive;
        });
        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    function initTabs() {
        var gotoButtons = document.querySelectorAll("[data-tab-goto]");
        var tabButtons = document.querySelectorAll(".clearance-tab");

        Array.prototype.forEach.call(gotoButtons, function (btn) {
            btn.addEventListener("click", function () {
                var tab = parseInt(btn.getAttribute("data-tab-goto"), 10);
                if (tab >= 1 && tab <= 3) {
                    switchTab(tab);
                }
            });
        });
        Array.prototype.forEach.call(tabButtons, function (btn) {
            btn.addEventListener("click", function () {
                var tab = parseInt(btn.getAttribute("data-tab"), 10);
                if (tab >= 1 && tab <= 3) {
                    switchTab(tab);
                }
            });
        });

        var page1Stored = document.getElementById("page1StoredName");
        var page2Stored = document.getElementById("page2StoredName");
        var page3Stored = document.getElementById("page3StoredName");
        if (page1Stored && page1Stored.value) {
            updateTabBadge(1, true);
        }
        if (page2Stored && page2Stored.value) {
            updateTabBadge(2, true);
        }
        if (page3Stored && page3Stored.value) {
            updateTabBadge(3, true);
        }
        if (page3Stored && page3Stored.value && (!page1Stored || !page1Stored.value) && (!page2Stored || !page2Stored.value)) {
            switchTab(3);
        } else if (page2Stored && page2Stored.value && (!page1Stored || !page1Stored.value)) {
            switchTab(2);
        }
        var params = new URLSearchParams(window.location.search);
        var requestedTab = parseInt(params.get("tab"), 10);
        if (requestedTab >= 1 && requestedTab <= 3) {
            switchTab(requestedTab);
        }
    }

    function uploadDocument(page, file, onSuccess, onError) {
        var chassisNo = getChassisNo();
        if (!chassisNo) {
            onError("Chassis number is missing. Reload the page and try again.");
            return;
        }
        var data = new FormData();
        data.append("file", file);
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        var xhr = new XMLHttpRequest();
        xhr.open(
            "POST",
            "/customs/" + encodeURIComponent(chassisNo) + "/upload-document?page=" + page
        );
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

    var JEVIC_FIELDS = [
        "jevicChassisVin", "jevicMake", "jevicModel", "jevicInspectionDate",
        "jevicLocation", "jevicCertificateNo", "jevicIssueDate", "jevicCurrentOdometer",
        "jevicAuctionReadingDate", "jevicDealerReadingDate", "jevicDeregistrationReadingDate"
    ];

    var ASSESSMENT_FIELDS = [
        "assessmentOffice", "assessmentNoticeRef", "assessmentModel", "assessmentPackages",
        "assessmentCustomsReference", "assessmentDeclarantReference", "assessmentReference",
        "assessmentDeclarantId", "assessmentDeclarantName", "assessmentDeclarantAddress",
        "assessmentDeclarantChaExp", "assessmentConsigneeId", "assessmentConsigneeName",
        "assessmentConsigneeAddress", "assessmentTaxOtc", "assessmentTaxCom", "assessmentTaxExm",
        "assessmentTaxCid", "assessmentTaxSur", "assessmentTaxXid", "assessmentTaxVat",
        "assessmentTaxVel", "assessmentTotalAssessed", "assessmentTotalPaid"
    ];

    function isEmptyValue(value) {
        if (value == null) {
            return true;
        }
        var trimmed = String(value).trim();
        if (!trimmed) {
            return true;
        }
        var lower = trimmed.toLowerCase();
        return trimmed === "-" || trimmed === "—" || trimmed === "--"
            || lower === "n/a" || lower === "na" || lower === "nil" || lower === "none";
    }

    function fillForm(result, page) {
        if (page === 1) {
            JEVIC_FIELDS.forEach(function (fieldId) {
                var el = document.getElementById(fieldId);
                if (el) {
                    el.value = "";
                }
            });
        } else if (page === 3) {
            ASSESSMENT_FIELDS.forEach(function (fieldId) {
                var el = document.getElementById(fieldId);
                if (el) {
                    el.value = "";
                }
            });
        }

        var fields = result.fields || {};
        Object.keys(fields).forEach(function (key) {
            var value = fields[key];
            if (isEmptyValue(value)) {
                return;
            }
            var el = document.getElementById(key);
            if (el) {
                el.value = String(value).trim();
            }
        });
        var ocr = document.getElementById("ocrTextPage" + page);
        if (ocr && result.rawText) {
            ocr.value = result.rawText;
        }
    }

    function initPage(page) {
        var uploadLabel = document.getElementById("uploadLabel" + page);
        var input = document.getElementById("sheetFile" + page);
        var previewEmpty = document.getElementById("sheetPreviewEmpty" + page);
        var previewImage = document.getElementById("sheetPreviewImage" + page);
        var previewPdf = document.getElementById("sheetPreviewPdf" + page);
        var fileNameLabel = document.getElementById("sheetFileName" + page);
        var fillBtn = document.getElementById("autoFillBtn" + page);
        var previewBtn = document.getElementById("previewSheetBtn" + page);
        var storedNameInput = document.getElementById(pageMetaId(page, "StoredName"));
        var originalNameInput = document.getElementById(pageMetaId(page, "OriginalName"));
        var contentTypeInput = document.getElementById(pageMetaId(page, "ContentType"));
        var previewPanel = document.getElementById("sheetPreview" + page);
        var state = getPageState(page);
        var selectedFile = null;
        var objectUrl = null;

        if (!input) {
            return;
        }

        function hasSavedDocument() {
            return !!(storedNameInput && storedNameInput.value.trim());
        }

        function canAutoFill() {
            return !!selectedFile || hasSavedDocument();
        }

        function canPreview() {
            return !!state.previewUrl || hasSavedDocument();
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

        function clearObjectUrl() {
            if (objectUrl) {
                URL.revokeObjectURL(objectUrl);
                objectUrl = null;
            }
        }

        function updatePreviewState(url, name, contentType) {
            state.previewUrl = url;
            state.previewName = name || ("Clearance page " + page);
            state.previewIsPdf = isPdfType(contentType, name);
        }

        function renderPreview(url, name, contentType) {
            if (previewPanel) {
                previewPanel.classList.add("has-file");
            }
            if (previewEmpty) {
                previewEmpty.hidden = true;
            }
            if (fileNameLabel) {
                fileNameLabel.textContent = name || ("Page " + page);
            }
            updatePreviewState(url, name, contentType);
            updateActionButtons();
            if (state.previewIsPdf) {
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
            if (window.DocumentViewer) {
                DocumentViewer.refresh();
            }
        }

        function setMetadata(originalName, storedName, contentType) {
            if (originalNameInput) {
                originalNameInput.value = originalName || "";
            }
            if (storedNameInput) {
                storedNameInput.value = storedName || "";
            }
            if (contentTypeInput) {
                contentTypeInput.value = contentType || "";
            }
            updateTabBadge(page, !!(storedName && storedName.length > 0));
        }

        function selectFile(file) {
            selectedFile = file;
            updateActionButtons();
            setStatus(page, "Saving document...", true);

            uploadDocument(page, file, function (result) {
                clearObjectUrl();
                selectedFile = file;
                objectUrl = URL.createObjectURL(file);
                setMetadata(result.originalName, result.storedName, result.contentType);
                renderPreview(objectUrl, result.originalName || file.name, result.contentType);
                if (result.storedName) {
                    state.previewUrl = "/customs/documents/" + encodeURIComponent(result.storedName);
                }
                setStatus(page, "Document saved. Preview it, use Auto fill, then continue.", true);
                updateActionButtons();
            }, function (message) {
                selectedFile = null;
                updateActionButtons();
                setStatus(page, message, false);
            });
        }

        function getFileForParse(callback) {
            if (selectedFile) {
                callback(selectedFile);
                return;
            }
            resolvePreviewUrl(page);
            if (!state.previewUrl) {
                setStatus(page, "Upload page " + page + " first.", false);
                return;
            }
            fetch(state.previewUrl)
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("fetch failed");
                    }
                    return response.blob();
                })
                .then(function (blob) {
                    var name = (originalNameInput && originalNameInput.value) || ("page" + page + ".jpg");
                    callback(new File([blob], name, { type: blob.type }));
                })
                .catch(function () {
                    setStatus(page, "Upload page " + page + " first.", false);
                });
        }

        function autoFill() {
            showLoader(true);
            setStatus(page, "Reading document...", true);
            getFileForParse(function (file) {
                var data = new FormData();
                data.append("file", file);
                data.append("page", String(page));
                var token = document.querySelector('meta[name="_csrf"]');
                var header = document.querySelector('meta[name="_csrf_header"]');
                var xhr = new XMLHttpRequest();
                xhr.open("POST", "/customs/parse-document");
                xhr.timeout = 180000;
                if (token && header) {
                    xhr.setRequestHeader(header.content, token.content);
                }
                xhr.onload = function () {
                    showLoader(false);
                    if (xhr.status === 401 || xhr.status === 403) {
                        setStatus(page, "Please sign in again, then try Auto fill.", false);
                        return;
                    }
                    try {
                        var result = JSON.parse(xhr.responseText);
                        fillForm(result, page);
                        setStatus(page, result.message || "Fields filled.", result.success);
                    } catch (e) {
                        setStatus(page, "Could not read the document. Fill manually.", false);
                    }
                };
                xhr.onerror = function () {
                    showLoader(false);
                    setStatus(page, "Auto fill failed.", false);
                };
                xhr.ontimeout = function () {
                    showLoader(false);
                    setStatus(page, "Reading took too long. Try again.", false);
                };
                xhr.send(data);
            });
        }

        function initExisting() {
            if (!hasSavedDocument()) {
                updateActionButtons();
                return;
            }
            var url = resolvePreviewUrl(page);
            var name = originalNameInput && originalNameInput.value
                ? originalNameInput.value
                : storedNameInput.value;
            var contentType = contentTypeInput ? contentTypeInput.value : "";
            renderPreview(url, name, contentType);
            updateTabBadge(page, true);
            setStatus(page, "Saved document loaded. You can preview it or upload a new one.", true);
            updateActionButtons();
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
            previewBtn.addEventListener("click", function () {
                openViewer(page);
            });
        }
        initExisting();
    }

    initTabs();
    initPage(1);
    initPage(2);
    initPage(3);
})();
