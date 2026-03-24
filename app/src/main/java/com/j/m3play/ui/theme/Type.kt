package com.j.m3play.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.j.m3play.R

val GeomFont = FontFamily(
    Font(R.font.geom, FontWeight.Normal)
)

val Typography = Typography(

    titleLarge = TextStyle(
        fontFamily = GeomFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),

    titleMedium = TextStyle(
        fontFamily = GeomFont,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = GeomFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = GeomFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),

    labelLarge = TextStyle(
        fontFamily = GeomFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)
