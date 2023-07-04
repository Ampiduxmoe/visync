package com.example.visync.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.visync.R

fun getProfilePreferences(context: Context): SharedPreferences {
    val packageName = context.getString(R.string.app_name)
    val profilePrefsKey = context.getString(R.string.prefs_profile_file_key)
    return context.getSharedPreferences(
        "$packageName.$profilePrefsKey",
        Context.MODE_PRIVATE
    )
}