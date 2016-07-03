package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.LineGraphActivity;

/**
 * Created by rajat on 7/3/2016.
 */
public class WidgetProvider extends AppWidgetProvider{

    private static HandlerThread workerThread;
    private static Handler workerQueue;
    private static QuoteDataProviderObserver sDataObserver;
    public static String CLICK_ACTION = "com.sam_chordas.android.quotelistwidget.CLICK";
    public WidgetProvider() {
        workerThread = new HandlerThread("widgetProvider");
        workerThread.start();
        // looper is used to create a queue of the runnables
        workerQueue = new Handler(workerThread.getLooper());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(CLICK_ACTION)) {
            final String symbol = intent.getStringExtra("symbol");
            Intent i = new Intent(context, LineGraphActivity.class);
            i.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("symbol", symbol);
            context.startActivity(i);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
            RemoteViews layout = buildLayout(context, appWidgetIds[i]);
            appWidgetManager.updateAppWidget(appWidgetIds[i], layout);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        final ContentResolver r = context.getContentResolver();
        if (sDataObserver == null) {
            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            final ComponentName cn = new ComponentName(context, WidgetProvider.class);
            sDataObserver = new QuoteDataProviderObserver(mgr, cn, workerQueue);
            r.registerContentObserver(QuoteProvider.Quotes.CONTENT_URI, true, sDataObserver);
        }
    }

    private RemoteViews buildLayout(Context context, int appWidgetId) {
        RemoteViews rv;

        final Intent intent = new Intent(context, QuoteWidgetRemoteViewsService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        rv = new RemoteViews(context.getPackageName(), R.layout.widget_collection);
        rv.setRemoteAdapter(R.id.widget_list, intent);

        final Intent onClickIntent = new Intent(context, WidgetProvider.class);
        onClickIntent.setAction(WidgetProvider.CLICK_ACTION);
        onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0,
                onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.widget_list, onClickPendingIntent);

        return rv;
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }
}


