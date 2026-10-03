(function () {
    var chassisInput = document.getElementById("chassisNo");
    var chassisNotice = document.getElementById("chassisDuplicateNotice");
    if (!chassisInput || chassisInput.readOnly || !chassisNotice) {
        return;
    }
    var form = chassisInput.form;
    var chassisDuplicateActive = false;
    var chassisCheckTimer = null;
    var lastCheckedChassis = "";

    function escapeHtml(text) {
        var div = document.createElement("div");
        div.textContent = text == null ? "" : String(text);
        return div.innerHTML;
    }

    function clearChassisDuplicateNotice() {
        chassisDuplicateActive = false;
        lastCheckedChassis = "";
        chassisNotice.hidden = true;
        chassisNotice.innerHTML = "";
        chassisInput.setCustomValidity("");
    }

    function showChassisDuplicateNotice(result) {
        if (!result || !result.exists) {
            clearChassisDuplicateNotice();
            return;
        }
        chassisDuplicateActive = true;
        lastCheckedChassis = (result.chassisNo || "").trim();
        var msg = result.message || "This chassis number is already in the import pipeline.";
        var url = result.pipelineUrl || "#";
        chassisNotice.innerHTML =
            "<strong>Chassis already registered</strong>" +
            "<p>" + escapeHtml(msg) + "</p>" +
            "<p class=\"chassis-duplicate-actions\">" +
            "<a class=\"panel-btn\" href=\"" + escapeHtml(url) + "\">Open existing import pipeline</a>" +
            "<span class=\"chassis-duplicate-hint\">Do not save again — continue the existing record instead.</span>" +
            "</p>";
        chassisNotice.hidden = false;
        chassisInput.setCustomValidity("This chassis number is already in the import pipeline.");
    }

    function checkChassisDuplicate() {
        var value = (chassisInput.value || "").trim();
        if (!value) {
            clearChassisDuplicateNotice();
            return;
        }
        if (value === lastCheckedChassis && chassisDuplicateActive) {
            return;
        }
        fetch("/auction/check-chassis?chassisNo=" + encodeURIComponent(value))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("check failed");
                }
                return response.json();
            })
            .then(function (result) {
                if ((chassisInput.value || "").trim() !== value) {
                    return;
                }
                if (result.exists) {
                    showChassisDuplicateNotice(result);
                } else {
                    clearChassisDuplicateNotice();
                }
            })
            .catch(function () {
            });
    }

    function scheduleChassisCheck() {
        if (chassisCheckTimer) {
            clearTimeout(chassisCheckTimer);
        }
        chassisCheckTimer = setTimeout(checkChassisDuplicate, 350);
    }

    chassisInput.addEventListener("blur", checkChassisDuplicate);
    chassisInput.addEventListener("input", function () {
        if (chassisDuplicateActive && (chassisInput.value || "").trim() !== lastCheckedChassis) {
            clearChassisDuplicateNotice();
        }
        scheduleChassisCheck();
    });

    if (form) {
        form.addEventListener("submit", function (e) {
            if (chassisDuplicateActive) {
                e.preventDefault();
                chassisNotice.scrollIntoView({ behavior: "smooth", block: "nearest" });
                chassisInput.reportValidity();
                chassisInput.focus();
            }
        });
    }
})();
