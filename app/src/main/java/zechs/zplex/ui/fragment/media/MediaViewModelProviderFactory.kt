package zechs.zplex.ui.fragment.media

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import zechs.zplex.repository.FilesRepository
import zechs.zplex.repository.TmdbRepository


@Suppress("UNCHECKED_CAST")
class MediaViewModelProviderFactory(
    val app: Application,
    private val filesRepository: FilesRepository,
    private val tmdbRepository: TmdbRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MediaViewModel(app, filesRepository, tmdbRepository) as T
    }

}