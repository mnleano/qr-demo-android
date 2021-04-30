package com.neds.qrdemo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.neds.qrdemo.databinding.ActivityMainBinding
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.button.setOnClickListener { selectImage() }
    }

    private fun hasStoragePermission() =
        EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)

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
                Manifest.permission.READ_EXTERNAL_STORAGE
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

    companion object {
        const val RC_STORAGE = 100
        const val RC_PICK_IMAGE = 102
        const val TAG = "MainActivity"
    }
}