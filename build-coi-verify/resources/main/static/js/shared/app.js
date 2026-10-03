(function () {
    var sidebar = document.getElementById("sidebar");
    var toggle = document.getElementById("sidebarToggle");
    var closeBtn = document.getElementById("sidebarClose");
    var backdrop = document.getElementById("sidebarBackdrop");

    function setNavOpen(open) {
        if (!sidebar) {
            return;
        }
        sidebar.classList.toggle("is-open", open);
        document.body.classList.toggle("nav-open", open);
        if (backdrop) {
            backdrop.hidden = !open;
        }
        if (toggle) {
            toggle.setAttribute("aria-expanded", open ? "true" : "false");
        }
    }

    if (toggle && sidebar) {
        toggle.addEventListener("click", function () {
            setNavOpen(!sidebar.classList.contains("is-open"));
        });
    }
    if (closeBtn) {
        closeBtn.addEventListener("click", function () {
            setNavOpen(false);
        });
    }
    if (backdrop) {
        backdrop.addEventListener("click", function () {
            setNavOpen(false);
        });
    }
    if (sidebar) {
        sidebar.addEventListener("click", function (event) {
            var link = event.target.closest ? event.target.closest("a") : null;
            if (link && sidebar.contains(link) && window.matchMedia("(max-width: 1100px)").matches) {
                setNavOpen(false);
            }
        });
    }
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            setNavOpen(false);
        }
    });

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

    function restartLoaderGif(root) {
        var gif = root.querySelector(".app-loader-gif");
        if (!gif) {
            return;
        }
        var src = gif.getAttribute("src");
        if (src) {
            gif.setAttribute("src", src);
        }
    }

    function showPageLoader(show) {
        var loader = document.getElementById("pageLoader");
        if (!loader) {
            return;
        }
        loader.hidden = !show;
        loader.setAttribute("aria-hidden", show ? "false" : "true");
        document.body.classList.toggle("is-page-loading", show);
        if (show) {
            restartLoaderGif(loader);
        }
    }

    window.showAppLoader = showPageLoader;

    Array.prototype.forEach.call(document.querySelectorAll(".app-loader"), function (el) {
        if (typeof MutationObserver === "undefined") {
            return;
        }
        new MutationObserver(function () {
            if (!el.hidden) {
                restartLoaderGif(el);
                el.setAttribute("aria-hidden", "false");
            } else {
                el.setAttribute("aria-hidden", "true");
            }
        }).observe(el, { attributes: true, attributeFilter: ["hidden"] });
    });

    document.addEventListener("click", function (event) {
        if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
            return;
        }
        var link = event.target.closest ? event.target.closest("a") : null;
        if (!link || link.getAttribute("download") != null) {
            return;
        }
        var target = link.getAttribute("target");
        if (target && target !== "_self") {
            return;
        }
        var href = link.getAttribute("href");
        if (!href || href.charAt(0) === "#" || href.indexOf("javascript:") === 0 || href.indexOf("mailto:") === 0) {
            return;
        }
        try {
            var url = new URL(link.href, window.location.href);
            if (url.origin !== window.location.origin) {
                return;
            }
            if (url.pathname === window.location.pathname && url.search === window.location.search) {
                return;
            }
        } catch (ignored) {
            return;
        }
        showPageLoader(true);
    });

    document.addEventListener("submit", function (event) {
        if (event.defaultPrevented) {
            return;
        }
        var form = event.target;
        if (!form || (form.className && form.className.indexOf("table-action-form") >= 0)) {
            return;
        }
        showPageLoader(true);
    });

    window.addEventListener("pageshow", function () {
        showPageLoader(false);
    });
})();
