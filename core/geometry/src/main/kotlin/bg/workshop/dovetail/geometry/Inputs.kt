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
 * вертикалният стопер е изместен на половин стъпка от хоризонталния
 * (½" при шаблон 1/2", 7/16" при 7/16", 9/16" при 9/16"),
 * върховете на пръстите са 3 mm навътре от вертикалния детайл,
 * фрезерът започва с 10–11 mm под пръстите.
 */
object JigSpec {
    /** Изместване на вертикалния стопер = половин стъпка (пръст + прорез) / 2. */
    fun stopOffset(finger: Double?, slot: Double?): Double? =
        if (finger == null || slot == null) null else (finger + slot) / 2
    const val TEMPLATE_SETBACK = 3.0
    const val START_DEPTH_MIN = 10.0
    const val START_DEPTH_MAX = 11.0
}

/**
 * Размер в mm като дроб в инчове до 1/16", например 11,1125 → "7/16\"".
 * Връща null, ако размерът не е кратен на 1/16" (допуск 0,02 mm).
 */
fun inchFraction(mm: Double): String? {
    val sixteenths = mm / 25.4 * 16
    val n = kotlin.math.round(sixteenths).toInt()
    if (n <= 0 || kotlin.math.abs(sixteenths - n) * 25.4 / 16 > 0.02) return null
    val whole = n / 16
    var num = n % 16
    var den = 16
    while (num != 0 && num % 2 == 0) { num /= 2; den /= 2 }
    return when {
        num == 0 -> "$whole\""
        whole == 0 -> "$num/$den\""
        else -> "$whole $num/$den\""
    }
}
