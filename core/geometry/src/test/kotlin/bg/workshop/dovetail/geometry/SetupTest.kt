package bg.workshop.dovetail.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupTest {
    private val tol = 1e-3

    private fun <T> ok(c: Calc<T>): Calc.Ok<T> {
        assertTrue("Очаквах Ok, получих $c", c is Calc.Ok)
        return c as Calc.Ok<T>
    }

    // ─── parseMeasure ───

    @Test fun parsesCommaAndDot() {
        assertEquals(12.7, parseMeasure("12,7")!!, 0.0)
        assertEquals(12.7, parseMeasure("12.7")!!, 0.0)
        assertEquals(3.0, parseMeasure(" 3 ")!!, 0.0)
    }

    @Test fun rejectsInvalidMeasures() {
        listOf("", "abc", "0", "-1", "10001", "NaN", "Infinity").forEach { assertNull(it, parseMeasure(it)) }
    }

    // ─── centering ───

    @Test fun centeringDefaults() {
        val c = ok(centering(180.0, 12.7, 12.7))
        assertEquals(90.0, c.value.center, tol)
        assertEquals(25.4, c.value.pitch, tol)
        assertEquals(13.8, c.value.edgeToFingerCenter, tol)
        assertEquals(7, c.value.fingersOnBoard)
    }

    @Test fun widthNotMultipleOfHalfInchWarns() {
        val c = ok(centering(180.0, 12.7, 12.7))
        assertTrue(Warning.WIDTH_NOT_FULLY_FORMED in c.warnings)
        assertEquals(177.8, c.value.nearestFullWidths.first, tol)
        assertEquals(190.5, c.value.nearestFullWidths.second, tol)
    }

    @Test fun widthMultipleOfHalfInchIsFullyFormed() {
        val c = ok(centering(177.8, 12.7, 12.7))
        assertTrue(c.value.fullyFormed)
        assertTrue(c.warnings.isEmpty())
        assertEquals(177.8, c.value.nearestFullWidths.first, tol)
        assertEquals(177.8, c.value.nearestFullWidths.second, tol)
    }

    @Test fun centeringNeedsInput() {
        assertEquals(Calc.NeedsInput(setOf(Field.FINGER)), centering(180.0, null, 12.7))
    }

    // ─── backStop ───

    @Test fun backStopMatchesManualExample() {
        // Ръководство: основа 160 mm, страни 18 mm, връх 14,8 mm → 108,6 mm
        assertEquals(108.6, ok(backStop(160.0, null, false, 18.0, 14.8)).value.distance, tol)
    }

    @Test fun backStopWithFlatSide() {
        assertEquals(98.325, ok(backStop(160.0, 70.0, true, 18.0, 15.35)).value.distance, tol)
    }

    @Test fun backStopIgnoresChordWhenRound() {
        assertEquals(108.325, ok(backStop(160.0, null, false, 18.0, 15.35)).value.distance, tol)
    }

    @Test fun backStopFlatNeedsChord() {
        assertEquals(Calc.NeedsInput(setOf(Field.CENTER_TO_FLAT)), backStop(160.0, null, true, 18.0, 15.35))
    }

    @Test fun chordLargerThanRadiusIsInvalid() {
        assertEquals(Calc.Invalid(Reason.CHORD_EXCEEDS_RADIUS), backStop(160.0, 81.0, true, 18.0, 15.35))
    }

    @Test fun nonPositiveStopIsInvalid() {
        assertEquals(Calc.Invalid(Reason.STOP_NOT_POSITIVE), backStop(2.0, null, false, 0.1, 15.35))
    }

    // ─── wall ───

    @Test fun wallWithinLimits() {
        val w = ok(wall(18.0, 10.0, 3.0))
        assertEquals(8.0, w.value.remaining, tol)
        assertTrue(w.warnings.isEmpty())
    }

    @Test fun thinWallWarns() {
        val w = ok(wall(18.0, 16.0, 3.0))
        assertTrue(Warning.WALL_BELOW_MINIMUM in w.warnings)
        assertTrue(Warning.DEPTH_OUTSIDE_RECOMMENDED in w.warnings)
    }

    @Test fun grooveThroughBoardIsInvalid() {
        assertEquals(Calc.Invalid(Reason.GROOVE_THROUGH_BOARD), wall(18.0, 18.0, 3.0))
    }

    // ─── bitProfile ───

    @Test fun bitProfileGeometry() {
        val b = ok(bitProfile(15.35, 14.0, 12.7, 10.0))
        assertEquals(9.01707, b.value.narrowDiameter, tol)
        assertEquals(10.36344, b.value.diameterAtDepth!!, tol)
        assertEquals(b.value.narrowDiameter, b.value.diameterAt(12.7), 1e-9)
    }

    @Test fun angleMustBeBelow90() {
        assertEquals(Calc.Invalid(Reason.ANGLE_NOT_BELOW_90), bitProfile(15.35, 90.0, 12.7, 10.0))
    }

    @Test fun impossibleBitIsInvalid() {
        assertEquals(Calc.Invalid(Reason.BIT_GEOMETRY_IMPOSSIBLE), bitProfile(15.35, 60.0, 12.7, 10.0))
    }

    @Test fun depthAboveCuttingHeightWarns() {
        val b = ok(bitProfile(15.35, 14.0, 12.7, 15.0))
        assertNull(b.value.diameterAtDepth)
        assertTrue(Warning.DEPTH_EXCEEDS_CUTTING_HEIGHT in b.warnings)
    }

    @Test fun bitNeedsAngleAndHeight() {
        assertEquals(
            Calc.NeedsInput(setOf(Field.ANGLE, Field.CUTTING_HEIGHT)),
            bitProfile(15.35, null, null, 10.0),
        )
    }
}
