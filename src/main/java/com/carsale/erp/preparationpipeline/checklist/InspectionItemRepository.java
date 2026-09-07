package com.carsale.erp.preparationpipeline.checklist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.preparationpipeline.checklist.InspectionItem;

public interface InspectionItemRepository extends JpaRepository<InspectionItem, Long> {

    List<InspectionItem> findAllByOrderBySortOrderAscIdAsc();

    List<InspectionItem> findByActiveTrueOrderBySortOrderAscIdAsc();

    Optional<InspectionItem> findByItemKey(String itemKey);

    boolean existsByItemKey(String itemKey);
}
