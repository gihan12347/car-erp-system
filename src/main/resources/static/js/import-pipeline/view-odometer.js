(function () {
    var root = document.querySelector(".import-view-stage.clearance-form");
    if (!root) {
        return;
    }

    function switchTab(tab) {
        var tabs = root.querySelectorAll(".clearance-tab");
        var panels = root.querySelectorAll(".clearance-tab-panel");
        Array.prototype.forEach.call(tabs, function (btn) {
            btn.classList.toggle("is-active", btn.getAttribute("data-tab") === String(tab));
        });
        Array.prototype.forEach.call(panels, function (panel) {
            var isActive = panel.getAttribute("data-tab-panel") === String(tab);
            panel.classList.toggle("is-active", isActive);
            panel.hidden = !isActive;
        });
        window.scrollTo({ top: 0, behavior: "smooth" });
        if (window.DocumentViewer) {
            DocumentViewer.refresh();
        }
    }

    Array.prototype.forEach.call(root.querySelectorAll(".clearance-tab"), function (btn) {
        btn.addEventListener("click", function () {
            var tab = parseInt(btn.getAttribute("data-tab"), 10);
            if (tab === 1 || tab === 2) {
                switchTab(tab);
            }
        });
    });

    var nextBtn = document.getElementById("clearanceViewNextBtn");
    if (nextBtn) {
        nextBtn.addEventListener("click", function () {
            switchTab(2);
        });
    }

    var prevBtn = document.getElementById("clearanceViewPrevBtn");
    if (prevBtn) {
        prevBtn.addEventListener("click", function () {
            switchTab(1);
        });
    }
})();
