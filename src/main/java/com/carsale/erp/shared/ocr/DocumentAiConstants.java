package com.carsale.erp.shared.ocr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Document names and custom-extractor schema values sent to Document AI.
 */
public final class DocumentAiConstants {

    private DocumentAiConstants() {
    }

    public static final class Documents {
        private Documents() {
        }

        public static final String AUCTION_SHEET = "auction sheet";
        public static final String REGISTRATION_CERTIFICATE = "registration certificate";
        public static final String WORKING_SHEET = "working sheet";
    }

    public static final class Schema {
        private Schema() {
        }

        public static final String SCHEMA_OVERRIDE = "schemaOverride";
        public static final String ENTITY_TYPES = "entityTypes";
        public static final String NAME = "name";
        public static final String BASE_TYPES = "baseTypes";
        public static final String PROPERTIES = "properties";
        public static final String VALUE_TYPE = "valueType";
        public static final String OCCURRENCE_TYPE = "occurrenceType";
        public static final String CUSTOM_EXTRACTION_DOCUMENT_TYPE = "custom_extraction_document_type";
        public static final String DOCUMENT = "document";
        public static final String STRING = "string";
        public static final String OPTIONAL_ONCE = "OPTIONAL_ONCE";
    }

    public static final String[] AUCTION_FIELDS = {
            "chassis_no",
            "lot_no",
            "model_code",
            "make",
            "model",
            "grade",
            "auction_grade",
            "exterior_grade",
            "interior_grade",
            "mileage",
            "year",
            "engine_size",
            "fuel",
            "transmission",
            "color",
            "color_code",
            "seats",
            "body_style",
            "ac",
            "history"
    };

    public static final String[] REGISTRATION_FIELDS = {
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

    public static Map<String, Object> customExtractorOptions(String[] fields) {
        List<Map<String, Object>> properties = new ArrayList<>();
        for (String name : fields) {
            Map<String, Object> property = new LinkedHashMap<>();
            property.put(Schema.NAME, name);
            property.put(Schema.VALUE_TYPE, Schema.STRING);
            property.put(Schema.OCCURRENCE_TYPE, Schema.OPTIONAL_ONCE);
            properties.add(property);
        }

        Map<String, Object> documentType = new LinkedHashMap<>();
        documentType.put(Schema.NAME, Schema.CUSTOM_EXTRACTION_DOCUMENT_TYPE);
        documentType.put(Schema.BASE_TYPES, Collections.singletonList(Schema.DOCUMENT));
        documentType.put(Schema.PROPERTIES, properties);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put(Schema.ENTITY_TYPES, Collections.singletonList(documentType));

        Map<String, Object> options = new LinkedHashMap<>();
        options.put(Schema.SCHEMA_OVERRIDE, schema);
        return options;
    }
}
