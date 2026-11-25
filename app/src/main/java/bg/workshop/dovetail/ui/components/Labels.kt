package bg.workshop.dovetail.ui.components

import bg.workshop.dovetail.geometry.Field
import bg.workshop.dovetail.geometry.Reason
import bg.workshop.dovetail.geometry.Warning
import java.util.Locale

/** 12,70 → "12,7"; запазва до 2 знака, маха излишните нули. */
fun Double.mm(): String =
    String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.').replace('.', ',')

val Field.label: String
    get() = when (this) {
        Field.FINGER -> "Метален пръст"
        Field.SLOT -> "Прорез"
        Field.BOARD_WIDTH -> "Ширина на детайла"
        Field.THICKNESS -> "Дебелина на страните"
        Field.BASE_DIAMETER -> "Диаметър на основата на фрезера"
        Field.CENTER_TO_FLAT -> "Център до плоската страна"
        Field.BIT_DIAMETER -> "Диаметър на върха на фрезера"
        Field.DEPTH -> "Дълбочина под пръстите"
        Field.MIN_WALL -> "Минимална остатъчна стена"
        Field.ANGLE -> "Ъгъл на страната спрямо оста"
        Field.CUTTING_HEIGHT -> "Височина на режещата част"
        Field.BEARING_DIAMETER -> "Диаметър на копирния лагер"
        Field.PROUD -> "Надстояване на зъбите (по избор)"
    }

val Field.unit: String get() = if (this == Field.ANGLE) "°" else "mm"

/** Полета, които могат да са празни или 0. */
val Field.optional: Boolean get() = this == Field.PROUD

val Reason.message: String
    get() = when (this) {
        Reason.CHORD_EXCEEDS_RADIUS -> "Разстоянието до плоската страна е по-голямо от радиуса на основата."
        Reason.STOP_NOT_POSITIVE -> "Ограничителят излиза ≤ 0 mm — провери основата, дебелината и диаметъра."
        Reason.GROOVE_THROUGH_BOARD -> "Дълбочината е равна или по-голяма от дебелината — фрезерът ще пробие детайла."
        Reason.ANGLE_NOT_BELOW_90 -> "Ъгълът трябва да е под 90°."
        Reason.BIT_GEOMETRY_IMPOSSIBLE -> "Невъзможна геометрия: конусът се затваря преди зададената височина."
        Reason.BEARING_WIDER_THAN_SLOT -> "Лагерът е по-широк от прореза на шаблона — няма да влезе в него."
        Reason.FINGER_GEOMETRY_IMPOSSIBLE -> "Фрезерът е твърде голям спрямо лагера — гнездата изяждат зъбите."
    }

val Warning.message: String
    get() = when (this) {
        Warning.WALL_BELOW_MINIMUM -> "Остатъчната стена е под зададения минимум."
        Warning.DEPTH_EXCEEDS_CUTTING_HEIGHT -> "Дълбочината е над режещата част на фрезера."
        Warning.WIDTH_NOT_FULLY_FORMED -> "Ширината не е кратна на половин стъпка на шаблона — крайните опашки няма да са изцяло оформени."
        Warning.DEPTH_OUTSIDE_RECOMMENDED -> "Ръководството препоръчва начална дълбочина 10–11 mm под пръстите."
        Warning.JOINT_INTERFERENCE -> "Фрезерът е по-малък от лагера — зъбите са по-широки от гнездата и сглобката няма да влезе."
        Warning.JOINT_LOOSE -> "Хлабината е над 0,3 mm — сглобката ще е видимо хлабава."
        Warning.CUTTER_TOO_SHORT -> "Режещата част е по-къса от нужната дълбочина — няма да среже цялата дебелина."
    }
