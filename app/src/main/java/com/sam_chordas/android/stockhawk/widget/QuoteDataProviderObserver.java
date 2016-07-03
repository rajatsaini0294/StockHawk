package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.database.ContentObserver;
import android.os.Handler;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by rajat on 7/3/2016.
 */
public class QuoteDataProviderObserver extends ContentObserver {
    private AppWidgetManager mAppWidgetManager;
    private ComponentName mComponentName;

    QuoteDataProviderObserver(AppWidgetManager mAppWidgetManager, ComponentName componentName, Handler handler) {
        super(handler);
        mAppWidgetManager = mAppWidgetManager;
        mComponentName = componentName;
    }
    @Override
    public void onChange(boolean selfChange) {
        // The data has changed, so notify the widget that the collection view needs to be updated.
        // In response, the factory's onDataSetChanged() will be called which will requery the
        // cursor for the new data.
        mAppWidgetManager.notifyAppWidgetViewDataChanged(
                mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.widget_list);
    }
}