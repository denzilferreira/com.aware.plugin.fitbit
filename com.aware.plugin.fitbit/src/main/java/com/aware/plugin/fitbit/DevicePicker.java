package com.aware.plugin.fitbit;

import android.content.ContentValues;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.BoringLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.aware.Aware;
import com.aware.Aware_Preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sklakegg on 09/01/17.
 */

public class DevicePicker extends AppCompatActivity {

    public static final String DEVICES_JSON = "devicesJSON";
    private JSONArray devices;
    private JSONObject selected;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.device_picker);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        LinearLayout device_container = (LinearLayout) findViewById(R.id.device_picker);

        if (getIntent() != null && getIntent().getExtras() != null) {
            String devicesJSON = getIntent().getStringExtra(DEVICES_JSON);

            RadioGroup singleChoice = new RadioGroup(this);
            try {
                devices = new JSONArray(devicesJSON);
                for(int i = 0; i < devices.length(); i++) {
                    JSONObject device = devices.getJSONObject(i);

                    RadioButton rDevice = new RadioButton(this);
                    rDevice.setText(device.getString("deviceVersion"));
                    rDevice.setTag(device.getString("id"));
                    singleChoice.addView(rDevice);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            device_container.addView(singleChoice);

            singleChoice.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                    try {
                        RadioButton selectedRadio = (RadioButton) radioGroup.findViewById(checkedId);
                        for (int i=0; i < devices.length(); i++) {
                            JSONObject device = devices.getJSONObject(i);
                            if (device.getString("id").equalsIgnoreCase(selectedRadio.getTag().toString())) {
                                selected = device;
                                break;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            Button saveDevice = (Button) findViewById(R.id.select_device);
            saveDevice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        ContentValues device = new ContentValues();
                        device.put(Provider.Fitbit_Devices.TIMESTAMP, System.currentTimeMillis());
                        device.put(Provider.Fitbit_Devices.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        device.put(Provider.Fitbit_Devices.FITBIT_ID, selected.getString("id"));
                        device.put(Provider.Fitbit_Devices.FITBIT_BATTERY, selected.getString("battery"));
                        device.put(Provider.Fitbit_Devices.FITBIT_VERSION, selected.getString("deviceVersion"));
                        device.put(Provider.Fitbit_Devices.LAST_SYNC, selected.getString("lastSyncTime"));
                        getContentResolver().insert(Provider.Fitbit_Devices.CONTENT_URI, device);

                        JSONArray activities = new JSONArray(FitbitAPI.fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/activities/date/" + selected.getString("lastSyncTime").split("T")[0] + ".json"));
                        JSONArray hr = new JSONArray(FitbitAPI.fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/heart/date/" + selected.getString("lastSyncTime").split("T")[0] + ".json"));
                        JSONArray sleep = new JSONArray(FitbitAPI.fetchData(getApplicationContext(), "https://api.fitbit.com/1/user/-/sleep/date/" + selected.getString("lastSyncTime").split("T")[0] + ".json"));

                        ContentValues activityData = new ContentValues();
                        activityData.put(Provider.Fitbit_Data.TIMESTAMP, System.currentTimeMillis());
                        activityData.put(Provider.Fitbit_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        activityData.put(Provider.Fitbit_Data.DATA_TYPE, "activities");
                        activityData.put(Provider.Fitbit_Data.FITBIT_JSON, activities.toString());
                        getContentResolver().insert(Provider.Fitbit_Data.CONTENT_URI, activityData);

                        ContentValues heartRateData = new ContentValues();
                        activityData.put(Provider.Fitbit_Data.TIMESTAMP, System.currentTimeMillis());
                        activityData.put(Provider.Fitbit_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        activityData.put(Provider.Fitbit_Data.DATA_TYPE, "heartrate");
                        activityData.put(Provider.Fitbit_Data.FITBIT_JSON, hr.toString());
                        getContentResolver().insert(Provider.Fitbit_Data.CONTENT_URI, heartRateData);

                        ContentValues sleepData = new ContentValues();
                        activityData.put(Provider.Fitbit_Data.TIMESTAMP, System.currentTimeMillis());
                        activityData.put(Provider.Fitbit_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        activityData.put(Provider.Fitbit_Data.DATA_TYPE, "sleep");
                        activityData.put(Provider.Fitbit_Data.FITBIT_JSON, sleep.toString());
                        getContentResolver().insert(Provider.Fitbit_Data.CONTENT_URI, sleepData);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    finish();
                }
            });
        }
    }
}
