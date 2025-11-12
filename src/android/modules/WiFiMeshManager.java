package org.apache.cordova.resqpeernet.modules;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WifiMeshManager {
    private static final String TAG = "WifiMeshManager";
    
    private Context context;
    private WifiManager wifiManager;
    private WifiP2pManager wifiP2pManager;
    private Channel channel;
    
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private boolean isGroupOwner = false;
    private String groupOwnerAddress;
    
    public interface WifiMeshCallback {
        void onPeersDiscovered(List<WifiP2pDevice> peers);
        void onConnectionEstablished(WifiP2pInfo info);
        void onConnectionLost();
        void onMessageReceived(String fromAddress, String message);
        void onGroupCreated(boolean isOwner);
    }
    
    private WifiMeshCallback meshCallback;

    public WifiMeshManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        
        if (wifiP2pManager != null) {
            this.channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        }
    }

    /**
     * Initialize WiFi Mesh networking
     */
    public void initializeMesh(CallbackContext callbackContext) {
        try {
            if (wifiP2pManager == null) {
                callbackContext.error("WiFi P2P not supported on this device");
                return;
            }
            
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
            
            JSONObject result = new JSONObject();
            result.put("status", "initialized");
            result.put("wifiP2pSupported", true);
            result.put("wifiEnabled", wifiManager.isWifiEnabled());
            
            callbackContext.success(result);
            Log.i(TAG, "WiFi Mesh initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing WiFi Mesh", e);
            callbackContext.error("Error initializing WiFi Mesh: " + e.getMessage());
        }
    }

    /**
     * Create a WiFi P2P group (become Group Owner)
     */
    public void createMeshGroup(CallbackContext callbackContext) {
        try {
            if (wifiP2pManager == null) {
                callbackContext.error("WiFi P2P not supported");
                return;
            }
            
            wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "WiFi Mesh group created successfully");
                    try {
                        JSONObject result = new JSONObject();
                        result.put("status", "group_created");
                        result.put("isGroupOwner", true);
                        callbackContext.success(result);
                    } catch (JSONException e) {
                        callbackContext.error("Error creating response: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(int reason) {
                    String errorMsg = getErrorReason(reason);
                    Log.e(TAG, "Failed to create WiFi Mesh group: " + errorMsg);
                    callbackContext.error("Failed to create group: " + errorMsg);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating mesh group", e);
            callbackContext.error("Error creating mesh group: " + e.getMessage());
        }
    }

    /**
     * Discover available WiFi P2P peers
     */
    public void discoverPeers(CallbackContext callbackContext) {
        try {
            if (wifiP2pManager == null) {
                callbackContext.error("WiFi P2P not supported");
                return;
            }
            
            wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "WiFi P2P discovery started");
                    try {
                        JSONObject result = new JSONObject();
                        result.put("status", "discovery_started");
                        callbackContext.success(result);
                    } catch (JSONException e) {
                        callbackContext.error("Error creating response: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(int reason) {
                    String errorMsg = getErrorReason(reason);
                    Log.e(TAG, "Failed to start discovery: " + errorMsg);
                    callbackContext.error("Failed to start discovery: " + errorMsg);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting peer discovery", e);
            callbackContext.error("Error starting discovery: " + e.getMessage());
        }
    }

    /**
     * Connect to a specific WiFi P2P device
     */
    public void connectToDevice(JSONObject args, CallbackContext callbackContext) {
        try {
            if (wifiP2pManager == null) {
                callbackContext.error("WiFi P2P not supported");
                return;
            }
            
            String deviceAddress = args.getString("deviceAddress");
            
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = deviceAddress;
            config.groupOwnerIntent = 0;
            
            wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Connection initiated to device: " + deviceAddress);
                    try {
                        JSONObject result = new JSONObject();
                        result.put("status", "connection_initiated");
                        result.put("deviceAddress", deviceAddress);
                        callbackContext.success(result);
                    } catch (JSONException e) {
                        callbackContext.error("Error creating response: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(int reason) {
                    String errorMsg = getErrorReason(reason);
                    Log.e(TAG, "Failed to connect to device: " + errorMsg);
                    callbackContext.error("Failed to connect: " + errorMsg);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to device", e);
            callbackContext.error("Error connecting to device: " + e.getMessage());
        }
    }

    /**
     * Get list of discovered peers
     */
    public void getDiscoveredPeers(CallbackContext callbackContext) {
        try {
            JSONArray peersArray = new JSONArray();
            
            for (WifiP2pDevice device : peers) {
                JSONObject peer = new JSONObject();
                peer.put("deviceName", device.deviceName);
                peer.put("deviceAddress", device.deviceAddress);
                peer.put("status", getDeviceStatus(device.status));
                peer.put("isGroupOwner", device.isGroupOwner());
                
                peersArray.put(peer);
            }
            
            JSONObject result = new JSONObject();
            result.put("peers", peersArray);
            result.put("count", peers.size());
            
            callbackContext.success(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting discovered peers", e);
            callbackContext.error("Error getting peers: " + e.getMessage());
        }
    }

    /**
     * Send message through WiFi Mesh network
     */
    public void sendMeshMessage(JSONObject args, CallbackContext callbackContext) {
        try {
            // Untuk demo, kita simulasikan pengiriman message
            String targetAddress = args.optString("targetAddress", "");
            String message = args.getString("message");
            
            Log.i(TAG, "Sending message - Target: " + targetAddress + ", Message: " + message);
            
            JSONObject result = new JSONObject();
            result.put("status", "message_sent");
            result.put("target", targetAddress.isEmpty() ? "broadcast" : targetAddress);
            result.put("messageLength", message.length());
            
            callbackContext.success(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending mesh message", e);
            callbackContext.error("Error sending message: " + e.getMessage());
        }
    }

    /**
     * Get current mesh network topology
     */
    public void getMeshTopology(CallbackContext callbackContext) {
        try {
            JSONObject topology = new JSONObject();
            topology.put("isGroupOwner", isGroupOwner);
            topology.put("groupOwnerAddress", groupOwnerAddress != null ? groupOwnerAddress : "");
            topology.put("connectedClients", 0); // Simplified for demo
            topology.put("peersCount", peers.size());
            
            callbackContext.success(topology);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting mesh topology", e);
            callbackContext.error("Error getting topology: " + e.getMessage());
        }
    }

    /**
     * Remove group and clean up
     */
    public void removeGroup(CallbackContext callbackContext) {
        try {
            if (wifiP2pManager == null) {
                callbackContext.error("WiFi P2P not supported");
                return;
            }
            
            wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "WiFi Mesh group removed");
                    cleanup();
                    try {
                        JSONObject result = new JSONObject();
                        result.put("status", "group_removed");
                        callbackContext.success(result);
                    } catch (JSONException e) {
                        callbackContext.error("Error creating response: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(int reason) {
                    String errorMsg = getErrorReason(reason);
                    Log.e(TAG, "Failed to remove group: " + errorMsg);
                    callbackContext.error("Failed to remove group: " + errorMsg);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error removing group", e);
            callbackContext.error("Error removing group: " + e.getMessage());
        }
    }

    /**
     * Set mesh callback for events
     */
    public void setMeshCallback(WifiMeshCallback callback) {
        this.meshCallback = callback;
    }

    /**
     * Update peers list (called from ResqPeerNet)
     */
    public void updatePeersList(List<WifiP2pDevice> peerList) {
        this.peers.clear();
        this.peers.addAll(peerList);
        
        if (meshCallback != null) {
            meshCallback.onPeersDiscovered(peers);
        }
    }

    /**
     * Update connection info (called from ResqPeerNet)
     */
    public void updateConnectionInfo(WifiP2pInfo info) {
        this.isGroupOwner = info.isGroupOwner;
        this.groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
        
        if (meshCallback != null) {
            meshCallback.onConnectionEstablished(info);
            meshCallback.onGroupCreated(isGroupOwner);
        }
    }

    private void cleanup() {
        isGroupOwner = false;
        groupOwnerAddress = null;
        peers.clear();
    }

    private String getErrorReason(int reason) {
        switch (reason) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "P2P unsupported";
            case WifiP2pManager.ERROR:
                return "Internal error";
            case WifiP2pManager.BUSY:
                return "Busy";
            default:
                return "Unknown error: " + reason;
        }
    }

    private String getDeviceStatus(int status) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE:
                return "available";
            case WifiP2pDevice.INVITED:
                return "invited";
            case WifiP2pDevice.CONNECTED:
                return "connected";
            case WifiP2pDevice.FAILED:
                return "failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "unavailable";
            default:
                return "unknown";
        }
    }
}