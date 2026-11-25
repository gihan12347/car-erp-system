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
})();
