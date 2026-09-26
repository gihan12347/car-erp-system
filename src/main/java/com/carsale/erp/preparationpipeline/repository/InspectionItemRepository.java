package com.carsale.erp.preparationpipeline.repository;

import java.util.List;

import com.carsale.erp.preparationpipeline.model.InspectionItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionItemRepository extends JpaRepository<InspectionItem, Long> {

    List<InspectionItem> findAllByOrderBySortOrderAscIdAsc();

    List<InspectionItem> findByActiveTrueOrderBySortOrderAscIdAsc();

    boolean existsByItemKey(String itemKey);
}
