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

Below is a summary of the methods and events so that the **www/resqpeernet.js** wrapper and the Android side (**ResqPeerNet.java**) can stay in sync:

### 📌 API Methods (called from JavaScript)

| Category        | Method (JS → Native)                     | Short Description                                              |
| --------------- | ---------------------------------------- | -------------------------------------------------------------- |
| **Core**        | `listenEvents()`                         | Enable event stream to JS                                      |
|                 | `connect(options)`                       | Connection: Wi-Fi Direct / BLE / mesh-hybrid                   |
|                 | `disconnect()`                           | Disconnect Wi-Fi Direct                                        |
|                 | `sendMessage(msg)`                       | Send text via Wi-Fi Direct TCP                                 |
|                 | `discoverPeers()` / `getPeers()`         | Discover list of Wi-Fi Direct peers                            |
| **mDNS**        | `startMDNSServer(name,port)`             | Start mDNS server                                              |
|                 | `stopMDNSServer()`                       | Stop mDNS server                                               |
|                 | `startMDNSDiscovery()`                   | Discover other mDNS services                                   |
|                 | `startMDNSListener()`                    | Listen for mDNS multicast messages                             |
|                 | `sendMDNS(msg)`                          | Send mDNS multicast message                                    |
| **BLE**         | `startBLEScan()` / `stopBLEScan()`       | Scan for BLE devices                                           |
| **Mesh-Hybrid** | `startMeshHybrid()` / `stopMeshHybrid()` | Start / stop combined mDNS + BLE + Wi-Fi Direct                |
|                 | `sendMeshHybrid(msg)`                    | Send message across all media (mDNS, TCP, WebRTC if available) |
| **WebRTC/Data** | `initWebRTC(iceServers)`                 | Initialize ICE server list                                     |
|                 | `createOffer()` / `createAnswer()`       | SDP negotiation                                                |
|                 | `setRemoteDescription(desc)`             | Set remote SDP                                                 |
|                 | `addIceCandidate(candidate)`             | Add ICE candidate                                              |
|                 | `createDataChannel(label)`               | Create DataChannel                                             |
|                 | `sendWebRTC(msg)`                        | Send text via DataChannel                                      |
|                 | `sendFile(filename,base64)`              | Send chunked file through DataChannel                          |
| **WebTorrent**  | `webtorrentSignal(json)`                 | Send WebTorrent signal via mDNS                                |
|                 | `webtorrentSendPiece(id,idx,b64)`        | Send torrent piece                                             |
|                 | `webtorrentRequestPiece(id,idx)`         | Request torrent piece                                          |
| **Wallpaper**   | `getWallpaper()`                         | Retrieve device wallpaper (base64 PNG)                         |
|                 | `listenWallpaperChanged()`               | Event for wallpaper changes                                    |

---

### 📡 Events (Native → JS)

| Event Name                                    | Data Payload                                                                                                        |
| --------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `onWiFiDirectPeers`                           | `{peers:[{deviceName,deviceAddress},...]}`                                                                          |
| `onWiFiDirectConnection`                      | `{isGroupOwner,groupFormed,ownerAddress}`                                                                           |
| `onWiFiDirectMessage`                         | `{message}`                                                                                                         |
| `onBLEDeviceFound`                            | `{name,address}`                                                                                                    |
| `onMDNSServerStarted` / `onMDNSServerStopped` | `{name,port}` / `null`                                                                                              |
| `onMDNSPeerFound` / `onMDNSPeerRemoved`       | `{name,host,port}` / `{name}`                                                                                       |
| `onMDNSData`                                  | `{message}` (raw multicast message)                                                                                 |
| `onMeshHybridStarted` / `onMeshHybridStopped` | `null`                                                                                                              |
| `onIceCandidate`                              | `{sdpMid,sdpMLineIndex,candidate}`                                                                                  |
| `onLocalDescription`                          | `{type,sdp}`                                                                                                        |
| `onDataChannelOpen`                           | `null`                                                                                                              |
| `onDataChannelState`                          | `{state}`                                                                                                           |
| `onDataChannelMessage`                        | `{message}`                                                                                                         |
| **File transfer**                             | `onFileStart` `{id,name,expected}`<br>`onFileProgress` `{id,received,expected}`<br>`onFileReceived` `{id,path}`     |
| **WebTorrent bridge**                         | `onWebTorrentSignal` `{…}`<br>`onWebTorrentPiece` `{id,pieceIndex,data}`<br>`onWebTorrentRequest` `{id,pieceIndex}` |
| **Wallpaper**                                 | `wallpaperChanged` `{data: base64}`                                                                                 |

---


