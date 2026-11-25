package bg.workshop.dovetail

import android.app.Application
import bg.workshop.dovetail.data.LibraryRepository
import kotlinx.coroutines.MainScope
import java.io.File

class DovetailApp : Application() {
    val repository: LibraryRepository by lazy {
        LibraryRepository(File(filesDir, "library.json"), MainScope())
    }
}
