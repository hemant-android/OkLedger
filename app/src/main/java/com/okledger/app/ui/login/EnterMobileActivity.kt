package com.okledger.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
            val savedMobile = prefs.getMobile()
            when {
                mobile.length < 10 -> {
                    binding.etMobile.error = "Enter valid mobile"
                }

                savedMobile == mobile -> {
                    Toast.makeText(this, "Number is already registered", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    prefs.setMobile(mobile)
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                }
            }
        }
    }
}