# cordova-plugin-resqpeernet
A Cordova plugin that provides secure peer-to-peer and mesh-hybrid communication (BLE, Wi-Fi Direct, LAN, WebRTC, WebSocket) with auto-connect and emergency-ready features, designed for both everyday use and disaster scenarios.

### Supported Platforms

- Android
- Browser
- Electron

```bash
cordova-plugin-resqpeernet/
│
├── plugin.xml
├── package.json
├── README.md
├── SECURITY.md
├── API_REFERENCE.md
│
├── www/
│   └── resqpeernet.js        # runtime
│
├── src/
│   ├── js/
│   │   └── resqpeernet.js    # developer
│   ├── android/
│   │   └── ResqPeerNet.java  # native Android
│   └── ios/
│       └── ResqPeerNet.m     # native iOS
│
└── docs/
    ├── INSTALL.md
    ├── USAGE.md
    └── STORAGE_SCHEMA.md

```

## Installation

```bash
cordova plugin add https://github.com/MuhammadAndikCahyono/cordova-plugin-resqpeernet
```
## Core Service

| API                     | Tipe              | Deskripsi                                                 | Contoh                                                                          |
| ----------------------- | ----------------- | --------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `startService(options)` | Method            | Menyalakan service background agar tetap hidup.           | `cordova.plugins.resqpeernet.startService({ keepAlive: true })`                       |
| `stopService()`         | Method            | Mematikan service background.                             | `cordova.plugins.resqpeernet.stopService()`                                           |
| `isRunning()`           | Method            | Mengecek apakah service sedang aktif.                     | `cordova.plugins.resqpeernet.isRunning(status => console.log(status))`                |
| `onServiceStarted(cb)`  | Event             | Listener saat service berhasil start.                     | `cordova.plugins.resqpeernet.onServiceStarted(() => console.log("Service aktif"))`    |
| `onServiceStopped(cb)`  | Event             | Listener saat service berhenti.                           | `cordova.plugins.resqpeernet.onServiceStopped(() => console.log("Service berhenti"))` |
| `onError(cb)`           | Event             | Listener ketika terjadi error di plugin.                  | `cordova.plugins.resqpeernet.onError(err => console.error(err))`                      |
| `startAuto(options)`    | Method (opsional) | Wrapper otomatis `startService()` + setup listener dasar. | `cordova.plugins.resqpeernet.startAuto({ keepAlive: true })`                          |


## Peer-to-Peer (P2P)

| API                        | Tipe   | Deskripsi                           | Contoh                                                              |
| -------------------------- | ------ | ----------------------------------- | ------------------------------------------------------------------- |
| `connect(peerId, options)` | Method | Membuka koneksi ke peer tertentu.   | `cordova.plugins.resqpeernet.connect("peer-123", { secure: true })`       |
| `disconnect(peerId)`       | Method | Memutus koneksi ke peer tertentu.   | `cordova.plugins.resqpeernet.disconnect("peer-123")`                      |
| `getPeers()`               | Method | Mendapatkan daftar peer yang aktif. | `cordova.plugins.resqpeernet.getPeers(peers => console.log(peers))`       |
| `onPeerDiscovered(cb)`     | Event  | Listener saat peer baru ditemukan.  | `cordova.plugins.resqpeernet.onPeerDiscovered(peer => console.log(peer))` |

## Messaging

| API                        | Tipe   | Deskripsi                           | Contoh                                                       |
| -------------------------- | ------ | ----------------------------------- | ------------------------------------------------------------ |
| `sendMessage(peerId, msg)` | Method | Mengirim pesan ke peer tertentu.    | `cordova.plugins.resqpeernet.sendMessage("peer-123", "Halo!")`     |
| `broadcastMessage(msg)`    | Method | Mengirim pesan ke semua peer aktif. | `cordova.plugins.resqpeernet.broadcastMessage("Halo semua peer!")` |
| `onMessage(cb)`            | Event  | Listener saat pesan masuk diterima. | `cordova.plugins.resqpeernet.onMessage(msg => console.log(msg))`   |
