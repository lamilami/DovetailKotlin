package bg.workshop.dovetail.data

import bg.workshop.dovetail.geometry.Field
import bg.workshop.dovetail.geometry.JointType
import bg.workshop.dovetail.geometry.parseMeasure
import bg.workshop.dovetail.model.Bit
import bg.workshop.dovetail.model.BitKind
import bg.workshop.dovetail.model.Jig
import bg.workshop.dovetail.model.Library
import bg.workshop.dovetail.model.Project
import bg.workshop.dovetail.model.RouterBase
import bg.workshop.dovetail.model.bitKind
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/** Число → текст за поле: до 3 знака, с десетична запетая. */
fun Double.asInput(): String =
    String.format(Locale.US, "%.3f", this).trimEnd('0').trimEnd('.').replace('.', ',')

private fun Double?.asOptionalInput(): String = this?.asInput() ?: ""

// ─── Инструмент → стойности в полетата ──────────────────────────────────

fun Jig.inputs(): Map<Field, String> = mapOf(
    Field.FINGER to finger.asInput(),
    Field.SLOT to slot.asInput(),
)

fun Bit.inputs(): Map<Field, String> = buildMap {
    put(Field.BIT_DIAMETER, diameter.asInput())
    put(Field.CUTTING_HEIGHT, cuttingHeight.asOptionalInput())
    when (kind) {
        BitKind.DOVETAIL -> put(Field.ANGLE, angleDeg.asOptionalInput())
        BitKind.STRAIGHT_BEARING -> put(Field.BEARING_DIAMETER, bearingDiameter.asOptionalInput())
    }
}

fun RouterBase.inputs(): Map<Field, String> = mapOf(
    Field.BASE_DIAMETER to diameter.asInput(),
    Field.CENTER_TO_FLAT to centerToFlat.asOptionalInput(),
)

/** Инструментът е „избран“, ако стойностите му съвпадат с текущите полета — без пазене на ID. */
fun Map<Field, String>.matches(preset: Map<Field, String>): Boolean = preset.all { (field, value) ->
    val a = parseMeasure(value)
    val b = parseMeasure(this[field].orEmpty())
    when {
        a == null && b == null -> true
        a != null && b != null -> abs(a - b) < 1e-6
        else -> false
    }
}

// ─── Текущи полета → нов инструмент ─────────────────────────────────────

fun newId(): String = UUID.randomUUID().toString()

fun jigFrom(name: String, v: (Field) -> Double?): Jig? {
    val finger = v(Field.FINGER) ?: return null
    val slot = v(Field.SLOT) ?: return null
    return Jig(newId(), name, finger, slot)
}

fun bitFrom(name: String, joint: JointType, v: (Field) -> Double?): Bit? {
    val d = v(Field.BIT_DIAMETER) ?: return null
    return when (joint.bitKind) {
        BitKind.DOVETAIL -> Bit(newId(), name, BitKind.DOVETAIL, d, angleDeg = v(Field.ANGLE), cuttingHeight = v(Field.CUTTING_HEIGHT))
        BitKind.STRAIGHT_BEARING -> Bit(
            newId(), name, BitKind.STRAIGHT_BEARING, d,
            cuttingHeight = v(Field.CUTTING_HEIGHT),
            bearingDiameter = v(Field.BEARING_DIAMETER) ?: return null,
        )
    }
}

fun baseFrom(name: String, flat: Boolean, v: (Field) -> Double?): RouterBase? {
    val d = v(Field.BASE_DIAMETER) ?: return null
    return RouterBase(newId(), name, d, centerToFlat = if (flat) v(Field.CENTER_TO_FLAT) else null)
}

// ─── Нов проект ─────────────────────────────────────────────────────────

private val commonDefaults = mapOf(
    Field.FINGER to "12,7",
    Field.SLOT to "12,7",
    Field.BOARD_WIDTH to "180",
    Field.THICKNESS to "18",
)

private val dovetailDefaults = commonDefaults + mapOf(
    Field.FRONT_THICKNESS to "18",
    Field.BASE_DIAMETER to "160",
    Field.CENTER_TO_FLAT to "70",
    Field.BIT_DIAMETER to "15,35",
    Field.DEPTH to "10",
    Field.MIN_WALL to "3",
    Field.ANGLE to "",
    Field.CUTTING_HEIGHT to "",
)

private val fingerDefaults = commonDefaults + mapOf(
    Field.BIT_DIAMETER to "12,7",
    Field.BEARING_DIAMETER to "12,7",
    Field.CUTTING_HEIGHT to "",
    Field.PROUD to "",
)

val JointType.title: String
    get() = when (this) {
        JointType.DOVETAIL -> "Лястовича опашка"
        JointType.FINGER -> "Прав зъб"
    }

/** Стойностите по подразбиране се допълват от първия подходящ инструмент в библиотеката. */
fun newProject(library: Library, joint: JointType, now: Long): Project {
    val inputs = buildMap {
        putAll(if (joint == JointType.DOVETAIL) dovetailDefaults else fingerDefaults)
        library.jigs.firstOrNull()?.let { putAll(it.inputs()) }
        library.bitsFor(joint).firstOrNull()?.let { putAll(it.inputs()) }
        if (joint == JointType.DOVETAIL) library.bases.firstOrNull()?.let { putAll(it.inputs()) }
    }
    val number = library.projects.count { it.joint == joint } + 1
    return Project(
        id = newId(),
        name = "${joint.title} $number",
        joint = joint,
        inputs = inputs.mapKeys { it.key.name },
        createdAt = now,
        updatedAt = now,
    )
}

fun Project.fieldInputs(): Map<Field, String> =
    inputs.mapNotNull { (k, v) -> Field.entries.firstOrNull { it.name == k }?.let { it to v } }.toMap()
