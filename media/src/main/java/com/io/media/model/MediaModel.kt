package com.io.media.model

import java.util.concurrent.TimeUnit

data class MediaModel(
    val id: String? = null,
    val path: String? = null,
    val duration: Long = 0L,
    val hasSelected: Int = 0,
){
    fun getDurationFormatted(): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun getSelected():Int {
        if (hasSelected==0) {
            return 1
        }
        return 0

    }


}
