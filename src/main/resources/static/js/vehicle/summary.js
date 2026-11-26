(function () {
    var back = document.getElementById("summaryBack");
    if (back) {
        back.addEventListener("click", function () {
            if (window.history.length > 1) {
                window.history.back();
                return;
            }
            window.location.href = "/import";
        });
    }

    var filters = document.querySelectorAll("[data-doc-filter]");
    var cards = document.querySelectorAll(".doc-card");
    var empty = document.getElementById("summaryDocEmpty");

    function applyFilter(status) {
        var visible = 0;
        for (var i = 0; i < cards.length; i++) {
            var card = cards[i];
            var show = status === "all" || card.getAttribute("data-status") === status;
            card.hidden = !show;
            if (show) {
                visible++;
            }
        }
        if (empty) {
            empty.hidden = visible !== 0;
        }
    }

    for (var f = 0; f < filters.length; f++) {
        filters[f].addEventListener("click", function () {
            for (var i = 0; i < filters.length; i++) {
                filters[i].classList.toggle("is-active", filters[i] === this);
            }
            applyFilter(this.getAttribute("data-doc-filter"));
        });
    }

    var thumbs = document.querySelectorAll(".summary-thumb");
    var stageImage = document.getElementById("summaryStageImage");
    var stageFile = document.getElementById("summaryStageFile");
    var stageFileName = document.getElementById("summaryStageFileName");
    var stageFileOpen = document.getElementById("summaryStageFileOpen");
    var stageZoom = document.getElementById("summaryStageZoom");
    var stageCount = document.getElementById("summaryStageCount");
    var stageName = document.getElementById("summaryStageName");

    function selectPhoto(thumb) {
        if (!thumb) {
            return;
        }
        for (var i = 0; i < thumbs.length; i++) {
            var selected = thumbs[i] === thumb;
            thumbs[i].classList.toggle("is-selected", selected);
            thumbs[i].setAttribute("aria-selected", selected ? "true" : "false");
        }
        var url = thumb.getAttribute("data-photo-url");
        var title = thumb.getAttribute("data-photo-title") || "Photo";
        var isPdf = thumb.getAttribute("data-photo-pdf") === "true";
        var index = thumb.getAttribute("data-photo-index");
        if (stageName) {
            stageName.textContent = title;
        }
        if (stageCount) {
            stageCount.textContent = index + " / " + thumbs.length;
        }
        if (isPdf) {
            if (stageImage) {
                stageImage.hidden = true;
            }
            if (stageFile) {
                stageFile.hidden = false;
            }
            if (stageFileName) {
                stageFileName.textContent = title;
            }
            if (stageFileOpen) {
                stageFileOpen.setAttribute("data-doc-url", url);
                stageFileOpen.setAttribute("data-doc-title", title);
                stageFileOpen.setAttribute("data-doc-pdf", "true");
            }
            if (stageZoom) {
                stageZoom.hidden = true;
            }
            return;
        }
        if (stageFile) {
            stageFile.hidden = true;
        }
        if (stageImage) {
            stageImage.hidden = false;
            stageImage.src = url;
            stageImage.alt = title;
        }
        if (stageZoom) {
            stageZoom.hidden = false;
            stageZoom.setAttribute("data-doc-url", url);
            stageZoom.setAttribute("data-doc-title", title);
            stageZoom.setAttribute("data-doc-pdf", "false");
        }
    }

    for (var t = 0; t < thumbs.length; t++) {
        thumbs[t].addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();
            selectPhoto(this);
        });
    }

    var thumbsWrap = document.getElementById("summaryThumbs");
    if (thumbsWrap) {
        thumbsWrap.addEventListener("keydown", function (event) {
            if (!thumbs.length) {
                return;
            }
            var current = 0;
            for (var i = 0; i < thumbs.length; i++) {
                if (thumbs[i].classList.contains("is-selected")) {
                    current = i;
                }
            }
            var next = current;
            if (event.key === "ArrowRight" || event.key === "ArrowDown") {
                next = (current + 1) % thumbs.length;
            } else if (event.key === "ArrowLeft" || event.key === "ArrowUp") {
                next = (current - 1 + thumbs.length) % thumbs.length;
            } else {
                return;
            }
            event.preventDefault();
            selectPhoto(thumbs[next]);
            thumbs[next].focus();
        });
    }

    document.addEventListener("click", function (event) {
        var trigger = event.target.closest ? event.target.closest("[data-doc-url]") : null;
        if (!trigger || !window.DocumentViewer) {
            return;
        }
        event.preventDefault();
        var pdfFlag = trigger.getAttribute("data-doc-pdf");
        window.DocumentViewer.open({
            url: trigger.getAttribute("data-doc-url"),
            title: trigger.getAttribute("data-doc-title") || "Document",
            isPdf: pdfFlag === "true"
        });
    });

    var shareForm = document.getElementById("shareVehicleForm");
    var shareButton = document.getElementById("shareVehicleButton");
    var shareInput = document.getElementById("shareVehicleUrl");
    var shareError = document.getElementById("shareVehicleError");
    if (shareForm && shareButton && shareInput) {
        var shareLabel = shareButton.querySelector("span");

        function markCopied() {
            if (shareLabel) {
                shareLabel.textContent = "Link copied";
            }
        }

        function showUrl(url) {
            shareInput.value = url;
        }

        function showShareError(message) {
            if (!shareError) {
                return;
            }
            shareError.hidden = false;
            shareError.textContent = message;
        }

        function copyWithInput(url) {
            showUrl(url);
            var area = document.createElement("textarea");
            area.value = url;
            area.setAttribute("readonly", "");
            area.style.position = "fixed";
            area.style.left = "-9999px";
            document.body.appendChild(area);
            area.select();
            try {
                document.execCommand("copy");
            } catch (ignore) {
                // Clipboard access can be blocked; the button still confirms the attempt.
            }
            document.body.removeChild(area);
            markCopied();
        }

        function copyNow(url) {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(url).then(markCopied).catch(function () {
                    copyWithInput(url);
                });
                return;
            }
            copyWithInput(url);
        }

        function createLink() {
            return fetch(shareForm.getAttribute("action"), {
                method: "POST",
                body: new FormData(shareForm),
                headers: { "Accept": "application/json" },
                credentials: "same-origin"
            }).then(function (response) {
                return response.json().then(function (data) {
                    if (!response.ok || !data.url) {
                        throw new Error(data.error || "Could not create the sharing link.");
                    }
                    return data.url;
                });
            });
        }

        shareForm.addEventListener("submit", function (event) {
            event.preventDefault();
            if (shareError) {
                shareError.hidden = true;
            }
            var existing = (shareInput.value || "").trim();
            if (existing) {
                copyNow(existing);
                return;
            }
            var pending = createLink();
            if (navigator.clipboard && window.ClipboardItem) {
                var item = new ClipboardItem({
                    "text/plain": pending.then(function (url) {
                        showUrl(url);
                        return new Blob([url], { type: "text/plain" });
                    })
                });
                navigator.clipboard.write([item]).then(markCopied).catch(function () {
                    pending.then(copyWithInput).catch(function (error) {
                        showShareError(error.message || "Could not create the sharing link.");
                    });
                });
                return;
            }
            pending.then(function (url) {
                showUrl(url);
                copyNow(url);
            }).catch(function (error) {
                showShareError(error.message || "Could not create the sharing link.");
            });
        });
    }

    var priceModal = document.getElementById("summaryPriceModal");
    var priceButtons = document.querySelectorAll(".summary-price-edit");
    if (priceModal && priceButtons.length) {
        var priceKind = document.getElementById("summaryPriceKind");
        var priceTitle = document.getElementById("summaryPriceTitle");
        var priceValue = document.getElementById("summaryPriceValue");

        function formatSummaryPrice(raw) {
            var cleaned = String(raw || "").replace(/,/g, "").replace(/[^\d.]/g, "");
            if (!cleaned) {
                return "";
            }
            var dot = cleaned.indexOf(".");
            var whole = dot === -1 ? cleaned : cleaned.slice(0, dot);
            var fraction = dot === -1 ? null : cleaned.slice(dot + 1).replace(/\./g, "").slice(0, 2);
            whole = whole.replace(/^0+(?=\d)/, "");
            if (!whole) {
                whole = fraction === null ? "" : "0";
            }
            var grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
            if (fraction === null) {
                return grouped;
            }
            return grouped + "." + fraction;
        }

        function caretAfterDigits(formatted, count) {
            if (count <= 0) {
                return 0;
            }
            var seen = 0;
            var i;
            for (i = 0; i < formatted.length; i++) {
                var ch = formatted.charAt(i);
                if ((ch >= "0" && ch <= "9") || ch === ".") {
                    seen++;
                    if (seen >= count) {
                        return i + 1;
                    }
                }
            }
            return formatted.length;
        }

        function closePriceModal() {
            priceModal.classList.remove("is-open");
            document.body.classList.remove("yard-modal-open");
        }

        function openPriceModal(button) {
            if (priceKind) {
                priceKind.value = button.getAttribute("data-price-kind") || "actual";
            }
            if (priceTitle) {
                priceTitle.textContent = button.getAttribute("data-price-label") || "Price";
            }
            if (priceValue) {
                priceValue.value = formatSummaryPrice(button.getAttribute("data-price-value") || "");
            }
            priceModal.classList.add("is-open");
            document.body.classList.add("yard-modal-open");
            if (priceValue) {
                priceValue.focus();
                priceValue.select();
            }
        }

        for (var p = 0; p < priceButtons.length; p++) {
            priceButtons[p].addEventListener("click", function () {
                if (!this.getAttribute("data-price-kind")) {
                    return;
                }
                openPriceModal(this);
            });
        }
        var backdrop = document.getElementById("summaryPriceBackdrop");
        var cancel = document.getElementById("summaryPriceCancel");
        var close = document.getElementById("summaryPriceClose");
        if (backdrop) {
            backdrop.addEventListener("click", closePriceModal);
        }
        if (cancel) {
            cancel.addEventListener("click", closePriceModal);
        }
        if (close) {
            close.addEventListener("click", closePriceModal);
        }
        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && priceModal.classList.contains("is-open")) {
                closePriceModal();
            }
        });
        if (priceValue) {
            priceValue.addEventListener("input", function () {
                var start = priceValue.selectionStart || 0;
                var before = priceValue.value.slice(0, start).replace(/[^\d.]/g, "").length;
                var formatted = formatSummaryPrice(priceValue.value);
                if (priceValue.value !== formatted) {
                    priceValue.value = formatted;
                    var next = caretAfterDigits(formatted, before);
                    priceValue.setSelectionRange(next, next);
                }
            });
        }
    }

    var extraModal = document.getElementById("extraCostModal");
    var extraButtons = document.querySelectorAll(".extra-cost-open");
    if (extraModal && extraButtons.length) {
        var extraDate = document.getElementById("extraCostDate");
        var extraReason = document.getElementById("extraCostReason");
        var extraAmount = document.getElementById("extraCostAmount");
        var extraForm = document.getElementById("extraCostForm");

        function formatExtraCost(raw) {
            var cleaned = String(raw || "").replace(/,/g, "").replace(/[^\d.]/g, "");
            if (!cleaned) {
                return "";
            }
            var dot = cleaned.indexOf(".");
            var whole = dot === -1 ? cleaned : cleaned.slice(0, dot);
            var fraction = dot === -1 ? null : cleaned.slice(dot + 1).replace(/\./g, "").slice(0, 2);
            whole = whole.replace(/^0+(?=\d)/, "");
            if (!whole) {
                whole = fraction === null ? "" : "0";
            }
            var grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
            if (fraction === null) {
                return grouped;
            }
            return grouped + "." + fraction;
        }

        function closeExtraCost() {
            extraModal.classList.remove("is-open");
            document.body.classList.remove("yard-modal-open");
        }

        function openExtraCost() {
            if (extraForm) {
                extraForm.reset();
            }
            extraModal.classList.add("is-open");
            document.body.classList.add("yard-modal-open");
            if (extraDate) {
                extraDate.focus();
            }
        }

        for (var e = 0; e < extraButtons.length; e++) {
            extraButtons[e].addEventListener("click", openExtraCost);
        }
        var extraBackdrop = document.getElementById("extraCostBackdrop");
        var extraCancel = document.getElementById("extraCostCancel");
        var extraClose = document.getElementById("extraCostClose");
        if (extraBackdrop) {
            extraBackdrop.addEventListener("click", closeExtraCost);
        }
        if (extraCancel) {
            extraCancel.addEventListener("click", closeExtraCost);
        }
        if (extraClose) {
            extraClose.addEventListener("click", closeExtraCost);
        }
        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && extraModal.classList.contains("is-open")) {
                closeExtraCost();
            }
        });
        if (extraAmount) {
            extraAmount.addEventListener("input", function () {
                var start = extraAmount.selectionStart || 0;
                var before = extraAmount.value.slice(0, start).replace(/[^\d.]/g, "").length;
                var formatted = formatExtraCost(extraAmount.value);
                if (extraAmount.value !== formatted) {
                    extraAmount.value = formatted;
                    var next = 0;
                    var seen = 0;
                    var i;
                    for (i = 0; i < formatted.length; i++) {
                        var ch = formatted.charAt(i);
                        if ((ch >= "0" && ch <= "9") || ch === ".") {
                            seen++;
                            if (seen >= before) {
                                next = i + 1;
                                break;
                            }
                        }
                    }
                    if (before > 0 && next === 0) {
                        next = formatted.length;
                    }
                    extraAmount.setSelectionRange(next, next);
                }
            });
        }
        if (extraForm) {
            extraForm.addEventListener("submit", function (event) {
                var date = extraDate ? String(extraDate.value || "").trim() : "";
                var reason = extraReason ? String(extraReason.value || "").trim() : "";
                var amount = extraAmount ? String(extraAmount.value || "").replace(/,/g, "").trim() : "";
                if (!date || !reason || !amount || amount === "." || !(Number(amount) > 0)) {
                    event.preventDefault();
                }
            });
        }
    }
})();
