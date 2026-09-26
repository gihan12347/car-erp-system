package com.carsale.erp.importpipeline.repository;

import com.carsale.erp.importpipeline.model.InspectionCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionCertificateRepository extends JpaRepository<InspectionCertificate, String> {
}
