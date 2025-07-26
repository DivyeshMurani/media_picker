package com.io.media.picker

import android.content.Context
import android.content.Intent
import com.io.media.MediaActivity
import com.io.media.model.MediaModel
import java.io.Serializable

class MediaPicker private constructor(private val builder: Builder) {

    private val config: MediaConfig = builder.config
    private val callback: MediaCallback = builder.callback

    private fun start() {

        val intent = Intent(builder.mContext, MediaActivity::class.java)
        intent.putExtra("data", builder.config)
        builder.mContext.startActivity(intent)

    }

    class Builder(context: Context) {

        internal var config: MediaConfig = MediaConfig()
            private set
        internal var callback: MediaCallback = DefaultCallback()
            private set
        internal var mContext: Context = context
            private set

        fun setConfig(config: MediaConfig) = apply {
            this.config = config
        }

//        fun setCallback(callback: MediaCallback) = apply {
//            this.callback = callback
//        }

        fun setMediaType(type: MediaType) = apply {
            this.config = config.copy(mediaType = type)
        }

        fun enableMultiSelect(enable: Boolean = true) = apply {
            this.config = config.copy(enableMultiSelect = enable)
        }

        fun setMaxItems(max: Int) = apply {
            this.config = config.copy(maxItems = max)
        }

        fun setMediaExtension(max: MediaExtension) = apply {
            this.config = config.copy(mediaExtension = max)
        }

        // Add suspend function to get results
        suspend fun pickMedia(): List<MediaModel> {
            build().start()
            return MediaPickerChannel.receive()
        }


        private fun build(): MediaPicker {
            return MediaPicker(this)
        }

    }

    data class MediaConfig(
        val mediaType: MediaType = MediaType.VIDEO,
        val maxItems: Int = Int.MAX_VALUE,
        val enableMultiSelect: Boolean = false,
        val mediaExtension: MediaExtension = MediaExtension.ALL,

    ) : Serializable

    enum class MediaType {
        VIDEO, IMAGE,ALL
    }

    enum class MediaExtension {
        JPEG, PNG, ALL
    }

    interface MediaCallback {
        fun onMediaSelected(items: List<MediaModel>)
    }

    private class DefaultCallback : MediaCallback {
        override fun onMediaSelected(items: List<MediaModel>) {}
    }
}