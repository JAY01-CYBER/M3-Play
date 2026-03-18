package com.j.m3play.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Guarda una imagen personalizada para una playlist */
fun saveCustomPlaylistImage(context: Context, playlistId: String, imageUri: Uri) {
    try {
        // Crear directorio para imágenes de playlist si no existe
        val playlistImagesDir = File(context.filesDir, "playlist_images")
        if (!playlistImagesDir.exists()) {
            playlistImagesDir.mkdirs()
        }

        // Nombre del archivo basado en el ID de la playlist (usando el ID como String)
        val imageFile = File(playlistImagesDir, "playlist_${playlistId}.jpg")

        // Copiar la imagen seleccionada al directorio interno
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(imageFile).use { output ->
                input.copyTo(output)
            }
        }

        // Guardar la referencia en SharedPreferences
        context.getSharedPreferences("playlist_images", Context.MODE_PRIVATE).edit {
            putString("playlist_$playlistId", imageFile.toUri().toString())
        }
    } catch (e: IOException) {
        Timber.e(e, "Error al guardar imagen de playlist")
    }
}

/** Obtiene la URI de la imagen personalizada para una playlist */
fun getPlaylistImageUri(context: Context, playlistId: String): Uri? {
    val uriString = context.getSharedPreferences("playlist_images", Context.MODE_PRIVATE)
        .getString("playlist_$playlistId", null)

    return if (uriString != null) {
        val uri = uriString.toUri()
        // Verificar que el archivo existe
        val file = File(uri.path ?: "")
        if (file.exists()) uri else null
    } else null
}

/** Elimina la imagen personalizada de una playlist */
fun deletePlaylistImage(context: Context, playlistId: String) {
    val uriString = context.getSharedPreferences("playlist_images", Context.MODE_PRIVATE)
        .getString("playlist_$playlistId", null)

    if (uriString != null) {
        val file = File(uriString.toUri().path ?: "")
        if (file.exists()) {
            file.delete()
        }

        // Eliminar referencia de SharedPreferences
        context.getSharedPreferences("playlist_images", Context.MODE_PRIVATE).edit {
            remove("playlist_$playlistId")
        }
    }
}
