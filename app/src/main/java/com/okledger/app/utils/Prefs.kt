package com.okledger.app.utils

import android.content.Context

class Prefs(private val context: Context) {
    private val p = context.getSharedPreferences("okledger_prefs", Context.MODE_PRIVATE)
    fun setMobile(mobile: String) = p.edit().putString("mobile", mobile).apply()
    fun getMobile(): String? = p.getString("mobile", null)

    fun setProfileImage(uri: String) = p.edit().putString("profile_image", uri).apply()
    fun getProfileImage(): String? = p.getString("profile_image", null)

    fun setName(name: String) = p.edit().putString("name", name).apply()
    fun getName(): String? = p.getString("name", null)

    fun setAddress(address: String) = p.edit().putString("address", address).apply()
    fun getAddress(): String? = p.getString("address", null)


}
