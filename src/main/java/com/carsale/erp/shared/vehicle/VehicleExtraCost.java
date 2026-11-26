package com.carsale.erp.shared.vehicle;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vehicle_extra_costs")
public class VehicleExtraCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chassis_no", nullable = false, length = 40)
    private String chassisNo;

    @Column(name = "cost_date", nullable = false, length = 10)
    private String costDate;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(nullable = false, length = 40)
    private String amount;
}
