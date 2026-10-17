package com.carsale.erp.readypipeline.model;

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
@Table(name = "sale_listings")
public class SaleListing {

    @Id
    @Column(name = "chassis_no", length = 40, nullable = false)
    private String chassisNo;

    @Column(length = 40)
    private String askingPrice;

    @Column(length = 40)
    private String advertisedPrice;

    @Column(length = 40)
    private String saleCondition;

    @Column(length = 40)
    private String listedOn;

    @Column(name = "sale_code", length = 40)
    private String saleCode;

    @Column(name = "sale_location", length = 80)
    private String saleLocation;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String listingNotes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean listed;

    @Column(nullable = false)
    private boolean sold;

    @Column(name = "assignment_complete", nullable = false)
    private boolean assignmentComplete;

    @Lob
    @Column(name = "buyer_full_name", columnDefinition = "TEXT")
    private String buyerFullName;

    @Column(name = "buyer_nic_no", length = 40)
    private String buyerNicNo;

    @Lob
    @Column(name = "buyer_address", columnDefinition = "TEXT")
    private String buyerAddress;

    @Column(name = "buyer_date_of_birth", length = 40)
    private String buyerDateOfBirth;

    @Column(name = "buyer_gender", length = 40)
    private String buyerGender;

    @Column(name = "buyer_document_original_name", length = 255)
    private String buyerDocumentOriginalName;

    @Column(name = "buyer_document_stored_name", length = 120)
    private String buyerDocumentStoredName;

    @Column(name = "buyer_document_content_type", length = 80)
    private String buyerDocumentContentType;

    @Lob
    @Column(name = "buyer_ocr_text", columnDefinition = "TEXT")
    private String buyerOcrText;

    @Column(name = "buyer_back_document_original_name", length = 255)
    private String buyerBackDocumentOriginalName;

    @Column(name = "buyer_back_document_stored_name", length = 120)
    private String buyerBackDocumentStoredName;

    @Column(name = "buyer_back_document_content_type", length = 80)
    private String buyerBackDocumentContentType;

    @Lob
    @Column(name = "buyer_back_ocr_text", columnDefinition = "TEXT")
    private String buyerBackOcrText;

    public String getListedOnIso() {
        return isoOrBlank(listedOn);
    }

    public String getBuyerDateOfBirthIso() {
        return isoOrBlank(buyerDateOfBirth);
    }

    public static String toIsoDate(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        String normalized = text.replace('.', '/').replace('-', '/');
        String[] parts = normalized.split("/");
        if (parts.length < 3) {
            return null;
        }
        String year;
        String month;
        String day;
        if (parts[0].length() == 4) {
            year = parts[0];
            month = digits(parts[1]);
            day = digits(parts[2]);
        } else if (parts[2].length() >= 4) {
            year = parts[2].substring(0, 4);
            month = digits(parts[1]);
            day = digits(parts[0]);
        } else {
            return null;
        }
        if (year.length() != 4 || month.length() != 2 || day.length() != 2) {
            return null;
        }
        try {
            java.time.LocalDate.parse(year + "-" + month + "-" + day);
        } catch (RuntimeException ex) {
            return null;
        }
        return year + "-" + month + "-" + day;
    }

    private static String isoOrBlank(String raw) {
        String iso = toIsoDate(raw);
        return iso == null ? "" : iso;
    }

    private static String digits(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("\\D", "");
        if (cleaned.length() >= 2) {
            return cleaned.substring(0, 2);
        }
        if (cleaned.length() == 1) {
            return "0" + cleaned;
        }
        return "";
    }

}
