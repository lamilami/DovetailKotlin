package bg.workshop.dovetail.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bg.workshop.dovetail.data.inputs
import bg.workshop.dovetail.data.matches
import bg.workshop.dovetail.geometry.Calc
import bg.workshop.dovetail.geometry.Field
import bg.workshop.dovetail.geometry.JigSpec
import bg.workshop.dovetail.geometry.JointType
import bg.workshop.dovetail.geometry.valueOrNull
import bg.workshop.dovetail.model.Library
import bg.workshop.dovetail.ui.components.Callout
import bg.workshop.dovetail.ui.components.CalcView
import bg.workshop.dovetail.ui.components.Instructions
import bg.workshop.dovetail.ui.components.MeasureField
import bg.workshop.dovetail.ui.components.Panel
import bg.workshop.dovetail.ui.components.PresetOption
import bg.workshop.dovetail.ui.components.PresetPicker
import bg.workshop.dovetail.ui.components.ResultGrid
import bg.workshop.dovetail.ui.components.ResultItem
import bg.workshop.dovetail.ui.components.StepHeader
import bg.workshop.dovetail.ui.components.Tone
import bg.workshop.dovetail.ui.components.message
import bg.workshop.dovetail.ui.components.mm
import bg.workshop.dovetail.ui.components.rangeText
import bg.workshop.dovetail.ui.diagram.BackStopDistanceDiagram
import bg.workshop.dovetail.ui.diagram.BitDepthDiagram
import bg.workshop.dovetail.ui.diagram.CenteringDiagram
import bg.workshop.dovetail.ui.diagram.ClampDiagram
import bg.workshop.dovetail.ui.diagram.CutPathDiagram
import bg.workshop.dovetail.ui.diagram.DrawerPartsDiagram
import bg.workshop.dovetail.ui.diagram.EndStopsDiagram
import bg.workshop.dovetail.ui.diagram.stopOffsetText
import bg.workshop.dovetail.ui.diagram.FingerBitDiagram
import bg.workshop.dovetail.ui.diagram.FingerMeshDiagram
import bg.workshop.dovetail.ui.diagram.FingerSlotDiagram
import bg.workshop.dovetail.ui.diagram.RouterBaseDiagram

private const val FINGER_NOTE =
    "Ръководството описва само лястовича опашка. Стъпките за прав зъб са общата практика с гребеновиден шаблон — потвърди с пробна сглобка."

// ─── Избор от библиотеката ──────────────────────────────────────────────

@Composable
private fun JigPicker(state: SetupUiState, lib: Library, vm: SetupViewModel) = PresetPicker(
    title = "От библиотеката",
    options = lib.jigs.map { PresetOption(it.id, it.name, state.text.matches(it.inputs())) },
    onPick = { id -> lib.jigs.firstOrNull { it.id == id }?.let { vm.apply(it) } },
    onSaveCurrent = vm::saveJig,
    saveTitle = "Запази шаблона",
)

@Composable
private fun BitPicker(state: SetupUiState, lib: Library, vm: SetupViewModel) {
    val bits = lib.bitsFor(state.joint)
    PresetPicker(
        title = "От библиотеката",
        options = bits.map { PresetOption(it.id, it.name, state.text.matches(it.inputs())) },
        onPick = { id -> bits.firstOrNull { it.id == id }?.let { vm.apply(it) } },
        onSaveCurrent = vm::saveBit,
        saveTitle = "Запази фрезера",
    )
}

@Composable
private fun BasePicker(state: SetupUiState, lib: Library, vm: SetupViewModel) = PresetPicker(
    title = "От библиотеката",
    options = lib.bases.map { PresetOption(it.id, it.name, state.text.matches(it.inputs())) },
    onPick = { id -> lib.bases.firstOrNull { it.id == id }?.let { vm.apply(it) } },
    onSaveCurrent = vm::saveBase,
    saveTitle = "Запази основата",
)

// ─── 1. Стопери ─────────────────────────────────────────────────────────

@Composable
fun StopsStep(state: SetupUiState, lib: Library, vm: SetupViewModel) {
    StepHeader("Стопери и шаблон", "Настройва се веднъж, огледално от двете страни.")
    Panel { EndStopsDiagram(state.value(Field.FINGER), state.value(Field.SLOT)) }
    Panel("Действия") {
        Instructions(
            listOf(
                "С гония начертай линия с молив през средата на първия пръст — перпендикулярна на ръбовете на шаблона.",
                "Подравни вътрешния ръб на левия хоризонтален стопер с линията. Провери с гония.",
                "Измести вертикалния стопер на " +
                    (JigSpec.stopOffset(state.value(Field.FINGER), state.value(Field.SLOT))?.let(::stopOffsetText) ?: "половин стъпка") +
                    " вдясно от хоризонталния (половин стъпка на шаблона), перпендикулярно на горната страна.",
                "Повтори огледално от дясната страна.",
            ),
        )
    }
    Panel("Шаблон") {
        JigPicker(state, lib, vm)
        MeasureField(Field.FINGER, state.text(Field.FINGER), vm::onValue)
        MeasureField(Field.SLOT, state.text(Field.SLOT), vm::onValue)
    }
    Callout("Монтирай шаблона на тезгяха наравно с предния ръб и остави място отзад за най-дългия детайл.")
}

// ─── 2. Детайли ─────────────────────────────────────────────────────────

@Composable
fun PartsStep(state: SetupUiState, vm: SetupViewModel) {
    StepHeader(
        "Детайли",
        if (state.joint == JointType.DOVETAIL) "Ширина, центриране и коя страна на шаблона реже кой ъгъл."
        else "Ширина и как се отместват двата детайла.",
    )
    Panel {
        CenteringDiagram(state.value(Field.BOARD_WIDTH), state.value(Field.FINGER), state.value(Field.SLOT))
        MeasureField(Field.BOARD_WIDTH, state.text(Field.BOARD_WIDTH), vm::onValue)
        CalcView(state.centering) { c ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultGrid(
                    listOf(
                        ResultItem("Център", c.center.mm(), highlight = true),
                        ResultItem("Стъпка", c.pitch.mm()),
                        ResultItem("Ръб → център на пръст", c.edgeToFingerCenter.mm()),
                        ResultItem("Пръсти по ширината", "${c.fingersOnBoard}", "бр."),
                    ),
                )
                if (!c.fullyFormed) {
                    val (lo, hi) = c.nearestFullWidths
                    Text(
                        "Най-близки ширини: ${lo.mm()} или ${hi.mm()} mm",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
    if (state.joint == JointType.DOVETAIL) ThicknessPanel(state, vm)
    when (state.joint) {
        JointType.DOVETAIL -> Panel("Разположение") {
            DrawerPartsDiagram()
            Instructions(
                listOf(
                    "Ъглите отдясно на чекмеджето (1 + 2) се режат в лявата страна на шаблона, ъглите отляво (3 + 4) — в дясната.",
                    "Лицето и гърбът — хоризонтално отгоре, вътрешната страна нагоре, допрени до хоризонталния стопер.",
                    "Страните — вертикално отпред, вътрешната страна към теб, горният край допрян до вертикалния стопер.",
                    "Маркирай детайлите, преди да ги закрепиш.",
                ),
            )
        }
        JointType.FINGER -> {
            val offset = state.finger.valueOrNull()?.offset
            val offsetText = offset?.let { "${it.mm()} mm" } ?: "½ стъпка"
            Panel("Отместване") {
                FingerMeshDiagram(state.finger.valueOrNull())
                Instructions(
                    listOf(
                        "Двата детайла се режат с един и същ шаблон и фрезер.",
                        "Детайл А се допира до стопера.",
                        "Детайл Б се измества с $offsetText спрямо А — например с дистанционна подложка с тази дебелина до стопера.",
                        "Маркирай А и Б и коя страна е навън — режи ги еднакво ориентирани.",
                    ),
                )
            }
            Callout(FINGER_NOTE)
        }
    }
}

// ─── 3. Закрепване ──────────────────────────────────────────────────────

@Composable
fun ClampStep(state: SetupUiState, vm: SetupViewModel) {
    val t = state.value(Field.THICKNESS)
    val front = state.value(Field.FRONT_THICKNESS)
    val finger = state.joint == JointType.FINGER
    StepHeader("Закрепване", if (finger) "Детайлът вертикално, шаблонът лежи равно върху опора." else "Двата детайла трябва да са наравно под шаблона.")
    Panel {
        ClampDiagram(if (finger) t else front, finger)
        if (!finger) MeasureField(Field.FRONT_THICKNESS, state.text(Field.FRONT_THICKNESS), vm::onValue)
        MeasureField(Field.THICKNESS, state.text(Field.THICKNESS), vm::onValue)
        state.thickness.forEach { Callout(it.message, Tone.Warning) }
    }
    Panel("Действия") {
        Instructions(
            if (finger) listOf(
                "Сложи детайла вертикално в предната летва, горният край наравно с горната повърхност на опората.",
                "В горната летва сложи опорна дъска със същата дебелина" + (t?.let { " (${it.mm()} mm)" } ?: "") + ", допряна до детайла — шаблонът трябва да лежи равно.",
                "Провери, че прорезите на шаблона минават над цялата дебелина на детайла и лагерът влиза в тях.",
                "Заключи: горния лост — от теб, долния — надолу.",
            ) else listOf(
                "Сложи страната в предната летва така, че да излиза над предния ръб на шаблона с дебелината на хоризонталния детайл" +
                    (front?.let { " (${it.mm()} mm)." } ?: "."),
                "Сложи лицето или гърба в горната летва и го допри до вертикалния детайл — двата да са наравно под шаблона.",
                "Провери, че и двата са допрени до стоперите. Заключи: горния лост — от теб, долния — надолу.",
                "Отвий двата винта на шаблона, постави върховете на пръстите ${JigSpec.TEMPLATE_SETBACK.mm()} mm назад от лицето на вертикалния детайл и затегни.",
            ),
        )
    }
    Callout("Регулирай летвите с винтовете в краищата им според дебелината, така че дървото да е здраво притиснато.")
    Callout("При тесни детайли сложи парче със същата дебелина под летвата в другия край — иначе летвата се изкривява.", Tone.Warning)
    if (finger) Callout(FINGER_NOTE)
}

// ─── 4. Заден ограничител (само лястовича опашка) ───────────────────────

@Composable
fun BackStopStep(state: SetupUiState, lib: Library, vm: SetupViewModel) {
    val stop = state.backStop.valueOrNull()
    StepHeader("Заден ограничител", "Разстоянието от върховете на пръстите до ограничителя зависи от основата на фрезера.")
    Panel { BackStopDistanceDiagram(stop?.distance) }
    Panel("Основа на фрезера") {
        BasePicker(state, lib, vm)
        RouterBaseDiagram(
            state.value(Field.BASE_DIAMETER),
            state.value(Field.CENTER_TO_FLAT),
            state.flatToStop,
            state.value(Field.BIT_DIAMETER),
        )
        MeasureField(Field.BASE_DIAMETER, state.text(Field.BASE_DIAMETER), vm::onValue)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Плоска страна към ограничителя", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = state.flatToStop, onCheckedChange = vm::onFlatToStop)
        }
        if (state.flatToStop) MeasureField(Field.CENTER_TO_FLAT, state.text(Field.CENTER_TO_FLAT), vm::onValue)
        MeasureField(Field.THICKNESS, state.text(Field.THICKNESS), vm::onValue)
        MeasureField(Field.BIT_DIAMETER, state.text(Field.BIT_DIAMETER), vm::onValue)
    }
    Panel("Резултат") {
        CalcView(state.backStop) { b ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ResultGrid(listOf(ResultItem("Ограничител", b.distance.mm(), highlight = true)))
                val t = state.value(Field.THICKNESS)
                val d = state.value(Field.BIT_DIAMETER)
                if (t != null && d != null) {
                    Text(
                        "${if (state.flatToStop) "C" else "R"} + 2t − d/2 = ${b.baseReference.mm()} + ${(2 * t).mm()} − ${(d / 2).mm()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    Instructions(
        listOf(
            "Настрой шублер на изчисленото разстояние и нагласи ограничителя спрямо върховете на пръстите.",
            "Заключи с винтовете на ограничителя.",
        ),
    )
    Callout("Тук не е нужна пълна точност — финото донастройване е след пробната сглобка.")
}

// ─── 5. Фрезер ──────────────────────────────────────────────────────────

@Composable
fun BitStep(state: SetupUiState, lib: Library, vm: SetupViewModel) {
    if (state.joint == JointType.FINGER) FingerBitStep(state, lib, vm) else DovetailBitStep(state, lib, vm)
}

@Composable
private fun DovetailBitStep(state: SetupUiState, lib: Library, vm: SetupViewModel) {
    StepHeader(
        "Фрезер и дълбочина",
        "Начално ${JigSpec.START_DEPTH_MIN.mm()}–${JigSpec.START_DEPTH_MAX.mm()} mm под пръстите на шаблона.",
    )
    Panel {
        BitDepthDiagram(state.value(Field.DEPTH), state.value(Field.BIT_DIAMETER), state.bit.valueOrNull())
        MeasureField(Field.DEPTH, state.text(Field.DEPTH), vm::onValue)
        MeasureField(Field.FRONT_THICKNESS, state.text(Field.FRONT_THICKNESS), vm::onValue)
        MeasureField(Field.MIN_WALL, state.text(Field.MIN_WALL), vm::onValue)
        CalcView(state.wall) { w ->
            ResultGrid(listOf(ResultItem("Остатъчна стена (жлеб по дебелината)", w.remaining.mm(), highlight = true)))
        }
    }
    Panel("Профил на фрезера") {
        BitPicker(state, lib, vm)
        MeasureField(Field.BIT_DIAMETER, state.text(Field.BIT_DIAMETER), vm::onValue)
        MeasureField(Field.ANGLE, state.text(Field.ANGLE), vm::onValue)
        MeasureField(Field.CUTTING_HEIGHT, state.text(Field.CUTTING_HEIGHT), vm::onValue)
        CalcView(state.bit) { b ->
            ResultGrid(
                buildList {
                    add(ResultItem("Тесен диаметър", b.narrowDiameter.mm()))
                    b.diameterAtDepth?.let { add(ResultItem("Диаметър при шаблона", it.mm())) }
                },
            )
        }
        Callout("Ъгълът е спрямо оста, а не пълният ъгъл на конуса.")
    }
    Callout("Ако лагерът не допира добре прорезите на шаблона, сложи подложка между шаблона и детайла.")
    Callout("Дълбочината решава колко стегната е сглобката — дори малка промяна има голям ефект.", Tone.Warning)
}

@Composable
private fun FingerBitStep(state: SetupUiState, lib: Library, vm: SetupViewModel) {
    val j = state.finger.valueOrNull()
    StepHeader("Прав фрезер с лагер", "Дълбочината е дебелината на другия детайл; ширината на гнездото — от лагера.")
    Panel {
        FingerBitDiagram(j?.depth, state.value(Field.BIT_DIAMETER), state.value(Field.BEARING_DIAMETER), state.value(Field.SLOT))
        BitPicker(state, lib, vm)
        MeasureField(Field.BIT_DIAMETER, state.text(Field.BIT_DIAMETER), vm::onValue)
        MeasureField(Field.BEARING_DIAMETER, state.text(Field.BEARING_DIAMETER), vm::onValue)
        MeasureField(Field.CUTTING_HEIGHT, state.text(Field.CUTTING_HEIGHT), vm::onValue)
        MeasureField(Field.THICKNESS, state.text(Field.THICKNESS), vm::onValue)
        MeasureField(Field.PROUD, state.text(Field.PROUD), vm::onValue)
    }
    Panel("Резултат") {
        CalcView(state.finger) { f ->
            ResultGrid(
                listOf(
                    ResultItem("Дълбочина под шаблона", f.depth.mm(), highlight = true),
                    ResultItem("Хлабина", f.clearance.mm(), highlight = true),
                    ResultItem("Гнездо", f.socketWidth.mm()),
                    ResultItem("Зъб", f.pinWidth.mm()),
                ),
            )
        }
    }
    Panel("Прорез и гнездо") {
        FingerSlotDiagram(state.value(Field.FINGER), state.value(Field.SLOT), state.value(Field.BIT_DIAMETER), state.value(Field.BEARING_DIAMETER), j)
        Text(
            "Гнездо = прорез + (Ø фрезер − Ø лагер). При еднакви пръст и прорез хлабината е 2 × (Ø фрезер − Ø лагер): 0,1 mm разлика дава 0,2 mm хлабина.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Callout("Лагерът трябва да опира в ръба на шаблона. Ако режещата част е по-дълга от нужната дълбочина, повдигни шаблона с подложка.")
}

// ─── 6. Рязане ──────────────────────────────────────────────────────────

@Composable
fun CutStep(state: SetupUiState) {
    val finger = state.joint == JointType.FINGER
    StepHeader("Рязане", "Първо на два пробни детайла със същата ширина и дебелина.")
    Panel { CutPathDiagram(state.value(Field.FINGER), state.value(Field.SLOT)) }
    Panel("Действия") {
        val offset = state.finger.valueOrNull()?.offset?.let { "${it.mm()} mm" } ?: "½ стъпка"
        Instructions(
            if (finger) listOf(
                "Постави фрезера върху шаблона така, че да не допира дървото, и го включи.",
                "Първи проход: отдясно наляво по лицето, почти докосвайки върховете — пази ръба от разцепване.",
                "Следвай шаблона навътре и навън, отляво надясно, докато лагерът мине през цялата дебелина на детайла.",
                "Изключи фрезера, смени с детайл Б (изместен с $offset) и повтори.",
            ) else listOf(
                "Постави фрезера върху шаблона така, че да не допира дървото, и го включи.",
                "Първи проход: отдясно наляво през детайла, почти докосвайки върховете на пръстите.",
                "Следвай шаблона навътре и навън между пръстите, отляво надясно. Не натискай — остави фрезера да работи.",
                "Изключи фрезера, преди да го вдигнеш от шаблона.",
            ),
        )
    }
    Callout("Никога не вдигай фрезера от шаблона, докато работи.", Tone.Error)
}

// ─── 7. Проба и карта ───────────────────────────────────────────────────

@Composable
fun TrialStep(state: SetupUiState) {
    StepHeader("Проба и карта", "Всички настройки на едно място и как да донастроиш след пробата.")
    val c = state.centering.valueOrNull()
    Panel("Карта с настройки") {
        SummaryRow("Център на детайла", c?.center?.let { "${it.mm()} mm" })
        SummaryRow("Стъпка", c?.pitch?.let { "${it.mm()} mm" })
        when (state.joint) {
            JointType.DOVETAIL -> {
                SummaryRow("Стопери", JigSpec.stopOffset(state.value(Field.FINGER), state.value(Field.SLOT))?.let(::stopOffsetText))
                SummaryRow("Дебелина лице / страни", listOf(state.value(Field.FRONT_THICKNESS), state.value(Field.THICKNESS))
                    .takeIf { l -> l.all { it != null } }?.joinToString(" / ") { it!!.mm() }?.let { "$it mm" })
                SummaryRow("Шаблон назад от детайла", "${JigSpec.TEMPLATE_SETBACK.mm()} mm")
                SummaryRow("Заден ограничител", state.backStop.display { "${it.distance.mm()} mm" })
                SummaryRow("Дълбочина под пръстите", state.value(Field.DEPTH)?.let { "${it.mm()} mm" })
                SummaryRow("Остатъчна стена", state.wall.display { "${it.remaining.mm()} mm" })
                SummaryRow("Тесен диаметър на фрезера", state.bit.display { "${it.narrowDiameter.mm()} mm" })
            }
            JointType.FINGER -> {
                SummaryRow("Отместване на Б", state.finger.display { "${it.offset.mm()} mm" })
                SummaryRow("Дълбочина под шаблона", state.finger.display { "${it.depth.mm()} mm" })
                SummaryRow("Гнездо / зъб", state.finger.display { "${it.socketWidth.mm()} / ${it.pinWidth.mm()}" })
                SummaryRow("Хлабина", state.finger.display { "${it.clearance.mm()} mm" })
            }
        }
    }
    Panel("Фина настройка") {
        when (state.joint) {
            JointType.DOVETAIL -> {
                TuneRow("Хлабава сглобка", "Увеличи дълбочината — свали фрезера.")
                TuneRow("Стегната сглобка", "Намали дълбочината — вдигни фрезера.")
                TuneRow("Твърде дълбока или плитка", "Премести ограничителя.")
                Callout("Това са типичните посоки при шаблони за полускрити опашки. Сравни ги с фигурата за фина настройка в ръководството.")
            }
            JointType.FINGER -> {
                TuneRow("Хлабава сглобка", "По-голям лагер или по-тънък фрезер — хлабината е 2 × разликата в диаметрите.")
                TuneRow("Стегната или не влиза", "По-малък лагер или по-дебел фрезер.")
                TuneRow("Зъбите са къси", "Свали фрезера — дълбочината трябва да е поне дебелината на другия детайл.")
                TuneRow("Зъбите стърчат", "Вдигни фрезера или изравни след лепене.")
                TuneRow("Зъбите не застават срещу гнездата", "Провери отместването на детайл Б.")
            }
        }
    }
    Callout("След корекцията направи втора проба. Проверявай настройките периодично при серийна работа.")
}

private fun <T> Calc<T>.display(format: (T) -> String): String? = valueOrNull()?.let(format)

@Composable
private fun SummaryRow(label: String, value: String?) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                value ?: "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun TuneRow(problem: String, fix: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(problem, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(fix, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Дебелини спрямо шаблона ────────────────────────────────────────────

@Composable
private fun ThicknessPanel(state: SetupUiState, vm: SetupViewModel) = Panel("Дебелини") {
    MeasureField(Field.FRONT_THICKNESS, state.text(Field.FRONT_THICKNESS), vm::onValue)
    MeasureField(Field.THICKNESS, state.text(Field.THICKNESS), vm::onValue)
    val limits = state.jig?.limits
    if (limits != null) {
        Text(
            "${state.jig.name}: лице ${rangeText(limits.frontMin, limits.frontMax)}, страни ${rangeText(limits.sideMin, limits.sideMax)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            "За този шаблон няма зададен обхват на дебелините — може да го добавиш в „Инструменти → Шаблони“.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    state.thickness.forEach { Callout(it.message, Tone.Warning) }
}
