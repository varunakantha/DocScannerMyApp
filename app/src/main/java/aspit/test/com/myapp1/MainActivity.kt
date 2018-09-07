package aspit.test.com.myapp1

import android.os.Bundle

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View

import com.dynamsoft.camerasdk.exception.DcsCameraNotAuthorizedException
import com.dynamsoft.camerasdk.exception.DcsException
import com.dynamsoft.camerasdk.exception.DcsValueNotValidException
import com.dynamsoft.camerasdk.exception.DcsValueOutOfRangeException
import com.dynamsoft.camerasdk.io.DcsPDFEncodeParameter
import com.dynamsoft.camerasdk.model.DcsDocument
import com.dynamsoft.camerasdk.model.DcsImage
import com.dynamsoft.camerasdk.view.DcsVideoView
import com.dynamsoft.camerasdk.view.DcsVideoViewListener
import com.dynamsoft.camerasdk.view.DcsView
import com.dynamsoft.camerasdk.view.DcsViewListener

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), DcsViewListener {

    private var mCurrentPhotoPath: String = ""
    lateinit var imageURI: Uri
    private var pdfName: String = ""

    companion object {
        private val CAMERA_OK = 10
        private val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            DcsView.setLicense(applicationContext, "your license number")
        } catch (e: DcsValueNotValidException) {
            e.printStackTrace()
        }

        dcsview_id.currentView = DcsView.DVE_IMAGEGALLERYVIEW
        dcsview_id.setListener(this)

        try {
            dcsview_id.videoView.mode = DcsView.DME_DOCUMENT
        } catch (e: DcsValueOutOfRangeException) {
            e.printStackTrace()
        }

        dcsview_id.videoView.nextViewAfterCancel = DcsView.DVE_IMAGEGALLERYVIEW
        dcsview_id.videoView.nextViewAfterCapture = DcsView.DVE_EDITORVIEW

        tv_show_id.setOnClickListener { dcsview_id.currentView = DcsView.DVE_VIDEOVIEW }
        dcsview_id.videoView.setListener(object : DcsVideoViewListener {

            override fun onPreCapture(dcsVideoView: DcsVideoView): Boolean {
                return true
            }

            override fun onCaptureFailure(dcsVideoView: DcsVideoView, e: DcsException) {
                // do nothing
            }

            override fun onPostCapture(dcsVideoView: DcsVideoView, dcsImage: DcsImage) {
                savePicture(dcsImage.image)
                if (dcsview_id.buffer.currentIndex > 0) {
                    dcsview_id.buffer.delete(0)
                }
                val array = intArrayOf(0)
                dcsview_id.io.save(array, getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.path + "/" + pdfName + ".pdf", DcsPDFEncodeParameter()) //<-Save as a PDF
            }

            override fun onCancelTapped(dcsVideoView: DcsVideoView) {
                // do nothing
            }

            override fun onCaptureTapped(dcsVideoView: DcsVideoView) {
                // do nothing
            }

            override fun onDocumentDetected(dcsVideoView: DcsVideoView, dcsDocument: DcsDocument) {
                // do nothing
            }
        })

    }

    override fun onResume() {
        super.onResume()
        if (dcsview_id.currentView == DcsView.DVE_VIDEOVIEW) {
            try {
                dcsview_id!!.videoView.preview()
            } catch (e: DcsCameraNotAuthorizedException) {
                e.printStackTrace()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestPermissions()
        if (dcsview_id.currentView == DcsView.DVE_VIDEOVIEW) {
            try {
                dcsview_id.videoView.preview()
            } catch (e: DcsCameraNotAuthorizedException) {
                e.printStackTrace()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        dcsview_id.videoView.stopPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        dcsview_id.videoView.destroyCamera()
    }

    override fun onCurrentViewChanged(dcsView: DcsView, lastView: Int, currentView: Int) {

        if (currentView == DcsView.DVE_IMAGEGALLERYVIEW) {
            tv_show_id.visibility = View.VISIBLE
            tv_title_id.visibility = View.VISIBLE
        } else {
            tv_show_id.visibility = View.GONE
            tv_title_id.visibility = View.GONE
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT > 22) {
            try {
                if (ContextCompat.checkSelfPermission(this@MainActivity, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE)
                }
                if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.CAMERA), CAMERA_OK)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else {
            // do nothing
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        try {
            DcsView.setLicense(applicationContext, "your license number")
        } catch (e: DcsValueNotValidException) {
            e.printStackTrace()
        }
    }

    private fun savePicture(documentImage: Bitmap) {

        var photoFile: File? = null

        try {
            photoFile = createImageFile(documentImage)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (photoFile != null) {
            imageURI = FileProvider.getUriForFile(this,
                    "aspit.test.com.myapp1",
                    photoFile)

        }

    }

    @Throws(IOException::class)
    private fun createImageFile(documentImage: Bitmap): File {

        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_TEST_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )

        // Convert bitmap to Image
        mCurrentPhotoPath = image.absolutePath
        val file = File(mCurrentPhotoPath)
        val os = BufferedOutputStream(FileOutputStream(file))
        documentImage.compress(Bitmap.CompressFormat.JPEG, 100, os)
        os.close()
        pdfName = file.name.substring(0, (file.name.length - 4))
        return image
    }


}
