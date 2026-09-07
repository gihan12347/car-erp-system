(function () {
    var gallery = document.getElementById("photoGallery");
    var input = document.getElementById("photoFile");
    var addSlot = document.getElementById("photoAdd");
    var status = document.getElementById("uploadStatus");
    var countLabel = document.getElementById("photoCountLabel");
    var pendingFields = document.getElementById("pendingPhotoFields");
    var chassisInput = document.getElementById("chassisNo");
    var max = gallery ? parseInt(gallery.getAttribute("data-max") || "5", 10) : 5;
    var uploading = false;

    if (!gallery || !input) {
        return;
    }

    function chassisNo() {
        if (chassisInput && chassisInput.value) {
            return chassisInput.value.trim();
        }
        return (gallery.getAttribute("data-chassis") || "").trim();
    }

    function cards() {
        return gallery.querySelectorAll(".photo-card");
    }

    function count() {
        return cards().length;
    }

    function remaining() {
        return Math.max(0, max - count());
    }

    function setStatus(text, ok) {
        if (!status) {
            return;
        }
        status.textContent = text || "";
        status.className = "upload-status" + (text ? (ok ? " ok" : " err") : "");
    }

    function updateChrome() {
        if (countLabel) {
            countLabel.textContent = count() + " / " + max;
        }
        if (addSlot) {
            if (remaining() === 0) {
                addSlot.classList.add("is-hidden");
            } else {
                addSlot.classList.remove("is-hidden");
            }
        }
    }

    function csrfHeaders(xhr) {
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header) {
            xhr.setRequestHeader(header.content, token.content);
        }
    }

    function addPendingFields(storedName, originalName, contentType) {
        if (!pendingFields) {
            return;
        }
        function hidden(name, value) {
            var el = document.createElement("input");
            el.type = "hidden";
            el.name = name;
            el.value = value || "";
            pendingFields.appendChild(el);
        }
        hidden("storedNames", storedName);
        hidden("originalNames", originalName);
        hidden("contentTypes", contentType);
    }

    function removePendingFields(storedName) {
        if (!pendingFields) {
            return;
        }
        var inputs = pendingFields.querySelectorAll("input[name='storedNames']");
        for (var i = 0; i < inputs.length; i++) {
            if (inputs[i].value === storedName) {
                var original = pendingFields.querySelectorAll("input[name='originalNames']")[i];
                var type = pendingFields.querySelectorAll("input[name='contentTypes']")[i];
                inputs[i].parentNode.removeChild(inputs[i]);
                if (original) {
                    original.parentNode.removeChild(original);
                }
                if (type) {
                    type.parentNode.removeChild(type);
                }
                return;
            }
        }
    }

    function renderCard(photo) {
        var card = document.createElement("div");
        card.className = "photo-card";
        if (photo.photoId) {
            card.setAttribute("data-photo-id", photo.photoId);
        }
        card.setAttribute("data-stored", photo.storedName);
        var img = document.createElement("img");
        img.src = photo.previewUrl || ("/photos/documents/" + encodeURIComponent(photo.storedName));
        img.alt = photo.originalName || "Vehicle image";
        var remove = document.createElement("button");
        remove.type = "button";
        remove.className = "photo-remove";
        remove.title = "Remove image";
        remove.innerHTML = '<i class="fa-solid fa-trash"></i>';
        card.appendChild(img);
        card.appendChild(remove);
        gallery.insertBefore(card, addSlot);
        updateChrome();
    }

    function postForm(url, data, onSuccess, onError) {
        var xhr = new XMLHttpRequest();
        xhr.open("POST", url);
        xhr.timeout = 120000;
        csrfHeaders(xhr);
        xhr.onload = function () {
            if (xhr.status === 401 || xhr.status === 403) {
                onError("Please sign in again, then try again.");
                return;
            }
            try {
                var result = JSON.parse(xhr.responseText);
                if (!result.success) {
                    onError(result.message || "Could not update images.");
                    return;
                }
                onSuccess(result);
            } catch (e) {
                onError("Could not update images.");
            }
        };
        xhr.onerror = function () {
            onError("Upload failed.");
        };
        xhr.ontimeout = function () {
            onError("The request took too long.");
        };
        xhr.send(data);
    }

    function uploadFile(file, done) {
        var data = new FormData();
        data.append("file", file);
        var chassis = chassisNo();
        if (chassis) {
            data.append("chassisNo", chassis);
        }
        postForm("/photos/upload", data, function (result) {
            renderCard(result);
            if (!result.photoId) {
                addPendingFields(result.storedName, result.originalName, result.contentType);
            }
            done(null, result);
        }, done);
    }

    function isAllowedImage(file) {
        var name = (file && file.name ? file.name : "").toLowerCase();
        var type = (file && file.type ? file.type : "").toLowerCase();
        return type === "image/jpeg" || type === "image/png" || type === "image/webp"
            || name.lastIndexOf(".jpg") === name.length - 4
            || name.lastIndexOf(".jpeg") === name.length - 5
            || name.lastIndexOf(".png") === name.length - 4
            || name.lastIndexOf(".webp") === name.length - 5;
    }

    function uploadFiles(fileList) {
        var files = [];
        var i;
        for (i = 0; i < fileList.length; i++) {
            if (!isAllowedImage(fileList[i])) {
                setStatus("Only JPG, PNG, or WEBP images are allowed.", false);
                continue;
            }
            files.push(fileList[i]);
        }
        if (!files.length) {
            return;
        }
        var allowed = remaining();
        if (allowed <= 0) {
            setStatus("A vehicle can have at most " + max + " images.", false);
            return;
        }
        if (files.length > allowed) {
            files = files.slice(0, allowed);
            setStatus("Only " + allowed + " more image" + (allowed === 1 ? "" : "s") + " can be added.", false);
        } else {
            setStatus("Saving image...", true);
        }
        uploading = true;
        var index = 0;
        function next() {
            if (index >= files.length) {
                uploading = false;
                setStatus(count() + " of " + max + " images added.", true);
                return;
            }
            uploadFile(files[index], function (error) {
                if (error) {
                    uploading = false;
                    setStatus(error, false);
                    return;
                }
                index += 1;
                next();
            });
        }
        next();
    }

    function removeCard(card) {
        var photoId = card.getAttribute("data-photo-id");
        var stored = card.getAttribute("data-stored");
        var chassis = chassisNo();
        if (photoId && chassis) {
            postForm("/photos/" + encodeURIComponent(chassis) + "/photos/" + encodeURIComponent(photoId) + "/delete",
                new FormData(), function () {
                    card.parentNode.removeChild(card);
                    updateChrome();
                    setStatus("Image removed.", true);
                }, function (message) {
                    setStatus(message, false);
                });
            return;
        }
        if (stored) {
            var data = new FormData();
            data.append("storedName", stored);
            postForm("/photos/delete-stored", data, function () {
                removePendingFields(stored);
                card.parentNode.removeChild(card);
                updateChrome();
                setStatus("Image removed.", true);
            }, function (message) {
                setStatus(message, false);
            });
        }
    }

    input.addEventListener("change", function () {
        if (uploading) {
            return;
        }
        if (input.files && input.files.length) {
            var selected = input.files;
            input.value = "";
            uploadFiles(selected);
        }
    });

    gallery.addEventListener("click", function (event) {
        var button = event.target.closest ? event.target.closest(".photo-remove") : null;
        if (!button) {
            return;
        }
        var card = button.closest ? button.closest(".photo-card") : button.parentNode;
        if (card) {
            removeCard(card);
        }
    });

    updateChrome();
})();
