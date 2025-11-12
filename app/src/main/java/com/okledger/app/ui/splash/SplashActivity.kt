package com.okledger.app.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.okledger.app.databinding.ActivitySplashBinding
import com.okledger.app.ui.login.EnterMobileActivity
import com.okledger.app.ui.dashboard.DashboardActivity
import com.okledger.app.utils.Prefs

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = Prefs(this)
        val mobile = prefs.getMobile()

        // Small delay to show splash (optional)
        binding.root.postDelayed({
            if (mobile.isNullOrEmpty()) {
                startActivity(Intent(this, EnterMobileActivity::class.java))
            } else {
                startActivity(Intent(this, DashboardActivity::class.java))
            }
            finish()
        }, 3000)
    }
}
