package org.apache.cordova.resqpeernet.modules;

// =========================================================================
// EXISTING IMPORTS
// =========================================================================
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

// =========================================================================
// NEW IMPORTS FOR SYSTEM MONITORING - PHASE 1
// =========================================================================
import android.app.ActivityManager;
import android.app.usage.StorageStatsManager;
import android.app.usage.UsageStatsManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.lang.reflect.Method;

public class DeviceManagerBridge {
    private static final String TAG = "DeviceManagerBridge";
    private Context context;
    private WindowManager windowManager;
    
    // =========================================================================
    // EXISTING MONITORING VARIABLES
    // =========================================================================
    // Battery monitoring
    private CallbackContext batteryCallbackContext;
    private BatteryReceiver batteryReceiver;
    private boolean isBatteryListening = false;
    
    // Network monitoring
    private ConnectivityManager connectivityManager;
    private CallbackContext networkCallbackContext;
    private NetworkReceiver networkReceiver;
    private boolean isNetworkListening = false;
    
    // =========================================================================
    // NEW VARIABLES FOR SYSTEM MONITORING - PHASE 1
    // =========================================================================
    private SensorManager sensorManager;
    private ActivityManager activityManager;
    private StorageManager storageManager;
    private UsageStatsManager usageStatsManager;

    // =========================================================================
    // BATTERY RECEIVER CLASS
    // =========================================================================
    private class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (batteryCallbackContext != null) {
                try {
                    JSONObject batteryInfo = getCurrentBatteryInfo(intent);
                    PluginResult result = new PluginResult(PluginResult.Status.OK, batteryInfo);
                    result.setKeepCallback(true);
                    batteryCallbackContext.sendPluginResult(result);
                } catch (JSONException e) {
                    android.util.Log.e(TAG, "Error sending battery update", e);
                }
            }
        }
    }

    // =========================================================================
    // NETWORK RECEIVER CLASS
    // =========================================================================
    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (networkCallbackContext != null) {
                try {
                    JSONObject networkInfo = getCurrentNetworkInfo();
                    PluginResult result = new PluginResult(PluginResult.Status.OK, networkInfo);
                    result.setKeepCallback(true);
                    networkCallbackContext.sendPluginResult(result);
                } catch (JSONException e) {
                    android.util.Log.e(TAG, "Error sending network update", e);
                }
            }
        }
    }

    public DeviceManagerBridge(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        // =========================================================================
        // INITIALIZE NEW SYSTEM SERVICES - PHASE 1
        // =========================================================================
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        }
        
        android.util.Log.d(TAG, "DeviceManagerBridge initialized with System Monitoring features");
    }

    // =========================================================================
    // EXISTING METHODS THAT WERE MISSING
    // =========================================================================

    /**
     * Get current battery information
     */
    private JSONObject getCurrentBatteryInfo() throws JSONException {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return getCurrentBatteryInfo(batteryIntent);
    }

    /**
     * Get current battery information from intent
     */
    private JSONObject getCurrentBatteryInfo(Intent batteryIntent) throws JSONException {
        JSONObject batteryInfo = new JSONObject();
        
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            int temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            int voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            
            float batteryPct = (level / (float) scale) * 100;
            
            batteryInfo.put("level", (int) batteryPct);
            batteryInfo.put("isCharging", status == BatteryManager.BATTERY_STATUS_CHARGING);
            batteryInfo.put("temperature", temperature / 10.0); // Convert to Celsius
            
            // Health status
            String healthStatus = "unknown";
            switch (health) {
                case BatteryManager.BATTERY_HEALTH_GOOD:
                    healthStatus = "good";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                    healthStatus = "overheat";
                    break;
                case BatteryManager.BATTERY_HEALTH_DEAD:
                    healthStatus = "dead";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                    healthStatus = "over_voltage";
                    break;
                case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                    healthStatus = "failure";
                    break;
                case BatteryManager.BATTERY_HEALTH_COLD:
                    healthStatus = "cold";
                    break;
            }
            batteryInfo.put("health", healthStatus);
            batteryInfo.put("voltage", voltage);
        }
        
        return batteryInfo;
    }

    /**
     * Get current network information
     */
    private JSONObject getCurrentNetworkInfo() throws JSONException {
        JSONObject networkInfo = new JSONObject();
        
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            networkInfo.put("isConnected", isConnected);
            
            if (isConnected) {
                String type = "unknown";
                switch (activeNetwork.getType()) {
                    case ConnectivityManager.TYPE_WIFI:
                        type = "wifi";
                        break;
                    case ConnectivityManager.TYPE_MOBILE:
                        type = "cellular";
                        break;
                    case ConnectivityManager.TYPE_ETHERNET:
                        type = "ethernet";
                        break;
                    case ConnectivityManager.TYPE_BLUETOOTH:
                        type = "bluetooth";
                        break;
                }
                networkInfo.put("type", type);
                networkInfo.put("hasInternet", true); // Simplified check
            } else {
                networkInfo.put("type", "none");
                networkInfo.put("hasInternet", false);
            }
        }
        
        return networkInfo;
    }

    /**
     * Check if device is running on emulator
     */
    private boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    /**
     * Get basic device information
     */
    public void getDeviceInfo(CallbackContext callbackContext) {
        try {
            JSONObject deviceInfo = new JSONObject();
            
            // Basic device info
            deviceInfo.put("brand", Build.BRAND);
			deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("model", Build.MODEL);
            deviceInfo.put("product", Build.PRODUCT);
            deviceInfo.put("device", Build.DEVICE);
            deviceInfo.put("board", Build.BOARD);
            deviceInfo.put("hardware", Build.HARDWARE);
            deviceInfo.put("fingerprint", Build.FINGERPRINT);
            deviceInfo.put("type", Build.TYPE);
            deviceInfo.put("host", Build.HOST);
            deviceInfo.put("id", Build.ID);
            deviceInfo.put("user", Build.USER);       
            deviceInfo.put("bootloder", Build.BOOTLOADER);            
            deviceInfo.put("androidVersion", Build.VERSION.RELEASE);
            deviceInfo.put("sdkVersion", Build.VERSION.SDK_INT);

			deviceInfo.put("version_release", Build.VERSION.RELEASE);
			deviceInfo.put("version_sdk_int", Build.VERSION.SDK_INT);
			deviceInfo.put("version_codename", Build.VERSION.CODENAME);
			deviceInfo.put("version_incremental", Build.VERSION.INCREMENTAL);

            // Display info
            Display display = windowManager.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            
            JSONObject displayInfo = new JSONObject();
            displayInfo.put("width", metrics.widthPixels);
            displayInfo.put("height", metrics.heightPixels);
            displayInfo.put("density", metrics.density);
            displayInfo.put("densityDpi", metrics.densityDpi);
            deviceInfo.put("display", displayInfo);
            
            // Locale and timezone
			//Locale defaultLocale = Locale.getDefault();
            deviceInfo.put("language", Locale.getDefault().getLanguage());
            deviceInfo.put("country", Locale.getDefault().getCountry());
            deviceInfo.put("timezone", TimeZone.getDefault().getID());
			deviceInfo.put("displayName", Locale.getDefault().getDisplayName());
            deviceInfo.put("languageTag", Locale.getDefault().toLanguageTag());
            
            // Emulator detection
            deviceInfo.put("isEmulator", isEmulator());
            
            callbackContext.success(deviceInfo);
            
        } catch (JSONException e) {
            android.util.Log.e(TAG, "Error getting device info", e);
            callbackContext.error("Error getting device info: " + e.getMessage());
        }
    }

    /**
     * Get current battery status
     */
    public void getBatteryStatus(CallbackContext callbackContext) {
        try {
            JSONObject batteryInfo = getCurrentBatteryInfo();
            callbackContext.success(batteryInfo);
        } catch (JSONException e) {
            android.util.Log.e(TAG, "Error getting battery status", e);
            callbackContext.error("Error getting battery status: " + e.getMessage());
        }
    }

    /**
     * Start battery level listener
     */
    public void startBatteryListener(CallbackContext callbackContext) {
        try {
            if (isBatteryListening) {
                callbackContext.error("Battery listener already started");
                return;
            }
            
            this.batteryCallbackContext = callbackContext;
            this.batteryReceiver = new BatteryReceiver();
            
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            context.registerReceiver(batteryReceiver, filter);
            
            isBatteryListening = true;
            
            // Send initial battery status
            JSONObject batteryInfo = getCurrentBatteryInfo();
            PluginResult result = new PluginResult(PluginResult.Status.OK, batteryInfo);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            
            android.util.Log.d(TAG, "Battery listener started");
            
        } catch (JSONException e) {
            android.util.Log.e(TAG, "Error starting battery listener", e);
            callbackContext.error("Error starting battery listener: " + e.getMessage());
        }
    }

    /**
     * Stop battery level listener
     */
    public void stopBatteryListener(CallbackContext callbackContext) {
        try {
            if (!isBatteryListening || batteryReceiver == null) {
                callbackContext.error("Battery listener not running");
                return;
            }
            
            context.unregisterReceiver(batteryReceiver);
            batteryReceiver = null;
            batteryCallbackContext = null;
            isBatteryListening = false;
            
            callbackContext.success("Battery listener stopped");
            android.util.Log.d(TAG, "Battery listener stopped");
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error stopping battery listener", e);
            callbackContext.error("Error stopping battery listener: " + e.getMessage());
        }
    }

    // =========================================================================
    // PHASE 1: SYSTEM RESOURCE MONITORING METHODS
    // =========================================================================

    /**
     * Get comprehensive system resources information
     * Includes: Memory, CPU, Storage, Thermal status
     */
    public void getSystemResources(CallbackContext callbackContext) {
        try {
            android.util.Log.d(TAG, "Getting system resources information");
            
            JSONObject resources = new JSONObject();
            
            // Memory usage information
            resources.put("memory", getMemoryInfo());
            
            // CPU usage and information
            resources.put("cpu", getCPUInfo());
            
            // Storage information
            resources.put("storage", getStorageInfo());
            
            // Thermal status (if available)
            resources.put("thermal", getThermalInfo());
            
            // System load and performance
            resources.put("performance", getPerformanceInfo());
            
            // Timestamp for tracking
            resources.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(resources);
            android.util.Log.d(TAG, "System resources data sent successfully");
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error getting system resources", e);
            callbackContext.error("Error getting system resources: " + e.getMessage());
        }
    }

    /**
     * Get detailed memory information including usage, available, and status
     */
    private JSONObject getMemoryInfo() throws JSONException {
        JSONObject memory = new JSONObject();
        
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (activityManager != null) {
                activityManager.getMemoryInfo(mi);
                
                // Convert bytes to MB for readability
                long totalMB = mi.totalMem / (1024 * 1024);
                long availableMB = mi.availMem / (1024 * 1024);
                long usedMB = totalMB - availableMB;
                int usagePercentage = (int) ((usedMB * 100) / totalMB);
                
                memory.put("totalMB", totalMB);
                memory.put("availableMB", availableMB);
                memory.put("usedMB", usedMB);
                memory.put("usagePercentage", usagePercentage);
                memory.put("isLowMemory", mi.lowMemory);
                memory.put("thresholdMB", mi.threshold / (1024 * 1024));
                
                // Get additional memory stats from /proc/meminfo
                memory.put("detailed", getDetailedMemoryInfo());
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error getting memory info", e);
            memory.put("error", "Unable to retrieve memory information");
        }
        
        return memory;
    }

    /**
     * Get detailed memory information from /proc/meminfo
     * This provides more granular memory statistics
     */
    private JSONObject getDetailedMemoryInfo() throws JSONException {
        JSONObject detailed = new JSONObject();
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"));
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    detailed.put("memTotal", line.replace("MemTotal:", "").trim());
                } else if (line.startsWith("MemFree:")) {
                    detailed.put("memFree", line.replace("MemFree:", "").trim());
                } else if (line.startsWith("MemAvailable:")) {
                    detailed.put("memAvailable", line.replace("MemAvailable:", "").trim());
                } else if (line.startsWith("Buffers:")) {
                    detailed.put("buffers", line.replace("Buffers:", "").trim());
                } else if (line.startsWith("Cached:")) {
                    detailed.put("cached", line.replace("Cached:", "").trim());
                } else if (line.startsWith("SwapTotal:")) {
                    detailed.put("swapTotal", line.replace("SwapTotal:", "").trim());
                } else if (line.startsWith("SwapFree:")) {
                    detailed.put("swapFree", line.replace("SwapFree:", "").trim());
                }
            }
            reader.close();
        } catch (IOException e) {
            android.util.Log.w(TAG, "Cannot read /proc/meminfo", e);
            detailed.put("error", "Cannot read detailed memory info");
        }
        
        return detailed;
    }

    /**
     * Get CPU information and usage statistics
     */
    private JSONObject getCPUInfo() throws JSONException {
        JSONObject cpu = new JSONObject();
        
        try {
            // Basic CPU information
            cpu.put("architecture", System.getProperty("os.arch"));
            cpu.put("cores", Runtime.getRuntime().availableProcessors());
            cpu.put("maxFrequency", getCPUMaxFrequency());
            
            // CPU usage statistics
            JSONObject usage = getCPUUsage();
            cpu.put("usage", usage);
            
            // CPU load averages (if available)
            cpu.put("loadAverage", getCPULoadAverage());
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error getting CPU info", e);
            cpu.put("error", "Unable to retrieve CPU information");
        }
        
        return cpu;
    }

    /**
     * Get CPU usage statistics from /proc/stat
     */
    private JSONObject getCPUUsage() throws JSONException {
        JSONObject usage = new JSONObject();
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
            String line = reader.readLine();
            reader.close();
            
            if (line != null && line.startsWith("cpu ")) {
                String[] parts = line.split("\\s+");
                
                // CPU time components
                long user = Long.parseLong(parts[1]);
                long nice = Long.parseLong(parts[2]);
                long system = Long.parseLong(parts[3]);
                long idle = Long.parseLong(parts[4]);
                long iowait = Long.parseLong(parts[5]);
                long irq = Long.parseLong(parts[6]);
                long softirq = Long.parseLong(parts[7]);
                
                long total = user + nice + system + idle + iowait + irq + softirq;
                long used = total - idle;
                
                int usagePercentage = total > 0 ? (int) ((used * 100) / total) : 0;
                
                usage.put("usagePercentage", usagePercentage);
                usage.put("userTime", user);
                usage.put("systemTime", system);
                usage.put("idleTime", idle);
                usage.put("totalTime", total);
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Cannot read CPU usage", e);
            usage.put("error", "Cannot read CPU usage");
        }
        
        return usage;
    }

    /**
     * Get CPU maximum frequency
     */
    private String getCPUMaxFrequency() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"));
            String frequency = reader.readLine();
            reader.close();
            
            if (frequency != null) {
                int freqKHz = Integer.parseInt(frequency.trim());
                return (freqKHz / 1000) + " MHz";
            }
        } catch (Exception e) {
            // Ignore - not all devices expose this information
        }
        return "Unknown";
    }

    /**
     * Get CPU load averages from /proc/loadavg
     */
    private JSONObject getCPULoadAverage() throws JSONException {
        JSONObject loadAvg = new JSONObject();
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/loadavg"));
            String line = reader.readLine();
            reader.close();
            
            if (line != null) {
                String[] parts = line.split("\\s+");
                loadAvg.put("oneMinute", parts[0]);
                loadAvg.put("fiveMinutes", parts[1]);
                loadAvg.put("fifteenMinutes", parts[2]);
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Cannot read load average", e);
            loadAvg.put("error", "Cannot read load average");
        }
        
        return loadAvg;
    }

    /**
     * Get comprehensive storage information
     */
    private JSONObject getStorageInfo() throws JSONException {
        JSONObject storage = new JSONObject();
        
        try {
            // Internal storage
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            
            long totalInternal = (totalBlocks * blockSize) / (1024 * 1024); // MB
            long availableInternal = (availableBlocks * blockSize) / (1024 * 1024); // MB
            long usedInternal = totalInternal - availableInternal;
            int usagePercentage = totalInternal > 0 ? (int) ((usedInternal * 100) / totalInternal) : 0;
            
            JSONObject internal = new JSONObject();
            internal.put("totalMB", totalInternal);
            internal.put("availableMB", availableInternal);
            internal.put("usedMB", usedInternal);
            internal.put("usagePercentage", usagePercentage);
            internal.put("path", Environment.getDataDirectory().getPath());
            
            storage.put("internal", internal);
            
            // External storage (SD card) if available
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                StatFs externalStat = new StatFs(Environment.getExternalStorageDirectory().getPath());
                long externalBlockSize = externalStat.getBlockSizeLong();
                long externalTotalBlocks = externalStat.getBlockCountLong();
                long externalAvailableBlocks = externalStat.getAvailableBlocksLong();
                
                long totalExternal = (externalTotalBlocks * externalBlockSize) / (1024 * 1024);
                long availableExternal = (externalAvailableBlocks * externalBlockSize) / (1024 * 1024);
                
                JSONObject external = new JSONObject();
                external.put("totalMB", totalExternal);
                external.put("availableMB", availableExternal);
                external.put("usedMB", totalExternal - availableExternal);
                external.put("path", Environment.getExternalStorageDirectory().getPath());
                external.put("isMounted", true);
                
                storage.put("external", external);
            } else {
                JSONObject external = new JSONObject();
                external.put("isMounted", false);
                storage.put("external", external);
            }
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error getting storage info", e);
            storage.put("error", "Unable to retrieve storage information");
        }
        
        return storage;
    }

    /**
     * Get thermal status information
     */
    private JSONObject getThermalInfo() throws JSONException {
        JSONObject thermal = new JSONObject();
        
        try {
            // Basic thermal information
            thermal.put("batteryTemperature", getCurrentBatteryInfo().getDouble("temperature"));
            
            // Additional thermal data from /sys/class/thermal if available
            thermal.put("thermalZones", getThermalZones());
            
        } catch (Exception e) {
            android.util.Log.w(TAG, "Error getting thermal info", e);
            thermal.put("error", "Thermal information not available");
        }
        
        return thermal;
    }

    /**
     * Get thermal zones information
     */
    private JSONArray getThermalZones() throws JSONException {
        JSONArray zones = new JSONArray();
        
        try {
            // This is a simplified implementation
            // Real implementation would read from /sys/class/thermal/thermal_zone*/
            JSONObject zone = new JSONObject();
            zone.put("type", "battery");
            zone.put("temperature", getCurrentBatteryInfo().getDouble("temperature"));
            zones.put(zone);
            
        } catch (Exception e) {
            // Ignore - thermal zones not available on all devices
        }
        
        return zones;
    }

    /**
     * Get system performance information
     */
    private JSONObject getPerformanceInfo() throws JSONException {
        JSONObject performance = new JSONObject();
        
        try {
            // System uptime
            performance.put("uptime", getSystemUptime());
            
            // Available RAM
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024); // MB
            long totalMemory = runtime.totalMemory() / (1024 * 1024); // MB
            long freeMemory = runtime.freeMemory() / (1024 * 1024); // MB
            long usedMemory = totalMemory - freeMemory;
            
            performance.put("maxHeapMB", maxMemory);
            performance.put("totalHeapMB", totalMemory);
            performance.put("freeHeapMB", freeMemory);
            performance.put("usedHeapMB", usedMemory);
            
            // Garbage collector info
            performance.put("gcStats", getGCStats());
            
        } catch (Exception e) {
            android.util.Log.w(TAG, "Error getting performance info", e);
            performance.put("error", "Performance information not available");
        }
        
        return performance;
    }

    /**
     * Get system uptime in human readable format
     */
    private String getSystemUptime() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/uptime"));
            String uptime = reader.readLine();
            reader.close();
            
            if (uptime != null) {
                String[] parts = uptime.split(" ");
                double seconds = Double.parseDouble(parts[0]);
                
                // Convert to readable format
                int hours = (int) (seconds / 3600);
                int minutes = (int) ((seconds % 3600) / 60);
                
                return hours + "h " + minutes + "m";
            }
        } catch (Exception e) {
            // Ignore error
        }
        return "Unknown";
    }

    /**
     * Get garbage collector statistics
     */
    private JSONObject getGCStats() throws JSONException {
        JSONObject gcStats = new JSONObject();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            gcStats.put("totalMemory", runtime.totalMemory());
            gcStats.put("freeMemory", runtime.freeMemory());
            gcStats.put("maxMemory", runtime.maxMemory());
            
            // Suggest GC if memory is low
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryThreshold = runtime.maxMemory() * 80 / 100; // 80% threshold
            
            if (usedMemory > memoryThreshold) {
                gcStats.put("gcRecommended", true);
                gcStats.put("memoryPressure", "high");
            } else {
                gcStats.put("gcRecommended", false);
                gcStats.put("memoryPressure", "normal");
            }
            
        } catch (Exception e) {
            gcStats.put("error", "GC stats not available");
        }
        
        return gcStats;
    }

    // =========================================================================
    // PHASE 1: SENSOR INTEGRATION METHODS
    // =========================================================================

    /**
     * Get list of all available sensors on the device
     */
    public void getAvailableSensors(CallbackContext callbackContext) {
        try {
            android.util.Log.d(TAG, "Getting available sensors list");
            
            if (sensorManager == null) {
                callbackContext.error("Sensor service not available");
                return;
            }
            
            List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
            JSONArray sensorList = new JSONArray();
            
            for (Sensor sensor : sensors) {
                JSONObject sensorInfo = new JSONObject();
                sensorInfo.put("name", sensor.getName());
                sensorInfo.put("type", sensor.getType());
                sensorInfo.put("vendor", sensor.getVendor());
                sensorInfo.put("version", sensor.getVersion());
                sensorInfo.put("power", sensor.getPower());
                sensorInfo.put("resolution", sensor.getResolution());
                sensorInfo.put("maxRange", sensor.getMaximumRange());
                
                sensorList.put(sensorInfo);
            }
            
            JSONObject result = new JSONObject();
            result.put("sensors", sensorList);
            result.put("totalSensors", sensors.size());
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            android.util.Log.d(TAG, "Available sensors data sent successfully. Total: " + sensors.size());
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error getting available sensors", e);
            callbackContext.error("Error getting available sensors: " + e.getMessage());
        }
    }

    /**
     * Get detailed information about specific sensor types
     */
    public void getSensorCapabilities(CallbackContext callbackContext) {
        try {
            android.util.Log.d(TAG, "Getting sensor capabilities");
            
            JSONObject capabilities = new JSONObject();
            
            // Check for common important sensors
            capabilities.put("accelerometer", sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null);
            capabilities.put("gyroscope", sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null);
            capabilities.put("magnetometer", sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null);
            capabilities.put("light", sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null);
            capabilities.put("proximity", sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null);
            capabilities.put("pressure", sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null);
            capabilities.put("humidity", sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null);
            capabilities.put("ambientTemp", sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null);
            capabilities.put("stepCounter", sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null);
            capabilities.put("heartRate", sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null);
            
            // Motion sensors
            capabilities.put("gravity", sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null);
            capabilities.put("linearAcceleration", sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null);
            capabilities.put("rotationVector", sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null);
            
            // Additional info
            capabilities.put("timestamp", System.currentTimeMillis());
            capabilities.put("sensorServiceAvailable", sensorManager != null);
            
            callbackContext.success(capabilities);
            android.util.Log.d(TAG, "Sensor capabilities data sent successfully");
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error getting sensor capabilities", e);
            callbackContext.error("Error getting sensor capabilities: " + e.getMessage());
        }
    }

    // =========================================================================
    // PHASE 1: DEVICE HEALTH DIAGNOSTICS METHODS
    // =========================================================================

    /**
     * Run comprehensive device health diagnostics
     */
    public void runDeviceDiagnostics(CallbackContext callbackContext) {
        try {
            android.util.Log.d(TAG, "Running device health diagnostics");
            
            JSONObject diagnostics = new JSONObject();
            
            // Battery health
            diagnostics.put("battery", getBatteryHealth());
            
            // Storage health
            diagnostics.put("storage", getStorageHealth());
            
            // Memory health
            diagnostics.put("memory", getMemoryHealth());
            
            // Network health
            diagnostics.put("network", getNetworkHealth());
            
            // System health
            diagnostics.put("system", getSystemHealth());
            
            // Overall health score
            diagnostics.put("healthScore", calculateHealthScore(diagnostics));
            diagnostics.put("timestamp", System.currentTimeMillis());
            diagnostics.put("diagnosticsRun", true);
            
            callbackContext.success(diagnostics);
            android.util.Log.d(TAG, "Device diagnostics completed successfully");
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error running device diagnostics", e);
            callbackContext.error("Error running device diagnostics: " + e.getMessage());
        }
    }

    /**
     * Get battery health information
     */
    private JSONObject getBatteryHealth() throws JSONException {
        JSONObject batteryHealth = new JSONObject();
        
        try {
            JSONObject batteryInfo = getCurrentBatteryInfo();
            
            int level = batteryInfo.getInt("level");
            String health = batteryInfo.getString("health");
            double temperature = batteryInfo.getDouble("temperature");
            boolean isCharging = batteryInfo.getBoolean("isCharging");
            
            batteryHealth.put("level", level);
            batteryHealth.put("healthStatus", health);
            batteryHealth.put("temperature", temperature);
            batteryHealth.put("isCharging", isCharging);
            
            // Health assessment
            if (level <= 15 && !isCharging) {
                batteryHealth.put("status", "critical");
                batteryHealth.put("message", "Battery level very low");
            } else if (temperature > 40) {
                batteryHealth.put("status", "warning");
                batteryHealth.put("message", "Battery temperature high");
            } else if ("poor".equals(health) || "dead".equals(health)) {
                batteryHealth.put("status", "warning");
                batteryHealth.put("message", "Battery health degraded");
            } else {
                batteryHealth.put("status", "good");
                batteryHealth.put("message", "Battery health normal");
            }
            
            batteryHealth.put("score", calculateBatteryHealthScore(level, health, temperature));
            
        } catch (Exception e) {
            batteryHealth.put("status", "unknown");
            batteryHealth.put("message", "Unable to assess battery health");
            batteryHealth.put("score", 0);
        }
        
        return batteryHealth;
    }

    /**
     * Calculate battery health score (0-100)
     */
    private int calculateBatteryHealthScore(int level, String health, double temperature) {
        int score = 100;
        
        // Deduct based on level
        if (level <= 20) score -= 30;
        else if (level <= 50) score -= 10;
        
        // Deduct based on health status
        if ("poor".equals(health)) score -= 40;
        else if ("overheat".equals(health)) score -= 30;
        else if ("cold".equals(health)) score -= 20;
        else if ("dead".equals(health)) score -= 50;
        
        // Deduct based on temperature
        if (temperature > 45) score -= 25;
        else if (temperature > 40) score -= 15;
        else if (temperature < 10) score -= 10;
        
        return Math.max(0, score);
    }

    /**
     * Get storage health information
     */
    private JSONObject getStorageHealth() throws JSONException {
        JSONObject storageHealth = new JSONObject();
        
        try {
            JSONObject storageInfo = getStorageInfo();
            JSONObject internal = storageInfo.getJSONObject("internal");
            
            int usagePercentage = internal.getInt("usagePercentage");
            long availableMB = internal.getLong("availableMB");
            
            storageHealth.put("usagePercentage", usagePercentage);
            storageHealth.put("availableMB", availableMB);
            storageHealth.put("totalMB", internal.getLong("totalMB"));
            
            // Health assessment
            if (usagePercentage >= 95) {
                storageHealth.put("status", "critical");
                storageHealth.put("message", "Storage almost full");
            } else if (usagePercentage >= 85) {
                storageHealth.put("status", "warning");
                storageHealth.put("message", "Storage getting full");
            } else if (availableMB < 100) { // Less than 100MB available
                storageHealth.put("status", "critical");
                storageHealth.put("message", "Very low storage available");
            } else {
                storageHealth.put("status", "good");
                storageHealth.put("message", "Storage health normal");
            }
            
            storageHealth.put("score", calculateStorageHealthScore(usagePercentage, availableMB));
            
        } catch (Exception e) {
            storageHealth.put("status", "unknown");
            storageHealth.put("message", "Unable to assess storage health");
            storageHealth.put("score", 0);
        }
        
        return storageHealth;
    }

    /**
     * Calculate storage health score (0-100)
     */
    private int calculateStorageHealthScore(int usagePercentage, long availableMB) {
        int score = 100;
        
        // Deduct based on usage percentage
        if (usagePercentage >= 95) score -= 50;
        else if (usagePercentage >= 85) score -= 30;
        else if (usagePercentage >= 75) score -= 15;
        
        // Deduct based on available space
        if (availableMB < 50) score -= 40;
        else if (availableMB < 100) score -= 25;
        else if (availableMB < 500) score -= 10;
        
        return Math.max(0, score);
    }

    /**
     * Get memory health information
     */
    private JSONObject getMemoryHealth() throws JSONException {
        JSONObject memoryHealth = new JSONObject();
        
        try {
            JSONObject memoryInfo = getMemoryInfo();
            
            int usagePercentage = memoryInfo.getInt("usagePercentage");
            boolean isLowMemory = memoryInfo.getBoolean("isLowMemory");
            
            memoryHealth.put("usagePercentage", usagePercentage);
            memoryHealth.put("isLowMemory", isLowMemory);
            memoryHealth.put("availableMB", memoryInfo.getLong("availableMB"));
            memoryHealth.put("totalMB", memoryInfo.getLong("totalMB"));
            
            // Health assessment
            if (isLowMemory) {
                memoryHealth.put("status", "critical");
                memoryHealth.put("message", "System memory critically low");
            } else if (usagePercentage >= 90) {
                memoryHealth.put("status", "warning");
                memoryHealth.put("message", "Memory usage very high");
            } else if (usagePercentage >= 80) {
                memoryHealth.put("status", "warning");
                memoryHealth.put("message", "Memory usage high");
            } else {
                memoryHealth.put("status", "good");
                memoryHealth.put("message", "Memory health normal");
            }
            
            memoryHealth.put("score", calculateMemoryHealthScore(usagePercentage, isLowMemory));
            
        } catch (Exception e) {
            memoryHealth.put("status", "unknown");
            memoryHealth.put("message", "Unable to assess memory health");
            memoryHealth.put("score", 0);
        }
        
        return memoryHealth;
    }

    /**
     * Calculate memory health score (0-100)
     */
    private int calculateMemoryHealthScore(int usagePercentage, boolean isLowMemory) {
        int score = 100;
        
        if (isLowMemory) {
            score -= 60;
        } else {
            if (usagePercentage >= 90) score -= 40;
            else if (usagePercentage >= 80) score -= 25;
            else if (usagePercentage >= 70) score -= 10;
        }
        
        return Math.max(0, score);
    }

    /**
     * Get network health information
     */
    private JSONObject getNetworkHealth() throws JSONException {
        JSONObject networkHealth = new JSONObject();
        
        try {
            JSONObject networkInfo = getCurrentNetworkInfo();
            
            boolean isConnected = networkInfo.getBoolean("isConnected");
            String type = networkInfo.getString("type");
            boolean hasInternet = networkInfo.getBoolean("hasInternet");
            
            networkHealth.put("isConnected", isConnected);
            networkHealth.put("type", type);
            networkHealth.put("hasInternet", hasInternet);
            
            // Health assessment
            if (!isConnected) {
                networkHealth.put("status", "critical");
                networkHealth.put("message", "No network connection");
            } else if (!hasInternet) {
                networkHealth.put("status", "warning");
                networkHealth.put("message", "Connected but no internet access");
            } else {
                networkHealth.put("status", "good");
                networkHealth.put("message", "Network connection healthy");
            }
            
            networkHealth.put("score", calculateNetworkHealthScore(isConnected, hasInternet));
            
        } catch (Exception e) {
            networkHealth.put("status", "unknown");
            networkHealth.put("message", "Unable to assess network health");
            networkHealth.put("score", 0);
        }
        
        return networkHealth;
    }

    /**
     * Calculate network health score (0-100)
     */
    private int calculateNetworkHealthScore(boolean isConnected, boolean hasInternet) {
        if (!isConnected) return 0;
        if (!hasInternet) return 50;
        return 100;
    }

    /**
     * Get system health information
     */
    private JSONObject getSystemHealth() throws JSONException {
        JSONObject systemHealth = new JSONObject();
        
        try {
            // Basic system information
            systemHealth.put("androidVersion", Build.VERSION.RELEASE);
            systemHealth.put("sdkVersion", Build.VERSION.SDK_INT);
            systemHealth.put("model", Build.MODEL);
            systemHealth.put("manufacturer", Build.MANUFACTURER);
            
            // System status
            systemHealth.put("isEmulator", isEmulator());
            systemHealth.put("uptime", getSystemUptime());
            
            // Health assessment
            if (isEmulator()) {
                systemHealth.put("status", "info");
                systemHealth.put("message", "Running on emulator");
            } else {
                systemHealth.put("status", "good");
                systemHealth.put("message", "System running normally");
            }
            
            systemHealth.put("score", calculateSystemHealthScore());
            
        } catch (Exception e) {
            systemHealth.put("status", "unknown");
            systemHealth.put("message", "Unable to assess system health");
            systemHealth.put("score", 0);
        }
        
        return systemHealth;
    }

    /**
     * Calculate system health score (0-100)
     */
    private int calculateSystemHealthScore() {
        int score = 100;
        
        if (isEmulator()) {
            score -= 20; // Emulators may have limitations
        }
        
        // Additional system health checks could be added here
        
        return Math.max(0, score);
    }

    /**
     * Calculate overall health score based on all diagnostics
     */
    private int calculateHealthScore(JSONObject diagnostics) throws JSONException {
        int totalScore = 0;
        int componentCount = 0;
        
        String[] components = {"battery", "storage", "memory", "network", "system"};
        
        for (String component : components) {
            if (diagnostics.has(component)) {
                JSONObject componentHealth = diagnostics.getJSONObject(component);
                if (componentHealth.has("score")) {
                    totalScore += componentHealth.getInt("score");
                    componentCount++;
                }
            }
        }
        
        return componentCount > 0 ? totalScore / componentCount : 0;
    }

    // =========================================================================
    // CLEANUP AND RESOURCE MANAGEMENT
    // =========================================================================

    /**
     * Clean up resources when the bridge is destroyed
     */
    public void destroy() {
        try {
            // Unregister battery receiver if active
            if (isBatteryListening && batteryReceiver != null) {
                context.unregisterReceiver(batteryReceiver);
                batteryReceiver = null;
                isBatteryListening = false;
            }
            
            // Unregister network receiver if active
            if (isNetworkListening && networkReceiver != null) {
                context.unregisterReceiver(networkReceiver);
                networkReceiver = null;
                isNetworkListening = false;
            }
            
            android.util.Log.d(TAG, "DeviceManagerBridge resources cleaned up");
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error during cleanup", e);
        }
    }
}
