package bg.workshop.dovetail.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bg.workshop.dovetail.data.LibraryRepository
import bg.workshop.dovetail.data.newProject
import bg.workshop.dovetail.geometry.JointType
import bg.workshop.dovetail.model.Bit
import bg.workshop.dovetail.model.Jig
import bg.workshop.dovetail.model.Library
import bg.workshop.dovetail.model.RouterBase
import bg.workshop.dovetail.ui.home.HomeScreen
import bg.workshop.dovetail.ui.library.LibraryScreen
import bg.workshop.dovetail.ui.setup.SetupScreen
import bg.workshop.dovetail.ui.setup.SetupViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface Screen {
    data object Home : Screen
    data object Library : Screen
    data class Setup(val projectId: String) : Screen
}

class AppViewModel(private val repo: LibraryRepository) : ViewModel() {
    val library: StateFlow<Library> = repo.library

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    fun home() { _screen.value = Screen.Home }
    fun showLibrary() { _screen.value = Screen.Library }
    fun open(projectId: String) { _screen.value = Screen.Setup(projectId) }

    fun create(joint: JointType) {
        val project = newProject(repo.library.value, joint, System.currentTimeMillis())
        repo.update { it.upsert(project) }
        open(project.id)
    }

    fun deleteProject(id: String) = repo.update { it.deleteProject(id) }
    fun save(jig: Jig) = repo.update { it.upsert(jig) }
    fun save(bit: Bit) = repo.update { it.upsert(bit) }
    fun save(base: RouterBase) = repo.update { it.upsert(base) }
    fun deleteJig(id: String) = repo.update { it.deleteJig(id) }
    fun deleteBit(id: String) = repo.update { it.deleteBit(id) }
    fun deleteBase(id: String) = repo.update { it.deleteBase(id) }
}

@Composable
fun AppRoot(repo: LibraryRepository) {
    val app: AppViewModel = viewModel { AppViewModel(repo) }
    val screen by app.screen.collectAsStateWithLifecycle()
    when (val s = screen) {
        Screen.Home -> HomeScreen(app)
        Screen.Library -> {
            BackHandler { app.home() }
            LibraryScreen(app, onBack = app::home)
        }
        is Screen.Setup -> {
            if (repo.library.value.project(s.projectId) == null) {
                app.home()
            } else {
                val vm: SetupViewModel = viewModel(key = s.projectId) { SetupViewModel(repo, s.projectId) }
                SetupScreen(vm, onClose = app::home)
            }
        }
    }
}
