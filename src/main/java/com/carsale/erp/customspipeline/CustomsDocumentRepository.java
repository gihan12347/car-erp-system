package com.carsale.erp.customspipeline;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.customspipeline.CustomsDocument;

public interface CustomsDocumentRepository extends JpaRepository<CustomsDocument, String> {
}
