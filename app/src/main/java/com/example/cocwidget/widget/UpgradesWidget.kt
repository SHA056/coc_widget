package com.example.cocwidget.widget

import android.content.Context
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.cocwidget.R
import com.example.cocwidget.data.Snapshot
import com.example.cocwidget.data.Upgrade
import com.example.cocwidget.data.UpgradeRepository
import com.example.cocwidget.data.nowEpochSec
import com.example.cocwidget.ui.MainActivity
import com.example.cocwidget.util.formatAgo
import com.example.cocwidget.util.formatRemaining

private val Bg = Color(0xFF1E2A3A)
private val Fg = ColorProvider(Color.White)
private val Muted = ColorProvider(Color(0xFFB0BEC5))
private val Ready = ColorProvider(Color(0xFF81C784))

private const val MAX_ROWS = 5

class UpgradesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read once per update. Updates are triggered by WidgetRefresh.
        val snapshot = UpgradeRepository(context).current()
        val now = nowEpochSec()
        provideContent { WidgetContent(snapshot, now) }
    }
}

@Composable
private fun WidgetContent(snapshot: Snapshot?, now: Long) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Bg)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (snapshot == null) {
            Text("No village data", style = TextStyle(color = Fg, fontWeight = FontWeight.Bold))
            Text("Tap to import", style = TextStyle(color = Muted, fontSize = 12.sp))
            return@Column
        }

        val pending = snapshot.upgrades.filter { it.finishAtEpochSec > now }
        val ready = snapshot.upgrades.size - pending.size

        Text(
            "${snapshot.tag ?: "Upgrades"} · synced ${formatAgo(now - snapshot.exportedAtEpochSec)}",
            style = TextStyle(color = Muted, fontSize = 11.sp),
        )
        Spacer(GlanceModifier.height(4.dp))

        if (ready > 0) {
            Text("$ready ready to collect", style = TextStyle(color = Ready, fontSize = 13.sp, fontWeight = FontWeight.Bold))
        }

        val next = pending.firstOrNull()
        if (next == null) {
            if (ready == 0) Text("No upgrades in progress", style = TextStyle(color = Fg))
            return@Column
        }

        // Soonest upgrade gets a live, launcher-driven countdown.
        Text("${next.name} → ${next.level + 1}", style = TextStyle(color = Fg, fontSize = 13.sp))
        LiveCountdown(next)
        Spacer(GlanceModifier.height(6.dp))

        pending.drop(1).take(MAX_ROWS - 1).forEach { up ->
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    up.name,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(color = Fg, fontSize = 12.sp),
                    maxLines = 1,
                )
                Text(formatRemaining(up.finishAtEpochSec - now), style = TextStyle(color = Muted, fontSize = 12.sp))
            }
        }
    }
}

/**
 * Glance has no Chronometer, so embed a RemoteViews one. Chronometer's base is in
 * elapsedRealtime, so convert the wall-clock finish time.
 */
@Composable
private fun LiveCountdown(up: Upgrade) {
    val context = LocalContext.current
    val msUntilFinish = up.finishAtEpochSec * 1000 - System.currentTimeMillis()
    val base = SystemClock.elapsedRealtime() + msUntilFinish
    val views = RemoteViews(context.packageName, R.layout.widget_chronometer).apply {
        setChronometer(R.id.chrono, base, null, true)
        setChronometerCountDown(R.id.chrono, true)
    }
    // Explicit height: without it the embedded view expands and hides the rows below.
    AndroidRemoteViews(remoteViews = views, modifier = GlanceModifier.height(34.dp))
}
