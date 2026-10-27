package com.standby.clock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

/**
 * Picked photos are resized (max 2048 px) and copied into the app's own storage, so they keep
 * working if you move or delete the originals. Up to MAX_PHOTOS are kept (oldest dropped).
 */
object PhotoStore {
    private const val MAX_PHOTOS = 120
    private const val MAX_SIDE = 2048

    var revision by mutableIntStateOf(0); private set

    private fun dir(c: Context): File = File(c.filesDir, "photos").apply { mkdirs() }

    fun load(c: Context): List<Uri> =
        (dir(c).listFiles { f -> f.extension == "jpg" } ?: emptyArray()).sortedBy { it.name }.map { Uri.fromFile(it) }

    fun clear(c: Context) {
        dir(c).listFiles()?.forEach { it.delete() }
        revision++
    }

    fun import(c: Context, uris: List<Uri>) {
        val app = c.applicationContext
        thread {
            val d = dir(app)
            for (u in uris) {
                val bmp = decode(app, u) ?: continue
                try {
                    FileOutputStream(File(d, "p" + System.currentTimeMillis() + "_" + System.nanoTime() % 1000 + ".jpg")).use {
                        bmp.compress(Bitmap.CompressFormat.JPEG, 88, it)
                    }
                } finally { bmp.recycle() }
            }
            val files = (d.listFiles() ?: emptyArray()).sortedBy { it.name }
            files.take((files.size - MAX_PHOTOS).coerceAtLeast(0)).forEach { it.delete() }
            Handler(Looper.getMainLooper()).post { revision++ }
        }
    }

    private fun decode(c: Context, u: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(c.contentResolver, u)) { dec, info, _ ->
                val m = maxOf(info.size.width, info.size.height)
                if (m > MAX_SIDE) {
                    val s = MAX_SIDE.toFloat() / m
                    dec.setTargetSize((info.size.width * s).toInt().coerceAtLeast(1), (info.size.height * s).toInt().coerceAtLeast(1))
                }
                dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            c.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
            var sample = 1
            while (maxOf(o.outWidth, o.outHeight) / sample > MAX_SIDE) sample *= 2
            c.contentResolver.openInputStream(u)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }
    } catch (e: Exception) { null }
}

/** Call as a property initializer in an Activity; launch it with an array of MIME types for images. */
fun ComponentActivity.registerPhotoPicker(): ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) PhotoStore.import(this, uris)
    }
