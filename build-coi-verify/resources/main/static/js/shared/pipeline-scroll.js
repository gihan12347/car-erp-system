(function () {
    function scrollNodeIntoScroller(scroller, node, behavior) {
        if (!scroller || !node) {
            return;
        }
        var max = scroller.scrollWidth - scroller.clientWidth;
        if (max <= 0) {
            return;
        }
        var scrollerBox = scroller.getBoundingClientRect();
        var nodeBox = node.getBoundingClientRect();
        var delta = (nodeBox.left + nodeBox.width / 2) - (scrollerBox.left + scrollerBox.width / 2);
        var nextLeft = Math.max(0, Math.min(max, scroller.scrollLeft + delta));
        if (typeof scroller.scrollTo === "function") {
            scroller.scrollTo({
                left: nextLeft,
                behavior: behavior || "auto"
            });
            return;
        }
        scroller.scrollLeft = nextLeft;
    }

    function scrollImportPipelineToCurrent(behavior) {
        var scroller = document.querySelector(".import-pipeline-steps");
        if (!scroller) {
            return;
        }
        var current = scroller.querySelector("li.is-current")
            || scroller.querySelector("li.is-workshop-next")
            || scroller.querySelector("li.is-pending");
        scrollNodeIntoScroller(scroller, current, behavior);
    }

    function scrollListPipelineFlows(behavior) {
        var flows = document.querySelectorAll("[data-pipeline-flow] .list-pipeline-flow");
        for (var i = 0; i < flows.length; i++) {
            var flow = flows[i];
            var current = flow.querySelector("li.is-current")
                || flow.querySelector("li.is-pending")
                || flow.querySelector("li.is-done:last-child");
            scrollNodeIntoScroller(flow, current, behavior);
        }
    }

    function runPipelineScroll(behavior) {
        scrollImportPipelineToCurrent(behavior);
        scrollListPipelineFlows(behavior);
    }

    window.scrollImportPipelineToCurrent = scrollImportPipelineToCurrent;
    window.scrollListPipelineFlows = scrollListPipelineFlows;

    function afterLayout(fn) {
        requestAnimationFrame(function () {
            requestAnimationFrame(fn);
        });
    }

    afterLayout(function () {
        runPipelineScroll("auto");
    });
    window.addEventListener("load", function () {
        afterLayout(function () {
            runPipelineScroll("auto");
        });
    });
})();
