package com.example.cocwidget.ui

import android.appwidget.AppWidgetManager
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.cocwidget.data.Snapshot
import com.example.cocwidget.data.UpgradeRepository
import com.example.cocwidget.data.nowEpochSec
import com.example.cocwidget.util.formatAgo
import com.example.cocwidget.util.formatRemaining
import com.example.cocwidget.widget.UpgradesWidgetReceiver
import com.example.cocwidget.widget.WidgetRefresh
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repo: UpgradeRepository
    private var status by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = UpgradeRepository(this)
        handleShareIntent(intent)

        setContent {
            MaterialTheme {
                val snapshot by repo.snapshot.collectAsState(initial = null)
                MainScreen(
                    snapshot = snapshot,
                    status = status,
                    onPaste = ::importFromClipboard,
                    onLoadSample = ::importSample,
                    onAddWidget = ::requestPinWidget,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let(::importText)
        }
    }

    private fun importFromClipboard() {
        val clip = getSystemService(ClipboardManager::class.java).primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        if (text.isNullOrBlank()) {
            status = "Clipboard is empty — copy your village data in-game first."
        } else {
            importText(text)
        }
    }

    /** Asks the launcher to place the widget (shows a system confirmation dialog). */
    private fun requestPinWidget() {
        val manager = getSystemService(AppWidgetManager::class.java)
        status = if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(this, UpgradesWidgetReceiver::class.java), null, null)
            null
        } else {
            "Your launcher can't add widgets from apps. Long-press the home screen → Widgets instead."
        }
    }

    private fun importSample() {
        importText(assets.open("sample_export.json").bufferedReader().use { it.readText() })
    }

    private fun importText(text: String) {
        lifecycleScope.launch {
            status = try {
                val snap = repo.import(text)
                WidgetRefresh.schedulePeriodic(this@MainActivity)
                WidgetRefresh.refreshNow(this@MainActivity)
                "Imported ${snap.upgrades.size} active upgrade(s)."
            } catch (e: Exception) {
                "Couldn't read that as a village export: ${e.message?.take(120)}"
            }
        }
    }
}

@Composable
private fun MainScreen(
    snapshot: Snapshot?,
    status: String?,
    onPaste: () -> Unit,
    onLoadSample: () -> Unit,
    onAddWidget: () -> Unit,
) {
    // Tick once per second so the in-app countdowns are live.
    var now by remember { mutableLongStateOf(nowEpochSec()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = nowEpochSec()
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text("Clash Upgrade Widget", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "In Clash of Clans, copy your village data (Settings → More Settings), " +
                    "then paste it here. Add the widget from your home screen.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPaste) { Text("Paste from clipboard") }
                OutlinedButton(onClick = onLoadSample) { Text("Load sample") }
            }
            OutlinedButton(onClick = onAddWidget) { Text("Add widget to home screen") }
            status?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            if (snapshot == null) {
                Text("No data imported yet.")
                return@Column
            }

            Text(
                buildString {
                    append(snapshot.tag ?: "Village")
                    append(" · exported ")
                    append(formatAgo(now - snapshot.exportedAtEpochSec))
                    if (!snapshot.exportTimestampKnown) append(" (no timestamp in export; assumed import time)")
                },
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(8.dp))

            if (snapshot.upgrades.isEmpty()) {
                Text("No upgrades in progress.")
            }
            LazyColumn {
                items(snapshot.upgrades) { up ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("${up.name} → ${up.level + 1}", style = MaterialTheme.typography.bodyLarge)
                            Text(up.source.label, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            formatRemaining(up.finishAtEpochSec - now),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
