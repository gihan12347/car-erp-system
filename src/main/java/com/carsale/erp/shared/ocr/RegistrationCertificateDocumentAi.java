package com.carsale.erp.shared.ocr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request options for the Sri Lanka certificate-of-registration custom extractor.
 * The processor rejects a call unless the request names at least one entity type.
 */
public final class RegistrationCertificateDocumentAi {

    private static final String[] FIELDS = {
            "absolute_owner",
            "chassis_no",
            "class_of_vehicle",
            "colour",
            "conditions_special_notes",
            "country_of_origin",
            "cr_no",
            "current_owner_address_id_no",
            "cylinder_capacity",
            "date_of_first_registration",
            "engine_no",
            "fuel_type",
            "height",
            "length",
            "make",
            "manufactures_description",
            "model",
            "over_hang",
            "previous_owners",
            "printed_date",
            "provincial_council",
            "registration_no",
            "seating_capacity",
            "status_when_registered",
            "taxation_class",
            "taxes_payable",
            "type_of_body",
            "tyre_size_dual",
            "tyre_size_front",
            "tyre_size_rear",
            "tyre_size_single",
            "weight_gross",
            "weight_unladen",
            "wheel_base",
            "width",
            "year_of_manufacture"
    };

    private RegistrationCertificateDocumentAi() {
    }

    public static Map<String, Object> processOptions() {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("schemaOverride", schema());
        return options;
    }

    public static String[] fieldNames() {
        return FIELDS.clone();
    }

    private static Map<String, Object> schema() {
        List<Map<String, Object>> properties = new ArrayList<Map<String, Object>>();
        for (String name : FIELDS) {
            properties.add(property(name));
        }

        Map<String, Object> documentType = new LinkedHashMap<String, Object>();
        documentType.put("name", "custom_extraction_document_type");
        documentType.put("baseTypes", Collections.singletonList("document"));
        documentType.put("properties", properties);

        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("entityTypes", Collections.singletonList(documentType));
        return schema;
    }

    private static Map<String, Object> property(String name) {
        Map<String, Object> property = new LinkedHashMap<String, Object>();
        property.put("name", name);
        property.put("valueType", "string");
        property.put("occurrenceType", "OPTIONAL_ONCE");
        return property;
    }
}
