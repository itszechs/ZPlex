package zechs.zplex.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_episode.view.*
import zechs.zplex.R
import zechs.zplex.models.tmdb.StillSize
import zechs.zplex.models.zplex.Episode
import zechs.zplex.utils.Constants.TMDB_IMAGE_PREFIX
import zechs.zplex.utils.GlideApp


class EpisodesAdapter : RecyclerView.Adapter<EpisodesAdapter.EpisodesViewHolder>() {

    inner class EpisodesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val differCallback = object : DiffUtil.ItemCallback<Episode>() {
        override fun areItemsTheSame(oldItem: Episode, newItem: Episode): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Episode, newItem: Episode): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodesViewHolder {
        return EpisodesViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_episode, parent, false
            )
        )
    }

    override fun getItemCount() = differ.currentList.size

    override fun onBindViewHolder(holder: EpisodesViewHolder, position: Int) {
        val episode = differ.currentList[position]

        val episodeStillUrl = if (episode.still_path.isNullOrEmpty()) {
            R.drawable.no_thumb
        } else {
            "${TMDB_IMAGE_PREFIX}/${StillSize.w300}${episode.still_path}"
        }

        val count = "Episode ${episode.episode_number}"

        holder.itemView.apply {
            frame_not_available.isVisible = episode.file_id == null
            if (episode.name == count) {
                tv_episodeCount.tag = position
            }

            tv_episodeCount.isInvisible = tv_episodeCount.tag == position

            tv_title.text = episode.name.ifEmpty { "No title" }
            tv_episodeCount.text = count
            tv_overview.text = if (episode.overview.isNullOrEmpty()) {
                "No description"
            } else episode.overview

            GlideApp.with(this)
                .asBitmap()
                .load(episodeStillUrl)
                .placeholder(R.drawable.no_thumb)
                .into(iv_backdrop)

            if (episode.file_id != null) {
                setOnClickListener {
                    onItemClickListener?.let { it(episode) }
                }
            }
        }

    }

    private var onItemClickListener: ((Episode) -> Unit)? = null

    fun setOnItemClickListener(listener: (Episode) -> Unit) {
        onItemClickListener = listener
    }

}