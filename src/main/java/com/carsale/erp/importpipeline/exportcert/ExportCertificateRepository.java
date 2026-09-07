package com.carsale.erp.importpipeline.exportcert;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carsale.erp.importpipeline.exportcert.ExportCertificate;

public interface ExportCertificateRepository extends JpaRepository<ExportCertificate, String> {
}
