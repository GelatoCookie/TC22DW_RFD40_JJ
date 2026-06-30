# Zebra RFID & Barcode Sample App (TC501-RC0)

This Android app demonstrates Zebra RFID and Barcode integration, using Zebra RFID SDK and DataWedge API for asset tracking and data collection. It supports USB and Bluetooth connections, real-time inventory, barcode scanning, and hardware trigger events.

## Features

- **RFID Inventory:** Connect to Zebra RFID readers (RFD40, RFD8500, MC33xxR) via USB or Bluetooth for high-speed inventory.
- **Auto-Reconnect:** Automatically reconnects to the reader when USB is unplugged or a "QC" reader is detected.
- **Barcode Scanning:** Uses Zebra DataWedge for seamless barcode scanning (1D/2D) and manual "Soft Scan" triggers.
- **Smart UI:**
    - Persistent DataWedge status monitoring.
    - Dynamic visibility: "Pull Trigger" button automatically hides if the Scanner SDK is unavailable.
- **Error Resilience:** Detects and reports Zebra SDK/Firmware version mismatches (e.g., `unRegisterQrs` error).
- **Real-time UI Updates:** Shows scanned tag/barcode data and unique tag count.
- **Trigger Handling:** Supports hardware triggers for inventory and barcode scans.
- **Permissions Management:** Handles Bluetooth permissions for Android 12+.

## Prerequisites

- **Hardware:** Zebra RFID Handheld Reader (RFD40, RFD8500, MC33xxR).
- **Software:**
    - Android Studio Hedgehog or newer
    - Zebra RFID SDK for Android (rfidapi3lib-2.0.5.292)
    - Zebra DataWedge (pre-installed)
    - **Zebra RFID Service:** Latest version recommended for SDK compatibility.

## Usage

- **Connect:** Tap the status text or use the Readers List menu to connect/disconnect.
- **Inventory:** Click "Start" or use hardware trigger to scan tags. Click "Stop" to stop.
- **Barcode:** 
    - Click "Pull Trigger" for Scanner SDK-based scanning.
    - Click "DW Start/Stop" for DataWedge Soft Scan.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

---
*Developed as a sample application for Zebra RFID SDK integration.*
