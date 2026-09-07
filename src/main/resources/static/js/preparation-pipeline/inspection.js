(function () {
    var form = document.getElementById("inspectionForm");
    var list = document.getElementById("inspectionItemList");
    var addBtn = document.getElementById("addInspectionItem");
    var template = document.getElementById("inspectionItemTemplate");
    var saveBtn = document.getElementById("saveInspectionBtn");
    var completed = document.getElementById("inspectionCompleted");
    var statusEl = document.getElementById("inspectionJobStatus");
    var errorEl = document.getElementById("inspectionJobError");
    if (!form || !list) {
        return;
    }

    function cards() {
        return list.querySelectorAll(".inspection-item-card");
    }

    function csrfHeaders() {
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        var headers = { "Content-Type": "application/json" };
        if (token && header) {
            headers[header.content] = token.content;
        }
        return headers;
    }

    function showBanner(ok, text) {
        if (statusEl) {
            statusEl.hidden = !ok;
            statusEl.textContent = ok ? text : "";
        }
        if (errorEl) {
            errorEl.hidden = ok;
            errorEl.textContent = ok ? "" : text;
        }
    }

    function fieldValue(card, suffix) {
        var field = card.querySelector('[name$=".' + suffix + '"]');
        return field ? String(field.value || "").trim() : "";
    }

    function selectedResult(card) {
        var checked = card.querySelector('input[type="radio"]:checked');
        return checked ? checked.value : "";
    }

    function syncItemNotes(card) {
        if (!card) {
            return;
        }
        var notes = card.querySelector(".inspection-item-notes");
        if (notes) {
            notes.hidden = selectedResult(card) !== "NO";
        }
    }

    function syncAllNotes() {
        Array.prototype.forEach.call(cards(), syncItemNotes);
    }

    function cardTitle(card) {
        var input = card.querySelector(".inspection-item-title-input");
        if (input) {
            return input.value.trim();
        }
        var label = card.querySelector(".inspection-item-title");
        return label ? label.textContent.trim() : "";
    }

    function formComplete() {
        var inspector = form.querySelector("#inspector");
        var date = form.querySelector("#inspectionDate");
        if (!inspector || !inspector.value.trim() || !date || !date.value.trim()) {
            return false;
        }
        var rows = cards();
        if (!rows.length) {
            return false;
        }
        for (var i = 0; i < rows.length; i++) {
            if (!cardTitle(rows[i]) || !selectedResult(rows[i])) {
                return false;
            }
        }
        return true;
    }

    function refreshSave() {
        var ready = formComplete();
        if (saveBtn) {
            saveBtn.disabled = !ready;
        }
        if (completed) {
            completed.disabled = !ready;
            if (!ready) {
                completed.checked = false;
            }
        }
    }

    function reindex() {
        Array.prototype.forEach.call(cards(), function (card, index) {
            card.setAttribute("data-index", String(index));
            var num = card.querySelector(".inspection-item-num");
            if (num) {
                num.textContent = String(index + 1);
            }
            Array.prototype.forEach.call(card.querySelectorAll("input, select, textarea"), function (field) {
                if (field.name) {
                    field.name = field.name.replace(/lines\[\d+]/g, "lines[" + index + "]");
                }
                if (field.id) {
                    field.id = field.id.replace(/line\d+_/g, "line" + index + "_");
                }
            });
            Array.prototype.forEach.call(card.querySelectorAll("label[for]"), function (label) {
                label.htmlFor = label.htmlFor.replace(/line\d+_/g, "line" + index + "_");
            });
            var sortOrder = card.querySelector('input[name$=".sortOrder"]');
            if (sortOrder) {
                sortOrder.value = String(index);
            }
        });
        refreshSave();
    }

    function addItem() {
        if (!template) {
            return;
        }
        var html = template.innerHTML
            .replace(/__idx__/g, String(cards().length))
            .replace(/__num__/g, String(cards().length + 1));
        var wrap = document.createElement("div");
        wrap.innerHTML = html.trim();
        list.appendChild(wrap.firstChild);
        reindex();
        syncAllNotes();
    }

    function createWorkshopJob(card, radio) {
        var title = cardTitle(card);
        if (!title) {
            radio.checked = false;
            showBanner(false, "Enter the item name before selecting No.");
            refreshSave();
            return;
        }
        var note = card.querySelector(".inspection-job-note");
        var lineId = fieldValue(card, "id");
        var body = {
            lineId: lineId ? Number(lineId) : null,
            itemKey: fieldValue(card, "itemKey"),
            itemTitle: title,
            notes: fieldValue(card, "notes"),
            catalogItem: fieldValue(card, "catalogItem") === "true"
        };
        var url = form.getAttribute("data-fail-url");
        if (!url) {
            return;
        }
        fetch(url, {
            method: "POST",
            credentials: "same-origin",
            headers: csrfHeaders(),
            body: JSON.stringify(body)
        })
            .then(function (response) {
                return response.json().then(function (payload) {
                    return { ok: response.ok, payload: payload };
                });
            })
            .then(function (result) {
                var message = (result.payload && result.payload.message)
                    || (result.ok ? "Workshop job created." : "Could not create the workshop job.");
                showBanner(result.ok && result.payload && result.payload.success, message);
                if (note) {
                    note.hidden = false;
                    note.textContent = message;
                }
            })
            .catch(function () {
                showBanner(false, "Could not create the workshop job.");
            });
    }

    if (addBtn) {
        addBtn.addEventListener("click", function (event) {
            event.preventDefault();
            addItem();
        });
    }

    list.addEventListener("click", function (event) {
        var button = event.target.closest(".inspection-item-remove");
        if (!button) {
            return;
        }
        event.preventDefault();
        var card = button.closest(".inspection-item-card");
        if (card && card.classList.contains("is-custom")) {
            card.parentNode.removeChild(card);
            reindex();
        }
    });

    list.addEventListener("change", function (event) {
        var radio = event.target;
        if (radio && radio.type === "radio") {
            var card = radio.closest(".inspection-item-card");
            syncItemNotes(card);
            if (radio.value === "NO" && radio.checked) {
                createWorkshopJob(card, radio);
            }
        }
        refreshSave();
    });

    form.addEventListener("input", refreshSave);
    form.addEventListener("change", refreshSave);

    reindex();
    syncAllNotes();
})();
