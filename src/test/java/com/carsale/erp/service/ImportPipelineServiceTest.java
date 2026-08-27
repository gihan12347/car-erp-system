package com.carsale.erp.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.service.ImportPipelineService.ImportStatus;

class ImportPipelineServiceTest {

    @Test
    void importIsCompleteWhenAuctionPreshipmentAndEquipmentAreDone() {
        ImportStatus pending = new ImportStatus(false, false, false);
        ImportStatus auctionOnly = new ImportStatus(true, false, false);
        ImportStatus twoOfThree = new ImportStatus(true, true, false);
        ImportStatus complete = new ImportStatus(true, true, true);

        assertThat(pending.completedCount()).isEqualTo(0);
        assertThat(pending.isImportComplete()).isFalse();
        assertThat(auctionOnly.completedCount()).isEqualTo(1);
        assertThat(auctionOnly.isImportComplete()).isFalse();
        assertThat(twoOfThree.completedCount()).isEqualTo(2);
        assertThat(twoOfThree.isImportComplete()).isFalse();
        assertThat(complete.completedCount()).isEqualTo(3);
        assertThat(complete.isImportComplete()).isTrue();
        assertThat(complete.isAnyReady()).isTrue();
    }
}
