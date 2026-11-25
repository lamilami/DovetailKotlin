package bg.workshop.dovetail.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FingerJointTest {
    private val tol = 1e-6

    private fun ok(c: Calc<FingerJoint>): Calc.Ok<FingerJoint> {
        assertTrue("Очаквах Ok, получих $c", c is Calc.Ok)
        return c as Calc.Ok<FingerJoint>
    }

    @Test fun equalCutterAndBearingGiveExactJoint() {
        val j = ok(fingerJoint(12.7, 12.7, 12.7, 12.7, 18.0))
        assertEquals(25.4, j.value.pitch, tol)
        assertEquals(12.7, j.value.socketWidth, tol)
        assertEquals(12.7, j.value.pinWidth, tol)
        assertEquals(0.0, j.value.clearance, tol)
        assertEquals(12.7, j.value.offset, tol)
        assertEquals(18.0, j.value.depth, tol)
        assertTrue(j.warnings.isEmpty())
    }

    @Test fun clearanceIsTwiceDiameterDifference() {
        // фрезер 0,1 mm по-голям от лагера → гнездо +0,1, зъб −0,1, хлабина 0,2
        val j = ok(fingerJoint(12.7, 12.7, 12.8, 12.7, 18.0))
        assertEquals(12.8, j.value.socketWidth, tol)
        assertEquals(12.6, j.value.pinWidth, tol)
        assertEquals(0.2, j.value.clearance, tol)
        assertTrue(j.warnings.isEmpty())
    }

    @Test fun smallerCutterInterferes() {
        val j = ok(fingerJoint(12.7, 12.7, 12.6, 12.7, 18.0))
        assertTrue(Warning.JOINT_INTERFERENCE in j.warnings)
    }

    @Test fun largeClearanceIsLoose() {
        val j = ok(fingerJoint(12.7, 12.7, 12.7, 12.5, 18.0))
        assertEquals(0.4, j.value.clearance, tol)
        assertTrue(Warning.JOINT_LOOSE in j.warnings)
    }

    @Test fun proudAddsToDepth() {
        assertEquals(18.5, ok(fingerJoint(12.7, 12.7, 12.7, 12.7, 18.0, proud = 0.5)).value.depth, tol)
    }

    @Test fun shortCutterWarns() {
        val j = ok(fingerJoint(12.7, 12.7, 12.7, 12.7, 18.0, cuttingLength = 12.0))
        assertTrue(Warning.CUTTER_TOO_SHORT in j.warnings)
    }

    @Test fun bearingWiderThanSlotIsInvalid() {
        assertEquals(Calc.Invalid(Reason.BEARING_WIDER_THAN_SLOT), fingerJoint(12.7, 12.7, 16.0, 15.0, 18.0))
    }

    @Test fun oversizedCutterIsInvalid() {
        assertEquals(Calc.Invalid(Reason.FINGER_GEOMETRY_IMPOSSIBLE), fingerJoint(12.7, 12.7, 26.0, 12.7, 18.0))
    }

    @Test fun needsBearing() {
        assertEquals(Calc.NeedsInput(setOf(Field.BEARING_DIAMETER)), fingerJoint(12.7, 12.7, 12.7, null, 18.0))
    }
}
