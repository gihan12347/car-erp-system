package com.carsale.erp.preparationpipeline.repository;

import java.util.Collection;
import java.util.List;

import com.carsale.erp.preparationpipeline.model.RegistrationCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationCertificateRepository extends JpaRepository<RegistrationCertificate, String> {

    List<RegistrationCertificate> findByChassisNoIn(Collection<String> chassisNos);
}
