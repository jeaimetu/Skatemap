package com.almomin.skatemap

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: WebView

    companion object {
        /**
         * Request code for onActivityResult for when the user wants to pick an image
         * in the WebFragment
         */
        const val RESULT_OK = 1
    }

    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    private val REQUEST_CAMERA_PERMISSION_CODE = 1

    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
    }

    private val fileUploadActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("MYWEB", "fileUploadActivityResultLauncher is called ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("MYWEB", "fileUploadActivityResultLauncher success case")

                val resultIntent = result.data as Intent
                val action = resultIntent.action
                val extra = resultIntent.extras
                Log.d("MYWEB", "action = $action extra = $extra")

                // check the value of mCameraPhotoPath
                if (mCameraPhotoPath != null) {
                    Log.d("MYWEB", "mCameraPhotoPath $mCameraPhotoPath")
                } else {
                    Log.d("MYWEB", "mCameraPhotoPath is null")
                }

                var results: Array<Uri>? = null
                if (action != null) {
                    Log.d("MYWEB", "Taking a photo case")
                    if (mCameraPhotoPath != null) {
                        Log.d("MYWEB", "mCameraPhotoPath is not null")
                        //parse intent extra

                        results = arrayOf(Uri.parse(mCameraPhotoPath))
                    }
                    val intentExtra: String? = resultIntent.dataString
                    for (key in resultIntent.extras!!.keySet()) {
                        Log.d("MYWEB", "Key set $key")
                    }
                    val pathData: String? = resultIntent.extras!!.getString("data")
                    Log.d("MYWEB", "Path Data $pathData")
                    Log.d("MYWEB", "intentExtra $intentExtra")
                } else {
                    Log.d("MYWEB", "File picker case")
                    //check photopath
                    Log.d("MYWEB", "data string ${result.data.toString()}")
                    val results = result.data?.let {
                        WebChromeClient.FileChooserParams.parseResult(
                            result.resultCode,
                            it
                        )
                    }
                    fileUploadCallback?.onReceiveValue(results)
                }

                mFilePathCallback?.onReceiveValue(results)
                mFilePathCallback = null


            } else {
                Log.d("MYWEB", "fileUploadActivityResultLauncher take photo case")

                // check the value of mCameraPathCallBack
                if (mCameraPhotoPath != null) {
                    Log.d("MYWEB", "mCameraPhotoPath $mCameraPhotoPath")
                } else {
                    Log.d("MYWEB", "mCameraPhotoPath is null")
                }

                val dataString: String = result.data.toString()
                var results: Array<Uri>? = null
                if (dataString != null) {

                    Log.d("MYWEB", "dataString is not null")
                    results = arrayOf(Uri.fromFile(File(mCameraPhotoPath?.substring(7))))
                }

                results = arrayOf(Uri.fromFile(File(mCameraPhotoPath?.substring(7))))



                Log.d("MYWEB", "take photo results " + results.get(0).toString())
                fileUploadCallback?.onReceiveValue(results)
                mFilePathCallback?.onReceiveValue(results)
            }
            Log.d("MYWEB", "fileUploadActivityResultLauncher always called")
            fileUploadCallback = null

            return@registerForActivityResult
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //request location permission
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            }
        }
        myWebView = findViewById(R.id.webview)
        myWebView.webViewClient = MyWebViewClient()
        myWebView.setWebViewClient(MyWebViewClient())
        myWebView.getSettings().textZoom = 100
        val webSettings = myWebView.getSettings()
        webSettings.javaScriptEnabled = true


        myWebView.settings.allowFileAccess = true
        myWebView.settings.domStorageEnabled = true
        myWebView.settings.allowContentAccess = true

        // Enable file upload from the WebView
        myWebView.webChromeClient = object : WebChromeClient() {


            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                Log.d("MYWEB", "onShowFileChooser")
                fileUploadCallback = filePathCallback ?: return false


                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                if (takePictureIntent?.let {
                        packageManager.queryIntentActivities(
                            it,
                            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                        )
                    } != null) {

                    // Create the File where the photo should go
                    Log.d("MYWEB", "takePictureIntent resolved " + takePictureIntent.toString())
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        Log.e("ErrorCreatingFile", "Unable to create Image File", ex)
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        Log.d("MYWEB", "Photo file creation $mCameraPhotoPath" )
                        takePictureIntent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile)
                        )
                    } else {
                        Log.d("MYWEB", "photoFile null case")
                        takePictureIntent = null
                    }
                } else {
                    Log.d("MYWEB", "Intent resolving error")
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"

                val intentArray: Array<Intent?>
                intentArray = if (takePictureIntent != null) {
                    arrayOf(takePictureIntent)
                } else {
                    arrayOfNulls(0)
                }



                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

                //fileUploadActivityResultLauncher.launch(intent)
                //startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
                fileUploadActivityResultLauncher.launch(chooserIntent)
                return true
            }
        }
        myWebView.loadUrl("https://skatemap.kr/skatemap%202.0.html")
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf<String>(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }

            0 -> {
                    if (grantResults[0] == 0)
                        Toast.makeText(this, "카메라 권한이 승인됨", Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(this, "카메라 권한이 거절됨", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class MyWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if ("skatemap.kr" == Uri.parse(url).host) {
                // This is my website, so do not override; let my WebView load the page
                return false
            }
            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            return true
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            // Handle downloads from the WebView
            val url = request?.url.toString()

            Log.d("MYWEB", "shouldInterceptRequest called $url")
            if (url.endsWith(".jpg") || url.endsWith(".gif") || url.endsWith(".png") || url.endsWith(".jpeg")
            ) {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                return if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    Log.e("MYWEB", "Image not found $url")
                    WebResourceResponse("image/*", "UTF-8", null)
                } else {
                    val inputStream = connection.inputStream
                    Log.e("MYWEB", "Image found $url")
                    WebResourceResponse("image/*", "UTF-8", inputStream)
                }

            }
            return super.shouldInterceptRequest(view, request)
        }
    }

    override fun onBackPressed() {
        if (myWebView!!.canGoBack()) {
            myWebView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }
}