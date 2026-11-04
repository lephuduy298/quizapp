package com.example.learning

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyWidgetPickerPreviewTest {
    @Test fun bothPickerPreviewsInflateThroughRemoteViews() {
        assumeTrue(Build.VERSION.SDK_INT >= 31)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val providers = AppWidgetManager.getInstance(context).installedProviders
            .filter { it.provider.packageName == context.packageName }
        assertEquals(2, providers.size)
        for (provider in providers) {
            assertTrue("Preview layout for ${provider.provider}", provider.previewLayout != 0)
            assertNotNull("Fallback preview for ${provider.provider}",
                provider.loadPreviewImage(context, context.resources.displayMetrics.densityDpi))
            // The picker uses RemoteViews; a regular LayoutInflater would miss unsupported views.
            RemoteViews(context.packageName, provider.previewLayout).apply(context, FrameLayout(context))
        }
    }
}
