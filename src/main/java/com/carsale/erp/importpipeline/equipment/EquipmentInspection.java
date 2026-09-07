package com.carsale.erp.importpipeline.equipment;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "equipment_inspections")
public class EquipmentInspection {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(length = 20)
    private String sunroof;
    @Column(length = 20)
    private String moonRoof;
    @Column(length = 20)
    private String cigaretteLighter;
    @Column(length = 20)
    private String powerSteering;
    @Column(length = 20)
    private String powerWindows;
    @Column(length = 20)
    private String centralLocking;
    @Column(length = 20)
    private String leatherSeats;
    @Column(length = 20)
    private String revolvingSeat;
    @Column(length = 20)
    private String seatPower;
    @Column(length = 20)
    private String seatMemoryMassager;
    @Column(length = 20)
    private String camera;
    @Column(length = 20)
    private String airConditioning;
    @Column(length = 20)
    private String airPureFilter;
    @Column(length = 20)
    private String navigation;
    @Column(length = 20)
    private String tv;
    @Column(length = 20)
    private String radioCassette;
    @Column(length = 20)
    private String cd;
    @Column(length = 20)
    private String md;
    @Column(length = 20)
    private String cdChanger;
    @Column(length = 20)
    private String usbLink;
    @Column(length = 20)
    private String rearSpeakers;
    @Column(length = 20)
    private String floorMats;
    @Column(length = 20)
    private String multifunctionSteeringWheel;
    @Column(length = 20)
    private String powerShutters;
    @Column(length = 20)
    private String parkingSensor;

    @Column(length = 20)
    private String bullBarsGrillGuard;
    @Column(length = 20)
    private String rearTyreRack;
    @Column(length = 20)
    private String rearWipers;
    @Column(length = 20)
    private String roofRail;
    @Column(length = 20)
    private String hoodRoofRack;
    @Column(length = 20)
    private String bodyKitFront;
    @Column(length = 20)
    private String bodyKitSide;
    @Column(length = 20)
    private String bodyKitRear;
    @Column(length = 20)
    private String alloyWheels;
    @Column(length = 20)
    private String radioAntenna;
    @Column(length = 20)
    private String highMountStopLight;
    @Column(length = 20)
    private String bumpers;
    @Column(length = 20)
    private String doorVisor;
    @Column(length = 20)
    private String mudGuardTyreFlaps;
    @Column(length = 20)
    private String wheelCoverHubCaps;
    @Column(length = 20)
    private String sideSlidingWindows;
    @Column(length = 20)
    private String sideSteps;
    @Column(length = 20)
    private String rollBars;
    @Column(length = 20)
    private String solarPower;
    @Column(length = 20)
    private String rearProtectorBar;
    @Column(length = 20)
    private String rearSpareTyreCover;
    @Column(length = 20)
    private String fogLights;
    @Column(length = 20)
    private String fenderMirror;
    @Column(length = 20)
    private String powerMirror;
    @Column(length = 20)
    private String winch;
    @Column(length = 20)
    private String powerDoor;
    @Column(length = 20)
    private String rearCargoBedCover;
    @Column(length = 20)
    private String canopy;
    @Column(length = 20)
    private String truckCabType;
    @Column(length = 20)
    private String truckTrailerType;
    @Column(length = 20)
    private String truckBedLiner;
    @Column(length = 20)
    private String truckCrane;
    @Column(length = 20)
    private String truckElectricCurtain;
    @Column(length = 20)
    private String truckFreezerUnit;

    @Column(length = 20)
    private String driversAirbag;
    @Column(length = 20)
    private String passengerAirbag;
    @Column(length = 20)
    private String absBrakingSystem;
    @Column(length = 20)
    private String handicapOptions;
    @Column(length = 20)
    private String tyreWrench;
    @Column(length = 20)
    private String jack;
    @Column(length = 20)
    private String chargingCable;
    @Column(length = 20)
    private String punctureKit;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrText;

    @Column(length = 255)
    private String documentOriginalName;

    @Column(length = 120)
    private String documentStoredName;

    @Column(length = 80)
    private String documentContentType;

    public String getChassisNo() {
        return chassisNo;
    }

    public void setChassisNo(String chassisNo) {
        this.chassisNo = chassisNo;
    }

    public String getSunroof() {
        return sunroof;
    }

    public void setSunroof(String sunroof) {
        this.sunroof = sunroof;
    }

    public String getMoonRoof() {
        return moonRoof;
    }

    public void setMoonRoof(String moonRoof) {
        this.moonRoof = moonRoof;
    }

    public String getCigaretteLighter() {
        return cigaretteLighter;
    }

    public void setCigaretteLighter(String cigaretteLighter) {
        this.cigaretteLighter = cigaretteLighter;
    }

    public String getPowerSteering() {
        return powerSteering;
    }

    public void setPowerSteering(String powerSteering) {
        this.powerSteering = powerSteering;
    }

    public String getPowerWindows() {
        return powerWindows;
    }

    public void setPowerWindows(String powerWindows) {
        this.powerWindows = powerWindows;
    }

    public String getCentralLocking() {
        return centralLocking;
    }

    public void setCentralLocking(String centralLocking) {
        this.centralLocking = centralLocking;
    }

    public String getLeatherSeats() {
        return leatherSeats;
    }

    public void setLeatherSeats(String leatherSeats) {
        this.leatherSeats = leatherSeats;
    }

    public String getRevolvingSeat() {
        return revolvingSeat;
    }

    public void setRevolvingSeat(String revolvingSeat) {
        this.revolvingSeat = revolvingSeat;
    }

    public String getSeatPower() {
        return seatPower;
    }

    public void setSeatPower(String seatPower) {
        this.seatPower = seatPower;
    }

    public String getSeatMemoryMassager() {
        return seatMemoryMassager;
    }

    public void setSeatMemoryMassager(String seatMemoryMassager) {
        this.seatMemoryMassager = seatMemoryMassager;
    }

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }

    public String getAirConditioning() {
        return airConditioning;
    }

    public void setAirConditioning(String airConditioning) {
        this.airConditioning = airConditioning;
    }

    public String getAirPureFilter() {
        return airPureFilter;
    }

    public void setAirPureFilter(String airPureFilter) {
        this.airPureFilter = airPureFilter;
    }

    public String getNavigation() {
        return navigation;
    }

    public void setNavigation(String navigation) {
        this.navigation = navigation;
    }

    public String getTv() {
        return tv;
    }

    public void setTv(String tv) {
        this.tv = tv;
    }

    public String getRadioCassette() {
        return radioCassette;
    }

    public void setRadioCassette(String radioCassette) {
        this.radioCassette = radioCassette;
    }

    public String getCd() {
        return cd;
    }

    public void setCd(String cd) {
        this.cd = cd;
    }

    public String getMd() {
        return md;
    }

    public void setMd(String md) {
        this.md = md;
    }

    public String getCdChanger() {
        return cdChanger;
    }

    public void setCdChanger(String cdChanger) {
        this.cdChanger = cdChanger;
    }

    public String getUsbLink() {
        return usbLink;
    }

    public void setUsbLink(String usbLink) {
        this.usbLink = usbLink;
    }

    public String getRearSpeakers() {
        return rearSpeakers;
    }

    public void setRearSpeakers(String rearSpeakers) {
        this.rearSpeakers = rearSpeakers;
    }

    public String getFloorMats() {
        return floorMats;
    }

    public void setFloorMats(String floorMats) {
        this.floorMats = floorMats;
    }

    public String getMultifunctionSteeringWheel() {
        return multifunctionSteeringWheel;
    }

    public void setMultifunctionSteeringWheel(String multifunctionSteeringWheel) {
        this.multifunctionSteeringWheel = multifunctionSteeringWheel;
    }

    public String getPowerShutters() {
        return powerShutters;
    }

    public void setPowerShutters(String powerShutters) {
        this.powerShutters = powerShutters;
    }

    public String getParkingSensor() {
        return parkingSensor;
    }

    public void setParkingSensor(String parkingSensor) {
        this.parkingSensor = parkingSensor;
    }

    public String getBullBarsGrillGuard() {
        return bullBarsGrillGuard;
    }

    public void setBullBarsGrillGuard(String bullBarsGrillGuard) {
        this.bullBarsGrillGuard = bullBarsGrillGuard;
    }

    public String getRearTyreRack() {
        return rearTyreRack;
    }

    public void setRearTyreRack(String rearTyreRack) {
        this.rearTyreRack = rearTyreRack;
    }

    public String getRearWipers() {
        return rearWipers;
    }

    public void setRearWipers(String rearWipers) {
        this.rearWipers = rearWipers;
    }

    public String getRoofRail() {
        return roofRail;
    }

    public void setRoofRail(String roofRail) {
        this.roofRail = roofRail;
    }

    public String getHoodRoofRack() {
        return hoodRoofRack;
    }

    public void setHoodRoofRack(String hoodRoofRack) {
        this.hoodRoofRack = hoodRoofRack;
    }

    public String getBodyKitFront() {
        return bodyKitFront;
    }

    public void setBodyKitFront(String bodyKitFront) {
        this.bodyKitFront = bodyKitFront;
    }

    public String getBodyKitSide() {
        return bodyKitSide;
    }

    public void setBodyKitSide(String bodyKitSide) {
        this.bodyKitSide = bodyKitSide;
    }

    public String getBodyKitRear() {
        return bodyKitRear;
    }

    public void setBodyKitRear(String bodyKitRear) {
        this.bodyKitRear = bodyKitRear;
    }

    public String getAlloyWheels() {
        return alloyWheels;
    }

    public void setAlloyWheels(String alloyWheels) {
        this.alloyWheels = alloyWheels;
    }

    public String getRadioAntenna() {
        return radioAntenna;
    }

    public void setRadioAntenna(String radioAntenna) {
        this.radioAntenna = radioAntenna;
    }

    public String getHighMountStopLight() {
        return highMountStopLight;
    }

    public void setHighMountStopLight(String highMountStopLight) {
        this.highMountStopLight = highMountStopLight;
    }

    public String getBumpers() {
        return bumpers;
    }

    public void setBumpers(String bumpers) {
        this.bumpers = bumpers;
    }

    public String getDoorVisor() {
        return doorVisor;
    }

    public void setDoorVisor(String doorVisor) {
        this.doorVisor = doorVisor;
    }

    public String getMudGuardTyreFlaps() {
        return mudGuardTyreFlaps;
    }

    public void setMudGuardTyreFlaps(String mudGuardTyreFlaps) {
        this.mudGuardTyreFlaps = mudGuardTyreFlaps;
    }

    public String getWheelCoverHubCaps() {
        return wheelCoverHubCaps;
    }

    public void setWheelCoverHubCaps(String wheelCoverHubCaps) {
        this.wheelCoverHubCaps = wheelCoverHubCaps;
    }

    public String getSideSlidingWindows() {
        return sideSlidingWindows;
    }

    public void setSideSlidingWindows(String sideSlidingWindows) {
        this.sideSlidingWindows = sideSlidingWindows;
    }

    public String getSideSteps() {
        return sideSteps;
    }

    public void setSideSteps(String sideSteps) {
        this.sideSteps = sideSteps;
    }

    public String getRollBars() {
        return rollBars;
    }

    public void setRollBars(String rollBars) {
        this.rollBars = rollBars;
    }

    public String getSolarPower() {
        return solarPower;
    }

    public void setSolarPower(String solarPower) {
        this.solarPower = solarPower;
    }

    public String getRearProtectorBar() {
        return rearProtectorBar;
    }

    public void setRearProtectorBar(String rearProtectorBar) {
        this.rearProtectorBar = rearProtectorBar;
    }

    public String getRearSpareTyreCover() {
        return rearSpareTyreCover;
    }

    public void setRearSpareTyreCover(String rearSpareTyreCover) {
        this.rearSpareTyreCover = rearSpareTyreCover;
    }

    public String getFogLights() {
        return fogLights;
    }

    public void setFogLights(String fogLights) {
        this.fogLights = fogLights;
    }

    public String getFenderMirror() {
        return fenderMirror;
    }

    public void setFenderMirror(String fenderMirror) {
        this.fenderMirror = fenderMirror;
    }

    public String getPowerMirror() {
        return powerMirror;
    }

    public void setPowerMirror(String powerMirror) {
        this.powerMirror = powerMirror;
    }

    public String getWinch() {
        return winch;
    }

    public void setWinch(String winch) {
        this.winch = winch;
    }

    public String getPowerDoor() {
        return powerDoor;
    }

    public void setPowerDoor(String powerDoor) {
        this.powerDoor = powerDoor;
    }

    public String getRearCargoBedCover() {
        return rearCargoBedCover;
    }

    public void setRearCargoBedCover(String rearCargoBedCover) {
        this.rearCargoBedCover = rearCargoBedCover;
    }

    public String getCanopy() {
        return canopy;
    }

    public void setCanopy(String canopy) {
        this.canopy = canopy;
    }

    public String getTruckCabType() {
        return truckCabType;
    }

    public void setTruckCabType(String truckCabType) {
        this.truckCabType = truckCabType;
    }

    public String getTruckTrailerType() {
        return truckTrailerType;
    }

    public void setTruckTrailerType(String truckTrailerType) {
        this.truckTrailerType = truckTrailerType;
    }

    public String getTruckBedLiner() {
        return truckBedLiner;
    }

    public void setTruckBedLiner(String truckBedLiner) {
        this.truckBedLiner = truckBedLiner;
    }

    public String getTruckCrane() {
        return truckCrane;
    }

    public void setTruckCrane(String truckCrane) {
        this.truckCrane = truckCrane;
    }

    public String getTruckElectricCurtain() {
        return truckElectricCurtain;
    }

    public void setTruckElectricCurtain(String truckElectricCurtain) {
        this.truckElectricCurtain = truckElectricCurtain;
    }

    public String getTruckFreezerUnit() {
        return truckFreezerUnit;
    }

    public void setTruckFreezerUnit(String truckFreezerUnit) {
        this.truckFreezerUnit = truckFreezerUnit;
    }

    public String getDriversAirbag() {
        return driversAirbag;
    }

    public void setDriversAirbag(String driversAirbag) {
        this.driversAirbag = driversAirbag;
    }

    public String getPassengerAirbag() {
        return passengerAirbag;
    }

    public void setPassengerAirbag(String passengerAirbag) {
        this.passengerAirbag = passengerAirbag;
    }

    public String getAbsBrakingSystem() {
        return absBrakingSystem;
    }

    public void setAbsBrakingSystem(String absBrakingSystem) {
        this.absBrakingSystem = absBrakingSystem;
    }

    public String getHandicapOptions() {
        return handicapOptions;
    }

    public void setHandicapOptions(String handicapOptions) {
        this.handicapOptions = handicapOptions;
    }

    public String getTyreWrench() {
        return tyreWrench;
    }

    public void setTyreWrench(String tyreWrench) {
        this.tyreWrench = tyreWrench;
    }

    public String getJack() {
        return jack;
    }

    public void setJack(String jack) {
        this.jack = jack;
    }

    public String getChargingCable() {
        return chargingCable;
    }

    public void setChargingCable(String chargingCable) {
        this.chargingCable = chargingCable;
    }

    public String getPunctureKit() {
        return punctureKit;
    }

    public void setPunctureKit(String punctureKit) {
        this.punctureKit = punctureKit;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public String getDocumentOriginalName() {
        return documentOriginalName;
    }

    public void setDocumentOriginalName(String documentOriginalName) {
        this.documentOriginalName = documentOriginalName;
    }

    public String getDocumentStoredName() {
        return documentStoredName;
    }

    public void setDocumentStoredName(String documentStoredName) {
        this.documentStoredName = documentStoredName;
    }

    public String getDocumentContentType() {
        return documentContentType;
    }

    public void setDocumentContentType(String documentContentType) {
        this.documentContentType = documentContentType;
    }
}
