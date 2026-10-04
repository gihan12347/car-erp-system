(function () {
    if (!window.L) {
        return;
    }

    var nodes = document.querySelectorAll(".place-map");
    Array.prototype.forEach.call(nodes, function (node) {
        var form = node.closest("form");
        if (!form) {
            return;
        }
        var latInput = form.querySelector("input[name='latitude']");
        var lngInput = form.querySelector("input[name='longitude']");
        var lat = parseFloat(node.getAttribute("data-lat"));
        var lng = parseFloat(node.getAttribute("data-lng"));
        var hasPoint = isFinite(lat) && isFinite(lng);
        var map = L.map(node, { scrollWheelZoom: false }).setView(
            hasPoint ? [lat, lng] : [6.9271, 79.8612],
            hasPoint ? 15 : 12
        );
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19,
            attribution: "&copy; OpenStreetMap"
        }).addTo(map);

        var marker = null;

        function write(nextLat, nextLng) {
            if (latInput) {
                latInput.value = Number(nextLat).toFixed(6);
                latInput.dispatchEvent(new Event("input", { bubbles: true }));
            }
            if (lngInput) {
                lngInput.value = Number(nextLng).toFixed(6);
                lngInput.dispatchEvent(new Event("input", { bubbles: true }));
            }
        }

        function placeMarker(nextLat, nextLng) {
            if (!marker) {
                marker = L.marker([nextLat, nextLng], { draggable: true }).addTo(map);
                marker.on("dragend", function () {
                    var point = marker.getLatLng();
                    write(point.lat, point.lng);
                });
            } else {
                marker.setLatLng([nextLat, nextLng]);
            }
        }

        if (hasPoint) {
            placeMarker(lat, lng);
        }

        map.on("click", function (event) {
            placeMarker(event.latlng.lat, event.latlng.lng);
            write(event.latlng.lat, event.latlng.lng);
        });

        var clear = form.querySelector(".place-map-clear");
        if (clear) {
            clear.addEventListener("click", function () {
                if (marker) {
                    map.removeLayer(marker);
                    marker = null;
                }
                if (latInput) {
                    latInput.value = "";
                    latInput.dispatchEvent(new Event("input", { bubbles: true }));
                }
                if (lngInput) {
                    lngInput.value = "";
                    lngInput.dispatchEvent(new Event("input", { bubbles: true }));
                }
            });
        }

        node._placeMap = map;
        window.setTimeout(function () {
            map.invalidateSize();
        }, 80);
    });
})();
