(function (window) {
    function remainingFor(select, selected) {
        var remaining = parseInt(selected.getAttribute("data-remaining"), 10);
        var current = (select.getAttribute("data-current") || "").toUpperCase();
        if (selected.value && selected.value.toUpperCase() === current) {
            return isNaN(remaining) ? 0 : remaining;
        }
        return isNaN(remaining) ? 0 : remaining;
    }

    function ensureOption(select, value) {
        if (!value) {
            return;
        }
        var found = false;
        Array.prototype.forEach.call(select.options, function (opt) {
            if (opt.value === value) {
                found = true;
            }
        });
        if (!found) {
            var opt = document.createElement("option");
            opt.value = value;
            opt.textContent = value;
            select.appendChild(opt);
        }
        select.value = value;
    }

    window.bindCapacitySelect = function (ids) {
        var select = document.getElementById(ids.selectId);
        var locationDisplay = document.getElementById(ids.displayId);
        var sectionValue = document.getElementById(ids.valueId);
        var hint = document.getElementById(ids.hintId);
        if (!select) {
            return;
        }
        function sync() {
            var selected = select.options[select.selectedIndex];
            if (!selected || !selected.value) {
                if (locationDisplay) {
                    locationDisplay.value = "";
                }
                if (sectionValue) {
                    sectionValue.value = "";
                }
                if (hint) {
                    hint.hidden = true;
                    hint.textContent = "";
                }
                return;
            }
            var location = selected.getAttribute("data-location") || "";
            if (locationDisplay) {
                locationDisplay.value = location;
            }
            if (sectionValue) {
                sectionValue.value = location;
            }
            if (!hint) {
                return;
            }
            var remaining = remainingFor(select, selected);
            var capacity = selected.getAttribute("data-capacity") || "";
            var occupied = selected.getAttribute("data-occupied") || "";
            var name = selected.getAttribute("data-name") || selected.value;
            var afterSelect = remaining;
            var current = (select.getAttribute("data-current") || "").toUpperCase();
            if (!selected.value || selected.value.toUpperCase() !== current) {
                afterSelect = Math.max(0, remaining - 1);
            }
            hint.hidden = false;
            hint.textContent = afterSelect + " space" + (afterSelect === 1 ? "" : "s") + " left at "
                    + name + " after this vehicle (" + occupied + "/" + capacity + " in use now).";
        }
        ensureOption(select, select.getAttribute("data-current") || select.value);
        select.addEventListener("change", sync);
        sync();
    };
})(window);
