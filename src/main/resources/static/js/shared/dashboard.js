(function () {
    var node = document.getElementById("dash-charts");
    var data = {};
    if (node) {
        try {
            data = JSON.parse(node.textContent || "{}");
        } catch (error) {
            data = {};
        }
    }

    var palette = ["#0f766e", "#b45309", "#c2410c", "#047857", "#9a3412", "#78716c"];
    var ink = "#1c1917";
    var muted = "#78716c";

    if (window.Chart) {
        Chart.defaults.font.family = "Poppins, sans-serif";
        Chart.defaults.color = muted;
        var stages = data.stages || {};
        var canvas = document.getElementById("chart-status");
        if (canvas) {
            var values = stages.values || [];
            var total = values.reduce(function (sum, value) { return sum + Number(value || 0); }, 0);
            var labels = (stages.labels || []).map(function (label, index) {
                var count = Number(values[index] || 0);
                var share = total <= 0 ? 0 : Math.round(count * 100 / total);
                return label + "  " + share + "%";
            });
            new Chart(canvas, {
                type: "doughnut",
                data: {
                    labels: labels,
                    datasets: [{
                        data: values,
                        backgroundColor: palette,
                        borderColor: "#fffdf8",
                        borderWidth: 3,
                        hoverOffset: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: "68%",
                    plugins: {
                        legend: {
                            position: "right",
                            labels: {
                                boxWidth: 10,
                                padding: 10,
                                color: ink,
                                font: { family: "Poppins, sans-serif", size: 11 }
                            }
                        }
                    }
                }
            });
        }
    }

    var mapNode = document.getElementById("fleetMap");
    if (!mapNode || !window.L) {
        return;
    }

    var map = L.map(mapNode, { scrollWheelZoom: false }).setView([6.9271, 79.8612], 12);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap"
    }).addTo(map);

    var routes = [
        {
            name: "Sample import run",
            color: "#38bdf8",
            path: [[6.942, 79.842], [6.935, 79.855], [6.927, 79.872], [6.918, 79.890]]
        },
        {
            name: "Sample yard transfer",
            color: "#fb923c",
            path: [[6.910, 79.860], [6.916, 79.875], [6.928, 79.888], [6.940, 79.900]]
        },
        {
            name: "Sample sale loop",
            color: "#4ade80",
            path: [[6.905, 79.855], [6.912, 79.848], [6.922, 79.852], [6.918, 79.866], [6.905, 79.855]]
        }
    ];

    var bounds = [];
    routes.forEach(function (route) {
        L.polyline(route.path, { color: route.color, weight: 3, opacity: 0.85 }).addTo(map);
        route.path.forEach(function (point) { bounds.push(point); });
        var marker = L.circleMarker(route.path[0], {
            radius: 7,
            color: "#fff",
            weight: 2,
            fillColor: route.color,
            fillOpacity: 1
        }).addTo(map);
        marker.bindTooltip(route.name, { permanent: false });
        var step = 0;
        window.setInterval(function () {
            step = (step + 1) % route.path.length;
            marker.setLatLng(route.path[step]);
        }, 1600);
    });

    var saleIcon = L.divIcon({
        className: "fleet-pin-icon is-sale",
        html: "<span></span>",
        iconSize: [18, 18],
        iconAnchor: [9, 9]
    });
    var yardIcon = L.divIcon({
        className: "fleet-pin-icon is-yard",
        html: "<span></span>",
        iconSize: [18, 18],
        iconAnchor: [9, 9]
    });

    (data.places || []).forEach(function (place) {
        if (place.lat == null || place.lng == null) {
            return;
        }
        var point = [Number(place.lat), Number(place.lng)];
        bounds.push(point);
        var kind = place.kind === "yard" ? "Yard" : "Sale";
        L.marker(point, { icon: place.kind === "yard" ? yardIcon : saleIcon })
            .addTo(map)
            .bindPopup(
                "<strong>" + kind + " · " + place.name + "</strong><br>"
                + (place.detail || place.code || "")
                + "<br>" + (place.occupied || 0) + " / " + (place.capacity || 0) + " occupied"
            );
    });

    if (bounds.length > 0) {
        map.fitBounds(bounds, { padding: [24, 24], maxZoom: 13 });
    }

    var latInput = document.getElementById("pinLat");
    var lngInput = document.getElementById("pinLng");
    var draft = null;
    map.on("click", function (event) {
        var lat = event.latlng.lat.toFixed(6);
        var lng = event.latlng.lng.toFixed(6);
        if (latInput) {
            latInput.value = lat;
        }
        if (lngInput) {
            lngInput.value = lng;
        }
        if (draft) {
            draft.setLatLng(event.latlng);
        } else {
            draft = L.marker(event.latlng, { draggable: true }).addTo(map);
            draft.on("dragend", function () {
                var point = draft.getLatLng();
                if (latInput) {
                    latInput.value = point.lat.toFixed(6);
                }
                if (lngInput) {
                    lngInput.value = point.lng.toFixed(6);
                }
            });
        }
    });

    var kindSelect = document.getElementById("pinKind");
    var idSelect = document.getElementById("pinId");
    function syncPlaces() {
        if (!kindSelect || !idSelect) {
            return;
        }
        var kind = kindSelect.value;
        var first = null;
        Array.prototype.forEach.call(idSelect.options, function (option) {
            var show = option.getAttribute("data-kind") === kind;
            option.hidden = !show;
            option.disabled = !show;
            if (show && !first) {
                first = option;
            }
        });
        if (idSelect.selectedOptions.length && idSelect.selectedOptions[0].disabled && first) {
            idSelect.value = first.value;
        }
    }
    if (kindSelect) {
        kindSelect.addEventListener("change", syncPlaces);
        syncPlaces();
    }

    window.setTimeout(function () { map.invalidateSize(); }, 200);
})();
