package com.seif.facemaskdetector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.renderscript.*
import androidx.camera.extensions.BuildConfig
import java.nio.ByteBuffer

// this class used to convert image object from Y′UV444toRGB888 to bitmap
class YuvToRgbConverter(context: Context) {

    private val rs = RenderScript.create(context)
    private val scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
    private lateinit var yuvBuffer: ByteBuffer
    private lateinit var inputAllocation: Allocation
    private lateinit var outputAllocation: Allocation
    private var pixelCount: Int = -1

    // this method is built to achieve the same fps as the camerax image analysis use case on a pixel 3x device.
    @Synchronized
    fun yuvToRgb(image: Image, output: Bitmap) {
        // checking that the intermediate output by the buffer is allocated
        if (!::yuvBuffer.isInitialized) {
            // croping it accourding to her size (pixel*pixel is abbreviation for the whole image)
            // so it's useful to compute the size of the full buffer
            // but it shouldn't be used to determine the pixel of bits
            pixelCount = image.cropRect.width() * image.cropRect.height()
            val pixelSizeBits =
                ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) // convert it to bitmap
            yuvBuffer = ByteBuffer.allocateDirect(pixelCount * pixelSizeBits)
        }
        // we have to remind the buffer no need to clear it since it will be fail
        yuvBuffer.rewind()
        imageToByteBuffer(image, yuvBuffer.array())
        // input allocation
        if (!::inputAllocation.isInitialized) {
            val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
            inputAllocation = Allocation.createSized(rs, elemType.element, yuvBuffer.array().size)

        }
        if (!::outputAllocation.isInitialized) {
            outputAllocation = Allocation.createFromBitmap(rs, output)
        }
        inputAllocation.copyFrom(yuvBuffer.array())
        scriptYuvToRgb.setInput(inputAllocation)
        scriptYuvToRgb.forEach(outputAllocation)
        outputAllocation.copyTo(output)

    }

    private fun imageToByteBuffer(image: Image, outputBuffer: ByteArray) {
        // we have to check for  the values that are in a readable form for each input and output.
        if (BuildConfig.DEBUG && image.format != ImageFormat.YUV_420_888) {
            error("Assertion Failure")
        }
        val imageCrop = image.cropRect
        val imagePlanes = image.planes
        // how many values are in read for input and for each output return
        imagePlanes.forEachIndexed { planeIndex, plane ->
            val outputStride: Int
            var outputOffset: Int
            when (planeIndex) {
                0 -> {
                    outputStride = 1
                    outputOffset = 0
                }
                1 -> {
                    outputStride = 2
                    outputOffset = pixelCount + 1 // we added 1 bec v is odd
                }
                2 -> {
                    outputStride = 2
                    outputOffset = pixelCount
                }
                else ->{
                    return@forEachIndexed
                }

            }
            val planeBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val planeCrop = if(planeIndex ==0){
                imageCrop
            }
            else{
                Rect(
                    imageCrop.left/2,
                    imageCrop.top/2,
                    imageCrop.right/2,
                    imageCrop.bottom/2
                )
            }
             val planeWidth = planeCrop.width()
            val planeHeight = planeCrop.height()
            val rowBuffer = ByteArray(plane.rowStride)
            val rowLength = if (pixelStride ==1 && outputStride == 1){
                planeWidth
            }
            else{
                (planeWidth-1)*pixelStride+1
            }
            for (row in 0 until planeHeight){
                (row + planeCrop.top)*rowStride + planeCrop.left*pixelStride
            }
            if (pixelStride==1 && outputStride ==1){
                planeBuffer.get(outputBuffer, outputOffset, rowLength)
                outputOffset+=rowLength
            }
            else{
                planeBuffer.get(rowBuffer, 0 , rowLength)
                for (col in 0 until planeWidth){
                    outputBuffer[outputOffset] = rowBuffer[col*pixelStride]
                    outputOffset+= outputStride
                }
            }
        }
    }
}

// The color encoding system used for analog television worldwide (NTSC, PAL and SECAM).
// The YUV color space (color model) differs from RGB,
// which is what the camera captures and what humans view.
// When color signals were developed in the 1950s,
// it was decided to allow black and white TVs to continue to receive and decode monochrome signals,
// while color sets would decode both monochrome and color signals.

// advantages to use Yuv colors:
//YUV color-spaces are a more efficient coding and reduce the bandwidth more than RGB capture can.
// Most video cards, therefore, render directly using YUV or luminance/chrominance images.
// The most important component for YUV capture is always the luminance, or Y component.
// For this reason, Y should have the highest sampling rate, or the same rate as the other components.

// To convert from Yuv to Rgb: The function [R, G, B] = Y′UV444toRGB888(Y′, U, V) converts Y′UV format to simple RGB format.