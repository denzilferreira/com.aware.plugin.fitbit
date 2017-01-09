//package com.aware.plugin.fitbit;
//
//import android.content.ContentValues;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.graphics.Color;
//import android.net.Uri;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.support.annotation.Nullable;
//import android.support.customtabs.CustomTabsIntent;
//import android.support.v7.app.AppCompatActivity;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.CheckBox;
//import android.widget.CompoundButton;
//import android.widget.Spinner;
//
//import com.aware.Aware;
//import com.aware.Aware_Preferences;
//import com.github.scribejava.core.builder.ServiceBuilder;
//import com.github.scribejava.core.model.OAuth2AccessToken;
//import com.github.scribejava.core.model.OAuthRequest;
//import com.github.scribejava.core.model.Response;
//import com.github.scribejava.core.model.Verb;
//import com.github.scribejava.core.oauth.OAuth20Service;
//
//import net.danlew.android.joda.JodaTimeAndroid;
//
//import org.joda.time.DateTime;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.IOException;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
///**
// * Created by sklakegg on 13/12/16.
// */
//
//public class Fitbit extends AppCompatActivity {
//
//    // Retrieved from  fitbit
//    private final String api_Key = "227YG3";
//    private final String api_Secret = "033ed2a3710c0cde04343d073c09e378";
//    private String auth_scope;
//    private final String response = "token";
//    private final String redirect_URI = "Fitbit://logincallback";
//    private String responseString;
//    private String string_Activity;
//    private String string_HR;
//    private String string_Sleep;
//    private String lastSynced;
//    private String lastSynced_MinusH;
//    private String stepsLastHours = "";
//
//    // Defined by the redirect URI fragment
//    private String access_Token;
//    private String userID;
//    private String data_scope;
//    private String token_Type;
//    private int expires_In;
//
//    // Shared preferences.
//    private SharedPreferences prefs;
//    final private String Fitbit_Preference = "Fitbit_Preference";
//
//    // Scribe variables.
//    private OAuth20Service OA2_Service;
//    private OAuth2AccessToken OA2_Access_Token;
//
//    // UI.
//    private Button button1;
//    private Spinner unitSpinner;
//    private CheckBox checkbox_Activity;
//    private CheckBox checkbox_HR;
//    private CheckBox checkbox_Sleep;
//    private int unitSelected;
//    private String unit_Array[] = {"en_US", "en_GB", "Metric"};
//    private ArrayAdapter<CharSequence> unit_Adapter;
//    private ScheduledExecutorService scheduler;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        //TESTING
//        Intent aware = new Intent(this, Aware.class);
//        startService(aware);
//        //--
//
//        setContentView(R.layout.card);
//
//        JodaTimeAndroid.init(this);
//        unitSpinner = (Spinner) findViewById(R.id.spinner_Unit);
//        checkbox_Activity = (CheckBox) findViewById(R.id.check_Activity);
//        checkbox_HR = (CheckBox) findViewById(R.id.check_HR);
//        checkbox_Sleep = (CheckBox) findViewById(R.id.check_Sleep);
//        button1 = (Button) findViewById(R.id.button);
//
//        prefs = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE);
//        access_Token = prefs.getString("OA2_Access_Token", null);
//
//        checkAuthorization();
//
//        // Inflate spinner and get selections.
//        unit_Adapter = ArrayAdapter.createFromResource(this, R.array.spinner_units, android.R.layout.simple_spinner_item);
//        unit_Adapter.setDropDownViewResource(R.layout.spinner_item);
//        unitSpinner.setAdapter(unit_Adapter);
//        // Set up spinner
//        unitSpinner.setSelection(unitSelected);
//        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
//                editor.putInt("unitSelected", i);
//                editor.commit();
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//            }
//        });
//
//        setButtonChecked();
//
//        checkbox_Activity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
//                if (isChecked) {
//                    editor.putBoolean("Activity_Checked", true);
//                }
//                if (!isChecked) {
//                    editor.putBoolean("Activity_Checked", false);
//                }
//                editor.commit();
//                checkAuthorization();
//            }
//        });
//        checkbox_HR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
//                if (isChecked) {
//                    editor.putBoolean("HR_Checked", true);
//                }
//                if (!isChecked) {
//                    editor.putBoolean("HR_Checked", false);
//                }
//                editor.commit();
//                checkAuthorization();
//            }
//        });
//        checkbox_Sleep.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
//                if (isChecked) {
//                    editor.putBoolean("Sleep_Checked", true);
//                }
//                if (!isChecked) {
//                    editor.putBoolean("Sleep_Checked", false);
//                }
//                editor.commit();
//                checkAuthorization();
//            }
//        });
//
//        button1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!checkAuthorization()) {
//                    AuthorizeClient();
//                }
//            }
//        });
//
//        // Used to get Fitbit data.
//        /*if (checkAuthorization() && prefs.getString("OA2_Access_Token", null) != null) {
//            new getFitbitData().execute();
//        } */
//    }
//
//    // Check if the user's requested scope equals the scope of the access token.
//    public boolean checkAuthorization() {
//
//        if (prefs.getBoolean("Activity_Checked", false) == prefs.getBoolean("access_Token_Activity", false)
//            && prefs.getBoolean("HR_Checked", false) == prefs.getBoolean("access_Token_HR", false)
//            && prefs.getBoolean("Sleep_Checked", false) == prefs.getBoolean("access_Token_Sleep", false)) {
//            button1.setBackgroundColor(Color.GREEN);
//            button1.setText("Authorized");
//            startSchedule();
//            return true;
//        } else {
//            button1.setBackgroundColor(Color.RED);
//            button1.setText("Authorize");
//            if (scheduler != null) { scheduler.shutdownNow(); }
//            return false;
//        }
//    }
//
//    // Start data collection and store in DB.
//    public void startSchedule() {
//        if (prefs.getBoolean("Activity_Checked", false) == prefs.getBoolean("access_Token_Activity", false) &&
//                prefs.getBoolean("Activity_Checked", false) &&
//                prefs.getBoolean("access_Token_Activity", false) &&
//                prefs.getBoolean("access_Token_Settings", false) &&
//                prefs.getString("OA2_Access_Token", null) != null )  {
//
//            scheduler = Executors.newSingleThreadScheduledExecutor();
//
//            // Start to get step count and put in DB.
//            scheduler.scheduleAtFixedRate(new Runnable() {
//                public void run() {
//                    new stepCounterLastHoursClass().execute("3");
//                    new stepCounterLastHoursClass().execute("5");
//                }
//            }, 0, 10, TimeUnit.SECONDS);
//        }
//    }
//
//    // Put checkboxes in the correct state.
//    public void setButtonChecked() {
//
//        if (prefs.getBoolean("Activity_Checked", false)) {
//            checkbox_Activity.setChecked(true);
//        } else {
//            checkbox_Activity.setChecked(false);
//        }
//
//        if (prefs.getBoolean("HR_Checked", false)) {
//            checkbox_HR.setChecked(true);
//        } else {
//            checkbox_HR.setChecked(false);
//        }
//
//        if (prefs.getBoolean("Sleep_Checked", false)) {
//            checkbox_Sleep.setChecked(true);
//        } else {
//            checkbox_Sleep.setChecked(false);
//        }
//    }
//
//    // Called to authorize the client.
//    public void AuthorizeClient() {
//
//        // Build service.
//        OA2_Service = buildOAuth20Service();
//
//        // Launch Chrome Custom Tab.
//        final String authorizationUrl = OA2_Service.getAuthorizationUrl();
//        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
//        CustomTabsIntent customTabsIntent = builder.build();
//        customTabsIntent.launchUrl(this, Uri.parse(authorizationUrl));
//    }
//
//    // Called on redirect URI from Chrome Custom Tab
//    protected void onNewIntent(Intent intent) {
//
//        if (intent.getData() != null) {
//            Uri URL_Fragment = intent.getData();
//            String URL_Fragment_String = URL_Fragment.toString();
//
//            // Retrieve information about access token.
//            access_Token = URL_Fragment_String.substring(URL_Fragment_String.indexOf("access_token=") + 13, URL_Fragment_String.indexOf("&user_id"));
//            userID = URL_Fragment_String.substring(URL_Fragment_String.indexOf("user_id=") + 8, URL_Fragment_String.indexOf("&scope"));
//            data_scope = URL_Fragment_String.substring(URL_Fragment_String.indexOf("scope=") + 6, URL_Fragment_String.indexOf("&token_type"));
//            token_Type = URL_Fragment_String.substring(URL_Fragment_String.indexOf("token_type=") + 11, URL_Fragment_String.indexOf("&expires_in"));
//            expires_In = Integer.parseInt(URL_Fragment_String.substring(URL_Fragment_String.indexOf("expires_in=") + 11, URL_Fragment_String.length()));
//
//            Aware.setSetting(this, Settings.OAUTH_TOKEN, access_Token);
//
//            try {
//                JSONObject data_scopes = new JSONObject();
//                data_scopes.put("activity", data_scope.toLowerCase().contains("activity".toLowerCase()));
//                data_scopes.put("heartrate", data_scope.toLowerCase().contains("heartrate".toLowerCase()));
//                data_scopes.put("sleep", data_scope.toLowerCase().contains("sleep".toLowerCase()));
//
//                Aware.setSetting(this, Settings.OAUTH_SCOPES, data_scopes.toString());
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            Aware.setSetting(this, Settings.OAUTH_TOKEN_TYPE, token_Type);
//            Aware.setSetting(this, Settings.OAUTH_VALIDITY, expires_In);
//
//            // Store information about access token in shared preferences.
////            SharedPreferences.Editor editor = getSharedPreferences(Fitbit_Preference, MODE_PRIVATE).edit();
////
////            if (data_scope.toLowerCase().contains("activity".toLowerCase())) {
////                Aware.setSetting(getApplicationContext(), Settings.OAUTH_SCOPES);
////
////                editor.putBoolean("access_Token_Activity", true);
////            } else {
////                editor.putBoolean("access_Token_Activity", false);
////            }
////
////            if (data_scope.toLowerCase().contains("heartrate".toLowerCase())) {
////                editor.putBoolean("access_Token_HR", true);
////            } else {
////                editor.putBoolean("access_Token_HR", false);
////            }
////
////            if (data_scope.toLowerCase().contains("sleep".toLowerCase())) {
////                editor.putBoolean("access_Token_Sleep", true);
////            } else {
////                editor.putBoolean("access_Token_Sleep", false);
////            }
////
////            if (data_scope.toLowerCase().contains("settings".toLowerCase())) {
////                editor.putBoolean("access_Token_Settings", true);
////            } else {
////                editor.putBoolean("access_Token_Settings", false);
////            }
////
////            editor.putString("OA2_Access_Token", access_Token);
////            editor.putString("Token_Data_Scope", data_scope);
////            editor.putString("Token Type", token_Type);
////            editor.putInt("Expires In", expires_In);
////            editor.commit();
//
//            button1.setBackgroundColor(Color.GREEN);
//        }
//    }
//
//    private class getFitbitData extends AsyncTask<Void, Void, Void> {
//
//        @Override
//        protected Void doInBackground(Void... params) {
//
//            // Create the OAuth2.0 access token.
//            OA2_Access_Token = createOAuth2AccessToken();
//
//
//            String lastSynced_Date = "today";
//            // Retrieve the time and date of when the user last synced data.
//            lastSynced = requestSend("https://api.fitbit.com/1/user/-/devices.json");
//            try {
//                // Retrieve the time and date of when the user last synced data.
//                JSONArray lastSynced_JSONArr = new JSONArray(lastSynced);
//                JSONObject lastSynced_JSONObj = lastSynced_JSONArr.getJSONObject(0);
//                lastSynced = lastSynced_JSONObj.getString("lastSyncTime");
//
//                String[] parts = lastSynced.split("T");
//                lastSynced_Date = parts[0];
//                String lastSynced_Time = parts[1];
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            // Send request for data.
//            if (prefs.getBoolean("access_Token_Activity", false)) {
//
//                string_Activity = requestSend("https://api.fitbit.com/1/user/-/activities/date/" + lastSynced_Date + ".json");
//                storeData(string_Activity, "Activity");
//            }
//
//            if (prefs.getBoolean("access_Token_HR", false)) {
//                string_HR = requestSend("https://api.fitbit.com/1/user/-/activities/heart/date/" + lastSynced_Date + "/1d.json");
//                storeData(string_HR, "HR");
//            }
//
//            if (prefs.getBoolean("access_Token_Sleep", false)) {
//                string_Sleep = requestSend("https://api.fitbit.com/1/user/-/sleep/date/" + lastSynced_Date + ".json");
//                storeData(string_Sleep, "Sleep");
//            }
//            return null;
//        }
//    }
//
//    // Async tasks that get the stepcount in the last hours and puts in DB:
//    private class stepCounterLastHoursClass extends AsyncTask<String, Void, String> {
//
//        int hours;
//
//        @Override
//        protected String doInBackground(String... params) {
//            String myString = params[0];
//            hours = Integer.parseInt(myString);
//            OA2_Access_Token = createOAuth2AccessToken();
//            lastSynced = requestSend("https://api.fitbit.com/1/user/-/devices.json");
//
//            try {
//                // Retrieve the time and date of when the user last synced data.
//                JSONArray lastSynced_JSONArr = new JSONArray(lastSynced);
//                JSONObject lastSynced_JSONObj = lastSynced_JSONArr.getJSONObject(0);
//                lastSynced = lastSynced_JSONObj.getString("lastSyncTime");
//
//                String[] parts = lastSynced.split("T");
//                String lastSynced_Date = parts[0];
//                String lastSynced_Time = parts[1];
//                lastSynced_Time = lastSynced_Time.substring(0, lastSynced_Time.lastIndexOf(":"));
//                DateTime lastSynced_DateTime = DateTime.parse(lastSynced);
//
//                // Retrieve the time and date X hours before the user last synced data.
//                DateTime lastSynced_DateTime_MinusH = lastSynced_DateTime.minusHours(hours);
//                lastSynced_MinusH = lastSynced_DateTime_MinusH.toString();
//                lastSynced_MinusH = lastSynced_MinusH.substring(0, lastSynced_MinusH.lastIndexOf("+"));
//                parts = lastSynced_MinusH.split("T");
//                String lastSynced_MinusH_Date = parts[0];
//                String lastSynced_MinusH_Time = parts[1];
//                lastSynced_MinusH_Time = lastSynced_MinusH_Time.substring(0, lastSynced_MinusH_Time.lastIndexOf(":"));
//
//                // Request data in this time period.
//                stepsLastHours = requestSend(
//                        "https://api.fitbit.com/1/user/-/activities/steps/date/"
//                                + lastSynced_MinusH_Date + "/" + lastSynced_Date + "/15min/time/"
//                                + lastSynced_MinusH_Time + "/" + lastSynced_Time + ".json");
//
//                // Retrieve the step count from JSON.
//                // Parse JSON and get string.
//                /*
//                JSONObject obj = new JSONObject(stepsLastHours);
//                JSONArray stepsArray = obj.getJSONArray("activities-steps");
//                JSONObject stepsObject = stepsArray.getJSONObject(0);
//                stepsLastHours = stepsObject.getString("value");*/
//
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            return stepsLastHours;
//        }
//
//        @Override
//        protected void onPostExecute(String FitbitJSON) {
//
//            if (hours == 3) {
//                storeData(FitbitJSON, "Stepcount_3hours");
//            }
//            if (hours == 5) {
//                storeData(FitbitJSON, "Stepcount_5hours");
//            }
//        }
//    }
//
//    // Create request, add headers and send.
//    public String requestSend(String request_URL) {
//
//        OA2_Service = buildOAuth20Service();
//
//        final OAuthRequest request = new OAuthRequest(Verb.GET, request_URL, OA2_Service);
//        request.addHeader("Authorization", " " + prefs.getString("Token Type", null) + " " + prefs.getString("OA2_Access_Token", null));
//        request.addHeader("Accept-Language", unit_Array[prefs.getInt("unitSelected", 2)]);
//
//        OA2_Service.signRequest(OA2_Access_Token, request);
//        final Response response = request.send();
//        try {
//            responseString = response.getBody().toString();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return responseString;
//    }
//
//    // Create OAuth20Service
//    public OAuth20Service buildOAuth20Service() {
//
//        // Get the data scope from shared preferences.
//        auth_scope = "";
//        if (prefs.getBoolean("Activity_Checked", false)) {
//            auth_scope = "activity";
//        }
//        if (prefs.getBoolean("HR_Checked", false)) {
//            auth_scope = auth_scope + " " + "heartrate";
//        }
//        if (prefs.getBoolean("Sleep_Checked", false)) {
//            auth_scope = auth_scope + " " + "sleep";
//        }
//
//        // Build service.
//        OA2_Service = new ServiceBuilder()
//                .apiKey(api_Key)
//                .scope(auth_scope + " settings")
//                .responseType(response)
//                .callback(redirect_URI)
//                .apiSecret(api_Secret)
//                .build(FitbitAPI.instance());
//
//        return OA2_Service;
//    }
//
//    // Create a new OAuth2 access token.
//    public OAuth2AccessToken createOAuth2AccessToken() {
//        OA2_Access_Token = new OAuth2AccessToken(
//                prefs.getString("OA2_Access_Token", null),
//                prefs.getString("Token Type", null),
//                prefs.getInt("Expires In", 0),
//                "null",
//                prefs.getString("Token_Data_Scope", null),
//                "null");
//        return OA2_Access_Token;
//    }
//
//    // Store the fitbit data.
//    public void storeData(String responseData, String type) {
//        ContentValues rowData = new ContentValues();
//        rowData.put(Provider.Fitbit_Data.TIMESTAMP, System.currentTimeMillis());
//        rowData.put(Provider.Fitbit_Data.DEVICE_ID, Aware.getSetting(this, Aware_Preferences.DEVICE_ID));
//        rowData.put(Provider.Fitbit_Data.FITBIT_JSON, responseData);
//        rowData.put(Provider.Fitbit_Data.DATA_TYPE, type);
//        getContentResolver().insert(Provider.Fitbit_Data.CONTENT_URI, rowData);
//    }
//}