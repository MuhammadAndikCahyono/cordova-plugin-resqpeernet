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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocalNetworkMeshManager {
    private static final String TAG = "LocalNetworkMesh";
    private static final int MESH_PORT = 8888;
    private static final int DISCOVERY_INTERVAL = 10; // seconds
    
    private Context context;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private ScheduledExecutorService scheduler;
    
    private boolean isMeshActive = false;
    private MeshServer meshServer;
    private ConcurrentHashMap<String, MeshClient> connectedClients = new ConcurrentHashMap<>();
    private List<DiscoveredPeer> discoveredPeers = new ArrayList<>();
    private String localIpAddress;
	
    // Callback interface
    public interface LocalMeshCallback {
        void onPeerDiscovered(String peerIp, String peerName);
        void onPeerConnected(String peerIp);
        void onPeerDisconnected(String peerIp);
        void onMessageReceived(String fromIp, String message);
        void onNetworkStatusChanged(boolean isConnected);
    }
    
    private LocalMeshCallback meshCallback;

    // Discovered peer class
    private class DiscoveredPeer {
        String ipAddress;
        String hostname;
        long discoveredAt;
        boolean isReachable;
        
        DiscoveredPeer(String ip) {
            this.ipAddress = ip;
            this.hostname = "Unknown";
            this.discoveredAt = System.currentTimeMillis();
            this.isReachable = true;
        }
    }

    public LocalNetworkMeshManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.scheduler = Executors.newScheduledThreadPool(3);
    }

    /**
     * Initialize local network mesh
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
            
            // Start mesh server
            startMeshServer();
            
            // Start background discovery
            startBackgroundDiscovery();
            
            isMeshActive = true;
            
            JSONObject result = new JSONObject();
            result.put("status", "initialized");
            result.put("localIp", localIpAddress);
            result.put("networkSsid", getCurrentSSID());
            result.put("message", "Local network mesh initialized successfully");
            
            callbackContext.success(result);
            Log.i(TAG, "Local network mesh initialized on IP: " + localIpAddress);
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing local mesh", e);
            callbackContext.error("Error initializing local mesh: " + e.getMessage());
        }
    }

    /**
     * Discover peers in local network - MANUAL TRIGGER
     */
    public void discoverPeers(CallbackContext callbackContext) {
        try {
            if (!isMeshActive) {
                callbackContext.error("Mesh not initialized. Call initializeMesh first.");
                return;
            }
            
            Log.i(TAG, "Starting manual peer discovery...");
            
            // Clear old peers and do fresh scan
            discoveredPeers.clear();
            scanLocalNetwork();
            
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
                    
                    // Notify via callback if needed
                    if (meshCallback != null) {
                        for (DiscoveredPeer peer : discoveredPeers) {
                            meshCallback.onPeerDiscovered(peer.ipAddress, peer.hostname);
                        }
                    }
                    
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating discovery result", e);
                }
            }, 5, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting peer discovery", e);
            callbackContext.error("Error starting discovery: " + e.getMessage());
        }
    }

    /**
     * Get list of discovered peers
     */
    public void getDiscoveredPeers(CallbackContext callbackContext) {
        try {
            JSONArray peersArray = new JSONArray();
            
            // Clean up old peers (older than 5 minutes)
            cleanupOldPeers();
            
            for (DiscoveredPeer peer : discoveredPeers) {
                JSONObject peerInfo = new JSONObject();
                peerInfo.put("ipAddress", peer.ipAddress);
                peerInfo.put("hostname", peer.hostname);
                peerInfo.put("discoveredAt", peer.discoveredAt);
                peerInfo.put("isReachable", peer.isReachable);
                peerInfo.put("isConnected", connectedClients.containsKey(peer.ipAddress));
                peerInfo.put("ageSeconds", (System.currentTimeMillis() - peer.discoveredAt) / 1000);
                
                peersArray.put(peerInfo);
            }
            
            JSONObject result = new JSONObject();
            result.put("peers", peersArray);
            result.put("count", discoveredPeers.size());
            result.put("connectedCount", connectedClients.size());
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
     * Auto connect to discovered peers
     */
    public void autoConnectPeers(CallbackContext callbackContext) {
        try {
            if (!isMeshActive) {
                callbackContext.error("Mesh not initialized");
                return;
            }
            
            Log.i(TAG, "Starting auto-connect to discovered peers...");
            
            int connectionAttempts = 0;
            int successfulConnections = 0;
            List<String> failedConnections = new ArrayList<>();
            
            for (DiscoveredPeer peer : discoveredPeers) {
                if (!connectedClients.containsKey(peer.ipAddress) && peer.isReachable) {
                    connectionAttempts++;
                    if (connectToPeer(peer.ipAddress)) {
                        successfulConnections++;
                    } else {
                        failedConnections.add(peer.ipAddress);
                        peer.isReachable = false; // Mark as unreachable
                    }
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("status", "auto_connect_completed");
            result.put("attempts", connectionAttempts);
            result.put("successful", successfulConnections);
            result.put("failed", failedConnections.size());
            result.put("totalConnected", connectedClients.size());
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
     * Send message to local network mesh
     */
    public void sendMeshMessage(JSONObject args, CallbackContext callbackContext) {
        try {
            if (!isMeshActive) {
                callbackContext.error("Mesh not initialized");
                return;
            }
            
            String targetIp = args.optString("targetIp", ""); // Empty for broadcast
            String message = args.getString("message");
            String messageType = args.optString("messageType", "text");
            int timeout = args.optInt("timeout", 5000);
            
            if (connectedClients.isEmpty()) {
                callbackContext.error("No connected peers to send message to");
                return;
            }
            
            int recipients = 0;
            List<String> failedSends = new ArrayList<>();
            
            if (targetIp.isEmpty()) {
                // Broadcast to all connected peers
                for (MeshClient client : connectedClients.values()) {
                    if (client.isConnected()) {
                        boolean sent = client.sendMessage(message, timeout);
                        if (sent) {
                            recipients++;
                        } else {
                            failedSends.add(client.getIpAddress());
                        }
                    }
                }
            } else {
                // Send to specific peer
                MeshClient client = connectedClients.get(targetIp);
                if (client != null && client.isConnected()) {
                    boolean sent = client.sendMessage(message, timeout);
                    if (sent) {
                        recipients = 1;
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
            result.put("timestamp", System.currentTimeMillis());
            
            if (!failedSends.isEmpty()) {
                result.put("failedList", new JSONArray(failedSends));
            }
            
            callbackContext.success(result);
            Log.i(TAG, "Message sent to " + recipients + " recipients");
            
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
            if (!isMeshActive) {
                callbackContext.error("Mesh not initialized");
                return;
            }
            
            JSONObject topology = new JSONObject();
            topology.put("isActive", isMeshActive);
            topology.put("localIp", localIpAddress);
            topology.put("networkSsid", getCurrentSSID());
            topology.put("connectedPeers", connectedClients.size());
            topology.put("discoveredPeers", discoveredPeers.size());
            topology.put("meshPort", MESH_PORT);
            topology.put("uptime", System.currentTimeMillis()); // TODO: Track actual start time
            
            // Connected clients details
            JSONArray connectedArray = new JSONArray();
            for (MeshClient client : connectedClients.values()) {
                JSONObject clientInfo = new JSONObject();
                clientInfo.put("ipAddress", client.getIpAddress());
                clientInfo.put("connected", client.isConnected());
                clientInfo.put("lastActivity", client.getLastActivity());
                clientInfo.put("messageCount", client.getMessageCount());
                connectedArray.put(clientInfo);
            }
            topology.put("connectedClients", connectedArray);
            
            // Discovered peers details
            JSONArray discoveredArray = new JSONArray();
            for (DiscoveredPeer peer : discoveredPeers) {
                JSONObject peerInfo = new JSONObject();
                peerInfo.put("ipAddress", peer.ipAddress);
                peerInfo.put("hostname", peer.hostname);
                peerInfo.put("isReachable", peer.isReachable);
                peerInfo.put("isConnected", connectedClients.containsKey(peer.ipAddress));
                peerInfo.put("ageSeconds", (System.currentTimeMillis() - peer.discoveredAt) / 1000);
                discoveredArray.put(peerInfo);
            }
            topology.put("discoveredPeers", discoveredArray);
            
            // Network stats
            JSONObject stats = new JSONObject();
            stats.put("totalMessagesSent", getTotalMessagesSent());
            stats.put("totalMessagesReceived", getTotalMessagesReceived());
            stats.put("activeConnections", getActiveConnectionCount());
            topology.put("stats", stats);
            
            callbackContext.success(topology);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting mesh topology", e);
            callbackContext.error("Error getting topology: " + e.getMessage());
        }
    }

    /**
     * Stop local mesh network
     */
    public void stopLocalMesh(CallbackContext callbackContext) {
        try {
            Log.i(TAG, "Stopping local mesh network...");
            
            isMeshActive = false;
            
            // Stop scheduler
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }
            
            // Stop mesh server
            if (meshServer != null) {
                meshServer.stop();
                meshServer = null;
            }
            
            // Disconnect all clients
            for (MeshClient client : connectedClients.values()) {
                client.disconnect();
            }
            connectedClients.clear();
            
            // Clear discovered peers
            discoveredPeers.clear();
            
            JSONObject result = new JSONObject();
            result.put("status", "stopped");
            result.put("disconnectedClients", connectedClients.size());
            result.put("message", "Local mesh network stopped successfully");
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.i(TAG, "Local mesh network stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping local mesh", e);
            callbackContext.error("Error stopping local mesh: " + e.getMessage());
        }
    }

    // =========================================================================
    // PRIVATE METHODS
    // =========================================================================

    private void startBackgroundDiscovery() {
        // Schedule continuous network scanning
        scheduler.scheduleAtFixedRate(() -> {
            if (!isMeshActive) return;
            
            try {
                scanLocalNetwork();
                cleanupOldPeers();
                verifyConnectedPeers();
                
                Log.d(TAG, "Background scan: " + discoveredPeers.size() + " peers, " + 
                      connectedClients.size() + " connected");
                
            } catch (Exception e) {
                Log.e(TAG, "Background discovery error", e);
            }
        }, 0, DISCOVERY_INTERVAL, TimeUnit.SECONDS);
    }

    private void scanLocalNetwork() {
        try {
            String networkPrefix = localIpAddress.substring(0, localIpAddress.lastIndexOf(".") + 1);
            List<Thread> scanThreads = new ArrayList<>();
            
            // Scan IP range 1-254
            for (int i = 1; i <= 254; i++) {
                final String testIp = networkPrefix + i;
                
                // Skip self
                if (testIp.equals(localIpAddress)) continue;
                
                Thread scanThread = new Thread(() -> {
                    if (isHostReachable(testIp, MESH_PORT, 2000)) {
                        addDiscoveredPeer(testIp);
                    }
                });
                
                scanThreads.add(scanThread);
                scanThread.start();
            }
            
            // Wait for scans to complete with timeout
            for (Thread thread : scanThreads) {
                try {
                    thread.join(500); // 500ms timeout per thread
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Network scan error", e);
        }
    }

    private void addDiscoveredPeer(String ip) {
        // Check if peer already exists
        for (DiscoveredPeer peer : discoveredPeers) {
            if (peer.ipAddress.equals(ip)) {
                peer.discoveredAt = System.currentTimeMillis();
                peer.isReachable = true;
                return;
            }
        }
        
        // Add new peer
        DiscoveredPeer newPeer = new DiscoveredPeer(ip);
        discoveredPeers.add(newPeer);
        
        Log.d(TAG, "Discovered new peer: " + ip);
        
        // Notify callback
        if (meshCallback != null) {
            meshCallback.onPeerDiscovered(ip, "Peer-" + ip);
        }
    }

    private void cleanupOldPeers() {
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
        discoveredPeers.removeIf(peer -> peer.discoveredAt < fiveMinutesAgo && !peer.isReachable);
    }

    private void verifyConnectedPeers() {
        // Check if connected peers are still reachable
        for (MeshClient client : connectedClients.values()) {
            if (!client.isConnected() || !isHostReachable(client.getIpAddress(), MESH_PORT, 1000)) {
                client.disconnect();
                connectedClients.remove(client.getIpAddress());
                
                if (meshCallback != null) {
                    meshCallback.onPeerDisconnected(client.getIpAddress());
                }
            }
        }
    }

    private boolean connectToPeer(String peerIp) {
        try {
            if (connectedClients.containsKey(peerIp)) {
                return true; // Already connected
            }
            
            MeshClient client = new MeshClient(peerIp, MESH_PORT);
            if (client.connect(5000)) { // 5 second timeout
                connectedClients.put(peerIp, client);
                
                Log.i(TAG, "Connected to peer: " + peerIp);
                
                if (meshCallback != null) {
                    meshCallback.onPeerConnected(peerIp);
                }
                
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to peer: " + peerIp, e);
        }
        return false;
    }

    private void startMeshServer() {
        if (meshServer == null) {
            meshServer = new MeshServer();
            new Thread(meshServer).start();
            Log.i(TAG, "Mesh server started on port " + MESH_PORT);
        }
    }

    // Utility methods
    public boolean isWifiConnected() {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo != null && networkInfo.isConnected();
    }
	
	public String getCurrentSSID() {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            return ssid.replace("\"", "");
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
        } catch (SocketException e) {
            Log.e(TAG, "Error getting local IP", e);
        }
        return null;
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

    // Statistics methods
    private int getTotalMessagesSent() {
        int total = 0;
        for (MeshClient client : connectedClients.values()) {
            total += client.getMessageCount();
        }
        return total;
    }

    private int getTotalMessagesReceived() {
        // This would need to be tracked in MeshServer
        return 0; // Placeholder
    }

    private int getActiveConnectionCount() {
        int count = 0;
        for (MeshClient client : connectedClients.values()) {
            if (client.isConnected()) count++;
        }
        return count;
    }

    public void setMeshCallback(LocalMeshCallback callback) {
        this.meshCallback = callback;
    }

    // Mesh Server Class
    private class MeshServer implements Runnable {
        private java.net.ServerSocket serverSocket;
        private boolean running = false;
        private List<ClientHandler> clientHandlers = new ArrayList<>();

        @Override
        public void run() {
            try {
                serverSocket = new java.net.ServerSocket(MESH_PORT);
                running = true;
                Log.i(TAG, "Mesh server listening on port " + MESH_PORT);

                while (running) {
                    java.net.Socket clientSocket = serverSocket.accept();
                    String clientIp = clientSocket.getInetAddress().getHostAddress();
                    
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clientHandlers.add(clientHandler);
                    new Thread(clientHandler).start();
                    
                    Log.i(TAG, "New client connected: " + clientIp);
                }
            } catch (Exception e) {
                if (running) {
                    Log.e(TAG, "Mesh server error", e);
                }
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
            private java.net.Socket clientSocket;
            private boolean running = false;

            public ClientHandler(java.net.Socket socket) {
                this.clientSocket = socket;
            }

            @Override
            public void run() {
                try {
                    java.io.InputStream input = clientSocket.getInputStream();
                    running = true;

                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while (running && (bytesRead = input.read(buffer)) != -1) {
                        String message = new String(buffer, 0, bytesRead);
                        String fromIp = clientSocket.getInetAddress().getHostAddress();
                        
                        Log.d(TAG, "Received message from " + fromIp + ": " + message);
                        
                        if (meshCallback != null) {
                            meshCallback.onMessageReceived(fromIp, message);
                        }
                    }
                } catch (Exception e) {
                    if (running) {
                        Log.e(TAG, "Client handler error", e);
                    }
                } finally {
                    stop();
                }
            }

            public void stop() {
                running = false;
                try {
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                    clientHandlers.remove(this);
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping client handler", e);
                }
            }
        }
    }

    // Mesh Client Class
    private class MeshClient {
        private String serverIp;
        private int serverPort;
        private java.net.Socket socket;
        private boolean connected = false;
        private long lastActivity;
        private int messageCount = 0;

        public MeshClient(String ip, int port) {
            this.serverIp = ip;
            this.serverPort = port;
            this.lastActivity = System.currentTimeMillis();
        }

        public boolean connect(int timeout) {
            try {
                socket = new java.net.Socket();
                socket.connect(new java.net.InetSocketAddress(serverIp, serverPort), timeout);
                connected = true;
                lastActivity = System.currentTimeMillis();
                return true;
            } catch (Exception e) {
                connected = false;
                return false;
            }
        }

        public boolean sendMessage(String message, int timeout) {
            try {
                if (socket != null && connected) {
                    java.io.OutputStream output = socket.getOutputStream();
                    output.write(message.getBytes());
                    output.flush();
                    messageCount++;
                    lastActivity = System.currentTimeMillis();
                    return true;
                }
            } catch (Exception e) {
                connected = false;
                Log.e(TAG, "Error sending message to " + serverIp, e);
            }
            return false;
        }

        public void disconnect() {
            connected = false;
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting from " + serverIp, e);
            }
        }

        // Getters
        public String getIpAddress() { return serverIp; }
        public boolean isConnected() { return connected; }
        public long getLastActivity() { return lastActivity; }
        public int getMessageCount() { return messageCount; }
    }
}