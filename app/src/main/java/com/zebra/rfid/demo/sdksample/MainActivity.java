
package com.zebra.rfid.demo.sdksample;

import android.Manifest;
import android.animation.ObjectAnimator;
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
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
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

    /**
     * Show a modern Snackbar message instead of Toast.
     */
    private void showSnackbar(String message) {
        View root = findViewById(android.R.id.content);
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private static final String TAG = "MainActivity";
    private static final String PROFILE_NAME = "HHSampleAppProfile";
    
    /** UI components */
    private TextView statusTextViewRFID;
    private TextView scanResult;
    private TextView tagCountLabel;
    private ImageView statusDotIcon;
    private MaterialButton btnScan;
    private MaterialButton btnStart;
    private MaterialButton btnStop;
    private ListView tagListView;
    private ArrayList<String> tagList = new ArrayList<>();
    private ArrayAdapter<String> tagAdapter;
    private ObjectAnimator flashAnimator;

    private boolean isConnected = false;

    /**
     * Public accessor for statusTextViewRFID for use by RFIDHandler.
     */
    public TextView getStatusTextViewRFID() {
        return statusTextViewRFID;
    }
    
    /** Handler for RFID and Scanner related operations. */
    private RFIDHandler rfidHandler;

    /** Handler for DataWedge status notifications. */
    private DataWedgeHandler dataWedgeHandler;
    
    /** Map to track tag/barcode IDs and their seen counts. */
    private final LinkedHashMap<String, Integer> tagSeenCount = new LinkedHashMap<>();
    /** Map to store a display label (e.g. symbology) per tag/barcode ID. */
    private final LinkedHashMap<String, String> tagLabelMap = new LinkedHashMap<>();
    
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        statusTextViewRFID = findViewById(R.id.textViewStatusrfid);
        scanResult = findViewById(R.id.scanResult);
        tagCountLabel = findViewById(R.id.tagCountLabel);
        btnScan = findViewById(R.id.scan);
        btnStart = findViewById(R.id.TestButton);
        btnStop = findViewById(R.id.TestButton2);
        tagListView = findViewById(R.id.tag_list);

        // Find status dot as ImageView to support flash animation
        View statusDot = findViewById(R.id.statusDot);
        if (statusDot instanceof ImageView) {
            statusDotIcon = (ImageView) statusDot;
        }

        tagAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tagList);
        if (tagListView != null) {
            tagListView.setAdapter(tagAdapter);
        }

        if (statusTextViewRFID != null) {
            statusTextViewRFID.setOnClickListener(v -> {
                if (rfidHandler != null) {
                    rfidHandler.toggleConnection();
                }
            });
        }

        rfidHandler = new RFIDHandler();
        dataWedgeHandler = new DataWedgeHandler(this, this);
        checkPermissionsAndInit();
    }

    /**
     * Updates the reader status UI.
     */
    public void updateReaderStatus(String status, boolean connected) {
        boolean stateChanged = (this.isConnected != connected);
        this.isConnected = connected;
        runOnUiThread(() -> {
            if (statusTextViewRFID != null) {
                statusTextViewRFID.setText(status);
                int color = connected ? ContextCompat.getColor(this, R.color.status_connected) 
                                       : ContextCompat.getColor(this, R.color.status_disconnected);
                statusTextViewRFID.setTextColor(color);
            }
            
            if (statusDotIcon != null) {
                if (connected) {
                    statusDotIcon.setImageResource(R.drawable.ic_live_pulse);
                    startFlashAnimation(statusDotIcon);
                    if (stateChanged) {
                        playConnectBeep();
                    }
                } else {
                    statusDotIcon.setImageResource(R.drawable.ic_radio_disconnected);
                    stopFlashAnimation();
                    if (stateChanged) {
                        playDisconnectAlarm();
                    }
                }
            }
            invalidateOptionsMenu();
        });
    }

    private void startFlashAnimation(View view) {
        if (flashAnimator == null) {
            flashAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.2f);
            flashAnimator.setDuration(800);
            flashAnimator.setInterpolator(new LinearInterpolator());
            flashAnimator.setRepeatCount(Animation.INFINITE);
            flashAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        }
        if (!flashAnimator.isRunning()) {
            flashAnimator.start();
        }
    }

    private void stopFlashAnimation() {
        if (flashAnimator != null && flashAnimator.isRunning()) {
            flashAnimator.cancel();
            if (statusDotIcon != null) {
                statusDotIcon.setAlpha(1f);
            }
        }
    }

    private void playConnectBeep() {
        playBarcodeBeep();
        new Handler(Looper.getMainLooper()).postDelayed(this::playBarcodeBeep, 250);
    }

    private void playDisconnectAlarm() {
        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
            // Use TONE_SUP_ERROR for a distinct alarm sound
            toneGen.startTone(ToneGenerator.TONE_SUP_ERROR, 500);
            new Handler(Looper.getMainLooper()).postDelayed(toneGen::release, 600);
        } catch (Exception e) {
            Log.e(TAG, "Error playing disconnect alarm", e);
        }
    }

    private void playBarcodeBeep() {
        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
            new Handler(Looper.getMainLooper()).postDelayed(toneGen::release, 250);
        } catch (Exception e) {
            Log.e(TAG, "Error playing barcode beep", e);
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
                showSnackbar("Bluetooth Permissions not granted");
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem connectItem = menu.findItem(R.id.menu_connect);
        MenuItem disconnectItem = menu.findItem(R.id.menu_disconnect);
        if (connectItem != null && disconnectItem != null) {
            connectItem.setVisible(!isConnected);
            disconnectItem.setVisible(isConnected);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_connect) {
            if (rfidHandler != null) {
                rfidHandler.toggleConnection();
                showSnackbar("Connecting...");
            }
            return true;
        } else if (id == R.id.menu_disconnect) {
            if (rfidHandler != null) {
                rfidHandler.disconnect();
                showSnackbar("Disconnecting...");
            }
            return true;
        }
        
        String result;
        if (id == R.id.antenna_settings) {
            result = rfidHandler.Test1();
            showSnackbar(result);
            return true;
        } else if (id == R.id.Singulation_control) {
            result = rfidHandler.Test2();
            showSnackbar(result);
            return true;
        } else if (id == R.id.Default) {
            result = rfidHandler.Defaults();
            showSnackbar(result);
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
            if (tagCountLabel != null) {
                tagCountLabel.setText("0 tags");
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
        // Show most recently seen at the top
        ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<>(tagSeenCount.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            Map.Entry<String, Integer> entry = entries.get(i);
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
        if (tagCountLabel != null) {
            tagCountLabel.setText(totalUniqueTags + (totalUniqueTags == 1 ? " tag" : " tags"));
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
                scanResult.setText(val);
            }
            playBarcodeBeep();
        });
    }

    @Override
    public void sendToast(String val) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, val, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onScannerStatusChange(String status) {
        Log.d(TAG, "Scanner Status: " + status);
    }

    @Override
    public void onBarcodeScanned(String barcode, String symbology) {
        runOnUiThread(() -> {
            if (scanResult != null) {
                scanResult.setText(barcode + " (" + symbology + ")");
            }
            playBarcodeBeep();
            
            // Also add to the list
            int count = tagSeenCount.getOrDefault(barcode, 0) + 1;
            tagSeenCount.put(barcode, count);
            tagLabelMap.put(barcode, symbology);
            rebuildTagList();
            updateUniqueTagCount(tagSeenCount.size());
        });
    }
}
