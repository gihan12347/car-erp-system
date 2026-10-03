(function () {
    if (window.__documentCropLoaded) {
        return;
    }
    window.__documentCropLoaded = true;

    var modal;
    var imageEl;
    var frameEl;
    var boxEl;
    var errorEl;
    var applyBtn;
    var activeInput = null;
    var drag = null;
    var MIN_SIZE = 28;

    function ensureModal() {
        if (modal) {
            return;
        }
        modal = document.createElement("div");
        modal.id = "imageCropModal";
        modal.className = "image-crop-modal";
        modal.hidden = true;
        modal.innerHTML =
            '<div class="image-crop-dialog" role="dialog" aria-modal="true" aria-labelledby="imageCropTitle">' +
                '<div class="image-crop-toolbar">' +
                    '<strong id="imageCropTitle">Crop image</strong>' +
                    '<p>Drag the box or its edges, then save. The cropped image replaces the uploaded document.</p>' +
                    '<p class="image-crop-error" id="imageCropError" hidden></p>' +
                '</div>' +
                '<div class="image-crop-stage">' +
                    '<div class="image-crop-frame" id="imageCropFrame">' +
                        '<img id="imageCropImage" alt="Document to crop">' +
                        '<div class="image-crop-box" id="imageCropBox">' +
                            '<span class="image-crop-handle nw" data-handle="nw"></span>' +
                            '<span class="image-crop-handle n" data-handle="n"></span>' +
                            '<span class="image-crop-handle ne" data-handle="ne"></span>' +
                            '<span class="image-crop-handle e" data-handle="e"></span>' +
                            '<span class="image-crop-handle se" data-handle="se"></span>' +
                            '<span class="image-crop-handle s" data-handle="s"></span>' +
                            '<span class="image-crop-handle sw" data-handle="sw"></span>' +
                            '<span class="image-crop-handle w" data-handle="w"></span>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
                '<div class="image-crop-actions">' +
                    '<button type="button" class="panel-btn ghost" id="imageCropCancel">Cancel</button>' +
                    '<button type="button" class="panel-btn" id="imageCropApply">Save crop</button>' +
                '</div>' +
            '</div>';
        document.body.appendChild(modal);
        imageEl = document.getElementById("imageCropImage");
        frameEl = document.getElementById("imageCropFrame");
        boxEl = document.getElementById("imageCropBox");
        errorEl = document.getElementById("imageCropError");
        applyBtn = document.getElementById("imageCropApply");

        document.getElementById("imageCropCancel").addEventListener("click", closeModal);
        applyBtn.addEventListener("click", applyCrop);
        modal.addEventListener("click", function (event) {
            if (event.target === modal) {
                closeModal();
            }
        });
        boxEl.addEventListener("pointerdown", onPointerDown);
        window.addEventListener("pointermove", onPointerMove);
        window.addEventListener("pointerup", endDrag);
        window.addEventListener("pointercancel", endDrag);
        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && modal && !modal.hidden) {
                closeModal();
            }
        });
    }

    function previewImage() {
        return document.getElementById("sheetPreviewImage");
    }

    function imageReady(img) {
        return !!(img && !img.hidden && img.getAttribute("src"));
    }

    function syncCropButtons() {
        var ready = imageReady(previewImage());
        var filling = document.body.classList.contains("is-filling");
        var buttons = document.querySelectorAll("[id^='cropSheetBtn']");
        Array.prototype.forEach.call(buttons, function (button) {
            button.disabled = !ready || filling;
        });
    }

    function fileNameFromPage() {
        var label = document.getElementById("sheetFileName");
        var name = label ? label.textContent.trim() : "";
        if (!name || /no file/i.test(name)) {
            var stored = document.getElementById("documentOriginalName");
            name = stored && stored.value ? stored.value.trim() : "";
        }
        if (!name) {
            return "document.jpg";
        }
        return name.replace(/\.[^.]+$/, "") + ".jpg";
    }

    function showError(message) {
        if (!errorEl) {
            return;
        }
        errorEl.hidden = !message;
        errorEl.textContent = message || "";
    }

    function placeBox(left, top, width, height) {
        var maxW = frameEl.clientWidth;
        var maxH = frameEl.clientHeight;
        if (left < 0) {
            width += left;
            left = 0;
        }
        if (top < 0) {
            height += top;
            top = 0;
        }
        if (left + width > maxW) {
            width = maxW - left;
        }
        if (top + height > maxH) {
            height = maxH - top;
        }
        if (width < MIN_SIZE) {
            width = MIN_SIZE;
            if (left + width > maxW) {
                left = Math.max(0, maxW - width);
            }
        }
        if (height < MIN_SIZE) {
            height = MIN_SIZE;
            if (top + height > maxH) {
                top = Math.max(0, maxH - height);
            }
        }
        boxEl.style.left = left + "px";
        boxEl.style.top = top + "px";
        boxEl.style.width = width + "px";
        boxEl.style.height = height + "px";
    }

    function resetBox() {
        var width = frameEl.clientWidth;
        var height = frameEl.clientHeight;
        if (!width || !height) {
            return;
        }
        var insetX = Math.min(24, width * 0.06);
        var insetY = Math.min(24, height * 0.06);
        placeBox(insetX, insetY, width - insetX * 2, height - insetY * 2);
    }

    function openModal(input) {
        var img = previewImage();
        if (!imageReady(img)) {
            return;
        }
        ensureModal();
        activeInput = input;
        showError("");
        applyBtn.disabled = true;
        boxEl.hidden = true;
        modal.hidden = false;
        imageEl.onload = function () {
            imageEl.onload = null;
            boxEl.hidden = false;
            applyBtn.disabled = false;
            requestAnimationFrame(resetBox);
        };
        imageEl.onerror = function () {
            imageEl.onerror = null;
            applyBtn.disabled = true;
            showError("This image cannot be cropped in the browser. Use a JPG or PNG.");
        };
        imageEl.src = img.src;
    }

    function closeModal() {
        if (!modal) {
            return;
        }
        modal.hidden = true;
        drag = null;
        activeInput = null;
        if (imageEl) {
            imageEl.onload = null;
            imageEl.onerror = null;
            imageEl.removeAttribute("src");
        }
    }

    function onPointerDown(event) {
        if (!boxEl || boxEl.hidden || event.button !== 0) {
            return;
        }
        var handle = event.target.getAttribute("data-handle");
        drag = {
            mode: handle ? "resize" : "move",
            handle: handle || "",
            startX: event.clientX,
            startY: event.clientY,
            left: boxEl.offsetLeft,
            top: boxEl.offsetTop,
            width: boxEl.offsetWidth,
            height: boxEl.offsetHeight
        };
        event.preventDefault();
    }

    function onPointerMove(event) {
        if (!drag) {
            return;
        }
        var dx = event.clientX - drag.startX;
        var dy = event.clientY - drag.startY;
        var left = drag.left;
        var top = drag.top;
        var width = drag.width;
        var height = drag.height;
        var handle = drag.handle;

        if (drag.mode === "move") {
            placeBox(left + dx, top + dy, width, height);
            return;
        }
        if (handle.indexOf("e") >= 0) {
            width = drag.width + dx;
        }
        if (handle.indexOf("s") >= 0) {
            height = drag.height + dy;
        }
        if (handle.indexOf("w") >= 0) {
            width = drag.width - dx;
            left = drag.left + dx;
        }
        if (handle.indexOf("n") >= 0) {
            height = drag.height - dy;
            top = drag.top + dy;
        }
        if (width < MIN_SIZE) {
            if (handle.indexOf("w") >= 0) {
                left = drag.left + drag.width - MIN_SIZE;
            }
            width = MIN_SIZE;
        }
        if (height < MIN_SIZE) {
            if (handle.indexOf("n") >= 0) {
                top = drag.top + drag.height - MIN_SIZE;
            }
            height = MIN_SIZE;
        }
        placeBox(left, top, width, height);
    }

    function endDrag() {
        drag = null;
    }

    function fileInputFor(button) {
        var toolbar = button.closest(".doc-upload-toolbar");
        if (toolbar) {
            var input = toolbar.querySelector("input[type='file']");
            if (input) {
                return input;
            }
        }
        return document.getElementById("sheetFile");
    }

    function applyCrop() {
        if (!imageEl || !imageEl.naturalWidth || !activeInput || !boxEl) {
            return;
        }
        var scaleX = imageEl.naturalWidth / imageEl.clientWidth;
        var scaleY = imageEl.naturalHeight / imageEl.clientHeight;
        var sx = Math.round(boxEl.offsetLeft * scaleX);
        var sy = Math.round(boxEl.offsetTop * scaleY);
        var sw = Math.round(boxEl.offsetWidth * scaleX);
        var sh = Math.round(boxEl.offsetHeight * scaleY);
        sw = Math.max(1, Math.min(sw, imageEl.naturalWidth - sx));
        sh = Math.max(1, Math.min(sh, imageEl.naturalHeight - sy));

        var canvas = document.createElement("canvas");
        canvas.width = sw;
        canvas.height = sh;
        var ctx = canvas.getContext("2d");
        ctx.drawImage(imageEl, sx, sy, sw, sh, 0, 0, sw, sh);

        var input = activeInput;
        var name = fileNameFromPage();
        applyBtn.disabled = true;
        canvas.toBlob(function (blob) {
            applyBtn.disabled = false;
            if (!blob) {
                showError("Could not crop this image. Try a JPG or PNG.");
                return;
            }
            var file = new File([blob], name, { type: "image/jpeg", lastModified: Date.now() });
            var transfer = new DataTransfer();
            transfer.items.add(file);
            closeModal();
            input.files = transfer.files;
            input.dispatchEvent(new Event("change", { bubbles: true }));
        }, "image/jpeg", 0.92);
    }

    function bindButtons() {
        var buttons = document.querySelectorAll("[id^='cropSheetBtn']");
        Array.prototype.forEach.call(buttons, function (button) {
            if (button.getAttribute("data-crop-bound") === "1") {
                return;
            }
            button.setAttribute("data-crop-bound", "1");
            button.addEventListener("click", function () {
                openModal(fileInputFor(button));
            });
        });
    }

    function watchPreview() {
        var img = previewImage();
        if (img && window.MutationObserver) {
            var observer = new MutationObserver(syncCropButtons);
            observer.observe(img, { attributes: true, attributeFilter: ["hidden", "src"] });
        }
        if (window.MutationObserver) {
            var bodyObserver = new MutationObserver(syncCropButtons);
            bodyObserver.observe(document.body, { attributes: true, attributeFilter: ["class"] });
        }
        syncCropButtons();
    }

    function start() {
        bindButtons();
        watchPreview();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", start);
    } else {
        start();
    }
})();
