AWARE Plugin: Fitbit
==========================

[![Release](https://jitpack.io/v/denzilferreira/com.aware.plugin.fitbit.svg)](https://jitpack.io/#denzilferreira/com.aware.plugin.fitbit)

This plugin allows researchers to collect the data from a Fitbit device: calories, steps, heart-rate, sleep.

# Settings
Parameters adjustable on the dashboard and client:
- **status_plugin_fitbit**: (boolean) activate/deactivate plugin
- **units_plugin_fitbit**: (String) one of metric/imperial
- **plugin_fitbit_frequency**: (integer) interval in which to check for new data on Fitbit. Fitbit has a hard-limit of 150 data checks, per hour, per device.
- **fitbit_granularity**: (String) intraday granularity. One of 1d/15min/1min for daily summary, 15 minutes and 1 minute, respectively.
- **fitbit_hr_granularity**: (String) intraday granularity. One of 1min/1sec for 1 minute, and 5 second interval respectively (setting is 1sec but returns every 5sec).
- **api_key_plugin_fitbit**: (String) Fitbit Client Key
- **api_secret_plugin_fitbit**: (String) Fitbit Client Secret

# Broadcasts
**ACTION_AWARE_PLUGIN_FITBIT**
Broadcasted every time we have new Fibit data, with the following extras:
- **extra_total_steps**: (double) number of steps today
- **extra_last_3h**: (double) number of steps in the past 3h
- **extra_last_5h**: (double) number of steps in the past 5h
**ACTION_AWARE_PLUGIN_FITBIT_SYNC**
Received broadcast to request the plugin to check for new data on Fitbit's servers

# Providers
##  Fitbit Devices
> content://com.aware.plugin.fitbit.provider.fitbit/fitbit_devices

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of sample
device_id | TEXT | AWARE device ID
fitbit_id | TEXT | Fitbit device ID
fitbit_version | TEXT | Fitbit device version (e.g., Charge HR)
fitbit_battery | TEXT | Fitbit device battery level (high, medium, low)
fitbit_mac | TEXT | Fitbit device Bluetooth MAC address
fitbit_last_sync | TEXT | Last time the device synched with Fitbit

##  Fitbit Data
> content://com.aware.plugin.fitbit.provider.fitbit/fitbit_data

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of sample
device_id | TEXT | AWARE device ID
fitbit_id | TEXT | Fitbit device ID
fitbit_data | TEXT | JSON with what Fitbit API returns. Depends on the data type
fitbit_data_type | TEXT | one of the following: sleep, steps, calories, heartrate