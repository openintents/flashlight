package org.openintents.flashlight;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        int length = appWidgetIds.length;
        for (int i = 0; i < length; i++) {
            int appWidgetId = appWidgetIds[i];

            Intent intent = new Intent(context, WidgetService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                    intent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);
            views.setOnClickPendingIntent(R.id.imageView1, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);

        }
    }
}
