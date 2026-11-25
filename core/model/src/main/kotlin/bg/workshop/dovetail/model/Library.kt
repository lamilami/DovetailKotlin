package bg.workshop.dovetail.model

import bg.workshop.dovetail.geometry.JointType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Шаблон: размерите на металните пръсти и прорезите. */
@Serializable
data class Jig(
    val id: String,
    val name: String,
    val finger: Double,
    val slot: Double,
)

@Serializable
enum class BitKind {
    /** Фрезер „лястовича опашка“. */
    DOVETAIL,
    /** Прав фрезер с копирен лагер — за прав зъб. */
    STRAIGHT_BEARING,
}

val JointType.bitKind: BitKind
    get() = when (this) {
        JointType.DOVETAIL -> BitKind.DOVETAIL
        JointType.FINGER -> BitKind.STRAIGHT_BEARING
    }

@Serializable
data class Bit(
    val id: String,
    val name: String,
    val kind: BitKind,
    /** Ø на върха (лястовича опашка) или Ø на режещата част (прав). */
    val diameter: Double,
    val angleDeg: Double? = null,
    /** Височина на режещата част / режеща дължина. */
    val cuttingHeight: Double? = null,
    val bearingDiameter: Double? = null,
)

@Serializable
data class RouterBase(
    val id: String,
    val name: String,
    val diameter: Double,
    val centerToFlat: Double? = null,
)

/**
 * Една сглобка в работа. Входовете се пазят като текст, точно както са въведени,
 * с ключ името на полето — така непознати или преименувани полета не чупят файла.
 */
@Serializable
data class Project(
    val id: String,
    val name: String,
    val joint: JointType,
    val inputs: Map<String, String> = emptyMap(),
    val flatToStop: Boolean = false,
    val step: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class Library(
    val version: Int = 1,
    val jigs: List<Jig> = emptyList(),
    val bits: List<Bit> = emptyList(),
    val bases: List<RouterBase> = emptyList(),
    val projects: List<Project> = emptyList(),
) {
    fun upsert(jig: Jig) = copy(jigs = jigs.upsert(jig) { it.id })
    fun upsert(bit: Bit) = copy(bits = bits.upsert(bit) { it.id })
    fun upsert(base: RouterBase) = copy(bases = bases.upsert(base) { it.id })
    fun upsert(project: Project) = copy(projects = projects.upsert(project) { it.id })

    fun deleteJig(id: String) = copy(jigs = jigs.filterNot { it.id == id })
    fun deleteBit(id: String) = copy(bits = bits.filterNot { it.id == id })
    fun deleteBase(id: String) = copy(bases = bases.filterNot { it.id == id })
    fun deleteProject(id: String) = copy(projects = projects.filterNot { it.id == id })

    fun project(id: String): Project? = projects.firstOrNull { it.id == id }
    fun bitsFor(joint: JointType): List<Bit> = bits.filter { it.kind == joint.bitKind }
    fun recentProjects(): List<Project> = projects.sortedByDescending { it.updatedAt }

    /** Добавя стандартните шаблони, които липсват в библиотека от по-стара версия. Не пипа съществуващите. */
    fun migrate(): Library {
        if (version >= CURRENT_VERSION) return this
        val known = jigs.map { it.id }.toSet()
        return copy(version = CURRENT_VERSION, jigs = jigs + PRESET_JIGS.filterNot { it.id in known })
    }

    companion object {
        const val CURRENT_VERSION = 2

        /** Стандартни шаблони 1/2", 7/16", 9/16" — пръст = прорез = размера на шаблона. */
        val PRESET_JIGS = listOf(
            Jig("jig-12-7", "Шаблон 1/2\" (12,7 mm)", 12.7, 12.7),
            Jig("jig-7-16", "Шаблон 7/16\" (11,1 mm)", 11.1125, 11.1125),
            Jig("jig-9-16", "Шаблон 9/16\" (14,3 mm)", 14.2875, 14.2875),
        )

        /** Начална библиотека: стандартните шаблони и типичните инструменти. */
        fun seed() = Library(
            version = CURRENT_VERSION,
            jigs = PRESET_JIGS,
            bits = listOf(
                Bit("bit-dt-14-8", "Лястовича Ø14,8 (стандартен)", BitKind.DOVETAIL, 14.8),
                Bit("bit-dt-15-35", "Лястовича Ø15,35", BitKind.DOVETAIL, 15.35),
                Bit("bit-st-12-7", "Прав Ø12,7 с лагер 12,7", BitKind.STRAIGHT_BEARING, 12.7, bearingDiameter = 12.7),
            ),
            bases = listOf(RouterBase("base-160", "Основа Ø160", 160.0, centerToFlat = 70.0)),
        )
    }
}

private inline fun <T> List<T>.upsert(item: T, key: (T) -> String): List<T> {
    val i = indexOfFirst { key(it) == key(item) }
    return if (i < 0) this + item else toMutableList().also { it[i] = item }
}

object LibraryCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    fun encode(library: Library): String = json.encodeToString(Library.serializer(), library)
    fun decode(text: String): Library = json.decodeFromString(Library.serializer(), text)
}
