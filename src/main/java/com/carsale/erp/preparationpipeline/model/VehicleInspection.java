package com.carsale.erp.preparationpipeline.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Setter
@Getter
@Entity
@Table(name = "vehicle_inspections")
public class VehicleInspection {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(length = 80)
    private String inspector;

    @Column(length = 40)
    private String inspectionDate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean completed;

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC, id ASC")
    private List<VehicleInspectionLine> lines = new ArrayList<>();

    public List<VehicleInspectionLine> getLines() {
        if (lines == null) {
            lines = new ArrayList<>();
        }
        return lines;
    }

}
