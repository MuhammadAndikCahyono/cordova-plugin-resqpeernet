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
│
├── www/
│   └── resqpeernet.js        # runtime
└── libs/
│     ├── google-webrtc-1.0.32006.jar
│     ├── jmdns-3.5.5.jar
│     └── jni/
│          ├── armeabi-v7a/libjingle_peerconnection_so.so
│          ├── arm64-v8a/libjingle_peerconnection_so.so
│          ├── x86/libjingle_peerconnection_so.so
│          └── x86_64/libjingle_peerconnection_so.so
├── src/
│   ├── js/
│   │   └── resqpeernet.js    # developer
│   ├── android/
│   │   └── ResqPeerNet.java  # native Android
├── examples/
└── docs/
    ├── INSTALL.md
    ├── USAGE.md
    └── STORAGE_SCHEMA.md

```

## Installation

```bash
cordova plugin add https://github.com/MuhammadAndikCahyono/cordova-plugin-resqpeernet.git
```



