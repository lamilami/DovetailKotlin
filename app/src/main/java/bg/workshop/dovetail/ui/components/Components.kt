package bg.workshop.dovetail.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import bg.workshop.dovetail.geometry.Calc
import bg.workshop.dovetail.geometry.Field
import bg.workshop.dovetail.geometry.parseMeasure
import bg.workshop.dovetail.geometry.parseOptionalMeasure
import bg.workshop.dovetail.ui.theme.LocalWorkshopColors

@Composable
fun StepHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Panel(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
fun MeasureField(field: Field, value: String, onChange: (Field, String) -> Unit, modifier: Modifier = Modifier) {
    val invalid = if (field.optional) parseOptionalMeasure(value) == null else value.isNotBlank() && parseMeasure(value) == null
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(field, it) },
        label = { Text(field.label) },
        suffix = { Text(field.unit) },
        singleLine = true,
        isError = invalid,
        supportingText = if (invalid) ({ Text("Положително число до 10 000") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        textStyle = MaterialTheme.typography.titleLarge,
        modifier = modifier.fillMaxWidth(),
    )
}

data class ResultItem(val label: String, val value: String, val unit: String = "mm", val highlight: Boolean = false)

@Composable
fun ResultGrid(items: List<ResultItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { ResultValue(it, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ResultValue(item: ResultItem, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(item.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                item.value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (item.unit.isNotEmpty()) {
                Text(" ${item.unit}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

enum class Tone { Info, Warning, Error }

@Composable
fun Callout(text: String, tone: Tone = Tone.Info) {
    val ws = LocalWorkshopColors.current
    val (bg, fg) = when (tone) {
        Tone.Info -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        Tone.Warning -> ws.warning to ws.onWarning
        Tone.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                if (tone == Tone.Info) Icons.Filled.Info else Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Номериран списък с действия от ръководството. */
@Composable
fun Instructions(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { i, text ->
            Row {
                Box(
                    Modifier
                        .size(26.dp)
                        .padding(top = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${i + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Показва Calc: числата при Ok, причината при Invalid, липсващите полета при NeedsInput. */
@Composable
fun <T> CalcView(calc: Calc<T>, ok: @Composable (T) -> Unit) {
    when (calc) {
        is Calc.Ok -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ok(calc.value)
            calc.warnings.forEach { Callout(it.message, Tone.Warning) }
        }
        is Calc.Invalid -> Callout(calc.reason.message, Tone.Error)
        is Calc.NeedsInput -> Text(
            "Въведи: " + calc.fields.joinToString { it.label.lowercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun DiagramCaption(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}
