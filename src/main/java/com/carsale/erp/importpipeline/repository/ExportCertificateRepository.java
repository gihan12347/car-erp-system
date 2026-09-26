package com.carsale.erp.importpipeline.repository;

import com.carsale.erp.importpipeline.model.ExportCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportCertificateRepository extends JpaRepository<ExportCertificate, String> {
}
