package com.carsale.erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.entity.ClearanceDocument;

public interface ClearanceDocumentRepository extends JpaRepository<ClearanceDocument, String> {
}
