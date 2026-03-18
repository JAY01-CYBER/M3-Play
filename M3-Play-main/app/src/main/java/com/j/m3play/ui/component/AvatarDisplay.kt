package com.j.m3play.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.j.m3play.R

/**
 * Componente para mostrar el avatar del usuario en cualquier parte de la
 * app
 */
@Composable
fun AvatarDisplay(
    size: Dp = 40.dp,
    showBorder: Boolean = false,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    borderWidth: Dp = 2.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)

    val displayModifier = if (showBorder) {
        modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
    } else {
        modifier
            .size(size)
            .clip(CircleShape)
    }

    Box(
        modifier = displayModifier,
        contentAlignment = Alignment.Center
    ) {
        when (currentSelection) {
            is AvatarSelection.Custom -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data((currentSelection as AvatarSelection.Custom).uri.toUri())
                        .crossfade(true)
                        .error(R.drawable.person)
                        .placeholder(R.drawable.person)
                        .build(),
                    contentDescription = contentDescription
                        ?: stringResource(id = R.string.custom_avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            is AvatarSelection.DiceBear -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data((currentSelection as AvatarSelection.DiceBear).url)
                        .crossfade(true)
                        .error(R.drawable.person)
                        .placeholder(R.drawable.person)
                        .build(),
                    contentDescription = contentDescription ?: "Avatar DiceBear",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                DefaultAvatarIcon(contentDescription = contentDescription)
            }
        }
    }
}

/** Variante pequeña para usar en listas o elementos compactos */
@Composable
fun SmallAvatarDisplay(
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    AvatarDisplay(
        size = 32.dp,
        showBorder = false,
        modifier = modifier,
        contentDescription = contentDescription
    )
}

/** Variante grande para perfiles o pantallas principales */
@Composable
fun LargeAvatarDisplay(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    showBorder: Boolean = true
) {
    AvatarDisplay(
        size = 80.dp,
        showBorder = showBorder,
        borderWidth = 3.dp,
        modifier = modifier,
        contentDescription = contentDescription
    )
}

/** Avatar con indicador de estado online/offline */
@Composable
fun AvatarWithStatus(
    isOnline: Boolean = true,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Box(modifier = modifier) {
        AvatarDisplay(
            size = size,
            showBorder = true,
            contentDescription = contentDescription
        )

        // Indicador de estado
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = "Online",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/** Icono de avatar por defecto */
@Composable
private fun DefaultAvatarIcon(contentDescription: String? = null) {
    Icon(
        painter = painterResource(id = R.drawable.person),
        contentDescription = contentDescription ?: stringResource(id = R.string.default_avatar),
        modifier = Modifier.fillMaxSize(0.6f),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Utilidad para obtener el avatar actual de manera síncrona (para casos
 * especiales)
 */
class AvatarUtils {
    companion object {
        /** Obtiene la URI del avatar personalizado si está seleccionado */
        fun getCustomAvatarUri(selection: AvatarSelection): String? {
            return when (selection) {
                is AvatarSelection.Custom -> selection.uri
                else -> null
            }
        }

        /** Obtiene la URL del avatar DiceBear si está seleccionado */
        fun getDiceBearAvatarUrl(selection: AvatarSelection): String? {
            return when (selection) {
                is AvatarSelection.DiceBear -> selection.url
                else -> null
            }
        }

        /** Verifica si el avatar actual es personalizado */
        fun isCustomAvatar(selection: AvatarSelection): Boolean {
            return selection is AvatarSelection.Custom
        }

        /** Verifica si el avatar actual es DiceBear */
        fun isDiceBearAvatar(selection: AvatarSelection): Boolean {
            return selection is AvatarSelection.DiceBear
        }

        /** Verifica si el avatar actual es por defecto */
        fun isDefaultAvatar(selection: AvatarSelection): Boolean {
            return selection is AvatarSelection.Default
        }

        /** Obtiene la URL o URI del avatar actual */
        fun getAvatarSource(selection: AvatarSelection): String? {
            return when (selection) {
                is AvatarSelection.Custom -> selection.uri
                is AvatarSelection.DiceBear -> selection.url
                else -> null
            }
        }
    }
}