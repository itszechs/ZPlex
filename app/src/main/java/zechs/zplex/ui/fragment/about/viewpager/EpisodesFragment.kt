package zechs.zplex.ui.fragment.about.viewpager

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.makeText
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import zechs.zplex.R
import zechs.zplex.adapter.MediaAdapter
import zechs.zplex.databinding.FragmentEpisodesBinding
import zechs.zplex.models.drive.DriveResponse
import zechs.zplex.models.drive.File
import zechs.zplex.ui.activity.PlayerActivity
import zechs.zplex.ui.activity.ZPlexActivity
import zechs.zplex.ui.fragment.ArgsViewModel
import zechs.zplex.ui.fragment.about.AboutViewModel
import zechs.zplex.utils.Constants.ZPLEX
import zechs.zplex.utils.Resource
import java.lang.Integer.parseInt
import java.net.*

class EpisodesFragment : Fragment(R.layout.fragment_episodes) {

    private var _binding: FragmentEpisodesBinding? = null
    private val binding get() = _binding!!

    private lateinit var aboutViewModel: AboutViewModel
    private lateinit var mediaAdapter: MediaAdapter

    private lateinit var groupedList: Map<String, List<File>>
    private val thisTAG = "EpisodesFragment"

    private val argsModel: ArgsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEpisodesBinding.bind(view)

        aboutViewModel = (activity as ZPlexActivity).aboutViewModel

        argsModel.args.observe(viewLifecycleOwner, { arg ->
            binding.btnRetryEpisodes.setOnClickListener {
                val driveQuery = "name contains 'mkv' " +
                        "and '${arg.file.id}'" +
                        " in parents and trashed = false"

                aboutViewModel.getMediaFiles(driveQuery)
            }

            mediaAdapter = MediaAdapter(arg.mediaId)
            mediaAdapter.setOnItemClickListener {
                playMedia(it, arg.mediaId, arg.name)
            }

            binding.rvEpisodes.apply {
                adapter = mediaAdapter
                layoutManager = GridLayoutManager(activity, 2)
            }
        })

        filesLoading()

        aboutViewModel.mediaList.observe(viewLifecycleOwner, { responseMedia ->
            when (responseMedia) {
                is Resource.Success -> {
                    responseMedia.data?.let { filesResponse ->
                        filesSuccess(filesResponse)
                    }
                }

                is Resource.Error -> {
                    filesError(responseMedia)
                }

                is Resource.Loading -> {
                    filesLoading()
                }
            }
        })

    }

    private fun filesSuccess(filesResponse: DriveResponse) {

        if (filesResponse.files.isNotEmpty()) {
            val filesList = filesResponse.files.toList()

            groupedList = filesList.groupBy {
                val season = it.name.take(3)
                val first = season.take(1).replace("S", "Season ")
                val count = parseInt(season.drop(1))
                first + count
            }

            var seasonIndex = 0
            val seasons = groupedList.keys.toList()
            mediaAdapter.differ.submitList(groupedList[seasons[seasonIndex]]?.toList())

            binding.apply {
                rvEpisodes.visibility = View.VISIBLE
                btnRetryEpisodes.visibility = View.GONE
                pbEpisodes.visibility = View.GONE
            }

            val filters = listOf("Chronological", "Newest Aired")
            context?.let { context ->
                val listPopUpSeasons =
                    ListPopupWindow(
                        context,
                        null,
                        R.attr.listPopupWindowStyle
                    )
                val listPopUpFilters =
                    ListPopupWindow(
                        context,
                        null,
                        R.attr.listPopupWindowStyle
                    )
                binding.seasonsMenu.apply {

                    visibility = View.VISIBLE
                    text = seasons[seasonIndex]

                    val adapter = ArrayAdapter(
                        context,
                        R.layout.item_dropdown,
                        seasons
                    )

                    listPopUpSeasons.anchorView = this
                    listPopUpSeasons.setAdapter(adapter)

                    setOnClickListener { listPopUpSeasons.show() }

                    listPopUpSeasons.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                        seasonIndex = position
                        mediaAdapter.differ.submitList(groupedList[seasons[seasonIndex]]?.toList())
                        text = seasons[seasonIndex]
                        binding.rvEpisodes.smoothScrollToPosition(0)
                        listPopUpSeasons.dismiss()
                    }

                }

                var reverseList = false

                binding.filterEps.apply {

                    visibility = View.VISIBLE
                    text = filters[0]

                    val adapter = ArrayAdapter(
                        context,
                        R.layout.item_dropdown,
                        filters
                    )

                    listPopUpFilters.anchorView = this
                    listPopUpFilters.setAdapter(adapter)

                    setOnClickListener { listPopUpFilters.show() }

                    listPopUpFilters.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                        reverseList = when (position) {
                            0 -> false
                            1 -> true
                            else -> false
                        }
                        // mediaAdapter.differ.submitList(groupedList[seasons[seasonIndex]]?.toList())
                        binding.rvEpisodes.smoothScrollToPosition(0)
                        text = filters[position]
                        listPopUpFilters.dismiss()
                    }
                }
            }
        }
    }


    private fun filesLoading() {

        val emptyList = listOf<String>().toList()

        context?.let {
            val adapter = ArrayAdapter(
                it, R.layout.item_dropdown, emptyList
            )

            val listPopupWindow = ListPopupWindow(
                it, null, R.attr.listPopupWindowStyle
            )
            listPopupWindow.anchorView = binding.seasonsMenu
            listPopupWindow.setAdapter(adapter)
            binding.seasonsMenu.text = ""
            binding.filterEps.text = ""
        }

        binding.apply {
            rvEpisodes.visibility = View.INVISIBLE
            btnRetryEpisodes.visibility = View.INVISIBLE
            pbEpisodes.visibility = View.VISIBLE
            seasonsMenu.visibility = View.INVISIBLE
            filterEps.visibility = View.INVISIBLE
//            toggleView.visibility = View.GONE
        }
    }

    private fun filesError(responseMedia: Resource.Error<DriveResponse>) {
        binding.apply {
            rvEpisodes.visibility = View.INVISIBLE
            btnRetryEpisodes.visibility = View.VISIBLE
            pbEpisodes.visibility = View.INVISIBLE
            seasonsMenu.visibility = View.INVISIBLE
            filterEps.visibility = View.INVISIBLE
        }
        responseMedia.message?.let { message ->
            makeText(
                context,
                "An error occurred: $message",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(thisTAG, "An error occurred: $message")
        }
    }


    private fun playMedia(it: File, mediaId: Int, name: String) {
        val items = arrayOf("ExoPlayer", "VLC")

        val fullEpisodeTitle = if (name.length > 30) {
            it.name.dropLast(4)
        } else {
            "$name - ${it.name.dropLast(4)}"
        }

        context?.let { it1 ->
            val roundedBg = ContextCompat.getDrawable(
                it1, R.drawable.popup_menu_bg
            )

            MaterialAlertDialogBuilder(it1)
                .setBackground(roundedBg)
                .setTitle("Play using")
                .setItems(items) { dialog, which ->
                    when (which) {
                        0 -> {
                            val intent = Intent(activity, PlayerActivity::class.java)
                            intent.putExtra("fileId", it.id)
                            intent.putExtra("title", fullEpisodeTitle)
                            intent.flags = FLAG_ACTIVITY_NEW_TASK
                            activity?.startActivity(intent)
                            dialog.dismiss()
                        }
                        1 -> {
                            val playUrl = "${ZPLEX}${mediaId} - $name - TV/${it.name}"

                            try {
                                val episodeURL = URL(playUrl)
                                val episodeURI = URI(
                                    episodeURL.protocol,
                                    episodeURL.userInfo,
                                    IDN.toASCII(episodeURL.host),
                                    episodeURL.port,
                                    episodeURL.path,
                                    episodeURL.query,
                                    episodeURL.ref
                                ).toASCIIString().replace("?", "%3F")

                                val vlcIntent = Intent(Intent.ACTION_VIEW)
                                vlcIntent.setPackage("org.videolan.vlc")
                                vlcIntent.component = ComponentName(
                                    "org.videolan.vlc",
                                    "org.videolan.vlc.gui.video.VideoPlayerActivity"
                                )
                                vlcIntent.setDataAndTypeAndNormalize(
                                    Uri.parse(episodeURI),
                                    "video/*"
                                )
                                vlcIntent.putExtra("title", fullEpisodeTitle)
                                vlcIntent.flags =
                                    FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                                requireContext().startActivity(vlcIntent)
                            } catch (notFoundException: ActivityNotFoundException) {
                                notFoundException.printStackTrace()
                                makeText(
                                    context,
                                    "VLC not found, Install VLC from Play Store",
                                    LENGTH_LONG
                                ).show()
                            } catch (e: MalformedURLException) {
                                e.printStackTrace()
                                makeText(
                                    context, e.localizedMessage, LENGTH_LONG
                                ).show()
                            } catch (e: URISyntaxException) {
                                e.printStackTrace()
                                makeText(
                                    context, e.localizedMessage, LENGTH_LONG
                                ).show()
                            }
                            dialog.dismiss()
                        }
                    }
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvEpisodes.adapter = null
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        binding.root.requestLayout()
    }

}