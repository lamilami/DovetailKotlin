package bg.workshop.dovetail.geometry

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.tan

private const val EPS = 1e-6

// ─── Центриране и ширина ────────────────────────────────────────────────

data class Centering(
    val center: Double,
    val pitch: Double,
    /** Разстояние от ръба на дъската до най-външния център на пръст върху нея. */
    val edgeToFingerCenter: Double,
    /** Брой центрове на пръсти върху ширината (включително централния). */
    val fingersOnBoard: Int,
    /** Ширината е точно кратна на половин стъпка → изцяло оформени опашки. */
    val fullyFormed: Boolean,
    /** Най-близките ширини, кратни на половин стъпка (≤ и ≥ текущата). */
    val nearestFullWidths: Pair<Double, Double>,
)

fun centering(boardWidth: Double?, finger: Double?, slot: Double?): Calc<Centering> {
    if (boardWidth == null || finger == null || slot == null) {
        return needs(Field.BOARD_WIDTH to boardWidth, Field.FINGER to finger, Field.SLOT to slot)
    }
    val pitch = finger + slot
    val half = boardWidth / 2
    val n = floor(half / pitch + EPS)
    val step = pitch / 2
    val k = boardWidth / step
    val fullyFormed = abs(k - round(k)) < EPS
    val lower = floor(k + EPS) * step
    val upper = ceil(k - EPS) * step
    val result = Centering(
        center = half,
        pitch = pitch,
        edgeToFingerCenter = (half - n * pitch).coerceAtLeast(0.0),
        fingersOnBoard = 2 * n.toInt() + 1,
        fullyFormed = fullyFormed,
        nearestFullWidths = lower to upper,
    )
    return Calc.Ok(result, if (fullyFormed) emptyList() else listOf(Warning.WIDTH_NOT_FULLY_FORMED))
}

// ─── Заден ограничител ──────────────────────────────────────────────────

data class BackStop(
    /** Разстояние от върховете на пръстите на шаблона до ограничителя. */
    val distance: Double,
    /** От центъра на фрезера до опорната страна на основата. */
    val baseReference: Double,
)

/** Формулата от ръководството: R + 2t − d/2 (R се заменя с разстоянието до плоската страна). */
fun backStop(
    baseDiameter: Double?,
    centerToFlat: Double?,
    flatToStop: Boolean,
    thickness: Double?,
    bitDiameter: Double?,
): Calc<BackStop> {
    if (baseDiameter == null || thickness == null || bitDiameter == null || (flatToStop && centerToFlat == null)) {
        val inputs = buildList {
            add(Field.BASE_DIAMETER to baseDiameter)
            if (flatToStop) add(Field.CENTER_TO_FLAT to centerToFlat)
            add(Field.THICKNESS to thickness)
            add(Field.BIT_DIAMETER to bitDiameter)
        }
        return needs(*inputs.toTypedArray())
    }
    val radius = baseDiameter / 2
    val reference = if (flatToStop) centerToFlat!! else radius
    if (reference > radius) return Calc.Invalid(Reason.CHORD_EXCEEDS_RADIUS)
    val distance = reference + 2 * thickness - bitDiameter / 2
    if (distance <= 0) return Calc.Invalid(Reason.STOP_NOT_POSITIVE)
    return Calc.Ok(BackStop(distance, reference))
}

// ─── Дълбочина и стена ──────────────────────────────────────────────────

data class Wall(val remaining: Double)

fun wall(thickness: Double?, depth: Double?, minWall: Double?): Calc<Wall> {
    if (thickness == null || depth == null || minWall == null) {
        return needs(Field.THICKNESS to thickness, Field.DEPTH to depth, Field.MIN_WALL to minWall)
    }
    val remaining = thickness - depth
    if (remaining <= 0) return Calc.Invalid(Reason.GROOVE_THROUGH_BOARD)
    val warnings = buildList {
        if (remaining < minWall) add(Warning.WALL_BELOW_MINIMUM)
        if (depth < JigSpec.START_DEPTH_MIN || depth > JigSpec.START_DEPTH_MAX) add(Warning.DEPTH_OUTSIDE_RECOMMENDED)
    }
    return Calc.Ok(Wall(remaining), warnings)
}

// ─── Профил на фрезера ──────────────────────────────────────────────────

data class BitProfile(
    val maxDiameter: Double,
    /** Тесният диаметър на идеалния конус, на височина h над върха. */
    val narrowDiameter: Double,
    val cuttingHeight: Double,
    val angleDeg: Double,
    /** Диаметър на дълбочината на рязане над върха; null ако дълбочината е над режещата част. */
    val diameterAtDepth: Double?,
) {
    fun diameterAt(heightAboveTip: Double): Double =
        maxDiameter - 2 * heightAboveTip * tan(Math.toRadians(angleDeg))
}

/** Ъгълът е спрямо оста, а не пълният ъгъл на конуса. */
fun bitProfile(maxDiameter: Double?, angleDeg: Double?, cuttingHeight: Double?, depth: Double?): Calc<BitProfile> {
    if (maxDiameter == null || angleDeg == null || cuttingHeight == null) {
        return needs(Field.ANGLE to angleDeg, Field.CUTTING_HEIGHT to cuttingHeight, Field.BIT_DIAMETER to maxDiameter)
    }
    if (angleDeg >= 90) return Calc.Invalid(Reason.ANGLE_NOT_BELOW_90)
    val slope = 2 * tan(Math.toRadians(angleDeg))
    val narrow = maxDiameter - cuttingHeight * slope
    if (narrow <= 0) return Calc.Invalid(Reason.BIT_GEOMETRY_IMPOSSIBLE)
    val atDepth = depth?.takeIf { it <= cuttingHeight }?.let { maxDiameter - it * slope }
    val warnings = if (depth != null && depth > cuttingHeight) listOf(Warning.DEPTH_EXCEEDS_CUTTING_HEIGHT) else emptyList()
    return Calc.Ok(BitProfile(maxDiameter, narrow, cuttingHeight, angleDeg, atDepth), warnings)
}
