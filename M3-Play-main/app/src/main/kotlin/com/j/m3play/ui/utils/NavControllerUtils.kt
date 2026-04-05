/*
 * M3Play Utility Module
 *
 * Internal helper functions
 * Signature: M3PLAY::UTILITY::V1
 */

package com.j.m3play.ui.utils

import androidx.navigation.NavController
import com.j.m3play.ui.screens.Screens

fun NavController.backToMain() {
    val mainRoutes = Screens.MainScreens.map { it.route }

    while (previousBackStackEntry != null &&
        currentBackStackEntry?.destination?.route !in mainRoutes
    ) {
        popBackStack()
    }
}
