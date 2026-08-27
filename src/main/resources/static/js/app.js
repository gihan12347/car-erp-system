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
})();
