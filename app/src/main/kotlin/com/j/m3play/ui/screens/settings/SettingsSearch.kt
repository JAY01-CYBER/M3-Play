package com.j.m3play.ui.screens.settings

fun filterSettingsItems(
    items: List<SettingsItem>,
    query: String,
): List<SettingsItem> {
    if (query.isBlank()) return items
    return items.filter { matchesQuery(it, query) }
}

fun filterSettingsGroups(
    groups: List<SettingsGroup>,
    query: String,
): List<SettingsGroup> {
    if (query.isBlank()) return groups
    return groups.mapNotNull { group ->
        if (group.title.contains(query, ignoreCase = true)) {
            group
        } else {
            val filtered = group.items.filter { matchesQuery(it, query) }
            if (filtered.isEmpty()) null else group.copy(items = filtered)
        }
    }
}

fun matchesQuery(item: SettingsItem, query: String): Boolean {
    if (item.title.contains(query, ignoreCase = true)) return true
    if (item.subtitle?.contains(query, ignoreCase = true) == true) return true
    return item.keywords.any { it.contains(query, ignoreCase = true) }
}
