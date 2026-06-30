package com.zebra.rfid.demo.sdksample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

/**
 * Handler for DataWedge API interactions.
 * Handles registering for notifications and profile creation.
 */
public class DataWedgeHandler {
    private static final String TAG = "DataWedgeHandler";

    // DataWedge Intent Constants
    private static final String ACTION_DATAWEDGE = "com.symbol.datawedge.api.ACTION";
    private static final String EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG";
    private static final String EXTRA_REGISTER_NOTIFICATION = "com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION";
    private static final String NOTIFICATION_ACTION = "com.symbol.datawedge.api.NOTIFICATION_ACTION";
    private static final String RESULT_ACTION = "com.symbol.datawedge.api.RESULT_ACTION";
    
    // Barcode Intent constants
    public static final String BARCODE_ACTION = "com.zebra.rfid.demo.sdksample.BARCODE_ACTION";
    private static final String DATA_STRING_EXTRA = "com.symbol.datawedge.data_string";
    private static final String LABEL_TYPE_EXTRA = "com.symbol.datawedge.label_type";
    
    // Notification Types
    private static final String NOTIFICATION_TYPE_SCANNER_STATUS = "SCANNER_STATUS";
    
    private final Context context;
    private final DataWedgeStatusListener listener;

    /**
     * Interface for listening to DataWedge events.
     */
    public interface DataWedgeStatusListener {
        void onScannerStatusChange(String status);
        void onBarcodeScanned(String barcode, String symbology);
    }

    public DataWedgeHandler(Context context, DataWedgeStatusListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * Creates or updates a DataWedge profile for this application.
     * Associates the profile with the app and configures Intent output.
     */
    public void createProfile(String profileName) {
        Bundle profileConfig = new Bundle();
        profileConfig.putString("PROFILE_NAME", profileName);
        profileConfig.putString("PROFILE_ENABLED", "true");
        profileConfig.putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST");

        // Configure Barcode Input
        Bundle barcodeConfig = new Bundle();
        barcodeConfig.putString("PLUGIN_NAME", "BARCODE");
        barcodeConfig.putString("RESET_CONFIG", "true"); // Resets barcode settings to default
        Bundle barcodeProps = new Bundle();
        barcodeProps.putString("barcode_input_enabled", "true");
        barcodeProps.putString("scanner_selection", "auto");
        barcodeConfig.putBundle("PARAM_LIST", barcodeProps);

        // Configure RFID Input (Disable it)
        Bundle rfidConfig = new Bundle();
        rfidConfig.putString("PLUGIN_NAME", "RFID");
        Bundle rfidProps = new Bundle();
        rfidProps.putString("rfid_input_enabled", "false");
        rfidConfig.putBundle("PARAM_LIST", rfidProps);

        // Configure Intent Output
        Bundle intentConfig = new Bundle();
        intentConfig.putString("PLUGIN_NAME", "INTENT");
        intentConfig.putString("RESET_CONFIG", "true");
        Bundle intentProps = new Bundle();
        intentProps.putString("intent_output_enabled", "true");
        intentProps.putString("intent_action", BARCODE_ACTION);
        intentProps.putInt("intent_delivery", 2); // 2 = Broadcast Intent
        intentConfig.putBundle("PARAM_LIST", intentProps);

        // Configure Keystroke Output (disable it to prevent text field interference)
        Bundle keystrokeConfig = new Bundle();
        keystrokeConfig.putString("PLUGIN_NAME", "KEYSTROKE");
        Bundle keystrokeProps = new Bundle();
        keystrokeProps.putString("keystroke_output_enabled", "false");
        keystrokeConfig.putBundle("PARAM_LIST", keystrokeProps);

        // Add plugins to profile
        ArrayList<Bundle> pluginConfigs = new ArrayList<>();
        pluginConfigs.add(barcodeConfig);
        pluginConfigs.add(rfidConfig);
        pluginConfigs.add(intentConfig);
        pluginConfigs.add(keystrokeConfig);
        profileConfig.putParcelableArrayList("PLUGIN_CONFIG", pluginConfigs);

        // Associate with this app
        Bundle appConfig = new Bundle();
        appConfig.putString("PACKAGE_NAME", context.getPackageName());
        appConfig.putStringArray("ACTIVITY_LIST", new String[]{"*"});
        profileConfig.putParcelableArray("APP_LIST", new Bundle[]{appConfig});

        // Send Intent
        Intent i = new Intent();
        i.setAction(ACTION_DATAWEDGE);
        i.putExtra(EXTRA_SET_CONFIG, profileConfig);
        i.putExtra("SEND_RESULT", "true");
        i.putExtra("COMMAND_IDENTIFIER", "CREATE_PROFILE");
        context.sendBroadcast(i);
        Log.d(TAG, "Sent create profile broadcast for: " + profileName);
    }

    /**
     * Sends a broadcast to DataWedge to register for scanner status notifications.
     */
    public void registerForNotifications() {
        // Register for Scanner Status
        Bundle bScanner = new Bundle();
        bScanner.putString("com.symbol.datawedge.api.APPLICATION_NAME", context.getPackageName());
        bScanner.putString("com.symbol.datawedge.api.NOTIFICATION_TYPE", NOTIFICATION_TYPE_SCANNER_STATUS);
        
        Intent iScanner = new Intent();
        iScanner.setAction(ACTION_DATAWEDGE);
        iScanner.putExtra(EXTRA_REGISTER_NOTIFICATION, bScanner);
        context.sendBroadcast(iScanner);
    }

    /**
     * Toggles the DataWedge scanner state (Soft Scan).
     * @param start True to start scanning, false to stop.
     */
    public void toggleSoftScan(boolean start) {
        Intent i = new Intent();
        i.setAction(ACTION_DATAWEDGE);
        i.putExtra("com.symbol.datawedge.api.SOFT_SCAN_TRIGGER", start ? "START_SCANNING" : "STOP_SCANNING");
        context.sendBroadcast(i);
    }

    /**
     * Registers the broadcast receiver to listen for DataWedge notifications and barcode data.
     */
    public void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(NOTIFICATION_ACTION);
        filter.addAction(RESULT_ACTION);
        filter.addAction(BARCODE_ACTION);
        
        // Target SDK is 33, so we must specify RECEIVER_EXPORTED for cross-app broadcasts
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(dataWedgeReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(dataWedgeReceiver, filter);
        }
        Log.d(TAG, "Receiver registered");
    }

    /**
     * Unregisters the broadcast receiver.
     */
    public void unregisterReceiver() {
        try {
            context.unregisterReceiver(dataWedgeReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver not registered or already unregistered");
        }
    }

    private final BroadcastReceiver dataWedgeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);
            
            if (NOTIFICATION_ACTION.equals(action)) {
                if (intent.hasExtra("com.symbol.datawedge.api.NOTIFICATION")) {
                    Bundle b = intent.getBundleExtra("com.symbol.datawedge.api.NOTIFICATION");
                    String notificationType = b.getString("NOTIFICATION_TYPE");
                    
                    if (NOTIFICATION_TYPE_SCANNER_STATUS.equals(notificationType)) {
                        String status = b.getString("STATUS");
                        if (listener != null) listener.onScannerStatusChange(status);
                    }
                }
            } else if (RESULT_ACTION.equals(action)) {
                if (intent.hasExtra("COMMAND_IDENTIFIER")) {
                    String command = intent.getStringExtra("COMMAND_IDENTIFIER");
                    String result = intent.getStringExtra("RESULT");
                    Log.d(TAG, "Command Result: " + command + " -> " + result);
                }
            } else if (BARCODE_ACTION.equals(action)) {
                String barcode = intent.getStringExtra(DATA_STRING_EXTRA);
                String symbology = intent.getStringExtra(LABEL_TYPE_EXTRA);
                Log.d(TAG, "Barcode received: " + barcode + " (" + symbology + ")");
                if (barcode != null && listener != null) {
                    listener.onBarcodeScanned(barcode, symbology);
                }
            }
        }
    };
}
