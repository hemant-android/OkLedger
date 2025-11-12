package com.okledger.app.ui.login

import android.content.Intent
import android.os.Bundle
import com.okledger.app.base.BaseActivity
import com.okledger.app.databinding.ActivityEnterMobileBinding
import com.okledger.app.ui.dashboard.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EnterMobileActivity : BaseActivity<ActivityEnterMobileBinding>() {

    override fun getViewBinding() = ActivityEnterMobileBinding.inflate(layoutInflater)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnContinue.setOnClickListener {
            val mobile = binding.etMobile.text.toString().trim()
            if (mobile.length >= 10) {
                prefs.setMobile(mobile)
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                binding.etMobile.error = "Enter valid mobile"
            }
        }
    }
}