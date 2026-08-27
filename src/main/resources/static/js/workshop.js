(function () {
    var list = document.getElementById("workshopJobList");
    var addBtn = document.getElementById("addWorkshopJob");
    var template = document.getElementById("workshopJobTemplate");
    if (!list || !addBtn || !template) {
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
            var removeBtn = card.querySelector(".workshop-job-remove");
            if (removeBtn) {
                removeBtn.disabled = cards().length <= 1;
            }
        });
    }

    function addJob() {
        var html = template.innerHTML
            .replace(/__idx__/g, String(cards().length))
            .replace(/__num__/g, String(cards().length + 1));
        var wrap = document.createElement("div");
        wrap.innerHTML = html.trim();
        list.appendChild(wrap.firstChild);
        reindex();
    }

    addBtn.addEventListener("click", function (event) {
        event.preventDefault();
        addJob();
    });

    list.addEventListener("click", function (event) {
        var button = event.target.closest(".workshop-job-remove");
        if (!button) {
            return;
        }
        event.preventDefault();
        if (cards().length <= 1) {
            return;
        }
        var card = button.closest(".workshop-job-card");
        if (card) {
            card.parentNode.removeChild(card);
            reindex();
        }
    });

    reindex();
})();
