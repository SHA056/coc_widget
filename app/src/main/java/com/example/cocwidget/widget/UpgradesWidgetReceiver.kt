package com.example.cocwidget.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class UpgradesWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = UpgradesWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefresh.schedulePeriodic(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefresh.cancelAll(context)
    }
}
