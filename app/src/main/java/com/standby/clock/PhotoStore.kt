package com.standby.clock

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object PhotoStore {
    var revision by mutableIntStateOf(0); private set

    fun load(c: Context): List<Uri> =
        (Prefs.sp(c).getString("photos", "") ?: "").split('\n').filter { it.isNotBlank() }.map { Uri.parse(it) }

    fun save(c: Context, uris: List<Uri>) {
        Prefs.sp(c).edit().putString("photos", uris.joinToString("\n")).apply()
        revision++
    }

    fun clear(c: Context) = save(c, emptyList())
}

/** Call as a property initializer in an Activity; launch with picker.launch(arrayOf("image/*")). */
fun ComponentActivity.registerPhotoPicker(): ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach {
                try {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: SecurityException) { /* provider doesn't allow persisting */ }
            }
            PhotoStore.save(this, uris)
        }
    }
