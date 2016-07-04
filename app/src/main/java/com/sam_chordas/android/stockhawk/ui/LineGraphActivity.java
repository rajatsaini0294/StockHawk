package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.eazegraph.lib.charts.ValueLineChart;
import org.eazegraph.lib.models.ValueLinePoint;
import org.eazegraph.lib.models.ValueLineSeries;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class LineGraphActivity extends AppCompatActivity{

    private View progressCircle;
    private ValueLineChart lineChart;

    private boolean isLoaded = false;
    private String companySymbol;
    private String companyName;
    private ArrayList<String> labels;
    private ArrayList<Float> values;
    private MenuItem shareItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        progressCircle = findViewById(R.id.progress_circle);
        lineChart = (ValueLineChart) findViewById(R.id.linechart);
        companySymbol = getIntent().getStringExtra("symbol");
        if (savedInstanceState == null) {
            setTitle(companySymbol);
            downloadStockDetails();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.graph_activity_menu, menu);
        shareItem = menu.findItem(R.id.share);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if(item.getItemId() == R.id.share){
            Bitmap bitmap = captureGraph();
            shareBitmap(bitmap);
        }
        return true;

    }
    private void shareBitmap(Bitmap bitmap) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File f = new File(Environment.getExternalStorageDirectory() + File.separator + "temporary_file.jpg");
        try {
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "NO sd found", Toast.LENGTH_SHORT).show();
        }
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/temporary_file.jpg"));
        startActivity(Intent.createChooser(intent, "Share Image"));
    }

    private Bitmap captureGraph() {
        View view = findViewById(R.id.linechart);
        view.setDrawingCacheEnabled(true);
        int totalHeight = view.getHeight();
        int totalWidth = view.getWidth();
        view.layout(0, 0, totalWidth, totalHeight);
        view.buildDrawingCache(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(255, 255, 255));
        paint.setTextSize(24);

        Rect bounds = new Rect();
        paint.getTextBounds(companyName, 0, companyName.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width())/2;
        int y = (bitmap.getHeight() + bounds.height())/2;

        canvas.drawText(companyName, x, y, paint);

        return bitmap;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isLoaded) {
            outState.putString("company_name", companyName);
            outState.putStringArrayList("labels", labels);

            float[] valuesArray = new float[values.size()];
            for (int i = 0; i < valuesArray.length; i++) {
                valuesArray[i] = values.get(i);
            }
            outState.putFloatArray("values", valuesArray);
        }
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("company_name")) {
            isLoaded = true;

            companyName = savedInstanceState.getString("company_name");
            labels = savedInstanceState.getStringArrayList("labels");
            values = new ArrayList<>();

            float[] valuesArray = savedInstanceState.getFloatArray("values");
            for (float f : valuesArray) {
                values.add(f);
            }
            onDownloadCompleted();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }
    private void downloadStockDetails() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://chartapi.finance.yahoo.com/instrument/1.0/" + companySymbol + "/chartdata;type=quote;range=5y/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            public void onResponse(Response response) throws IOException {
                if (response.code() == 200) {
                    try {
                        // Trim response string
                        String result = response.body().string();
                        if (result.startsWith("finance_charts_json_callback( ")) {
                            result = result.substring(29, result.length() - 2);
                        }

                        // Parse JSON
                        JSONObject object = new JSONObject(result);
                        companyName = object.getJSONObject("meta").getString("Company-Name");
                        labels = new ArrayList<>();
                        values = new ArrayList<>();
                        JSONArray series = object.getJSONArray("series");
                        for (int i = 0; i < series.length(); i++) {
                            JSONObject seriesItem = series.getJSONObject(i);
                            SimpleDateFormat srcFormat = new SimpleDateFormat("yyyyMMdd");
                            String date = android.text.format.DateFormat.
                                    getMediumDateFormat(getApplicationContext()).
                                    format(srcFormat.parse(seriesItem.getString("Date")));
                            labels.add(date);
                            values.add(Float.parseFloat(seriesItem.getString("close")));
                        }

                        onDownloadCompleted();
                    } catch (Exception e) {
                        onDownloadFailed();
                        e.printStackTrace();
                    }
                } else {
                    onDownloadFailed();
                }
            }

            public void onFailure(Request request, IOException e) {
                onDownloadFailed();
            }
        });
    }

    private void onDownloadCompleted() {
        LineGraphActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle(companyName);

                progressCircle.setVisibility(View.GONE);

                ValueLineSeries series = new ValueLineSeries();
                series.setColor(0xFF56B7F1);
                for (int i = 0; i < labels.size(); i++) {
                    series.addPoint(new ValueLinePoint(labels.get(i), values.get(i)));
                }
                if (!isLoaded) {
                    lineChart.startAnimation();
                }
                lineChart.addSeries(series);
                lineChart.setVisibility(View.VISIBLE);

                isLoaded = true;
            }
        });
    }
    private void onDownloadFailed() {
        LineGraphActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lineChart.setVisibility(View.GONE);
                progressCircle.setVisibility(View.GONE);
            }
        });
    }
}
