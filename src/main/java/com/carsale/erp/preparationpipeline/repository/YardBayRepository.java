package com.carsale.erp.preparationpipeline.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import com.carsale.erp.preparationpipeline.model.YardBay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface YardBayRepository extends JpaRepository<YardBay, Long> {

    List<YardBay> findAllByOrderBySortOrderAscIdAsc();

    List<YardBay> findByActiveTrueOrderBySortOrderAscIdAsc();

    boolean existsByBayCodeIgnoreCase(String bayCode);

    Optional<YardBay> findByBayCodeIgnoreCase(String bayCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT y FROM YardBay y WHERE LOWER(y.bayCode) = LOWER(:code)")
    Optional<YardBay> lockByBayCodeIgnoreCase(@Param("code") String code);
}
