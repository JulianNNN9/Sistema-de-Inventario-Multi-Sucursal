package com.optiplant.inventario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lightweight sanity check for Fase 0.A. Full Spring context / integration tests
 * that require a live PostgreSQL instance are introduced per-module in Fase E.
 */
class SanityTest {

    @Test
    void buildToolchainWorks() {
        assertTrue(Runtime.version().feature() >= 21, "Java 21+ required");
    }
}
