package zechs.zplex.ui.fragment.episodes

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import zechs.zplex.repository.FilesRepository
import zechs.zplex.repository.TmdbRepository

@Suppress("UNCHECKED_CAST")
class EpisodesViewModelProviderFactory(
    val app: Application,
    private val filesRepository: FilesRepository,
    private val tmdbRepository: TmdbRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EpisodesViewModel(app, filesRepository, tmdbRepository) as T
    }

}