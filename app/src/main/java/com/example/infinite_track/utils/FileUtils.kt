package com.example.infinite_track.utils

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import java.net.URLEncoder
import java.util.Locale

fun openFile(context: Context, filePath: String) {
	val fileUri = filePath.toUri()
	val intent = Intent(Intent.ACTION_VIEW).apply {
		setDataAndType(fileUri, getMimeType(filePath))
		flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
	}
	context.startActivity(intent)
}

fun getMimeType(filePath: String): String {
	return when {
		filePath.endsWith(".jpg") || filePath.endsWith(".jpeg") -> "image/jpeg"
		filePath.endsWith(".png") -> "image/png"
		filePath.endsWith(".pdf") -> "application/pdf"
		else -> "*/*"
	}
}

fun updateAppLanguage(context: Context, language: String) {
	val normalizedTag = when (language.lowercase(Locale.ROOT)) {
		"id" -> "in" // Map BCP-47 'id' to Android resource qualifier 'in'
		else -> language
	}
	val locales: LocaleListCompat = LocaleListCompat.forLanguageTags(normalizedTag)
	AppCompatDelegate.setApplicationLocales(locales)
}

fun openWhatsApp(context: Context, phoneNumber: String, messageWA: String) {
	val encodedMessage = URLEncoder.encode(messageWA, "UTF-8")
	val url = "https://wa.me/62$phoneNumber?text=$encodedMessage"

	try {
		val intent = Intent(Intent.ACTION_VIEW, url.toUri())
		context.startActivity(intent)
	} catch (e: Exception) {
		Toast.makeText(context, "WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
	}
}

fun sendEmail(context: Context, emailAddress: String) {
	val intent = Intent(Intent.ACTION_SENDTO).apply {
		data = "mailto:$emailAddress".toUri()
		putExtra(Intent.EXTRA_SUBJECT, "Subjek Email")
		putExtra(Intent.EXTRA_TEXT, "Isi email...")
	}
	try {
		context.startActivity(intent)
	} catch (e: Exception) {
		Toast.makeText(context, "Tidak ada aplikasi email", Toast.LENGTH_SHORT).show()
	}
}

fun makePhoneCall(context: Context, phoneNumber: String) {
	val intent = Intent(Intent.ACTION_DIAL).apply {
		data = "tel:$phoneNumber".toUri()
	}
	context.startActivity(intent)
}
