package bg.workshop.dovetail.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

data class PresetOption(val id: String, val label: String, val selected: Boolean)

/**
 * Избор на инструмент от библиотеката. Избраният е този, чиито стойности съвпадат
 * с полетата; ако никой не съвпада — стойностите са ръчни и могат да се запазят.
 */
@Composable
fun PresetPicker(
    title: String,
    options: List<PresetOption>,
    onPick: (String) -> Unit,
    onSaveCurrent: (String) -> Boolean,
    saveTitle: String,
) {
    var saving by rememberSaveable { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val anySelected = options.any { it.selected }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o ->
                FilterChip(
                    selected = o.selected,
                    onClick = { onPick(o.id) },
                    label = { Text(o.label) },
                    leadingIcon = if (o.selected) ({ Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }) else null,
                )
            }
            if (!anySelected) {
                AssistChip(
                    onClick = { saving = true },
                    label = { Text("Запази текущите") },
                    leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) },
                )
            }
        }
        if (failed) {
            Text("Попълни всички стойности, преди да запазиш.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (saving) {
        NameDialog(
            title = saveTitle,
            initial = "",
            onDismiss = { saving = false },
            onConfirm = { name ->
                failed = !onSaveCurrent(name)
                saving = false
            },
        )
    }
}

@Composable
fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Име") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text("Запази") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отказ") } },
    )
}

@Composable
fun ConfirmDeleteDialog(what: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изтриване") },
        text = { Text("Да изтрия ли „$what“? Това не може да се върне.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Изтрий", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отказ") } },
    )
}
