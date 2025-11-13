package com.muhammadandikcahyono.resqpeernet.modules;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoLocalMeshManager {
    private static final String TAG = "AutoLocalMesh";
    private static final int MESH_PORT = 8888;
    private static final int DISCOVERY_INTERVAL = 5; // seconds
    private static final int PEER_CLEANUP_INTERVAL = 30; // seconds
    
    private Context context;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private ScheduledExecutorService scheduler;
    
    private boolean isAutoMeshRunning = false;
    private MeshServer meshServer;
    private ConcurrentHashMap<String, MeshNode> meshNodes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, DiscoveredPeer> discoveredPeers = new ConcurrentHashMap<>();
    private String localIpAddress;
    
    // Statistics
    private int totalMessagesSent = 0;
    private int totalMessagesReceived = 0;
    private long meshStartTime = 0;
    
    // Enhanced callback interface
    public interface AutoMeshCallback {
        void onMeshNodeJoined(String nodeIp, String nodeInfo);
        void onMeshNodeLeft(String nodeIp);
        void onMeshMessageReceived(String fromIp, String message);
        void onMeshNetworkReady(int nodeCount);
        void onAutoDiscoveryStarted();
        void onPeerDiscovered(String peerIp, String peerInfo);
        void onPeerConnected(String peerIp);
        void onPeerDisconnected(String peerIp);
        void onNetworkStatusChanged(boolean isConnected);
        void onMessageDelivered(String toIp, boolean success);
    }
    
    private AutoMeshCallback meshCallback;

    // Discovered peer class for manual management
    private class DiscoveredPeer {
        String ipAddress;
        String hostname;
        long discoveredAt;
        boolean isReachable;
        int failedConnectionAttempts;
        
        DiscoveredPeer(String ip) {
            this.ipAddress = ip;
            this.hostname = "Unknown";
            this.discoveredAt = System.currentTimeMillis();
            this.isReachable = true;
            this.failedConnectionAttempts = 0;
        }
    }

    public AutoLocalMeshManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.scheduler = Executors.newScheduledThreadPool(3);
    }

    /**
     * START AUTO-MESH - Full automatic mode
     */
    public void startAutoMesh(CallbackContext callbackContext) {
        try {
            if (!isWifiConnected()) {
                callbackContext.error("Connect to a WiFi network first");
                return;
            }
            
            localIpAddress = getLocalIpAddress();
            if (localIpAddress == null) {
                callbackContext.error("Cannot get local IP address");
                return;
            }
            
            // Reset statistics
            meshStartTime = System.currentTimeMillis();
            totalMessagesSent = 0;
            totalMessagesReceived = 0;
            
            // 1. Start mesh server
            startMeshServer();
            
            // 2. Start continuous auto-discovery
            startAutoDiscovery();
            
            // 3. Start peer cleanup task
            startPeerCleanupTask();
            
            // 4. Add self to mesh
            addSelfToMesh();
            
            isAutoMeshRunning = true;
            
            JSONObject result = new JSONObject();
            result.put("status", "auto_mesh_started");
            result.put("localIp", localIpAddress);
            result.put("network", getCurrentSSID());
            result.put("port", MESH_PORT);
            result.put("message", "Auto-mesh network activated! Devices will auto-connect.");
            
            callbackContext.success(result);
            
            if (meshCallback != null) {
                meshCallback.onAutoDiscoveryStarted();
                meshCallback.onMeshNetworkReady(1); // Self counted as first node
            }
            
            Log.i(TAG, "🚀 AUTO-MESH STARTED on " + localIpAddress + " in network: " + getCurrentSSID());
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting auto-mesh", e);
            callbackContext.error("Error starting auto-mesh: " + e.getMessage());
        }
    }

    /**
     * MANUAL MESH INITIALIZATION - For controlled setup
     */
    public void initializeMesh(CallbackContext callbackContext) {
        try {
            if (!isWifiConnected()) {
                callbackContext.error("Not connected to WiFi network");
                return;
            }
            
            localIpAddress = getLocalIpAddress();
            if (localIpAddress == null) {
                callbackContext.error("Cannot get local IP address");
                return;
            }
            
            // Start mesh server only (no auto-connect)
            startMeshServer();
            
            // Start discovery but don't auto-connect
            startDiscoveryOnly();
            
            isAutoMeshRunning = true;
            
            JSONObject result = new JSONObject();
            result.put("status", "initialized");
            result.put("localIp", localIpAddress);
            result.put("networkSsid", getCurrentSSID());
            result.put("autoConnect", false);
            result.put("message", "Manual mesh initialized successfully");
            
            callbackContext.success(result);
            Log.i(TAG, "Manual mesh initialized on IP: " + localIpAddress);
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing mesh", e);
            callbackContext.error("Error initializing mesh: " + e.getMessage());
        }
    }

    /**
     * MANUAL PEER DISCOVERY
     */
    public void discoverPeers(CallbackContext callbackContext) {
        try {
            if (!isAutoMeshRunning) {
                callbackContext.error("Mesh not initialized. Call initializeMesh first.");
                return;
            }
            
            Log.i(TAG, "Starting manual peer discovery...");
            
            // Do fresh scan
            manualNetworkScan();
            
            JSONObject result = new JSONObject();
            result.put("status", "discovery_started");
            result.put("localIp", localIpAddress);
            result.put("network", getCurrentSSID());
            result.put("message", "Manual peer discovery started");
            
            callbackContext.success(result);
            
            // Schedule result check after scan completes
            scheduler.schedule(() -> {
                try {
                    JSONObject discoveryResult = new JSONObject();
                    discoveryResult.put("status", "discovery_completed");
                    discoveryResult.put("peersFound", discoveredPeers.size());
                    discoveryResult.put("timestamp", System.currentTimeMillis());
                    
                    // Notify via callback
                    if (meshCallback != null) {
                        for (DiscoveredPeer peer : discoveredPeers.values()) {
                            meshCallback.onPeerDiscovered(peer.ipAddress, peer.hostname);
                        }
                    }
                    
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating discovery result", e);
                }
            }, 3, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting peer discovery", e);
            callbackContext.error("Error starting discovery: " + e.getMessage());
        }
    }

    /**
     * GET DISCOVERED PEERS
     */
    public void getDiscoveredPeers(CallbackContext callbackContext) {
        try {
            JSONArray peersArray = new JSONArray();
            
            for (DiscoveredPeer peer : discoveredPeers.values()) {
                JSONObject peerInfo = new JSONObject();
                peerInfo.put("ipAddress", peer.ipAddress);
                peerInfo.put("hostname", peer.hostname);
                peerInfo.put("discoveredAt", peer.discoveredAt);
                peerInfo.put("isReachable", peer.isReachable);
                peerInfo.put("isConnected", meshNodes.containsKey(peer.ipAddress));
                peerInfo.put("failedAttempts", peer.failedConnectionAttempts);
                peerInfo.put("ageSeconds", (System.currentTimeMillis() - peer.discoveredAt) / 1000);
                
                peersArray.put(peerInfo);
            }
            
            JSONObject result = new JSONObject();
            result.put("peers", peersArray);
            result.put("count", discoveredPeers.size());
            result.put("connectedCount", meshNodes.size());
            result.put("network", getCurrentSSID());
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.d(TAG, "Returning " + discoveredPeers.size() + " discovered peers");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting discovered peers", e);
            callbackContext.error("Error getting peers: " + e.getMessage());
        }
    }

    /**
     * MANUAL CONNECT TO SPECIFIC PEER
     */
    public void connectToPeer(JSONObject args, CallbackContext callbackContext) {
        try {
            if (!isAutoMeshRunning) {
                callbackContext.error("Mesh not initialized");
                return;
            }
            
            String peerIp = args.getString("peerIp");
            int timeout = args.optInt("timeout", 5000);
            
            if (meshNodes.containsKey(peerIp)) {
                callbackContext.error("Already connected to peer: " + peerIp);
                return;
            }
            
            boolean success = manualConnectToPeer(peerIp, timeout);
            
            JSONObject result = new JSONObject();
            result.put("status", success ? "connected" : "failed");
            result.put("peerIp", peerIp);
            result.put("timeout", timeout);
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to peer", e);
            callbackContext.error("Error connecting to peer: " + e.getMessage());
        }
    }

    /**
     * AUTO CONNECT TO ALL DISCOVERED PEERS
     */
    public void autoConnectPeers(CallbackContext callbackContext) {
        try {
            if (!isAutoMeshRunning) {
                callbackContext.error("Mesh not initialized");
                return;
            }
            
            Log.i(TAG, "Starting auto-connect to discovered peers...");
            
            int connectionAttempts = 0;
            int successfulConnections = 0;
            List<String> failedConnections = new ArrayList<>();
            
            for (DiscoveredPeer peer : discoveredPeers.values()) {
                if (!meshNodes.containsKey(peer.ipAddress) && peer.isReachable) {
                    connectionAttempts++;
                    if (manualConnectToPeer(peer.ipAddress, 5000)) {
                        successfulConnections++;
                    } else {
                        failedConnections.add(peer.ipAddress);
                        peer.failedConnectionAttempts++;
                        if (peer.failedConnectionAttempts >= 3) {
                            peer.isReachable = false;
                        }
                    }
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("status", "auto_connect_completed");
            result.put("attempts", connectionAttempts);
            result.put("successful", successfulConnections);
            result.put("failed", failedConnections.size());
            result.put("totalConnected", meshNodes.size());
            result.put("failedList", new JSONArray(failedConnections));
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.i(TAG, "Auto-connect completed: " + successfulConnections + "/" + connectionAttempts + " successful");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in auto-connect", e);
            callbackContext.error("Error in auto-connect: " + e.getMessage());
        }
    }

    /**
     * SEND MESSAGE - Supports both targeted and broadcast
     */
    public void sendMeshMessage(JSONObject args, CallbackContext callbackContext) {
        try {
            if (!isAutoMeshRunning) {
                callbackContext.error("Mesh not initialized");
                return;
            }
            
            String targetIp = args.optString("targetIp", ""); // Empty for broadcast
            String message = args.getString("message");
            String messageType = args.optString("messageType", "text");
            int timeout = args.optInt("timeout", 5000);
            boolean reliable = args.optBoolean("reliable", false);
            
            if (meshNodes.isEmpty()) {
                callbackContext.error("No connected peers to send message to");
                return;
            }
            
            int recipients = 0;
            List<String> failedSends = new ArrayList<>();
            List<String> successfulSends = new ArrayList<>();
            
            if (targetIp.isEmpty()) {
                // Broadcast to all connected peers
                for (MeshNode node : meshNodes.values()) {
                    if (node.isConnected() && !node.getIpAddress().equals(localIpAddress)) {
                        boolean sent = node.sendMessage(message, timeout);
                        if (sent) {
                            recipients++;
                            successfulSends.add(node.getIpAddress());
                            totalMessagesSent++;
                        } else {
                            failedSends.add(node.getIpAddress());
                        }
                    }
                }
            } else {
                // Send to specific peer
                MeshNode node = meshNodes.get(targetIp);
                if (node != null && node.isConnected()) {
                    boolean sent = node.sendMessage(message, timeout);
                    if (sent) {
                        recipients = 1;
                        successfulSends.add(targetIp);
                        totalMessagesSent++;
                    } else {
                        failedSends.add(targetIp);
                    }
                } else {
                    callbackContext.error("Target peer not connected: " + targetIp);
                    return;
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("status", "message_sent");
            result.put("target", targetIp.isEmpty() ? "broadcast" : targetIp);
            result.put("recipients", recipients);
            result.put("failedSends", failedSends.size());
            result.put("messageLength", message.length());
            result.put("messageType", messageType);
            result.put("reliable", reliable);
            result.put("timestamp", System.currentTimeMillis());
            
            if (!failedSends.isEmpty()) {
                result.put("failedList", new JSONArray(failedSends));
            }
            if (!successfulSends.isEmpty()) {
                result.put("successfulList", new JSONArray(successfulSends));
            }
            
            callbackContext.success(result);
            Log.i(TAG, "Message sent to " + recipients + " recipients");
            
            // Notify callback for successful deliveries
            if (meshCallback != null) {
                for (String ip : successfulSends) {
                    meshCallback.onMessageDelivered(ip, true);
                }
                for (String ip : failedSends) {
                    meshCallback.onMessageDelivered(ip, false);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending mesh message", e);
            callbackContext.error("Error sending message: " + e.getMessage());
        }
    }

    /**
     * BROADCAST MESSAGE - Simplified broadcast
     */
    public void broadcastToMesh(JSONObject args, CallbackContext callbackContext) {
        try {
            String message = args.getString("message");
            String messageType = args.optString("type", "broadcast");
            
            int recipients = 0;
            for (MeshNode node : meshNodes.values()) {
                if (node.isConnected() && !node.getIpAddress().equals(localIpAddress)) {
                    node.sendMessage(message);
                    recipients++;
                    totalMessagesSent++;
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("status", "broadcast_sent");
            result.put("recipients", recipients);
            result.put("totalNodes", meshNodes.size());
            result.put("messageType", messageType);
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.i(TAG, "📢 Broadcast to " + recipients + " nodes: " + message);
            
        } catch (Exception e) {
            Log.e(TAG, "Error broadcasting to mesh", e);
            callbackContext.error("Error broadcasting: " + e.getMessage());
        }
    }

    /**
     * GET MESH TOPOLOGY - Detailed network information
     */
    public void getMeshTopology(CallbackContext callbackContext) {
        try {
            if (!isAutoMeshRunning) {
                callbackContext.error("Mesh not initialized");
                return;
            }
            
            JSONObject topology = new JSONObject();
            topology.put("isActive", isAutoMeshRunning);
            topology.put("localIp", localIpAddress);
            topology.put("networkSsid", getCurrentSSID());
            topology.put("connectedPeers", meshNodes.size() - 1); // Exclude self
            topology.put("discoveredPeers", discoveredPeers.size());
            topology.put("meshPort", MESH_PORT);
            topology.put("uptime", System.currentTimeMillis() - meshStartTime);
            
            // Connected nodes details
            JSONArray connectedArray = new JSONArray();
            for (MeshNode node : meshNodes.values()) {
                JSONObject nodeInfo = new JSONObject();
                nodeInfo.put("ipAddress", node.getIpAddress());
                nodeInfo.put("isSelf", node.getIpAddress().equals(localIpAddress));
                nodeInfo.put("connected", node.isConnected());
                nodeInfo.put("lastActivity", node.getLastSeen());
                nodeInfo.put("messageCount", node.getMessageCount());
                nodeInfo.put("connectionType", node.getConnectionType());
                connectedArray.put(nodeInfo);
            }
            topology.put("connectedNodes", connectedArray);
            
            // Discovered peers details
            JSONArray discoveredArray = new JSONArray();
            for (DiscoveredPeer peer : discoveredPeers.values()) {
                JSONObject peerInfo = new JSONObject();
                peerInfo.put("ipAddress", peer.ipAddress);
                peerInfo.put("hostname", peer.hostname);
                peerInfo.put("isReachable", peer.isReachable);
                peerInfo.put("isConnected", meshNodes.containsKey(peer.ipAddress));
                peerInfo.put("ageSeconds", (System.currentTimeMillis() - peer.discoveredAt) / 1000);
                peerInfo.put("failedAttempts", peer.failedConnectionAttempts);
                discoveredArray.put(peerInfo);
            }
            topology.put("discoveredPeers", discoveredArray);
            
            // Network statistics
            JSONObject stats = new JSONObject();
            stats.put("totalMessagesSent", totalMessagesSent);
            stats.put("totalMessagesReceived", totalMessagesReceived);
            stats.put("activeConnections", getActiveConnectionCount());
            stats.put("meshUptime", System.currentTimeMillis() - meshStartTime);
            stats.put("discoveryCycles", getDiscoveryCycleCount());
            topology.put("statistics", stats);
            
            callbackContext.success(topology);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting mesh topology", e);
            callbackContext.error("Error getting topology: " + e.getMessage());
        }
    }

    /**
     * GET MESH STATUS - Simplified status
     */
    public void getMeshStatus(CallbackContext callbackContext) {
        try {
            JSONObject status = new JSONObject();
            status.put("isActive", isAutoMeshRunning);
            status.put("localIp", localIpAddress);
            status.put("network", getCurrentSSID());
            status.put("totalNodes", meshNodes.size());
            status.put("startTime", meshStartTime);
            status.put("uptime", System.currentTimeMillis() - meshStartTime);
            
            JSONArray nodesArray = new JSONArray();
            for (MeshNode node : meshNodes.values()) {
                JSONObject nodeInfo = new JSONObject();
                nodeInfo.put("ipAddress", node.getIpAddress());
                nodeInfo.put("isConnected", node.isConnected());
                nodeInfo.put("isSelf", node.getIpAddress().equals(localIpAddress));
                nodeInfo.put("lastSeen", node.getLastSeen());
                nodesArray.put(nodeInfo);
            }
            
            status.put("nodes", nodesArray);
            callbackContext.success(status);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting mesh status", e);
            callbackContext.error("Error getting mesh status: " + e.getMessage());
        }
    }

    /**
     * STOP MESH NETWORK
     */
    public void stopMesh(CallbackContext callbackContext) {
        try {
            Log.i(TAG, "Stopping mesh network...");
            
            isAutoMeshRunning = false;
            
            // Stop scheduler
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }
            
            // Stop mesh server
            if (meshServer != null) {
                meshServer.stop();
                meshServer = null;
            }
            
            // Disconnect all nodes
            for (MeshNode node : meshNodes.values()) {
                node.disconnect();
            }
            meshNodes.clear();
            
            // Clear discovered peers
            discoveredPeers.clear();
            
            JSONObject result = new JSONObject();
            result.put("status", "stopped");
            result.put("disconnectedNodes", meshNodes.size());
            result.put("uptime", System.currentTimeMillis() - meshStartTime);
            result.put("totalMessages", totalMessagesSent);
            result.put("message", "Mesh network stopped successfully");
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.i(TAG, "Mesh network stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping mesh", e);
            callbackContext.error("Error stopping mesh: " + e.getMessage());
        }
    }

    // =========================================================================
    // PRIVATE IMPLEMENTATION METHODS
    // =========================================================================

    private void startAutoDiscovery() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!isAutoMeshRunning) return;
            
            try {
                scanNetworkForPeers();
                cleanupDisconnectedNodes();
                verifyConnectedPeers();
                
                Log.d(TAG, "🔄 Auto-scan: " + discoveredPeers.size() + " peers, " + 
                      (meshNodes.size() - 1) + " connected");
                
            } catch (Exception e) {
                Log.e(TAG, "Auto-discovery error", e);
            }
        }, 0, DISCOVERY_INTERVAL, TimeUnit.SECONDS);
    }

    private void startDiscoveryOnly() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!isAutoMeshRunning) return;
            
            try {
                manualNetworkScan();
                cleanupOldPeers();
                
                Log.d(TAG, "Manual scan: " + discoveredPeers.size() + " peers discovered");
                
            } catch (Exception e) {
                Log.e(TAG, "Discovery error", e);
            }
        }, 0, DISCOVERY_INTERVAL, TimeUnit.SECONDS);
    }

    private void startPeerCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!isAutoMeshRunning) return;
            
            try {
                cleanupOldPeers();
                cleanupStaleNodes();
                
            } catch (Exception e) {
                Log.e(TAG, "Peer cleanup error", e);
            }
        }, PEER_CLEANUP_INTERVAL, PEER_CLEANUP_INTERVAL, TimeUnit.SECONDS);
    }

    private void scanNetworkForPeers() {
        try {
            String networkPrefix = localIpAddress.substring(0, localIpAddress.lastIndexOf(".") + 1);
            List<Thread> scanThreads = new ArrayList<>();
            
            for (int i = 1; i <= 254; i++) {
                final String testIp = networkPrefix + i;
                
                if (testIp.equals(localIpAddress)) continue;
                
                Thread scanThread = new Thread(() -> {
                    if (isMeshNodeAvailable(testIp)) {
                        onPeerDiscovered(testIp);
                    }
                });
                
                scanThreads.add(scanThread);
                scanThread.start();
            }
            
            for (Thread thread : scanThreads) {
                try {
                    thread.join(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Network scan error", e);
        }
    }

    private void manualNetworkScan() {
        try {
            String networkPrefix = localIpAddress.substring(0, localIpAddress.lastIndexOf(".") + 1);
            List<Thread> scanThreads = new ArrayList<>();
            
            for (int i = 1; i <= 254; i++) {
                final String testIp = networkPrefix + i;
                
                if (testIp.equals(localIpAddress)) continue;
                
                Thread scanThread = new Thread(() -> {
                    if (isHostReachable(testIp, MESH_PORT, 2000)) {
                        addDiscoveredPeer(testIp);
                    }
                });
                
                scanThreads.add(scanThread);
                scanThread.start();
            }
            
            for (Thread thread : scanThreads) {
                try {
                    thread.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Manual network scan error", e);
        }
    }

    private boolean isMeshNodeAvailable(String ip) {
        try {
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(ip, MESH_PORT), 2000);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isHostReachable(String ip, int port, int timeout) {
        try {
            java.net.Socket socket = new java.net.Socket();
            java.net.SocketAddress socketAddress = new java.net.InetSocketAddress(ip, port);
            socket.connect(socketAddress, timeout);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void onPeerDiscovered(String peerIp) {
        try {
            if (!meshNodes.containsKey(peerIp) || !meshNodes.get(peerIp).isConnected()) {
                // Auto-connect to the new peer
                MeshNode newNode = new MeshNode(peerIp, MESH_PORT);
                if (newNode.connect(5000)) {
                    meshNodes.put(peerIp, newNode);
                    
                    Log.i(TAG, "✅ Auto-connected to mesh node: " + peerIp);
                    
                    if (meshCallback != null) {
                        meshCallback.onMeshNodeJoined(peerIp, "Auto-discovered node");
                        meshCallback.onPeerConnected(peerIp);
                    }
                    
                    // Add to discovered peers
                    addDiscoveredPeer(peerIp);
                    
                    // Send welcome message
                    sendWelcomeMessage(newNode);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to discovered peer: " + peerIp, e);
        }
    }

    private void addDiscoveredPeer(String ip) {
        DiscoveredPeer peer = discoveredPeers.get(ip);
        if (peer != null) {
            peer.discoveredAt = System.currentTimeMillis();
            peer.isReachable = true;
        } else {
            peer = new DiscoveredPeer(ip);
            discoveredPeers.put(ip, peer);
            
            Log.d(TAG, "Discovered new peer: " + ip);
            
            if (meshCallback != null) {
                meshCallback.onPeerDiscovered(ip, "Peer-" + ip);
            }
        }
    }

    private boolean manualConnectToPeer(String peerIp, int timeout) {
        try {
            if (meshNodes.containsKey(peerIp)) {
                return true; // Already connected
            }
            
            MeshNode newNode = new MeshNode(peerIp, MESH_PORT);
            if (newNode.connect(timeout)) {
                meshNodes.put(peerIp, newNode);
                
                Log.i(TAG, "Manually connected to peer: " + peerIp);
                
                if (meshCallback != null) {
                    meshCallback.onPeerConnected(peerIp);
                    meshCallback.onMeshNodeJoined(peerIp, "Manually connected");
                }
                
                // Add to discovered peers
                addDiscoveredPeer(peerIp);
                
                return true;
            } else {
                // Update discovered peer status
                DiscoveredPeer peer = discoveredPeers.get(peerIp);
                if (peer != null) {
                    peer.failedConnectionAttempts++;
                }
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to peer: " + peerIp, e);
            return false;
        }
    }

    private void startMeshServer() {
        if (meshServer == null) {
            meshServer = new MeshServer();
            new Thread(meshServer).start();
            Log.i(TAG, "Mesh server started on port " + MESH_PORT);
        }
    }

    private void addSelfToMesh() {
        MeshNode selfNode = new MeshNode(localIpAddress, MESH_PORT);
        selfNode.setConnected(true);
        selfNode.setConnectionType("self");
        meshNodes.put(localIpAddress, selfNode);
    }

    private void cleanupDisconnectedNodes() {
        List<String> disconnectedNodes = new ArrayList<>();
        
        for (String nodeIp : meshNodes.keySet()) {
            MeshNode node = meshNodes.get(nodeIp);
            if (!node.isConnected() && !nodeIp.equals(localIpAddress)) {
                disconnectedNodes.add(nodeIp);
            }
        }
        
        for (String nodeIp : disconnectedNodes) {
            meshNodes.remove(nodeIp);
            if (meshCallback != null) {
                meshCallback.onMeshNodeLeft(nodeIp);
                meshCallback.onPeerDisconnected(nodeIp);
            }
            Log.i(TAG, "Node removed from mesh: " + nodeIp);
        }
    }

    private void cleanupOldPeers() {
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
        discoveredPeers.entrySet().removeIf(entry -> 
            entry.getValue().discoveredAt < fiveMinutesAgo && 
            !entry.getValue().isReachable &&
            !meshNodes.containsKey(entry.getKey())
        );
    }

    private void cleanupStaleNodes() {
        long tenMinutesAgo = System.currentTimeMillis() - (10 * 60 * 1000);
        for (MeshNode node : meshNodes.values()) {
            if (!node.getIpAddress().equals(localIpAddress) && 
                node.getLastSeen() < tenMinutesAgo) {
                node.disconnect();
                meshNodes.remove(node.getIpAddress());
                Log.i(TAG, "Removed stale node: " + node.getIpAddress());
            }
        }
    }

    private void verifyConnectedPeers() {
        for (MeshNode node : meshNodes.values()) {
            if (!node.getIpAddress().equals(localIpAddress) && 
                (!node.isConnected() || !isHostReachable(node.getIpAddress(), MESH_PORT, 1000))) {
                node.disconnect();
                meshNodes.remove(node.getIpAddress());
                
                if (meshCallback != null) {
                    meshCallback.onPeerDisconnected(node.getIpAddress());
                    meshCallback.onMeshNodeLeft(node.getIpAddress());
                }
            }
        }
    }

    private void sendWelcomeMessage(MeshNode newNode) {
        try {
            JSONObject welcomeMsg = new JSONObject();
            welcomeMsg.put("type", "welcome");
            welcomeMsg.put("from", localIpAddress);
            welcomeMsg.put("network", getCurrentSSID());
            welcomeMsg.put("timestamp", System.currentTimeMillis());
            welcomeMsg.put("message", "Welcome to the auto-mesh network!");
            
            newNode.sendMessage(welcomeMsg.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error sending welcome message", e);
        }
    }

    // Statistics methods
    private int getActiveConnectionCount() {
        int count = 0;
        for (MeshNode node : meshNodes.values()) {
            if (node.isConnected() && !node.getIpAddress().equals(localIpAddress)) {
                count++;
            }
        }
        return count;
    }

    private long getDiscoveryCycleCount() {
        // This would track actual discovery cycles
        return (System.currentTimeMillis() - meshStartTime) / (DISCOVERY_INTERVAL * 1000);
    }

    // Utility methods
    public boolean isWifiConnected() {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo != null && networkInfo.isConnected();
    }

    public String getCurrentSSID() {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                return ssid.replace("\"", "");
            }
        }
        return "unknown";
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting local IP", e);
        }
        return null;
    }

    public void setMeshCallback(AutoMeshCallback callback) {
        this.meshCallback = callback;
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    private class MeshServer implements Runnable {
        private java.net.ServerSocket serverSocket;
        private boolean running = false;
        private List<ClientHandler> clientHandlers = new ArrayList<>();

        @Override
        public void run() {
            try {
                serverSocket = new java.net.ServerSocket(MESH_PORT);
                running = true;
                Log.i(TAG, "🔄 Mesh server listening for incoming connections...");

                while (running) {
                    java.net.Socket clientSocket = serverSocket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    
                    handleIncomingConnection(clientSocket, clientIp);
                }
            } catch (Exception e) {
                if (running) {
                    Log.e(TAG, "Mesh server error", e);
                }
            }
        }

        private void handleIncomingConnection(java.net.Socket socket, String clientIp) {
            try {
                // Create node for incoming connection
                MeshNode incomingNode = new MeshNode(socket);
                meshNodes.put(clientIp, incomingNode);
                
                Log.i(TAG, "✅ Incoming mesh connection from: " + clientIp);
                
                if (meshCallback != null) {
                    meshCallback.onMeshNodeJoined(clientIp, "Incoming connection");
                    meshCallback.onPeerConnected(clientIp);
                }
                
                // Add to discovered peers
                addDiscoveredPeer(clientIp);
                
                // Start message listener
                ClientHandler clientHandler = new ClientHandler(incomingNode, clientIp);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
                
            } catch (Exception e) {
                Log.e(TAG, "Error handling incoming connection", e);
            }
        }

        public void stop() {
            running = false;
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                for (ClientHandler handler : clientHandlers) {
                    handler.stop();
                }
                clientHandlers.clear();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping mesh server", e);
            }
        }

        private class ClientHandler implements Runnable {
            private MeshNode node;
            private String clientIp;
            private boolean running = false;

            public ClientHandler(MeshNode node, String clientIp) {
                this.node = node;
                this.clientIp = clientIp;
            }

            @Override
            public void run() {
                try {
                    java.io.InputStream input = node.getInputStream();
                    running = true;
                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while (running && node.isConnected() && (bytesRead = input.read(buffer)) != -1) {
                        String message = new String(buffer, 0, bytesRead);
                        totalMessagesReceived++;
                        
                        Log.d(TAG, "📨 Message from " + clientIp + ": " + message);
                        
                        if (meshCallback != null) {
                            meshCallback.onMeshMessageReceived(clientIp, message);
                        }
                        
                        // Relay to other nodes (mesh networking)
                        relayMessageToMesh(message, clientIp);
                    }
                } catch (Exception e) {
                    if (running) {
                        Log.e(TAG, "Client handler error for " + clientIp, e);
                    }
                } finally {
                    stop();
                }
            }

            private void relayMessageToMesh(String message, String fromIp) {
                // Broadcast message to all other nodes
                for (MeshNode node : meshNodes.values()) {
                    String nodeIp = node.getIpAddress();
                    if (!nodeIp.equals(fromIp) && !nodeIp.equals(localIpAddress) && node.isConnected()) {
                        node.sendMessage(message);
                    }
                }
            }

            public void stop() {
                running = false;
                clientHandlers.remove(this);
            }
        }
    }

    private class MeshNode {
        private String ipAddress;
        private int port;
        private java.net.Socket socket;
        private boolean connected = false;
        private long lastSeen;
        private int messageCount = 0;
        private String connectionType = "outgoing"; // or "incoming" or "self"
        
        public MeshNode(String ip, int port) {
            this.ipAddress = ip;
            this.port = port;
            this.lastSeen = System.currentTimeMillis();
        }
        
        public MeshNode(java.net.Socket socket) {
            this.socket = socket;
            this.ipAddress = socket.getInetAddress().getHostAddress();
            this.connected = true;
            this.lastSeen = System.currentTimeMillis();
            this.connectionType = "incoming";
        }
        
        public boolean connect(int timeout) {
            try {
                socket = new java.net.Socket();
                socket.connect(new java.net.InetSocketAddress(ipAddress, port), timeout);
                connected = true;
                lastSeen = System.currentTimeMillis();
                return true;
            } catch (Exception e) {
                connected = false;
                return false;
            }
        }
        
        public boolean sendMessage(String message) {
            return sendMessage(message, 5000);
        }
        
        public boolean sendMessage(String message, int timeout) {
            try {
                if (socket != null && connected) {
                    socket.setSoTimeout(timeout);
                    java.io.OutputStream output = socket.getOutputStream();
                    output.write(message.getBytes());
                    output.flush();
                    messageCount++;
                    lastSeen = System.currentTimeMillis();
                    return true;
                }
            } catch (Exception e) {
                connected = false;
                Log.e(TAG, "Error sending message to " + ipAddress, e);
            }
            return false;
        }
        
        public java.io.InputStream getInputStream() throws Exception {
            return socket != null ? socket.getInputStream() : null;
        }
        
        public void disconnect() {
            connected = false;
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting node " + ipAddress, e);
            }
        }
        
        // Getters and setters
        public String getIpAddress() { return ipAddress; }
        public boolean isConnected() { return connected; }
        public long getLastSeen() { return lastSeen; }
        public int getMessageCount() { return messageCount; }
        public String getConnectionType() { return connectionType; }
        public void setConnected(boolean connected) { this.connected = connected; }
        public void setConnectionType(String type) { this.connectionType = type; }
    }
}