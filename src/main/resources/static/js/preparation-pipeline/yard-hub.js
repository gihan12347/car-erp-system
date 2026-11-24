(function () {
    function encodePath(value) {
        return encodeURIComponent(value || "").replace(/%2F/g, "/");
    }

    function openModal(modal) {
        if (!modal) {
            return;
        }
        modal.hidden = false;
        document.body.classList.add("modal-open");
    }

    function closeModal(modal) {
        if (!modal) {
            return;
        }
        modal.hidden = true;
        if (!document.querySelector(".add-vehicle-modal:not([hidden])")) {
            document.body.classList.remove("modal-open");
        }
    }

    function remainingOf(select) {
        if (!select || !select.options || select.selectedIndex < 0) {
            return 0;
        }
        var option = select.options[select.selectedIndex];
        if (!option) {
            return 0;
        }
        return parseInt(option.getAttribute("data-remaining") || "0", 10) || 0;
    }

    function bindModalDismiss(root) {
        if (!root) {
            return;
        }
        Array.prototype.forEach.call(root.querySelectorAll("[data-close-modal]"), function (el) {
            el.addEventListener("click", function () {
                closeModal(root);
            });
        });
    }

    function bindHub() {
        var root = document.getElementById("yardHubRoot");
        if (!root) {
            return;
        }
        var buttons = root.querySelectorAll(".view-switch-btn");
        var search = document.getElementById("yardHubSearch");
        var yardView = document.getElementById("yardView");
        var vehicleView = document.getElementById("vehicleView");
        var fillFilter = document.getElementById("yardFillFilter");
        var vehicleFilter = document.getElementById("vehicleYardFilter");
        var yardFilterWrap = document.getElementById("yardFilterWrap");
        var vehicleFilterWrap = document.getElementById("vehicleFilterWrap");
        var assignModal = document.getElementById("yardAssignModal");
        var transferModal = document.getElementById("yardTransferModal");
        var assignForm = document.getElementById("hubAssignForm");
        var assignYard = document.getElementById("hubAssignYard");
        var assignChassis = document.getElementById("hubAssignChassis");
        var assignRemaining = document.getElementById("hubAssignRemaining");
        var transferForm = document.getElementById("hubTransferForm");
        var transferYard = document.getElementById("hubTransferYard");
        var transferFrom = document.getElementById("hubTransferFrom");
        var transferRemaining = document.getElementById("hubTransferRemaining");

        function currentView() {
            return root.getAttribute("data-view") === "vehicles" ? "vehicles" : "yards";
        }

        function setView(view, persist) {
            var next = view === "vehicles" ? "vehicles" : "yards";
            root.setAttribute("data-view", next);
            Array.prototype.forEach.call(buttons, function (button) {
                var active = button.getAttribute("data-view") === next;
                button.classList.toggle("is-active", active);
                button.setAttribute("aria-selected", active ? "true" : "false");
            });
            if (yardView) {
                yardView.classList.toggle("is-hidden", next === "vehicles");
            }
            if (vehicleView) {
                vehicleView.classList.toggle("is-hidden", next !== "vehicles");
            }
            if (yardFilterWrap) {
                yardFilterWrap.hidden = next === "vehicles";
            }
            if (vehicleFilterWrap) {
                vehicleFilterWrap.hidden = next !== "vehicles";
            }
            if (persist && window.history && window.history.replaceState) {
                var url = new URL(window.location.href);
                if (next === "yards") {
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
                filterYardView(needle);
            }
        }

        function filterYardView(needle) {
            var rows = yardView ? yardView.querySelectorAll(".yard-alloc-row") : [];
            var fill = fillFilter && fillFilter.value ? fillFilter.value : "all";
            var shown = 0;
            Array.prototype.forEach.call(rows, function (row) {
                var haystack = (row.getAttribute("data-yard-search") || "").toLowerCase();
                var state = row.getAttribute("data-fill") || "";
                var match = (!needle || haystack.indexOf(needle) >= 0)
                    && (fill === "all" || fill === state || (fill === "available" && (state === "available" || state === "low")));
                row.hidden = !match;
                if (match) {
                    shown++;
                }
            });
            var empty = yardView ? yardView.querySelector(".yard-hub-empty.is-filter") : null;
            if (empty) {
                empty.hidden = shown > 0 || (!needle && fill === "all");
            }
        }

        function filterVehicleView(needle) {
            var rows = vehicleView ? vehicleView.querySelectorAll(".yard-alloc-row") : [];
            var yardCode = vehicleFilter && vehicleFilter.value ? vehicleFilter.value : "all";
            var shown = 0;
            Array.prototype.forEach.call(rows, function (row) {
                var haystack = (row.getAttribute("data-yard-search") || "").toLowerCase();
                var code = (row.getAttribute("data-yard-code") || "").toLowerCase();
                var match = !needle || haystack.indexOf(needle) >= 0;
                if (yardCode !== "all") {
                    match = match && code === yardCode.toLowerCase();
                }
                row.hidden = !match;
                if (match) {
                    shown++;
                }
            });
            var filterEmpty = vehicleView ? vehicleView.querySelector(".yard-hub-empty.is-filter") : null;
            if (filterEmpty) {
                filterEmpty.hidden = shown > 0 || (!needle && yardCode === "all");
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

        Array.prototype.forEach.call(root.querySelectorAll("[data-open-assign]"), function (button) {
            button.addEventListener("click", function () {
                if (assignChassis) {
                    assignChassis.value = button.getAttribute("data-chassis") || "";
                }
                if (assignYard) {
                    assignYard.selectedIndex = 0;
                }
                if (assignRemaining) {
                    assignRemaining.textContent = "—";
                }
                openModal(assignModal);
            });
        });
        Array.prototype.forEach.call(root.querySelectorAll("[data-open-transfer]"), function (button) {
            button.addEventListener("click", function () {
                var from = button.getAttribute("data-from") || "";
                var chassis = button.getAttribute("data-chassis") || "";
                if (transferFrom) {
                    transferFrom.textContent = button.getAttribute("data-from-title") || from;
                }
                if (transferForm) {
                    transferForm.setAttribute("action", "/yards/" + encodePath(from) + "/" + encodePath(chassis) + "/transfer");
                }
                if (transferYard) {
                    Array.prototype.forEach.call(transferYard.options, function (option) {
                        option.hidden = option.value && option.value.toLowerCase() === from.toLowerCase();
                    });
                    transferYard.selectedIndex = 0;
                }
                if (transferRemaining) {
                    transferRemaining.textContent = "—";
                }
                openModal(transferModal);
            });
        });
        if (assignYard && assignRemaining) {
            assignYard.addEventListener("change", function () {
                assignRemaining.textContent = String(remainingOf(assignYard));
            });
        }
        if (transferYard && transferRemaining) {
            transferYard.addEventListener("change", function () {
                transferRemaining.textContent = String(remainingOf(transferYard));
            });
        }
        if (assignForm) {
            assignForm.addEventListener("submit", function (event) {
                var yardCode = assignYard ? assignYard.value : "";
                if (!yardCode || remainingOf(assignYard) < 1) {
                    event.preventDefault();
                    alert("Choose a yard with available space.");
                    return;
                }
                assignForm.setAttribute("action", "/yards/" + encodePath(yardCode) + "/assign");
            });
        }
        if (transferForm) {
            transferForm.addEventListener("submit", function (event) {
                if (!transferYard || !transferYard.value) {
                    event.preventDefault();
                    alert("Choose a destination yard.");
                    return;
                }
                if (remainingOf(transferYard) < 1) {
                    event.preventDefault();
                    alert("The destination yard is full.");
                    return;
                }
                if (!confirm("Transfer this vehicle to the selected yard?")) {
                    event.preventDefault();
                }
            });
        }
        bindModalDismiss(assignModal);
        bindModalDismiss(transferModal);
        setView(currentView(), false);
    }

    function bindManage() {
        var root = document.getElementById("yardManageRoot");
        if (!root) {
            return;
        }
        var remaining = parseInt(root.getAttribute("data-remaining") || "0", 10) || 0;
        var addModal = document.getElementById("yardAddModal");
        var addForm = document.getElementById("yardAddForm");
        var addSearch = document.getElementById("yardAddSearch");
        var addCount = document.getElementById("yardAddCount");
        var addError = document.getElementById("yardAddError");
        var addSubmit = document.getElementById("yardAddSubmit");
        var openAdd = document.getElementById("openAddVehicles");
        var transferModal = document.getElementById("yardTransferModal");
        var transferForm = document.getElementById("hubTransferForm");
        var transferYard = document.getElementById("hubTransferYard");
        var transferFrom = document.getElementById("hubTransferFrom");
        var transferRemaining = document.getElementById("hubTransferRemaining");

        function selectedCount() {
            return addForm ? addForm.querySelectorAll(".yard-add-check:checked").length : 0;
        }

        function syncAdd() {
            var count = selectedCount();
            if (addCount) {
                addCount.textContent = String(count);
            }
            var over = count > remaining;
            if (addError) {
                addError.hidden = !over;
            }
            if (addSubmit) {
                addSubmit.disabled = count === 0 || over;
            }
        }

        if (openAdd) {
            openAdd.addEventListener("click", function () {
                openModal(addModal);
            });
        }
        if (addSearch) {
            addSearch.addEventListener("input", function () {
                var needle = addSearch.value.trim().toLowerCase();
                Array.prototype.forEach.call(document.querySelectorAll(".yard-add-row"), function (row) {
                    var haystack = (row.getAttribute("data-search") || "").toLowerCase();
                    row.hidden = needle && haystack.indexOf(needle) < 0;
                });
            });
        }
        if (addForm) {
            addForm.addEventListener("change", syncAdd);
            addForm.addEventListener("submit", function (event) {
                var count = selectedCount();
                if (count === 0 || count > remaining) {
                    event.preventDefault();
                    syncAdd();
                }
            });
        }
        Array.prototype.forEach.call(root.querySelectorAll("[data-open-transfer]"), function (button) {
            button.addEventListener("click", function () {
                var from = button.getAttribute("data-from") || "";
                var chassis = button.getAttribute("data-chassis") || "";
                if (transferFrom) {
                    transferFrom.textContent = button.getAttribute("data-from-title") || from;
                }
                if (transferForm) {
                    transferForm.setAttribute("action", "/yards/" + encodePath(from) + "/" + encodePath(chassis) + "/transfer");
                }
                if (transferYard) {
                    transferYard.selectedIndex = 0;
                }
                if (transferRemaining) {
                    transferRemaining.textContent = "—";
                }
                openModal(transferModal);
            });
        });
        if (transferYard && transferRemaining) {
            transferYard.addEventListener("change", function () {
                transferRemaining.textContent = String(remainingOf(transferYard));
            });
        }
        if (transferForm) {
            transferForm.addEventListener("submit", function (event) {
                if (!transferYard || !transferYard.value) {
                    event.preventDefault();
                    alert("Choose a destination yard.");
                    return;
                }
                if (remainingOf(transferYard) < 1) {
                    event.preventDefault();
                    alert("The destination yard is full.");
                    return;
                }
                if (!confirm("Transfer this vehicle to the selected yard?")) {
                    event.preventDefault();
                }
            });
        }
        bindModalDismiss(addModal);
        bindModalDismiss(transferModal);
        syncAdd();
    }

    bindHub();
    bindManage();
})();
