(function () {
    function bindHub() {
        var root = document.getElementById("saleHubRoot");
        if (!root) {
            return;
        }
        var buttons = root.querySelectorAll(".view-switch-btn");
        var search = document.getElementById("saleHubSearch");
        var saleView = document.getElementById("saleView");
        var vehicleView = document.getElementById("vehicleView");
        var fillFilter = document.getElementById("saleFillFilter");
        var vehicleFilter = document.getElementById("vehicleSaleFilter");
        var saleFilterWrap = document.getElementById("saleFilterWrap");
        var vehicleFilterWrap = document.getElementById("vehicleFilterWrap");

        function currentView() {
            return root.getAttribute("data-view") === "vehicles" ? "vehicles" : "sales";
        }

        function setView(view, persist) {
            var next = view === "vehicles" ? "vehicles" : "sales";
            root.setAttribute("data-view", next);
            Array.prototype.forEach.call(buttons, function (button) {
                var active = button.getAttribute("data-view") === next;
                button.classList.toggle("is-active", active);
                button.setAttribute("aria-selected", active ? "true" : "false");
            });
            if (saleView) {
                saleView.classList.toggle("is-hidden", next === "vehicles");
            }
            if (vehicleView) {
                vehicleView.classList.toggle("is-hidden", next !== "vehicles");
            }
            if (saleFilterWrap) {
                saleFilterWrap.hidden = next === "vehicles";
            }
            if (vehicleFilterWrap) {
                vehicleFilterWrap.hidden = next !== "vehicles";
            }
            if (persist && window.history && window.history.replaceState) {
                var url = new URL(window.location.href);
                if (next === "sales") {
                    url.searchParams.delete("view");
                } else {
                    url.searchParams.set("view", "vehicles");
                }
                window.history.replaceState({}, "", url.pathname + url.search + url.hash);
            }
            applySearch();
        }

        function applySearch() {
            var needle = search && search.value ? search.value.trim().toLowerCase() : "";
            if (currentView() === "vehicles") {
                filterVehicleView(needle);
            } else {
                filterSaleView(needle);
            }
        }

        function filterSaleView(needle) {
            var rows = saleView ? saleView.querySelectorAll(".yard-alloc-row") : [];
            var fill = fillFilter && fillFilter.value ? fillFilter.value : "all";
            var shown = 0;
            Array.prototype.forEach.call(rows, function (row) {
                var haystack = (row.getAttribute("data-sale-search") || "").toLowerCase();
                var state = row.getAttribute("data-fill") || "";
                var match = (!needle || haystack.indexOf(needle) >= 0)
                    && (fill === "all" || fill === state || (fill === "available" && (state === "available" || state === "low")));
                row.hidden = !match;
                if (match) {
                    shown++;
                }
            });
            var empty = saleView ? saleView.querySelector(".yard-hub-empty.is-filter") : null;
            if (empty) {
                empty.hidden = shown > 0 || (!needle && fill === "all");
            }
        }

        function filterVehicleView(needle) {
            var rows = vehicleView ? vehicleView.querySelectorAll(".yard-alloc-row") : [];
            var saleCode = vehicleFilter && vehicleFilter.value ? vehicleFilter.value : "all";
            var shown = 0;
            Array.prototype.forEach.call(rows, function (row) {
                var haystack = (row.getAttribute("data-sale-search") || "").toLowerCase();
                var code = (row.getAttribute("data-sale-code") || "").toLowerCase();
                var match = !needle || haystack.indexOf(needle) >= 0;
                if (saleCode !== "all") {
                    match = match && code === saleCode.toLowerCase();
                }
                row.hidden = !match;
                if (match) {
                    shown++;
                }
            });
            var filterEmpty = vehicleView ? vehicleView.querySelector(".yard-hub-empty.is-filter") : null;
            if (filterEmpty) {
                filterEmpty.hidden = shown > 0 || (!needle && saleCode === "all");
            }
        }

        Array.prototype.forEach.call(buttons, function (button) {
            button.addEventListener("click", function () {
                setView(button.getAttribute("data-view"), true);
            });
        });
        if (search) {
            search.addEventListener("input", applySearch);
        }
        if (fillFilter) {
            fillFilter.addEventListener("change", applySearch);
        }
        if (vehicleFilter) {
            vehicleFilter.addEventListener("change", applySearch);
        }
        setView(currentView(), false);
    }

    bindHub();
})();
