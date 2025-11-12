package org.apache.cordova.resqpeernet.modules;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NetworkManagerBridge - Network Monitoring and Information
 */
public class NetworkManagerBridge {

    private static final String TAG = "NetworkManagerBridge";
    
    private final Context context;
    private final ExecutorService executorService;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private TelephonyManager telephonyManager;

    public NetworkManagerBridge(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        initializeManagers();
    }

    private void initializeManagers() {
        try {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            Log.i(TAG, "Network managers initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing network managers", e);
        }
    }

    /**
     * Get comprehensive network status
     */
    public void getNetworkStatus(final CallbackContext callbackContext) {
        executorService.execute(() -> {
            try {
                JSONObject status = new JSONObject();
                
                // Connection status
                status.put("isConnected", isConnected());
                status.put("isWifiConnected", isWifiConnected());
                status.put("isMobileConnected", isMobileConnected());
                
                // Network type
                status.put("networkType", getNetworkType());
                status.put("connectionType", getConnectionType());
                
                // WiFi information
                if (isWifiConnected()) {
                    status.put("wifiInfo", getWifiInfo());
                }
                
                // Mobile network information
                if (isMobileConnected()) {
                    status.put("mobileInfo", getMobileInfo());
                }
                
                // IP Address
                status.put("ipAddress", getIPAddress());
                
                // Timestamp
                status.put("timestamp", System.currentTimeMillis());
                
                sendSuccess(callbackContext, status);
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting network status", e);
                sendError(callbackContext, "Failed to get network status: " + e.getMessage());
            }
        });
    }

    /**
     * Get detailed WiFi information
     */
    public void getWifiStatus(final CallbackContext callbackContext) {
        executorService.execute(() -> {
            try {
                JSONObject wifiStatus = new JSONObject();
                
                wifiStatus.put("isWifiEnabled", isWifiEnabled());
                wifiStatus.put("isWifiConnected", isWifiConnected());
                
                if (isWifiConnected()) {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    
                    wifiStatus.put("ssid", wifiInfo.getSSID().replace("\"", ""));
                    wifiStatus.put("bssid", wifiInfo.getBSSID());
                    wifiStatus.put("signalStrength", wifiInfo.getRssi());
                    wifiStatus.put("linkSpeed", wifiInfo.getLinkSpeed() + " Mbps");
                    wifiStatus.put("frequency", wifiInfo.getFrequency() + " MHz");
                    wifiStatus.put("ipAddress", formatIPAddress(wifiInfo.getIpAddress()));
                    
                    // Network capabilities (Android 21+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        wifiStatus.put("isMetered", isMeteredConnection());
                    }
                }
                
                wifiStatus.put("timestamp", System.currentTimeMillis());
                sendSuccess(callbackContext, wifiStatus);
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting WiFi status", e);
                sendError(callbackContext, "Failed to get WiFi status: " + e.getMessage());
            }
        });
    }

    /**
     * Get mobile network information
     */
    public void getMobileStatus(final CallbackContext callbackContext) {
        executorService.execute(() -> {
            try {
                JSONObject mobileStatus = new JSONObject();
                
                mobileStatus.put("isMobileConnected", isMobileConnected());
                
                if (isMobileConnected()) {
                    mobileStatus.put("networkOperator", telephonyManager.getNetworkOperatorName());
                    mobileStatus.put("networkType", getMobileNetworkType());
                    mobileStatus.put("signalStrength", getMobileSignalStrength());
                    mobileStatus.put("isRoaming", telephonyManager.isNetworkRoaming());
                    
                    // SIM information
                    mobileStatus.put("simOperator", telephonyManager.getSimOperatorName());
                    mobileStatus.put("hasSim", telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY);
                }
                
                mobileStatus.put("timestamp", System.currentTimeMillis());
                sendSuccess(callbackContext, mobileStatus);
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting mobile status", e);
                sendError(callbackContext, "Failed to get mobile status: " + e.getMessage());
            }
        });
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private boolean isConnected() {
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && 
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    private boolean isWifiConnected() {
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    private boolean isMobileConnected() {
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    private boolean isWifiEnabled() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    private String getNetworkType() {
        if (!isConnected()) return "DISCONNECTED";
        if (isWifiConnected()) return "WIFI";
        if (isMobileConnected()) return "MOBILE";
        return "UNKNOWN";
    }

    private String getConnectionType() {
        if (!isConnected()) return "none";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return "unknown";
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) return "unknown";
            
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "wifi";
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "cellular";
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "ethernet";
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return "vpn";
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) return "bluetooth";
        }
        
        return "unknown";
    }

    private JSONObject getWifiInfo() throws JSONException {
        JSONObject wifiInfo = new JSONObject();
        WifiInfo info = wifiManager.getConnectionInfo();
        
        wifiInfo.put("ssid", info.getSSID().replace("\"", ""));
        wifiInfo.put("bssid", info.getBSSID());
        wifiInfo.put("rssi", info.getRssi());
        wifiInfo.put("linkSpeed", info.getLinkSpeed());
        wifiInfo.put("frequency", info.getFrequency());
        wifiInfo.put("ipAddress", formatIPAddress(info.getIpAddress()));
        
        return wifiInfo;
    }

    private JSONObject getMobileInfo() throws JSONException {
        JSONObject mobileInfo = new JSONObject();
        
        mobileInfo.put("networkOperator", telephonyManager.getNetworkOperatorName());
        mobileInfo.put("networkType", getMobileNetworkType());
        mobileInfo.put("isRoaming", telephonyManager.isNetworkRoaming());
        
        return mobileInfo;
    }

    private String getMobileNetworkType() {
        if (telephonyManager == null) return "UNKNOWN";
        
        int networkType = telephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS: return "2G";
            case TelephonyManager.NETWORK_TYPE_EDGE: return "2G";
            case TelephonyManager.NETWORK_TYPE_CDMA: return "2G";
            case TelephonyManager.NETWORK_TYPE_1xRTT: return "2G";
            case TelephonyManager.NETWORK_TYPE_IDEN: return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS: return "3G";
            case TelephonyManager.NETWORK_TYPE_EVDO_0: return "3G";
            case TelephonyManager.NETWORK_TYPE_EVDO_A: return "3G";
            case TelephonyManager.NETWORK_TYPE_HSDPA: return "3G";
            case TelephonyManager.NETWORK_TYPE_HSUPA: return "3G";
            case TelephonyManager.NETWORK_TYPE_HSPA: return "3G";
            case TelephonyManager.NETWORK_TYPE_EVDO_B: return "3G";
            case TelephonyManager.NETWORK_TYPE_EHRPD: return "3G";
            case TelephonyManager.NETWORK_TYPE_HSPAP: return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE: return "4G";
            case TelephonyManager.NETWORK_TYPE_NR: return "5G";
            default: return "UNKNOWN";
        }
    }

    private int getMobileSignalStrength() {
        // Note: This is a simplified implementation
        // Real signal strength would require more complex handling
        return -1; // Placeholder
    }

    private boolean isMeteredConnection() {
        if (connectivityManager == null) return false;
        return connectivityManager.isActiveNetworkMetered();
    }

    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (isIPv4) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting IP address", e);
        }
        return "unknown";
    }

    private String formatIPAddress(int ip) {
        return String.format("%d.%d.%d.%d",
            (ip & 0xff),
            (ip >> 8 & 0xff),
            (ip >> 16 & 0xff),
            (ip >> 24 & 0xff));
    }

    private void sendSuccess(CallbackContext callbackContext, JSONObject result) {
        if (callbackContext != null) {
            callbackContext.success(result);
        }
    }

    private void sendError(CallbackContext callbackContext, String message) {
        if (callbackContext != null) {
            try {
                JSONObject error = new JSONObject();
                error.put("error", message);
                error.put("code", "NETWORK_ERROR");
                callbackContext.error(error);
            } catch (JSONException e) {
                callbackContext.error("{\"error\":\"" + message + "\",\"code\":\"NETWORK_ERROR\"}");
            }
        }
    }

    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        Log.i(TAG, "NetworkManagerBridge destroyed");
    }
}