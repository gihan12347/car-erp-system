(function () {
    var list = document.getElementById("workshopJobList");
    if (!list) {
        return;
    }

    function cards() {
        return list.querySelectorAll(".workshop-job-card");
    }

    function reindex() {
        Array.prototype.forEach.call(cards(), function (card, index) {
            card.setAttribute("data-index", String(index));
            var num = card.querySelector(".workshop-job-num");
            if (num) {
                num.textContent = String(index + 1);
            }
            Array.prototype.forEach.call(card.querySelectorAll("input, select, textarea"), function (field) {
                if (!field.name) {
                    return;
                }
                field.name = field.name.replace(/lines\[\d+]/g, "lines[" + index + "]");
                if (field.id) {
                    field.id = field.id.replace(/lines\d+/g, "lines" + index);
                }
            });
        });
    }

    list.addEventListener("click", function (event) {
        var button = event.target.closest(".workshop-job-remove");
        if (!button) {
            return;
        }
        event.preventDefault();
        var card = button.closest(".workshop-job-card");
        if (card) {
            card.parentNode.removeChild(card);
            reindex();
        }
    });

    reindex();
})();
