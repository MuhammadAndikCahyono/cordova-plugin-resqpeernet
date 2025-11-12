package org.apache.cordova.resqpeernet.modules;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.ArrayList;

public class AppDiscoveryManagerBridge {
    private static final String TAG = "AppDiscoveryManagerBridge";
    
    private PackageManager packageManager;
    private CordovaInterface cordova;
    private Context context;
    
    // Untuk real-time monitoring
    private BroadcastReceiver packageChangeReceiver;
    private boolean isMonitoring = false;
    private List<CallbackContext> appChangeCallbacks = new ArrayList<>();
    
    public AppDiscoveryManagerBridge(PackageManager pm, CordovaInterface cordova) {
        this.packageManager = pm;
        this.cordova = cordova;
        this.context = cordova.getActivity().getApplicationContext();
    }
    
    /**
     * Constructor alternatif tanpa CordovaInterface (untuk kompatibilitas)
     */
    public AppDiscoveryManagerBridge(PackageManager pm, Context context) {
        this.packageManager = pm;
        this.context = context;
    }
    
    /**
     * Method untuk mendapatkan daftar aplikasi terinstall
     */
    /*
    public void getInstalledApps(CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Getting installed apps list...");
            
            List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            JSONArray appList = new JSONArray();
            
            for (ApplicationInfo appInfo : apps) {
                try {
                    JSONObject appData = new JSONObject();
                    
                    // Nama aplikasi (label)
                    String appName = packageManager.getApplicationLabel(appInfo).toString();
                    appData.put("name", appName);
                    
                    // Package name
                    appData.put("packageName", appInfo.packageName);
                    
                    // Informasi tambahan
                    appData.put("isSystemApp", (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                    appData.put("isUpdatedSystemApp", (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
                    
                    // Coba mendapatkan icon (opsional)
                    try {
                        Drawable icon = packageManager.getApplicationIcon(appInfo);
                        String iconBase64 = drawableToBase64(icon);
                        appData.put("icon", iconBase64);
                        appData.put("hasIcon", true);
                    } catch (Exception iconError) {
                        Log.w(TAG, "Could not get icon for: " + appInfo.packageName);
                        appData.put("hasIcon", false);
                        appData.put("icon", "");
                    }
                    
                    appList.put(appData);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing app: " + appInfo.packageName, e);
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("apps", appList);
            result.put("count", appList.length());
            result.put("timestamp", System.currentTimeMillis());
            result.put("isRealTime", true);
            
            callbackContext.success(result);
            Log.d(TAG, "Retrieved " + appList.length() + " installed apps");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting installed apps", e);
            callbackContext.error("Error retrieving app list: " + e.getMessage());
        }
    }
    */
   
    public void getInstalledApps(boolean includeSystemApps, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Getting installed apps list - User Apps Only: " + !includeSystemApps);
            
            List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            JSONArray appList = new JSONArray();
            int totalApps = apps.size();
            int userAppCount = 0;
            int systemAppCount = 0;
            
            for (ApplicationInfo appInfo : apps) {
                try {
                    boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    boolean isUpdatedSystemApp = (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
                    
                    // Filter: hanya user apps jika includeSystemApps = false
                    if (!includeSystemApps && (isSystemApp || isUpdatedSystemApp)) {
                        systemAppCount++;
                        continue; // Skip system apps
                    }
                    
                    if (includeSystemApps) {
                        // Hitung semua apps
                        if (isSystemApp || isUpdatedSystemApp) {
                            systemAppCount++;
                        } else {
                            userAppCount++;
                        }
                    } else {
                        userAppCount++;
                    }
                    
                    JSONObject appData = new JSONObject();
                    
                    // Nama aplikasi (label)
                    String appName = packageManager.getApplicationLabel(appInfo).toString();
                    appData.put("name", appName);
                    
                    // Package name
                    appData.put("packageName", appInfo.packageName);
                    
                    // Informasi tambahan
                    appData.put("isSystemApp", isSystemApp);
                    appData.put("isUpdatedSystemApp", isUpdatedSystemApp);
                    appData.put("isUserApp", !isSystemApp && !isUpdatedSystemApp);
                    
                    // Coba mendapatkan icon (opsional)
                    try {
                        Drawable icon = packageManager.getApplicationIcon(appInfo);
                        String iconBase64 = drawableToBase64(icon);
                        appData.put("icon", iconBase64);
                        appData.put("hasIcon", true);
                    } catch (Exception iconError) {
                        Log.w(TAG, "Could not get icon for: " + appInfo.packageName);
                        appData.put("hasIcon", false);
                        appData.put("icon", "");
                    }
                    
                    // Coba mendapatkan info versi
                    try {
                        android.content.pm.PackageInfo pkgInfo = packageManager.getPackageInfo(appInfo.packageName, 0);
                        appData.put("versionName", pkgInfo.versionName != null ? pkgInfo.versionName : "unknown");
                        appData.put("versionCode", pkgInfo.versionCode);
                    } catch (Exception versionError) {
                        Log.w(TAG, "Could not get version info for: " + appInfo.packageName);
                        appData.put("versionName", "unknown");
                        appData.put("versionCode", 0);
                    }
                    
                    appList.put(appData);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error processing app: " + appInfo.packageName, e);
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("apps", appList);
            result.put("count", appList.length());
            result.put("totalInSystem", totalApps);
            result.put("userAppsCount", userAppCount);
            result.put("systemAppsCount", systemAppCount);
            result.put("isUserAppsOnly", !includeSystemApps);
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.d(TAG, "Retrieved " + appList.length() + " user apps (filtered from " + totalApps + " total apps)");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting installed apps", e);
            callbackContext.error("Error retrieving app list: " + e.getMessage());
        }
    }

    /**
     * Method untuk mendapatkan SEMUA aplikasi (termasuk sistem apps)
     */
    public void getAllInstalledApps(CallbackContext callbackContext) {
        getInstalledApps(true, callbackContext);
    }

    /**
     * Method untuk mendapatkan hanya SYSTEM APPS
     */
    public void getSystemApps(CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Getting system apps list...");
            
            List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            JSONArray appList = new JSONArray();
            int totalApps = apps.size();
            int systemAppCount = 0;
            
            for (ApplicationInfo appInfo : apps) {
                try {
                    boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    boolean isUpdatedSystemApp = (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
                    
                    // Filter: hanya system apps
                    if (!isSystemApp && !isUpdatedSystemApp) {
                        continue; // Skip user apps
                    }
                    
                    systemAppCount++;
                    
                    JSONObject appData = new JSONObject();
                    
                    // Nama aplikasi (label)
                    String appName = packageManager.getApplicationLabel(appInfo).toString();
                    appData.put("name", appName);
                    appData.put("packageName", appInfo.packageName);
                    appData.put("isSystemApp", isSystemApp);
                    appData.put("isUpdatedSystemApp", isUpdatedSystemApp);
                    appData.put("isUserApp", false);
                    
                    // Coba mendapatkan icon
                    try {
                        Drawable icon = packageManager.getApplicationIcon(appInfo);
                        String iconBase64 = drawableToBase64(icon);
                        appData.put("icon", iconBase64);
                        appData.put("hasIcon", true);
                    } catch (Exception iconError) {
                        appData.put("hasIcon", false);
                        appData.put("icon", "");
                    }
                    
                    // Info versi
                    try {
                        android.content.pm.PackageInfo pkgInfo = packageManager.getPackageInfo(appInfo.packageName, 0);
                        appData.put("versionName", pkgInfo.versionName != null ? pkgInfo.versionName : "unknown");
                        appData.put("versionCode", pkgInfo.versionCode);
                    } catch (Exception versionError) {
                        appData.put("versionName", "unknown");
                        appData.put("versionCode", 0);
                    }
                    
                    appList.put(appData);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error processing system app: " + appInfo.packageName, e);
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("apps", appList);
            result.put("count", appList.length());
            result.put("totalInSystem", totalApps);
            result.put("systemAppsCount", systemAppCount);
            result.put("isSystemAppsOnly", true);
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.d(TAG, "Retrieved " + appList.length() + " system apps");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting system apps", e);
            callbackContext.error("Error retrieving system apps: " + e.getMessage());
        }
    }

    /**
     * Method untuk mendapatkan info aplikasi spesifik
     */
    public void getAppInfo(String packageName, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Getting app info for: " + packageName);
            
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            JSONObject appData = new JSONObject();
            
            // Nama aplikasi
            String appName = packageManager.getApplicationLabel(appInfo).toString();
            appData.put("name", appName);
            
            // Package name
            appData.put("packageName", appInfo.packageName);
            
            // Informasi tambahan
            appData.put("isSystemApp", (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
            appData.put("isUpdatedSystemApp", (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
            
            // Coba mendapatkan icon
            try {
                Drawable icon = packageManager.getApplicationIcon(appInfo);
                String iconBase64 = drawableToBase64(icon);
                appData.put("icon", iconBase64);
                appData.put("hasIcon", true);
            } catch (Exception iconError) {
                Log.w(TAG, "Could not get icon for: " + packageName);
                appData.put("hasIcon", false);
                appData.put("icon", "");
            }
            
            // Coba mendapatkan info versi
            try {
                android.content.pm.PackageInfo pkgInfo = packageManager.getPackageInfo(packageName, 0);
                appData.put("versionName", pkgInfo.versionName);
                appData.put("versionCode", pkgInfo.versionCode);
            } catch (Exception versionError) {
                Log.w(TAG, "Could not get version info for: " + packageName);
                appData.put("versionName", "unknown");
                appData.put("versionCode", 0);
            }
            
            callbackContext.success(appData);
            Log.d(TAG, "App info retrieved for: " + packageName);
            
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "App not found: " + packageName);
            callbackContext.error("App not found: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error getting app info for: " + packageName, e);
            callbackContext.error("Error getting app info: " + e.getMessage());
        }
    }
    
    /**
     * Method untuk uninstall aplikasi
     */
    public void uninstallApp(String packageName, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Attempting to uninstall app: " + packageName);
            
            // Cek apakah aplikasi ada
            try {
                packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "App not found: " + packageName);
                callbackContext.error("App not found: " + packageName);
                return;
            }
            
            // Buat intent untuk uninstall
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // Start activity untuk uninstall
            if (context != null) {
                context.startActivity(intent);
            } else if (cordova != null && cordova.getActivity() != null) {
                cordova.getActivity().startActivity(intent);
            } else {
                callbackContext.error("No context available to start uninstall activity");
                return;
            }
            
            // Kirim response success (user masih perlu konfirmasi manual)
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("packageName", packageName);
            result.put("message", "Uninstall intent launched successfully");
            result.put("userActionRequired", true);
            
            callbackContext.success(result);
            Log.d(TAG, "Uninstall intent launched for: " + packageName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error uninstalling app: " + packageName, e);
            callbackContext.error("Error uninstalling app: " + e.getMessage());
        }
    }
    
    /**
     * Method untuk membuka aplikasi
     */
    public void launchApp(String packageName, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Launching app: " + packageName);
            
            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                if (context != null) {
                    context.startActivity(launchIntent);
                } else if (cordova != null && cordova.getActivity() != null) {
                    cordova.getActivity().startActivity(launchIntent);
                } else {
                    callbackContext.error("No context available to launch app");
                    return;
                }
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("packageName", packageName);
                result.put("message", "App launched successfully");
                
                callbackContext.success(result);
                Log.d(TAG, "App launched: " + packageName);
            } else {
                Log.e(TAG, "No launch intent found for: " + packageName);
                callbackContext.error("App cannot be launched or no main activity found");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error launching app: " + packageName, e);
            callbackContext.error("Error launching app: " + e.getMessage());
        }
    }
    
    /**
     * Method untuk membuka di Play Store
     */
    public void openAppInPlayStore(String packageName, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Opening app in Play Store: " + packageName);
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (context != null) {
                context.startActivity(intent);
            } else if (cordova != null && cordova.getActivity() != null) {
                cordova.getActivity().startActivity(intent);
            } else {
                callbackContext.error("No context available to open Play Store");
                return;
            }
            
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("packageName", packageName);
            result.put("message", "Play Store intent launched");
            
            callbackContext.success(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening Play Store for: " + packageName, e);
            
            // Fallback ke browser web
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                if (context != null) {
                    context.startActivity(intent);
                } else if (cordova != null && cordova.getActivity() != null) {
                    cordova.getActivity().startActivity(intent);
                }
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("packageName", packageName);
                result.put("message", "Web Play Store launched as fallback");
                
                callbackContext.success(result);
            } catch (Exception ex) {
                callbackContext.error("Error opening app in Play Store: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Method untuk mengecek apakah aplikasi terinstall
     */
    public void isAppInstalled(String packageName, CallbackContext callbackContext) {
        try {
            boolean installed = false;
            String appName = "";
            
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                installed = true;
                appName = packageManager.getApplicationLabel(appInfo).toString();
            } catch (PackageManager.NameNotFoundException e) {
                installed = false;
            }
            
            JSONObject result = new JSONObject();
            result.put("installed", installed);
            result.put("packageName", packageName);
            result.put("appName", appName);
            
            callbackContext.success(result);
            Log.d(TAG, "App installed check: " + packageName + " = " + installed);
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking app installation: " + packageName, e);
            callbackContext.error("Error checking app installation: " + e.getMessage());
        }
    }
    
    /**
     * Method untuk memulai monitoring
     */
    public void startAppMonitoring(CallbackContext callbackContext) {
        try {
            if (isMonitoring) {
                Log.w(TAG, "App monitoring already started");
                callbackContext.success("App monitoring already active");
                return;
            }
            
            setupPackageChangeReceiver();
            
            // Simpan callback untuk mengirim update
            appChangeCallbacks.add(callbackContext);
            isMonitoring = true;
            
            Log.i(TAG, "App monitoring started successfully");
            
            // Kirim status awal
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, createStatusMessage("monitoring_started", "App monitoring started"));
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting app monitoring", e);
            callbackContext.error("Error starting app monitoring: " + e.getMessage());
        }
    }
    
    /**
     * Method untuk menghentikan monitoring
     */
    public void stopAppMonitoring(CallbackContext callbackContext) {
        try {
            if (!isMonitoring) {
                callbackContext.success("App monitoring not active");
                return;
            }
            
            if (packageChangeReceiver != null && context != null) {
                try {
                    context.unregisterReceiver(packageChangeReceiver);
                } catch (Exception e) {
                    Log.w(TAG, "Error unregistering receiver: " + e.getMessage());
                }
                packageChangeReceiver = null;
            }
            
            appChangeCallbacks.clear();
            isMonitoring = false;
            
            callbackContext.success("App monitoring stopped");
            Log.i(TAG, "App monitoring stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping app monitoring", e);
            callbackContext.error("Error stopping app monitoring: " + e.getMessage());
        }
    }
    
    /**
     * Method untuk mendapatkan status monitoring
     */
    public void getMonitoringStatus(CallbackContext callbackContext) {
        try {
            JSONObject status = new JSONObject();
            status.put("isMonitoring", isMonitoring);
            status.put("activeCallbacks", appChangeCallbacks.size());
            status.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(status);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting monitoring status", e);
            callbackContext.error("Error getting monitoring status: " + e.getMessage());
        }
    }
    
    /**
     * Setup BroadcastReceiver untuk monitoring
     */
    private void setupPackageChangeReceiver() {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot setup package change receiver");
            return;
        }
        
        packageChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Uri data = intent.getData();
                
                if (data == null) {
                    return;
                }
                
                String packageName = data.getSchemeSpecificPart();
                
                Log.d(TAG, "Package change detected - Action: " + action + ", Package: " + packageName);
                
                if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                    handlePackageAdded(packageName, intent);
                } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                    handlePackageRemoved(packageName, intent);
                } else if (Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
                    handlePackageReplaced(packageName, intent);
                }
            }
        };
        
        // Daftarkan receiver untuk berbagai event package
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        
        context.registerReceiver(packageChangeReceiver, filter);
        Log.d(TAG, "Package change receiver registered");
    }
    
    /**
     * Handle package added
     */
    private void handlePackageAdded(String packageName, Intent intent) {
        try {
            boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            
            if (!isReplacing) { // Hanya untuk install baru, bukan update
                JSONObject eventData = new JSONObject();
                eventData.put("eventType", "app_installed");
                eventData.put("packageName", packageName);
                eventData.put("timestamp", System.currentTimeMillis());
                
                sendUpdateToCallbacks(eventData);
                Log.i(TAG, "New app installed: " + packageName);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling package added", e);
        }
    }
    
    /**
     * Handle package removed
     */
    private void handlePackageRemoved(String packageName, Intent intent) {
        try {
            boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            
            if (!isReplacing) { // Hanya untuk uninstall, bukan update
                JSONObject eventData = new JSONObject();
                eventData.put("eventType", "app_uninstalled");
                eventData.put("packageName", packageName);
                eventData.put("timestamp", System.currentTimeMillis());
                
                sendUpdateToCallbacks(eventData);
                Log.i(TAG, "App uninstalled: " + packageName);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling package removed", e);
        }
    }
    
    /**
     * Handle package replaced
     */
    private void handlePackageReplaced(String packageName, Intent intent) {
        try {
            JSONObject eventData = new JSONObject();
            eventData.put("eventType", "app_updated");
            eventData.put("packageName", packageName);
            eventData.put("timestamp", System.currentTimeMillis());
            
            sendUpdateToCallbacks(eventData);
            Log.i(TAG, "App updated: " + packageName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling package replaced", e);
        }
    }
    
    /**
     * Kirim update ke callbacks
     */
    private void sendUpdateToCallbacks(JSONObject eventData) {
        for (CallbackContext callbackContext : appChangeCallbacks) {
            if (callbackContext != null) {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, eventData);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        }
    }
    
    /**
     * Convert drawable to Base64
     */
    private String drawableToBase64(Drawable drawable) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888
            );
            
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting drawable to Base64", e);
            return "";
        }
    }
    
    /**
     * Create status message
     */
    private JSONObject createStatusMessage(String type, String message) {
        try {
            JSONObject status = new JSONObject();
            status.put("type", type);
            status.put("message", message);
            status.put("timestamp", System.currentTimeMillis());
            return status;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
    
    /**
     * Cleanup resources
     */
    public void destroy() {
        // Cleanup monitoring
        if (isMonitoring && packageChangeReceiver != null && context != null) {
            try {
                context.unregisterReceiver(packageChangeReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
        }
        
        appChangeCallbacks.clear();
        packageManager = null;
        cordova = null;
        context = null;
    }
}