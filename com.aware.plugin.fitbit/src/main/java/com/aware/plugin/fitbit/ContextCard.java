package com.aware.plugin.fitbit;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aware.utils.IContextCard;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by denzil on 13/01/2017.
 */

public class ContextCard implements IContextCard {

    public ContextCard() {
    }

    @Override
    public View getContextCard(Context context) {

        View card = LayoutInflater.from(context).inflate(R.layout.card, null);
        getDailySteps(context, (BarChart) card.findViewById(R.id.daily_steps));
        getDailyHeartRate(context, (LineChart) card.findViewById(R.id.daily_heartrate));

        return card;
    }

    /**
     * Get today's steps plot
     *
     * @param context
     * @param chart
     */
    private void getDailySteps(Context context, BarChart chart) {

        Calendar startDayTime = Calendar.getInstance();
        startDayTime.setTimeInMillis(System.currentTimeMillis());
        startDayTime.set(Calendar.HOUR_OF_DAY, 0);
        startDayTime.set(Calendar.MINUTE, 0);
        startDayTime.set(Calendar.SECOND, 0);
        startDayTime.set(Calendar.MILLISECOND, 0);

        Cursor latest_steps = context.getContentResolver().query(Provider.Fitbit_Data.CONTENT_URI, null, Provider.Fitbit_Data.DATA_TYPE + " LIKE 'steps'", null, Provider.Fitbit_Data.TIMESTAMP + " DESC LIMIT 1");
        if (latest_steps != null && latest_steps.moveToFirst()) {
            try {

                JSONObject stepsJSON = new JSONObject(latest_steps.getString(latest_steps.getColumnIndex(Provider.Fitbit_Data.FITBIT_JSON)));

                String total_steps = stepsJSON.getJSONArray("activities-steps").getJSONObject(0).getString("value"); //today's total steps
                JSONArray steps = stepsJSON.getJSONObject("activities-steps-intraday").getJSONArray("dataset"); //contains all of today's step count, per 15 minutes

                ArrayList<BarEntry> bars = new ArrayList<>();
                for (int i = 0; i < steps.length(); i++) {
                    JSONObject step_counts = steps.getJSONObject(i);

                    String time = step_counts.getString("time");
                    int step_count = step_counts.getInt("value");

                    DateFormat parseTime = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
                    bars.add(new BarEntry(parseTime.parse(time).getHours(), step_count));
                }

                BarDataSet barsDataset = new BarDataSet(bars, "Steps today: " + total_steps);
                barsDataset.setColor(Color.parseColor("#33B5E5"));
                barsDataset.setDrawValues(false);

                BarData data = new BarData(barsDataset);

                chart.getDescription().setEnabled(false);
                chart.setData(data);
                chart.invalidate();

                ViewGroup.LayoutParams params = chart.getLayoutParams();
                params.height = 400;
                chart.setLayoutParams(params);

                chart.setContentDescription("");
                chart.setBackgroundColor(Color.WHITE);
                chart.setDrawGridBackground(false);
                chart.setDrawBorders(false);

                YAxis left = chart.getAxisLeft();
                left.setDrawLabels(true);
                left.setDrawGridLines(false);
                left.setDrawAxisLine(false);
                left.setGranularity(10);
                left.setGranularityEnabled(true);

                YAxis right = chart.getAxisRight();
                right.setDrawAxisLine(false);
                right.setDrawLabels(false);
                right.setDrawGridLines(false);

                XAxis bottom = chart.getXAxis();
                bottom.setPosition(XAxis.XAxisPosition.BOTTOM);
                bottom.setDrawGridLines(false);

                Legend l = chart.getLegend();
                l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
                l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
                l.setForm(Legend.LegendForm.LINE);
                l.setTypeface(Typeface.DEFAULT_BOLD);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (latest_steps != null && !latest_steps.isClosed()) latest_steps.close();
    }

    /**
     * Get today's HR plot
     *
     * @param context
     * @param chart
     */
    private void getDailyHeartRate(Context context, LineChart chart) {

        Calendar startDayTime = Calendar.getInstance();
        startDayTime.setTimeInMillis(System.currentTimeMillis());
        startDayTime.set(Calendar.HOUR_OF_DAY, 0);
        startDayTime.set(Calendar.MINUTE, 0);
        startDayTime.set(Calendar.SECOND, 0);
        startDayTime.set(Calendar.MILLISECOND, 0);

        Cursor latest_hr = context.getContentResolver().query(Provider.Fitbit_Data.CONTENT_URI, null, Provider.Fitbit_Data.DATA_TYPE + " LIKE 'heartrate'", null, Provider.Fitbit_Data.TIMESTAMP + " DESC LIMIT 1");
        if (latest_hr != null && latest_hr.moveToFirst()) {
            try {

                JSONObject hrJSON = new JSONObject(latest_hr.getString(latest_hr.getColumnIndex(Provider.Fitbit_Data.FITBIT_JSON)));

                int restingHR = hrJSON.getJSONArray("activities-heart").getJSONObject(0).getJSONObject("value").optInt("restingHeartRate", -1); //today's resting heart rate
                JSONArray hearts = hrJSON.getJSONObject("activities-heart-intraday").getJSONArray("dataset"); //contains all of today's heart rate, every 5 seconds

                ArrayList<Entry> entries = new ArrayList<>();
                DateFormat parseTime = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

                for (int i = 0; i < hearts.length(); i++) {
                    JSONObject hr_counts = hearts.getJSONObject(i);

                    String time = hr_counts.getString("time");
                    Date parsed = parseTime.parse(time);

                    Calendar timer = Calendar.getInstance();
                    timer.setTimeInMillis(System.currentTimeMillis());
                    timer.set(Calendar.HOUR_OF_DAY, parsed.getHours());
                    timer.set(Calendar.MINUTE, parsed.getMinutes());
                    timer.set(Calendar.SECOND, parsed.getSeconds());

                    int hr = hr_counts.getInt("value");

                    entries.add(new Entry(timer.getTimeInMillis(), hr));
                }

                LineDataSet lineDataset = new LineDataSet(entries, "Resting Heart Rate today: " + restingHR);
                lineDataset.setColor(Color.parseColor("#F44336"));
                lineDataset.setDrawValues(false);
                lineDataset.setDrawCircles(false);

                LineData data = new LineData(lineDataset);

                chart.getDescription().setEnabled(false);
                chart.setData(data);
                chart.invalidate();

                ViewGroup.LayoutParams params = chart.getLayoutParams();
                params.height = 400;
                chart.setLayoutParams(params);

                chart.setContentDescription("");
                chart.setBackgroundColor(Color.WHITE);
                chart.setDrawGridBackground(false);
                chart.setDrawBorders(false);

                YAxis left = chart.getAxisLeft();
                left.setDrawLabels(true);
                left.setDrawGridLines(false);
                left.setDrawAxisLine(false);
                left.setGranularity(10);
                left.setGranularityEnabled(true);

                YAxis right = chart.getAxisRight();
                right.setDrawAxisLine(false);
                right.setDrawLabels(false);
                right.setDrawGridLines(false);

                XAxis bottom = chart.getXAxis();
                bottom.setPosition(XAxis.XAxisPosition.BOTTOM);
                bottom.setDrawGridLines(false);

                Legend l = chart.getLegend();
                l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
                l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
                l.setForm(Legend.LegendForm.LINE);
                l.setTypeface(Typeface.DEFAULT_BOLD);

                chart.getXAxis().setValueFormatter(new HourAxisValueFormatter());

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (latest_hr != null && !latest_hr.isClosed()) latest_hr.close();
    }

    public class HourAxisValueFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            Calendar day = Calendar.getInstance();
            day.setTimeInMillis((long) value);
            return String.valueOf(day.get(Calendar.HOUR_OF_DAY));
        }
    }
}
