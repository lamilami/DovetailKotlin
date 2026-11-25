package bg.workshop.dovetail.geometry

/** Над тази хлабина (mm) сглобката се счита за забележимо хлабава. */
const val LOOSE_CLEARANCE = 0.3

data class FingerJoint(
    val pitch: Double,
    /** Ширина на гнездото: прорезът плюс разликата фрезер − лагер. */
    val socketWidth: Double,
    /** Ширина на зъба: стъпката минус гнездото. */
    val pinWidth: Double,
    /** Гнездо − зъб. Отрицателна = натегната, не влиза. */
    val clearance: Double,
    /** Отместване на втория детайл — половин стъпка. */
    val offset: Double,
    /** Колко излиза фрезерът под шаблона: дебелина + надстояване. */
    val depth: Double,
)

/**
 * Прав зъб с прав фрезер и копирен лагер. Лагерът следва стените на прореза в шаблона,
 * затова всяко гнездо е широко колкото прореза плюс (Ø фрезер − Ø лагер).
 * Двата детайла се режат с един и същ шаблон, отместени с половин стъпка.
 */
fun fingerJoint(
    finger: Double?,
    slot: Double?,
    cutterDiameter: Double?,
    bearingDiameter: Double?,
    thickness: Double?,
    cuttingLength: Double? = null,
    proud: Double? = null,
): Calc<FingerJoint> {
    if (finger == null || slot == null || cutterDiameter == null || bearingDiameter == null || thickness == null) {
        return needs(
            Field.FINGER to finger,
            Field.SLOT to slot,
            Field.BIT_DIAMETER to cutterDiameter,
            Field.BEARING_DIAMETER to bearingDiameter,
            Field.THICKNESS to thickness,
        )
    }
    if (bearingDiameter > slot + 1e-9) return Calc.Invalid(Reason.BEARING_WIDER_THAN_SLOT)
    val pitch = finger + slot
    val socket = slot + (cutterDiameter - bearingDiameter)
    val pin = pitch - socket
    if (socket <= 0 || pin <= 0) return Calc.Invalid(Reason.FINGER_GEOMETRY_IMPOSSIBLE)
    val clearance = socket - pin
    val depth = thickness + (proud ?: 0.0)
    val warnings = buildList {
        if (clearance < -1e-9) add(Warning.JOINT_INTERFERENCE)
        if (clearance > LOOSE_CLEARANCE) add(Warning.JOINT_LOOSE)
        if (cuttingLength != null && cuttingLength < depth) add(Warning.CUTTER_TOO_SHORT)
    }
    return Calc.Ok(FingerJoint(pitch, socket, pin, clearance, pitch / 2, depth), warnings)
}
