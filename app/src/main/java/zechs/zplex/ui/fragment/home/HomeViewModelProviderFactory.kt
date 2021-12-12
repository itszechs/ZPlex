package zechs.zplex.ui.fragment.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import zechs.zplex.repository.FilesRepository
import zechs.zplex.repository.ReleasesRepository

@Suppress("UNCHECKED_CAST")
class HomeViewModelProviderFactory(
    val app: Application,
    private val filesRepository: FilesRepository,
    private val releasesRepository: ReleasesRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(
            app,
            filesRepository,
            releasesRepository
        ) as T
    }

}