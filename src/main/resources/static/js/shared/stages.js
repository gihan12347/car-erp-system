(function () {
    var statusEl = document.getElementById("stageOrderStatus");
    var errorEl = document.getElementById("stageOrderError");

    function csrfHeaders(json) {
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        var headers = {};
        if (json) {
            headers["Content-Type"] = "application/json";
        }
        if (token && header) {
            headers[header.content] = token.content;
        }
        return headers;
    }

    function showMessage(ok, text) {
        if (statusEl) {
            statusEl.hidden = !ok;
            statusEl.textContent = ok ? text : "";
        }
        if (errorEl) {
            errorEl.hidden = ok;
            errorEl.textContent = ok ? "" : text;
        }
    }

    function refreshIndexes(list) {
        var items = list.querySelectorAll(".stage-order-item");
        Array.prototype.forEach.call(items, function (item, index) {
            var badge = item.querySelector(".stage-order-index");
            if (badge) {
                badge.textContent = String(index + 1);
            }
        });
    }

    function collectKeys(list) {
        var keys = [];
        var items = list.querySelectorAll(".stage-order-item");
        Array.prototype.forEach.call(items, function (item) {
            keys.push(item.getAttribute("data-stage-key"));
        });
        return keys;
    }

    function sameKeys(a, b) {
        if (!a || !b || a.length !== b.length) {
            return false;
        }
        for (var i = 0; i < a.length; i++) {
            if (a[i] !== b[i]) {
                return false;
            }
        }
        return true;
    }

    function parseError(response, fallback) {
        if (response.status === 403) {
            return Promise.resolve("Your session expired. Refresh the page and try again.");
        }
        return response.json().then(function (body) {
            return (body && body.message) || fallback;
        }).catch(function () {
            return fallback;
        });
    }

    function saveOrder(flowId, keys, onDone) {
        fetch("/stages/reorder", {
            method: "POST",
            credentials: "same-origin",
            headers: csrfHeaders(true),
            body: JSON.stringify({ flowId: Number(flowId), stageKeys: keys })
        })
            .then(function (response) {
                if (!response.ok) {
                    return parseError(response, "Could not save stage order.").then(function (msg) {
                        throw new Error(msg);
                    });
                }
                return response.json();
            })
            .then(function (result) {
                if (!result.success) {
                    throw new Error(result.message || "Could not save stage order.");
                }
                showMessage(true, result.message || "Stage order saved.");
                if (onDone) {
                    onDone(true);
                }
            })
            .catch(function (err) {
                showMessage(false, err.message || "Could not save stage order.");
                if (onDone) {
                    onDone(false);
                }
            });
    }

    function dragAfterElement(list, y) {
        var items = list.querySelectorAll(".stage-order-item:not(.is-dragging)");
        var closest = null;
        var closestOffset = Number.NEGATIVE_INFINITY;
        Array.prototype.forEach.call(items, function (item) {
            var box = item.getBoundingClientRect();
            var offset = y - box.top - box.height / 2;
            if (offset < 0 && offset > closestOffset) {
                closestOffset = offset;
                closest = item;
            }
        });
        return closest;
    }

    function bindList(list) {
        var items = list.querySelectorAll(".stage-order-item");
        if (items.length < 2) {
            Array.prototype.forEach.call(items, function (item) {
                item.setAttribute("draggable", "false");
            });
            return;
        }

        var dragging = null;
        var startKeys = collectKeys(list);

        Array.prototype.forEach.call(items, function (item) {
            item.addEventListener("dragstart", function (event) {
                dragging = item;
                startKeys = collectKeys(list);
                item.classList.add("is-dragging");
                if (event.dataTransfer) {
                    event.dataTransfer.effectAllowed = "move";
                    event.dataTransfer.setData("text/plain", item.getAttribute("data-stage-key"));
                }
            });
            item.addEventListener("dragend", function () {
                item.classList.remove("is-dragging");
                dragging = null;
                refreshIndexes(list);
                var nextKeys = collectKeys(list);
                if (sameKeys(startKeys, nextKeys)) {
                    return;
                }
                saveOrder(list.getAttribute("data-flow-id"), nextKeys, function (ok) {
                    if (!ok) {
                        window.location.reload();
                    } else {
                        startKeys = nextKeys;
                    }
                });
            });
        });

        list.addEventListener("dragover", function (event) {
            event.preventDefault();
            if (!dragging) {
                return;
            }
            var after = dragAfterElement(list, event.clientY);
            if (after == null) {
                list.appendChild(dragging);
            } else {
                list.insertBefore(dragging, after);
            }
        });

        list.addEventListener("drop", function (event) {
            event.preventDefault();
        });
    }

    function bindReset(button) {
        button.addEventListener("click", function () {
            var flowId = button.getAttribute("data-flow-id");
            if (!window.confirm("Restore the default stage order for this pipeline?")) {
                return;
            }
            fetch("/stages/reset?flowId=" + encodeURIComponent(flowId), {
                method: "POST",
                credentials: "same-origin",
                headers: csrfHeaders(false)
            })
                .then(function (response) {
                    if (!response.ok) {
                        return parseError(response, "Could not restore default order.").then(function (msg) {
                            throw new Error(msg);
                        });
                    }
                    return response.json();
                })
                .then(function (result) {
                    if (!result.success) {
                        throw new Error(result.message || "Could not restore default order.");
                    }
                    window.location.reload();
                })
                .catch(function (err) {
                    showMessage(false, err.message || "Could not restore default order.");
                });
        });
    }

    Array.prototype.forEach.call(document.querySelectorAll(".stage-order-list"), bindList);
    Array.prototype.forEach.call(document.querySelectorAll(".stage-reset-btn"), bindReset);
})();
