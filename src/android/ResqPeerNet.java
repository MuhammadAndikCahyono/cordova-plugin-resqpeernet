package com.muhammadandikcahyono.resqpeernet;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.muhammadandikcahyono.resqpeernet.modules.WallpaperManagerBridge;
import com.muhammadandikcahyono.resqpeernet.modules.PermissionManager;
import com.muhammadandikcahyono.resqpeernet.modules.NetworkManagerBridge;
import com.muhammadandikcahyono.resqpeernet.modules.BluetoothMeshManager;
import com.muhammadandikcahyono.resqpeernet.modules.AutoLocalMeshManager;
import com.muhammadandikcahyono.resqpeernet.modules.DeviceManagerBridge;
import com.muhammadandikcahyono.resqpeernet.modules.MediaManagerBridge;
import com.muhammadandikcahyono.resqpeernet.modules.LocationManagerBridge;
import com.muhammadandikcahyono.resqpeernet.modules.DisplayManagerBridge;
import com.muhammadandikcahyono.resqpeernet.modules.AppDiscoveryManagerBridge;
import com.muhammadandikcahyono.resqpeernet.modules.FileManagerBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;


public class ResqPeerNet extends CordovaPlugin {

    private static final String TAG = "ResqPeerNet";
    private boolean isInitialized = false;
    private WallpaperManagerBridge wallpaperBridge;
    private PermissionManager permissionManager;
    private NetworkManagerBridge networkBridge;
	private BluetoothMeshManager bluetoothMeshManager;
	private AutoLocalMeshManager autoLocalMeshManager;
    private DeviceManagerBridge deviceManagerBridge;
    private MediaManagerBridge mediaManagerBridge;
    private LocationManagerBridge locationManagerBridge;
	private DisplayManagerBridge displayManagerBridge;
	private AppDiscoveryManagerBridge appDiscoveryBridge;
    private FileManagerBridge fileManagerBridge;
	
	public class PluginConstants {
		public static final String PLUGIN_NAME = "ResqPeerNet";
		public static final String PLUGIN_VERSION = "1.0.0";
	}
	
    /**
     * MediaManagerBridge callback implementation
     */
	private final MediaManagerBridge.MediaManagerCallback mediaCallback = new MediaManagerBridge.MediaManagerCallback() {
		@Override
		public void sendEvent(String eventName, JSONObject data) {
			Log.d(TAG, "Media event: " + eventName);
			sendEventToJavaScript(eventName, data);
		}
		
		@Override
		public void onMediaResult(String action, JSONObject result) {
			Log.d(TAG, "Media action completed: " + action);
		}
		
		@Override
		public void onMediaError(String action, String error) {
			Log.e(TAG, "Media action failed: " + action + " - " + error);
		}
		
		@Override
		public void startActivityForResult(Intent intent, int requestCode) {
			cordova.startActivityForResult(ResqPeerNet.this, intent, requestCode);
		}
		
		@Override
		public void requestPermission(int requestCode, String permission) {
			cordova.requestPermission(ResqPeerNet.this, requestCode, permission);
		}
		
		@Override
		public void requestPermissions(int requestCode, String[] permissions) {
			cordova.requestPermissions(ResqPeerNet.this, requestCode, permissions);
		}
		
		@Override
		public boolean hasPermission(String permission) {
			return cordova.hasPermission(permission);
		}
		
		@Override
		public Context getContext() {
			return cordova.getContext();
		}
	};

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        
        Log.i(TAG, "ResqPeerNet initialization started");
        
        try {
            // Initialize Permission Manager first
            permissionManager = new PermissionManager(cordova, new PermissionManager.PermissionCallback() {
                @Override
                public void onPermissionsGranted(String action, JSONObject args, CallbackContext callbackContext) {
                    Log.i(TAG, "Permissions granted for action: " + action);
                    executeActionWithPermissions(action, args, callbackContext);
                }
                
                @Override
                public void onPermissionsDenied(String action, JSONObject args, CallbackContext callbackContext, String reason) {
                    Log.w(TAG, "Permissions denied for action: " + action + ", reason: " + reason);
                    sendPermissionError(callbackContext, reason);
                }
                
                @Override
                public void requestPermissions(int requestId, String[] permissions) {
                    cordova.requestPermissions(ResqPeerNet.this, requestId, permissions);
                }
            });
			
			
			// Initialize Bluetooth Mesh Manager
			bluetoothMeshManager = new BluetoothMeshManager(cordova.getContext());
			Log.i(TAG, "BluetoothMeshManager initialized successfully");
			
			// Initialize Local Mesh Manager
			autoLocalMeshManager = new AutoLocalMeshManager(cordova.getContext());
			Log.i(TAG, "AutoLocalMeshManager initialized successfully");

            // Initialize Wallpaper Manager Bridge
            wallpaperBridge = new WallpaperManagerBridge(
                cordova.getContext(), 
                new WallpaperManagerBridge.CallbackSender() {
                    @Override
                    public void sendEvent(String eventName, JSONObject data) {
                        Log.d(TAG, "Wallpaper event: " + eventName);
                        sendEventToJavaScript(eventName, data);
                    }
                }
            );
            
            networkBridge = new NetworkManagerBridge(cordova.getContext());
            Log.i(TAG, "NetworkManagerBridge initialized successfully");
  
            // Initialize Media Manager Bridge
            mediaManagerBridge = new MediaManagerBridge(cordova, webView, mediaCallback);
            Log.i(TAG, "MediaManagerBridge initialized successfully");

            deviceManagerBridge = new DeviceManagerBridge(cordova.getContext());
            Log.i(TAG, "DeviceManagerBridge initialized successfully");
			
			locationManagerBridge = new LocationManagerBridge(cordova.getContext(), cordova);
            Log.i(TAG, "LocationManagerBridge initialized successfully");
			
			locationManagerBridge.setLocationCallback(new LocationManagerBridge.LocationCallback() {
				@Override
				public void onLocationUpdate(JSONObject locationData) {
					Log.d(TAG, "Location update received in main plugin");
					sendEventToJavaScript("location_update", locationData);
				}
				
				@Override
				public void onLocationError(String error) {
					Log.d(TAG, "Location error received in main plugin: " + error);
					try {
						JSONObject errorData = new JSONObject();
						errorData.put("error", error);
						sendEventToJavaScript("location_error", errorData);
					} catch (JSONException e) {
						Log.e(TAG, "Error creating error JSON", e);
					}
				}
				
				@Override
				public void onProviderStatusChanged(String provider, boolean enabled) {
					Log.d(TAG, "Provider status changed: " + provider + " = " + enabled);
					try {
						JSONObject statusData = new JSONObject();
						statusData.put("provider", provider);
						statusData.put("enabled", enabled);
						sendEventToJavaScript("provider_status", statusData);
					} catch (JSONException e) {
						Log.e(TAG, "Error creating status JSON", e);
					}
				}
			});
			
            displayManagerBridge = new DisplayManagerBridge(
				cordova.getContext(), cordova.getActivity()
			);
            Log.i(TAG, "DisplayManagerBridge initialized successfully");
			
			// Initialize App Discovery Manager Bridge
			appDiscoveryBridge = new AppDiscoveryManagerBridge(
				cordova.getActivity().getPackageManager(), 
				cordova
			);
			Log.i(TAG, "AppDiscoveryManagerBridge initialized successfully");

            fileManagerBridge = new FileManagerBridge(
                cordova.getContext(),
                new FileManagerBridge.CallbackSender() {
                    @Override
                    public void sendEvent(String eventName, JSONObject data) {
                        Log.d(TAG, "File system event: " + eventName);
                        sendEventToJavaScript(eventName, data);
                    }
                }
            );
            Log.i(TAG, "FileManagerBridge initialized successfully");

            isInitialized = true;
            Log.i(TAG, "ResqPeerNet initialized successfully with all modules");
            
        } catch (Exception e) {
            Log.e(TAG, "ResqPeerNet initialization failed", e);
            isInitialized = false;
        }
    }
	
	
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "Executing action: " + action);

        if (!isInitialized) {
            Log.e(TAG, "Plugin not initialized");
            callbackContext.error("Plugin not initialized");
            return false;
        }

        try {
            JSONObject arguments = args.length() > 0 ? args.getJSONObject(0) : new JSONObject();
            
            switch (action) {
			
				case "init":
					initialize(callbackContext);
					return true;

                case "getPluginInfo":
                    getPluginInfo(callbackContext);
                    return true;
                    
                case "checkStatus":
                    checkStatus(callbackContext);
                    return true;
                    
                case "testConnection":
                    testConnection(callbackContext);
                    return true;
                    
                case "getSystemInfo":
                    getSystemInfo(callbackContext);
                    return true;

                case "getPermissionStatus":
                    getPermissionStatus(callbackContext);
                    return true;
                    
                case "requestPermission":
                    requestPermission(arguments, callbackContext);
                    return true;
					
				 case "requestPermissions":
                    requestPermissions(arguments, callbackContext);
                    return true;
                    
                case "hasStoragePermission":
                    hasStoragePermission(callbackContext);
                    return true;
                    
                case "hasNetworkPermissions":
                    hasNetworkPermissions(callbackContext);
                    return true;
				/**
				* Wallpape Manager
				**/
                case "getWallpaper":
                    getWallpaper(arguments, callbackContext);
                    return true;
                    
                case "getWallpaperInfo":
                    getWallpaperInfo(arguments, callbackContext);
                    return true;
					
				case "setWallpaperFromFile":
                    setWallpaperFromFile(arguments, callbackContext);
                    return true;
					
				case "setWallpaperFromUri":
                    setWallpaperFromUri(arguments, callbackContext);
                    return true;
                    
                case "listenWallpaperChanged":
                    listenWallpaperChanged(arguments, callbackContext);
                    return true;
                    
                case "stopListeningWallpaper":
                    stopListeningWallpaper(callbackContext);
                    return true;
                    
                case "getNetworkStatus":
                    getNetworkStatus(callbackContext);
                    return true;
                    
                case "getWifiStatus":
                    getWifiStatus(callbackContext);
                    return true;
                    
                case "getMobileStatus":
                    getMobileStatus(callbackContext);
                    return true;

                case "getDeviceInfo":
					deviceManagerBridge.getDeviceInfo(callbackContext);
                    return true;
                    
                case "getBatteryStatus":
					deviceManagerBridge.getBatteryStatus(callbackContext);
                    return true;
                    
                case "startBatteryListener":
					deviceManagerBridge.startBatteryListener(callbackContext);
                    return true;
                    
                case "stopBatteryListener":
					deviceManagerBridge.stopBatteryListener(callbackContext);
                    return true;

                // =========================================================================
                // MEDIA METHODS - NEW
                // =========================================================================
                case "createAudio":
                    createAudio(arguments, callbackContext);
                    return true;
                    
                case "playAudio":
                    playAudio(arguments, callbackContext);
                    return true;
                    
                case "pauseAudio":
                    pauseAudio(arguments, callbackContext);
                    return true;
                    
                case "stopAudio":
                    stopAudio(arguments, callbackContext);
                    return true;
                    
                case "seekAudio":
                    seekAudio(arguments, callbackContext);
                    return true;
                    
                case "getAudioDuration":
                    getAudioDuration(arguments, callbackContext);
                    return true;
                    
                case "getAudioPosition":
                    getAudioPosition(arguments, callbackContext);
                    return true;
                    
                case "setAudioVolume":
                    setAudioVolume(arguments, callbackContext);
                    return true;
                    
                case "releaseAudio":
                    releaseAudio(arguments, callbackContext);
                    return true;
                    
                case "captureAudio":
                    captureAudio(arguments, callbackContext);
                    return true;
                    
                case "captureImage":
                    captureImage(arguments, callbackContext);
                    return true;
                    
                case "captureVideo":
                    captureVideo(arguments, callbackContext);
                    return true;
                    
                case "getSupportedFormats":
                    getSupportedFormats(arguments, callbackContext);
                    return true;
					
				case "proCameraOpen":
					proCameraOpen(arguments, callbackContext);
					return true;
					
				case "proCameraCapture":
					proCameraCapture(arguments, callbackContext);
					return true;
					
				case "proCameraSetISO":
					proCameraSetISO(arguments, callbackContext);
					return true;
					
				case "proCameraSetShutterSpeed":
					proCameraSetShutterSpeed(arguments, callbackContext);
					return true;
					
				case "proCameraSetWhiteBalance":
					proCameraSetWhiteBalance(arguments, callbackContext);
					return true;
					
				case "proCameraSetFocus":
					proCameraSetFocus(arguments, callbackContext);
					return true;
					
				case "proCameraSetExposureCompensation":
					proCameraSetExposureCompensation(arguments, callbackContext);
					return true;
					
				case "proCameraEnableRAW":
					proCameraEnableRAW(arguments, callbackContext);
					return true;
					
				case "proCameraClose":
					proCameraClose(arguments, callbackContext);
					return true;
					
				case "proCameraGetSettings":
					proCameraGetSettings(arguments, callbackContext);
					return true;

                case "getCurrentLocation":
                    getCurrentLocation(arguments, callbackContext);
                    return true;
                    
                case "startLocationTracking":
                    startLocationTracking(arguments, callbackContext);
                    return true;
                    
                case "stopLocationTracking":
                    stopLocationTracking(callbackContext);
                    return true;
                    
                case "getLocationPermissions":
                    getLocationPermissions(callbackContext);
                    return true;
                    
                case "calculateDistance":
                    calculateDistance(arguments, callbackContext);
                    return true;
                    
                case "getAvailableProviders":
                    getAvailableProviders(callbackContext);
                    return true;
                
                // =========================================================================
                // PHASE 1: SYSTEM MONITORING METHODS - NEWLY ADDED
                // =========================================================================
                    
                case "getSystemResources":
					deviceManagerBridge.getSystemResources(callbackContext);
                    return true;
                    
                case "getAvailableSensors":
					deviceManagerBridge.getAvailableSensors(callbackContext);
                    return true;
                    
                case "getSensorCapabilities":
					deviceManagerBridge.getSensorCapabilities(callbackContext);
                    return true;
                    
                case "runDeviceDiagnostics":
					deviceManagerBridge.runDeviceDiagnostics(callbackContext);
                    return true;
					
				// =========================================================================
				// DISPLAY MANAGEMENT METHODS - NEW
				// =========================================================================
				case "enterFullscreen":
					enterFullscreen(callbackContext);
					return true;
				
				case "enterPermanentFullscreen":
					enterPermanentFullscreen(callbackContext);
					return true;
					
				case "exitFullscreen":
					exitFullscreen(callbackContext);
					return true;
					
				case "toggleFullscreen":
					toggleFullscreen(callbackContext);
					return true;
					
				case "setScreenOrientation":
					setScreenOrientation(arguments, callbackContext);
					return true;
					
				case "keepScreenOn":
					keepScreenOn(arguments, callbackContext);
					return true;
					
				case "getDisplayMetrics":
					getDisplayMetrics(callbackContext);
					return true;
					
				case "getFullscreenStatus":
					getFullscreenStatus(callbackContext);
					return true;
					
				case "enterImmersiveMode":
					enterImmersiveMode(callbackContext);
					return true;
					
				case "setScreenshotAllowed":
					setScreenshotAllowed(arguments, callbackContext);
					return true;
					
				case "getScreenshotStatus":
					getScreenshotStatus(callbackContext);
					return true;
					
				case "enableContentProtection":
					enableContentProtection(arguments, callbackContext);
					return true;

				// =========================================================================
				// App Manager Brifge METHODS - NEW
				// =========================================================================

                case "startAppMonitoring":
                    appDiscoveryBridge.startAppMonitoring(callbackContext);
                    return true;

                case "stopAppMonitoring":
                    appDiscoveryBridge.stopAppMonitoring(callbackContext);
                    return true;

                 case "getMonitoringStatus":
                    appDiscoveryBridge.getMonitoringStatus(callbackContext);
                    return true;
					
				case "getInstalledApps":
					getInstalledApps(callbackContext);
					return true;

                case "getAllInstalledApps":
                    getAllInstalledApps(callbackContext);
                    return true;
                    
                case "getSystemApps":
                    getSystemApps(callbackContext);
                    return true;
                    
                case "getUserApps":
                    getUserApps(callbackContext);
                    return true;
					
				case "getAppInfo":
					getAppInfo(arguments, callbackContext);
					return true;
					
				case "uninstallApp":
					uninstallApp(arguments, callbackContext);
					return true;
					
				case "launchApp":
					launchApp(arguments, callbackContext);
					return true;
					
				case "openAppInPlayStore":
					openAppInPlayStore(arguments, callbackContext);
					return true;
					
				case "isAppInstalled":
					isAppInstalled(arguments, callbackContext);
					return true;
				// =========================================================================
				// FILE MANAGER METHODS
				// =========================================================================
                case "readFileAsText":
                    readFileAsText(args, callbackContext);
                    return true;
                case "writeFile":
                    writeFile(args, callbackContext);
                    return true;
                case "createDirectory":
                    createDirectory(args, callbackContext);
                    return true;
                case "listDirectory":
                    listDirectory(args, callbackContext);
                    return true;
                case "deleteFile":
                    deleteFile(args, callbackContext);
                    return true;
                case "deleteDirectory":
                    deleteDirectory(args, callbackContext);
                    return true;
                case "getStorageInfo":
                    getStorageInfo(callbackContext);
                    return true;
                case "searchFiles":
                    searchFiles(args, callbackContext);
                    return true;
                case "getFileInfo":
                    getFileInfo(args, callbackContext);
                    return true;
					
				// =========================================================================
				// BLUETOOTH MESH METHODS
				// =========================================================================
				case "BTMeshStartNetwork":
					BTMeshStartNetwork(callbackContext);
					return true;
					
				case "BTMeshStopNetwork":
					BTMeshStopNetwork(callbackContext);
					return true;
					
				case "BTMeshGetStatus":
					BTMeshGetStatus(callbackContext);
					return true;
					
				case "BTMeshStartAdvertising":
					BTMeshStartAdvertising(callbackContext);
					return true;
					
				case "BTMeshStartEnhancedAdvertising":
					BTMeshStartEnhancedAdvertising(callbackContext);
					return true;
					
				case "BTMeshStopAdvertising":
					BTMeshStopAdvertising(callbackContext);
					return true;
					
				case "BTMeshStartScanning":
					BTMeshStartScanning(callbackContext);
					return true;
					
				case "BTMeshStopScanning":
					BTMeshStopScanning(callbackContext);
					return true;
					
				case "BTMeshSendMessage":
					BTMeshSendMessage(arguments, callbackContext);
					return true;
					
				case "BTMeshBroadcastMessage":
					BTMeshBroadcastMessage(arguments, callbackContext);
					return true;
					
				case "BTMeshSendReliableMessage":
					BTMeshSendReliableMessage(arguments, callbackContext);
					return true;
					
				case "BTMeshSendAcknowledgedMessage":
					BTMeshSendAcknowledgedMessage(arguments, callbackContext);
					return true;
					
				case "BTMeshConnectDevice":
					BTMeshConnectDevice(arguments, callbackContext);
					return true;
					
				case "BTMeshDisconnectDevice":
					BTMeshDisconnectDevice(arguments, callbackContext);
					return true;
					
				case "BTMeshGetConnectedDevices":
					BTMeshGetConnectedDevices(callbackContext);
					return true;
					
				case "BTMeshIsBluetoothEnabled":
					BTMeshIsBluetoothEnabled(callbackContext);
					return true;
					
				case "BTMeshEnableBluetooth":
					BTMeshEnableBluetooth(callbackContext);
					return true;
					
				case "BTMeshGetBondedDevices":
					BTMeshGetBondedDevices(callbackContext);
					return true;
					
				case "BTMeshStartDiscovery":
					BTMeshStartDiscovery(callbackContext);
					return true;
					
				case "BTMeshCheckPermissions":
					BTMeshCheckPermissions(callbackContext);
					return true;
					
				case "BTMeshSetConfiguration":
					BTMeshSetConfiguration(arguments, callbackContext);
					return true;
					
				case "BTMeshGetConfiguration":
					BTMeshGetConfiguration(callbackContext);
					return true;
					
				case "BTMeshGetBLEStatus":
					BTMeshGetBLEStatus(callbackContext);
					return true;
					
				case "BTMeshGetPerformanceStats":
					BTMeshGetPerformanceStats(callbackContext);
					return true;
					
				case "BTMeshGetErrorLog":
					BTMeshGetErrorLog(callbackContext);
					return true;
					
				case "BTMeshResetNetwork":
					BTMeshResetNetwork(callbackContext);
					return true;
					
				// =========================================================================
				// BLUETOOTH MESH EVENT LISTENERS
				// =========================================================================
				case "BTMeshStartDiscoveryListener":
					BTMeshStartDiscoveryListener(callbackContext);
					return true;
					
				case "BTMeshStartConnectionListener":
					BTMeshStartConnectionListener(callbackContext);
					return true;
					
				case "BTMeshStartMessageListener":
					BTMeshStartMessageListener(callbackContext);
					return true;
					
				case "BTMeshStartRoutingListener":
					BTMeshStartRoutingListener(callbackContext);
					return true;
					
				case "BTMeshStartAdvertisingListener":
					BTMeshStartAdvertisingListener(callbackContext);
					return true;
					
				// =========================================================================
				// LOCAL MESH METHODS (WI-FI)
				// =========================================================================
				
				case "initializeMesh":
					initializeMesh(callbackContext);
					return true;
					
				case "discoverPeers":
					discoverPeers(callbackContext);
					return true;
					
				case "getDiscoveredPeers":
					getDiscoveredPeers(callbackContext);
					return true;
					
				case "connectToPeer":
					connectToPeer(arguments, callbackContext);
					return true;
					
				case "autoConnectPeers":
					autoConnectPeers(callbackContext);
					return true;
					
				case "sendMeshMessage":
					sendMeshMessage(arguments, callbackContext);
					return true;
					
				case "getMeshTopology":
					getMeshTopology(callbackContext);
					return true;
					
				case "stopMesh":
					stopMesh(callbackContext);
					return true;

                default:
                    Log.w(TAG, "Unknown action: " + action);
                    callbackContext.error("Unknown action: " + action);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing action: " + action, e);
            callbackContext.error("Error: " + e.getMessage());
            return false;
        }
    }
	
	private void initialize(CallbackContext callbackContext) {
		try {
			JSONObject info = new JSONObject();

			//Ambil nama & versi langsung dari Android manifest / build
			Context ctx = cordova.getContext();
			String packageName = ctx.getPackageName();
			String versionName = "unknown";
			int versionCode = -1;

			try {
				PackageManager pm = ctx.getPackageManager();
				PackageInfo pInfo = pm.getPackageInfo(packageName, 0);
				versionName = pInfo.versionName;
				versionCode = pInfo.versionCode;
			} catch (Exception e) {
				Log.w(TAG, "Failed to read package info", e);
			}

			//Ambil status dinamis berdasarkan modul yang aktif
			boolean active = isInitialized
					&& permissionManager != null
					&& (mediaManagerBridge != null || wallpaperBridge != null);

			//Kumpulkan semua informasi
			info.put("name", packageName);
			//info.put("pluginName", PluginConstants.PLUGIN_NAME);
			//info.put("version", PluginConstants.PLUGIN_VERSION);
			info.put("versionCode", versionCode);
			info.put("status", active ? "active" : "inactive");
			info.put("initialized", isInitialized);

			//Modul aktif
			info.put("modules", new JSONObject()
					.put("wallpaper", wallpaperBridge != null)
					.put("permission", permissionManager != null)
					.put("media", mediaManagerBridge != null)
					.put("diagnostics", true)
					.put("eventBus", true)
					.put("logger", true)
			);

			//Waktu & sistem
			info.put("timestamp", System.currentTimeMillis());
			info.put("device", android.os.Build.MODEL);
			info.put("androidVersion", android.os.Build.VERSION.RELEASE);

			callbackContext.success(info);
			Log.d(TAG, "Plugin info sent successfully: " + info.toString());

		} catch (Exception e) {
			Log.e(TAG, "Error getting plugin info", e);
			callbackContext.error("Error getting plugin info: " + e.getMessage());
		}
	}

    
	// =========================================================================
	// BLUETOOTH MESH MANAGEMENT METHODS - WITH BTMesh PREFIX
	// =========================================================================

	private void BTMeshStartNetwork(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.startMeshNetwork(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error starting mesh network", e);
			callbackContext.error("Error starting mesh network: " + e.getMessage());
		}
	}

	private void BTMeshStopNetwork(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.stopMeshNetwork(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error stopping mesh network", e);
			callbackContext.error("Error stopping mesh network: " + e.getMessage());
		}
	}

	private void BTMeshGetStatus(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.getMeshStatus(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error getting mesh status", e);
			callbackContext.error("Error getting mesh status: " + e.getMessage());
		}
	}

	// =========================================================================
	// BLE ADVERTISING METHODS
	// =========================================================================

	private void BTMeshStartAdvertising(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.startBLEAdvertising(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error starting BLE advertising", e);
			callbackContext.error("Error starting BLE advertising: " + e.getMessage());
		}
	}

	private void BTMeshStartEnhancedAdvertising(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.startEnhancedBLEAdvertising(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error starting enhanced BLE advertising", e);
			callbackContext.error("Error starting enhanced BLE advertising: " + e.getMessage());
		}
	}

	private void BTMeshStopAdvertising(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.stopBLEAdvertising(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error stopping BLE advertising", e);
			callbackContext.error("Error stopping BLE advertising: " + e.getMessage());
		}
	}

	// =========================================================================
	// BLE SCANNING METHODS
	// =========================================================================

	private void BTMeshStartScanning(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.startBLEScanning(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error starting BLE scanning", e);
			callbackContext.error("Error starting BLE scanning: " + e.getMessage());
		}
	}

	private void BTMeshStopScanning(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.stopBLEScanning(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error stopping BLE scanning", e);
			callbackContext.error("Error stopping BLE scanning: " + e.getMessage());
		}
	}

	// =========================================================================
	// MESH MESSAGING METHODS
	// =========================================================================

	private void BTMeshSendMessage(JSONObject args, CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			String destinationNodeId = args.getString("destinationNodeId");
			String message = args.getString("message");
			bluetoothMeshManager.sendMeshMessage(destinationNodeId, message, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error sending mesh message", e);
			callbackContext.error("Error sending mesh message: " + e.getMessage());
		}
	}

	private void BTMeshBroadcastMessage(JSONObject args, CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			String message = args.getString("message");
			bluetoothMeshManager.broadcastMeshMessage(message, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error broadcasting mesh message", e);
			callbackContext.error("Error broadcasting mesh message: " + e.getMessage());
		}
	}

	private void BTMeshSendReliableMessage(JSONObject args, CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			String destinationNodeId = args.getString("destinationNodeId");
			String message = args.getString("message");
			int maxRetries = args.optInt("maxRetries", 3);
			bluetoothMeshManager.sendReliableMessage(destinationNodeId, message, maxRetries, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error sending reliable message", e);
			callbackContext.error("Error sending reliable message: " + e.getMessage());
		}
	}

	private void BTMeshSendAcknowledgedMessage(JSONObject args, CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			String destinationNodeId = args.getString("destinationNodeId");
			String message = args.getString("message");
			int timeoutMs = args.optInt("timeoutMs", 10000);
			bluetoothMeshManager.sendAcknowledgedMessage(destinationNodeId, message, timeoutMs, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error sending acknowledged message", e);
			callbackContext.error("Error sending acknowledged message: " + e.getMessage());
		}
	}

	// =========================================================================
	// DEVICE CONNECTION METHODS
	// =========================================================================

	private void BTMeshConnectDevice(JSONObject args, CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			String deviceAddress = args.getString("deviceAddress");
			bluetoothMeshManager.connectToBLEDevice(deviceAddress, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error connecting to BLE device", e);
			callbackContext.error("Error connecting to BLE device: " + e.getMessage());
		}
	}

	private void BTMeshDisconnectDevice(JSONObject args, CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			String deviceAddress = args.getString("deviceAddress");
			bluetoothMeshManager.disconnectBLEDevice(deviceAddress, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error disconnecting BLE device", e);
			callbackContext.error("Error disconnecting BLE device: " + e.getMessage());
		}
	}

	private void BTMeshGetConnectedDevices(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.getConnectedBLEDevices(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error getting connected BLE devices", e);
			callbackContext.error("Error getting connected BLE devices: " + e.getMessage());
		}
	}

	// =========================================================================
	// BLUETOOTH MANAGEMENT METHODS
	// =========================================================================

	private void BTMeshIsBluetoothEnabled(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.isBluetoothEnabled(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error checking Bluetooth state", e);
			callbackContext.error("Error checking Bluetooth state: " + e.getMessage());
		}
	}

	private void BTMeshEnableBluetooth(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.enableBluetooth(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error enabling Bluetooth", e);
			callbackContext.error("Error enabling Bluetooth: " + e.getMessage());
		}
	}

	private void BTMeshGetBondedDevices(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.getBondedDevices(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error getting bonded devices", e);
			callbackContext.error("Error getting bonded devices: " + e.getMessage());
		}
	}

	private void BTMeshStartDiscovery(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.startDiscovery(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error starting discovery", e);
			callbackContext.error("Error starting discovery: " + e.getMessage());
		}
	}

	private void BTMeshCheckPermissions(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.checkPermissions(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error checking Bluetooth permissions", e);
			callbackContext.error("Error checking Bluetooth permissions: " + e.getMessage());
		}
	}

	// =========================================================================
	// CONFIGURATION AND STATUS METHODS
	// =========================================================================

	private void BTMeshSetConfiguration(JSONObject args, CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.setMeshConfiguration(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error setting mesh configuration", e);
			callbackContext.error("Error setting mesh configuration: " + e.getMessage());
		}
	}

	private void BTMeshGetConfiguration(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.getMeshConfiguration(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error getting mesh configuration", e);
			callbackContext.error("Error getting mesh configuration: " + e.getMessage());
		}
	}

	private void BTMeshGetBLEStatus(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.getBLEStatus(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error getting BLE status", e);
			callbackContext.error("Error getting BLE status: " + e.getMessage());
		}
	}

	private void BTMeshGetPerformanceStats(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.getPerformanceStats(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error getting performance stats", e);
			callbackContext.error("Error getting performance stats: " + e.getMessage());
		}
	}

	private void BTMeshGetErrorLog(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.getErrorLog(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error getting error log", e);
			callbackContext.error("Error getting error log: " + e.getMessage());
		}
	}

	private void BTMeshResetNetwork(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.resetMeshNetwork(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error resetting mesh network", e);
			callbackContext.error("Error resetting mesh network: " + e.getMessage());
		}
	}

	// =========================================================================
	// EVENT LISTENER METHODS
	// =========================================================================

	private void BTMeshStartDiscoveryListener(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.startMeshDiscoveryListener(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error starting mesh discovery listener", e);
			callbackContext.error("Error starting mesh discovery listener: " + e.getMessage());
		}
	}

	private void BTMeshStartConnectionListener(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.startMeshConnectionListener(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error starting mesh connection listener", e);
			callbackContext.error("Error starting mesh connection listener: " + e.getMessage());
		}
	}

	private void BTMeshStartMessageListener(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.startMeshMessageListener(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error starting mesh message listener", e);
			callbackContext.error("Error starting mesh message listener: " + e.getMessage());
		}
	}

	private void BTMeshStartRoutingListener(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.startMeshRoutingListener(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error starting mesh routing listener", e);
			callbackContext.error("Error starting mesh routing listener: " + e.getMessage());
		}
	}

	private void BTMeshStartAdvertisingListener(CallbackContext callbackContext) {
		try {
			if (bluetoothMeshManager == null) {
				callbackContext.error("Bluetooth Mesh module not available");
				return;
			}
			bluetoothMeshManager.startBLEAdvertisingListener(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error starting BLE advertising listener", e);
			callbackContext.error("Error starting BLE advertising listener: " + e.getMessage());
		}
	}

	// =========================================================================
	// LOCAL MESH MANAGEMENT METHODS
	// =========================================================================

	private void initializeMesh(CallbackContext callbackContext) {
		try {
			autoLocalMeshManager.initializeMesh(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error initializing mesh", e);
			callbackContext.error("Error initializing mesh: " + e.getMessage());
		}
	}

	private void discoverPeers(CallbackContext callbackContext) {
		try {
			autoLocalMeshManager.discoverPeers(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error discovering peers", e);
			callbackContext.error("Error discovering peers: " + e.getMessage());
		}
	}

	private void getDiscoveredPeers(CallbackContext callbackContext) {
		try {
			autoLocalMeshManager.getDiscoveredPeers(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error getting discovered peers", e);
			callbackContext.error("Error getting discovered peers: " + e.getMessage());
		}
	}

	private void connectToPeer(JSONObject args, CallbackContext callbackContext) {
		try {
			autoLocalMeshManager.connectToPeer(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error connecting to peer", e);
			callbackContext.error("Error connecting to peer: " + e.getMessage());
		}
	}

	private void autoConnectPeers(CallbackContext callbackContext) {
		try {
			autoLocalMeshManager.autoConnectPeers(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error in auto-connect", e);
			callbackContext.error("Error in auto-connect: " + e.getMessage());
		}
	}

	private void sendMeshMessage(JSONObject args, CallbackContext callbackContext) {
		try {
			autoLocalMeshManager.sendMeshMessage(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error sending mesh message", e);
			callbackContext.error("Error sending mesh message: " + e.getMessage());
		}
	}

	private void getMeshTopology(CallbackContext callbackContext) {
		try {
			autoLocalMeshManager.getMeshTopology(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error getting mesh topology", e);
			callbackContext.error("Error getting mesh topology: " + e.getMessage());
		}
	}

	private void stopMesh(CallbackContext callbackContext) {
		try {
			autoLocalMeshManager.stopMesh(callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error stopping mesh", e);
			callbackContext.error("Error stopping mesh: " + e.getMessage());
		}
	}

	// Implementasi method handlers file manager
    private void readFileAsText(JSONArray args, CallbackContext callbackContext) {
        try {
            JSONObject arguments = args.getJSONObject(0);
            String filePath = arguments.getString("filePath");
            String encoding = arguments.optString("encoding", "UTF-8");
            fileManagerBridge.readFileAsText(filePath, encoding, callbackContext);
        } catch (Exception e) {
            callbackContext.error("Error in readFileAsText: " + e.getMessage());
        }
    }

    private void writeFile(JSONArray args, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Executing writeFile action");
            
            if (fileManagerBridge == null) {
                callbackContext.error("File Manager module not available");
                return;
            }
            
            JSONObject arguments = args.getJSONObject(0);
            String filePath = arguments.getString("filePath");
            String content = arguments.getString("content");
            boolean append = arguments.optBoolean("append", false);
            String encoding = arguments.optString("encoding", "UTF-8");
            
            // Validasi parameter required
            if (filePath == null || filePath.isEmpty()) {
                callbackContext.error("File path is required");
                return;
            }
            
            if (content == null) {
                callbackContext.error("Content is required");
                return;
            }
            
            fileManagerBridge.writeFile(filePath, content, append, encoding, callbackContext);
            Log.d(TAG, "writeFile executed for: " + filePath);
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON error in writeFile", e);
            callbackContext.error("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error in writeFile", e);
            callbackContext.error("Error writing file: " + e.getMessage());
        }
    }

    private void createDirectory(JSONArray args, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Executing createDirectory action");
            
            if (fileManagerBridge == null) {
                callbackContext.error("File Manager module not available");
                return;
            }
            
            JSONObject arguments = args.getJSONObject(0);
            String dirPath = arguments.getString("dirPath");
            boolean createParents = arguments.optBoolean("createParents", true);
            
            // Validasi parameter required
            if (dirPath == null || dirPath.isEmpty()) {
                callbackContext.error("Directory path is required");
                return;
            }
            
            fileManagerBridge.createDirectory(dirPath, createParents, callbackContext);
            Log.d(TAG, "createDirectory executed for: " + dirPath);
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON error in createDirectory", e);
            callbackContext.error("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error in createDirectory", e);
            callbackContext.error("Error creating directory: " + e.getMessage());
        }
    }
	
    private void listDirectory(JSONArray args, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Executing listDirectory action");
            
            if (fileManagerBridge == null) {
                callbackContext.error("File Manager module not available");
                return;
            }
            
            JSONObject arguments = args.getJSONObject(0);
            String dirPath = arguments.getString("dirPath");
            boolean includeHidden = arguments.optBoolean("includeHidden", false);
            
            // Validasi parameter required
            if (dirPath == null || dirPath.isEmpty()) {
                callbackContext.error("Directory path is required");
                return;
            }
            
            fileManagerBridge.listDirectory(dirPath, includeHidden, callbackContext);
            Log.d(TAG, "listDirectory executed for: " + dirPath);
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON error in listDirectory", e);
            callbackContext.error("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error in listDirectory", e);
            callbackContext.error("Error listing directory: " + e.getMessage());
        }
    }

    private void deleteFile(JSONArray args, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Executing deleteFile action");
            
            if (fileManagerBridge == null) {
                callbackContext.error("File Manager module not available");
                return;
            }
            
            JSONObject arguments = args.getJSONObject(0);
            String filePath = arguments.getString("filePath");
            
            // Validasi parameter required
            if (filePath == null || filePath.isEmpty()) {
                callbackContext.error("File path is required");
                return;
            }
            
            fileManagerBridge.deleteFile(filePath, callbackContext);
            Log.d(TAG, "deleteFile executed for: " + filePath);
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON error in deleteFile", e);
            callbackContext.error("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error in deleteFile", e);
            callbackContext.error("Error deleting file: " + e.getMessage());
        }
    }

    private void deleteDirectory(JSONArray args, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Executing deleteDirectory action");
            
            if (fileManagerBridge == null) {
                callbackContext.error("File Manager module not available");
                return;
            }
            
            JSONObject arguments = args.getJSONObject(0);
            String dirPath = arguments.getString("dirPath");
            boolean recursive = arguments.optBoolean("recursive", true);
            
            // Validasi parameter required
            if (dirPath == null || dirPath.isEmpty()) {
                callbackContext.error("Directory path is required");
                return;
            }
            
            if (recursive) {
                fileManagerBridge.deleteDirectoryRecursively(dirPath, callbackContext);
            } else {
                // Untuk non-recursive delete, kita perlu implementasi sederhana
                deleteDirectorySimple(dirPath, callbackContext);
            }
            
            Log.d(TAG, "deleteDirectory executed for: " + dirPath + ", recursive: " + recursive);
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON error in deleteDirectory", e);
            callbackContext.error("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error in deleteDirectory", e);
            callbackContext.error("Error deleting directory: " + e.getMessage());
        }
    }

    private void deleteDirectorySimple(String dirPath, CallbackContext callbackContext) {
        try {
            File directory = new File(dirPath);
            
            if (!directory.exists()) {
                callbackContext.error(createFileErrorResponse(FileManagerBridge.FileError.NOT_FOUND_ERR,
                    "Directory not found: " + dirPath));
                return;
            }
            
            if (!directory.isDirectory()) {
                callbackContext.error(createFileErrorResponse(FileManagerBridge.FileError.TYPE_MISMATCH_ERR,
                    "Path is not a directory: " + dirPath));
                return;
            }
            
            // Check if directory is empty
            String[] files = directory.list();
            if (files != null && files.length > 0) {
                callbackContext.error(createFileErrorResponse(FileManagerBridge.FileError.INVALID_MODIFICATION_ERR,
                    "Directory is not empty: " + dirPath));
                return;
            }
            
            boolean success = directory.delete();
            
            if (success) {
                JSONObject result = new JSONObject();
                result.put("dirPath", dirPath);
                result.put("deleted", true);
                result.put("recursive", false);
                result.put("timestamp", System.currentTimeMillis());
                
                callbackContext.success(result);
                Log.d(TAG, "Directory deleted (non-recursive): " + dirPath);
                
                // Send event
                sendDirectoryEvent("directory_deleted", dirPath);
                
            } else {
                callbackContext.error(createFileErrorResponse(FileManagerBridge.FileError.NO_MODIFICATION_ALLOWED_ERR,
                    "Cannot delete directory: " + dirPath));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in deleteDirectorySimple", e);
            callbackContext.error("Error deleting directory: " + e.getMessage());
        }
    }

    private void getStorageInfo(CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Executing getStorageInfo action");
            
            if (fileManagerBridge == null) {
                callbackContext.error("File Manager module not available");
                return;
            }
            
            fileManagerBridge.getStorageInfo(callbackContext);
            Log.d(TAG, "getStorageInfo executed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in getStorageInfo", e);
            callbackContext.error("Error getting storage info: " + e.getMessage());
        }
    }

    private void searchFiles(JSONArray args, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Executing searchFiles action");
            
            if (fileManagerBridge == null) {
                callbackContext.error("File Manager module not available");
                return;
            }
            
            JSONObject arguments = args.getJSONObject(0);
            String searchDir = arguments.getString("searchDir");
            String searchPattern = arguments.getString("searchPattern");
            boolean searchInSubdirs = arguments.optBoolean("searchInSubdirs", true);
            boolean caseSensitive = arguments.optBoolean("caseSensitive", false);
            
            // Validasi parameter required
            if (searchDir == null || searchDir.isEmpty()) {
                callbackContext.error("Search directory is required");
                return;
            }
            
            if (searchPattern == null || searchPattern.isEmpty()) {
                callbackContext.error("Search pattern is required");
                return;
            }
            
            fileManagerBridge.searchFiles(searchDir, searchPattern, searchInSubdirs, caseSensitive, callbackContext);
            Log.d(TAG, "searchFiles executed in: " + searchDir + " for pattern: " + searchPattern);
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON error in searchFiles", e);
            callbackContext.error("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error in searchFiles", e);
            callbackContext.error("Error searching files: " + e.getMessage());
        }
    }

    private void getFileInfo(JSONArray args, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Executing getFileInfo action");
            
            if (fileManagerBridge == null) {
                callbackContext.error("File Manager module not available");
                return;
            }
            
            JSONObject arguments = args.getJSONObject(0);
            String filePath = arguments.getString("filePath");
            
            // Validasi parameter required
            if (filePath == null || filePath.isEmpty()) {
                callbackContext.error("File path is required");
                return;
            }
            
            fileManagerBridge.getFileInfo(filePath, callbackContext);
            Log.d(TAG, "getFileInfo executed for: " + filePath);
            
        } catch (JSONException e) {
            Log.e(TAG, "JSON error in getFileInfo", e);
            callbackContext.error("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error in getFileInfo", e);
            callbackContext.error("Error getting file info: " + e.getMessage());
        }
    }

    private JSONObject createFileErrorResponse(int code, String message) {
        try {
            JSONObject error = new JSONObject();
            error.put("code", code);
            error.put("message", message);
            error.put("timestamp", System.currentTimeMillis());
            return error;
        } catch (JSONException e) {
            Log.e(TAG, "Error creating file error response", e);
            return new JSONObject();
        }
    }

    private void sendDirectoryEvent(String eventType, String dirPath) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("eventType", eventType);
            eventData.put("dirPath", dirPath);
            eventData.put("timestamp", System.currentTimeMillis());
            
            sendEventToJavaScript("file_system_event", eventData);
            Log.d(TAG, "Directory event sent: " + eventType + " - " + dirPath);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error sending directory event", e);
        }
    }

    private void sendFileEvent(String eventType, String filePath) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("eventType", eventType);
            eventData.put("filePath", filePath);
            eventData.put("timestamp", System.currentTimeMillis());
            
            sendEventToJavaScript("file_system_event", eventData);
            Log.d(TAG, "File event sent: " + eventType + " - " + filePath);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error sending file event", e);
        }
    }

    private void getInstalledApps(CallbackContext callbackContext) {
        try {
            if (appDiscoveryBridge == null) {
                callbackContext.error("App Discovery module not available");
                return;
            }
            // Default: user apps only
            appDiscoveryBridge.getInstalledApps(false, callbackContext);
            Log.d(TAG, "getInstalledApps (USER ONLY) executed");
        } catch (Exception e) {
            Log.e(TAG, "Error getting installed apps", e);
            callbackContext.error("Error getting installed apps: " + e.getMessage());
        }
    }

    private void getAllInstalledApps(CallbackContext callbackContext) {
        try {
            if (appDiscoveryBridge == null) {
                callbackContext.error("App Discovery module not available");
                return;
            }
            // All apps including system
            appDiscoveryBridge.getAllInstalledApps(callbackContext);
            Log.d(TAG, "getAllInstalledApps executed");
        } catch (Exception e) {
            Log.e(TAG, "Error getting all installed apps", e);
            callbackContext.error("Error getting all installed apps: " + e.getMessage());
        }
    }

    private void getSystemApps(CallbackContext callbackContext) {
        try {
            if (appDiscoveryBridge == null) {
                callbackContext.error("App Discovery module not available");
                return;
            }
            appDiscoveryBridge.getSystemApps(callbackContext);
            Log.d(TAG, "getSystemApps executed");
        } catch (Exception e) {
            Log.e(TAG, "Error getting system apps", e);
            callbackContext.error("Error getting system apps: " + e.getMessage());
        }
    }

    private void getUserApps(CallbackContext callbackContext) {
        try {
            if (appDiscoveryBridge == null) {
                callbackContext.error("App Discovery module not available");
                return;
            }
            // User apps only (same as default getInstalledApps)
            appDiscoveryBridge.getInstalledApps(false, callbackContext);
            Log.d(TAG, "getUserApps executed");
        } catch (Exception e) {
            Log.e(TAG, "Error getting user apps", e);
            callbackContext.error("Error getting user apps: " + e.getMessage());
        }
    }

	private void getAppInfo(JSONObject args, CallbackContext callbackContext) {
		try {
			if (appDiscoveryBridge == null) {
				callbackContext.error("App Discovery module not available");
				return;
			}
			String packageName = args.getString("packageName");
			appDiscoveryBridge.getAppInfo(packageName, callbackContext);
			Log.d(TAG, "getAppInfo executed for: " + packageName);
		} catch (Exception e) {
			Log.e(TAG, "Error getting app info", e);
			callbackContext.error("Error getting app info: " + e.getMessage());
		}
	}
	
	private void uninstallApp(JSONObject args, CallbackContext callbackContext) {
		try {
			if (appDiscoveryBridge == null) {
				callbackContext.error("App Discovery module not available");
				return;
			}
			String packageName = args.getString("packageName");
			appDiscoveryBridge.uninstallApp(packageName, callbackContext);
			Log.d(TAG, "uninstallApp executed for: " + packageName);
		} catch (Exception e) {
			Log.e(TAG, "Error uninstalling app", e);
			callbackContext.error("Error uninstalling app: " + e.getMessage());
		}
	}

	private void launchApp(JSONObject args, CallbackContext callbackContext) {
		try {
			if (appDiscoveryBridge == null) {
				callbackContext.error("App Discovery module not available");
				return;
			}
			String packageName = args.getString("packageName");
			appDiscoveryBridge.launchApp(packageName, callbackContext);
			Log.d(TAG, "launchApp executed for: " + packageName);
		} catch (Exception e) {
			Log.e(TAG, "Error launching app", e);
			callbackContext.error("Error launching app: " + e.getMessage());
		}
	}

	private void openAppInPlayStore(JSONObject args, CallbackContext callbackContext) {
		try {
			if (appDiscoveryBridge == null) {
				callbackContext.error("App Discovery module not available");
				return;
			}
			String packageName = args.getString("packageName");
			appDiscoveryBridge.openAppInPlayStore(packageName, callbackContext);
			Log.d(TAG, "openAppInPlayStore executed for: " + packageName);
		} catch (Exception e) {
			Log.e(TAG, "Error opening app in Play Store", e);
			callbackContext.error("Error opening app in Play Store: " + e.getMessage());
		}
	}

	private void isAppInstalled(JSONObject args, CallbackContext callbackContext) {
		try {
			if (appDiscoveryBridge == null) {
				callbackContext.error("App Discovery module not available");
				return;
			}
			String packageName = args.getString("packageName");
			appDiscoveryBridge.isAppInstalled(packageName, callbackContext);
			Log.d(TAG, "isAppInstalled executed for: " + packageName);
		} catch (Exception e) {
			Log.e(TAG, "Error checking app installation", e);
			callbackContext.error("Error checking app installation: " + e.getMessage());
		}
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "Activity result received for requestCode: " + requestCode);
        
        // Handle media capture results (5001-5003)
        if (requestCode >= MediaManagerBridge.CAPTURE_AUDIO_REQUEST && 
            requestCode <= MediaManagerBridge.CAPTURE_VIDEO_REQUEST) {
            if (mediaManagerBridge != null) {
                mediaManagerBridge.onActivityResult(requestCode, resultCode, intent);
            } else {
                Log.e(TAG, "MediaManagerBridge is null, cannot handle activity result");
            }
            return;
        }
        
        // Handle other activity results...
        Log.d(TAG, "Activity result not handled by media manager, requestCode: " + requestCode);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "Permission result received for requestCode: " + requestCode);
        
        // Handle media permission results (5001-5003)
        if (requestCode >= MediaManagerBridge.CAPTURE_AUDIO_REQUEST && 
            requestCode <= MediaManagerBridge.CAPTURE_VIDEO_REQUEST) {
            if (mediaManagerBridge != null) {
                mediaManagerBridge.onRequestPermissionResult(requestCode, permissions, grantResults);
            } else {
                Log.e(TAG, "MediaManagerBridge is null, cannot handle permission result");
            }
            return;
        }
        
        // Handle other permission results
        if (permissionManager != null) {
            permissionManager.onRequestPermissionResult(requestCode, permissions, grantResults);
        } else {
            Log.e(TAG, "PermissionManager is null, cannot handle permission result");
        }
    }

    // =========================================================================
    // BASIC PLUGIN METHODS
    // =========================================================================

	private void getPluginInfo(CallbackContext callbackContext) {
        try {
            JSONObject info = new JSONObject();
            info.put("name", PluginConstants.PLUGIN_NAME);
            info.put("version", PluginConstants.PLUGIN_VERSION);
            info.put("status", "active");
            info.put("initialized", isInitialized);
            info.put("hasWallpaperModule", wallpaperBridge != null);
            info.put("hasPermissionModule", permissionManager != null);
            info.put("hasMediaModule", mediaManagerBridge != null);
            info.put("hasSystemMonitoring", true);
            info.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(info);
            Log.d(TAG, "Plugin info sent successfully");
            
        } catch (JSONException e) {
            Log.e(TAG, "Error getting plugin info", e);
            callbackContext.error("Error getting plugin info");
        }
    }

    private void checkStatus(CallbackContext callbackContext) {
        try {
            JSONObject status = new JSONObject();
            status.put("plugin", PluginConstants.PLUGIN_NAME);
            status.put("initialized", isInitialized);
            status.put("androidVersion", android.os.Build.VERSION.RELEASE);
            status.put("wallpaperModule", wallpaperBridge != null ? "available" : "unavailable");
            status.put("permissionModule", permissionManager != null ? "available" : "unavailable");
            status.put("mediaModule", mediaManagerBridge != null ? "available" : "unavailable");
            status.put("locationModule", locationManagerBridge != null ? "available" : "unavailable");
            status.put("networkModule", networkBridge != null ? "available" : "unavailable");
			status.put("btMeshModule", bluetoothMeshManager != null ? "available" : "unavailable");
            status.put("deviceModule", deviceManagerBridge != null ? "available" : "unavailable");
            status.put("systemMonitoring", "available"); 
            status.put("allPermissions", permissionManager != null && permissionManager.hasAllPermissions());
            status.put("timestamp", System.currentTimeMillis());

            callbackContext.success(status);
            Log.d(TAG, "Status check completed");
            
        } catch (JSONException e) {
            Log.e(TAG, "Error checking status", e);
            callbackContext.error("Error checking status");
        }
    }

    private void testConnection(CallbackContext callbackContext) {
        try {
            JSONObject result = new JSONObject();
            result.put("message", "Plugin is working with all modules!");
            result.put("code", "SUCCESS");
            result.put("wallpaperSupport", wallpaperBridge != null);
            result.put("permissionSupport", permissionManager != null);
            result.put("mediaSupport", mediaManagerBridge != null);
            result.put("systemMonitoring", true);
            result.put("allPermissions", permissionManager != null && permissionManager.hasAllPermissions());
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.d(TAG, "Connection test passed");
            
        } catch (JSONException e) {
            Log.e(TAG, "Error testing connection", e);
            callbackContext.error("Error testing connection");
        }
    }

    private void getSystemInfo(CallbackContext callbackContext) {
        try {
            JSONObject systemInfo = new JSONObject();
            
            // Basic device info
            systemInfo.put("model", android.os.Build.MODEL);
            systemInfo.put("manufacturer", android.os.Build.MANUFACTURER);
            systemInfo.put("androidVersion", android.os.Build.VERSION.RELEASE);
            systemInfo.put("sdkVersion", android.os.Build.VERSION.SDK_INT);
            
            // Plugin info
            systemInfo.put("pluginInitialized", isInitialized);
            systemInfo.put("pluginVersion", PluginConstants.PLUGIN_VERSION);
            systemInfo.put("wallpaperModule", wallpaperBridge != null);
            systemInfo.put("permissionModule", permissionManager != null);
            systemInfo.put("mediaModule", mediaManagerBridge != null);
            systemInfo.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(systemInfo);
            Log.d(TAG, "System info sent successfully");
            
        } catch (JSONException e) {
            Log.e(TAG, "Error getting system info", e);
            callbackContext.error("Error getting system info");
        }
    }

    // =========================================================================
    // PERMISSION MANAGEMENT METHODS 
    // =========================================================================

    private void getPermissionStatus(CallbackContext callbackContext) {
        try {
            if (permissionManager == null) {
                callbackContext.error("Permission module not available");
                return;
            }
            
            JSONObject status = permissionManager.getPermissionStatus();
            callbackContext.success(status);
            Log.d(TAG, "Permission status sent successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting permission status", e);
            callbackContext.error("Error getting permission status: " + e.getMessage());
        }
    }
	
	private void requestPermission(JSONObject args, CallbackContext callbackContext) {
		try {
			if (permissionManager == null) {
				callbackContext.error("Permission module not available");
				return;
			}
			
			String permission = args.optString("permission", "");
			String action = args.optString("action", "");
			String permissionType = args.optString("type", "STORAGE");
			
			Log.d(TAG, "Requesting permission - action: " + action + ", permission: " + permission + ", type: " + permissionType);
			
			// Use PermissionManager to handle the permission request
			permissionManager.requestPermissions(action, args, callbackContext);
			
		} catch (Exception e) {
			Log.e(TAG, "Error in requestPermission", e);
			callbackContext.error("Error requesting permission: " + e.getMessage());
		}
	}

    private void requestPermissions(JSONObject args, CallbackContext callbackContext) {
        try {
            if (permissionManager == null) {
                callbackContext.error("Permission module not available");
                return;
            }
            
            String permissionType = args.optString("type", "ALL");
            permissionManager.requestPermissionType(permissionType, args, callbackContext);
            Log.d(TAG, "Permission request initiated for type: " + permissionType);
            
        } catch (Exception e) {
            Log.e(TAG, "Error requesting permissions", e);
            callbackContext.error("Error requesting permissions: " + e.getMessage());
        }
    }

    private void hasStoragePermission(CallbackContext callbackContext) {
        try {
            if (permissionManager == null) {
                callbackContext.error("Permission module not available");
                return;
            }
            
            JSONObject result = new JSONObject();
            result.put("hasPermission", permissionManager.hasStoragePermission());
            callbackContext.success(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking storage permission", e);
            callbackContext.error("Error checking storage permission: " + e.getMessage());
        }
    }

    private void hasNetworkPermissions(CallbackContext callbackContext) {
        try {
            if (permissionManager == null) {
                callbackContext.error("Permission module not available");
                return;
            }
            
            JSONObject result = new JSONObject();
            result.put("hasPermission", permissionManager.hasNetworkPermissions());
            callbackContext.success(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking network permissions", e);
            callbackContext.error("Error checking network permissions: " + e.getMessage());
        }
    }

    // =========================================================================
    // WALLPAPER MANAGER METHODS (WITH PERMISSION CHECKS)
    // =========================================================================

    private void getWallpaper(JSONObject args, CallbackContext callbackContext) {
        try {
            if (wallpaperBridge == null) {
                callbackContext.error("Wallpaper module not available");
                return;
            }
            // Check permissions first
            if (permissionManager.hasRequiredPermissions("getWallpaper")) {
                wallpaperBridge.getWallpaper(callbackContext);
                Log.d(TAG, "getWallpaper executed with permissions");
            } else {
                permissionManager.requestPermissions("getWallpaper", args, callbackContext);
                Log.d(TAG, "getWallpaper - requesting permissions first");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in getWallpaper", e);
            callbackContext.error("Error getting wallpaper: " + e.getMessage());
        }
    }

    private void getWallpaperInfo(JSONObject args, CallbackContext callbackContext) {
        try {
            if (wallpaperBridge == null) {
                callbackContext.error("Wallpaper module not available");
                return;
            }
            
            // Check permissions first
            if (permissionManager.hasRequiredPermissions("getWallpaperInfo")) {
                wallpaperBridge.getWallpaperInfo(callbackContext);
                Log.d(TAG, "getWallpaperInfo executed with permissions");
            } else {
                permissionManager.requestPermissions("getWallpaperInfo", args, callbackContext);
                Log.d(TAG, "getWallpaperInfo - requesting permissions first");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in getWallpaperInfo", e);
            callbackContext.error("Error getting wallpaper info: " + e.getMessage());
        }
    }
	
	private void setWallpaperFromFile(JSONObject args, CallbackContext callbackContext) {
		try {
			if (wallpaperBridge == null) {
				callbackContext.error("Wallpaper module not available");
				return;
			}
			
			// Extract parameters dari JSON args
			String filePath = args.optString("filePath", "");
			int wallpaperType = args.optInt("wallpaperType", 1); // Default ke WALLPAPER_HOME
			
			// Check permissions first
			if (permissionManager.hasRequiredPermissions("setWallpaperFromFile")) {
				wallpaperBridge.setWallpaperFromFile(filePath, wallpaperType, callbackContext);
				Log.d(TAG, "setWallpaperFromFile executed with permissions");
			} else {
				permissionManager.requestPermissions("setWallpaperFromFile", args, callbackContext);
				Log.d(TAG, "setWallpaperFromFile - requesting permissions first");
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Error in setWallpaperFromFile", e);
			callbackContext.error("Error in setWallpaperFromFile: " + e.getMessage());
		}
	}
	
	private void setWallpaperFromUri(JSONObject args, CallbackContext callbackContext) {
		try {
			if (wallpaperBridge == null) {
				callbackContext.error("Wallpaper module not available");
				return;
			}
			
			// Extract parameters dari JSON args
			String uriString = args.optString("uri", "");
			int wallpaperType = args.optInt("wallpaperType", 1); // Default ke WALLPAPER_HOME
			
			// Check permissions first
			if (permissionManager.hasRequiredPermissions("setWallpaperFromUri")) {
				wallpaperBridge.setWallpaperFromUri(uriString, wallpaperType, callbackContext);
				Log.d(TAG, "setWallpaperFromUri executed with permissions");
			} else {
				permissionManager.requestPermissions("setWallpaperFromUri", args, callbackContext);
				Log.d(TAG, "setWallpaperFromUri - requesting permissions first");
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Error in setWallpaperFromUri", e);
			callbackContext.error("Error in setWallpaperFromUri: " + e.getMessage());
		}
	}

    private void listenWallpaperChanged(JSONObject args, CallbackContext callbackContext) {
        try {
            if (wallpaperBridge == null) {
                callbackContext.error("Wallpaper module not available");
                return;
            }
            
            // Check permissions first
            if (permissionManager.hasRequiredPermissions("listenWallpaperChanged")) {
                wallpaperBridge.listenWallpaperChanged(callbackContext);
                Log.d(TAG, "Wallpaper change listener started with permissions");
            } else {
                permissionManager.requestPermissions("listenWallpaperChanged", args, callbackContext);
                Log.d(TAG, "Wallpaper listener - requesting permissions first");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting wallpaper listener", e);
            callbackContext.error("Error starting wallpaper listener: " + e.getMessage());
        }
    }

    private void stopListeningWallpaper(CallbackContext callbackContext) {
        try {
            if (wallpaperBridge == null) {
                callbackContext.error("Wallpaper module not available");
                return;
            }
            
            wallpaperBridge.stopListening();
            callbackContext.success("Wallpaper listener stopped");
            Log.d(TAG, "Wallpaper change listener stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping wallpaper listener", e);
            callbackContext.error("Error stopping wallpaper listener: " + e.getMessage());
        }
    }
    
    // =========================================================================
    // NETWORK MANAGER METHODS
    // =========================================================================
    
    private void getNetworkStatus(CallbackContext callbackContext) {
        try {
            if (networkBridge == null) {
                callbackContext.error("Network module not available");
                return;
            }
            networkBridge.getNetworkStatus(callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error getting network status", e);
            callbackContext.error("Error getting network status: " + e.getMessage());
        }
    }

    private void getWifiStatus(CallbackContext callbackContext) {
        try {
            if (networkBridge == null) {
                callbackContext.error("Network module not available");
                return;
            }
            networkBridge.getWifiStatus(callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error getting WiFi status", e);
            callbackContext.error("Error getting WiFi status: " + e.getMessage());
        }
    }

    private void getMobileStatus(CallbackContext callbackContext) {
        try {
            if (networkBridge == null) {
                callbackContext.error("Network module not available");
                return;
            }
            networkBridge.getMobileStatus(callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error getting mobile status", e);
            callbackContext.error("Error getting mobile status: " + e.getMessage());
        }
    }

    private void getDeviceInfo(CallbackContext callbackContext) {
        try {
            if (deviceManagerBridge == null) {
                callbackContext.error("Device manager module not available");
                return;
            }
            deviceManagerBridge.getDeviceInfo(callbackContext);
            Log.d(TAG, "getDeviceInfo executed");
        } catch (Exception e) {
            Log.e(TAG, "Error getting device info", e);
            callbackContext.error("Error getting device info: " + e.getMessage());
        }
    }

    private void getBatteryStatus(CallbackContext callbackContext) {
        try {
            if (deviceManagerBridge == null) {
                callbackContext.error("Device manager module not available");
                return;
            }
            deviceManagerBridge.getBatteryStatus(callbackContext);
            Log.d(TAG, "getBatteryStatus executed");
        } catch (Exception e) {
            Log.e(TAG, "Error getting battery status", e);
            callbackContext.error("Error getting battery status: " + e.getMessage());
        }
    }

    private void startBatteryListener(CallbackContext callbackContext) {
        try {
            if (deviceManagerBridge == null) {
                callbackContext.error("Device manager module not available");
                return;
            }
            deviceManagerBridge.startBatteryListener(callbackContext);
            Log.d(TAG, "startBatteryListener executed");
        } catch (Exception e) {
            Log.e(TAG, "Error starting battery listener", e);
            callbackContext.error("Error starting battery listener: " + e.getMessage());
        }
    }

    private void stopBatteryListener(CallbackContext callbackContext) {
        try {
            if (deviceManagerBridge == null) {
                callbackContext.error("Device manager module not available");
                return;
            }
            deviceManagerBridge.stopBatteryListener(callbackContext);
            Log.d(TAG, "stopBatteryListener executed");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping battery listener", e);
            callbackContext.error("Error stopping battery listener: " + e.getMessage());
        }
    }   

    // =========================================================================
    // MEDIA METHODS - NEW
    // =========================================================================

    private void createAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.createAudio(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error creating audio", e);
            callbackContext.error("Error creating audio: " + e.getMessage());
        }
    }

    private void playAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.playAudio(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio", e);
            callbackContext.error("Error playing audio: " + e.getMessage());
        }
    }

    private void pauseAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.pauseAudio(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error pausing audio", e);
            callbackContext.error("Error pausing audio: " + e.getMessage());
        }
    }

    private void stopAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.stopAudio(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping audio", e);
            callbackContext.error("Error stopping audio: " + e.getMessage());
        }
    }

    private void seekAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.seekAudio(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error seeking audio", e);
            callbackContext.error("Error seeking audio: " + e.getMessage());
        }
    }

    private void getAudioDuration(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.getAudioDuration(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error getting audio duration", e);
            callbackContext.error("Error getting audio duration: " + e.getMessage());
        }
    }

    private void getAudioPosition(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.getAudioPosition(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error getting audio position", e);
            callbackContext.error("Error getting audio position: " + e.getMessage());
        }
    }

    private void setAudioVolume(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.setAudioVolume(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error setting audio volume", e);
            callbackContext.error("Error setting audio volume: " + e.getMessage());
        }
    }

    private void releaseAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.releaseAudio(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error releasing audio", e);
            callbackContext.error("Error releasing audio: " + e.getMessage());
        }
    }

    // =========================================================================
    // MEDIA CAPTURE METHODS
    // =========================================================================

    private void captureAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.captureAudio(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error capturing audio", e);
            callbackContext.error("Error capturing audio: " + e.getMessage());
        }
    }

    private void captureImage(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.captureImage(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error capturing image", e);
            callbackContext.error("Error capturing image: " + e.getMessage());
        }
    }

    private void captureVideo(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.captureVideo(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error capturing video", e);
            callbackContext.error("Error capturing video: " + e.getMessage());
        }
    }

    private void getSupportedFormats(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mediaManagerBridge == null) {
                callbackContext.error("Media module not available");
                return;
            }
            mediaManagerBridge.getSupportedFormats(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error getting supported formats", e);
            callbackContext.error("Error getting supported formats: " + e.getMessage());
        }
    }
	
	// =========================================================================
	// PRO CAMERA DELEGATION METHODS
	// =========================================================================

	private void proCameraOpen(JSONObject args, CallbackContext callbackContext) {
		try {
			if (mediaManagerBridge == null) {
				callbackContext.error("Media module not available");
				return;
			}
			mediaManagerBridge.proCameraOpen(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error in proCameraOpen", e);
			callbackContext.error("Error opening Pro Camera: " + e.getMessage());
		}
	}

	private void proCameraCapture(JSONObject args, CallbackContext callbackContext) {
		try {
			if (mediaManagerBridge == null) {
				callbackContext.error("Media module not available");
				return;
			}
			mediaManagerBridge.proCameraCapture(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error in proCameraCapture", e);
			callbackContext.error("Error capturing with Pro Camera: " + e.getMessage());
		}
	}

	private void proCameraSetISO(JSONObject args, CallbackContext callbackContext) {
		try {
			if (mediaManagerBridge == null) {
				callbackContext.error("Media module not available");
				return;
			}
			mediaManagerBridge.proCameraSetISO(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error in proCameraSetISO", e);
			callbackContext.error("Error setting ISO: " + e.getMessage());
		}
	}

	private void proCameraSetShutterSpeed(JSONObject args, CallbackContext callbackContext) {
		try {
			if (mediaManagerBridge == null) {
				callbackContext.error("Media module not available");
				return;
			}
			mediaManagerBridge.proCameraSetShutterSpeed(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error in proCameraSetShutterSpeed", e);
			callbackContext.error("Error setting shutter speed: " + e.getMessage());
		}
	}

	private void proCameraSetWhiteBalance(JSONObject args, CallbackContext callbackContext) {
		try {
			if (mediaManagerBridge == null) {
				callbackContext.error("Media module not available");
				return;
			}
			mediaManagerBridge.proCameraSetWhiteBalance(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error in proCameraSetWhiteBalance", e);
			callbackContext.error("Error setting white balance: " + e.getMessage());
		}
	}

	private void proCameraSetFocus(JSONObject args, CallbackContext callbackContext) {
		try {
			if (mediaManagerBridge == null) {
				callbackContext.error("Media module not available");
				return;
			}
			mediaManagerBridge.proCameraSetFocus(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error in proCameraSetFocus", e);
			callbackContext.error("Error setting focus: " + e.getMessage());
		}
	}

	private void proCameraSetExposureCompensation(JSONObject args, CallbackContext callbackContext) {
		try {
			if (mediaManagerBridge == null) {
				callbackContext.error("Media module not available");
				return;
			}
			mediaManagerBridge.proCameraSetExposureCompensation(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error in proCameraSetExposureCompensation", e);
			callbackContext.error("Error setting exposure compensation: " + e.getMessage());
		}
	}

	private void proCameraEnableRAW(JSONObject args, CallbackContext callbackContext) {
		try {
			if (mediaManagerBridge == null) {
				callbackContext.error("Media module not available");
				return;
			}
			mediaManagerBridge.proCameraEnableRAW(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error in proCameraEnableRAW", e);
			callbackContext.error("Error enabling RAW: " + e.getMessage());
		}
	}

	private void proCameraClose(JSONObject args, CallbackContext callbackContext) {
		try {
			if (mediaManagerBridge == null) {
				callbackContext.error("Media module not available");
				return;
			}
			mediaManagerBridge.proCameraClose(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error in proCameraClose", e);
			callbackContext.error("Error closing Pro Camera: " + e.getMessage());
		}
	}

	private void proCameraGetSettings(JSONObject args, CallbackContext callbackContext) {
		try {
			if (mediaManagerBridge == null) {
				callbackContext.error("Media module not available");
				return;
			}
			mediaManagerBridge.proCameraGetSettings(args, callbackContext);
		} catch (Exception e) {
			Log.e(TAG, "Error in proCameraGetSettings", e);
			callbackContext.error("Error getting camera settings: " + e.getMessage());
		}
	}

    private void getCurrentLocation(JSONObject args, CallbackContext callbackContext) {
        try {
            if (locationManagerBridge == null) {
                callbackContext.error("Location module not available");
                return;
            }
            locationManagerBridge.getCurrentLocation(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error getting current location", e);
            callbackContext.error("Error getting current location: " + e.getMessage());
        }
    }

    private void startLocationTracking(JSONObject args, CallbackContext callbackContext) {
        try {
            if (locationManagerBridge == null) {
                callbackContext.error("Location module not available");
                return;
            }
            locationManagerBridge.startLocationTracking(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error starting location tracking", e);
            callbackContext.error("Error starting location tracking: " + e.getMessage());
        }
    }

    private void stopLocationTracking(CallbackContext callbackContext) {
        try {
            if (locationManagerBridge == null) {
                callbackContext.error("Location module not available");
                return;
            }
            locationManagerBridge.stopLocationTracking(callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping location tracking", e);
            callbackContext.error("Error stopping location tracking: " + e.getMessage());
        }
    }

    private void getLocationPermissions(CallbackContext callbackContext) {
        try {
            if (locationManagerBridge == null) {
                callbackContext.error("Location module not available");
                return;
            }
            locationManagerBridge.getLocationPermissions(callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error getting location permissions", e);
            callbackContext.error("Error getting location permissions: " + e.getMessage());
        }
    }

    private void calculateDistance(JSONObject args, CallbackContext callbackContext) {
        try {
            if (locationManagerBridge == null) {
                callbackContext.error("Location module not available");
                return;
            }
            locationManagerBridge.calculateDistance(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance", e);
            callbackContext.error("Error calculating distance: " + e.getMessage());
        }
    }

    private void getAvailableProviders(CallbackContext callbackContext) {
        try {
            if (locationManagerBridge == null) {
                callbackContext.error("Location module not available");
                return;
            }
            locationManagerBridge.getAvailableProviders(callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error getting available providers", e);
            callbackContext.error("Error getting available providers: " + e.getMessage());
        }
    }

    /**
    * Get comprehensive system resources information
    * Includes: Memory, CPU, Storage, Thermal status
    */
    private void getSystemResources(CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Getting system resources via DeviceManagerBridge");
            
            if (deviceManagerBridge == null) {
                callbackContext.error("Device manager module not available");
                return;
            }
            
            deviceManagerBridge.getSystemResources(callbackContext);
            Log.d(TAG, "getSystemResources executed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting system resources", e);
            callbackContext.error("Error getting system resources: " + e.getMessage());
        }
    }

    /**
     * Get list of all available sensors on the device
     */
    private void getAvailableSensors(CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Getting available sensors list via DeviceManagerBridge");
            
            if (deviceManagerBridge == null) {
                callbackContext.error("Device manager module not available");
                return;
            }
            
            deviceManagerBridge.getAvailableSensors(callbackContext);
            Log.d(TAG, "getAvailableSensors executed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting available sensors", e);
            callbackContext.error("Error getting available sensors: " + e.getMessage());
        }
    }

    /**
     * Get detailed information about specific sensor types
     */
    private void getSensorCapabilities(CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Getting sensor capabilities via DeviceManagerBridge");
            
            if (deviceManagerBridge == null) {
                callbackContext.error("Device manager module not available");
                return;
            }
            
            deviceManagerBridge.getSensorCapabilities(callbackContext);
            Log.d(TAG, "getSensorCapabilities executed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting sensor capabilities", e);
            callbackContext.error("Error getting sensor capabilities: " + e.getMessage());
        }
    }

    /**
     * Run comprehensive device health diagnostics
     */
    private void runDeviceDiagnostics(CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Running device health diagnostics via DeviceManagerBridge");
            
            if (deviceManagerBridge == null) {
                callbackContext.error("Device manager module not available");
                return;
            }
            
            deviceManagerBridge.runDeviceDiagnostics(callbackContext);
            Log.d(TAG, "runDeviceDiagnostics executed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error running device diagnostics", e);
            callbackContext.error("Error running device diagnostics: " + e.getMessage());
        }
    }
	
	// =========================================================================
	// DISPLAY MANAGEMENT METHODS - NEW
	// =========================================================================

	private void enterFullscreen(CallbackContext callbackContext) {
		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.enterFullscreen(callbackContext);
			Log.d(TAG, "enterFullscreen executed");
		} catch (Exception e) {
			Log.e(TAG, "Error entering fullscreen", e);
			callbackContext.error("Error entering fullscreen: " + e.getMessage());
		}
	}
	
	private void enterPermanentFullscreen(CallbackContext callbackContext) {
		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.enterPermanentFullscreen(callbackContext);
			Log.d(TAG, "enterFullscreen executed");
		} catch (Exception e) {
			Log.e(TAG, "Error entering fullscreen", e);
			callbackContext.error("Error entering fullscreen: " + e.getMessage());
		}
	}

	private void exitFullscreen(CallbackContext callbackContext) {
		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.exitFullscreen(callbackContext);
			Log.d(TAG, "exitFullscreen executed");
		} catch (Exception e) {
			Log.e(TAG, "Error exiting fullscreen", e);
			callbackContext.error("Error exiting fullscreen: " + e.getMessage());
		}
	}

	private void toggleFullscreen(CallbackContext callbackContext) {
		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.toggleFullscreen(callbackContext);
			Log.d(TAG, "toggleFullscreen executed");
		} catch (Exception e) {
			Log.e(TAG, "Error toggling fullscreen", e);
			callbackContext.error("Error toggling fullscreen: " + e.getMessage());
		}
	}

	private void setScreenOrientation(JSONObject args, CallbackContext callbackContext) {
		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.setScreenOrientation(args, callbackContext);
			Log.d(TAG, "setScreenOrientation executed");
		} catch (Exception e) {
			Log.e(TAG, "Error setting screen orientation", e);
			callbackContext.error("Error setting screen orientation: " + e.getMessage());
		}
	}

	private void keepScreenOn(JSONObject args, CallbackContext callbackContext) {
		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.keepScreenOn(args, callbackContext);
			Log.d(TAG, "keepScreenOn executed");
		} catch (Exception e) {
			Log.e(TAG, "Error setting keep screen on", e);
			callbackContext.error("Error setting keep screen on: " + e.getMessage());
		}
	}

	private void getDisplayMetrics(CallbackContext callbackContext) {
		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.getDisplayMetrics(callbackContext);
			Log.d(TAG, "getDisplayMetrics executed");
		} catch (Exception e) {
			Log.e(TAG, "Error getting display metrics", e);
			callbackContext.error("Error getting display metrics: " + e.getMessage());
		}
	}

	private void getFullscreenStatus(CallbackContext callbackContext) {
		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.getFullscreenStatus(callbackContext);
			Log.d(TAG, "getFullscreenStatus executed");
		} catch (Exception e) {
			Log.e(TAG, "Error getting fullscreen status", e);
			callbackContext.error("Error getting fullscreen status: " + e.getMessage());
		}
	}

	private void enterImmersiveMode(CallbackContext callbackContext) {
		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.enterImmersiveMode(callbackContext);
			Log.d(TAG, "enterImmersiveMode executed");
		} catch (Exception e) {
			Log.e(TAG, "Error entering immersive mode", e);
			callbackContext.error("Error entering immersive mode: " + e.getMessage());
		}
	}
	
	// Method dengan parameter (untuk call dari JavaScript dengan parameter)
	private void setScreenshotAllowed(JSONObject args, CallbackContext callbackContext) {
		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.setScreenshotAllowed(args, callbackContext);
			Log.d(TAG, "setScreenshotAllowed executed");
		} catch (Exception e) {
			Log.e(TAG, "Error setScreenshotAllowed", e);
			callbackContext.error("Error setScreenshotAllowed: " + e.getMessage());
		}
		
	}
	
	private void getScreenshotStatus(CallbackContext callbackContext) {

		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.getScreenshotStatus(callbackContext);
			Log.d(TAG, "getScreenshotStatus executed");
		} catch (Exception e) {
			Log.e(TAG, "Error getScreenshotStatus", e);
			callbackContext.error("Error getScreenshotStatus: " + e.getMessage());
		}
	}
	
	private void enableContentProtection(JSONObject args, CallbackContext callbackContext) {
		try {
			if (displayManagerBridge == null) {
				callbackContext.error("Display manager module not available");
				return;
			}
			displayManagerBridge.enableContentProtection(args, callbackContext);
			Log.d(TAG, "enableContentProtection executed");
		} catch (Exception e) {
			Log.e(TAG, "Error setting screen orientation", e);
			callbackContext.error("Error enableContentProtection: " + e.getMessage());
		}
		
	}

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    private void executeActionWithPermissions(String action, JSONObject args, CallbackContext callbackContext) {
        try {
            switch (action) {
                case "getWallpaper":
                    if (wallpaperBridge != null) {
                        wallpaperBridge.getWallpaper(callbackContext);
                    }
                    break;
                    
                case "getWallpaperInfo":
                    if (wallpaperBridge != null) {
                        wallpaperBridge.getWallpaperInfo(callbackContext);
                    }
                    break;
                    
                case "listenWallpaperChanged":
                    if (wallpaperBridge != null) {
                        wallpaperBridge.listenWallpaperChanged(callbackContext);
                    }
                    break;
                    
                // NEW: Media actions
                case "captureAudio":
                    if (mediaManagerBridge != null) {
                        mediaManagerBridge.captureAudio(args, callbackContext);
                    }
                    break;
                    
                case "captureImage":
                    if (mediaManagerBridge != null) {
                        mediaManagerBridge.captureImage(args, callbackContext);
                    }
                    break;
					
				case "requestPermission":
					// Permission sudah diberikan, kirim success response
					try {
						JSONObject result = new JSONObject();
						result.put("status", "permissions_granted");
						result.put("action", args.optString("action", "unknown"));
						result.put("permission", args.optString("permission", "unknown"));
						callbackContext.success(result);
					} catch (JSONException e) {
						callbackContext.success("Permissions granted for: " + args.optString("action", "unknown"));
					}
					break;
                    
                case "captureVideo":
                    if (mediaManagerBridge != null) {
                        mediaManagerBridge.captureVideo(args, callbackContext);
                    }
                    break;
                    
                default:
                    Log.w(TAG, "No action handler for: " + action);
                    callbackContext.error("No handler for action: " + action);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing action with permissions: " + action, e);
            callbackContext.error("Error executing action: " + e.getMessage());
        }
    }

    private void sendPermissionError(CallbackContext cb, String reason) {
        if (cb == null) return;
        
        try {
            JSONObject error = new JSONObject();
            error.put("error", "Permissions denied");
            error.put("reason", reason);
            error.put("code", "PERMISSION_DENIED");
            cb.error(error);
        } catch (JSONException e) {
            cb.error("{\"error\":\"Permissions denied\",\"reason\":\"" + 
                    reason.replace("\"", "\\\"") + "\",\"code\":\"PERMISSION_DENIED\"}");
        }
    }

    private void sendEventToJavaScript(String eventName, JSONObject data) {
        // Implement event sending to JavaScript
        String js = String.format("javascript:if(window.ResqPeerNet&&window.ResqPeerNet.onEvent){window.ResqPeerNet.onEvent('%s', %s);}", 
                                eventName, data.toString());
        webView.loadUrl(js);
        Log.d(TAG, "Event sent to JavaScript: " + eventName);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "ResqPeerNet destroyed");
        
        // Clean up all modules
        if (wallpaperBridge != null) {
            wallpaperBridge.destroy();
            wallpaperBridge = null;
        }
        
        if (permissionManager != null) {
            permissionManager.destroy();
            permissionManager = null;
        }
        
        if (networkBridge != null) {
            networkBridge.destroy();
            networkBridge = null;
        }
		
		if (bluetoothMeshManager != null) {
			bluetoothMeshManager.destroy();
			bluetoothMeshManager = null;
		}

        if (deviceManagerBridge != null) {
            deviceManagerBridge.destroy();
            deviceManagerBridge = null;
        }
        
        // Clean up media manager
        if (mediaManagerBridge != null) {
            mediaManagerBridge.destroy();
            mediaManagerBridge = null;
        }
		
		// Clean up display manager
		if (displayManagerBridge != null) {
			displayManagerBridge.destroy();
			displayManagerBridge = null;
		}
		
		if (appDiscoveryBridge != null) {
			appDiscoveryBridge.destroy();
			appDiscoveryBridge = null;
		}

        if (fileManagerBridge != null) {
            fileManagerBridge.destroy();
            fileManagerBridge = null;
        }
		        
        isInitialized = false;
        super.onDestroy();
    }

    @Override
    public void onReset() {
        Log.i(TAG, "ResqPeerNet reset");
        
        // Stop listening when WebView navigates
        if (wallpaperBridge != null) {
            wallpaperBridge.stopListening();
        }
        
        super.onReset();
    }
}