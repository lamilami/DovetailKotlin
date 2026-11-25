package bg.workshop.dovetail.geometry

/**
 * Резултат от изчисление. UI-ят решава как да го покаже;
 * геометрията никога не връща готов текст.
 */
sealed interface Calc<out T> {
    data class Ok<out T>(val value: T, val warnings: List<Warning> = emptyList()) : Calc<T>
    data class Invalid(val reason: Reason) : Calc<Nothing>
    data class NeedsInput(val fields: Set<Field>) : Calc<Nothing>
}

/** Защо изчислението е невъзможно. */
enum class Reason {
    CHORD_EXCEEDS_RADIUS,
    STOP_NOT_POSITIVE,
    GROOVE_THROUGH_BOARD,
    ANGLE_NOT_BELOW_90,
    BIT_GEOMETRY_IMPOSSIBLE,
    BEARING_WIDER_THAN_SLOT,
    FINGER_GEOMETRY_IMPOSSIBLE,
}

/** Резултатът е валиден, но изисква внимание. */
enum class Warning {
    WALL_BELOW_MINIMUM,
    DEPTH_EXCEEDS_CUTTING_HEIGHT,
    WIDTH_NOT_FULLY_FORMED,
    DEPTH_OUTSIDE_RECOMMENDED,
    JOINT_INTERFERENCE,
    JOINT_LOOSE,
    CUTTER_TOO_SHORT,
}

fun <T> Calc<T>.valueOrNull(): T? = (this as? Calc.Ok)?.value

internal fun needs(vararg inputs: Pair<Field, Double?>): Calc.NeedsInput =
    Calc.NeedsInput(inputs.filter { it.second == null }.map { it.first }.toSet())
