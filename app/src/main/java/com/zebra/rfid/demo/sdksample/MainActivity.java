package com.zebra.rfid.demo.sdksample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zebra.rfid.api3.TagData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main Activity for the RFID Sample application.
 * This activity handles the UI and user interactions for connecting to a reader,
 * performing inventory, and scanning barcodes.
 */
public class MainActivity extends AppCompatActivity implements RFIDHandler.ResponseHandlerInterface, DataWedgeHandler.DataWedgeStatusListener {

    private static final String TAG = "MainActivity";
    private static final String PROFILE_NAME = "HHSampleAppProfile";
    
    /** TextView to display RFID connection and operation status. */
    private TextView statusTextViewRFID;
        /**
         * Public accessor for statusTextViewRFID for use by RFIDHandler.
         */
        public TextView getStatusTextViewRFID() {
            return statusTextViewRFID;
        }
    
    /** ListView to display scanned RFID tag data. */
    private ListView tagListView;
    
    /** Adapter for the tag list. */
    private ArrayAdapter<String> tagAdapter;
    
    /** List to hold tag strings for the adapter. */
    private final ArrayList<String> tagList = new ArrayList<>();
    /** TextView to display barcode scan results or scanner status. */
    private TextView scanResult;

    /** Buttons for RFID Inventory control. */
    private Button btnStart;
    private Button btnStop;
    
    /** Button for Barcode Scanning. */
    private Button btnScan;
    
    /** Handler for RFID and Scanner related operations. */
    private RFIDHandler rfidHandler;

    /** Handler for DataWedge status notifications. */
    private DataWedgeHandler dataWedgeHandler;
    
    /** Map to track tag/barcode IDs and their seen counts. */
    private final LinkedHashMap<String, Integer> tagSeenCount = new LinkedHashMap<>();
    /** Map to store a display label (e.g. symbology) per tag/barcode ID. */
    private final LinkedHashMap<String, String> tagLabelMap = new LinkedHashMap<>();
    
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;
    private boolean wasConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextViewRFID = findViewById(R.id.textViewStatusrfid);
        if (statusTextViewRFID != null) {
            statusTextViewRFID.setOnClickListener(v -> {
                if (rfidHandler != null) {
                    rfidHandler.toggleConnection();
                }
            });
        }

        scanResult = findViewById(R.id.scanResult);
        
        // Initialize ListView and Adapter
        tagListView = findViewById(R.id.tag_list);
        tagAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tagList);
        if (tagListView != null) {
            tagListView.setAdapter(tagAdapter);
        }
        
        btnStart = findViewById(R.id.TestButton);
        btnStop = findViewById(R.id.TestButton2);
        btnScan = findViewById(R.id.scan);
        
        // Initially inventory is not running and reader is not connected
        if (btnStart != null) btnStart.setEnabled(false);
        if (btnStop != null) btnStop.setEnabled(false);
        
        // Initially disable scan button until session established
        if (btnScan != null) btnScan.setEnabled(false);

        rfidHandler = new RFIDHandler();
        dataWedgeHandler = new DataWedgeHandler(this, this);
        checkPermissionsAndInit();
    }

    /**
     * Updates the reader status UI with appropriate colors.
     * @param status The status message to display.
     * @param isConnected Whether the reader is connected.
     */
    public void updateReaderStatus(String status, boolean isConnected) {
        runOnUiThread(() -> {
            if (statusTextViewRFID != null) {
                statusTextViewRFID.setText(status);
                if (isConnected) {
                    statusTextViewRFID.setTextColor(ContextCompat.getColor(this, R.color.status_connected));
                    if (btnStart != null) btnStart.setEnabled(true);
                    if (!wasConnected) {
                        Log.d(TAG, "State change: disconnected -> connected, playing connect sound");
                        playConnectSound();
                        wasConnected = true;
                    }
                } else {
                    statusTextViewRFID.setTextColor(ContextCompat.getColor(this, R.color.status_disconnected));
                    if (btnStart != null) btnStart.setEnabled(false);
                    if (btnStop != null) btnStop.setEnabled(false);
                    if (wasConnected) {
                        Log.d(TAG, "State change: connected -> disconnected, playing disconnect sound");
                        playDisconnectSound();
                        wasConnected = false;
                    }
                }
            }
        });
    }

    private void playConnectSound() {
        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                new Handler(Looper.getMainLooper()).postDelayed(toneGen::release, 300);
            }, 300);
        } catch (Exception e) {
            Log.e(TAG, "Error playing connect sound", e);
        }
    }

    private void playDisconnectSound() {
        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
            toneGen.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 400);
            new Handler(Looper.getMainLooper()).postDelayed(toneGen::release, 500);
        } catch (Exception e) {
            Log.e(TAG, "Error playing disconnect sound", e);
        }
    }

    /**
     * Checks for necessary Bluetooth permissions and initializes the RFID handler.
     * Required for Android 12 (API 31) and higher.
     */
    private void checkPermissionsAndInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                        BLUETOOTH_PERMISSION_REQUEST_CODE);
            } else {
                rfidHandler.onCreate(this);
            }
        } else {
            rfidHandler.onCreate(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                rfidHandler.onCreate(this);
            } else {
                Toast.makeText(this, "Bluetooth Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        String result;
        if (id == R.id.antenna_settings) {
            result = rfidHandler.Test1();
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.Singulation_control) {
            result = rfidHandler.Test2();
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.Default) {
            result = rfidHandler.Defaults();
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        rfidHandler.onPause();
        if (dataWedgeHandler != null) {
            dataWedgeHandler.unregisterReceiver();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        rfidHandler.onResume();
        if (dataWedgeHandler != null) {
            dataWedgeHandler.registerReceiver();
            dataWedgeHandler.createProfile(PROFILE_NAME);
            dataWedgeHandler.registerForNotifications();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rfidHandler.onDestroy();
    }

    /**
     * Toggles the enabled state of the Inventory control buttons.
     * @param isRunning True if inventory is currently running.
     */
    private void toggleInventoryButtons(boolean isRunning) {
        runOnUiThread(() -> {
            if (btnStart != null) btnStart.setEnabled(!isRunning);
            if (btnStop != null) btnStop.setEnabled(isRunning);
        });
    }

    /**
     * Enables or disables the scan button.
     * @param enabled True to enable the button.
     */
    public void setScanButtonEnabled(boolean enabled) {
        runOnUiThread(() -> {
            if (btnScan != null) {
                btnScan.setEnabled(enabled);
            }
        });
    }

    /**
     * Called when the Start Inventory button is clicked.
     * @param view The view that was clicked.
     */
    public void StartInventory(View view) {
        toggleInventoryButtons(true);
        clearTagData();
        rfidHandler.performInventory();
    }

    private void clearTagData() {
        runOnUiThread(() -> {
            tagSeenCount.clear();
            tagLabelMap.clear();
            tagList.clear();
            if (tagAdapter != null) {
                tagAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Called when the Scan Code button is clicked.
     * @param view The view that was clicked.
     */
    public void scanCode(View view) {
        rfidHandler.scanCode();
    }

    /**
     * Called when the Stop Inventory button is clicked.
     * @param view The view that was clicked.
     */
    public void StopInventory(View view) {
        toggleInventoryButtons(false);
        rfidHandler.stopInventory();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void handleTagdata(TagData[] tagData) {
        if (tagData == null || tagData.length == 0) return;

        // Build intermediate list of tag info from the callback data
        final ArrayList<String[]> candidates = new ArrayList<>();
        for (TagData tag : tagData) {
            if (tag == null) continue;
            String tagId = tag.getTagID();
            if (tagId != null) {
                candidates.add(new String[]{tagId, String.valueOf(tag.getPeakRSSI())});
            }
        }

        if (!candidates.isEmpty()) {
            runOnUiThread(() -> {
                for (String[] pair : candidates) {
                    String tagId = pair[0];
                    String rssi = pair[1];
                    int count = tagSeenCount.getOrDefault(tagId, 0) + 1;
                    tagSeenCount.put(tagId, count);
                }
                rebuildTagList();
                updateUniqueTagCount(tagSeenCount.size());
            });
        }
    }

    /** Rebuilds the display list from the tagSeenCount map. Must be called on UI thread. */
    private void rebuildTagList() {
        tagList.clear();
        for (Map.Entry<String, Integer> entry : tagSeenCount.entrySet()) {
            String id = entry.getKey();
            int count = entry.getValue();
            String label = tagLabelMap.get(id);
            if (label != null) {
                tagList.add(id + " (" + label + ")  x" + count);
            } else {
                tagList.add(id + "  x" + count);
            }
        }
        if (tagAdapter != null) {
            tagAdapter.notifyDataSetChanged();
        }
    }

    private void updateUniqueTagCount(int totalUniqueTags) {
        if (statusTextViewRFID != null && statusTextViewRFID.getText() != null) {
            String statusStr = statusTextViewRFID.getText().toString();
            if (statusStr.contains("Connected")) {
                String[] parts = statusStr.split("\n");
                String currentStatus = parts.length > 0 ? parts[0] : statusStr;
                statusTextViewRFID.setText(currentStatus + "\nUnique Tags: " + totalUniqueTags);
            }
        }
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
        toggleInventoryButtons(pressed);
        if (pressed) {
            clearTagData();
            rfidHandler.performInventory();
        } else {
            rfidHandler.stopInventory();
        }
    }

    @Override
    public void barcodeData(String val) {
        runOnUiThread(() -> {
            if (scanResult != null) {
                scanResult.setText(String.format("Scan Result : %s", val != null ? val : ""));
            }
            if (val != null) {
                int count = tagSeenCount.getOrDefault(val, 0) + 1;
                tagSeenCount.put(val, count);
                tagLabelMap.put(val, "Barcode");
                rebuildTagList();
                updateUniqueTagCount(tagSeenCount.size());
            }
            Toast.makeText(MainActivity.this, "Barcode: " + val, Toast.LENGTH_SHORT).show();
        });
    }

    public void barcodeData(String val, String symbology) {
        runOnUiThread(() -> {
            if (scanResult != null) {
                scanResult.setText(String.format("Scan Result: %s (%s)", val != null ? val : "", symbology != null ? symbology : ""));
            }
            if (val != null) {
                int count = tagSeenCount.getOrDefault(val, 0) + 1;
                tagSeenCount.put(val, count);
                tagLabelMap.put(val, symbology != null ? symbology : "Barcode");
                rebuildTagList();
                updateUniqueTagCount(tagSeenCount.size());
            }
            Toast.makeText(MainActivity.this, "Barcode: " + val + " (" + symbology + ")", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void sendToast(String val) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, val, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onScannerStatusChange(String status) {
        runOnUiThread(() -> {
            Log.d(TAG, "DataWedge Scanner Status Changed: " + status);
            if (scanResult != null) {
                scanResult.setText(String.format("Scanner Status: %s", status));
            }
        });
    }

    @Override
    public void onBarcodeScanned(String barcode, String symbology) {
        barcodeData(barcode, symbology);
    }

}
