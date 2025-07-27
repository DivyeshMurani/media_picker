package com.io.media.crop

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var imageBitmap: Bitmap? = null
    private var imageMatrix = Matrix()
    private var cropRect = RectF()
    private var imageRect = RectF()
    
    private var scaleFactor = 1.0f
    private var rotationAngle = 0f
    private var translateX = 0f
    private var translateY = 0f
    
    private var aspectRatio = 0f // 0 means free aspect ratio
    private var isDragging = false
    private var isResizing = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var resizeCorner = -1 // -1 = none, 0 = top-left, 1 = top-right, 2 = bottom-left, 3 = bottom-right

    private var pendingBitmap: Bitmap? = null


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (pendingBitmap != null) {
            setImageBitmap(pendingBitmap!!)
            pendingBitmap = null
        }
    }

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
    
    private val cropPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor("#80000000")
    }
    
    private val cornerPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    
    private val cornerStrokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.BLACK
    }

    fun setImageBitmap(bitmap: Bitmap) {

        if (width == 0 || height == 0) {
            pendingBitmap = bitmap
            return
        }

        imageBitmap = bitmap
        resetCrop()
        invalidate()
    }

    fun setAspectRatio(ratio: Float) {
        aspectRatio = ratio
        updateCropRect()
        invalidate()
    }

    fun resetCrop() {



        imageBitmap?.let { bitmap ->
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            Log.d("developer","imageWidth ::: $imageWidth imageHeight ::: $imageHeight viewWidth ::: $viewWidth viewHeight ::: $viewHeight aspectRatio ::: $aspectRatio")

            
            // Calculate scale to fit image in view
            val scaleX = viewWidth / imageWidth
            val scaleY = viewHeight / imageHeight
            scaleFactor = minOf(scaleX, scaleY) * 0.8f // 80% of max fit
            
            // Center the image
            translateX = (viewWidth - imageWidth * scaleFactor) / 2
            translateY = (viewHeight - imageHeight * scaleFactor) / 2
            
            updateImageRect()
            updateCropRect()
            invalidate()
        }?:run {
            Log.d("developer","imageBitmap is null")
        }
    }

    fun rotateImage() {
        rotationAngle += 90f
        if (rotationAngle >= 360f) rotationAngle = 0f
        updateImageRect()
        updateCropRect()
        invalidate()
    }

    private fun updateImageRect() {
        imageBitmap?.let { bitmap ->
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            
            imageMatrix.reset()
            imageMatrix.postTranslate(translateX, translateY)
            imageMatrix.postScale(scaleFactor, scaleFactor)
            imageMatrix.postRotate(rotationAngle, viewWidth / 2, viewHeight / 2)
            
            imageRect.set(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            imageMatrix.mapRect(imageRect)
        }
    }

    private fun updateCropRect() {
        val centerX = width / 2f
        val centerY = height / 2f
        val cropSize = minOf(width, height) * 0.6f
        
        if (aspectRatio > 0) {
            // Fixed aspect ratio
            val cropWidth = cropSize
            val cropHeight = cropWidth / aspectRatio
            cropRect.set(
                centerX - cropWidth / 2,
                centerY - cropHeight / 2,
                centerX + cropWidth / 2,
                centerY + cropHeight / 2
            )
        } else {
            // Free aspect ratio
            cropRect.set(
                centerX - cropSize / 2,
                centerY - cropSize / 2,
                centerX + cropSize / 2,
                centerY + cropSize / 2
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        imageBitmap?.let { bitmap ->
            // Draw image
            canvas.save()
            canvas.concat(imageMatrix)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            canvas.restore()
            
            // Draw crop overlay
            drawCropOverlay(canvas)
        }
    }

    private fun drawCropOverlay(canvas: Canvas) {
        // Draw semi-transparent overlay
        val path = Path()
        path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        path.addRect(cropRect, Path.Direction.CCW)
        canvas.drawPath(path, cropPaint)
        
        // Draw crop border
        canvas.drawRect(cropRect, paint)
        
        // Draw corner indicators with better visibility
        val cornerSize = 25f
        val cornerLength = 50f
        
        // Top-left corner
        canvas.drawRect(cropRect.left, cropRect.top, cropRect.left + cornerSize, cropRect.top + cornerSize, cornerPaint)
        canvas.drawRect(cropRect.left, cropRect.top, cropRect.left + cornerLength, cropRect.top + cornerSize, cornerPaint)
        canvas.drawRect(cropRect.left, cropRect.top, cropRect.left + cornerSize, cropRect.top + cornerLength, cornerPaint)
        // Add stroke for better visibility
        canvas.drawRect(cropRect.left, cropRect.top, cropRect.left + cornerSize, cropRect.top + cornerSize, cornerStrokePaint)
        
        // Top-right corner
        canvas.drawRect(cropRect.right - cornerSize, cropRect.top, cropRect.right, cropRect.top + cornerSize, cornerPaint)
        canvas.drawRect(cropRect.right - cornerLength, cropRect.top, cropRect.right, cropRect.top + cornerSize, cornerPaint)
        canvas.drawRect(cropRect.right - cornerSize, cropRect.top, cropRect.right, cropRect.top + cornerLength, cornerPaint)
        canvas.drawRect(cropRect.right - cornerSize, cropRect.top, cropRect.right, cropRect.top + cornerSize, cornerStrokePaint)
        
        // Bottom-left corner
        canvas.drawRect(cropRect.left, cropRect.bottom - cornerSize, cropRect.left + cornerSize, cropRect.bottom, cornerPaint)
        canvas.drawRect(cropRect.left, cropRect.bottom - cornerSize, cropRect.left + cornerLength, cropRect.bottom, cornerPaint)
        canvas.drawRect(cropRect.left, cropRect.bottom - cornerLength, cropRect.left + cornerSize, cropRect.bottom, cornerPaint)
        canvas.drawRect(cropRect.left, cropRect.bottom - cornerSize, cropRect.left + cornerSize, cropRect.bottom, cornerStrokePaint)
        
        // Bottom-right corner
        canvas.drawRect(cropRect.right - cornerSize, cropRect.bottom - cornerSize, cropRect.right, cropRect.bottom, cornerPaint)
        canvas.drawRect(cropRect.right - cornerLength, cropRect.bottom - cornerSize, cropRect.right, cropRect.bottom, cornerPaint)
        canvas.drawRect(cropRect.right - cornerSize, cropRect.bottom - cornerLength, cropRect.right, cropRect.bottom, cornerPaint)
        canvas.drawRect(cropRect.right - cornerSize, cropRect.bottom - cornerSize, cropRect.right, cropRect.bottom, cornerStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y


                // Check if touch is on a corner
                resizeCorner = getCornerAtPoint(event.x, event.y)
                if (resizeCorner != -1) {
                    isResizing = true
                } else if (isPointInCropRect(event.x, event.y)) {
                    isDragging = true
                }

                Log.d("developer","lastTouchX ::: $lastTouchX lastTouchY ::: $lastTouchY")



                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val deltaX = event.x - lastTouchX
                    val deltaY = event.y - lastTouchY
                    
                    // Move crop rect
                    cropRect.offset(deltaX, deltaY)
                    
                    // Keep crop rect within bounds
                    cropRect.left = maxOf(0f, cropRect.left)
                    cropRect.top = maxOf(0f, cropRect.top)
                    cropRect.right = minOf(width.toFloat(), cropRect.right)
                    cropRect.bottom = minOf(height.toFloat(), cropRect.bottom)
                    
                    lastTouchX = event.x
                    lastTouchY = event.y

                    Log.d("developer","lastTouchX ::: $lastTouchX lastTouchY ::: $lastTouchY")

                    invalidate()
                } else if (isResizing) {
                    val deltaX = event.x - lastTouchX
                    val deltaY = event.y - lastTouchY
                    
                    // Resize crop rect based on corner
                    resizeCropRect(deltaX, deltaY)
                    
                    lastTouchX = event.x
                    lastTouchY = event.y

                    Log.d("developer","lastTouchX ::: $lastTouchX lastTouchY ::: $lastTouchY")

                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                isDragging = false
                isResizing = false
                resizeCorner = -1
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getCornerAtPoint(x: Float, y: Float): Int {
        val cornerSize = 40f
        val touchArea = 60f // Larger touch area for easier interaction

        // Top-left corner
        if (x >= cropRect.left - touchArea && x <= cropRect.left + touchArea &&
            y >= cropRect.top - touchArea && y <= cropRect.top + touchArea) {
            return 0
        }

        // Top-right corner
        if (x >= cropRect.right - touchArea && x <= cropRect.right + touchArea &&
            y >= cropRect.top - touchArea && y <= cropRect.top + touchArea) {
            return 1
        }

        // Bottom-left corner
        if (x >= cropRect.left - touchArea && x <= cropRect.left + touchArea &&
            y >= cropRect.bottom - touchArea && y <= cropRect.bottom + touchArea) {
            return 2
        }

        // Bottom-right corner
        if (x >= cropRect.right - touchArea && x <= cropRect.right + touchArea &&
            y >= cropRect.bottom - touchArea && y <= cropRect.bottom + touchArea) {
            return 3
        }

        return -1
    }

    private fun isPointInCropRect(x: Float, y: Float): Boolean {
        return x >= cropRect.left && x <= cropRect.right &&
                y >= cropRect.top && y <= cropRect.bottom
    }

        private fun resizeCropRect(deltaX: Float, deltaY: Float) {
        val minSize = 100f // Minimum crop size
        val maxWidth = width.toFloat()
        val maxHeight = height.toFloat()
        
        when (resizeCorner) {
            0 -> { // Top-left corner
                if (aspectRatio > 0) {
                    // Maintain aspect ratio - calculate based on width change
                    val newLeft = cropRect.left + deltaX
                    val newWidth = cropRect.right - newLeft
                    val newHeight = newWidth / aspectRatio
                    val newTop = cropRect.bottom - newHeight
                    
                    // Apply constraints
                    if (newLeft >= 0f && newTop >= 0f && newWidth >= minSize && newHeight >= minSize) {
                        cropRect.left = newLeft
                        cropRect.top = newTop
                    }
                } else {
                    // Free aspect ratio
                    val newLeft = maxOf(0f, minOf(cropRect.right - minSize, cropRect.left + deltaX))
                    val newTop = maxOf(0f, minOf(cropRect.bottom - minSize, cropRect.top + deltaY))
                    cropRect.left = newLeft
                    cropRect.top = newTop
                }
            }
            1 -> { // Top-right corner
                if (aspectRatio > 0) {
                    // Maintain aspect ratio - calculate based on width change
                    val newRight = cropRect.right + deltaX
                    val newWidth = newRight - cropRect.left
                    val newHeight = newWidth / aspectRatio
                    val newTop = cropRect.bottom - newHeight
                    
                    // Apply constraints
                    if (newRight <= maxWidth && newTop >= 0f && newWidth >= minSize && newHeight >= minSize) {
                        cropRect.right = newRight
                        cropRect.top = newTop
                    }
                } else {
                    // Free aspect ratio
                    val newRight = minOf(maxWidth, maxOf(cropRect.left + minSize, cropRect.right + deltaX))
                    val newTop = maxOf(0f, minOf(cropRect.bottom - minSize, cropRect.top + deltaY))
                    cropRect.right = newRight
                    cropRect.top = newTop
                }
            }
            2 -> { // Bottom-left corner
                if (aspectRatio > 0) {
                    // Maintain aspect ratio - calculate based on width change
                    val newLeft = cropRect.left + deltaX
                    val newWidth = cropRect.right - newLeft
                    val newHeight = newWidth / aspectRatio
                    val newBottom = cropRect.top + newHeight
                    
                    // Apply constraints
                    if (newLeft >= 0f && newBottom <= maxHeight && newWidth >= minSize && newHeight >= minSize) {
                        cropRect.left = newLeft
                        cropRect.bottom = newBottom
                    }
                } else {
                    // Free aspect ratio
                    val newLeft = maxOf(0f, minOf(cropRect.right - minSize, cropRect.left + deltaX))
                    val newBottom = minOf(maxHeight, maxOf(cropRect.top + minSize, cropRect.bottom + deltaY))
                    cropRect.left = newLeft
                    cropRect.bottom = newBottom
                }
            }
            3 -> { // Bottom-right corner
                if (aspectRatio > 0) {
                    // Maintain aspect ratio - calculate based on width change
                    val newRight = cropRect.right + deltaX
                    val newWidth = newRight - cropRect.left
                    val newHeight = newWidth / aspectRatio
                    val newBottom = cropRect.top + newHeight
                    
                    // Apply constraints
                    if (newRight <= maxWidth && newBottom <= maxHeight && newWidth >= minSize && newHeight >= minSize) {
                        cropRect.right = newRight
                        cropRect.bottom = newBottom
                    }
                } else {
                    // Free aspect ratio
                    val newRight = minOf(maxWidth, maxOf(cropRect.left + minSize, cropRect.right + deltaX))
                    val newBottom = minOf(maxHeight, maxOf(cropRect.top + minSize, cropRect.bottom + deltaY))
                    cropRect.right = newRight
                    cropRect.bottom = newBottom
                }
            }
        }
    }

    fun getCroppedBitmap(): Bitmap? {
        val bitmap = imageBitmap ?: return null

        // Map cropRect (view coordinates) to image coordinates
        val inverseMatrix = Matrix()
        imageMatrix.invert(inverseMatrix)
        val imageCropRect = RectF(cropRect)
        inverseMatrix.mapRect(imageCropRect)

        // Clamp to bitmap bounds
        imageCropRect.left = imageCropRect.left.coerceIn(0f, bitmap.width.toFloat())
        imageCropRect.top = imageCropRect.top.coerceIn(0f, bitmap.height.toFloat())
        imageCropRect.right = imageCropRect.right.coerceIn(0f, bitmap.width.toFloat())
        imageCropRect.bottom = imageCropRect.bottom.coerceIn(0f, bitmap.height.toFloat())

        // Calculate width and height, ensure at least 1 pixel
        val cropX = imageCropRect.left.roundToInt()
        val cropY = imageCropRect.top.roundToInt()
        val cropWidth = max(1, (imageCropRect.width()).roundToInt())
        val cropHeight = max(1, (imageCropRect.height()).roundToInt())


        // 4. Ensure crop area is within bitmap bounds
        if (cropX < 0 || cropY < 0 || cropX + cropWidth > bitmap.width || cropY + cropHeight > bitmap.height) {
            // Log for debugging

        }

        Log.d("developer","Crop\", \"Crop area out of bounds: x=$cropX y=$cropY w=$cropWidth h=$cropHeight bitmap=${bitmap.width}x${bitmap.height}")

        // Ensure crop area is within bitmap bounds
        if (cropX + cropWidth > bitmap.width || cropY + cropHeight > bitmap.height) {
            return null
        }

        return try {
            Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
        } catch (e: Exception) {
            null
        }
    }

//    fun getCroppedBitmap(): Bitmap? {
//        imageBitmap?.let { bitmap ->
//            // Calculate the crop area in image coordinates
//            val inverseMatrix = Matrix()
//            imageMatrix.invert(inverseMatrix)
//
//            val imageCropRect = RectF(cropRect)
//            inverseMatrix.mapRect(imageCropRect)
//
//            // Ensure crop rect is within image bounds
//            imageCropRect.left = maxOf(0f, imageCropRect.left)
//            imageCropRect.top = maxOf(0f, imageCropRect.top)
//            imageCropRect.right = minOf(bitmap.width.toFloat(), imageCropRect.right)
//            imageCropRect.bottom = minOf(bitmap.height.toFloat(), imageCropRect.bottom)
//
//            // Create cropped bitmap
//            return try {
//                Bitmap.createBitmap(
//                    bitmap,
//                    imageCropRect.left.toInt(),
//                    imageCropRect.top.toInt(),
//                    imageCropRect.width().toInt(),
//                    imageCropRect.height().toInt()
//                )
//            } catch (e: Exception) {
//                null
//            }
//        }
//        return null
//    }
} 