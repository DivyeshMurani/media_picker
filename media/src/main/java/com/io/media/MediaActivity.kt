package com.io.media

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.io.media.base.BaseActivity
import com.io.media.base.BaseListAdapter
import com.io.media.databinding.ActivityMediaBinding
import com.io.media.databinding.ItemImageBinding
import com.io.media.model.MediaModel
import com.io.media.picker.MediaPicker
import com.io.media.picker.MediaPicker.MediaType
import com.io.media.picker.MediaPickerChannel
import com.io.media.util.GridSpacingItemDecoration
import com.io.media.util.PaginationScrollListener
import com.io.media.util.PaginationState
import com.io.media.util.write
import com.io.media.viewmodel.MediaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MediaActivity : BaseActivity<ActivityMediaBinding>() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var imageSize: Int = 0
    private val spanCount = 3 // Number of columns in grid

    private var adapter: MediaAdapter? = null
    private var mediaConfig: MediaPicker.MediaConfig? = null

    private val viewModel: MediaViewModel by viewModels()
    private val paginationState = PaginationState()

    override var layoutId: Int
        get() = R.layout.activity_media
        set(value) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure you are using a light or dark theme that supports translucent status bars


        getDataFromIntent()
        initRecyclerview()
        initClickEvent()
        initPermission()
        observer()

    }

    private fun getDataFromIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mediaConfig = intent.getSerializableExtra("data", MediaPicker.MediaConfig::class.java)
        } else {
            mediaConfig = intent.getSerializableExtra("data") as MediaPicker.MediaConfig
        }
        viewModel.mediaMaxSelect = mediaConfig?.maxItems ?: 1
        Log.d("developer", "mediaConfig?.mediaExtension ${mediaConfig?.mediaExtension}")

        when (mediaConfig?.mediaExtension) {
            MediaPicker.MediaExtension.JPEG -> {
                viewModel.mediaExtension = "jpg"

            }

            MediaPicker.MediaExtension.PNG -> {
                viewModel.mediaExtension = "png"

            }

            MediaPicker.MediaExtension.ALL -> {
                viewModel.mediaExtension = ""
            }

            else -> {

            }
        }
    }

    private fun initClickEvent() {

        binding.mFab.setOnClickListener {
            onSelectionComplete()

        }
    }

    private fun initPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            } else {
                initLoadMedia()

            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    android.Manifest.permission.READ_MEDIA_VIDEO
                ) != PackageManager.PERMISSION_GRANTED

            ) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.READ_MEDIA_IMAGES,
                        android.Manifest.permission.READ_MEDIA_VIDEO
                    ), 1
                )
            } else {
                initLoadMedia()
            }
        }
    }

    private fun onSelectionComplete() {
        val array = viewModel.liveOfArrayOfMedia.value?.filter {
            it.hasSelected == 1
        }

        if (array?.isNullOrEmpty()?.not() == true) {
            scope.launch {
                try {
                    MediaPickerChannel.send(array)
                    finish()
                } catch (e: Exception) {
                    write("MediaPickerChannel E :${e.message}")
                }
            }
        }
    }

    private fun initRecyclerview() {
        adapter = MediaAdapter()

        imageSize = calculateImageSize()
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)

        val gridLayoutManager = GridLayoutManager(this, spanCount)

        binding.mRecyclerView.apply {
            layoutManager = gridLayoutManager
            addItemDecoration(GridSpacingItemDecoration(3, spacing))

            addOnScrollListener(object : PaginationScrollListener(gridLayoutManager) {
                override fun loadMoreItems() {
                    paginationState.isLoading = true
                    paginationState.currentPage++
                    initLoadMedia()
                }

                override fun isLoading(): Boolean = paginationState.isLoading

                override fun isLastPage(): Boolean = paginationState.isLastPage
            })
            adapter = this@MediaActivity.adapter
        }


        // Initial load
//        initLoadMedia()

    }

    private fun observer() {
        viewModel.liveOfTotalPage.onEach {
            paginationState.totalItem = it

        }.launchIn(CoroutineScope(Dispatchers.IO))

        viewModel.hadShowFab.observe(this) {
            when (it) {
                1 -> {
                    binding.mFab.visibility = View.VISIBLE
                }

                0 -> {
                    binding.mFab.visibility = View.GONE
                }

                else -> {}
            }
        }

        viewModel.liveOfArrayOfMedia.observe(this) { arrayOfMedia ->
            paginationState.isLoading = false
            if (arrayOfMedia.isEmpty() || arrayOfMedia.size < paginationState.itemsPerPage) {
                paginationState.isLastPage = true
            }

            adapter?.submitList(arrayOfMedia)
        }
    }

    private fun initLoadMedia() {
        when (mediaConfig?.mediaType) {

            MediaPicker.MediaType.VIDEO -> {
                viewModel.getVideosWithPaths(
                    contentResolver,
                    paginationState.currentPage * paginationState.itemsPerPage,
                    paginationState.itemsPerPage
                )
            }

            MediaPicker.MediaType.IMAGE -> {
                viewModel.getImagesWithPaths(
                    contentResolver,
                    paginationState.currentPage * paginationState.itemsPerPage,
                    paginationState.itemsPerPage, format = viewModel.mediaExtension
                )
            }

            MediaType.ALL -> {
                viewModel.getVideoImagePaths(
                    contentResolver,
                    paginationState.currentPage * paginationState.itemsPerPage,
                    paginationState.itemsPerPage
                )
            }

            null -> {
            }
        }
    }

    private fun calculateImageSize(): Int {

        // Get screen width
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Calculate size considering spacing
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        write("spacing :: $spacing | screenWidth : $screenWidth | spanCount $spanCount")

        return (screenWidth - (spacing * (spanCount + 1))) / spanCount
    }


    /**
     *  Adapter
     */

    private inner class MediaAdapter :
        BaseListAdapter<MediaModel, ItemImageBinding>(object : DiffUtil.ItemCallback<MediaModel>() {
            override fun areItemsTheSame(oldItem: MediaModel, newItem: MediaModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MediaModel, newItem: MediaModel): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: MediaModel, newItem: MediaModel): Any? {
                val diff = Bundle()
                if (oldItem.hasSelected != newItem.hasSelected) {
                    diff.putString("hasSelected", "hasSelected")
                }
                return diff
//                return super.getChangePayload(oldItem, newItem)
            }
        }) {

        override fun getItemLayoutId(indexOf: Int): Int {
            return R.layout.item_image
        }

        override fun onViewRecycled(holder: BaseViewHolder<ItemImageBinding>) {
            super.onViewRecycled(holder)
            Glide.with(this@MediaActivity).clear(holder.binding.image)
        }

        override fun bind(
            binding: ItemImageBinding, item: MediaModel, indexOf: Int, payloads: MutableList<Any>?
        ) {

            if (payloads.isNullOrEmpty().not()) {
                val bundle = payloads?.get(0) as Bundle
                if (bundle.getString("hasSelected") == "hasSelected") {
                    if (item.hasSelected == 1) {
                        binding.mConstraintLayout.alpha = 0.5F
                        binding.mFrameOfSelection.visibility = View.GONE
                    } else {
                        binding.mConstraintLayout.alpha = 1.0F
                        binding.mFrameOfSelection.visibility = View.GONE
                    }
                    return
                }
            }

            binding.apply {

                image.setOnClickListener {
                    if (mediaConfig?.enableMultiSelect == true) {
                        viewModel.doSelect(indexOf)
                    } else {
                        onSelectionComplete()
                    }
                }

                image.setOnLongClickListener {
                    if (mediaConfig?.mediaType == MediaType.VIDEO) {
                        val i = Intent(this@MediaActivity, MediaPlayerActivity::class.java)
                        i.putExtra("url", item.path)
                        startActivity(i)
                    } else if (mediaConfig?.mediaType == MediaType.ALL) {

                        if (item.path?.contains("mp4") == true) {
                            val i = Intent(this@MediaActivity, MediaPlayerActivity::class.java)
                            i.putExtra("url", item.path)
                            startActivity(i)
                        }
                    }

                    return@setOnLongClickListener true
                }

                binding.tvVID.visibility= View.GONE
                if (mediaConfig?.mediaType == MediaType.VIDEO) {
                    binding.tvVID.visibility= View.VISIBLE


                } else if (mediaConfig?.mediaType == MediaType.ALL) {

                    if (item.path?.contains("mp4") == true) {
                      binding.tvVID.visibility= View.VISIBLE
                    }
                }






                if (item.hasSelected == 1) {
                    binding.mFrameOfSelection.visibility = View.VISIBLE
                } else {
                    binding.mFrameOfSelection.visibility = View.GONE
                }

                Glide.with(this@MediaActivity).load(item.path)
//                    .override(300, 300)
                    .override(imageSize, imageSize).diskCacheStrategy(DiskCacheStrategy.ALL)
                    .thumbnail(0.1f).centerCrop().placeholder(R.drawable.loading_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            image.setImageDrawable(resource)
                            return false
                        }
                    }).into(image)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLoadMedia()
            } else {

                Toast.makeText(this@MediaActivity, "Permission denied", Toast.LENGTH_SHORT).show()
                finish()
            }

        }


    }

    override fun onDestroy() {
        super.onDestroy()
        MediaPickerChannel.clear()
    }
}