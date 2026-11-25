package bg.workshop.dovetail.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bg.workshop.dovetail.data.fieldInputs
import bg.workshop.dovetail.data.title
import bg.workshop.dovetail.geometry.Field
import bg.workshop.dovetail.geometry.JointType
import bg.workshop.dovetail.model.Project
import bg.workshop.dovetail.ui.AppViewModel
import bg.workshop.dovetail.ui.components.ConfirmDeleteDialog
import bg.workshop.dovetail.ui.diagram.JointIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("bg"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(app: AppViewModel) {
    val lib by app.library.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Project?>(null) }
    val projects = lib.recentProjects()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Работилница", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = app::showAbout) { Icon(Icons.Filled.Info, "Относно") }
                },
                actions = {
                    TextButton(onClick = app::showLibrary) {
                        Icon(Icons.Filled.Build, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Инструменти")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle("Нова сглобка") }
            item {
                JointCard(
                    JointType.DOVETAIL,
                    "Полускрита, за чекмеджета. Фрезер „лястовича опашка“.",
                    onClick = { app.create(JointType.DOVETAIL) },
                )
            }
            item {
                JointCard(
                    JointType.FINGER,
                    "Finger joint, за кутии. Прав фрезер с копирен лагер.",
                    onClick = { app.create(JointType.FINGER) },
                )
            }
            item { Spacer(Modifier.height(4.dp)); SectionTitle("Проекти") }
            if (projects.isEmpty()) {
                item {
                    Text(
                        "Още няма проекти. Започни нова сглобка — настройките се пазят автоматично.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(projects, key = { it.id }) { p ->
                ProjectCard(p, onOpen = { app.open(p.id) }, onDelete = { deleting = p })
            }
        }
    }

    deleting?.let { p ->
        ConfirmDeleteDialog(
            what = p.name,
            onDismiss = { deleting = null },
            onConfirm = { app.deleteProject(p.id); deleting = null },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun JointCard(joint: JointType, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            JointIcon(joint)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    joint.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun ProjectCard(p: Project, onOpen: () -> Unit, onDelete: () -> Unit) {
    val inputs = p.fieldInputs()
    val size = listOfNotNull(inputs[Field.BOARD_WIDTH], inputs[Field.THICKNESS])
        .filter { it.isNotBlank() }
        .joinToString(" × ")
        .let { if (it.isEmpty()) "" else "$it mm · " }
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            JointIcon(p.joint, 40.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${p.joint.title} · $size${dateFormat.format(Date(p.updatedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Изтрий") }
        }
    }
}
