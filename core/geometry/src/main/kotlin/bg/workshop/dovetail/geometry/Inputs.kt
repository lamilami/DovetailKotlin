package bg.workshop.dovetail.geometry

/** Всички числови входове на настройката. */
enum class Field {
    FINGER,
    SLOT,
    BOARD_WIDTH,
    THICKNESS,
    BASE_DIAMETER,
    CENTER_TO_FLAT,
    BIT_DIAMETER,
    DEPTH,
    MIN_WALL,
    ANGLE,
    CUTTING_HEIGHT,
    BEARING_DIAMETER,
    /** Надстояване на зъбите над съседния детайл (за изравняване). Празно = 0. */
    PROUD,
}

/** Вид сглобка, която шаблонът реже. */
enum class JointType {
    /** Полускрита лястовича опашка — фрезер „лястовича опашка“. */
    DOVETAIL,
    /** Прав зъб (finger joint) — прав фрезер с копирен лагер. */
    FINGER,
}

const val MAX_MEASURE = 10_000.0

/** Приема "12,7" и "12.7". Връща null за празно, нечислово, ≤ 0 или > 10 000. */
fun parseMeasure(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 && it <= MAX_MEASURE }

/** За незадължителни полета: празно или 0 → 0.0; невалидно → null. */
fun parseOptionalMeasure(text: String): Double? {
    val t = text.trim().replace(',', '.')
    if (t.isEmpty()) return 0.0
    val v = t.toDoubleOrNull() ?: return null
    return v.takeIf { it.isFinite() && it >= 0 && it <= MAX_MEASURE }
}

/**
 * Фиксираните настройки на шаблона по ръководството:
 * вертикалният стопер е изместен на ½" от хоризонталния,
 * върховете на пръстите са 3 mm навътре от вертикалния детайл,
 * фрезерът започва с 10–11 mm под пръстите.
 */
object JigSpec {
    const val STOP_OFFSET = 12.7
    const val TEMPLATE_SETBACK = 3.0
    const val START_DEPTH_MIN = 10.0
    const val START_DEPTH_MAX = 11.0
}
