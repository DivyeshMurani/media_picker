package com.io.media.viewmodel

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.io.media.model.MediaModel
import com.io.media.util.write
import kotlinx.coroutines.flow.MutableSharedFlow

class MediaViewModel : ViewModel() {

    val liveOfArrayOfMedia = MutableLiveData<ArrayList<MediaModel>>()
    val liveOfTotalPage = MutableSharedFlow<Int>(1)
    val hadShowFab = MutableLiveData<Int>(0)
    var mediaMaxSelect = 1
    var mediaExtension = ""


    fun getImagesWithPaths(
        contentResolver: ContentResolver, offset: Int, limit: Int, type: Int = 1,
        format: String?=null
    ) {

        Log.d("developer","format :$format")

        val arrayOfMedia = ArrayList<MediaModel>()
        val imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE
        )

        val selectionBuilder = StringBuilder("${MediaStore.Images.Media.SIZE} > 0")
        val selectionArgs = mutableListOf<String>()

        if (format != null) {
            when (format.lowercase()) {
                "jpg", "jpeg" -> {
                    selectionBuilder.append(" AND ${MediaStore.Images.Media.MIME_TYPE}=?")
                    selectionArgs.add("image/jpeg")
                }
                "png" -> {
                    selectionBuilder.append(" AND ${MediaStore.Images.Media.MIME_TYPE}=?")
                    selectionArgs.add("image/png")
                }
            }
        }

        Log.d("developer","${selectionArgs.toString()}")



        // Add selection to filter out invalid images
//        val selection = "${MediaStore.Images.Media.SIZE} > 0"
        val selection = selectionBuilder.toString()


        // Sort by date, but remove the LIMIT/OFFSET
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                imagesUri, projection, selection, if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray(), sortOrder
            )?.use { cursor ->

                Log.e("developer", "cursor.count :: ${cursor.count}")

                liveOfTotalPage.tryEmit(cursor.count)

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                // Move to the offset position
                if (offset > 0) {
                    cursor.moveToPosition(offset - 1)
                }

                var count = 0
                // Only process 'limit' number of items
                while (cursor.moveToNext() && count < limit) {
                    val imageId = cursor.getLong(idColumn)
                    val filePath = cursor.getString(dataColumn)
                    arrayOfMedia.add(
                        MediaModel(
                            imageId.toString(), filePath
                        )
                    )
                    count++
                }
            }

            if (arrayOfMedia.isNotEmpty()) {

                if (liveOfArrayOfMedia.value.isNullOrEmpty()) {
                    liveOfArrayOfMedia.value = arrayOfMedia
                } else {

//                    val arrayOf = liveOfArrayOfMedia.value
//                    liveOfArrayOfMedia.value = arrayOfMedia

                    val arrayOf = ArrayList<MediaModel>()
                    liveOfArrayOfMedia.value?.forEach {
                        arrayOf.add(it.copy())
                    }

                    arrayOfMedia.forEach {
                        arrayOf.add(it.copy())
                    }

                    liveOfArrayOfMedia.value = arrayOf
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            write("Exception :: ${e.message}")
        }
    }



    fun getVideosWithPaths(
        contentResolver: ContentResolver, offset: Int, limit: Int
    ) {
        val arrayOfMedia = ArrayList<MediaModel>()
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.TITLE
        )






        val selection = "${MediaStore.Video.Media.SIZE} > 0"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                videoUri, projection, selection, null, sortOrder
            )?.use { cursor ->
                Log.e("developer", "cursor.count :: ${cursor.count}")
                liveOfTotalPage.tryEmit(cursor.count)

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)

                if (offset > 0) {
                    cursor.moveToPosition(offset - 1)
                }

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idColumn)
                    val path = cursor.getString(dataColumn)
                    val duration = cursor.getLong(durationColumn)
                    val title = cursor.getString(titleColumn)

                    arrayOfMedia.add(
                        MediaModel(
                            id = id.toString(),
                            path = path,
                            duration = duration,
//                            title = title
                        )
                    )
                    count++
                }
            }

            if (arrayOfMedia.isNotEmpty()) {
                val currentList = liveOfArrayOfMedia.value ?: ArrayList()
                val newList = ArrayList<MediaModel>().apply {
                    addAll(currentList.map { it.copy() })
                    addAll(arrayOfMedia.map { it.copy() })
                }
                liveOfArrayOfMedia.value = newList
            }
        } catch (e: Exception) {
            e.printStackTrace()
            write("Exception :: ${e.message}")
        }
    }



    fun getVideoImagePaths(
        contentResolver: ContentResolver, offset: Int, limit: Int
    ) {

        val arrayOfMedia = ArrayList<MediaModel>()
        val uri = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED
        )

//        val selectionBuilder = StringBuilder("${MediaStore.Images.Media.SIZE} > 0")

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"

//        selectionBuilder.append("$selection1")
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )





        Log.d("developer","${selectionArgs.toString()}")



        // Add selection to filter out invalid images
//        val selection = "${MediaStore.Images.Media.SIZE} > 0"
//        val selection = selectionBuilder.toString()


        // Sort by date, but remove the LIMIT/OFFSET
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                uri, projection, selection, if (selectionArgs.isEmpty()) null else selectionArgs, sortOrder
            )?.use { cursor ->

                Log.e("developer", "cursor.count :: ${cursor.count}")

                liveOfTotalPage.tryEmit(cursor.count)

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)


                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)




                // Move to the offset position
                if (offset > 0) {
                    cursor.moveToPosition(offset - 1)
                }

                var count = 0
                // Only process 'limit' number of items
                while (cursor.moveToNext() && count < limit) {
                    val imageId = cursor.getLong(idColumn)
                    val filePath = cursor.getString(dataColumn)
                    arrayOfMedia.add(
                        MediaModel(
                            imageId.toString(), filePath
                        )
                    )
                    count++
                }
            }

            if (arrayOfMedia.isNotEmpty()) {

                if (liveOfArrayOfMedia.value.isNullOrEmpty()) {
                    liveOfArrayOfMedia.value = arrayOfMedia
                } else {

//                    val arrayOf = liveOfArrayOfMedia.value
//                    liveOfArrayOfMedia.value = arrayOfMedia

                    val arrayOf = ArrayList<MediaModel>()
                    liveOfArrayOfMedia.value?.forEach {
                        arrayOf.add(it.copy())
                    }

                    arrayOfMedia.forEach {
                        arrayOf.add(it.copy())
                    }

                    liveOfArrayOfMedia.value = arrayOf
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            write("Exception :: ${e.message}")
        }
    }





    fun getPDFFiles2(
        contentResolver: ContentResolver, offset: Int, limit: Int
    ) {
        val arrayOfMedia = ArrayList<MediaModel>()
        val uri = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        // Filter for PDF files
        val selection =
            "${MediaStore.Files.FileColumns.MIME_TYPE} = ? AND ${MediaStore.Files.FileColumns.SIZE} > 0"
        val selectionArgs = arrayOf("application/pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                uri, projection, selection, selectionArgs, sortOrder
            )?.use { cursor ->
                Log.e("developer", "PDF cursor.count :: ${cursor.count}")
                liveOfTotalPage.tryEmit(cursor.count)

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

                if (offset > 0) {
                    cursor.moveToPosition(offset - 1)
                }

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idColumn)
                    val path = cursor.getString(dataColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)

                    arrayOfMedia.add(
                        MediaModel(
                            id = id.toString(),
                            path = path,
//                            title = name,
//                            size = size,
//                            type = MediaType.PDF
                        )
                    )
                    count++
                }
            }

            if (arrayOfMedia.isNotEmpty()) {
                val currentList = liveOfArrayOfMedia.value ?: ArrayList()
                val newList = ArrayList<MediaModel>().apply {
                    addAll(currentList.map { it.copy() })
                    addAll(arrayOfMedia.map { it.copy() })
                }
                liveOfArrayOfMedia.value = newList
            }
        } catch (e: Exception) {
            e.printStackTrace()
            write("Exception :: ${e.message}")
        }
    }

    fun getPDFFiles(
        contentResolver: ContentResolver, offset: Int, limit: Int
    ) {
        val arrayOfMedia = ArrayList<MediaModel>()

        try {
            // Query for PDF files using DocumentsContract
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATA
            )

            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
            val selectionArgs = arrayOf("application/pdf")
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            contentResolver.query(
                collection, projection, selection, selectionArgs, sortOrder
            )?.use { cursor ->
                Log.d("PDFFiles", "Found ${cursor.count} PDF files")
                liveOfTotalPage.tryEmit(cursor.count)

                if (offset > 0) {
                    cursor.moveToPosition(offset - 1)
                }

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val path = cursor.getString(dataColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Files.getContentUri("external"), id
                    )

                    arrayOfMedia.add(
                        MediaModel(
                            id = id.toString(),
                            path = path,
//                            title = name,
//                            size = size,
//                            type = MediaType.PDF,
//                            uri = contentUri
                        )
                    )
                    count++
                }
            } ?: {
                write("Null")
            }

            if (arrayOfMedia.isNotEmpty()) {
                val currentList = liveOfArrayOfMedia.value ?: ArrayList()
                val newList = ArrayList<MediaModel>().apply {
                    addAll(currentList.map { it.copy() })
                    addAll(arrayOfMedia.map { it.copy() })
                }
                liveOfArrayOfMedia.value = newList
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PDFFiles", "Error: ${e.message}")
        }
    }

    fun doSelect(indexOf: Int, hasSingle: Boolean = true) {

        val arrayOf = liveOfArrayOfMedia.value
        val array = ArrayList<MediaModel>()

        arrayOf?.forEachIndexed { index, modelOf ->

            if (index == indexOf) {
                array.add(modelOf.copy(hasSelected = modelOf.getSelected()))
            } else {
                array.add(modelOf.copy())
            }
        }

        val totalSelectCount = (array.filter {
            it.hasSelected == 1
        }.size ?: 0)
        if (totalSelectCount > 0) {
            hadShowFab.value = 1
        } else {
            hadShowFab.value = 0
        }

        if (totalSelectCount == mediaMaxSelect + 1) {
            return
        }

        liveOfArrayOfMedia.value = array


    }


// ... existing code ...
}