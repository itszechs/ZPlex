package zechs.zplex.adapter.home.adapter.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import zechs.zplex.databinding.ItemMediaBinding
import zechs.zplex.models.tmdb.entities.Media

class MediaAdapter(
    val mediaOnClick: (Media) -> Unit
) : RecyclerView.Adapter<MediaViewHolder>() {

    private val differCallback = object : DiffUtil.ItemCallback<Media>() {
        override fun areItemsTheSame(oldItem: Media, newItem: Media): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Media, newItem: Media): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ) = MediaViewHolder(
        itemBinding = ItemMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        ),
        mediaAdapter = this
    )

    override fun onBindViewHolder(
        holder: MediaViewHolder, position: Int
    ) {
        val media = differ.currentList[position]
        holder.bind(media)
    }

    override fun getItemCount() = differ.currentList.size
}