package zechs.zplex.adapter.browse

import androidx.annotation.Keep
import zechs.zplex.models.tmdb.entities.Genre

sealed class BrowseDataModel {

    @Keep
    data class Meta(
        val title: String
    ) : BrowseDataModel()

    @Keep
    data class Browse(
        val header: String,
        val genres: List<Genre>
    ) : BrowseDataModel()

}