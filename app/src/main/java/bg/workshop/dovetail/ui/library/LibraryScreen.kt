package bg.workshop.dovetail.ui.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bg.workshop.dovetail.data.asInput
import bg.workshop.dovetail.data.newId
import bg.workshop.dovetail.geometry.parseMeasure
import bg.workshop.dovetail.model.Bit
import bg.workshop.dovetail.model.BitKind
import bg.workshop.dovetail.model.Jig
import bg.workshop.dovetail.model.RouterBase
import bg.workshop.dovetail.ui.AppViewModel
import bg.workshop.dovetail.ui.components.ConfirmDeleteDialog
import bg.workshop.dovetail.ui.components.mm

private enum class LibTab(val title: String) { JIGS("Шаблони"), BITS("Фрезери"), BASES("Основи") }

/** Какво се редактира в момента: null = нищо, иначе нов (id == null) или съществуващ елемент. */
private sealed interface Editing {
    data class JigEdit(val jig: Jig?) : Editing
    data class BitEdit(val bit: Bit?) : Editing
    data class BaseEdit(val base: RouterBase?) : Editing
}

private data class Deleting(val name: String, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(app: AppViewModel, onBack: () -> Unit) {
    val lib by app.library.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(LibTab.JIGS) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    var deleting by remember { mutableStateOf<Deleting?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Инструменти", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = when (tab) {
                        LibTab.JIGS -> Editing.JigEdit(null)
                        LibTab.BITS -> Editing.BitEdit(null)
                        LibTab.BASES -> Editing.BaseEdit(null)
                    }
                },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Добави") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab.ordinal) {
                LibTab.entries.forEach { t ->
                    Tab(selected = t == tab, onClick = { tab = t }, text = { Text(t.title) })
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (tab) {
                    LibTab.JIGS -> items(lib.jigs, key = { it.id }) { j ->
                        ItemCard(j.name, j.subtitle(), { editing = Editing.JigEdit(j) }) {
                            deleting = Deleting(j.name) { app.deleteJig(j.id) }
                        }
                    }
                    LibTab.BITS -> items(lib.bits, key = { it.id }) { b ->
                        ItemCard(b.name, b.describe(), { editing = Editing.BitEdit(b) }) {
                            deleting = Deleting(b.name) { app.deleteBit(b.id) }
                        }
                    }
                    LibTab.BASES -> items(lib.bases, key = { it.id }) { b ->
                        val flat = b.centerToFlat?.let { " · плоска ${it.mm()}" } ?: ""
                        ItemCard(b.name, "Ø ${b.diameter.mm()} mm$flat", { editing = Editing.BaseEdit(b) }) {
                            deleting = Deleting(b.name) { app.deleteBase(b.id) }
                        }
                    }
                }
            }
        }
    }

    when (val e = editing) {
        is Editing.JigEdit -> JigDialog(e.jig, onDismiss = { editing = null }) { app.save(it); editing = null }
        is Editing.BitEdit -> BitDialog(e.bit, onDismiss = { editing = null }) { app.save(it); editing = null }
        is Editing.BaseEdit -> BaseDialog(e.base, onDismiss = { editing = null }) { app.save(it); editing = null }
        null -> Unit
    }
    deleting?.let { d ->
        ConfirmDeleteDialog(d.name, onDismiss = { deleting = null }, onConfirm = { d.action(); deleting = null })
    }
}

private fun Bit.describe(): String = when (kind) {
    BitKind.DOVETAIL -> buildString {
        append("Лястовича · Ø ${diameter.mm()}")
        angleDeg?.let { append(" · ${it.mm()}°") }
        cuttingHeight?.let { append(" · h ${it.mm()}") }
    }
    BitKind.STRAIGHT_BEARING -> buildString {
        append("Прав · Ø ${diameter.mm()}")
        bearingDiameter?.let { append(" · лагер ${it.mm()}") }
        cuttingHeight?.let { append(" · L ${it.mm()}") }
    }
}

@Composable
private fun ItemCard(title: String, subtitle: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Изтрий") }
        }
    }
}

// ─── Диалози за редакция ────────────────────────────────────────────────

private data class EdField(val key: String, val label: String, val required: Boolean = true, val unit: String = "mm")

/** Общ диалог: име + числови полета. Записът е разрешен само когато всичко задължително е валидно. */
@Composable
private fun EditorDialog(
    title: String,
    initialName: String,
    initialValues: Map<String, String>,
    fields: List<EdField>,
    header: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, values: Map<String, Double?>) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val values = remember { mutableStateMapOf<String, String>().apply { putAll(initialValues) } }
    fun valid(f: EdField): Boolean {
        val t = values[f.key].orEmpty()
        return if (t.isBlank()) !f.required else parseMeasure(t) != null
    }
    val canSave = name.isNotBlank() && fields.all(::valid)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Име") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                header?.invoke()
                fields.forEach { f ->
                    val t = values[f.key].orEmpty()
                    OutlinedTextField(
                        value = t,
                        onValueChange = { values[f.key] = it },
                        label = { Text(if (f.required) f.label else "${f.label} (по избор)") },
                        suffix = { Text(f.unit) },
                        singleLine = true,
                        isError = t.isNotBlank() && parseMeasure(t) == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(name.trim(), fields.associate { it.key to parseMeasure(values[it.key].orEmpty()) }) },
            ) { Text("Запази") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отказ") } },
    )
}

private fun Double?.text() = this?.asInput() ?: ""

@Composable
private fun JigDialog(jig: Jig?, onDismiss: () -> Unit, onSave: (Jig) -> Unit) = EditorDialog(
    title = if (jig == null) "Нов шаблон" else "Шаблон",
    initialName = jig?.name ?: "",
    initialValues = mapOf(
        "finger" to jig?.finger.text(),
        "slot" to jig?.slot.text(),
        "frontMin" to jig?.frontMin.text(),
        "frontMax" to jig?.frontMax.text(),
        "sideMin" to jig?.sideMin.text(),
        "sideMax" to jig?.sideMax.text(),
    ),
    fields = listOf(
        EdField("finger", "Метален пръст"),
        EdField("slot", "Прорез"),
        EdField("frontMin", "Лице — мин. дебелина", required = false),
        EdField("frontMax", "Лице — макс. дебелина", required = false),
        EdField("sideMin", "Страни — мин. дебелина", required = false),
        EdField("sideMax", "Страни — макс. дебелина", required = false),
    ),
    onDismiss = onDismiss,
) { name, v ->
    onSave(Jig(jig?.id ?: newId(), name, v["finger"]!!, v["slot"]!!, v["frontMin"], v["frontMax"], v["sideMin"], v["sideMax"]))
}

private fun Jig.subtitle(): String {
    val base = "Пръст ${finger.mm()} · прорез ${slot.mm()} mm"
    fun r(min: Double?, max: Double?) = when {
        min != null && max != null -> "${min.mm()}–${max.mm()}"
        min != null -> "≥${min.mm()}"
        max != null -> "≤${max.mm()}"
        else -> null
    }
    val front = r(frontMin, frontMax)?.let { "лице $it" }
    val side = r(sideMin, sideMax)?.let { "страни $it" }
    val ranges = listOfNotNull(front, side)
    return if (ranges.isEmpty()) base else base + "\n" + ranges.joinToString(" · ") + " mm"
}

@Composable
private fun BaseDialog(base: RouterBase?, onDismiss: () -> Unit, onSave: (RouterBase) -> Unit) = EditorDialog(
    title = if (base == null) "Нова основа" else "Основа на фрезер",
    initialName = base?.name ?: "",
    initialValues = mapOf("d" to base?.diameter.text(), "flat" to base?.centerToFlat.text()),
    fields = listOf(EdField("d", "Диаметър на основата"), EdField("flat", "Център до плоската страна", required = false)),
    onDismiss = onDismiss,
) { name, v -> onSave(RouterBase(base?.id ?: newId(), name, v["d"]!!, v["flat"])) }

@Composable
private fun BitDialog(bit: Bit?, onDismiss: () -> Unit, onSave: (Bit) -> Unit) {
    var kind by remember { mutableStateOf(bit?.kind ?: BitKind.DOVETAIL) }
    val fields = when (kind) {
        BitKind.DOVETAIL -> listOf(
            EdField("d", "Диаметър на върха"),
            EdField("angle", "Ъгъл спрямо оста", required = false, unit = "°"),
            EdField("h", "Височина на режещата част", required = false),
        )
        BitKind.STRAIGHT_BEARING -> listOf(
            EdField("d", "Диаметър на фрезера"),
            EdField("bearing", "Диаметър на лагера"),
            EdField("h", "Режеща дължина", required = false),
        )
    }
    // key(kind): при смяна на вида диалогът се пресъздава с правилните полета, стойностите се пазят
    androidx.compose.runtime.key(kind) {
        EditorDialog(
            title = if (bit == null) "Нов фрезер" else "Фрезер",
            initialName = bit?.name ?: "",
            initialValues = mapOf(
                "d" to bit?.diameter.text(),
                "angle" to bit?.angleDeg.text(),
                "h" to bit?.cuttingHeight.text(),
                "bearing" to bit?.bearingDiameter.text(),
            ),
            fields = fields,
            header = {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(kind == BitKind.DOVETAIL, { kind = BitKind.DOVETAIL }, label = { Text("Лястовича") })
                    FilterChip(kind == BitKind.STRAIGHT_BEARING, { kind = BitKind.STRAIGHT_BEARING }, label = { Text("Прав с лагер") })
                }
            },
            onDismiss = onDismiss,
        ) { name, v ->
            onSave(
                Bit(
                    id = bit?.id ?: newId(),
                    name = name,
                    kind = kind,
                    diameter = v["d"]!!,
                    angleDeg = if (kind == BitKind.DOVETAIL) v["angle"] else null,
                    cuttingHeight = v["h"],
                    bearingDiameter = if (kind == BitKind.STRAIGHT_BEARING) v["bearing"] else null,
                ),
            )
        }
    }
}
