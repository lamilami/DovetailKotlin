package bg.workshop.dovetail.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import bg.workshop.dovetail.data.title
import bg.workshop.dovetail.ui.components.NameDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val StepItemWidth = 78.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(vm: SetupViewModel, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lib by vm.library.collectAsStateWithLifecycle()
    var renaming by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (state.stepIndex > 0) vm.back() else onClose() }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Към началото") }
                },
                title = {
                    Column(Modifier.clickable { renaming = true }) {
                        Text(state.projectName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(state.joint.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { renaming = true }) { Icon(Icons.Filled.Edit, "Преименувай") }
                },
            )
        },
        bottomBar = { StepNavigation(state.steps, state.stepIndex, onBack = vm::back, onNext = vm::next) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            StepIndicator(steps = state.steps, current = state.step, onSelect = vm::goTo)
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally { it * dir / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it * dir / 4 } + fadeOut())
                },
                label = "step",
            ) { step ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (step) {
                        Step.STOPS -> StopsStep(state, lib, vm)
                        Step.PARTS -> PartsStep(state, vm)
                        Step.CLAMP -> ClampStep(state, vm)
                        Step.BACKSTOP -> BackStopStep(state, lib, vm)
                        Step.BIT -> BitStep(state, lib, vm)
                        Step.CUT -> CutStep(state)
                        Step.TRIAL -> TrialStep(state)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (renaming) {
        NameDialog(
            title = "Име на проекта",
            initial = state.projectName,
            onDismiss = { renaming = false },
            onConfirm = { vm.rename(it); renaming = false },
        )
    }
}

@Composable
private fun StepIndicator(steps: List<Step>, current: Step, onSelect: (Step) -> Unit) {
    val scroll = rememberScrollState()
    val itemPx = with(LocalDensity.current) { StepItemWidth.roundToPx() }
    val currentIndex = steps.indexOf(current).coerceAtLeast(0)
    LaunchedEffect(current) {
        scroll.animateScrollTo((currentIndex * itemPx - itemPx).coerceAtLeast(0))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        steps.forEachIndexed { i, s ->
            val active = s == current
            val done = i < currentIndex
            val scheme = MaterialTheme.colorScheme
            Column(
                Modifier
                    .width(StepItemWidth)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(s) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(
                            when {
                                active -> scheme.primary
                                done -> scheme.primaryContainer
                                else -> scheme.surfaceVariant
                            },
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${i + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            active -> scheme.onPrimary
                            done -> scheme.onPrimaryContainer
                            else -> scheme.onSurfaceVariant
                        },
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    s.short,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) scheme.onSurface else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StepNavigation(steps: List<Step>, index: Int, onBack: () -> Unit, onNext: () -> Unit) {
    val last = index == steps.lastIndex
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onBack, enabled = index > 0, modifier = Modifier.weight(1f).height(56.dp)) {
                Text("Назад", style = MaterialTheme.typography.titleMedium)
            }
            Button(onClick = onNext, enabled = !last, modifier = Modifier.weight(1f).height(56.dp)) {
                val next = steps.getOrNull(index + 1)
                Text(next?.short?.let { "$it →" } ?: "Готово", style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
    }
}
