package com.androidforge.studio.ui.nav

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * Professional Bottom navigation bar - AndroidForge Studio
 * Features:
 * - Professional IDE navigation
 * - Editor/Build routes are project-scoped
 * - Professional indicators for NDK, LibGDX, Real APK
 */
@Composable
fun BottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        bottomDestinations.forEach { dest ->
            val selected = currentRoute == dest.route ||
                (currentRoute?.startsWith("${dest.route}/") == true)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest.route) },
                icon = {
                    if (dest.professional) {
                        BadgedBox(badge = { Badge { Text("Pro", style = androidx.compose.material3.MaterialTheme.typography.labelSmall) } }) {
                            Icon(dest.icon, contentDescription = dest.label)
                        }
                    } else {
                        Icon(dest.icon, contentDescription = dest.label)
                    }
                },
                label = {
                    Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                },
            )
        }
    }
}
