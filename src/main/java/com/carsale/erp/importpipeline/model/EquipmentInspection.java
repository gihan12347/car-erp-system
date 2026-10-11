package com.carsale.erp.importpipeline.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Setter
@Getter
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

    @Column()
    private String documentOriginalName;

    @Column(length = 120)
    private String documentStoredName;

    @Column(length = 80)
    private String documentContentType;

}
