(function () {
    var DATE_TEXT = /^(\d{4})[\/.\-](\d{1,2})[\/.\-](\d{1,2})$/;

    function pad(value) {
        return ("0" + value).slice(-2);
    }

    function toIso(value) {
        if (!value) {
            return "";
        }
        var text = String(value).trim();
        var iso = text.match(/^(\d{4}-\d{2}-\d{2})/);
        if (iso) {
            return iso[1];
        }
        var match = text.match(DATE_TEXT);
        if (!match) {
            return "";
        }
        return match[1] + "-" + pad(match[2]) + "-" + pad(match[3]);
    }

    function toSlash(value) {
        var iso = toIso(value);
        if (!iso) {
            return "";
        }
        var parts = iso.split("-");
        var year = Number(parts[0]);
        var month = Number(parts[1]);
        var day = Number(parts[2]);
        var check = new Date(Date.UTC(year, month - 1, day));
        if (check.getUTCFullYear() !== year || check.getUTCMonth() !== month - 1 || check.getUTCDate() !== day) {
            return "";
        }
        return iso.replace(/-/g, "/");
    }

    function isDateInput(input) {
        if (!input || input.classList.contains("slash-date-picker")) {
            return false;
        }
        if (input.closest && input.closest(".slash-date")) {
            return false;
        }
        if (input.type === "date") {
            return true;
        }
        if (input.type !== "text") {
            return false;
        }
        var marker = (input.name || "") + " " + (input.id || "") + " " + (input.getAttribute("placeholder") || "");
        return /date/i.test(marker) || /y{2,4}/i.test(input.getAttribute("placeholder") || "");
    }

    function bindText(input, picker) {
        function syncFromText() {
            var slash = toSlash(input.value);
            if (input.value && !slash && input.value.replace(/\D/g, "").length >= 8) {
                input.setCustomValidity("Use YYYY/MM/DD");
                return;
            }
            input.setCustomValidity("");
            if (slash) {
                input.value = slash;
            }
            if (picker) {
                picker.value = slash ? slash.replace(/\//g, "-") : "";
            }
        }

        input.addEventListener("input", function () {
            var digits = input.value.replace(/\D/g, "").slice(0, 8);
            var formatted = digits;
            if (digits.length > 4) {
                formatted = digits.slice(0, 4) + "/" + digits.slice(4);
            }
            if (digits.length > 6) {
                formatted = digits.slice(0, 4) + "/" + digits.slice(4, 6) + "/" + digits.slice(6);
            }
            input.value = formatted;
            input.setCustomValidity("");
        });
        input.addEventListener("change", syncFromText);
        input.addEventListener("blur", syncFromText);
        if (picker) {
            picker.addEventListener("change", function () {
                input.value = picker.value ? picker.value.replace(/-/g, "/") : "";
                input.setCustomValidity("");
            });
        }
        syncFromText();
    }

    function enhance(input) {
        var slash = toSlash(input.value);
        if (input.readOnly || input.disabled) {
            if (slash) {
                input.type = "text";
                input.value = slash;
            }
            if (isDateInput(input) || input.type === "date") {
                input.type = "text";
                input.placeholder = "YYYY/MM/DD";
            }
            return;
        }

        var wrap = document.createElement("span");
        wrap.className = "slash-date";
        input.parentNode.insertBefore(wrap, input);
        wrap.appendChild(input);
        input.type = "text";
        input.placeholder = "YYYY/MM/DD";
        input.maxLength = 10;
        input.autoComplete = "off";
        input.inputMode = "numeric";
        input.value = slash;

        var icon = document.createElement("i");
        icon.className = "fa-regular fa-calendar";
        icon.setAttribute("aria-hidden", "true");
        wrap.appendChild(icon);

        var picker = document.createElement("input");
        picker.type = "date";
        picker.className = "slash-date-picker";
        picker.tabIndex = -1;
        picker.setAttribute("aria-label", "Choose date");
        picker.value = slash ? slash.replace(/\//g, "-") : "";
        wrap.appendChild(picker);
        bindText(input, picker);
    }

    function formatShownDates() {
        var nodes = document.querySelectorAll("em, td, dd, small");
        Array.prototype.forEach.call(nodes, function (node) {
            if (node.children.length) {
                return;
            }
            var slash = toSlash(node.textContent);
            if (slash) {
                node.textContent = slash;
            }
        });
    }

    function boot() {
        Array.prototype.forEach.call(document.querySelectorAll("input.readonly-field"), function (input) {
            if (input.type !== "text" || (input.closest && input.closest(".slash-date"))) {
                return;
            }
            var slash = toSlash(input.value);
            if (slash) {
                input.value = slash;
            }
        });
        Array.prototype.forEach.call(document.querySelectorAll("input"), function (input) {
            if (isDateInput(input)) {
                enhance(input);
            }
        });
        formatShownDates();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", boot);
    } else {
        boot();
    }
})();
