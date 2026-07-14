package com.j.m3play.ui.component

import com.j.m3play.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

@Composable
fun ThemeAwareLoadingAnimation(modifier: Modifier = Modifier) {
    // App ke Material 3 theme se primary color nikaal rahe hain
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    // Lottie composition load karna (loading.json ko res/raw/ folder mein rakhein)
    // Agar resource ID kuch aur ho toh R.raw.loading ko update kar lena
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading))

    // Dynamic properties banayein colors ko theme ke hisaab se replace karne ke liye
    val dynamicProperties = rememberLottieDynamicProperties(
        // Shapes ke fill color ke liye (stars aur music note)
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = primaryColor,
            keyPath = arrayOf("**") 
        ),
        // Shapes ke stroke/border color ke liye (ellipse/ring)
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = primaryColor,
            keyPath = arrayOf("**")
        )
    )

    // UI Container
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            dynamicProperties = dynamicProperties,
            iterations = LottieConstants.IterateForever, // Infinite loop ke liye
            modifier = Modifier.size(100.dp) // Ise apne UI ke hisaab se adjust kar sakte ho
        )
    }
}
