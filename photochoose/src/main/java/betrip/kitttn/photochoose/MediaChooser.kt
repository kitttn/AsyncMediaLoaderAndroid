package betrip.kitttn.photochoose

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.properties.Delegates


/**
 * @author kitttn
 */

typealias Handler<T> = (T) -> Unit

class MediaChooserFragment : Fragment() {
    private val TAG = "MediaChooserFragment"
    private val MAX_SIZE = 1280.0
    private var handler: Handler<Bitmap> = {}
    private var errorHandler: Handler<String> = {}
    private var fileHandler: Handler<File> = {}

    private var imageFile by Delegates.notNull<File>()

    companion object {
        const val GALLERY_REQ_CODE = 1
        const val CAMERA_REQ_CODE = 2

        fun newInstance(errorHandler: Handler<String> = {},
                        bitmapHandler: Handler<Bitmap> = {},
                        fileHandler: Handler<File> = {}): MediaChooserFragment {

            val fragment = MediaChooserFragment()
            fragment.errorHandler = errorHandler
            fragment.handler = bitmapHandler
            fragment.fileHandler = fileHandler

            return fragment
        }
    }

    // ======================== public methods ================

    fun onBitmapLoaded(handler: (Bitmap) -> Unit) {
        this.handler = handler
    }

    fun showError(errorMessage: String) {
        Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show()
        errorHandler(errorMessage)
    }

    fun openCamera() {
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        requestPermissions(permissions, CAMERA_REQ_CODE)
    }

    fun openGallery() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        requestPermissions(permissions, GALLERY_REQ_CODE)
    }

    // ========================== Camera request and results =======================

    private fun launchCamera() {
        val saveTo = createOutputImageFile()
        val photoUri = FileProvider.getUriForFile(activity, "photochooser.fileprovider", saveTo)
        val readAndWrite = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        Log.i(TAG, "launchCamera: Calling package: ${activity.callingPackage}")
        if (activity.callingPackage != null)
            activity.grantUriPermission(activity.callingPackage, photoUri, readAndWrite)

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

        if (takePictureIntent.resolveActivity(activity.packageManager) != null)
            startActivityForResult(takePictureIntent, CAMERA_REQ_CODE)
    }

    private fun createOutputImageFile(): File {
        val timestamp = System.currentTimeMillis()
        val fileName = "JPEG_${timestamp}_"
        val storageDir = activity.filesDir
        imageFile = File.createTempFile(fileName, ".jpg", storageDir)
        return imageFile
    }

    private fun parseImageFromCamera() {
        launch(UI) {
            val fis = FileInputStream(imageFile)
            val encoded = async { getBitmap(fis.fd) }
            fis.close()

            if (encoded == null) {
                showError("Can't load this image, try another one!")
                return@launch
            }
            handler(encoded)
            val file = async { compressBitmap(encoded) }
            fileHandler(file)
        }
    }

    // ======================= Gallery results ========================

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, GALLERY_REQ_CODE)
    }

    private fun parseImageFromGallery(uri: Uri) {
        launch(UI) {
            val fd = activity.contentResolver.openFileDescriptor(uri, "r")
            val encoded = async { getBitmap(fd.fileDescriptor) }
            fd.close()

            if (encoded == null) {
                showError("Can't load this image, try another one!")
                return@launch
            }

            handler(encoded)
            val file = async { compressBitmap(encoded) }
            fileHandler(file)
        }
    }

    // ================================ permissions and results ================================

    private suspend fun getBitmap(fd: FileDescriptor): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFileDescriptor(fd, null, options)

        var sampleSize = 1
        val mostSize = maxOf(options.outWidth, options.outHeight)
        while (MAX_SIZE * sampleSize < mostSize) sampleSize *= 2

        val otherOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = sampleSize
        }

        val encoded = BitmapFactory.decodeFileDescriptor(fd, null, otherOptions)
        return encoded
    }

    private suspend fun compressBitmap(bitmap: Bitmap): File {
        val file = createOutputImageFile()
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
        Log.i(TAG, "onActivityResult: Bitmap size: ${bitmap.width}x${bitmap.height}")
        fos.close()
        return file
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val success = grantResults.sum() == PackageManager.PERMISSION_GRANTED
        if (!success) {
            showError("You denied permission, please, try again!")
            return
        }

        when (requestCode) {
            GALLERY_REQ_CODE -> launchGallery()
            CAMERA_REQ_CODE -> launchCamera()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == GALLERY_REQ_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Log.i(TAG, "onActivityResult: Result code: $resultCode; Data: $data")
            parseImageFromGallery(data.data)
        }

        if (requestCode == CAMERA_REQ_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Log.i(TAG, "onActivityResult: Result code: $resultCode; Data: $data")
            parseImageFromCamera()
        }
    }
}

suspend fun <T> async(block: suspend () -> T): T {
    return async(CommonPool) { block() }.await()
}