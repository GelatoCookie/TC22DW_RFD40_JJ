# Zebra RFID & Barcode Sample App (AI Enhanced)

This Android app demonstrates Zebra RFID and Barcode integration, using Zebra RFID SDK and DataWedge API for asset tracking and data collection. It supports USB and Bluetooth connections, real-time inventory, barcode scanning, and hardware trigger events. The UI updates with scanned tags and barcodes, and permissions are managed for Android 12+.

## Features

- **RFID Inventory:** Connect to Zebra RFID readers (RFD40, RFD8500, MC33xxR) via USB or Bluetooth for high-speed inventory.
- **Barcode Scanning:** Uses Zebra DataWedge for seamless barcode scanning (1D/2D).
- **Real-time UI Updates:** Shows scanned tag/barcode data and unique tag count.
- **Trigger Handling:** Supports hardware triggers for inventory and barcode scans.
- **Permissions Management:** Handles Bluetooth permissions for Android 12+.
- **Configurable Settings:** Access antenna and singulation controls from menu.

## Prerequisites

- **Hardware:** Zebra RFID Handheld Reader (RFD40, RFD8500, MC33xxR).
- **Software:**
    - Android Studio Bumblebee or newer
    - Zebra RFID SDK for Android
    - Zebra DataWedge (pre-installed)

## Project Structure

- `MainActivity.java`: Main entry point, handles UI and coordinates RFID/Barcode handlers.
- `RFIDHandler.java`: Manages Zebra RFID SDK logic, connection, inventory, and events.
- `DataWedgeHandler.java`: Creates DataWedge profile, listens for barcode/scanner status via Broadcast Intents.
- `ScannerHandler.java`: Handles Zebra Scanner SDK events (implicit).

## Getting Started

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/HHSampleAppAI.git
    ```
2.  **Open in Android Studio:** Import the project and let Gradle sync.
3.  **Build & Deploy:** Run the app on a Zebra Android device.
4.  **Configuration:**
    - Enable Bluetooth for wireless connection.
    - App auto-creates DataWedge profile `HHSampleAppProfile` on first run.

## Usage

- **Connect:** Tap the status text to connect/disconnect to the nearest Zebra reader.
- **Inventory:** Click "Start Inventory" or use hardware trigger to scan tags. Click "Stop Inventory" or release trigger to stop.
- **Scan Barcode:** Click "Scan" to trigger barcode scanning.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

---
*Developed as a sample application for Zebra RFID SDK integration.*
