package com.carsale.erp.shared.share;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PublicVehicleView {

    private final String title;
    private final String price;
    private final String listPrice;
    private final String stockNo;
    private final String description;
    private final String registrationNo;
    private final String condition;
    private final List<Spec> specs;
    private final List<Feature> features;
    private final List<Photo> photos;
    private final String locationName;
    private final String locationAddress;
    private final Double latitude;
    private final Double longitude;

    public PublicVehicleView(
            String title,
            String price,
            String listPrice,
            String stockNo,
            String description,
            String registrationNo,
            String condition,
            List<Spec> specs,
            List<Feature> features,
            List<Photo> photos,
            String locationName,
            String locationAddress,
            Double latitude,
            Double longitude
    ) {
        this.title = title;
        this.price = price;
        this.listPrice = listPrice;
        this.stockNo = stockNo;
        this.description = description;
        this.registrationNo = registrationNo;
        this.condition = condition;
        this.specs = specs == null ? Collections.<Spec>emptyList() : specs;
        this.features = features == null ? Collections.<Feature>emptyList() : features;
        this.photos = photos == null ? Collections.<Photo>emptyList() : photos;
        this.locationName = locationName;
        this.locationAddress = locationAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getTitle() {
        return title;
    }

    public String getPrice() {
        return price;
    }

    public String getListPrice() {
        return listPrice;
    }

    public boolean isOffer() {
        return listPrice != null;
    }

    public String getStockNo() {
        return stockNo;
    }

    public String getDescription() {
        return description;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public String getCondition() {
        return condition;
    }

    public List<Spec> getSpecs() {
        return specs;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public List<String> getFeatureGroups() {
        List<String> groups = new ArrayList<String>();
        for (Feature feature : features) {
            if (!groups.contains(feature.getGroup())) {
                groups.add(feature.getGroup());
            }
        }
        return groups;
    }

    public List<Feature> featuresIn(String group) {
        List<Feature> rows = new ArrayList<Feature>();
        for (Feature feature : features) {
            if (feature.getGroup().equals(group)) {
                rows.add(feature);
            }
        }
        return rows;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public boolean isMapped() {
        return latitude != null && longitude != null;
    }

    public boolean isHasPhotos() {
        return !photos.isEmpty();
    }

    public boolean isLongDescription() {
        return description != null && description.length() > 220;
    }

    public String getDirectionsUrl() {
        if (isMapped()) {
            return "https://www.google.com/maps/dir/?api=1&destination=" + latitude + "," + longitude;
        }
        if (locationAddress == null || locationAddress.trim().isEmpty()) {
            return null;
        }
        try {
            return "https://www.google.com/maps/search/?api=1&query="
                    + URLEncoder.encode(locationAddress.trim(), "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    public static final class Spec {
        private final String label;
        private final String value;
        private final String icon;
        private final boolean highlight;

        public Spec(String label, String value, String icon, boolean highlight) {
            this.label = label;
            this.value = value;
            this.icon = icon;
            this.highlight = highlight;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public String getIcon() {
            return icon;
        }

        public boolean isHighlight() {
            return highlight;
        }
    }

    public static final class Feature {
        private final String label;
        private final String icon;
        private final String group;

        public Feature(String label, String icon, String group) {
            this.label = label;
            this.icon = icon;
            this.group = group;
        }

        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }

        public String getGroup() {
            return group;
        }
    }

    public static final class Photo {
        private final String url;
        private final String name;

        public Photo(String url, String name) {
            this.url = url;
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public String getName() {
            return name;
        }
    }
}
