package com.io.media.picker

import com.io.media.model.MediaModel
import com.io.media.util.write
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel

object MediaPickerChannel {
    private var channel = Channel<List<MediaModel>>(Channel.BUFFERED)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun send(media: List<MediaModel>) {
        try {
            // Check if channel is closed, if so create a new one
            if (channel.isClosedForSend) {
                channel = Channel(Channel.BUFFERED)
            }
            channel.send(media)
        } catch (e: Exception) {
            write("")
            // Handle send error
            e.printStackTrace()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun receive(): List<MediaModel> {
        return try {
            // Check if channel is closed, if so create a new one
            if (channel.isClosedForReceive) {
                channel = Channel(Channel.BUFFERED)
            }
            channel.receive()
        } catch (e: Exception) {
            // If channel was cancelled, create new one and wait for result
            channel = Channel(Channel.BUFFERED)
            channel.receive()
        }
    }

    // Optional: Clear channel when needed
    fun clear() {
        channel.cancel()
    }
}