package com.aware.plugin.fitbit;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.device_picker);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        LinearLayout device_container = (LinearLayout) findViewById(R.id.device_picker);

        if (getIntent() != null && getIntent().getExtras() != null) {
            try {
                String devicesJSON = getIntent().getStringExtra(DEVICES_JSON);

                if (devicesJSON == null || devicesJSON.length() == 0) {
                    Toast.makeText(this, "No Fitbit devices on this account?", Toast.LENGTH_SHORT).show();
                    Plugin.devicesPicker = null;
                    finish();
                } else {
                    RadioGroup singleChoice = new RadioGroup(this);
                    devices = new JSONArray(devicesJSON);

                    for (int i = 0; i < devices.length(); i++) {
                        JSONObject device = devices.getJSONObject(i);

                        RadioButton rDevice = new RadioButton(this);
                        rDevice.setText(device.getString("deviceVersion"));
                        rDevice.setTag(device.getString("id"));
                        singleChoice.addView(rDevice);
                    }

                    device_container.addView(singleChoice);

                    singleChoice.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                            try {
                                RadioButton selectedRadio = (RadioButton) radioGroup.findViewById(checkedId);
                                for (int i = 0; i < devices.length(); i++) {
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
                                device.put(Provider.Fitbit_Devices.FITBIT_MAC, selected.optString("mac", ""));
                                device.put(Provider.Fitbit_Devices.LAST_SYNC, selected.getString("lastSyncTime"));
                                getContentResolver().insert(Provider.Fitbit_Devices.CONTENT_URI, device);

                                Intent startSync = new Intent(getApplicationContext(), Plugin.class);
                                startSync.setAction(Plugin.ACTION_AWARE_PLUGIN_FITBIT_SYNC);
                                startService(startSync);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Plugin.devicesPicker = null;

                            finish();
                        }
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Plugin.devicesPicker = null;
                finish();
            }
        }
    }
}
