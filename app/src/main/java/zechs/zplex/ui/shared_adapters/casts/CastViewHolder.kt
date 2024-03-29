package zechs.zplex.ui.shared_adapters.casts

import androidx.recyclerview.widget.RecyclerView
import coil.load
import zechs.zplex.R
import zechs.zplex.data.model.ProfileSize
import zechs.zplex.data.model.tmdb.entities.Cast
import zechs.zplex.databinding.ItemCastBinding
import zechs.zplex.utils.Constants.TMDB_IMAGE_PREFIX

class CastViewHolder(
    private val itemBinding: ItemCastBinding,
    val castAdapter: CastAdapter
) : RecyclerView.ViewHolder(itemBinding.root) {

    fun bind(cast: Cast) {
        val imageUrl = if (cast.profile_path != null) {
            "${TMDB_IMAGE_PREFIX}/${ProfileSize.w185}${cast.profile_path}"
        } else {
            R.drawable.no_actor
        }
        itemBinding.apply {
            actorImage.load(imageUrl) {
                placeholder(R.drawable.no_actor)
            }
            actorName.text = cast.name
            role.text = cast.character.split("/")[0]
            root.setOnClickListener {
                castAdapter.castOnClick.invoke(cast)
            }
        }
    }
}