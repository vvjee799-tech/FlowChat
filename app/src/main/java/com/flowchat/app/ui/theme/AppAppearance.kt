package com.flowchat.app.ui.theme

import android.content.Context

enum class AppAppearance(val storageValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun load(context: Context): AppAppearance {
            val value = context.applicationContext
                .getSharedPreferences(AppAppearancePrefsName, Context.MODE_PRIVATE)
                .getString(AppAppearanceKey, Dark.storageValue)
            return entries.firstOrNull { it.storageValue == value } ?: Dark
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
