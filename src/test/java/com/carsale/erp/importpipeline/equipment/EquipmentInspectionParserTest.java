package com.carsale.erp.importpipeline.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.carsale.erp.shared.document.document.EquipmentInspectionParser;
import org.junit.jupiter.api.Test;

import com.carsale.erp.importpipeline.util.AuctionParseResult;

class EquipmentInspectionParserTest {

    private final EquipmentInspectionParser parser = new EquipmentInspectionParser();

    @Test
    void parsesInteriorExteriorAndSafetyFromSampleSheet() {
        AuctionParseResult result = parser.parsePage(SAMPLE_TEXT);

        assertTrue(result.isSuccess());
        assertEquals("NO", result.getFields().get("sunroof"));
        assertEquals("NO", result.getFields().get("moonRoof"));
        assertEquals("YES", result.getFields().get("cigaretteLighter"));
        assertEquals("YES", result.getFields().get("powerSteering"));
        assertEquals("YES", result.getFields().get("powerWindows"));
        assertEquals("YES", result.getFields().get("centralLocking"));
        assertEquals("NO", result.getFields().get("leatherSeats"));
        assertEquals("NO", result.getFields().get("revolvingSeat"));
        assertEquals("NO", result.getFields().get("seatPower"));
        assertEquals("NO", result.getFields().get("seatMemoryMassager"));
        assertEquals("YES", result.getFields().get("camera"));
        assertEquals("SINGLE", result.getFields().get("airConditioning"));
        assertEquals("YES", result.getFields().get("airPureFilter"));
        assertEquals("NO", result.getFields().get("navigation"));
        assertEquals("NO", result.getFields().get("tv"));
        assertEquals("NO", result.getFields().get("radioCassette"));
        assertEquals("NO", result.getFields().get("cd"));
        assertEquals("NO", result.getFields().get("md"));
        assertEquals("NO", result.getFields().get("cdChanger"));
        assertEquals("NO", result.getFields().get("usbLink"));
        assertEquals("YES", result.getFields().get("rearSpeakers"));
        assertEquals("0", result.getFields().get("floorMats"));
        assertEquals("YES", result.getFields().get("multifunctionSteeringWheel"));
        assertEquals("NO", result.getFields().get("powerShutters"));
        assertEquals("YES", result.getFields().get("parkingSensor"));

        assertEquals("NO", result.getFields().get("bullBarsGrillGuard"));
        assertEquals("N/A", result.getFields().get("rearTyreRack"));
        assertEquals("OK", result.getFields().get("rearWipers"));
        assertEquals("NO", result.getFields().get("roofRail"));
        assertEquals("NO", result.getFields().get("hoodRoofRack"));
        assertEquals("NO", result.getFields().get("bodyKitFront"));
        assertEquals("YES", result.getFields().get("bodyKitSide"));
        assertEquals("NO", result.getFields().get("bodyKitRear"));
        assertEquals("YES", result.getFields().get("alloyWheels"));
        assertEquals("OTHER", result.getFields().get("radioAntenna"));
        assertEquals("YES", result.getFields().get("highMountStopLight"));
        assertEquals("PLASTIC", result.getFields().get("bumpers"));
        assertEquals("0", result.getFields().get("doorVisor"));
        assertEquals("0", result.getFields().get("mudGuardTyreFlaps"));
        assertEquals("NO", result.getFields().get("wheelCoverHubCaps"));
        assertEquals("NO", result.getFields().get("sideSlidingWindows"));
        assertEquals("NO", result.getFields().get("sideSteps"));
        assertEquals("NO", result.getFields().get("rollBars"));
        assertEquals("NO", result.getFields().get("solarPower"));
        assertEquals("NO", result.getFields().get("rearProtectorBar"));
        assertEquals("NO", result.getFields().get("rearSpareTyreCover"));
        assertEquals("NO", result.getFields().get("fogLights"));
        assertEquals("YES", result.getFields().get("fenderMirror"));
        assertEquals("YES", result.getFields().get("powerMirror"));
        assertEquals("NO", result.getFields().get("winch"));
        assertEquals("YES", result.getFields().get("powerDoor"));
        assertEquals("NO", result.getFields().get("rearCargoBedCover"));
        assertEquals("NO", result.getFields().get("canopy"));
        assertEquals("N/A", result.getFields().get("truckCabType"));
        assertEquals("N/A", result.getFields().get("truckTrailerType"));
        assertEquals("NO", result.getFields().get("truckBedLiner"));
        assertEquals("N/A", result.getFields().get("truckCrane"));
        assertEquals("NO", result.getFields().get("truckElectricCurtain"));
        assertEquals("N/A", result.getFields().get("truckFreezerUnit"));

        assertEquals("OK", result.getFields().get("driversAirbag"));
        assertEquals("OK", result.getFields().get("passengerAirbag"));
        assertEquals("OK", result.getFields().get("absBrakingSystem"));
        assertEquals("NO", result.getFields().get("handicapOptions"));
        assertEquals("NO", result.getFields().get("tyreWrench"));
        assertEquals("NO", result.getFields().get("jack"));
        assertEquals("N/A", result.getFields().get("chargingCable"));
        assertEquals("YES", result.getFields().get("punctureKit"));
    }

    @Test
    void doesNotStealCdChangerValueForCd() {
        AuctionParseResult result = parser.parsePage(""
                + "CD NO\n"
                + "CD Changer YES\n");

        assertEquals("NO", result.getFields().get("cd"));
        assertEquals("YES", result.getFields().get("cdChanger"));
    }

    private static final String SAMPLE_TEXT = ""
            + "INTERIOR EQUIPMENT\n"
            + "Sunroof NO Moon Roof NO\n"
            + "Cigarette Lighter YES Power Steering YES\n"
            + "Power Window(s) YES Central Locking YES\n"
            + "Leather (Seats) NO Revolving Seat NO\n"
            + "Seat Power NO Seat Memory / Massager NO\n"
            + "Camera YES Air Conditioning SINGLE\n"
            + "Air Pure Filter YES Navigation NO\n"
            + "TV NO Radio Cassette NO\n"
            + "CD NO MD NO CD Changer NO\n"
            + "USB Link NO Rear Speakers YES\n"
            + "Floor Mats 0 Multifunction Steering Wheel YES\n"
            + "Power Shutters NO Parking Sensor YES\n"
            + "EXTERIOR EQUIPMENT\n"
            + "Bull Bars / Grill Guard NO\n"
            + "Rear Tyre Rack N/A Rear Wipers OK\n"
            + "Roof Rail NO Hood / Roof Rack NO\n"
            + "Body Kit a. Front NO b. Side YES c. Rear NO\n"
            + "Alloy Wheels YES Radio Antenna OTHER\n"
            + "High Mount Stop Light YES Bumpers PLASTIC\n"
            + "Door Visor 0 Mud Guard / Tyre Flaps 0\n"
            + "Wheel Cover / Hub Caps NO Side Sliding Windows NO\n"
            + "Side Steps NO Roll Bars (Roller Bar) NO\n"
            + "Solar Power NO Rear Protector Bar / Bull Bar NO\n"
            + "Rear Spare Tyre Cover (Hard) NO Lights - Fog NO\n"
            + "Fender Mirror YES Power Mirror YES\n"
            + "Winch NO Power Door YES\n"
            + "Rear Cargo Bed Cover NO Canopy (Pick-Up) NO\n"
            + "Truck Body Cab Type N/A Trailer Type N/A\n"
            + "Bed Liner NO Crane / Unic / Tadano / Hiab N/A\n"
            + "Electric Curtain NO Freezer Unit N/A\n"
            + "SAFETY EQUIPMENT\n"
            + "Drivers Airbag OK Passenger Airbag OK\n"
            + "ABS Braking System OK Handicap Options NO\n"
            + "Tyre Wrench NO Jack NO\n"
            + "Charging Cable N/A Puncture Kit YES\n";
}
