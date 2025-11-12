package com.okledger.app.ui.editprofile

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.okledger.app.R
import com.okledger.app.base.BaseActivity
import com.okledger.app.databinding.ActivityEditProfileBinding
import java.io.File
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditProfileActivity : BaseActivity<ActivityEditProfileBinding>() {

    private var profileImageUri: Uri? = null
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && profileImageUri != null) {
                setProfileImage(profileImageUri!!)
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                profileImageUri = it
                setProfileImage(it)
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val cameraGranted = result[Manifest.permission.CAMERA] == true
            val readGranted =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    result[Manifest.permission.READ_MEDIA_IMAGES] == true
                else
                    result[Manifest.permission.READ_EXTERNAL_STORAGE] == true

            if (cameraGranted || readGranted) {
                showImagePickerDialog()
            }
        }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")

        AlertDialog.Builder(this)
            .setTitle("Update Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }.show()
    }

    private fun openCamera() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (!permissions.all { checkSelfPermission(it) == PERMISSION_GRANTED }) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            profileImageUri = createImageUri()
            profileImageUri?.let { cameraLauncher.launch(it) }
        }
    }

    private fun setProfileImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .placeholder(R.drawable.ic_profile_default)
            .into(binding.imgProfile)
    }

    private fun openGallery() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (!perms.all { checkSelfPermission(it) == PERMISSION_GRANTED }) {
            permissionLauncher.launch(perms)
        } else {
            galleryLauncher.launch("image/*")
        }
    }

    private fun createImageUri(): Uri {
        val imagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(imagesDir, "profile_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            this,
            "com.okledger.app.fileprovider",
            file
        )
    }

    override fun getViewBinding() = ActivityEditProfileBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.toolbar.tvTitle.text = getString(R.string.text_title_edit_profile)

        loadSavedProfile()

        binding.toolbar.imgBack.setOnClickListener {
            finish()
        }

        binding.frame.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnUpdate.setOnClickListener {
            validateForm()
        }

    }

    // Loading Saved Profile Data
    private fun loadSavedProfile() {
        binding.etName.setText(prefs.getName() ?: "")
        binding.etPhone.setText(prefs.getMobile() ?: "")
        binding.etAddress.setText(prefs.getAddress() ?: "")

        prefs.getProfileImage()?.let { uri ->
            setProfileImage(uri.toUri())
        }
    }

    private fun validateForm() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        when {
            name.isEmpty() -> binding.etName.error = "Enter name"
            phone.length < 10 -> binding.etPhone.error = "Enter valid phone"
            address.isEmpty() -> binding.etAddress.error = "Enter address"
            else -> {

                prefs.setName(name)
                prefs.setMobile(phone)
                prefs.setAddress(address)

                profileImageUri?.let { prefs.setProfileImage(it.toString()) }

                showToast("Profile Updated Successfully 🎉")
                finish()
            }
        }
    }

}