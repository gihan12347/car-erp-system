package com.carsale.erp.shared.pipeline;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carsale.erp.shared.pipeline.PipelineStage;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {

    @Query("SELECT s FROM PipelineStage s "
            + "JOIN s.flow f "
            + "WHERE f.id = :flowId "
            + "ORDER BY s.sortOrder ASC")
    List<PipelineStage> findByFlowIdOrdered(@Param("flowId") Long flowId);

    @Query("SELECT s FROM PipelineStage s "
            + "JOIN s.flow f "
            + "WHERE f.id = :flowId AND s.stageKey = :stageKey")
    Optional<PipelineStage> findByFlowIdAndStageKey(@Param("flowId") Long flowId,
                                                    @Param("stageKey") String stageKey);

    @Query("SELECT COUNT(s) FROM PipelineStage s JOIN s.flow f WHERE f.id = :flowId")
    long countByFlowId(@Param("flowId") Long flowId);
}
