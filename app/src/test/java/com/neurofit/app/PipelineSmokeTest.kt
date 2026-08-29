package com.neurofit.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 1 exists to prove the pipeline, so this proves the test task actually
 * runs and can fail the build. Real calculation tests arrive in Phase 3.
 */
class PipelineSmokeTest {

    @Test
    fun `test task executes`() {
        assertTrue(true)
    }

    @Test
    fun `energy per kilogram constant is correct`() {
        // 7700 kcal per kg of body mass, used by the target calculator in Phase 3.
        val kcalPerKg = 7700.0
        val weeklyDeficitForHalfKg = kcalPerKg * 0.5
        assertEquals(3850.0, weeklyDeficitForHalfKg, 0.0001)
    }
}
