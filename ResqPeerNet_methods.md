# 📱 ResqPeerNet Plugin - Complete Method Documentation

## 🏗️ **Basic Plugin Methods**

| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `getPluginInfo` | `callbackContext` | Get plugin information (name, version, status, modules) | `JSONObject` |
| `checkStatus` | `callbackContext` | Check plugin and all modules status | `JSONObject` |
| `testConnection` | `callbackContext` | Test plugin connectivity and module availability | `JSONObject` |
| `getSystemInfo` | `callbackContext` | Get comprehensive device system information | `JSONObject` |

## 🔐 **Permission Management**

| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `getPermissionStatus` | `callbackContext` | Get current permission status for all types | `JSONObject` |
| `requestPermission` | `args, callbackContext` | Request single permission with action context | `JSONObject` |
| `requestPermissions` | `args, callbackContext` | Request multiple permissions by type | `JSONObject` |
| `hasStoragePermission` | `callbackContext` | Check if storage permissions are granted | `boolean` |
| `hasNetworkPermissions` | `callbackContext` | Check if network permissions are granted | `boolean` |

## 🖼️ **Wallpaper Management**

| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `getWallpaper` | `args, callbackContext` | Retrieve current wallpaper image/data | `JSONObject` |
| `getWallpaperInfo` | `args, callbackContext` | Get wallpaper metadata and properties | `JSONObject` |
| `listenWallpaperChanged` | `args, callbackContext` | Start listening for wallpaper change events | `void` |
| `stopListeningWallpaper` | `callbackContext` | Stop wallpaper change listener | `String` |

## 📡 **Network Management**

| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `getNetworkStatus` | `callbackContext` | Get overall network connectivity status | `JSONObject` |
| `getWifiStatus` | `callbackContext` | Get WiFi connection status and info | `JSONObject` |
| `getMobileStatus` | `callbackContext` | Get mobile data status and info | `JSONObject` |

## 📱 **Bluetooth Peer-to-Peer Networking**

### **Status & Device Management**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `getBluetoothStatus` | `callbackContext` | Check Bluetooth adapter status | `JSONObject` |
| `enableBluetooth` | `callbackContext` | Enable Bluetooth adapter | `JSONObject` |
| `getPairedDevices` | `callbackContext` | Get list of paired Bluetooth devices | `JSONArray` |

### **Discovery & Connection**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `startBluetoothDiscovery` | `callbackContext` | Start discovering nearby devices | `JSONObject` |
| `stopBluetoothDiscovery` | `callbackContext` | Stop device discovery | `JSONObject` |
| `connectToBluetoothDevice` | `args, callbackContext` | Connect to specific device by address | `JSONObject` |
| `startBluetoothServer` | `callbackContext` | Start Bluetooth server socket | `JSONObject` |
| `stopBluetoothServer` | `callbackContext` | Stop Bluetooth server | `JSONObject` |

### **Messaging & Communication**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `sendMessage` | `args, callbackContext` | Send message to specific device | `JSONObject` |
| `broadcastMessage` | `args, callbackContext` | Broadcast message to all connected devices | `JSONObject` |
| `getConnectedDevices` | `callbackContext` | Get list of currently connected devices | `JSONArray` |

### **Connection Management**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `disconnectDevice` | `args, callbackContext` | Disconnect specific device | `JSONObject` |
| `disconnectAllDevices` | `callbackContext` | Disconnect all connected devices | `JSONObject` |

### **Event Listeners**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `startDiscoveryListener` | `callbackContext` | Start listening for device discovery events | `void` |
| `startConnectionListener` | `callbackContext` | Start listening for connection events | `void` |
| `startMessageListener` | `callbackContext` | Start listening for incoming messages | `void` |

## 🌐 **Bluetooth Mesh Networking**

| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `initializeMesh` | `callbackContext` | Initialize mesh networking capabilities | `JSONObject` |
| `joinMesh` | `args, callbackContext` | Join existing mesh network | `JSONObject` |
| `sendMeshMessage` | `args, callbackContext` | Send message to specific mesh node | `JSONObject` |
| `broadcastToMesh` | `args, callbackContext` | Broadcast message to entire mesh | `JSONObject` |
| `getMeshTopology` | `callbackContext` | Get current mesh network topology | `JSONObject` |
| `startMeshEventListener` | `callbackContext` | Start mesh network event listener | `void` |
| `discoverMeshNodes` | `callbackContext` | Discover available mesh nodes | `JSONArray` |
| `autoJoinMesh` | `callbackContext` | Automatically join best available mesh | `JSONObject` |
| `getAvailableGateways` | `callbackContext` | Get available mesh gateway nodes | `JSONArray` |

## 📊 **Device Management & Monitoring**

### **Basic Device Info**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `getDeviceInfo` | `callbackContext` | Get comprehensive device information | `JSONObject` |
| `getBatteryStatus` | `callbackContext` | Get current battery status and level | `JSONObject` |
| `startBatteryListener` | `callbackContext` | Start battery level change listener | `void` |
| `stopBatteryListener` | `callbackContext` | Stop battery level listener | `void` |

### **System Monitoring (Phase 1)**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `getSystemResources` | `callbackContext` | Get memory, CPU, storage, thermal status | `JSONObject` |
| `getAvailableSensors` | `callbackContext` | Get list of all available sensors | `JSONArray` |
| `getSensorCapabilities` | `callbackContext` | Get detailed sensor capabilities | `JSONObject` |
| `runDeviceDiagnostics` | `callbackContext` | Run comprehensive device health check | `JSONObject` |

## 🎵 **Media Management**

### **Audio Playback Control**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `createAudio` | `args, callbackContext` | Create audio player instance | `JSONObject` |
| `playAudio` | `args, callbackContext` | Start audio playback | `JSONObject` |
| `pauseAudio` | `args, callbackContext` | Pause audio playback | `JSONObject` |
| `stopAudio` | `args, callbackContext` | Stop audio playback | `JSONObject` |
| `seekAudio` | `args, callbackContext` | Seek to position in audio | `JSONObject` |
| `getAudioDuration` | `args, callbackContext` | Get total audio duration | `JSONObject` |
| `getAudioPosition` | `args, callbackContext` | Get current playback position | `JSONObject` |
| `setAudioVolume` | `args, callbackContext` | Set audio volume level | `JSONObject` |
| `releaseAudio` | `args, callbackContext` | Release audio resources | `JSONObject` |

### **Media Capture**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `captureAudio` | `args, callbackContext` | Record audio using device microphone | `JSONObject` |
| `captureImage` | `args, callbackContext` | Capture image using camera | `JSONObject` |
| `captureVideo` | `args, callbackContext` | Record video using camera | `JSONObject` |
| `getSupportedFormats` | `args, callbackContext` | Get supported media formats | `JSONArray` |

### **Professional Camera Control**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `proCameraOpen` | `args, callbackContext` | Open professional camera interface | `JSONObject` |
| `proCameraCapture` | `args, callbackContext` | Capture with manual settings | `JSONObject` |
| `proCameraSetISO` | `args, callbackContext` | Set ISO sensitivity | `JSONObject` |
| `proCameraSetShutterSpeed` | `args, callbackContext` | Set shutter speed | `JSONObject` |
| `proCameraSetWhiteBalance` | `args, callbackContext` | Set white balance | `JSONObject` |
| `proCameraSetFocus` | `args, callbackContext` | Set focus mode | `JSONObject` |
| `proCameraSetExposureCompensation` | `args, callbackContext` | Set exposure compensation | `JSONObject` |
| `proCameraEnableRAW` | `args, callbackContext` | Enable RAW capture mode | `JSONObject` |
| `proCameraClose` | `args, callbackContext` | Close camera interface | `JSONObject` |
| `proCameraGetSettings` | `args, callbackContext` | Get current camera settings | `JSONObject` |

## 📍 **Location Services**

| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `getCurrentLocation` | `args, callbackContext` | Get current device location | `JSONObject` |
| `startLocationTracking` | `args, callbackContext` | Start continuous location tracking | `void` |
| `stopLocationTracking` | `callbackContext` | Stop location tracking | `void` |
| `getLocationPermissions` | `callbackContext` | Check location permissions status | `JSONObject` |
| `calculateDistance` | `args, callbackContext` | Calculate distance between coordinates | `JSONObject` |
| `getAvailableProviders` | `callbackContext` | Get available location providers | `JSONArray` |

## 🖥️ **Display Management**

| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `enterFullscreen` | `callbackContext` | Enter fullscreen mode | `JSONObject` |
| `exitFullscreen` | `callbackContext` | Exit fullscreen mode | `JSONObject` |
| `toggleFullscreen` | `callbackContext` | Toggle fullscreen state | `JSONObject` |
| `setScreenOrientation` | `args, callbackContext` | Set screen orientation | `JSONObject` |
| `keepScreenOn` | `args, callbackContext` | Keep screen always on | `JSONObject` |
| `getDisplayMetrics` | `callbackContext` | Get display specifications | `JSONObject` |
| `getFullscreenStatus` | `callbackContext` | Check if in fullscreen mode | `JSONObject` |
| `enterImmersiveMode` | `callbackContext` | Enter immersive fullscreen mode | `JSONObject` |

## 📱 **App Discovery & Management**

### **App Monitoring**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `startAppMonitoring` | `callbackContext` | Start monitoring app installations/removals | `void` |
| `stopAppMonitoring` | `callbackContext` | Stop app monitoring | `void` |
| `getMonitoringStatus` | `callbackContext` | Get app monitoring status | `JSONObject` |

### **App Discovery**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `getInstalledApps` | `callbackContext` | Get user-installed apps only | `JSONArray` |
| `getAllInstalledApps` | `callbackContext` | Get all apps (user + system) | `JSONArray` |
| `getSystemApps` | `callbackContext` | Get system apps only | `JSONArray` |
| `getUserApps` | `callbackContext` | Get user apps only | `JSONArray` |
| `getAppInfo` | `args, callbackContext` | Get detailed info for specific app | `JSONObject` |
| `isAppInstalled` | `args, callbackContext` | Check if app is installed | `boolean` |

### **App Management**
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `uninstallApp` | `args, callbackContext` | Uninstall specified app | `JSONObject` |
| `launchApp` | `args, callbackContext` | Launch specified app | `JSONObject` |
| `openAppInPlayStore` | `args, callbackContext` | Open app in Play Store | `JSONObject` |

### WiFi Mesh
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `initializeWiFiMesh` | None | Initialize WiFi Direct mesh | JSONObject |
| `createWiFiMeshGroup` | None | Create WiFi mesh group | JSONObject |
| `discoverWiFiPeers` | None | Discover WiFi Direct peers | JSONObject |
| `connectToWiFiDevice` | `deviceInfo` | Connect to WiFi device | JSONObject |
| `getWiFiPeers` | None | Get discovered WiFi peers | JSONObject |
| `sendWiFiMeshMessage` | `message`, `target` | Send WiFi mesh message | JSONObject |
| `getWiFiMeshTopology` | None | Get WiFi mesh topology | JSONObject |
| `removeWiFiMeshGroup` | None | Remove WiFi mesh group | JSONObject |

### Local Network Mesh
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `initializeLocalMesh` | None | Initialize local network mesh | JSONObject |
| `discoverLocalPeers` | None | Discover peers on local network | JSONObject |
| `getLocalPeers` | None | Get discovered local peers | JSONObject |
| `autoConnectLocalPeers` | None | Auto-connect to local peers | JSONObject |
| `sendLocalMeshMessage` | `message`, `target` | Send local mesh message | JSONObject |
| `getLocalMeshTopology` | None | Get local mesh topology | JSONObject |
| `stopLocalMesh` | None | Stop local mesh networking | JSONObject |

### Auto Mesh
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `startAutoMesh` | None | Start automatic mesh networking | JSONObject |
| `stopAutoMesh` | None | Stop automatic mesh | JSONObject |
| `broadcastToAutoMesh` | `message` | Broadcast to auto mesh network | JSONObject |
| `getAutoMeshStatus` | None | Get auto mesh status | JSONObject |

### Hybrid Mesh
| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `startHybridMesh` | None | Start hybrid mesh (multiple technologies) | JSONObject |
| `sendHybridMessage` | `message`, `options` | Send hybrid mesh message | JSONObject |
| `getHybridStatus` | None | Get hybrid mesh status | JSONObject |
| `stopHybridMesh` | None | Stop hybrid mesh networking | JSONObject |



## 🔧 **Internal/Helper Methods**

| Method | Parameters | Description | Return Type |
|--------|------------|-------------|-------------|
| `executeActionWithPermissions` | `action, args, callbackContext` | Internal: Execute action after permission grant | `void` |
| `sendPermissionError` | `callbackContext, reason` | Internal: Send permission error response | `void` |
| `sendEventToJavaScript` | `eventName, data` | Internal: Send events to JavaScript | `void` |
| `onDestroy` | - | Cleanup resources on plugin destroy | `void` |
| `onReset` | - | Reset plugin state on WebView navigation | `void` |
| `onActivityResult` | `requestCode, resultCode, intent` | Handle activity results | `void` |
| `onRequestPermissionResult` | `requestCode, permissions, grantResults` | Handle permission results | `void` |

---

## 📋 **Summary Statistics**
- **Total Methods**: 108 methods
- **Categories**: 12 main functional categories
- **Permission-protected**: 8 methods require runtime permissions
- **Event Listeners**: 7 methods for real-time events
- **Hardware Access**: Camera, Sensors, Bluetooth, Location, Audio, Display

This comprehensive plugin provides extensive access to Android system capabilities through a unified Cordova/PhoneGap interface.
