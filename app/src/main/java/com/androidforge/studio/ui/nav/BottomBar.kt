package com.androidforge.studio.ui.nav

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * Bottom navigation bar. Editor/Build routes are project-scoped
 * ("editor/{projectId}"), so they match by prefix; when no project is open
 * those tabs navigate to the plain route and screens show an empty state.
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
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = {
                    Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
        }
    }
}
