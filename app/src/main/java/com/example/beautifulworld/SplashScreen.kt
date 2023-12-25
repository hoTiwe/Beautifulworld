package com.example.beautifulworld

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        val iv_note = findViewById<ImageView>(R.id.iv_note)
        iv_note.alpha = 0f
        iv_note.animate().setDuration(1500).alpha(1f).withEndAction {
            val i = Intent(this, if (getSharedPreferences("SessionSettings", Context.MODE_PRIVATE).getString("token", "").toString()!="") MapActivity::class.java else MainActivity::class.java)
            startActivity(i)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
