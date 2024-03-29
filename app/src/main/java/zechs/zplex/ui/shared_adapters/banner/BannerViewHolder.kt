package zechs.zplex.ui.shared_adapters.banner

import androidx.recyclerview.widget.RecyclerView
import coil.load
import zechs.zplex.R
import zechs.zplex.data.model.BackdropSize
import zechs.zplex.data.model.tmdb.entities.Media
import zechs.zplex.databinding.ItemWideBannerBinding
import zechs.zplex.utils.Constants.TMDB_IMAGE_PREFIX
import zechs.zplex.utils.util.BlurTransformation

class BannerViewHolder(
    private val itemBinding: ItemWideBannerBinding,
    val bannerAdapter: BannerAdapter
) : RecyclerView.ViewHolder(itemBinding.root) {

    fun bind(media: Media) {
        val mediaBannerUrl = if (media.backdrop_path == null) {
            R.drawable.no_thumb
        } else {
            "${TMDB_IMAGE_PREFIX}/${BackdropSize.w780}${media.backdrop_path}"
        }

        itemBinding.apply {
            tvTitle.text = media.title
            val rating = media.vote_average?.div(2).toString()
            val ratingText = "$rating/5"
            rbRating.rating = rating.toFloat()
            tvRatingText.text = ratingText

            ivBanner.load(mediaBannerUrl) {
                placeholder(R.drawable.no_thumb)
                transformations(BlurTransformation(25, 1f))
            }

            ivMainBanner.load(mediaBannerUrl) {
                placeholder(R.drawable.no_thumb)
            }

            root.setOnClickListener {
                bannerAdapter.bannerOnClick.invoke(media)
            }
        }
    }
}