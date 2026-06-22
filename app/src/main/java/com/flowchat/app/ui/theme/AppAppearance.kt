package com.flowchat.app.ui.theme

import android.content.Context

enum class AppAppearance(val storageValue: String) {
    Light("light"),
    Dark("dark");

    companion object {
        fun load(context: Context): AppAppearance {
            val value = context.applicationContext
                .getSharedPreferences(AppAppearancePrefsName, Context.MODE_PRIVATE)
                .getString(AppAppearanceKey, Light.storageValue)
            return entries.firstOrNull { it.storageValue == value } ?: Light
        }

        fun save(context: Context, appearance: AppAppearance) {
            context.applicationContext
                .getSharedPreferences(AppAppearancePrefsName, Context.MODE_PRIVATE)
                .edit()
                .putString(AppAppearanceKey, appearance.storageValue)
                .apply()
        }
    }
}

private const val AppAppearancePrefsName = "app_appearance"
private const val AppAppearanceKey = "appearance"
