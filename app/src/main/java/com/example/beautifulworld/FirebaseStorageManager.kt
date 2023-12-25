package com.example.beautifulworld

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import com.example.beautifulworld.dto.User
import com.google.firebase.storage.FirebaseStorage

class FirebaseStorageManager {
    private val mStorageRef = FirebaseStorage.getInstance().reference
    private lateinit var mProgressDialog: ProgressDialog
    fun uploadImage(context: Context, imageFileUri: Uri, user: User) {
//        mProgressDialog = ProgressDialog(context)
//        mProgressDialog.setMessage("Пару секунд...")
//        mProgressDialog.show()
        val uploadTask = mStorageRef.child("users/${System.currentTimeMillis()}.png").putFile(imageFileUri)
        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            user.linkImage = mStorageRef.downloadUrl.toString()
            mStorageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
            } else {
                // Handle failures
                // ...
            }
        }
    }
}