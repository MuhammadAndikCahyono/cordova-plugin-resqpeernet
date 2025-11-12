package org.apache.cordova.resqpeernet.modules;

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
    
    private Context context;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    
    private boolean isAutoMeshRunning = false;
    private ScheduledExecutorService scheduler;
    private MeshServer meshServer;
    private ConcurrentHashMap<String, MeshNode> meshNodes = new ConcurrentHashMap<>();
    private String localIpAddress;
    
    // Auto-mesh callback
    public interface AutoMeshCallback {
        void onMeshNodeJoined(String nodeIp, String nodeInfo);
        void onMeshNodeLeft(String nodeIp);
        void onMeshMessageReceived(String fromIp, String message);
        void onMeshNetworkReady(int nodeCount);
        void onAutoDiscoveryStarted();
    }
    
    private AutoMeshCallback meshCallback;

    public AutoLocalMeshManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    /**
     * START AUTO-MESH - One method to rule them all!
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
            
            // 1. Start mesh server
            startMeshServer();
            
            // 2. Start continuous auto-discovery
            startAutoDiscovery();
            
            // 3. Add self to mesh
            addSelfToMesh();
            
            isAutoMeshRunning = true;
            
            JSONObject result = new JSONObject();
            result.put("status", "auto_mesh_started");
            result.put("localIp", localIpAddress);
            result.put("network", getCurrentSSID());
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
     * STOP auto-mesh network
     */
    public void stopAutoMesh(CallbackContext callbackContext) {
        try {
            isAutoMeshRunning = false;
            
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }
            
            if (meshServer != null) {
                meshServer.stop();
                meshServer = null;
            }
            
            // Disconnect all nodes
            for (MeshNode node : meshNodes.values()) {
                node.disconnect();
            }
            meshNodes.clear();
            
            JSONObject result = new JSONObject();
            result.put("status", "auto_mesh_stopped");
            result.put("nodesDisconnected", meshNodes.size());
            
            callbackContext.success(result);
            Log.i(TAG, "Auto-mesh stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping auto-mesh", e);
            callbackContext.error("Error stopping auto-mesh: " + e.getMessage());
        }
    }

    /**
     * BROADCAST message to entire mesh network
     */
    public void broadcastToMesh(JSONObject args, CallbackContext callbackContext) {
        try {
            String message = args.getString("message");
            String messageType = args.optString("type", "broadcast");
            
            int recipients = 0;
            for (MeshNode node : meshNodes.values()) {
                if (node.isConnected()) {
                    node.sendMessage(message);
                    recipients++;
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("status", "broadcast_sent");
            result.put("recipients", recipients);
            result.put("totalNodes", meshNodes.size());
            result.put("messageType", messageType);
            
            callbackContext.success(result);
            Log.i(TAG, "📢 Broadcast to " + recipients + " nodes: " + message);
            
        } catch (Exception e) {
            Log.e(TAG, "Error broadcasting to mesh", e);
            callbackContext.error("Error broadcasting: " + e.getMessage());
        }
    }

    /**
     * GET complete mesh status
     */
    public void getMeshStatus(CallbackContext callbackContext) {
        try {
            JSONObject status = new JSONObject();
            status.put("isActive", isAutoMeshRunning);
            status.put("localIp", localIpAddress);
            status.put("network", getCurrentSSID());
            status.put("totalNodes", meshNodes.size());
            status.put("startTime", System.currentTimeMillis());
            
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

    // =========================================================================
    // PRIVATE AUTO-MESH METHODS
    // =========================================================================

    private void startAutoDiscovery() {
        // Schedule continuous network scanning
        scheduler.scheduleAtFixedRate(() -> {
            if (!isAutoMeshRunning) return;
            
            try {
                scanNetworkForPeers();
                cleanupDisconnectedNodes();
                
                Log.d(TAG, "🔄 Auto-scan completed. Active nodes: " + meshNodes.size());
                
            } catch (Exception e) {
                Log.e(TAG, "Auto-discovery error", e);
            }
        }, 0, DISCOVERY_INTERVAL, TimeUnit.SECONDS);
        
        Log.i(TAG, "Auto-discovery scheduler started");
    }

    private void scanNetworkForPeers() {
        try {
            String networkPrefix = localIpAddress.substring(0, localIpAddress.lastIndexOf(".") + 1);
            
            // Scan IP range concurrently
            List<Thread> scanThreads = new ArrayList<>();
            
            for (int i = 1; i <= 254; i++) {
                final String testIp = networkPrefix + i;
                
                // Skip self
                if (testIp.equals(localIpAddress)) continue;
                
                // Skip already connected nodes
                if (meshNodes.containsKey(testIp) && meshNodes.get(testIp).isConnected()) {
                    continue;
                }
                
                Thread scanThread = new Thread(() -> {
                    if (isMeshNodeAvailable(testIp)) {
                        onPeerDiscovered(testIp);
                    }
                });
                
                scanThreads.add(scanThread);
                scanThread.start();
            }
            
            // Wait for all scans to complete
            for (Thread thread : scanThreads) {
                try {
                    thread.join(100); // Timeout 100ms per thread
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Network scan error", e);
        }
    }

    private boolean isMeshNodeAvailable(String ip) {
        try {
            // Try to connect to mesh port
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(ip, MESH_PORT), 2000); // 2 second timeout
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void onPeerDiscovered(String peerIp) {
        try {
            if (!meshNodes.containsKey(peerIp) || !meshNodes.get(peerIp).isConnected()) {
                // Connect to the new peer
                MeshNode newNode = new MeshNode(peerIp, MESH_PORT);
                if (newNode.connect()) {
                    meshNodes.put(peerIp, newNode);
                    
                    Log.i(TAG, "✅ Auto-connected to mesh node: " + peerIp);
                    
                    if (meshCallback != null) {
                        meshCallback.onMeshNodeJoined(peerIp, "Auto-discovered node");
                    }
                    
                    // Send welcome message
                    sendWelcomeMessage(newNode);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to discovered peer: " + peerIp, e);
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
        selfNode.setConnected(true); // Mark as self
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
            }
            Log.i(TAG, "Node removed from mesh: " + nodeIp);
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

    // Utility methods
    private boolean isWifiConnected() {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo != null && networkInfo.isConnected();
    }

    private String getCurrentSSID() {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            return ssid.replace("\"", "");
        }
        return "unknown";
    }

    private String getLocalIpAddress() {
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

    // Mesh Server for accepting incoming connections
    private class MeshServer implements Runnable {
        private java.net.ServerSocket serverSocket;
        private boolean running = false;

        @Override
        public void run() {
            try {
                serverSocket = new java.net.ServerSocket(MESH_PORT);
                running = true;
                Log.i(TAG, "🔄 Mesh server listening for incoming connections...");

                while (running) {
                    java.net.Socket clientSocket = serverSocket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    
                    // Handle new connection
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
                // Add to mesh nodes
                MeshNode incomingNode = new MeshNode(socket);
                meshNodes.put(clientIp, incomingNode);
                
                Log.i(TAG, "✅ Incoming mesh connection from: " + clientIp);
                
                if (meshCallback != null) {
                    meshCallback.onMeshNodeJoined(clientIp, "Incoming connection");
                }
                
                // Start message listener for this node
                new Thread(() -> listenForMessages(incomingNode, clientIp)).start();
                
            } catch (Exception e) {
                Log.e(TAG, "Error handling incoming connection", e);
            }
        }

        private void listenForMessages(MeshNode node, String fromIp) {
            try {
                java.io.InputStream input = node.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;

                while (node.isConnected() && (bytesRead = input.read(buffer)) != -1) {
                    String message = new String(buffer, 0, bytesRead);
                    
                    Log.d(TAG, "📨 Message from " + fromIp + ": " + message);
                    
                    if (meshCallback != null) {
                        meshCallback.onMeshMessageReceived(fromIp, message);
                    }
                    
                    // Optional: Broadcast to other nodes (mesh relay)
                    relayMessageToMesh(message, fromIp);
                }
            } catch (Exception e) {
                if (node.isConnected()) {
                    Log.e(TAG, "Message listener error for " + fromIp, e);
                }
            } finally {
                node.disconnect();
                meshNodes.remove(fromIp);
                if (meshCallback != null) {
                    meshCallback.onMeshNodeLeft(fromIp);
                }
            }
        }

        private void relayMessageToMesh(String message, String fromIp) {
            // Broadcast message to all other nodes (mesh networking)
            for (String nodeIp : meshNodes.keySet()) {
                if (!nodeIp.equals(fromIp) && !nodeIp.equals(localIpAddress)) {
                    MeshNode node = meshNodes.get(nodeIp);
                    if (node.isConnected()) {
                        node.sendMessage(message);
                    }
                }
            }
        }

        public void stop() {
            running = false;
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping mesh server", e);
            }
        }
    }

    // Mesh Node representation
    private class MeshNode {
        private String ipAddress;
        private int port;
        private java.net.Socket socket;
        private boolean connected = false;
        private long lastSeen;
        
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
        }
        
        public boolean connect() {
            try {
                socket = new java.net.Socket(ipAddress, port);
                connected = true;
                lastSeen = System.currentTimeMillis();
                return true;
            } catch (Exception e) {
                connected = false;
                return false;
            }
        }
        
        public void sendMessage(String message) {
            try {
                if (connected && socket != null) {
                    java.io.OutputStream output = socket.getOutputStream();
                    output.write(message.getBytes());
                    output.flush();
                    lastSeen = System.currentTimeMillis();
                }
            } catch (Exception e) {
                connected = false;
                Log.e(TAG, "Error sending message to " + ipAddress, e);
            }
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
        
        // Getters
        public String getIpAddress() { return ipAddress; }
        public boolean isConnected() { return connected; }
        public long getLastSeen() { return lastSeen; }
        public void setConnected(boolean connected) { this.connected = connected; }
    }
}