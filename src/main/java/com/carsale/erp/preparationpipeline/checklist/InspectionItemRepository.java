package com.carsale.erp.preparationpipeline.checklist;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionItemRepository extends JpaRepository<InspectionItem, Long> {

    List<InspectionItem> findAllByOrderBySortOrderAscIdAsc();

    List<InspectionItem> findByActiveTrueOrderBySortOrderAscIdAsc();

    boolean existsByItemKey(String itemKey);
}
