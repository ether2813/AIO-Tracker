package com.aio.tracker.ui.salat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.aio.tracker.data.db.Prayer
import com.aio.tracker.data.db.PrayerStatus
import com.aio.tracker.ui.theme.AppTheme

val Prayer.icon: ImageVector
    get() = when (this) {
        Prayer.FAJR -> Icons.Rounded.WbTwilight
        Prayer.DHUHR -> Icons.Rounded.WbSunny
        Prayer.ASR -> Icons.Rounded.LightMode
        Prayer.MAGHRIB -> Icons.Rounded.Brightness4
        Prayer.ISHA -> Icons.Rounded.NightsStay
        Prayer.JUMUAH -> Icons.Rounded.Groups
    }

@Composable
fun PrayerStatus.color(): Color = when (this) {
    PrayerStatus.JAMAAH -> AppTheme.ext.gold
    PrayerStatus.ON_TIME -> AppTheme.ext.income
    PrayerStatus.LATE -> AppTheme.ext.warning
    PrayerStatus.MISSED -> AppTheme.ext.expense
}

@Composable
fun prayerTint(prayer: Prayer): Color = when (prayer) {
    Prayer.FAJR -> Color(0xFF5B8DEF)
    Prayer.DHUHR -> Color(0xFFE8A93A)
    Prayer.ASR -> Color(0xFFEF8A4C)
    Prayer.MAGHRIB -> Color(0xFFD9607A)
    Prayer.ISHA -> Color(0xFF8B7BEA)
    Prayer.JUMUAH -> MaterialTheme.colorScheme.primary
}
