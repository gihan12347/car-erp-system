package com.carsale.erp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.carsale.erp.enums.FlowPipeline;

/**
 * Hibernate ddl-auto does not always add or widen columns on existing tables.
 */
@Component
@Order(1)
public class DatabaseSchemaUpdater implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaUpdater.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaUpdater(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        widenColumn();
        createClearanceAssessmentTable();
        createPipelineFlowsTable();
        seedPipelineFlows();
        createPipelineStagesTable();
        migratePipelineStagesToFlowId();
        ensurePipelineStagesFlowForeignKey();
        createEquipmentInspectionsTable();
        createInspectionItemsTable();
        createVehicleInspectionsTable();
        createVehicleInspectionLinesTable();
        addWorkshopJobInspectionItemKey();
    }

    private void createInspectionItemsTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS inspection_items ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT, "
                            + "item_key VARCHAR(80) NOT NULL, "
                            + "title VARCHAR(160) NOT NULL, "
                            + "sort_order INT NOT NULL, "
                            + "active TINYINT(1) NOT NULL DEFAULT 1, "
                            + "PRIMARY KEY (id), "
                            + "UNIQUE KEY uk_inspection_items_key (item_key)"
                            + ")"
            );
            log.info("Verified table inspection_items");
        } catch (Exception ex) {
            log.warn("Could not create inspection_items: {}", ex.getMessage());
        }
    }

    private void createVehicleInspectionsTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS vehicle_inspections ("
                            + "chassis_no VARCHAR(40) NOT NULL, "
                            + "inspector VARCHAR(80), "
                            + "inspection_date VARCHAR(40), "
                            + "notes TEXT, "
                            + "completed TINYINT(1) NOT NULL DEFAULT 0, "
                            + "PRIMARY KEY (chassis_no)"
                            + ")"
            );
            log.info("Verified table vehicle_inspections");
        } catch (Exception ex) {
            log.warn("Could not create vehicle_inspections: {}", ex.getMessage());
        }
    }

    private void createVehicleInspectionLinesTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS vehicle_inspection_lines ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT, "
                            + "chassis_no VARCHAR(40) NOT NULL, "
                            + "item_key VARCHAR(80), "
                            + "item_title VARCHAR(160) NOT NULL, "
                            + "result VARCHAR(12), "
                            + "notes TEXT, "
                            + "sort_order INT NOT NULL, "
                            + "catalog_item TINYINT(1) NOT NULL DEFAULT 0, "
                            + "PRIMARY KEY (id), "
                            + "KEY idx_vehicle_inspection_lines_chassis (chassis_no), "
                            + "CONSTRAINT fk_vehicle_inspection_lines_record "
                            + "FOREIGN KEY (chassis_no) REFERENCES vehicle_inspections (chassis_no) "
                            + "ON DELETE CASCADE"
                            + ")"
            );
            log.info("Verified table vehicle_inspection_lines");
        } catch (Exception ex) {
            log.warn("Could not create vehicle_inspection_lines: {}", ex.getMessage());
        }
    }

    private void addWorkshopJobInspectionItemKey() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND UPPER(TABLE_NAME) = UPPER('workshop_job_lines') "
                            + "AND UPPER(COLUMN_NAME) = UPPER('inspection_item_key')",
                    Integer.class
            );
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE workshop_job_lines ADD COLUMN inspection_item_key VARCHAR(80) NULL"
            );
            log.info("Added workshop_job_lines.inspection_item_key");
        } catch (Exception ex) {
            log.warn("Could not add workshop_job_lines.inspection_item_key: {}", ex.getMessage());
        }
    }

    private void createEquipmentInspectionsTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS equipment_inspections ("
                            + "chassis_no VARCHAR(40) NOT NULL, "
                            + "sunroof VARCHAR(20), moon_roof VARCHAR(20), cigarette_lighter VARCHAR(20), "
                            + "power_steering VARCHAR(20), power_windows VARCHAR(20), central_locking VARCHAR(20), "
                            + "leather_seats VARCHAR(20), revolving_seat VARCHAR(20), seat_power VARCHAR(20), "
                            + "seat_memory_massager VARCHAR(20), camera VARCHAR(20), air_conditioning VARCHAR(20), "
                            + "air_pure_filter VARCHAR(20), navigation VARCHAR(20), tv VARCHAR(20), "
                            + "radio_cassette VARCHAR(20), cd VARCHAR(20), md VARCHAR(20), cd_changer VARCHAR(20), "
                            + "usb_link VARCHAR(20), rear_speakers VARCHAR(20), floor_mats VARCHAR(20), "
                            + "multifunction_steering_wheel VARCHAR(20), power_shutters VARCHAR(20), parking_sensor VARCHAR(20), "
                            + "bull_bars_grill_guard VARCHAR(20), rear_tyre_rack VARCHAR(20), rear_wipers VARCHAR(20), "
                            + "roof_rail VARCHAR(20), hood_roof_rack VARCHAR(20), body_kit_front VARCHAR(20), "
                            + "body_kit_side VARCHAR(20), body_kit_rear VARCHAR(20), alloy_wheels VARCHAR(20), "
                            + "radio_antenna VARCHAR(20), high_mount_stop_light VARCHAR(20), bumpers VARCHAR(20), "
                            + "door_visor VARCHAR(20), mud_guard_tyre_flaps VARCHAR(20), wheel_cover_hub_caps VARCHAR(20), "
                            + "side_sliding_windows VARCHAR(20), side_steps VARCHAR(20), roll_bars VARCHAR(20), "
                            + "solar_power VARCHAR(20), rear_protector_bar VARCHAR(20), rear_spare_tyre_cover VARCHAR(20), "
                            + "fog_lights VARCHAR(20), fender_mirror VARCHAR(20), power_mirror VARCHAR(20), "
                            + "winch VARCHAR(20), power_door VARCHAR(20), rear_cargo_bed_cover VARCHAR(20), "
                            + "canopy VARCHAR(20), truck_cab_type VARCHAR(20), truck_trailer_type VARCHAR(20), "
                            + "truck_bed_liner VARCHAR(20), truck_crane VARCHAR(20), truck_electric_curtain VARCHAR(20), "
                            + "truck_freezer_unit VARCHAR(20), drivers_airbag VARCHAR(20), passenger_airbag VARCHAR(20), "
                            + "abs_braking_system VARCHAR(20), handicap_options VARCHAR(20), tyre_wrench VARCHAR(20), "
                            + "jack VARCHAR(20), charging_cable VARCHAR(20), puncture_kit VARCHAR(20), "
                            + "ocr_text TEXT, "
                            + "document_original_name VARCHAR(255), "
                            + "document_stored_name VARCHAR(120), "
                            + "document_content_type VARCHAR(80), "
                            + "PRIMARY KEY (chassis_no)"
                            + ") ENGINE=InnoDB ROW_FORMAT=DYNAMIC"
            );
            log.info("Verified table equipment_inspections");
        } catch (Exception ex) {
            log.warn("Could not create equipment_inspections: {}", ex.getMessage());
        }
    }

    private void createClearanceAssessmentTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS clearance_assessment_notices ("
                            + "chassis_no VARCHAR(40) NOT NULL, "
                            + "assessment_office TEXT, "
                            + "assessment_notice_ref TEXT, "
                            + "assessment_model TEXT, "
                            + "assessment_customs_reference TEXT, "
                            + "assessment_declarant_reference TEXT, "
                            + "assessment_reference TEXT, "
                            + "assessment_packages TEXT, "
                            + "assessment_declarant_id TEXT, "
                            + "assessment_declarant_name TEXT, "
                            + "assessment_declarant_address TEXT, "
                            + "assessment_declarant_cha_exp TEXT, "
                            + "assessment_consignee_id TEXT, "
                            + "assessment_consignee_name TEXT, "
                            + "assessment_consignee_address TEXT, "
                            + "assessment_tax_otc TEXT, "
                            + "assessment_tax_com TEXT, "
                            + "assessment_tax_exm TEXT, "
                            + "assessment_tax_cid TEXT, "
                            + "assessment_tax_sur TEXT, "
                            + "assessment_tax_xid TEXT, "
                            + "assessment_tax_vat TEXT, "
                            + "assessment_tax_vel TEXT, "
                            + "assessment_total_assessed TEXT, "
                            + "assessment_total_paid TEXT, "
                            + "page3_original_name TEXT, "
                            + "page3_stored_name TEXT, "
                            + "page3_content_type TEXT, "
                            + "ocr_text_page3 TEXT, "
                            + "PRIMARY KEY (chassis_no)"
                            + ")"
            );
            log.info("Verified table clearance_assessment_notices");
        } catch (Exception ex) {
            log.warn("Could not create clearance_assessment_notices: {}", ex.getMessage());
        }
    }

    private void createPipelineFlowsTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS pipeline_flows ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT, "
                            + "flow_key VARCHAR(40) NOT NULL, "
                            + "title VARCHAR(80) NOT NULL, "
                            + "description VARCHAR(160), "
                            + "icon VARCHAR(40), "
                            + "sort_order INT NOT NULL, "
                            + "PRIMARY KEY (id), "
                            + "UNIQUE KEY uk_pipeline_flows_key (flow_key)"
                            + ")"
            );
            log.info("Verified table pipeline_flows");
        } catch (Exception ex) {
            log.warn("Could not create pipeline_flows: {}", ex.getMessage());
        }
    }

    private void seedPipelineFlows() {
        for (FlowPipeline flow : FlowPipeline.values()) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM pipeline_flows WHERE flow_key = ?",
                        Integer.class,
                        flow.getFlowKey()
                );
                if (count > 0) {
                    continue;
                }
                jdbcTemplate.update(
                        "INSERT INTO pipeline_flows (flow_key, title, description, icon, sort_order) VALUES (?, ?, ?, ?, ?)",
                        flow.getFlowKey(),
                        flow.getTitle(),
                        flow.getDescription(),
                        flow.getIcon(),
                        flow.getSortOrder()
                );
                log.info("Seeded pipeline_flows row {}", flow.getFlowKey());
            } catch (Exception ex) {
                log.warn("Could not seed pipeline_flows {}: {}", flow.getFlowKey(), ex.getMessage());
            }
        }
    }

    private void createPipelineStagesTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS pipeline_stages ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT, "
                            + "flow_id BIGINT NOT NULL, "
                            + "stage_key VARCHAR(40) NOT NULL, "
                            + "title VARCHAR(80) NOT NULL, "
                            + "subtitle VARCHAR(120), "
                            + "sort_order INT NOT NULL, "
                            + "PRIMARY KEY (id), "
                            + "UNIQUE KEY uk_pipeline_stages_flow_id_stage (flow_id, stage_key), "
                            + "CONSTRAINT fk_pipeline_stages_flow FOREIGN KEY (flow_id) REFERENCES pipeline_flows (id)"
                            + ")"
            );
            log.info("Verified table pipeline_stages");
        } catch (Exception ex) {
            log.warn("Could not create pipeline_stages: {}", ex.getMessage());
        }
    }

    private void migratePipelineStagesToFlowId() {
        if (hasColumn("flow_key")) {
            return;
        }
        try {
            if (hasColumn("flow_id")) {
                jdbcTemplate.execute("ALTER TABLE pipeline_stages ADD COLUMN flow_id BIGINT NULL");
            }
            jdbcTemplate.update(
                    "UPDATE pipeline_stages s INNER JOIN pipeline_flows f ON f.flow_key = s.flow_key "
                            + "SET s.flow_id = f.id WHERE s.flow_id IS NULL"
            );
            tryExecute("ALTER TABLE pipeline_stages DROP INDEX uk_pipeline_stages_flow_stage");
            tryExecute("ALTER TABLE pipeline_stages DROP COLUMN flow_key");
            tryExecute("ALTER TABLE pipeline_stages MODIFY COLUMN flow_id BIGINT NOT NULL");
            tryExecute("ALTER TABLE pipeline_stages ADD UNIQUE KEY uk_pipeline_stages_flow_id_stage (flow_id, stage_key)");
            ensurePipelineStagesFlowForeignKey();
            log.info("Migrated pipeline_stages.flow_key to flow_id");
        } catch (Exception ex) {
            log.warn("Could not migrate pipeline_stages to flow_id: {}", ex.getMessage());
        }
    }

    private void ensurePipelineStagesFlowForeignKey() {
        tryExecute(
                "ALTER TABLE pipeline_stages ADD CONSTRAINT fk_pipeline_stages_flow "
                        + "FOREIGN KEY (flow_id) REFERENCES pipeline_flows (id)"
        );
    }

    private boolean hasColumn(String column) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)",
                    Integer.class,
                    "pipeline_stages",
                    column
            );
            return count <= 0;
        } catch (Exception ex) {
            return true;
        }
    }

    private void tryExecute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ex) {
            log.debug("Skipped SQL [{}]: {}", sql, ex.getMessage());
        }
    }

    private void widenColumn() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE " + "vehicles" + " MODIFY COLUMN " + "model_year" + " " + "VARCHAR(40)"
            );
            log.info("Verified column {}.{} as {}", "vehicles", "model_year", "VARCHAR(40)");
        } catch (Exception ex) {
            log.warn("Could not alter {}.{} to {}: {}", "vehicles", "model_year", "VARCHAR(40)", ex.getMessage());
        }
    }
}
