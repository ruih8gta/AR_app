package com.ruih8gta.palmsafari

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class SceneTest {
    @Test fun sizeAndInvalidInputsStayWithinSafeBounds() {
        val result = SceneSettings(99, size = Float.NaN, angle = Float.POSITIVE_INFINITY).normalized()
        assertEquals(1, result.model); assertEquals(1f, result.size); assertEquals(0f, result.angle)
        assertEquals(.5f, SceneSettings(size = -10f).normalized().size)
        assertEquals(1.5f, SceneSettings(size = 100f).normalized().size)
    }
    @Test fun pausePreservesTimeAndThePreviousPose() {
        val clock = MotionClock()
        clock.tick(0, Motion.STEP); clock.tick(40_000_000, Motion.STEP)
        clock.tick(80_000_000, Motion.PAUSE)
        assertEquals(.04f, clock.seconds, 1e-6f); assertEquals(Motion.STEP, clock.pose)
        clock.tick(120_000_000, Motion.IDLE)
        assertEquals(.08f, clock.seconds, 1e-6f); assertEquals(Motion.IDLE, clock.pose)
    }
    @Test fun resumeDoesNotIncludeTimeSpentInBackground() {
        val clock = MotionClock(); clock.tick(0, Motion.IDLE); clock.tick(20_000_000, Motion.IDLE)
        clock.suspendClock(); clock.tick(100_000_000_000, Motion.IDLE)
        assertEquals(.02f, clock.seconds, 1e-6f)
    }
    @Test fun feetNeverMoveBelowTheirRestingHeight() {
        for(i in 0..1000) for(joint in listOf("legL","legR","backL","backR")) {
            val pose = AnimationPose.joint(joint, i*.02f, Motion.STEP)
            assertTrue(pose.y >= 0); assertTrue(abs(pose.z) <= .061f)
        }
    }
    @Test fun generatedMeshesHaveFiniteUnitNormalsAndNoDegenerateFaces() {
        for(mesh in listOf(MeshGeometry.sphere(), MeshGeometry.cone(), MeshGeometry.ring())) {
            assertTrue(mesh.isNotEmpty()); assertEquals(0, mesh.size % 18)
            assertTrue(mesh.all { it.isFinite() })
            for(i in mesh.indices step 6) {
                val len = sqrt(mesh[i+3]*mesh[i+3]+mesh[i+4]*mesh[i+4]+mesh[i+5]*mesh[i+5])
                assertEquals(1f, len, 1e-5f)
            }
        }
    }
}
