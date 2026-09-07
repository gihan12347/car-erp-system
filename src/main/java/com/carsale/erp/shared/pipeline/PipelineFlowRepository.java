package com.carsale.erp.shared.pipeline;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carsale.erp.shared.pipeline.PipelineFlow;

public interface PipelineFlowRepository extends JpaRepository<PipelineFlow, Long> {

    Optional<PipelineFlow> findByFlowKey(String flowKey);

    @Query("SELECT f FROM PipelineFlow f ORDER BY f.sortOrder ASC")
    List<PipelineFlow> findAllOrdered();

    @Query("SELECT DISTINCT f FROM PipelineFlow f "
            + "LEFT JOIN FETCH f.stages s "
            + "WHERE f.id = :flowId")
    Optional<PipelineFlow> findByIdWithStages(@Param("flowId") Long flowId);

}
