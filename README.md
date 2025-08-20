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


| Fitur                                                                 | **Full** | **Lite** |
| --------------------------------------------------------------------- | -------- | -------- |
| **Mesh-Hybrid (BLE + WiFi Direct + LAN mDNS + WebRTC + WS fallback)** | ✅        | ✅        |
| **Auto-connect background**                                           | ✅        | ✅        |
| **Emergency Mode**                                                    | ✅        | ✅        |
| **File transfer (foto, video, audio, dokumen)**                       | ✅        | ✅        |
| **Live streaming & screen sharing**                                   | ✅        | ✅        |
| **Social API (post, like, komentar)**                                 | ✅        | ✅        |
| **Marketplace API**                                              | ✅        | ✅        |
| **Offline Maps & Nearby**                                             | ✅        | ✅        |
| **NFC connect**                                                       | ✅        | ✅        |
| **Telepon & SMS API (baca & kirim SMS, panggil & terima panggilan)**  | ✅        | ❌        |
| **Akses kontak & riwayat panggilan**                                  | ✅        | ❌        |
| **Kompatibilitas Android 5+**                                         | ✅        | ✅        |
| **Cordova + Browser + Electron support**                              | ✅        | ✅        |


## Installation

```bash
cordova plugin add https://github.com/MuhammadAndikCahyono/cordova-plugin-resqpeernet
```
