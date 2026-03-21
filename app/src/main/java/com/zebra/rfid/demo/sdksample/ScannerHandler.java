package com.zebra.rfid.demo.sdksample;

import android.util.Log;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;

/**
 * Separated delegate handler for Zebra Scanner SDK events.
 */
public class ScannerHandler implements IDcsSdkApiDelegate {
    private static final String TAG = "ScannerHandler";
    private final MainActivity context;

    public ScannerHandler(MainActivity context) {
        this.context = context;
    }

    @Override
    public void dcssdkEventScannerAppeared(DCSScannerInfo dcsScannerInfo) {
        Log.d(TAG, "Scanner appeared: " + dcsScannerInfo.getScannerName());
    }

    @Override
    public void dcssdkEventScannerDisappeared(int i) {
        Log.d(TAG, "Scanner disappeared, ID: " + i);
        if (context != null) {
            context.setScanButtonEnabled(false);
        }
    }

    @Override
    public void dcssdkEventCommunicationSessionEstablished(DCSScannerInfo dcsScannerInfo) {
        Log.d(TAG, "Communication session established: " + dcsScannerInfo.getScannerName());
        if(context != null) {
            context.sendToast("Scanner established: " + dcsScannerInfo.getScannerName());
            context.setScanButtonEnabled(true);
        }
    }

    @Override
    public void dcssdkEventCommunicationSessionTerminated(int i) {
        Log.d(TAG, "Communication session terminated, ID: " + i);
        if (context != null) {
            context.setScanButtonEnabled(false);
        }
    }

    @Override
    public void dcssdkEventBarcode(byte[] barcodeData, int barcodeType, int fromScannerID) {
        String s = new String(barcodeData);
        if (context != null) {
            context.barcodeData(s);
        }
        Log.d(TAG, "Barcode scanned: " + s);
    }

    @Override public void dcssdkEventImage(byte[] bytes, int i) {}
    @Override public void dcssdkEventVideo(byte[] bytes, int i) {}
    @Override public void dcssdkEventBinaryData(byte[] bytes, int i) {}
    @Override public void dcssdkEventFirmwareUpdate(FirmwareUpdateEvent firmwareUpdateEvent) {}
    @Override public void dcssdkEventAuxScannerAppeared(DCSScannerInfo dcsScannerInfo, DCSScannerInfo dcsScannerInfo1) {}
}
