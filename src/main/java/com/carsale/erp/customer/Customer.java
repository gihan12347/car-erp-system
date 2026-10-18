package com.carsale.erp.customer;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chassis_no", length = 40)
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

    @Column(name = "back_document_original_name", length = 255)
    private String backDocumentOriginalName;

    @Column(name = "back_document_stored_name", length = 120)
    private String backDocumentStoredName;

    @Column(name = "back_document_content_type", length = 80)
    private String backDocumentContentType;

    @Lob
    @Column(name = "back_ocr_text", columnDefinition = "TEXT")
    private String backOcrText;
}
