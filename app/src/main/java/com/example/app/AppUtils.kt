package com.example.app

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
}