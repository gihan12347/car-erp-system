package com.carsale.erp.shared.share;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.carsale.erp.importpipeline.model.EquipmentInspection;
import com.carsale.erp.importpipeline.model.VehiclePhoto;
import com.carsale.erp.importpipeline.repository.EquipmentInspectionRepository;
import com.carsale.erp.importpipeline.repository.VehiclePhotoRepository;
import com.carsale.erp.importpipeline.util.EquipmentInspectionFields;
import com.carsale.erp.importpipeline.util.EquipmentInspectionFields.FieldDef;
import com.carsale.erp.readypipeline.model.SaleListing;
import com.carsale.erp.readypipeline.model.SaleLocation;
import com.carsale.erp.readypipeline.model.VehicleRegistration;
import com.carsale.erp.readypipeline.repository.SaleListingRepository;
import com.carsale.erp.readypipeline.repository.VehicleRegistrationRepository;
import com.carsale.erp.readypipeline.service.SaleLocationService;
import com.carsale.erp.report.ReportService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import com.carsale.erp.shared.vehicle.VehicleStage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class VehicleShareService {

    static final String NOT_READY = "This vehicle must reach Sales Ready status before it can be shared.";

    private final VehicleShareLinkRepository shareLinkRepository;
    private final VehicleRepository vehicleRepository;
    private final SaleListingRepository saleListingRepository;
    private final VehiclePhotoRepository vehiclePhotoRepository;
    private final EquipmentInspectionRepository equipmentInspectionRepository;
    private final VehicleRegistrationRepository vehicleRegistrationRepository;
    private final SaleLocationService saleLocationService;
    private final ReportService reportService;

    public boolean isShareable(Vehicle vehicle) {
        if (vehicle == null || blank(vehicle.getChassisNo())) {
            return false;
        }
        SaleListing listing = saleListingRepository.findById(vehicle.getChassisNo().trim()).orElse(null);
        if (listing != null && listing.isSold()) {
            return false;
        }
        if (vehicle.getStage() == VehicleStage.SOLD) {
            return false;
        }
        // The summary badge "Sale" is VehicleStage.READY. Pipelines can still be open.
        if (vehicle.getStage() == VehicleStage.READY) {
            return true;
        }
        String place = reportService.placeOf(vehicle, listing);
        return ReportService.READY.equals(place) || ReportService.LISTED.equals(place);
    }

    public String activeToken(String chassisNo) {
        if (blank(chassisNo)) {
            return null;
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        if (!isShareable(vehicle)) {
            return null;
        }
        return shareLinkRepository.findFirstByChassisNoAndActiveTrueOrderByIdDesc(chassisNo.trim())
                .map(VehicleShareLink::getPublicToken)
                .orElse(null);
    }

    @Transactional
    public String ensureLink(String chassisNo) {
        if (blank(chassisNo)) {
            throw new IllegalArgumentException("Vehicle not found.");
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim()).orElse(null);
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle not found.");
        }
        if (!isShareable(vehicle)) {
            throw new IllegalArgumentException(NOT_READY);
        }
        VehicleShareLink existing = shareLinkRepository
                .findFirstByChassisNoAndActiveTrueOrderByIdDesc(vehicle.getChassisNo())
                .orElse(null);
        if (existing != null) {
            return existing.getPublicToken();
        }
        VehicleShareLink created = new VehicleShareLink();
        created.setPublicToken(UUID.randomUUID().toString());
        created.setChassisNo(vehicle.getChassisNo());
        created.setActive(true);
        created.setCreatedAt(LocalDateTime.now());
        return shareLinkRepository.save(created).getPublicToken();
    }

    @Transactional(readOnly = true)
    public PublicVehicleView publicView(String token) {
        Vehicle vehicle = shareableVehicle(token);
        if (vehicle == null) {
            return null;
        }
        SaleListing listing = saleListingRepository.findById(vehicle.getChassisNo()).orElse(null);
        VehicleRegistration registration = vehicleRegistrationRepository.findById(vehicle.getChassisNo()).orElse(null);
        EquipmentInspection equipment = equipmentInspectionRepository.findById(vehicle.getChassisNo()).orElse(null);
        SaleLocation location = locationFor(listing);
        List<PublicVehicleView.Spec> specLines = specs(vehicle);
        String registrationNo = registration == null ? null : text(registration.getRegistrationNo());
        addSpec(specLines, "Registration", registrationNo, "fa-id-card", false);
        addSpec(specLines, "Condition", listing == null ? null : listing.getSaleCondition(), "fa-certificate", false);
        SharePrice sharePrice = sharePrice(listing);
        return new PublicVehicleView(
                title(vehicle),
                sharePrice.price,
                sharePrice.listPrice,
                text(vehicle.getStockNo()),
                text(vehicle.getSalesPoints()),
                registrationNo,
                listing == null ? null : text(listing.getSaleCondition()),
                specLines,
                features(equipment),
                photos(token, vehicle.getChassisNo()),
                locationName(location, listing),
                location == null ? null : text(location.getLocation()),
                location == null ? null : location.getLatitude(),
                location == null ? null : location.getLongitude()
        );
    }

    public boolean linkExists(String token) {
        return findActive(token) != null;
    }

    @Transactional(readOnly = true)
    public VehiclePhoto publicPhoto(String token, String storedName) {
        Vehicle vehicle = shareableVehicle(token);
        if (vehicle == null || blank(storedName)) {
            return null;
        }
        String name = storedName.trim();
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.indexOf("..") >= 0) {
            return null;
        }
        List<VehiclePhoto> photos = vehiclePhotoRepository.findByChassisNoOrderBySortOrderAscIdAsc(vehicle.getChassisNo());
        for (VehiclePhoto photo : photos) {
            if (photo.getStoredName() != null && photo.getStoredName().equals(name) && isImage(photo)) {
                return photo;
            }
        }
        return null;
    }

    public String publicUrl(HttpServletRequest request, String token) {
        if (request == null || blank(token)) {
            return "";
        }
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(request.getContextPath() + "/shared/vehicle/" + token.trim())
                .replaceQuery(null)
                .build()
                .toUriString();
    }

    private Vehicle shareableVehicle(String token) {
        VehicleShareLink link = findActive(token);
        if (link == null) {
            return null;
        }
        Vehicle vehicle = vehicleRepository.findById(link.getChassisNo()).orElse(null);
        return isShareable(vehicle) ? vehicle : null;
    }

    private VehicleShareLink findActive(String token) {
        if (blank(token)) {
            return null;
        }
        String value = token.trim();
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        VehicleShareLink link = shareLinkRepository.findByPublicToken(value).orElse(null);
        if (link == null || !link.isActive()) {
            return null;
        }
        return link;
    }

    private SaleLocation locationFor(SaleListing listing) {
        if (listing == null || blank(listing.getSaleCode())) {
            return null;
        }
        return saleLocationService.findByCode(listing.getSaleCode());
    }

    private List<PublicVehicleView.Spec> specs(Vehicle vehicle) {
        List<PublicVehicleView.Spec> specs = new ArrayList<PublicVehicleView.Spec>();
        addSpec(specs, "Year", vehicle.getYear(), "fa-calendar", true);
        addSpec(specs, "Mileage", vehicle.getMileage(), "fa-gauge-high", true);
        addSpec(specs, "Exterior colour", vehicle.getColor(), "fa-palette", false);
        addSpec(specs, "Engine capacity", vehicle.getEngineSize(), "fa-gear", false);
        addSpec(specs, "Fuel type", vehicle.getFuel(), "fa-gas-pump", true);
        addSpec(specs, "Transmission", vehicle.getTransmissionLabel(), "fa-sliders", true);
        addSpec(specs, "Grade", vehicle.getGrade(), "fa-award", true);
        addSpec(specs, "Body style", vehicle.getBodyStyle(), "fa-car-side", false);
        addSpec(specs, "Drive", vehicle.getDriveSystem(), "fa-road", false);
        addSpec(specs, "Doors", vehicle.getDoors(), "fa-door-open", false);
        addSpec(specs, "Seats", vehicle.getSeats(), "fa-chair", false);
        return specs;
    }

    private List<PublicVehicleView.Feature> features(EquipmentInspection equipment) {
        List<PublicVehicleView.Feature> features = new ArrayList<PublicVehicleView.Feature>();
        if (equipment == null) {
            return features;
        }
        BeanWrapper values = new BeanWrapperImpl(equipment);
        for (FieldDef field : EquipmentInspectionFields.all()) {
            Object raw = values.getPropertyValue(field.getKey());
            String feature = featureLabel(field.getLabel(), raw == null ? null : String.valueOf(raw));
            if (feature != null) {
                features.add(new PublicVehicleView.Feature(feature, featureIcon(field.getKey()), featureGroup(field)));
            }
        }
        return features;
    }

    private static String featureGroup(FieldDef field) {
        if (EquipmentInspectionFields.safety().contains(field)) {
            return "Safety";
        }
        if (EquipmentInspectionFields.exterior().contains(field)) {
            return "Exterior";
        }
        return "Comfort";
    }

    private static String featureIcon(String key) {
        if ("airConditioning".equals(key) || "airPureFilter".equals(key)) {
            return "fa-snowflake";
        }
        if ("powerSteering".equals(key) || "multifunctionSteeringWheel".equals(key)) {
            return "fa-dharmachakra";
        }
        if ("powerWindows".equals(key) || "sideSlidingWindows".equals(key)) {
            return "fa-window-maximize";
        }
        if ("absBrakingSystem".equals(key)) {
            return "fa-car-burst";
        }
        if ("driversAirbag".equals(key) || "passengerAirbag".equals(key)) {
            return "fa-shield-halved";
        }
        if ("alloyWheels".equals(key) || "wheelCoverHubCaps".equals(key)) {
            return "fa-circle-dot";
        }
        if ("navigation".equals(key)) {
            return "fa-location-dot";
        }
        if ("camera".equals(key)) {
            return "fa-camera";
        }
        if ("parkingSensor".equals(key)) {
            return "fa-satellite-dish";
        }
        if ("sunroof".equals(key) || "moonRoof".equals(key)) {
            return "fa-sun";
        }
        if ("leatherSeats".equals(key) || "seatPower".equals(key) || "revolvingSeat".equals(key)) {
            return "fa-chair";
        }
        if ("fogLights".equals(key) || "highMountStopLight".equals(key)) {
            return "fa-lightbulb";
        }
        if ("usbLink".equals(key) || "tv".equals(key) || "cd".equals(key) || "radioCassette".equals(key)) {
            return "fa-music";
        }
        return "fa-check";
    }

    static String featureLabel(String label, String raw) {
        String choice = EquipmentInspectionFields.canonicalChoice(raw);
        if ("YES".equals(choice) || "OK".equals(choice)) {
            return label;
        }
        return null;
    }

    private List<PublicVehicleView.Photo> photos(String token, String chassisNo) {
        List<PublicVehicleView.Photo> photos = new ArrayList<PublicVehicleView.Photo>();
        List<VehiclePhoto> stored = vehiclePhotoRepository.findByChassisNoOrderBySortOrderAscIdAsc(chassisNo);
        int index = 1;
        for (VehiclePhoto photo : stored) {
            if (!isImage(photo) || blank(photo.getStoredName())) {
                continue;
            }
            String name = text(photo.getOriginalName());
            if (name == null) {
                name = "Photo " + index;
            }
            photos.add(new PublicVehicleView.Photo(
                    "/shared/vehicle/" + token + "/photos/" + photo.getStoredName(),
                    name
            ));
            index++;
        }
        return photos;
    }

    private static boolean isImage(VehiclePhoto photo) {
        String type = photo.getContentType() == null ? "" : photo.getContentType().toLowerCase(Locale.ROOT);
        if (type.startsWith("image/")) {
            return true;
        }
        String name = photo.getStoredName() == null ? "" : photo.getStoredName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".webp") || name.endsWith(".gif");
    }

    static SharePrice sharePrice(SaleListing listing) {
        if (listing == null) {
            return new SharePrice(null, null);
        }
        String askingRaw = text(listing.getAskingPrice());
        if (askingRaw == null) {
            askingRaw = text(listing.getAdvertisedPrice());
        }
        BigDecimal asking = ReportService.parseMoney(askingRaw);
        BigDecimal actual = ReportService.parseMoney(text(listing.getActualPrice()));
        String shown = asking != null ? ReportService.formatMoney(asking) : askingRaw;
        if (shown == null) {
            return new SharePrice(null, null);
        }
        if (asking != null && actual != null && asking.compareTo(actual) < 0) {
            return new SharePrice(shown, ReportService.formatMoney(actual));
        }
        return new SharePrice(shown, null);
    }

    static final class SharePrice {
        final String price;
        final String listPrice;

        private SharePrice(String price, String listPrice) {
            this.price = price;
            this.listPrice = listPrice;
        }
    }

    private static String locationName(SaleLocation location, SaleListing listing) {
        if (location != null && text(location.getSaleName()) != null) {
            return location.getSaleName().trim();
        }
        if (listing != null) {
            return text(listing.getSaleLocation());
        }
        return null;
    }

    private static String title(Vehicle vehicle) {
        String model = text(vehicle.getModel());
        String make = text(vehicle.getMake());
        String name;
        if (model != null && make != null && model.toLowerCase(Locale.ROOT).indexOf(make.toLowerCase(Locale.ROOT)) < 0) {
            name = make + " " + model;
        } else if (model != null) {
            name = model;
        } else if (make != null) {
            name = make;
        } else {
            name = "Vehicle";
        }
        return name;
    }

    private static void addSpec(List<PublicVehicleView.Spec> specs, String label, String value, String icon, boolean highlight) {
        String text = text(value);
        if (text != null) {
            specs.add(new PublicVehicleView.Spec(label, text, icon, highlight));
        }
    }

    private static String text(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
