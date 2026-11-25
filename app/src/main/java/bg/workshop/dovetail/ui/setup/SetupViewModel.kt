package bg.workshop.dovetail.ui.setup

import androidx.lifecycle.ViewModel
import bg.workshop.dovetail.data.LibraryRepository
import bg.workshop.dovetail.data.baseFrom
import bg.workshop.dovetail.data.bitFrom
import bg.workshop.dovetail.data.fieldInputs
import bg.workshop.dovetail.data.inputs
import bg.workshop.dovetail.data.jigFrom
import bg.workshop.dovetail.geometry.BackStop
import bg.workshop.dovetail.geometry.BitProfile
import bg.workshop.dovetail.geometry.Calc
import bg.workshop.dovetail.geometry.Centering
import bg.workshop.dovetail.geometry.Field
import bg.workshop.dovetail.geometry.FingerJoint
import bg.workshop.dovetail.geometry.JointType
import bg.workshop.dovetail.geometry.Wall
import bg.workshop.dovetail.geometry.backStop
import bg.workshop.dovetail.geometry.bitProfile
import bg.workshop.dovetail.geometry.centering
import bg.workshop.dovetail.geometry.fingerJoint
import bg.workshop.dovetail.geometry.parseMeasure
import bg.workshop.dovetail.geometry.parseOptionalMeasure
import bg.workshop.dovetail.geometry.wall
import bg.workshop.dovetail.model.Bit
import bg.workshop.dovetail.model.Jig
import bg.workshop.dovetail.model.Library
import bg.workshop.dovetail.model.RouterBase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Стъпките следват реда на работа от ръководството на шаблона. */
enum class Step(val title: String, val short: String) {
    STOPS("Стопери и шаблон", "Стопери"),
    PARTS("Детайли", "Детайли"),
    CLAMP("Закрепване", "Захват"),
    BACKSTOP("Заден ограничител", "Огранич."),
    BIT("Фрезер и дълбочина", "Фрезер"),
    CUT("Рязане", "Рязане"),
    TRIAL("Проба и карта", "Проба"),
}

/** При прав зъб задният ограничител не участва — дълбочината идва от дебелината. */
fun stepsFor(joint: JointType): List<Step> = when (joint) {
    JointType.DOVETAIL -> Step.entries
    JointType.FINGER -> Step.entries - Step.BACKSTOP
}

data class SetupUiState(
    val projectName: String,
    val joint: JointType,
    val text: Map<Field, String>,
    val flatToStop: Boolean,
    val step: Step,
    val centering: Calc<Centering>,
    val backStop: Calc<BackStop>,
    val wall: Calc<Wall>,
    val bit: Calc<BitProfile>,
    val finger: Calc<FingerJoint>,
) {
    val steps: List<Step> get() = stepsFor(joint)
    val stepIndex: Int get() = steps.indexOf(step).coerceAtLeast(0)
    fun text(field: Field): String = text[field].orEmpty()
    fun value(field: Field): Double? = parseMeasure(text(field))
}

class SetupViewModel(private val repo: LibraryRepository, private val projectId: String) : ViewModel() {
    val library: StateFlow<Library> = repo.library

    private val project = repo.library.value.project(projectId)
        ?: error("Няма проект $projectId")

    private val _state = MutableStateFlow(
        compute(
            name = project.name,
            joint = project.joint,
            text = project.fieldInputs(),
            flatToStop = project.flatToStop,
            step = Step.entries.firstOrNull { it.name == project.step }?.takeIf { it in stepsFor(project.joint) } ?: Step.STOPS,
        ),
    )
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun onValue(field: Field, text: String) = edit { it.copy(text = it.text + (field to text)) }
    fun onFlatToStop(flat: Boolean) = edit { it.copy(flatToStop = flat) }
    fun rename(name: String) = edit { it.copy(projectName = name.trim().ifEmpty { it.projectName }) }

    fun goTo(step: Step) = edit { it.copy(step = step) }
    fun next() = edit { s -> s.copy(step = s.steps.getOrElse(s.stepIndex + 1) { s.step }) }
    fun back() = edit { s -> s.copy(step = s.steps.getOrElse(s.stepIndex - 1) { s.step }) }

    // ─── Библиотека ───

    fun apply(jig: Jig) = edit { it.copy(text = it.text + jig.inputs()) }
    fun apply(bit: Bit) = edit { it.copy(text = it.text + bit.inputs()) }
    fun apply(base: RouterBase) = edit {
        it.copy(text = it.text + base.inputs(), flatToStop = it.flatToStop && base.centerToFlat != null)
    }

    /** Записва текущите стойности като нов инструмент. Връща false, ако липсват стойности. */
    fun saveJig(name: String): Boolean = save { s -> jigFrom(name, s::value)?.let { j -> { lib: Library -> lib.upsert(j) } } }
    fun saveBit(name: String): Boolean = save { s -> bitFrom(name, s.joint, s::value)?.let { b -> { lib: Library -> lib.upsert(b) } } }
    fun saveBase(name: String): Boolean = save { s -> baseFrom(name, s.flatToStop, s::value)?.let { b -> { lib: Library -> lib.upsert(b) } } }

    private fun save(build: (SetupUiState) -> ((Library) -> Library)?): Boolean {
        val change = build(_state.value) ?: return false
        repo.update(change)
        return true
    }

    // ─── Вътрешни ───

    private fun edit(change: (SetupUiState) -> SetupUiState) {
        _state.update { s -> change(s).let { compute(it.projectName, it.joint, it.text, it.flatToStop, it.step) } }
        persist()
    }

    private fun persist() {
        val s = _state.value
        repo.update { lib ->
            val current = lib.project(projectId) ?: return@update lib
            lib.upsert(
                current.copy(
                    name = s.projectName,
                    inputs = s.text.mapKeys { it.key.name },
                    flatToStop = s.flatToStop,
                    step = s.step.name,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun compute(name: String, joint: JointType, text: Map<Field, String>, flatToStop: Boolean, step: Step): SetupUiState {
        fun v(f: Field) = parseMeasure(text[f].orEmpty())
        return SetupUiState(
            projectName = name,
            joint = joint,
            text = text,
            flatToStop = flatToStop,
            step = step,
            centering = centering(v(Field.BOARD_WIDTH), v(Field.FINGER), v(Field.SLOT)),
            backStop = backStop(v(Field.BASE_DIAMETER), v(Field.CENTER_TO_FLAT), flatToStop, v(Field.THICKNESS), v(Field.BIT_DIAMETER)),
            wall = wall(v(Field.THICKNESS), v(Field.DEPTH), v(Field.MIN_WALL)),
            bit = bitProfile(v(Field.BIT_DIAMETER), v(Field.ANGLE), v(Field.CUTTING_HEIGHT), v(Field.DEPTH)),
            finger = fingerJoint(
                v(Field.FINGER), v(Field.SLOT), v(Field.BIT_DIAMETER), v(Field.BEARING_DIAMETER), v(Field.THICKNESS),
                cuttingLength = v(Field.CUTTING_HEIGHT),
                proud = parseOptionalMeasure(text[Field.PROUD].orEmpty()),
            ),
        )
    }
}
