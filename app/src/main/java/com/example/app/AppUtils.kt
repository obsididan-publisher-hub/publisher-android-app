package com.example.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.MalformedURLException
import java.net.URL

object AppUtils {

    fun isUrlCorrect(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (e: MalformedURLException) {
            false
        }
    }
    fun extractHost(url: String): String? {
        return try {
            val parsed = URL(url)
            parsed.host
        } catch (e: MalformedURLException) {
            null
        }
    }

    fun tryToOpenStartNote(context: Context, url: String) {
        if (!isUrlCorrect(url)) {
            Toast.makeText(context, "Некорректный URL", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}