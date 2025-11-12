package org.apache.cordova.resqpeernet.modules;

import android.Manifest;
import android.os.Build;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONObject;
import org.json.JSONException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PermissionManager - Centralized Permission Management for ResqPeerNet
 * UPDATED VERSION - Fully compatible with Android 6-15
 */
public class PermissionManager {

    private static final String TAG = "PermissionManager";
    
    // Permission Request Codes
    public static final int STORAGE_PERMISSION_REQUEST_CODE = 1001;
    public static final int NETWORK_PERMISSION_REQUEST_CODE = 1002;
    public static final int MEDIA_PERMISSION_REQUEST_CODE = 1003;
    public static final int AUDIO_PERMISSION_REQUEST_CODE = 1004;
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 1005;
    public static final int ALL_PERMISSIONS_REQUEST_CODE = 1006;
    public static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1007;
    
    private final CordovaInterface cordova;
    private final PermissionCallback callback;
    private final Map<Integer, PermissionRequest> permissionQueue;
    private final AtomicInteger permissionRequestId;
    private final AtomicBoolean isDestroyed;

    public interface PermissionCallback {
        void onPermissionsGranted(String action, JSONObject args, CallbackContext callbackContext);
        void onPermissionsDenied(String action, JSONObject args, CallbackContext callbackContext, String reason);
        void requestPermissions(int requestId, String[] permissions);
    }

    private static class PermissionRequest {
        final CallbackContext callback;
        final String action;
        final JSONObject args;
        final long timestamp;
        final String permissionType;

        PermissionRequest(CallbackContext callback, String action, JSONObject args, String permissionType) {
            this.callback = callback;
            this.action = action;
            this.args = args;
            this.timestamp = System.currentTimeMillis();
            this.permissionType = permissionType;
        }
    }

    public PermissionManager(CordovaInterface cordova, PermissionCallback callback) {
        this.cordova = cordova;
        this.callback = callback;
        this.permissionQueue = new ConcurrentHashMap<>();
        this.permissionRequestId = new AtomicInteger(1000);
        this.isDestroyed = new AtomicBoolean(false);
    }

    /**
     * Check if storage permission is granted
     */
    public boolean hasStoragePermission() {
        if (isDestroyed.get()) return false;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ uses new media permissions
                return cordova.hasPermission(Manifest.permission.READ_MEDIA_IMAGES) &&
                       cordova.hasPermission(Manifest.permission.READ_MEDIA_VIDEO) &&
                       cordova.hasPermission(Manifest.permission.READ_MEDIA_AUDIO);
            } else {
                return cordova.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                       cordova.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking storage permission", e);
            return false;
        }
    }

    /**
     * Check if all network permissions are granted
     */
    public boolean hasNetworkPermissions() {
        if (isDestroyed.get()) return false;
        
        try {
            boolean hasNetworkState = cordova.hasPermission(Manifest.permission.ACCESS_NETWORK_STATE);
            boolean hasWifiState = cordova.hasPermission(Manifest.permission.ACCESS_WIFI_STATE);
            
            // For Android 10+, we need location permissions for WiFi SSID
            boolean hasLocationPermissions = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hasLocationPermissions = cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                                       cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            
            return hasNetworkState && hasWifiState && hasLocationPermissions;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking network permissions", e);
            return false;
        }
    }

    /**
     * Check if audio recording permission is granted
     */
    public boolean hasAudioRecordingPermission() {
        if (isDestroyed.get()) return false;
        
        try {
            return cordova.hasPermission(Manifest.permission.RECORD_AUDIO);
        } catch (Exception e) {
            Log.e(TAG, "Error checking audio recording permission", e);
            return false;
        }
    }

    /**
     * Check if camera permission is granted
     */
    public boolean hasCameraPermission() {
        if (isDestroyed.get()) return false;
        
        try {
            boolean hasCamera = cordova.hasPermission(Manifest.permission.CAMERA);
            boolean hasStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ? 
                               hasStoragePermission() : 
                               cordova.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            
            return hasCamera && hasStorage;
        } catch (Exception e) {
            Log.e(TAG, "Error checking camera permission", e);
            return false;
        }
    }
    
    /**
     * Check if all media permissions are granted
     */
    public boolean hasMediaPermissions() {
        return hasAudioRecordingPermission() && hasCameraPermission() && hasStoragePermission();
    }

    /**
     * Check if Bluetooth permissions are granted for Android 6-15
     */
    public boolean hasBluetoothPermissions() {
        if (isDestroyed.get()) return false;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires new Bluetooth permissions
                boolean hasConnect = cordova.hasPermission(Manifest.permission.BLUETOOTH_CONNECT);
                boolean hasScan = cordova.hasPermission(Manifest.permission.BLUETOOTH_SCAN);
                boolean hasAdvertise = cordova.hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE);
                
                return hasConnect && hasScan && hasAdvertise;
            } else {
                // Android 6-11
                boolean hasBluetooth = cordova.hasPermission(Manifest.permission.BLUETOOTH);
                boolean hasBluetoothAdmin = cordova.hasPermission(Manifest.permission.BLUETOOTH_ADMIN);
                boolean hasLocation = cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                                    cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
                
                return hasBluetooth && hasBluetoothAdmin && hasLocation;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking Bluetooth permissions", e);
            return false;
        }
    }

    /**
     * Check if notification permission is granted (Android 13+)
     */
    public boolean hasNotificationPermission() {
        if (isDestroyed.get() || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true; // Permission not required before Android 13
        }
        
        try {
            return cordova.hasPermission(Manifest.permission.POST_NOTIFICATIONS);
        } catch (Exception e) {
            Log.e(TAG, "Error checking notification permission", e);
            return false;
        }
    }

    /**
     * Check if all required permissions are granted for Android 6-15
     */
    public boolean hasAllPermissions() {
        boolean hasBasicPermissions = hasStoragePermission() && 
                                     hasNetworkPermissions() && 
                                     hasMediaPermissions() && 
                                     hasBluetoothPermissions();
        
        // Android 13+ requires notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasBasicPermissions = hasBasicPermissions && hasNotificationPermission();
        }
        
        return hasBasicPermissions;
    }

    /**
     * Get detailed permission status for Android 6-15
     */
    public JSONObject getPermissionStatus() throws JSONException {
        JSONObject status = new JSONObject();
        
        // Storage permissions
        JSONObject storagePermissions = new JSONObject();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            storagePermissions.put("readMediaImages", cordova.hasPermission(Manifest.permission.READ_MEDIA_IMAGES));
            storagePermissions.put("readMediaVideo", cordova.hasPermission(Manifest.permission.READ_MEDIA_VIDEO));
            storagePermissions.put("readMediaAudio", cordova.hasPermission(Manifest.permission.READ_MEDIA_AUDIO));
        } else {
            storagePermissions.put("readExternalStorage", cordova.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE));
            storagePermissions.put("writeExternalStorage", cordova.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE));
        }
        storagePermissions.put("allGranted", hasStoragePermission());
        status.put("storage", storagePermissions);
        
        // Network permissions
        JSONObject networkPermissions = new JSONObject();
        networkPermissions.put("networkState", cordova.hasPermission(Manifest.permission.ACCESS_NETWORK_STATE));
        networkPermissions.put("wifiState", cordova.hasPermission(Manifest.permission.ACCESS_WIFI_STATE));
        networkPermissions.put("fineLocation", cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));
        networkPermissions.put("coarseLocation", cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION));
        networkPermissions.put("allGranted", hasNetworkPermissions());
        
        status.put("network", networkPermissions);
        
        // Media permissions
        JSONObject mediaPermissions = new JSONObject();
        mediaPermissions.put("recordAudio", cordova.hasPermission(Manifest.permission.RECORD_AUDIO));
        mediaPermissions.put("camera", cordova.hasPermission(Manifest.permission.CAMERA));
        mediaPermissions.put("writeStorage", cordova.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE));
        mediaPermissions.put("allGranted", hasMediaPermissions());
        status.put("media", mediaPermissions);

        // Bluetooth permissions
        JSONObject bluetoothPermissions = new JSONObject();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissions.put("bluetoothConnect", cordova.hasPermission(Manifest.permission.BLUETOOTH_CONNECT));
            bluetoothPermissions.put("bluetoothScan", cordova.hasPermission(Manifest.permission.BLUETOOTH_SCAN));
            bluetoothPermissions.put("bluetoothAdvertise", cordova.hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE));
        } else {
            bluetoothPermissions.put("bluetooth", cordova.hasPermission(Manifest.permission.BLUETOOTH));
            bluetoothPermissions.put("bluetoothAdmin", cordova.hasPermission(Manifest.permission.BLUETOOTH_ADMIN));
        }
        bluetoothPermissions.put("fineLocation", cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));
        bluetoothPermissions.put("coarseLocation", cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION));
        bluetoothPermissions.put("allGranted", hasBluetoothPermissions());
        status.put("bluetooth", bluetoothPermissions);

        // Notification permissions (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            status.put("notifications", hasNotificationPermission());
        }
        
        status.put("allGranted", hasAllPermissions());
        status.put("androidVersion", Build.VERSION.SDK_INT);
        
        return status;
    }

	/**
	 * Request permissions based on action type - IMPROVED VERSION
	 */
	public void requestPermissions(String action, JSONObject args, CallbackContext callbackContext) {
		if (isDestroyed.get()) {
			sendError(callbackContext, "PermissionManager has been destroyed");
			return;
		}

		String permissionType = getPermissionTypeForAction(action);
		int requestId = permissionRequestId.getAndIncrement();
		
		PermissionRequest request = new PermissionRequest(callbackContext, action, args, permissionType);
		permissionQueue.put(requestId, request);

		try {
			String[] permissions = getRequiredPermissions(permissionType);
			
			Log.d(TAG, "Requesting permissions for action: " + action + 
				  ", type: " + permissionType + 
				  ", permissions: " + java.util.Arrays.toString(permissions));
			
			// Delegate permission request to the main plugin
			if (callback != null) {
				callback.requestPermissions(requestId, permissions);
			} else {
				Log.e(TAG, "Permission callback is null, cannot request permissions");
				sendError(callbackContext, "Permission system not available");
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Error requesting permissions", e);
			permissionQueue.remove(requestId);
			sendError(callbackContext, "Failed to request permissions: " + e.getMessage());
		}
	}

    /**
     * Request specific permission type
     */
    public void requestPermissionType(String permissionType, JSONObject args, CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PermissionManager has been destroyed");
            return;
        }

        int requestId = permissionRequestId.getAndIncrement();
        PermissionRequest request = new PermissionRequest(callbackContext, "requestPermission", args, permissionType);
        permissionQueue.put(requestId, request);

        try {
            String[] permissions = getRequiredPermissions(permissionType);
            
            if (callback != null) {
                callback.requestPermissions(requestId, permissions);
            }
            
            Log.d(TAG, "Requesting permissions for type: " + permissionType);
            
        } catch (Exception e) {
            Log.e(TAG, "Error requesting permissions", e);
            permissionQueue.remove(requestId);
            sendError(callbackContext, "Failed to request permissions: " + e.getMessage());
        }
    }

    /**
     * Handle permission request results
     */
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        PermissionRequest request = permissionQueue.remove(requestCode);
        
        if (request == null) {
            Log.w(TAG, "No permission request found for requestCode: " + requestCode);
            return;
        }

        // Clean up old requests
        cleanUpOldRequests();

        boolean allGranted = true;
        StringBuilder deniedPermissions = new StringBuilder();
        
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                if (deniedPermissions.length() > 0) {
                    deniedPermissions.append(", ");
                }
                deniedPermissions.append(permissions[i]);
            }
        }

        if (allGranted) {
            Log.i(TAG, "All permissions granted for action: " + request.action);
            if (callback != null) {
                callback.onPermissionsGranted(request.action, request.args, request.callback);
            }
        } else {
            Log.w(TAG, "Permissions denied for action: " + request.action + " - Denied: " + deniedPermissions.toString());
            if (callback != null) {
                callback.onPermissionsDenied(request.action, request.args, request.callback, 
                    "Permissions denied: " + deniedPermissions.toString());
            }
        }
    }

    /**
     * Execute action with permission check
     */
    public void executeWithPermissions(String action, JSONObject args, CallbackContext callbackContext, 
                                     Runnable executeAction) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PermissionManager has been destroyed");
            return;
        }

        if (hasRequiredPermissions(action)) {
            // Permissions already granted, execute immediately
            executeAction.run();
        } else {
            // Request permissions first
            requestPermissions(action, args, callbackContext);
        }
    }

    /**
     * Check if action has required permissions
     */
    public boolean hasRequiredPermissions(String action) {
        String permissionType = getPermissionTypeForAction(action);
        return hasPermissionType(permissionType);
    }

    /**
     * Get required permissions for media operations
     */
    public String[] getMediaPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            };
        } else {
            return new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
    }

    /**
     * Get required permissions for audio operations
     */
    public String[] getAudioPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_MEDIA_AUDIO
            };
        } else {
            return new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
    }

    /**
     * Get required permissions for camera operations
     */
    public String[] getCameraPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            };
        } else {
            return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
    }

    /**
     * Get required permissions for Bluetooth operations
     */
    public String[] getBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            return new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            };
        }
    }

    /**
     * Get required permissions for storage operations
     */
    public String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return new String[]{
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            };
        } else {
            return new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    // ==================== PRIVATE METHODS ====================

    private boolean hasPermissionType(String permissionType) {
        switch (permissionType) {
            case "STORAGE":
                return hasStoragePermission();
            case "NETWORK":
                return hasNetworkPermissions();
            case "MEDIA":
                return hasMediaPermissions();
            case "AUDIO":
                return hasAudioRecordingPermission() && hasStoragePermission();
            case "CAMERA":
                return hasCameraPermission();
            case "BLUETOOTH":
                return hasBluetoothPermissions();
            case "ALL":
                return hasAllPermissions();
            default:
                return false;
        }
    }

	/**
	 * Get permission type for action - IMPROVED VERSION
	 */
	private String getPermissionTypeForAction(String action) {
		if (action == null) {
			return "STORAGE";
		}
		
		switch (action) {
			case "getWallpaper":
			case "getWallpaperInfo":
			case "listenWallpaperChanged":
				return "STORAGE";
				
			case "startNetworkMonitoring":
			case "getNetworkStatus":
			case "getWifiStatus":
			case "getMobileStatus":
				return "NETWORK";
				
			case "captureAudio":
			case "createAudio":
			case "playAudio":
			case "pauseAudio":
			case "stopAudio":
			case "requestPermission":
				return "AUDIO";
				
			case "captureImage":
			case "captureVideo":
				return "CAMERA";
				
			case "getSupportedFormats":
				return "MEDIA";

            case "startBluetoothDiscovery":
            case "connectToBluetoothDevice":
            case "sendBluetoothData":
            case "receiveBluetoothData":
            case "enableBluetooth":
            case "disableBluetooth":
                return "BLUETOOTH";
				
			case "startAllMonitoring":
				return "ALL";
				
			default:
				Log.w(TAG, "Unknown action for permission mapping: " + action + ", defaulting to STORAGE");
				return "STORAGE";
		}
    }

    /**
     * Get required permissions for Android 6-15 compatibility
     */
    private String[] getRequiredPermissions(String permissionType) {
        switch (permissionType) {
            case "STORAGE":
                return getStoragePermissions();
                
            case "NETWORK":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return new String[] {
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    };
                } else {
                    return new String[] {
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_WIFI_STATE
                    };
                }
                
            case "MEDIA":
                return getMediaPermissions();
                
            case "AUDIO":
                return getAudioPermissions();
                
            case "CAMERA":
                return getCameraPermissions();

            case "BLUETOOTH":
                return getBluetoothPermissions();
                
            case "ALL":
                // Comprehensive permission set for Android 6-15
                java.util.List<String> allPermissions = new java.util.ArrayList<>();
                
                // Storage permissions based on Android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Android 14+
                    allPermissions.add(Manifest.permission.READ_MEDIA_IMAGES);
                    allPermissions.add(Manifest.permission.READ_MEDIA_VIDEO);
                    allPermissions.add(Manifest.permission.READ_MEDIA_AUDIO);
                } else {
                    // Android 6-13
                    allPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                    allPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                
                // Network permissions
                allPermissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
                allPermissions.add(Manifest.permission.ACCESS_WIFI_STATE);
                
                // Location permissions (required for network and Bluetooth)
                allPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
                allPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                
                // Media permissions
                allPermissions.add(Manifest.permission.RECORD_AUDIO);
                allPermissions.add(Manifest.permission.CAMERA);
                
                // Bluetooth permissions based on Android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+
                    allPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
                    allPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
                    allPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
                } else {
                    // Android 6-11
                    allPermissions.add(Manifest.permission.BLUETOOTH);
                    allPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
                }
                
                // Notification permission for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    allPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
                }
                
                return allPermissions.toArray(new String[0]);
                
            default:
                return getStoragePermissions();
        }
    }

    private void cleanUpOldRequests() {
        long currentTime = System.currentTimeMillis();
        permissionQueue.entrySet().removeIf(entry -> {
            PermissionRequest req = entry.getValue();
            boolean isOld = (currentTime - req.timestamp) > 5 * 60 * 1000; // 5 minutes
            if (isOld) {
                Log.w(TAG, "Cleaning up old permission request: " + req.action);
                sendError(req.callback, "Permission request timeout");
            }
            return isOld;
        });
    }

    private void sendError(CallbackContext cb, String message) {
        if (isDestroyed.get() || cb == null) return;
        
        cordova.getActivity().runOnUiThread(() -> {
            if (!isDestroyed.get() && cb != null) {
                try {
                    JSONObject error = new JSONObject();
                    error.put("error", message);
                    error.put("code", "PERMISSION_ERROR");
                    cb.error(error);
                } catch (JSONException e) {
                    cb.error("{\"error\":\"" + message.replace("\"", "\\\"") + "\",\"code\":\"PERMISSION_ERROR\"}");
                }
            }
        });
    }

    /**
     * Clean up resources
     */
    public void destroy() {
        if (isDestroyed.compareAndSet(false, true)) {
            permissionQueue.clear();
            Log.i(TAG, "PermissionManager destroyed successfully");
        }
    }
}