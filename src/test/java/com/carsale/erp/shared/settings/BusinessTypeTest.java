package com.carsale.erp.shared.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.carsale.erp.shared.pipeline.FlowPipeline;

class BusinessTypeTest {

    @Test
    void registeredKeepsOnlyTheLocalPurchasePipeline() {
        BusinessType type = BusinessType.REGISTERED;

        assertTrue(type.isLocalPurchaseEnabled());
        assertFalse(type.isUnregisteredPipelinesEnabled());
        assertFalse(type.isFlowEnabled(FlowPipeline.IMPORT.getFlowKey()));
        assertFalse(type.isFlowEnabled(FlowPipeline.CUSTOMS.getFlowKey()));
        assertTrue(type.isFlowEnabled(FlowPipeline.PREP.getFlowKey()));
        assertTrue(type.isFlowEnabled(FlowPipeline.READY.getFlowKey()));
    }

    @Test
    void unregisteredKeepsEveryUnregisteredPipeline() {
        BusinessType type = BusinessType.UNREGISTERED;

        assertFalse(type.isLocalPurchaseEnabled());
        assertTrue(type.isUnregisteredPipelinesEnabled());
        assertTrue(type.isFlowEnabled(FlowPipeline.IMPORT.getFlowKey()));
        assertTrue(type.isFlowEnabled(FlowPipeline.CUSTOMS.getFlowKey()));
        assertTrue(type.isFlowEnabled(FlowPipeline.PREP.getFlowKey()));
    }

    @Test
    void bothKeepsEveryPipeline() {
        BusinessType type = BusinessType.BOTH;

        assertTrue(type.isLocalPurchaseEnabled());
        assertTrue(type.isUnregisteredPipelinesEnabled());
        assertTrue(type.isFlowEnabled(FlowPipeline.IMPORT.getFlowKey()));
        assertTrue(type.isFlowEnabled(FlowPipeline.CUSTOMS.getFlowKey()));
    }

    @Test
    void storedCodesRoundTrip() {
        assertEquals(BusinessType.REGISTERED, BusinessType.parse(" registered "));
        assertEquals(BusinessType.UNREGISTERED, BusinessType.parse("unregistered"));
        assertEquals(BusinessType.BOTH, BusinessType.parse("both"));
        assertThrows(IllegalArgumentException.class, () -> BusinessType.parse("dealer"));
    }
}
