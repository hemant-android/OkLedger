package com.okledger.app.base

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.okledger.app.utils.Prefs

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    protected lateinit var binding: VB
    protected lateinit var prefs: Prefs

    abstract fun getViewBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getViewBinding()
        setContentView(binding.root)

        // Initialize SharedPreferences
        prefs = Prefs(this)
    }

    fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
