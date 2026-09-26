package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-JVM tests for the on-device Qibla bearing maths. */
class QiblaEngineTest {
  @Test
  fun relativeQiblaAngle_calculation() {
    // Exact match
    val direct = QiblaEngine.calculateRelativeAngle(110f, 110f)
    assertEquals(0f, direct, 0.01f)

    // Target is to the right (heading 90, target 120 -> +30)
    val rightTurn = QiblaEngine.calculateRelativeAngle(90f, 120f)
    assertEquals(30f, rightTurn, 0.01f)

    // Target is to the left (heading 120, target 90 -> -30)
    val leftTurn = QiblaEngine.calculateRelativeAngle(120f, 90f)
    assertEquals(-30f, leftTurn, 0.01f)

    // Wrapping across 0/360: heading 350, target 10 -> +20
    val wrapRight = QiblaEngine.calculateRelativeAngle(350f, 10f)
    assertEquals(20f, wrapRight, 0.01f)

    // Heading 10, target 350 -> -20
    val wrapLeft = QiblaEngine.calculateRelativeAngle(10f, 350f)
    assertEquals(-20f, wrapLeft, 0.01f)
  }

  @Test
  fun magneticField_evaluation() {
    assertEquals(MagneticFieldStatus.OPTIMAL, QiblaEngine.evaluateMagneticField(45f))
    assertEquals(MagneticFieldStatus.WEAK, QiblaEngine.evaluateMagneticField(15f))
    assertEquals(MagneticFieldStatus.INTERFERENCE, QiblaEngine.evaluateMagneticField(95f))
  }
}
