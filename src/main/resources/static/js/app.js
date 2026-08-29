(function () {
    var sidebar = document.getElementById("sidebar");
    var toggle = document.getElementById("sidebarToggle");
    if (toggle && sidebar) {
        toggle.addEventListener("click", function () {
            sidebar.classList.toggle("is-open");
        });
    }

    var navGroups = document.querySelectorAll("[data-nav-group]");
    Array.prototype.forEach.call(navGroups, function (toggle) {
        var group = toggle.parentElement;
        if (group && group.classList.contains("is-open")) {
            toggle.setAttribute("aria-expanded", "true");
        }
        toggle.addEventListener("click", function (event) {
            event.preventDefault();
            group.classList.toggle("is-open");
            toggle.setAttribute("aria-expanded", group.classList.contains("is-open") ? "true" : "false");
        });
    });

    var dropdowns = document.querySelectorAll("[data-dropdown]");
    Array.prototype.forEach.call(dropdowns, function (dropdown) {
        var trigger = dropdown.querySelector("[data-dropdown-trigger]");
        if (!trigger) {
            return;
        }
        trigger.addEventListener("click", function (event) {
            event.stopPropagation();
            var alreadyOpen = dropdown.classList.contains("open");
            Array.prototype.forEach.call(dropdowns, function (item) {
                item.classList.remove("open");
            });
            if (!alreadyOpen) {
                dropdown.classList.add("open");
            }
        });
    });

    document.addEventListener("click", function () {
        Array.prototype.forEach.call(dropdowns, function (item) {
            item.classList.remove("open");
        });
    });

    var counters = document.querySelectorAll("[data-count]");
    Array.prototype.forEach.call(counters, function (el) {
        var end = parseInt(el.getAttribute("data-count"), 10);
        var start = 0;
        var steps = 28;
        var step = 0;
        var timer = setInterval(function () {
            step += 1;
            el.textContent = String(Math.round((end * step) / steps));
            if (step >= steps) {
                el.textContent = String(end);
                clearInterval(timer);
            }
        }, 30);
    });

    requestAnimationFrame(function () {
        var bars = document.querySelectorAll(".bar i[data-width]");
        Array.prototype.forEach.call(bars, function (bar) {
            bar.style.width = bar.getAttribute("data-width") + "%";
        });
    });

    window.selectedOcrProvider = function (suffix) {
        var picker = document.getElementById("ocrModelPicker" + (suffix || ""));
        if (picker) {
            var value = picker.getAttribute("data-ocr-provider");
            if (value) {
                return value;
            }
        }
        return "ocrspace";
    };

    var pickers = document.querySelectorAll(".ocr-model-picker");
    Array.prototype.forEach.call(pickers, function (picker) {
        var storageKey = "ocrProvider:" + window.location.pathname + ":" + picker.id;
        try {
            var saved = window.localStorage.getItem(storageKey);
            if (saved === "google" || saved === "ocrspace") {
                picker.setAttribute("data-ocr-provider", saved);
                var buttons = picker.querySelectorAll(".ocr-model-btn");
                Array.prototype.forEach.call(buttons, function (item) {
                    var on = item.getAttribute("data-provider") === saved;
                    item.classList.toggle("is-selected", on);
                    item.setAttribute("aria-pressed", on ? "true" : "false");
                });
            }
        } catch (ignored) {
        }
        picker.addEventListener("click", function (event) {
            var btn = event.target.closest ? event.target.closest(".ocr-model-btn") : null;
            if (!btn || !picker.contains(btn)) {
                return;
            }
            var provider = btn.getAttribute("data-provider");
            picker.setAttribute("data-ocr-provider", provider);
            var buttons = picker.querySelectorAll(".ocr-model-btn");
            Array.prototype.forEach.call(buttons, function (item) {
                var on = item === btn;
                item.classList.toggle("is-selected", on);
                item.setAttribute("aria-pressed", on ? "true" : "false");
            });
            try {
                window.localStorage.setItem(storageKey, provider);
            } catch (ignored) {
            }
        });
    });
})();
