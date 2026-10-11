package com.carsale.erp.preparationpipeline.model;

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
@Table(name = "previous_owners")
public class PreviousOwner {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String fullName;

    @Column(length = 40)
    private String nicNo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 40)
    private String dateOfBirth;

    @Column(length = 40)
    private String gender;

    @Column(length = 255)
    private String documentOriginalName;

    @Column(length = 120)
    private String documentStoredName;

    @Column(length = 80)
    private String documentContentType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrText;

}
