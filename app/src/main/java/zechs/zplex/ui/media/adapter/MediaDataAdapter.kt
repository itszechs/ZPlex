package zechs.zplex.ui.media.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import zechs.zplex.R
import zechs.zplex.databinding.*

class MediaDataAdapter(
    val mediaClickListener: MediaClickListener
) : ListAdapter<MediaDataModel, MediaViewHolder>(MediaDataModelDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): MediaViewHolder {

        val headerViewHolder = MediaViewHolder.HeaderViewHolder(
            itemBinding = ItemMediaHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            ),
            mediaDataAdapter = this
        )

        val titleViewHolder = MediaViewHolder.TitleViewHolder(
            itemBinding = ItemMediaTitleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )

        val lastSeasonViewHolder = MediaViewHolder.LatestSeasonViewHolder(
            itemBinding = ItemMediaSeasonBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            ),
            mediaDataAdapter = this
        )

        val partOfCollectionViewHolder = MediaViewHolder.PartOfCollectionViewHolder(
            itemBinding = ItemMediaCollectionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            ),
            mediaDataAdapter = this
        )

        val buttonViewHolder = MediaViewHolder.ButtonViewHolder(
            itemBinding = ItemMediaButtonsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            ),
            mediaDataAdapter = this
        )

        val listViewHolder = MediaViewHolder.ListViewHolder(
            itemBinding = ItemListWithHeadingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            ),
            mediaDataAdapter = this
        )

        return when (viewType) {
            R.layout.item_media_header -> headerViewHolder
            R.layout.item_media_title -> titleViewHolder
            R.layout.item_media_season -> lastSeasonViewHolder
            R.layout.item_media_collection -> partOfCollectionViewHolder
            R.layout.item_media_buttons -> buttonViewHolder
            R.layout.item_list_with_heading -> listViewHolder
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val item = getItem(position)

        when (holder) {
            is MediaViewHolder.HeaderViewHolder -> holder.bind(item as MediaDataModel.Header)
            is MediaViewHolder.TitleViewHolder -> holder.bind(item as MediaDataModel.Title)
            is MediaViewHolder.LatestSeasonViewHolder -> holder.bind(item as MediaDataModel.LatestSeason)
            is MediaViewHolder.PartOfCollectionViewHolder -> holder.bind(item as MediaDataModel.PartOfCollection)
            is MediaViewHolder.ButtonViewHolder -> {
                when (item) {
                    is MediaDataModel.ShowButton -> holder.bindShow(item)
                    is MediaDataModel.MovieButton -> holder.bindMovie(item)
                    else -> {}
                }
            }
            is MediaViewHolder.ListViewHolder -> {
                when (item) {
                    is MediaDataModel.Casts -> holder.bindCasts(item)
                    is MediaDataModel.Recommendations -> holder.bindRecommendations(item)
                    is MediaDataModel.MoreFromCompany -> holder.bindMoreFromCompany(item)
                    is MediaDataModel.Videos -> holder.bindVideos(item)
                    else -> {}
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MediaDataModel.Header -> R.layout.item_media_header
            is MediaDataModel.Title -> R.layout.item_media_title
            is MediaDataModel.LatestSeason -> R.layout.item_media_season
            is MediaDataModel.PartOfCollection -> R.layout.item_media_collection

            is MediaDataModel.ShowButton,
            is MediaDataModel.MovieButton
            -> R.layout.item_media_buttons

            is MediaDataModel.Casts,
            is MediaDataModel.Recommendations,
            is MediaDataModel.MoreFromCompany,
            is MediaDataModel.Videos
            -> R.layout.item_list_with_heading
        }
    }
}