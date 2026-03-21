package com.zebra.rfid.demo.sdksample;

import android.content.Context;

/**
 * Abstract class to handle all UI updates from background handlers.
 */
public abstract class MainUIHandler {
    
    public enum UpdateType {
        READER_STATUS,
        SCAN_BUTTON_STATE,
        TAG_DATA,
        TRIGGER_PRESS,
        BARCODE_DATA,
        TOAST_MESSAGE
    }

    /**
     * Abstract method to handle UI updates.
     * @param type The type of update to perform.
     * @param data The data associated with the update.
     */
    public abstract void handleUIUpdate(UpdateType type, Object... data);

    /**
     * Provides the context for SDK initialization.
     * @return The application or activity context.
     */
    public abstract Context getContext();
}
