package com.neds.qrdemo

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Reader
import com.google.zxing.common.HybridBinarizer
import com.neds.qrdemo.databinding.ActivityMainBinding
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.button.setOnClickListener { selectImage() }
        Log.d(TAG, "onCreate")
    }

    private fun hasStoragePermission() =
        EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @AfterPermissionGranted(RC_STORAGE)
    private fun selectImage() {
        if (hasStoragePermission()) {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select QR"), RC_PICK_IMAGE)
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.rationale_storage),
                RC_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_PICK_IMAGE) {
            data?.data?.let {
                val imageStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(imageStream)
                var value = scanQRImage(bitmap)
                Log.d(
                    TAG,
                    "onActivityResult: width=${bitmap.width}, height=${bitmap.height}, value=$value"
                )
                var multiplier = 1
                while (value == null) {
                    resizeBitmap(bitmap, multiplier)?.let { bMap -> value = scanQRImage(bMap) }
                        ?: break
                    multiplier++
                    if (multiplier >= 10) break
                }

                Log.d(TAG, "value=$value")
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, multiplier: Int): Bitmap? {
        val ratio =  1024f/ bitmap.width
        val scaledWidth = (bitmap.width * ratio * multiplier * 0.05).toInt()
        val scaledHeight = (bitmap.height * ratio * multiplier * 0.05).toInt()
//        Log.d(
//            TAG,
//            "resizeBitmap: multiplier=$multiplier, scaledWidth=$scaledWidth, scaledHeight=$scaledHeight"
//        )
        val rescaleBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        val file = File(getExternalFilesDir(null), "temp.jpg")

        if (file.exists())
            file.delete()

        return try {
            val out = FileOutputStream(file)
            rescaleBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveImage(bitmap: Bitmap) {
        Log.d(TAG, "saveImage: width=${bitmap.width}, height=${bitmap.height}")
        val file = File(getExternalFilesDir(null), "temp.jpg")

        if (file.exists())
            file.delete()

        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()

            val bMap = BitmapFactory.decodeFile(file.absolutePath)
            binding.imageView.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            Log.d(TAG, "saveImage: scanQRImage=${scanQRImage(bMap)}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanQRImage(bitmap: Bitmap): String? {
//        Log.d(TAG, "scanQRImage: bitmap width=${bitmap.width}, height=${bitmap.height}")
        var contents: String? = null
        val intArray = IntArray(bitmap.width * bitmap.height)
        //copy pixel data from the Bitmap into the 'intArray' array
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val bBitMap = BinaryBitmap(HybridBinarizer(source))

        val reader: Reader = MultiFormatReader()
        try {
            val result = reader.decode(bBitMap)
            contents = result.text
        } catch (e: Exception) {
        }
        return contents
    }

    companion object {
        const val RC_STORAGE = 100
        const val RC_PICK_IMAGE = 102
        const val TAG = "MainActivity"
    }
}