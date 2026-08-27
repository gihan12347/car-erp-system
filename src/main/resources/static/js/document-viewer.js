(function () {
    var viewer;
    var viewerTitle;
    var viewerImage;
    var viewerPdf;
    var viewerBody;
    var zoomInBtn;
    var zoomOutBtn;
    var viewerClose;
    var viewerToolbar;
    var zoom = 1;
    var isDragging = false;
    var dragOffsetX = 0;
    var dragOffsetY = 0;
    var initialized = false;

    function cacheRefs() {
        viewer = document.getElementById("sheetViewer");
        if (!viewer) {
            return false;
        }
        viewerTitle = document.getElementById("sheetViewerTitle");
        viewerImage = document.getElementById("sheetViewerImage");
        viewerPdf = document.getElementById("sheetViewerPdf");
        viewerBody = document.getElementById("sheetViewerBody");
        zoomInBtn = document.getElementById("sheetZoomIn");
        zoomOutBtn = document.getElementById("sheetZoomOut");
        viewerClose = document.getElementById("sheetViewerClose");
        viewerToolbar = viewer.querySelector(".sheet-viewer-toolbar");
        return true;
    }

    function isPdfUrl(url) {
        return !!(url && /\.pdf(\?|#|$)/i.test(url));
    }

    function resolveTitle(el) {
        var panel = el.closest(".sheet-document, .panel");
        if (panel) {
            var heading = panel.querySelector(".panel-head h2");
            if (heading && heading.textContent.trim()) {
                return heading.textContent.trim();
            }
        }
        if (el.alt) {
            return el.alt;
        }
        if (el.title) {
            return el.title;
        }
        return "Document preview";
    }

    function applyZoom() {
        if (!viewerImage || viewerImage.hidden) {
            return;
        }
        viewerImage.style.transform = "scale(" + zoom + ")";
    }

    function open(options) {
        if (!cacheRefs()) {
            return;
        }
        var url = options && options.url;
        if (!url) {
            return;
        }
        var title = options.title || "Document preview";
        var pdf = options.isPdf != null ? options.isPdf : isPdfUrl(url);

        zoom = 1;
        if (viewerTitle) {
            viewerTitle.textContent = title;
        }

        if (pdf) {
            if (viewerImage) {
                viewerImage.hidden = true;
                viewerImage.removeAttribute("src");
                viewerImage.style.transform = "";
            }
            if (viewerPdf) {
                viewerPdf.hidden = false;
                viewerPdf.src = url;
            }
            if (zoomInBtn) {
                zoomInBtn.disabled = true;
            }
            if (zoomOutBtn) {
                zoomOutBtn.disabled = true;
            }
        } else {
            if (viewerPdf) {
                viewerPdf.hidden = true;
                viewerPdf.removeAttribute("src");
            }
            if (viewerImage) {
                viewerImage.hidden = false;
                viewerImage.src = url;
                viewerImage.alt = title;
                applyZoom();
            }
            if (zoomInBtn) {
                zoomInBtn.disabled = false;
            }
            if (zoomOutBtn) {
                zoomOutBtn.disabled = false;
            }
        }

        viewer.hidden = false;
        if (viewerBody) {
            viewerBody.scrollTop = 0;
            viewerBody.scrollLeft = 0;
        }
    }

    function close() {
        if (!viewer) {
            return;
        }
        viewer.hidden = true;
    }

    function openFromFrame(frame) {
        var img = frame.querySelector("img:not([hidden])");
        var iframe = frame.querySelector("iframe:not([hidden])");
        var el = img || iframe;
        if (!el || !el.getAttribute("src")) {
            return;
        }
        open({
            url: el.getAttribute("src"),
            title: resolveTitle(el),
            isPdf: !!iframe && !iframe.hidden
        });
    }

    function enhancePreviewFrame(frame) {
        if (!frame) {
            return;
        }
        var img = frame.querySelector("img");
        var iframe = frame.querySelector("iframe");
        var imgVisible = !!(img && !img.hidden && img.getAttribute("src"));
        var iframeVisible = !!(iframe && !iframe.hidden && iframe.getAttribute("src"));
        if (!imgVisible && !iframeVisible) {
            return;
        }

        var key = (iframeVisible ? "pdf:" : "img:") + (iframeVisible ? iframe.getAttribute("src") : img.getAttribute("src"));
        if (frame.dataset.previewKey === key) {
            return;
        }

        var oldOverlay = frame.querySelector(".sheet-preview-open");
        if (oldOverlay) {
            oldOverlay.parentNode.removeChild(oldOverlay);
        }
        frame.classList.remove("has-open-preview", "has-pdf-preview");
        frame.dataset.previewKey = key;
        frame.dataset.previewEnhanced = "1";
        frame.classList.add("has-open-preview");

        if (imgVisible) {
            if (img.dataset.previewClickBound !== "1") {
                img.dataset.previewClickBound = "1";
                img.addEventListener("click", function (event) {
                    event.preventDefault();
                    if (img.hidden || !img.getAttribute("src")) {
                        return;
                    }
                    open({
                        url: img.getAttribute("src"),
                        title: resolveTitle(img),
                        isPdf: false
                    });
                });
            }
        }

        if (iframeVisible) {
            frame.classList.add("has-pdf-preview");
            var overlay = document.createElement("button");
            overlay.type = "button";
            overlay.className = "sheet-preview-open";
            overlay.setAttribute("aria-label", "Open document preview");
            overlay.innerHTML = "<i class=\"fa-solid fa-magnifying-glass-plus\"></i><span>Open preview</span>";
            overlay.addEventListener("click", function (event) {
                event.preventDefault();
                event.stopPropagation();
                open({
                    url: iframe.getAttribute("src"),
                    title: resolveTitle(iframe),
                    isPdf: true
                });
            });
            frame.appendChild(overlay);
        }
    }

    function bindPreviewFrames() {
        document.querySelectorAll(".sheet-preview-frame").forEach(enhancePreviewFrame);
    }

    function initDrag() {
        if (!viewerToolbar || viewerToolbar.dataset.dragBound === "1") {
            return;
        }
        viewerToolbar.dataset.dragBound = "1";
        viewerToolbar.addEventListener("mousedown", function (event) {
            if (event.target.closest(".sheet-viewer-tools")) {
                return;
            }
            isDragging = true;
            viewer.classList.add("is-dragging");
            var rect = viewer.getBoundingClientRect();
            dragOffsetX = event.clientX - rect.left;
            dragOffsetY = event.clientY - rect.top;
            event.preventDefault();
        });
        document.addEventListener("mousemove", function (event) {
            if (!isDragging || !viewer) {
                return;
            }
            var left = event.clientX - dragOffsetX;
            var top = event.clientY - dragOffsetY;
            var maxLeft = Math.max(0, window.innerWidth - viewer.offsetWidth);
            var maxTop = Math.max(0, window.innerHeight - viewer.offsetHeight);
            viewer.style.left = Math.min(Math.max(0, left), maxLeft) + "px";
            viewer.style.top = Math.min(Math.max(0, top), maxTop) + "px";
            viewer.style.right = "auto";
        });
        document.addEventListener("mouseup", function () {
            if (!isDragging) {
                return;
            }
            isDragging = false;
            viewer.classList.remove("is-dragging");
        });
    }

    function initControls() {
        if (viewerClose && viewerClose.dataset.bound !== "1") {
            viewerClose.dataset.bound = "1";
            viewerClose.addEventListener("click", close);
        }
        if (zoomInBtn && zoomInBtn.dataset.bound !== "1") {
            zoomInBtn.dataset.bound = "1";
            zoomInBtn.addEventListener("click", function () {
                zoom = Math.min(3, zoom + 0.25);
                applyZoom();
            });
        }
        if (zoomOutBtn && zoomOutBtn.dataset.bound !== "1") {
            zoomOutBtn.dataset.bound = "1";
            zoomOutBtn.addEventListener("click", function () {
                zoom = Math.max(0.5, zoom - 0.25);
                applyZoom();
            });
        }
        if (viewerBody && viewerBody.dataset.bound !== "1") {
            viewerBody.dataset.bound = "1";
            viewerBody.addEventListener("wheel", function (event) {
                if (!viewer || viewer.hidden || !viewerImage || viewerImage.hidden) {
                    return;
                }
                event.preventDefault();
                zoom = event.deltaY < 0 ? Math.min(3, zoom + 0.1) : Math.max(0.5, zoom - 0.1);
                applyZoom();
            }, { passive: false });
        }
        if (document.documentElement.dataset.docViewerEsc !== "1") {
            document.documentElement.dataset.docViewerEsc = "1";
            document.addEventListener("keydown", function (event) {
                if (event.key === "Escape") {
                    close();
                }
            });
        }
    }

    function init() {
        if (initialized) {
            bindPreviewFrames();
            return;
        }
        if (!cacheRefs()) {
            return;
        }
        initialized = true;
        initDrag();
        initControls();
        bindPreviewFrames();
    }

    window.DocumentViewer = {
        open: open,
        close: close,
        refresh: bindPreviewFrames
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
