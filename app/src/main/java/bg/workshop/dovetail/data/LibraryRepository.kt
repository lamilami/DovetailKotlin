package bg.workshop.dovetail.data

import android.util.Log
import bg.workshop.dovetail.model.Library
import bg.workshop.dovetail.model.LibraryCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Цялата библиотека (инструменти + проекти) в един JSON файл в паметта на приложението.
 * Промените се записват с кратко забавяне; при спиране на приложението — веднага.
 */
class LibraryRepository(private val file: File, private val scope: CoroutineScope) {
    private val _library = MutableStateFlow(load())
    val library: StateFlow<Library> = _library.asStateFlow()

    private var saveJob: Job? = null
    private val lock = Any()

    fun update(transform: (Library) -> Library) {
        _library.update(transform)
        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.IO) {
            delay(400)
            write(_library.value)
        }
    }

    /** Синхронен запис — извиква се от onStop, за да не се губят последните промени. */
    fun flush() {
        saveJob?.cancel()
        write(_library.value)
    }

    private fun load(): Library {
        if (!file.exists()) return Library.seed()
        return try {
            LibraryCodec.decode(file.readText()).migrate()
        } catch (e: Exception) {
            // Не презаписваме повреден файл — пазим го до новия, за да не се губят данни.
            val backup = File(file.parentFile, "library.corrupt-${System.currentTimeMillis()}.json")
            file.renameTo(backup)
            Log.w("LibraryRepository", "Повреден файл, преместен в ${backup.name}", e)
            Library.seed()
        }
    }

    private fun write(library: Library) = synchronized(lock) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(LibraryCodec.encode(library))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
