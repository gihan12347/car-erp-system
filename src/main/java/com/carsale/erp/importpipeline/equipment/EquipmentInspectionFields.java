package com.carsale.erp.importpipeline.equipment;

import com.carsale.erp.importpipeline.equipment.EquipmentInspectionFields.FieldDef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.carsale.erp.shared.regex.RegexConstants;

/**
 * Interior, exterior, and safety fields on the vehicle equipment condition sheet.
 */
public final class EquipmentInspectionFields {

    public static final String GROUP_BODY_KIT = "bodyKit";
    public static final String GROUP_TRUCK_BODY = "truckBody";

    private static final List<String> CHOICE_OPTIONS = Collections.unmodifiableList(
            Arrays.asList("YES", "NO", "OK", "N/A"));

    private static final List<FieldDef> INTERIOR = list(
            field("sunroof", "Sunroof", "Sunroof"),
            field("moonRoof", "Moon roof", "Moon Roof", "Moonroof"),
            field("cigaretteLighter", "Cigarette lighter", "Cigarette Lighter"),
            field("powerSteering", "Power steering", "Power Steering"),
            field("powerWindows", "Power window(s)", "Power Window(s)", "Power Windows", "Power Window"),
            field("centralLocking", "Central locking", "Central Locking"),
            field("leatherSeats", "Leather (seats)", "Leather (Seats)", "Leather Seats"),
            field("revolvingSeat", "Revolving seat", "Revolving Seat"),
            field("seatPower", "Seat power", "Seat Power"),
            field("seatMemoryMassager", "Seat memory / massager", "Seat Memory / Massager", "Seat Memory/Massager", "Seat Memory"),
            field("camera", "Camera", "Camera"),
            field("airConditioning", "Air conditioning", "Air Conditioning", "A/C"),
            field("airPureFilter", "Air pure filter", "Air Pure Filter", "Air Purifier"),
            field("navigation", "Navigation", "Navigation", "Navi"),
            field("tv", "TV", "TV"),
            field("radioCassette", "Radio cassette", "Radio Cassette", "Radio/Cassette"),
            field("cd", "CD", "CD"),
            field("md", "MD", "MD"),
            field("cdChanger", "CD changer", "CD Changer"),
            field("usbLink", "USB link", "USB Link", "USB"),
            field("rearSpeakers", "Rear speakers", "Rear Speakers"),
            field("floorMats", "Floor mats", "Floor Mats"),
            field("multifunctionSteeringWheel", "Multifunction steering wheel",
                    "Multifunction Steering Wheel", "Multi Function Steering Wheel", "Multifunction Steering"),
            field("powerShutters", "Power shutters", "Power Shutters"),
            field("parkingSensor", "Parking sensor", "Parking Sensor", "Park Sensor")
    );

    private static final List<FieldDef> EXTERIOR = list(
            field("bullBarsGrillGuard", "Bull bars / grill guard", "Bull Bars / Grill Guard", "Bull Bars/Grill Guard", "Bull Bars", "Grill Guard"),
            field("rearTyreRack", "Rear tyre rack", "Rear Tyre Rack", "Rear Tire Rack"),
            field("rearWipers", "Rear wipers", "Rear Wipers", "Rear Wiper"),
            field("roofRail", "Roof rail", "Roof Rail", "Roof Rails"),
            field("hoodRoofRack", "Hood / roof rack", "Hood / Roof Rack", "Hood/Roof Rack"),
            grouped("bodyKitFront", "Front", GROUP_BODY_KIT, "a. Front", "a Front"),
            grouped("bodyKitSide", "Side", GROUP_BODY_KIT, "b. Side", "b Side"),
            grouped("bodyKitRear", "Rear", GROUP_BODY_KIT, "c. Rear", "c Rear"),
            field("alloyWheels", "Alloy wheels", "Alloy Wheels"),
            field("radioAntenna", "Radio antenna", "Radio Antenna"),
            field("highMountStopLight", "High mount stop light", "High Mount Stop Light", "High Mount Stop", "HMSL"),
            field("bumpers", "Bumpers", "Bumpers", "Bumper"),
            field("doorVisor", "Door visor", "Door Visor", "Door Visors"),
            field("mudGuardTyreFlaps", "Mud guard / tyre flaps", "Mud Guard / Tyre Flaps", "Mud Guard/Tyre Flaps", "Mud Guard", "Tyre Flaps"),
            field("wheelCoverHubCaps", "Wheel cover / hub caps", "Wheel Cover / Hub Caps", "Wheel Cover/Hub Caps", "Wheel Cover", "Hub Caps"),
            field("sideSlidingWindows", "Side sliding windows", "Side Sliding Windows"),
            field("sideSteps", "Side steps", "Side Steps"),
            field("rollBars", "Roll bars (roller bar)", "Roll Bars (Roller Bar)", "Roll Bars", "Roller Bar"),
            field("solarPower", "Solar power", "Solar Power"),
            field("rearProtectorBar", "Rear protector bar / bull bar", "Rear Protector Bar / Bull Bar", "Rear Protector Bar"),
            field("rearSpareTyreCover", "Rear spare tyre cover (hard)", "Rear Spare Tyre Cover (Hard)", "Rear Spare Tyre Cover", "Spare Tyre Cover"),
            field("fogLights", "Lights — fog", "Lights - Fog", "Lights Fog", "Fog Lights", "Fog Light"),
            field("fenderMirror", "Fender mirror", "Fender Mirror", "Fender Mirrors"),
            field("powerMirror", "Power mirror", "Power Mirror", "Power Mirrors"),
            field("winch", "Winch", "Winch"),
            field("powerDoor", "Power door", "Power Door", "Power Doors"),
            field("rearCargoBedCover", "Rear cargo bed cover", "Rear Cargo Bed Cover", "Cargo Bed Cover"),
            field("canopy", "Canopy (pick-up)", "Canopy (Pick-Up)", "Canopy (Pick Up)", "Canopy"),
            grouped("truckCabType", "Cab type", GROUP_TRUCK_BODY, "Cab Type"),
            grouped("truckTrailerType", "Trailer type", GROUP_TRUCK_BODY, "Trailer Type"),
            grouped("truckBedLiner", "Bed liner", GROUP_TRUCK_BODY, "Bed Liner"),
            grouped("truckCrane", "Crane / Unic / Tadano / Hiab", GROUP_TRUCK_BODY,
                    "Crane / Unic / Tadano / Hiab", "Crane/Unic/Tadano/Hiab", "Crane"),
            grouped("truckElectricCurtain", "Electric curtain", GROUP_TRUCK_BODY, "Electric Curtain"),
            grouped("truckFreezerUnit", "Freezer unit", GROUP_TRUCK_BODY, "Freezer Unit")
    );

    private static final List<FieldDef> SAFETY = list(
            field("driversAirbag", "Driver's airbag", "Drivers Airbag", "Driver's Airbag", "Driver Airbag"),
            field("passengerAirbag", "Passenger airbag", "Passenger Airbag"),
            field("absBrakingSystem", "ABS braking system", "ABS Braking System", "ABS"),
            field("handicapOptions", "Handicap options", "Handicap Options"),
            field("tyreWrench", "Tyre wrench", "Tyre Wrench", "Tire Wrench"),
            field("jack", "Jack", "Jack"),
            field("chargingCable", "Charging cable", "Charging Cable"),
            field("punctureKit", "Puncture kit", "Puncture Kit")
    );

    private EquipmentInspectionFields() {
    }

    public static List<FieldDef> interior() {
        return INTERIOR;
    }

    public static List<FieldDef> exterior() {
        return EXTERIOR;
    }

    public static List<FieldDef> safety() {
        return SAFETY;
    }

    public static List<FieldDef> ungrouped(List<FieldDef> fields) {
        List<FieldDef> result = new ArrayList<FieldDef>();
        for (FieldDef field : fields) {
            if (field.getGroup() == null) {
                result.add(field);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static List<FieldDef> group(List<FieldDef> fields, String group) {
        List<FieldDef> result = new ArrayList<FieldDef>();
        for (FieldDef field : fields) {
            if (group.equals(field.getGroup())) {
                result.add(field);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static List<FieldDef> all() {
        List<FieldDef> result = new ArrayList<FieldDef>();
        result.addAll(INTERIOR);
        result.addAll(EXTERIOR);
        result.addAll(SAFETY);
        return Collections.unmodifiableList(result);
    }

    public static List<String> allKeys() {
        List<String> keys = new ArrayList<String>();
        for (FieldDef field : all()) {
            keys.add(field.getKey());
        }
        return Collections.unmodifiableList(keys);
    }

    public static List<String> choiceOptions() {
        return CHOICE_OPTIONS;
    }

    public static boolean isChoiceOption(String value) {
        String canonical = canonicalChoice(value);
        return !canonical.isEmpty() && CHOICE_OPTIONS.contains(canonical);
    }

    public static boolean sameChoice(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        String leftValue = canonicalChoice(left);
        String rightValue = canonicalChoice(right);
        return !leftValue.isEmpty() && leftValue.equals(rightValue);
    }

    public static String choiceDomId(String key, String option) {
        String suffix = canonicalChoice(option).replaceAll(RegexConstants.Text.ALNUM_ONLY, "");
        if (suffix.isEmpty()) {
            suffix = "choice";
        }
        return key + "_" + suffix;
    }

    public static String canonicalChoice(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll(RegexConstants.Text.WHITESPACE, " ");
        if (trimmed.isEmpty()) {
            return "";
        }
        String compact = trimmed.toUpperCase(Locale.ROOT).replace(" ", "");
        if ("YES".equals(compact) || "Y".equals(compact)) {
            return "YES";
        }
        if ("NO".equals(compact) || "N".equals(compact)) {
            return "NO";
        }
        if ("OK".equals(compact)) {
            return "OK";
        }
        if ("NA".equals(compact) || "N/A".equals(compact) || "N.A.".equals(compact)
                || "N.A".equals(compact) || "NIL".equals(compact) || "NONE".equals(compact)
                || "NOTAPPLICABLE".equals(compact)) {
            return "N/A";
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private static List<FieldDef> list(FieldDef... fields) {
        List<FieldDef> result = new ArrayList<FieldDef>();
        Collections.addAll(result, fields);
        return Collections.unmodifiableList(result);
    }

    private static FieldDef field(String key, String label, String... aliases) {
        return new FieldDef(key, label, null, aliases);
    }

    private static FieldDef grouped(String key, String label, String group, String... aliases) {
        return new FieldDef(key, label, group, aliases);
    }

    public static final class FieldDef {
        private final String key;
        private final String label;
        private final String group;
        private final String[] aliases;

        private FieldDef(String key, String label, String group, String[] aliases) {
            this.key = key;
            this.label = label;
            this.group = group;
            this.aliases = aliases == null ? new String[0] : aliases;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public String getGroup() {
            return group;
        }

        public String[] getAliases() {
            return aliases;
        }
    }
}
