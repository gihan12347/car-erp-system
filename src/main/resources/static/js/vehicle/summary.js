(function () {
    var back = document.getElementById("summaryBack");
    if (back) {
        back.addEventListener("click", function () {
            if (window.history.length > 1) {
                window.history.back();
                return;
            }
            window.location.href = "/import";
        });
    }

    var filters = document.querySelectorAll("[data-doc-filter]");
    var cards = document.querySelectorAll(".doc-card");
    var empty = document.getElementById("summaryDocEmpty");

    function applyFilter(status) {
        var visible = 0;
        for (var i = 0; i < cards.length; i++) {
            var card = cards[i];
            var show = status === "all" || card.getAttribute("data-status") === status;
            card.hidden = !show;
            if (show) {
                visible++;
            }
        }
        if (empty) {
            empty.hidden = visible !== 0;
        }
    }

    for (var f = 0; f < filters.length; f++) {
        filters[f].addEventListener("click", function () {
            for (var i = 0; i < filters.length; i++) {
                filters[i].classList.toggle("is-active", filters[i] === this);
            }
            applyFilter(this.getAttribute("data-doc-filter"));
        });
    }

    document.addEventListener("click", function (event) {
        var trigger = event.target.closest ? event.target.closest("[data-doc-url]") : null;
        if (!trigger || !window.DocumentViewer) {
            return;
        }
        event.preventDefault();
        var pdfFlag = trigger.getAttribute("data-doc-pdf");
        window.DocumentViewer.open({
            url: trigger.getAttribute("data-doc-url"),
            title: trigger.getAttribute("data-doc-title") || "Document",
            isPdf: pdfFlag === "true"
        });
    });
})();
