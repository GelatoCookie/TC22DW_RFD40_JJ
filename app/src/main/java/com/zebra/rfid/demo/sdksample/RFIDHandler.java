package com.zebra.rfid.demo.sdksample;

import android.util.Log;
import android.widget.TextView;

import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.IRFIDLogger;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.TagData;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.SDKHandler;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler class for RFID operations.
 * This class encapsulates the Zebra RFID API logic.
 */
class RFIDHandler implements Readers.RFIDReaderEventHandler {

    private static final String TAG = "RFID_SAMPLE";
    private Readers readers;
    private ArrayList<ReaderDevice> availableRFIDReaderList;
    private ReaderDevice readerDevice;
    private RFIDReader reader;
    private TextView textView;
    private EventHandler eventHandler;
    private MainActivity context;
    private SDKHandler sdkHandler;
    private ScannerHandler scannerHandler;
    private ArrayList<DCSScannerInfo> scannerList;
    private int scannerID;
    private final int MAX_POWER = 270;
    private final String readerName = "RFD4031-G10B700-WR";
    private volatile boolean isInventoryRunning = false;
    
    /** Executor for background tasks. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Initializes the RFIDHandler with the activity context.
     * @param activity The MainActivity context.
     */
    void onCreate(MainActivity activity) {
        context = activity;
        textView = activity.getStatusTextViewRFID();
        scannerList = new ArrayList<>();
        scannerHandler = new ScannerHandler(activity);
        initSDK();
    }

    public String Test1() { return "TO DO"; }
    public String Test2() { return "TODO2"; }

    /**
     * @return The Zebra RFID SDK version string.
     */
    public String getSDKVersion() {
        return com.zebra.rfid.api3.BuildConfig.VERSION_NAME;
    }

    /**
     * Resets the reader settings to defaults.
     * @return Success or error message.
     */
    public String Defaults() {
        if (!isReaderConnected()) return "Not connected";
        try {
            Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(MAX_POWER);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);

            Antennas.SingulationControl singulationControl = reader.Config.Antennas.getSingulationControl(1);
            singulationControl.setSession(SESSION.SESSION_S0);
            singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
            singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, singulationControl);
        } catch (InvalidUsageException | OperationFailureException e) {
            Log.e(TAG, "Error in Defaults", e);
            return e.getMessage();
        }
        return "Default settings applied";
    }

    /**
     * @return List of available RFID readers.
     */
    public ArrayList<ReaderDevice> getAvailableRFIDReaderList() {
        return availableRFIDReaderList;
    }

    /**
     * Connects to a specific reader device.
     * @param device The reader device to connect to.
     */
    public void connectToReader(ReaderDevice device) {
        if (device == null) return;
        executor.execute(() -> {
            synchronized (RFIDHandler.this) {
                if (isReaderConnected()) {
                    disconnect();
                }
                readerDevice = device;
                reader = readerDevice.getRFIDReader();
                String result = connect();
                if (context != null) {
                    context.updateReaderStatus(result, isReaderConnected());
                }
            }
        });
    }

    private boolean isReaderConnected() {
        return reader != null && reader.isConnected();
    }

    /**
     * Toggles the connection to the reader.
     * If connected, it disconnects. If disconnected, it attempts to connect.
     */
    public void toggleConnection() {
        if (isReaderConnected()) {
            executor.execute(this::disconnect);
        } else {
            connectReader();
        }
    }

    void onPause() {
        disconnect();
    }

    void onDestroy() {
        dispose();
        executor.shutdown();
    }

    private void initSDK() {
        Log.d(TAG, "initSDK");
        if (readers == null) {
            executor.execute(() -> {
                InvalidUsageException exception = null;

                try {
                    ENUM_TRANSPORT[] transports = {
                        ENUM_TRANSPORT.BLUETOOTH,
                        ENUM_TRANSPORT.SERVICE_USB,
                        ENUM_TRANSPORT.RE_USB,
                        ENUM_TRANSPORT.SERVICE_SERIAL,
                        ENUM_TRANSPORT.RE_SERIAL,
                        ENUM_TRANSPORT.QC_SERIAL,
                        ENUM_TRANSPORT.ALL
                    };

                    for (ENUM_TRANSPORT transport : transports) {
                        try {
                            Log.d(TAG, "Trying transport: " + transport.name());
                            if (readers == null) {
                                readers = new Readers(context, transport);
                            } else {
                                readers.setTransport(transport);
                            }
                            ArrayList<ReaderDevice> list = readers.GetAvailableRFIDReaderList();
                            availableRFIDReaderList = (list != null) ? new ArrayList<>(list) : new ArrayList<>();
                            if (!availableRFIDReaderList.isEmpty()) {
                                Log.d(TAG, "Readers found using transport: " + transport.name());
                                exception = null;
                                break;
                            }
                        } catch (Throwable e) {
                            Log.e(TAG, "Error with transport " + transport.name(), e);
                            if (e instanceof InvalidUsageException) {
                                exception = (InvalidUsageException) e;
                            } else {
                                exception = new InvalidUsageException(e.getMessage(), "Transport error");
                            }
                        }
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "Unexpected error in transport loop", e);
                }
                
                final InvalidUsageException finalException = exception;
                if (context != null) {
                    context.runOnUiThread(() -> {
                        if (finalException != null) {
                            context.sendToast("Failed to get Available Readers\n" + finalException.getInfo());
                            readers = null;
                            context.updateReaderStatus("Failed to get Readers", false);
                        } else if (availableRFIDReaderList.isEmpty()) {
                            context.sendToast("No Available Readers to proceed");
                            readers = null;
                            context.updateReaderStatus("No Readers Found", false);
                        } else {
                            connectReader();
                        }
                    });
                }
            });
        } else {
            connectReader();
        }
    }

    private void connectReader() {
        // Offload the entire connection process to a background thread to keep UI responsive
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // Update UI to show connection is in progress
                if (context != null) {
                    context.updateReaderStatus("Connecting...", false);
                }

                synchronized (RFIDHandler.this) {
                    if (!isReaderConnected()) {
                        getAvailableReader();
                        String result = (reader != null) ? connect() : "Failed to find reader";
                        
                        // Update UI with the final result
                        if (context != null) {
                            context.updateReaderStatus(result, isReaderConnected());
                        }
                    } else {
                        // Already connected, just update UI
                        if (context != null) {
                            context.updateReaderStatus("Connected: " + reader.getHostName(), true);
                        }
                    }
                }
            }
        });
    }

    private synchronized void getAvailableReader() {
        if (readers != null) {
            readers.attach(this);
            try {
                ArrayList<ReaderDevice> availableReaders = readers.GetAvailableRFIDReaderList();
                if (availableReaders != null && !availableReaders.isEmpty()) {
                    availableRFIDReaderList = new ArrayList<>(availableReaders);
                    if (availableRFIDReaderList.size() == 1) {
                        readerDevice = availableRFIDReaderList.get(0);
                        reader = readerDevice.getRFIDReader();
                    } else {
                        for (ReaderDevice device : availableRFIDReaderList) {
                            if (device != null && device.getName() != null && device.getName().startsWith(readerName)) {
                                readerDevice = device;
                                reader = readerDevice.getRFIDReader();
                                break;
                            }
                        }
                    }
                }
            } catch (InvalidUsageException e) {
                Log.e(TAG, "Error getting available readers", e);
            }
        }
    }

    @Override
    public void RFIDReaderAppeared(ReaderDevice readerDevice) {
        connectReader();
    }

    @Override
    public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
        if (readerDevice != null) {
            if (context != null) context.sendToast("RFIDReaderDisappeared: " + readerDevice.getName());
            if (reader != null && readerDevice.getName() != null
                    && readerDevice.getName().equalsIgnoreCase(reader.getHostName())) {
                disconnect();
            }
        }
    }

    private synchronized String connect() {
        if (reader != null) {
            try {
                if (!reader.isConnected()) {
                    reader.connect();
                    configureReader();
                    setupScannerSDK();
                    if (reader.isConnected()) {
                        return "Connected: " + reader.getHostName();
                    }
                } else {
                    return "Connected: " + reader.getHostName();
                }
            } catch (Throwable t) {
                Log.e(TAG, "Connection failed", t);
                return "Connection failed: " + t.getMessage();
            }
        }
        return "Disconnected";
    }

    private void configureReader() {
        IRFIDLogger.getLogger("SDKSampleApp").EnableDebugLogs(true);
        if (reader.isConnected()) {
            try {
                if (eventHandler == null) eventHandler = new EventHandler();
                reader.Events.addEventsListener(eventHandler);
                reader.Events.setHandheldEvent(true);
                reader.Events.setTagReadEvent(true);
                reader.Events.setInventoryStartEvent(true);
                reader.Events.setInventoryStopEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);
                reader.Events.setReaderDisconnectEvent(true);
            } catch (InvalidUsageException | OperationFailureException e) {
                Log.e(TAG, "Configuration failed", e);
            }
        }
    }

    public void setupScannerSDK() {
        if (sdkHandler == null) {
            sdkHandler = new SDKHandler(context);
            sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC);
            sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
            sdkHandler.dcssdkSetDelegate(scannerHandler);
            int notifications_mask = DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value;
            sdkHandler.dcssdkSubsribeForEvents(notifications_mask);
        }

        ArrayList<DCSScannerInfo> availableScanners = (ArrayList<DCSScannerInfo>) sdkHandler.dcssdkGetAvailableScannersList();
        if (scannerList != null) {
            scannerList.clear();
        } else {
            scannerList = new ArrayList<>();
        }

        if (availableScanners != null) {
            for (DCSScannerInfo scanner : availableScanners) {
                if (scanner != null) {
                    scannerList.add(scanner);
                }
            }
        }

        if (reader != null && reader.isConnected()) {
            String hostName = reader.getHostName();
            for (DCSScannerInfo device : scannerList) {
                if (device != null && device.getScannerName() != null && hostName != null && device.getScannerName().contains(hostName)) {
                    try {
                        sdkHandler.dcssdkEstablishCommunicationSession(device.getScannerID());
                        scannerID = device.getScannerID();
                    } catch (Exception e) {
                        Log.e(TAG, "Error establishing scanner session", e);
                    }
                }
            }
        }
    }

    public boolean getConnectStatus(){
        if (reader!=null)
            return reader.isConnected();
        return false;
    }

    public synchronized String disconnect() {
        try {
            if (reader != null) {
                if (eventHandler != null) {
                    try {
                        reader.Events.removeEventsListener(eventHandler);
                    } catch (Exception e) {
                        Log.e(TAG, "Error removing events listener", e);
                    }
                }
                if (sdkHandler != null) {
                    try {
                        sdkHandler.dcssdkTerminateCommunicationSession(scannerID);
                    } catch (Exception e) {
                        Log.e(TAG, "Error terminating scanner session", e);
                    }
                }
//                try {
//                    reader.disconnect();
//                } catch (Throwable t) {
//                    Log.e(TAG, "Error during reader.disconnect()", t);
//                }
//                try {
//                    reader.Dispose();
//                } catch (Throwable t) {
//                    Log.e(TAG, "Error during reader.Dispose()", t);
//                }
                reader = null;
                sdkHandler = null;
                isInventoryRunning = false;
                if (context != null)
                    context.updateReaderStatus("Disconnected", false);
                return "Disconnected";
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error during disconnect wrapper", t);
            reader = null;
            sdkHandler = null;
            if (context != null)
                context.updateReaderStatus("Disconnected (with error)", false);
            return "Disconnect failed: " + t.getMessage();
        }
        return "Not connected";
    }

    private synchronized void dispose() {
        disconnect();
        try {
            if (readers != null) {
                readers.Dispose();
                readers = null;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error during dispose", t);
        }
    }

    synchronized void performInventory() {
        try {
            if (reader != null && reader.isConnected()) {
                reader.Actions.Inventory.perform();
                isInventoryRunning = true;
            }
        } catch (InvalidUsageException | OperationFailureException e) {
            Log.e(TAG, "Error performing inventory", e);
            if (context != null) context.sendToast("Inventory error: " + e.getMessage());
        }
    }

    synchronized void stopInventory() {
        try {
            if (reader != null && reader.isConnected() && isInventoryRunning) {
                reader.Actions.Inventory.stop();
                isInventoryRunning = false;
            }
        } catch (InvalidUsageException | OperationFailureException e) {
            Log.e(TAG, "Error stopping inventory", e);
        }
    }

    public void scanCode() {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        executor.execute(() -> executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_PULL_TRIGGER, in_xml, new StringBuilder(), scannerID));
    }

    private boolean executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE opCode, String inXML, StringBuilder outXML, int scannerID) {
        if (sdkHandler != null) {
            StringBuilder response = (outXML != null) ? outXML : new StringBuilder();
            DCSSDKDefs.DCSSDK_RESULT result = sdkHandler.dcssdkExecuteCommandOpCodeInXMLForScanner(opCode, inXML, response, scannerID);
            return result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS;
        }
        return false;
    }

    public class EventHandler implements RfidEventsListener {
        @Override
        public void eventReadNotify(RfidReadEvents e) {
            if (reader == null) return;
            TagData[] myTags = reader.Actions.getReadTags(100);
            if (myTags != null && context != null) {
                executor.execute(() -> context.handleTagdata(myTags));
            }
        }

        @Override
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            if (rfidStatusEvents == null || rfidStatusEvents.StatusEventData == null) return;
            STATUS_EVENT_TYPE eventType = rfidStatusEvents.StatusEventData.getStatusEventType();
            if (eventType == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData != null) {
                    HANDHELD_TRIGGER_EVENT_TYPE triggerEvent = rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent();
                    boolean pressed = (triggerEvent == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED);
                    if (context != null) {
                        executor.execute(() -> context.handleTriggerPress(pressed));
                    }
                }
            } else if (eventType == STATUS_EVENT_TYPE.INVENTORY_START_EVENT) {
                isInventoryRunning = true;
            } else if (eventType == STATUS_EVENT_TYPE.INVENTORY_STOP_EVENT) {
                isInventoryRunning = false;
            } else if (eventType == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {
                executor.execute(() -> {
                    disconnect();
                    dispose();
                });
            }
        }
    }

    interface ResponseHandlerInterface {
        void handleTagdata(TagData[] tagData);
        void handleTriggerPress(boolean pressed);
        void barcodeData(String val);
        void sendToast(String val);
    }
}
