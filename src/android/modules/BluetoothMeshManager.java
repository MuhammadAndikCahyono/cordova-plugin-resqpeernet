package org.apache.cordova.resqpeernet.modules;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BluetoothMeshManager {

    private static final String TAG = "BluetoothMeshManager";
    private static final UUID MESH_UUID = UUID.fromString("12345678-1234-5678-9abc-123456789abd");
    
    // Configuration constants
    private static final int CONNECTION_TIMEOUT_MS = 15000;
    private static final int HEARTBEAT_INTERVAL_MS = 30000;
    private static final int NODE_TIMEOUT_MS = 120000;
    private static final int DISCOVERY_DURATION_MS = 30000;
    private static final int MESSAGE_ACK_TIMEOUT_MS = 5000;

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final ExecutorService meshExecutor;
    private final ScheduledExecutorService scheduler;
    
    // Mesh network structure
    private final Map<String, MeshNode> meshNodes;
    private final Map<String, BluetoothSocket> nodeConnections;
    private final Map<String, Message> pendingAcks;
    private final List<Message> messageQueue;
    
    private String localNodeId;
    private AtomicBoolean isMeshActive = new AtomicBoolean(false);
    private int maxHops = 5;
    private CallbackContext meshEventCallback;
    private BroadcastReceiver discoveryReceiver;

    public BluetoothMeshManager(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.meshExecutor = Executors.newCachedThreadPool();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.meshNodes = new ConcurrentHashMap<>();
        this.nodeConnections = new ConcurrentHashMap<>();
        this.pendingAcks = new ConcurrentHashMap<>();
        this.messageQueue = new ArrayList<>();
        this.localNodeId = generateNodeId();
    }

    public void initializeMesh(final CallbackContext callbackContext) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            sendError(callbackContext, "Bluetooth not available or disabled");
            return;
        }

        meshExecutor.execute(() -> {
            try {
                Log.d(TAG, "🚀 INITIALIZE MESH: Starting mesh initialization...");
                
                // Bersihkan nodes sebelum initialize
                meshNodes.clear();
                nodeConnections.clear();

                // Register local node
                MeshNode localNode = new MeshNode(localNodeId, getDeviceName(), 0);
                localNode.setConnected(true);
                localNode.setLastSeen(System.currentTimeMillis());
                meshNodes.put(localNodeId, localNode);
                
                isMeshActive.set(true);
                
                // Start mesh services
                startMeshServer();
                startMessageProcessor();
                startHeartbeatService();
                startAckCleanupService();
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "Mesh network initialized");
                result.put("nodeId", localNodeId);
                result.put("status", "active");
                result.put("totalNodes", 1);
                result.put("neighbors", new JSONArray());
                
                Log.i(TAG, "✅ INITIALIZE MESH: Mesh initialized successfully");
                sendSuccess(callbackContext, result);
                sendMeshEvent("MESH_STARTED", "Mesh network started with node: " + localNodeId);
                
            } catch (Exception e) {
                Log.e(TAG, "❌ INITIALIZE MESH: Failed", e);
                sendError(callbackContext, "Mesh initialization failed: " + e.getMessage());
            }
        });
    }

    public void joinMesh(final String gatewayNodeId, final CallbackContext callbackContext) {
        meshExecutor.execute(() -> {
            try {
                Log.d(TAG, "🎯 JOIN MESH: Attempting to connect to gateway: " + gatewayNodeId);
                
                boolean connected = connectToNodeWithTimeout(gatewayNodeId, CONNECTION_TIMEOUT_MS);
                
                if (connected) {
                    Log.i(TAG, "✅ JOIN MESH: Connected to gateway: " + gatewayNodeId);
                    
                    // Perbaiki neighbor relationships
                    fixNeighborRelationships();
                    
                    // Kirim join message
                    Message joinMessage = new Message(
                        localNodeId,
                        "BROADCAST", 
                        "NODE_JOIN",
                        new JSONObject()
                            .put("nodeId", localNodeId)
                            .put("nodeName", getDeviceName())
                            .put("hopCount", 0)
                            .toString(),
                        0
                    );
                    
                    int sentCount = broadcastMessage(joinMessage);
                    Log.d(TAG, "✅ JOIN MESH: Join message sent to " + sentCount + " nodes");
                    
                    // Pastikan topology updated
                    debugConnectionStatus();
                    
                    JSONObject result = new JSONObject();
                    result.put("success", true);
                    result.put("message", "Joined mesh network via " + gatewayNodeId);
                    result.put("nodeId", localNodeId);
                    result.put("gateway", gatewayNodeId);
                    result.put("status", "connected");
                    result.put("nodeCount", meshNodes.size());
                    result.put("neighbors", new JSONArray(meshNodes.get(localNodeId).getNeighbors()));
                    
                    sendSuccess(callbackContext, result);
                    sendMeshEvent("MESH_JOINED", "Joined mesh via gateway: " + gatewayNodeId);
                    
                } else {
                    Log.e(TAG, "❌ JOIN MESH: Failed to connect to gateway: " + gatewayNodeId);
                    sendError(callbackContext, "Failed to connect to gateway node: " + gatewayNodeId);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "💥 JOIN MESH: Failed", e);
                sendError(callbackContext, "Join mesh failed: " + e.getMessage());
            }
        });
    }

    public void sendMeshMessage(final String targetNodeId, final String message, 
                               final CallbackContext callbackContext) {
        meshExecutor.execute(() -> {
            try {
                Log.d(TAG, "📨 SEND MESH MESSAGE: Target: " + targetNodeId + ", Message: " + message);
                
                // Debug status terlebih dahulu
                debugConnectionStatus();
                
                String messageId = UUID.randomUUID().toString();
                Message meshMessage = new Message(
                    localNodeId,
                    targetNodeId,
                    "USER_MESSAGE",
                    message,
                    0,
                    messageId,
                    true
                );
                
                // Cek jika target adalah local node
                if (targetNodeId.equals(localNodeId)) {
                    Log.d(TAG, "📍 SEND MESSAGE: Target is local node, delivering locally");
                    deliverMessageToApp(meshMessage);
                    
                    JSONObject result = new JSONObject();
                    result.put("success", true);
                    result.put("message", "Message delivered to local node");
                    result.put("targetNode", targetNodeId);
                    result.put("hopCount", 0);
                    result.put("messageId", messageId);
                    
                    sendSuccess(callbackContext, result);
                    return;
                }
                
                // Cek jika target adalah gateway (direct connection)
                if (nodeConnections.containsKey(targetNodeId)) {
                    Log.d(TAG, "📍 SEND MESSAGE: Target has direct connection, sending directly");
                    boolean sent = sendMessageToNodeWithAck(meshMessage, targetNodeId);
                    
                    JSONObject result = new JSONObject();
                    result.put("success", sent);
                    result.put("targetNode", targetNodeId);
                    result.put("hopCount", 1);
                    result.put("sent", sent);
                    result.put("messageId", messageId);
                    result.put("routeType", "direct");
                    
                    if (sent) {
                        sendSuccess(callbackContext, result);
                        sendMeshEvent("MESSAGE_SENT", 
                            "Direct message to " + targetNodeId);
                    } else {
                        sendError(callbackContext, "Failed to send direct message to " + targetNodeId);
                    }
                    return;
                }
                
                // Find route to target menggunakan Dijkstra
                Log.d(TAG, "📍 SEND MESSAGE: Finding route to " + targetNodeId);
                List<String> route = findRouteToNode(targetNodeId);
                
                Log.d(TAG, "📍 SEND MESSAGE: Route to " + targetNodeId + ": " + 
                    (route != null ? route : "NO ROUTE"));
                
                if (route != null && route.size() >= 2) {
                    boolean sent = sendMessageViaRouteWithAck(meshMessage, route);
                    
                    JSONObject result = new JSONObject();
                    result.put("success", sent);
                    result.put("targetNode", targetNodeId);
                    result.put("route", new JSONArray(route));
                    result.put("hopCount", route.size() - 1);
                    result.put("sent", sent);
                    result.put("messageId", messageId);
                    result.put("routeType", "multi-hop");
                    
                    if (sent) {
                        sendSuccess(callbackContext, result);
                        sendMeshEvent("MESSAGE_SENT", 
                            "Message to " + targetNodeId + " via " + (route.size() - 1) + " hops");
                    } else {
                        sendError(callbackContext, "Failed to send message via route to " + targetNodeId);
                    }
                    
                } else {
                    Log.e(TAG, "❌ SEND MESSAGE: No route to target node: " + targetNodeId);
                    
                    // Fallback: coba broadcast
                    Log.d(TAG, "🔄 SEND MESSAGE: Trying broadcast fallback...");
                    Message broadcastMessage = new Message(
                        localNodeId,
                        "BROADCAST",
                        "USER_MESSAGE",
                        new JSONObject()
                            .put("originalTarget", targetNodeId)
                            .put("message", message)
                            .put("messageId", messageId)
                            .toString(),
                        0
                    );
                    
                    int broadcastCount = broadcastMessage(broadcastMessage);
                    
                    JSONObject result = new JSONObject();
                    result.put("success", broadcastCount > 0);
                    result.put("targetNode", targetNodeId);
                    result.put("broadcastReach", broadcastCount);
                    result.put("messageId", messageId);
                    result.put("routeType", "broadcast_fallback");
                    
                    if (broadcastCount > 0) {
                        sendSuccess(callbackContext, result);
                        sendMeshEvent("MESSAGE_BROADCAST", 
                            "Broadcast fallback to " + targetNodeId + " reached " + broadcastCount + " nodes");
                    } else {
                        sendError(callbackContext, "No route and broadcast failed for " + targetNodeId);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "💥 SEND MESSAGE: Failed", e);
                sendError(callbackContext, "Send mesh message failed: " + e.getMessage());
            }
        });
    }

    public void broadcastToMesh(final JSONArray args, final CallbackContext callbackContext) {
        meshExecutor.execute(() -> {
            try {
                Log.d(TAG, "🎯 BROADCAST TO MESH CALLED");
                Log.d(TAG, "   Args length: " + args.length());
                
                String actualMessage = "";
                
                if (args.length() > 0) {
                    Object firstArg = args.get(0);
                    Log.d(TAG, "   First arg type: " + firstArg.getClass().getSimpleName());
                    
                    if (firstArg instanceof JSONObject) {
                        // Format: { message: "content" }
                        JSONObject messageObj = (JSONObject) firstArg;
                        if (messageObj.has("message")) {
                            actualMessage = messageObj.getString("message");
                            Log.d(TAG, "   Extracted message: " + actualMessage);
                        } else {
                            // Tidak ada field message, gunakan toString()
                            actualMessage = messageObj.toString();
                            Log.d(TAG, "   No message field, using object string: " + actualMessage);
                        }
                    } else if (firstArg instanceof String) {
                        // Format: string langsung (fallback)
                        actualMessage = (String) firstArg;
                        Log.d(TAG, "   String argument: " + actualMessage);
                    } else {
                        // Format lain, convert ke string
                        actualMessage = firstArg.toString();
                        Log.d(TAG, "   Other argument type: " + actualMessage);
                    }
                } else {
                    Log.e(TAG, "❌ No arguments provided");
                    sendError(callbackContext, "No message provided");
                    return;
                }
                
                Message broadcastMessage = new Message(
                    localNodeId,
                    "BROADCAST", 
                    "USER_MESSAGE",
                    actualMessage,
                    0
                );
                
                int reachableNodes = broadcastMessage(broadcastMessage);
                
                // Kirim event
                sendMessageSentEvent(broadcastMessage, reachableNodes);
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("reachableNodes", reachableNodes);
                result.put("totalNodes", meshNodes.size());
                result.put("message", "Broadcast completed");
                
                Log.d(TAG, "✅ BROADCAST SUCCESS: " + reachableNodes + " nodes reached");
                sendSuccess(callbackContext, result);
                
            } catch (JSONException e) {
                Log.e(TAG, "❌ Error in broadcast method", e);
                sendError(callbackContext, "Broadcast failed: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "💥 BROADCAST EXCEPTION", e);
                sendError(callbackContext, "Broadcast failed: " + e.getMessage());
            }
        });
    }

    // ✅ PERBAIKAN: Method untuk kirim message sent event
    private void sendMessageSentEvent(Message message, int reachableNodes) {
        try {
            JSONObject sentEvent = new JSONObject();
            sentEvent.put("type", "MESSAGE_SENT");
            sentEvent.put("messageId", message.getMessageId());
            sentEvent.put("fromNodeId", message.getFromNodeId());
            sentEvent.put("toNodeId", message.getToNodeId());
            sentEvent.put("reachableNodes", reachableNodes);
            sentEvent.put("timestamp", System.currentTimeMillis());
            
            sendMeshEvent("MESSAGE_SENT", sentEvent.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating message sent event", e);
        }
    }

    public void discoverMeshNodes(final CallbackContext callbackContext) {
        if (bluetoothAdapter == null) {
            sendError(callbackContext, "Bluetooth not available");
            return;
        }

        meshExecutor.execute(() -> {
            final AtomicReference<BroadcastReceiver> receiverRef = new AtomicReference<>();
            
            try {
                // Cancel any ongoing discovery
                bluetoothAdapter.cancelDiscovery();

                // Create receiver
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        
                        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            if (device != null) {
                                // Check if device might be mesh-capable
                                String deviceName = device.getName();
                                if (deviceName != null && isPotentialMeshNode(deviceName)) {
                                    sendMeshDiscoveryEvent(device.getName(), device.getAddress(), "FOUND");
                                }
                            }
                        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                            sendMeshDiscoveryEvent("", "", "DISCOVERY_COMPLETED");
                        }
                    }
                };

                receiverRef.set(receiver);

                // Register the receiver dengan multiple actions
                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                context.registerReceiver(receiver, filter);

                // Start discovery
                boolean discoveryStarted = bluetoothAdapter.startDiscovery();
                
                if (discoveryStarted) {
                    JSONObject successResult = new JSONObject();
                    successResult.put("message", "Mesh discovery started");
                    callbackContext.success(successResult);
                    
                    // Stop discovery after timeout dengan guarantee
                    final BroadcastReceiver finalReceiver = receiver;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        cleanupDiscovery(finalReceiver);
                    }, DISCOVERY_DURATION_MS);
                    
                } else {
                    cleanupDiscovery(receiver);
                    sendError(callbackContext, "Failed to start discovery");
                }

            } catch (Exception e) {
                Log.e(TAG, "Mesh discovery failed", e);
                cleanupDiscovery(receiverRef.get());
                sendError(callbackContext, "Mesh discovery failed: " + e.getMessage());
            }
        });
    }

    public void autoJoinMesh(final CallbackContext callbackContext) {
        meshExecutor.execute(() -> {
            try {
                Log.d(TAG, "🔄 AUTO JOIN MESH: Starting intelligent mesh discovery...");
                
                // Step 1: First try to connect to existing paired gateways
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                String connectedGateway = null;
                
                Log.d(TAG, "📡 Checking " + pairedDevices.size() + " paired devices...");
                
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();
                    
                    Log.d(TAG, "   - Device: " + deviceName + " (" + deviceAddress + ")");
                    
                    if (isPotentialMeshNode(deviceName)) {
                        Log.d(TAG, "   🎯 Potential mesh gateway found: " + deviceName);
                        
                        // Test connection dengan timeout lebih pendek
                        boolean canConnect = tryMeshConnectionWithTimeout(deviceAddress, 8000);
                        
                        if (canConnect) {
                            connectedGateway = deviceAddress;
                            Log.i(TAG, "✅ Successfully connected to gateway: " + deviceAddress);
                            break;
                        } else {
                            Log.w(TAG, "❌ Failed to connect to: " + deviceAddress);
                        }
                        
                        Thread.sleep(300);
                    }
                }
                
                // Step 2: Handle connection result
                if (connectedGateway != null) {
                    Log.i(TAG, "🎯 Joining existing mesh via: " + connectedGateway);
                    
                    // Perbaiki neighbor relationships
                    fixNeighborRelationships();
                    
                    // Kirim join request
                    Message joinMessage = new Message(
                        localNodeId,
                        "BROADCAST",
                        "NODE_JOIN",
                        new JSONObject()
                            .put("nodeId", localNodeId)
                            .put("nodeName", getDeviceName())
                            .put("hopCount", 0)
                            .toString(),
                        0
                    );
                    
                    broadcastMessage(joinMessage);
                    
                    JSONObject result = new JSONObject();
                    result.put("success", true);
                    result.put("message", "Joined mesh network via " + connectedGateway);
                    result.put("nodeId", localNodeId);
                    result.put("gateway", connectedGateway);
                    result.put("status", "connected");
                    result.put("nodeCount", meshNodes.size());
                    result.put("neighbors", new JSONArray(meshNodes.get(localNodeId).getNeighbors()));
                    
                    sendSuccess(callbackContext, result);
                    sendMeshEvent("MESH_JOINED", "Auto-joined mesh via " + connectedGateway);
                    
                } else {
                    Log.w(TAG, "⚠️ No mesh networks found, initializing as gateway...");
                    
                    // Fallback: Initialize sebagai gateway baru
                    initializeMesh(callbackContext);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "❌ AUTO JOIN MESH: Failed", e);
                sendError(callbackContext, "Auto-join failed: " + e.getMessage());
            }
        });
    }

    public void getMeshTopology(final CallbackContext callbackContext) {
        try {
            Log.d(TAG, "📊 GET TOPOLOGY: Generating topology...");
            
            // Cleanup dead nodes first
            cleanupDeadNodes();
            
            JSONObject topology = new JSONObject();
            JSONArray nodesArray = new JSONArray();
            JSONArray connectionsArray = new JSONArray();
            
            Log.d(TAG, "📊 GET TOPOLOGY: " + meshNodes.size() + " total nodes in meshNodes");
            
            int connectedNodes = 0;
            int totalConnections = 0;
            
            for (MeshNode node : meshNodes.values()) {
                // Skip ghost nodes
                if (!node.isConnected() && 
                    (System.currentTimeMillis() - node.getLastSeen()) > NODE_TIMEOUT_MS) {
                    Log.d(TAG, "🧹 GET TOPOLOGY: Skipping ghost node: " + node.getNodeId());
                    continue;
                }
                
                if (node.isConnected()) connectedNodes++;
                
                JSONObject nodeInfo = new JSONObject();
                nodeInfo.put("nodeId", node.getNodeId());
                nodeInfo.put("nodeName", node.getNodeName());
                nodeInfo.put("hopCount", node.getHopCount());
                nodeInfo.put("connected", node.isConnected());
                nodeInfo.put("lastSeen", node.getLastSeen());
                nodeInfo.put("neighborsCount", node.getNeighbors().size());
                nodeInfo.put("neighbors", new JSONArray(node.getNeighbors()));
                nodesArray.put(nodeInfo);
                
                Log.d(TAG, "📊 GET TOPOLOGY: Node " + node.getNodeId() + 
                    " - connected: " + node.isConnected() + 
                    ", neighbors: " + node.getNeighbors().size());
                
                // Add connections
                for (String neighborId : node.getNeighbors()) {
                    JSONObject connection = new JSONObject();
                    connection.put("from", node.getNodeId());
                    connection.put("to", neighborId);
                    connection.put("type", "neighbor");
                    connectionsArray.put(connection);
                    totalConnections++;
                }
            }
            
            topology.put("nodes", nodesArray);
            topology.put("connections", connectionsArray);
            topology.put("localNodeId", localNodeId);
            topology.put("totalNodes", meshNodes.size());
            topology.put("connectedNodes", connectedNodes);
            topology.put("totalConnections", totalConnections);
            topology.put("meshActive", isMeshActive.get());
            
            Log.d(TAG, "📊 GET TOPOLOGY: Result - " + connectedNodes + " connected nodes, " + 
                totalConnections + " connections");
            
            callbackContext.success(topology);
            
        } catch (JSONException e) {
            Log.e(TAG, "❌ GET TOPOLOGY: Error", e);
            sendError(callbackContext, "Failed to get mesh topology");
        }
    }

    public void getAvailableGateways(final CallbackContext callbackContext) {
        if (bluetoothAdapter == null) {
            sendError(callbackContext, "Bluetooth not available");
            return;
        }

        meshExecutor.execute(() -> {
            try {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                JSONArray gatewaysArray = new JSONArray();
                
                Log.d(TAG, "🔍 Scanning " + pairedDevices.size() + " paired devices:");
                
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();
                    boolean isMeshCapable = isPotentialMeshNode(deviceName);
                    boolean connectionTest = testConnection(deviceAddress);
                    
                    Log.d(TAG, "   - " + deviceName + " (" + deviceAddress + ") " + "-> meshCapable: " + isMeshCapable + ", connectionTest: " + connectionTest);
                    
                    if (isMeshCapable || connectionTest) {
                        JSONObject gatewayInfo = new JSONObject();
                        gatewayInfo.put("name", deviceName);
                        gatewayInfo.put("address", deviceAddress);
                        gatewayInfo.put("type", device.getType());
                        gatewayInfo.put("meshCapable", isMeshCapable);
                        gatewayInfo.put("connectionTest", connectionTest);
                        gatewaysArray.put(gatewayInfo);
                    }
                }
                
                JSONObject result = new JSONObject();
                result.put("gateways", gatewaysArray);
                result.put("count", gatewaysArray.length());
                result.put("localNodeId", localNodeId);
                
                Log.d(TAG, "✅ Found " + gatewaysArray.length() + " potential gateways");
                
                callbackContext.success(result);
                
            } catch (JSONException e) {
                Log.e(TAG, "Error getting available gateways", e);
                sendError(callbackContext, "Failed to get available gateways");
            }
        });
    }

    public void setMeshEventCallback(CallbackContext callbackContext) {
        this.meshEventCallback = callbackContext;
        Log.d(TAG, "✅ Mesh event callback set");
    }


    public void startMeshEventListener(final CallbackContext callbackContext) {
        Log.d(TAG, "🎯 START MESH EVENT LISTENER CALLED");
        
        try {
            // ✅ PERBAIKAN: Simpan callback context untuk events
            this.meshEventCallback = callbackContext;
            
            // ✅ Kirim immediate test event untuk verifikasi
            sendTestEventToJS();
            
            // ✅ Setup periodic heartbeat dengan events
            startEventBasedHeartbeat();
            
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("message", "Mesh event listener started");
            result.put("timestamp", System.currentTimeMillis());
            
            Log.d(TAG, "✅ Mesh event listener started successfully");
            sendSuccess(callbackContext, result);
            
        } catch (JSONException e) {
            Log.e(TAG, "❌ Error starting mesh event listener", e);
            sendError(callbackContext, "Failed to start event listener: " + e.getMessage());
        }
    }

    // ✅ PERBAIKAN: Method untuk kirim test event ke JavaScript
    private void sendTestEventToJS() {
        try {
            Log.d(TAG, "🧪 SENDING TEST EVENT TO JAVASCRIPT");
            
            JSONObject testEvent = new JSONObject();
            testEvent.put("type", "TEST_EVENT");
            testEvent.put("message", "Hello from Java!");
            testEvent.put("timestamp", System.currentTimeMillis());
            testEvent.put("localNodeId", localNodeId);
            testEvent.put("totalNodes", meshNodes.size());
            
            sendMeshEvent("MESSAGE_RECEIVED", testEvent.toString());
            
            Log.d(TAG, "✅ TEST EVENT SENT TO JS: " + testEvent.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "❌ ERROR SENDING TEST EVENT", e);
        }
    }

    // ✅ PERBAIKAN: Enhanced event sender dengan better error handling
    private void sendMeshEvent(String eventType, String eventData) {
        try {
            if (meshEventCallback == null) {
                Log.w(TAG, "⚠️ meshEventCallback is null - cannot send event: " + eventType);
                return;
            }
            
            Log.d(TAG, "📨 SENDING MESH EVENT: " + eventType);
            Log.d(TAG, "   Data: " + eventData);
            
            JSONObject event = new JSONObject();
            event.put("type", eventType);
            event.put("data", new JSONObject(eventData));
            event.put("timestamp", System.currentTimeMillis());
            
            PluginResult result = new PluginResult(PluginResult.Status.OK, event);
            result.setKeepCallback(true);
            meshEventCallback.sendPluginResult(result);
            
            Log.d(TAG, "✅ MESH EVENT SENT SUCCESSFULLY: " + eventType);
            
        } catch (JSONException e) {
            Log.e(TAG, "❌ ERROR CREATING MESH EVENT", e);
        } catch (Exception e) {
            Log.e(TAG, "❌ ERROR SENDING MESH EVENT", e);
        }
    }

    // ✅ PERBAIKAN: Heartbeat dengan events
    private void startEventBasedHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            if (isMeshActive.get() && meshEventCallback != null) {
                try {
                    JSONObject heartbeatEvent = new JSONObject();
                    heartbeatEvent.put("type", "HEARTBEAT");
                    heartbeatEvent.put("localNodeId", localNodeId);
                    heartbeatEvent.put("totalNodes", meshNodes.size());
                    heartbeatEvent.put("connectedNodes", getConnectedNodesCount());
                    heartbeatEvent.put("timestamp", System.currentTimeMillis());
                    
                    sendMeshEvent("HEARTBEAT", heartbeatEvent.toString());
                    
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating heartbeat event", e);
                }
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS); // Setiap 10 detik
    }

    private int getConnectedNodesCount() {
        int count = 0;
        for (MeshNode node : meshNodes.values()) {
            if (node.isConnected()) {
                count++;
            }
        }
        return count;
    }

    // ==================== PRIVATE METHODS ====================

    private boolean connectToNodeWithTimeout(String nodeId, int timeoutMs) {
        try {
            Log.d(TAG, "🔌 CONNECT: Attempting to connect to: " + nodeId);
            
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(nodeId);
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MESH_UUID);
            
            bluetoothAdapter.cancelDiscovery();
            
            final AtomicBoolean connected = new AtomicBoolean(false);
            final AtomicBoolean timedOut = new AtomicBoolean(false);
            
            Thread connectionThread = new Thread(() -> {
                try {
                    Log.d(TAG, "🔌 CONNECT: Socket connecting to: " + nodeId);
                    socket.connect();
                    connected.set(true);
                    Log.i(TAG, "✅ CONNECT: Socket connected successfully: " + nodeId);
                    
                    // Simpan socket dan start listener untuk koneksi permanen
                    if (timeoutMs > 5000) {
                        nodeConnections.put(nodeId, socket);
                        startMeshMessageListener(socket, nodeId);
                        
                        // Tambahkan node ke meshNodes dan setup neighbor relationship
                        MeshNode connectedNode = meshNodes.get(nodeId);
                        if (connectedNode == null) {
                            connectedNode = new MeshNode(nodeId, device.getName(), 1);
                            meshNodes.put(nodeId, connectedNode);
                            Log.i(TAG, "✅ CONNECT: Added new node to meshNodes: " + nodeId);
                        }
                        connectedNode.setConnected(true);
                        connectedNode.setLastSeen(System.currentTimeMillis());
                        
                        // Setup neighbor relationship
                        MeshNode localNode = meshNodes.get(localNodeId);
                        if (localNode != null) {
                            localNode.addNeighbor(nodeId);
                            connectedNode.addNeighbor(localNodeId);
                            Log.i(TAG, "✅ CONNECT: Established neighbor relationship: " + 
                                localNodeId + " <-> " + nodeId);
                        }
                    }
                    
                } catch (IOException e) {
                    if (!timedOut.get()) {
                        Log.e(TAG, "❌ CONNECT: Socket connection failed: " + nodeId, e);
                    }
                }
            });
            
            connectionThread.start();
            connectionThread.join(timeoutMs);
            
            if (connectionThread.isAlive()) {
                timedOut.set(true);
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing socket after timeout", e);
                }
                connectionThread.interrupt();
                Log.w(TAG, "⏰ CONNECT: Connection timeout: " + nodeId);
                return false;
            }
            
            return connected.get();
            
        } catch (Exception e) {
            Log.e(TAG, "💥 CONNECT: Connection error to " + nodeId, e);
            return false;
        }
    }

    private void fixNeighborRelationships() {
        try {
            Log.d(TAG, "🔧 FIXING NEIGHBOR RELATIONSHIPS");
            
            MeshNode localNode = meshNodes.get(localNodeId);
            if (localNode == null) {
                Log.e(TAG, "❌ Local node not found in meshNodes!");
                return;
            }
            
            // Clear existing neighbors dan rebuild dari koneksi aktif
            localNode.getNeighbors().clear();
            
            for (String connectedNodeId : nodeConnections.keySet()) {
                if (!connectedNodeId.equals(localNodeId)) {
                    localNode.addNeighbor(connectedNodeId);
                    
                    // Juga update node yang terhubung
                    MeshNode connectedNode = meshNodes.get(connectedNodeId);
                    if (connectedNode != null) {
                        connectedNode.addNeighbor(localNodeId);
                        Log.i(TAG, "✅ Fixed neighbor relationship: " + localNodeId + " <-> " + connectedNodeId);
                    }
                }
            }
            
            Log.d(TAG, "🔧 NEIGHBORS AFTER FIX: " + localNode.getNeighbors());
            
        } catch (Exception e) {
            Log.e(TAG, "💥 Error fixing neighbor relationships", e);
        }
    }

    private boolean tryMeshConnectionWithTimeout(String nodeAddress, int timeoutMs) {
        try {
            Log.d(TAG, "🔗 Testing connection to: " + nodeAddress);
            
            boolean connected = connectToNodeWithTimeout(nodeAddress, timeoutMs);
            
            if (connected) {
                Log.i(TAG, "✅ Connection test SUCCESS: " + nodeAddress);
                return true;
            }
            
            Log.w(TAG, "❌ Connection test FAILED: " + nodeAddress);
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Connection test error: " + nodeAddress, e);
            return false;
        }
    }

    private boolean sendMessageViaRouteWithAck(Message message, List<String> route) {
        if (route.size() < 2) return false;
        
        String nextHop = route.get(1);
        Log.d(TAG, "🛣️ Sending via route - Next hop: " + nextHop + ", Full route: " + route);
        return sendMessageToNodeWithAck(message, nextHop);
    }

    private boolean sendMessageToNodeWithAck(Message message, String nodeId) {
        try {
            Log.d(TAG, "📨 SEND WITH ACK: To " + nodeId + ", Type: " + message.getMessageType());
            
            BluetoothSocket socket = nodeConnections.get(nodeId);
            if (socket == null) {
                Log.e(TAG, "❌ SEND WITH ACK: No socket found for " + nodeId);
                return false;
            }
            
            if (!socket.isConnected()) {
                Log.e(TAG, "❌ SEND WITH ACK: Socket not connected for " + nodeId);
                handleNodeDisconnection(nodeId);
                return false;
            }
            
            OutputStream outputStream = socket.getOutputStream();
            
            JSONObject messagePacket = new JSONObject();
            messagePacket.put("id", message.getMessageId());
            messagePacket.put("from", message.getFromNodeId());
            messagePacket.put("to", message.getToNodeId());
            messagePacket.put("type", message.getMessageType());
            messagePacket.put("data", message.getMessageData());
            messagePacket.put("hopCount", message.getHopCount());
            messagePacket.put("timestamp", System.currentTimeMillis());
            messagePacket.put("requiresAck", message.requiresAck());
            
            String packetString = messagePacket.toString() + "\n";
            Log.d(TAG, "📨 SEND WITH ACK: Sending packet to " + nodeId + ": " + packetString);
            
            outputStream.write(packetString.getBytes());
            outputStream.flush();
            
            // Jika membutuhkan ack, tambahkan ke pending
            if (message.requiresAck()) {
                pendingAcks.put(message.getMessageId(), message);
                Log.d(TAG, "⏳ Added to pending ACKs: " + message.getMessageId());
            }
            
            Log.i(TAG, "✅ SEND WITH ACK: Successfully sent to " + nodeId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ SEND WITH ACK: Failed to send to " + nodeId, e);
            handleNodeDisconnection(nodeId);
            return false;
        }
    }

    private void processIncomingMessage(String fromNodeId, JSONObject messagePacket) {
        try {
            String messageId = messagePacket.optString("id", UUID.randomUUID().toString());
            String toNodeId = messagePacket.getString("to");
            String messageType = messagePacket.getString("type");
            String data = messagePacket.getString("data");
            int hopCount = messagePacket.getInt("hopCount");
            boolean requiresAck = messagePacket.optBoolean("requiresAck", false);
            
            Log.d(TAG, "📨 PROCESS INCOMING: From " + fromNodeId + ", Type: " + messageType + ", To: " + toNodeId);
            Log.d(TAG, "   Message packet: " + messagePacket.toString());

        
            Log.d(TAG, "   Type: " + messageType + ", To: " + toNodeId + ", Data: " + data);
            
            Message message = new Message(fromNodeId, toNodeId, messageType, data, hopCount, messageId, requiresAck);
            
            // Send ACK jika diperlukan
            if (requiresAck && !messageType.equals("ACK")) {
                sendAckMessage(messageId, fromNodeId);
            }
            
            if (hopCount >= maxHops) {
                Log.w(TAG, "Message exceeded max hops, dropping: " + messageType);
                return;
            }
            
            // Handle different message types
            switch (messageType) {
                case "USER_MESSAGE":
                case "BROADCAST_MESSAGE":
                    if (toNodeId.equals(localNodeId) || toNodeId.equals("BROADCAST")) {
                        // Deliver to local application
                        Log.d(TAG, "📨 DELIVERING MESSAGE: " + data);
                        deliverMessageToApp(message);
                    } else {
                        // Forward to next hop
                        Log.d(TAG, "🔄 FORWARDING MESSAGE to: " + toNodeId);
                        forwardMessage(message);
                    }
                    break;
                    
                case "NODE_JOIN":
                    handleNodeJoin(message);
                    break;
                    
                case "HEARTBEAT":
                    handleHeartbeat(message);
                    break;
                    
                case "ACK":
                    handleAckMessage(messageId);
                    break;
                    
                default:
                    Log.w(TAG, "❓ Unknown message type: " + messageType);
                    break;
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "❌ PROCESS INCOMING: Invalid message format", e);
        }
    }

    private void handleHeartbeat(Message message) {
        try {
            JSONObject heartbeatData = new JSONObject(message.getMessageData());
            String nodeId = heartbeatData.getString("nodeId");
            long timestamp = heartbeatData.getLong("timestamp");
            
            updateNodeStatus(nodeId, true);
            Log.d(TAG, "💓 Heartbeat from " + nodeId + " at " + timestamp);
            
        } catch (JSONException e) {
            Log.e(TAG, "Invalid heartbeat format", e);
        }
    }

    private void sendAckMessage(String messageId, String toNodeId) {
        try {
            Message ackMessage = new Message(
                localNodeId,
                toNodeId,
                "ACK",
                new JSONObject().put("ackedMessageId", messageId).toString(),
                0,
                UUID.randomUUID().toString(),
                false
            );
            
            Log.d(TAG, "✅ Sending ACK for message: " + messageId + " to " + toNodeId);
            sendMessageToNode(ackMessage, toNodeId);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating ACK message", e);
        }
    }

    private void handleAckMessage(String messageId) {
        Message originalMessage = pendingAcks.remove(messageId);
        if (originalMessage != null) {
            Log.d(TAG, "✅ Message acknowledged: " + messageId);
            sendMeshEvent("MESSAGE_ACKED", "Message delivered: " + messageId);
        } else {
            Log.w(TAG, "⚠️ ACK for unknown message: " + messageId);
        }
    }

    private void startHeartbeatService() {
        scheduler.scheduleAtFixedRate(() -> {
            if (isMeshActive.get()) {
                try {
                    Log.d(TAG, "💓 Sending heartbeat...");
                    
                    Message heartbeat = new Message(
                        localNodeId,
                        "BROADCAST", 
                        "HEARTBEAT",
                        new JSONObject()
                            .put("timestamp", System.currentTimeMillis())
                            .put("nodeId", localNodeId)
                            .put("battery", 100)
                            .toString(),
                        0
                    );
                    
                    int sentCount = broadcastMessage(heartbeat);
                    Log.d(TAG, "💓 Heartbeat sent to " + sentCount + " nodes");
                    
                    // Clean up dead nodes
                    cleanupDeadNodes();
                    
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating heartbeat", e);
                }
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void startAckCleanupService() {
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            List<String> expiredAcks = new ArrayList<>();
            
            for (Map.Entry<String, Message> entry : pendingAcks.entrySet()) {
                // Asumsikan message dibuat dengan timestamp current
                if (currentTime - System.currentTimeMillis() > MESSAGE_ACK_TIMEOUT_MS) {
                    expiredAcks.add(entry.getKey());
                }
            }
            
            for (String expiredId : expiredAcks) {
                Message expiredMessage = pendingAcks.remove(expiredId);
                if (expiredMessage != null) {
                    Log.w(TAG, "⏰ Message ACK timeout: " + expiredId);
                    sendMeshEvent("MESSAGE_TIMEOUT", "Message delivery failed: " + expiredId);
                }
            }
            
        }, MESSAGE_ACK_TIMEOUT_MS, MESSAGE_ACK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void cleanupDeadNodes() {
        long currentTime = System.currentTimeMillis();
        List<String> deadNodes = new ArrayList<>();
        
        for (MeshNode node : meshNodes.values()) {
            if (!node.getNodeId().equals(localNodeId) && 
                (currentTime - node.getLastSeen()) > NODE_TIMEOUT_MS) {
                deadNodes.add(node.getNodeId());
            }
        }
        
        for (String deadNodeId : deadNodes) {
            handleNodeDisconnection(deadNodeId);
            sendMeshEvent("NODE_TIMEOUT", "Node removed due to timeout: " + deadNodeId);
        }
    }

    private void startMeshServer() {
        meshExecutor.execute(() -> {
            BluetoothServerSocket serverSocket = null;
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    "ResqPeerNetMesh", MESH_UUID);
                
                Log.i(TAG, "🔵 Mesh server started, waiting for connections...");
                
                while (isMeshActive.get()) {
                    BluetoothSocket clientSocket = serverSocket.accept();
                    String clientNodeId = clientSocket.getRemoteDevice().getAddress();
                    
                    Log.i(TAG, "🔵 Mesh connection accepted from: " + clientNodeId);
                    
                    // Add to connections
                    nodeConnections.put(clientNodeId, clientSocket);
                    
                    // Add to mesh nodes
                    MeshNode clientNode = meshNodes.get(clientNodeId);
                    if (clientNode == null) {
                        clientNode = new MeshNode(clientNodeId, clientSocket.getRemoteDevice().getName(), 1);
                        meshNodes.put(clientNodeId, clientNode);
                    }
                    clientNode.setConnected(true);
                    clientNode.setLastSeen(System.currentTimeMillis());
                    
                    // Setup neighbor relationship
                    MeshNode localNode = meshNodes.get(localNodeId);
                    if (localNode != null) {
                        localNode.addNeighbor(clientNodeId);
                        clientNode.addNeighbor(localNodeId);
                    }
                    
                    // Start message listener for this connection
                    startMeshMessageListener(clientSocket, clientNodeId);
                    
                    sendMeshEvent("NODE_CONNECTED", "New node connected: " + clientNodeId);
                    
                }
                
            } catch (IOException e) {
                if (isMeshActive.get()) {
                    Log.e(TAG, "❌ Mesh server error", e);
                }
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing server socket", e);
                    }
                }
            }
        });
    }

    private void startMeshMessageListener(BluetoothSocket socket, String nodeId) {
        meshExecutor.execute(() -> {
            try {
                Log.d(TAG, "👂 STARTING MESSAGE LISTENER for: " + nodeId);
                
                InputStream inputStream = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes;
                
                while (socket.isConnected() && isMeshActive.get() && 
                    (bytes = inputStream.read(buffer)) != -1) {
                    
                    String messageStr = new String(buffer, 0, bytes).trim();
                    Log.d(TAG, "📨 MESSAGE LISTENER " + nodeId + " received: " + messageStr);
                    
                    String[] messages = messageStr.split("\n");
                    for (String singleMessage : messages) {
                        if (!singleMessage.trim().isEmpty()) {
                            try {
                                JSONObject messagePacket = new JSONObject(singleMessage);
                                Log.d(TAG, "📨 MESSAGE LISTENER: Parsed JSON successfully");
                                processIncomingMessage(nodeId, messagePacket);
                            } catch (JSONException e) {
                                Log.e(TAG, "❌ MESSAGE LISTENER: Invalid JSON: " + singleMessage, e);
                            }
                        }
                    }
                }
                
                Log.d(TAG, "👂 MESSAGE LISTENER: Stopped for " + nodeId);
                
            } catch (Exception e) {
                Log.e(TAG, "💥 MESSAGE LISTENER: Error for " + nodeId, e);
                handleNodeDisconnection(nodeId);
            }
        });
    }

    private void handleNodeDisconnection(String nodeId) {
        Log.d(TAG, "🔌 HANDLE NODE DISCONNECTION: " + nodeId);
        
        // Cleanup connection
        BluetoothSocket socket = nodeConnections.remove(nodeId);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket for " + nodeId, e);
            }
        }
        
        // Update node status
        MeshNode node = meshNodes.get(nodeId);
        if (node != null) {
            node.setConnected(false);
        }
        
        // Remove from local node's neighbors
        MeshNode localNode = meshNodes.get(localNodeId);
        if (localNode != null) {
            localNode.getNeighbors().remove(nodeId);
        }
        
        sendMeshEvent("NODE_DISCONNECTED", "Node disconnected: " + nodeId);
    }

    private boolean testConnection(String nodeAddress) {
        try {
            Log.d(TAG, "🔗 Testing connection to: " + nodeAddress);
            
            // Gunakan timeout lebih pendek untuk test (5 detik)
            boolean connected = connectToNodeWithTimeout(nodeAddress, 5000);
            
            if (connected) {
                Log.i(TAG, "✅ Connection test SUCCESS: " + nodeAddress);
                
                // Tutup koneksi test (hanya test, bukan koneksi permanen)
                BluetoothSocket testSocket = nodeConnections.get(nodeAddress);
                if (testSocket != null) {
                    try {
                        testSocket.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Error closing test socket", e);
                    }
                    nodeConnections.remove(nodeAddress);
                }
                
                return true;
            }
            
            Log.w(TAG, "❌ Connection test FAILED: " + nodeAddress);
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Connection test error: " + nodeAddress, e);
            return false;
        }
    }

    private boolean isPotentialMeshNode(String deviceName) {
        if (deviceName == null || deviceName.trim().isEmpty()) {
            return false;
        }
        
        String lowerName = deviceName.toLowerCase().trim();
        
        // Explicit check for common non-mesh devices
        if (lowerName.contains("iphone") || 
            lowerName.contains("speaker") ||
            lowerName.contains("music") ||
            lowerName.contains("earphone") ||
            lowerName.contains("headset") ||
            lowerName.contains("buds") ||
            lowerName.contains("airpods") ||
            lowerName.startsWith("bt ") ||
            lowerName.endsWith(" speaker")) {
            return false;
        }
        
        return lowerName.contains("resq") || 
            lowerName.contains("peer") ||
            lowerName.contains("mesh") ||
            lowerName.contains("node") ||
            deviceName.startsWith("RP_") || 
            deviceName.startsWith("MESH_") ||
            deviceName.equals("S45B") ||
            deviceName.contains("OLE") ||
            lowerName.contains("gateway") ||
            lowerName.contains("router");   
    }

    private List<String> findRouteToNode(String targetNodeId) {
        Log.d(TAG, "🛣️ FIND ROUTE: Looking for route to " + targetNodeId);
        
        if (targetNodeId.equals(localNodeId)) {
            Log.d(TAG, "🛣️ FIND ROUTE: Target is local node");
            List<String> route = new ArrayList<>();
            route.add(localNodeId);
            return route;
        }
        
        // Cek direct connection terlebih dahulu
        if (nodeConnections.containsKey(targetNodeId)) {
            Log.d(TAG, "🛣️ FIND ROUTE: Direct connection available");
            List<String> route = new ArrayList<>();
            route.add(localNodeId);
            route.add(targetNodeId);
            return route;
        }
        
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousNodes = new HashMap<>();
        List<String> unvisited = new ArrayList<>();
        
        // Initialize semua nodes
        for (String nodeId : meshNodes.keySet()) {
            MeshNode node = meshNodes.get(nodeId);
            if (node != null && node.isConnected()) {
                distances.put(nodeId, Integer.MAX_VALUE);
                previousNodes.put(nodeId, null);
                unvisited.add(nodeId);
            }
        }
        distances.put(localNodeId, 0);
        
        Log.d(TAG, "🛣️ FIND ROUTE: Starting Dijkstra with " + unvisited.size() + " nodes");
        
        while (!unvisited.isEmpty()) {
            String currentNode = getNodeWithMinDistance(unvisited, distances);
            if (currentNode == null) break;
            
            unvisited.remove(currentNode);
            
            if (currentNode.equals(targetNodeId)) {
                List<String> route = reconstructPath(previousNodes, targetNodeId);
                Log.d(TAG, "🛣️ FIND ROUTE: Found route: " + route);
                return route;
            }
            
            MeshNode currentMeshNode = meshNodes.get(currentNode);
            if (currentMeshNode != null) {
                for (String neighborId : currentMeshNode.getNeighbors()) {
                    MeshNode neighborNode = meshNodes.get(neighborId);
                    if (neighborNode != null && neighborNode.isConnected() && unvisited.contains(neighborId)) {
                        int alt = distances.get(currentNode) + 1;
                        if (alt < distances.get(neighborId)) {
                            distances.put(neighborId, alt);
                            previousNodes.put(neighborId, currentNode);
                            Log.d(TAG, "🛣️ FIND ROUTE: Updated " + neighborId + " via " + currentNode + " distance: " + alt);
                        }
                    }
                }
            }
        }
        
        Log.d(TAG, "🛣️ FIND ROUTE: No route found to " + targetNodeId);
        return null;
    }

    private String getNodeWithMinDistance(List<String> unvisited, Map<String, Integer> distances) {
        String minNode = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (String nodeId : unvisited) {
            Integer distance = distances.get(nodeId);
            if (distance != null && distance < minDistance) {
                minDistance = distance;
                minNode = nodeId;
            }
        }
        
        return minNode;
    }

    private List<String> reconstructPath(Map<String, String> previousNodes, String targetNodeId) {
        List<String> path = new ArrayList<>();
        String current = targetNodeId;
        
        while (current != null) {
            path.add(0, current);
            current = previousNodes.get(current);
        }
        
        return path;
    }

    private int broadcastMessage(Message message) {
        int sentCount = 0;
        
        try {
            Log.d(TAG, "📢 BROADCAST: Starting for message: " + message.getMessageType());
            Log.d(TAG, "   From: " + message.getFromNodeId() + " → To: " + message.getToNodeId());
            
            MeshNode localMeshNode = meshNodes.get(localNodeId);
           /*  if (localMeshNode == null) {
                Log.e(TAG, "❌ BROADCAST: Local mesh node not found");
                return 0;
            } */

           if (localMeshNode != null) {
                Log.d(TAG, "   Local node neighbors: " + localMeshNode.getNeighbors().size());
                
                for (String neighborId : localMeshNode.getNeighbors()) {
                    Log.d(TAG, "   Attempting to send to neighbor: " + neighborId);
                    
                    boolean sent = sendMessageToNode(message, neighborId);
                    if (sent) {
                        sentCount++;
                        Log.d(TAG, "   ✅ Successfully sent to: " + neighborId);
                    } else {
                        Log.e(TAG, "   ❌ Failed to send to: " + neighborId);
                    }
                }
            }
            
            List<String> neighbors = localMeshNode.getNeighbors();
            Log.d(TAG, "📊 BROADCAST: Found " + neighbors.size() + " neighbors: " + neighbors);
            
            for (String neighborId : neighbors) {
                if (neighborId.equals(localNodeId)) {
                    Log.d(TAG, "⏩ BROADCAST: Skipping self");
                    continue;
                }
                
                Log.d(TAG, "🎯 BROADCAST: Processing neighbor: " + neighborId);
                
                boolean sent = sendMessageToNode(message, neighborId);
                Log.d(TAG, "📤 BROADCAST: Send result for " + neighborId + ": " + sent);
                
                if (sent) {
                    sentCount++;
                    Log.i(TAG, "✅ BROADCAST: Success to " + neighborId);
                } else {
                    Log.e(TAG, "❌ BROADCAST: Failed to " + neighborId);
                }
            }
            
            Log.d(TAG, "📢 BROADCAST: Completed - " + sentCount + "/" + neighbors.size() + " successful");
            
        } catch (Exception e) {
            Log.e(TAG, "💥 BROADCAST: Unexpected error", e);
        }
        
        Log.d(TAG, "📤 BROADCAST COMPLETE: " + sentCount + " nodes reached");
        return sentCount;
    }

    private boolean sendMessageToNode(Message message, String nodeId) {
        try {
            Log.d(TAG, "📨 SEND TO NODE: Attempting to send to " + nodeId);
            
            BluetoothSocket socket = nodeConnections.get(nodeId);
            
            if (socket == null) {
                Log.e(TAG, "❌ SEND TO NODE: No socket found for " + nodeId);
                return false;
            }
            
            if (!socket.isConnected()) {
                Log.e(TAG, "❌ SEND TO NODE: Socket not connected for " + nodeId);
                handleNodeDisconnection(nodeId);
                return false;
            }
            
            Log.d(TAG, "📨 SEND TO NODE: Socket is connected for " + nodeId);
            
            OutputStream outputStream = socket.getOutputStream();
            
            JSONObject messagePacket = new JSONObject();
            messagePacket.put("from", message.getFromNodeId());
            messagePacket.put("to", message.getToNodeId());
            messagePacket.put("type", message.getMessageType());
            messagePacket.put("data", message.getMessageData());
            messagePacket.put("hopCount", message.getHopCount());
            messagePacket.put("timestamp", System.currentTimeMillis());
            
            String packetString = messagePacket.toString() + "\n";
            Log.d(TAG, "📨 SEND TO NODE: Sending packet: " + packetString);
            
            outputStream.write(packetString.getBytes());
            outputStream.flush();
            
            Log.i(TAG, "✅ SEND TO NODE: Successfully sent to " + nodeId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ SEND TO NODE: Failed to send to " + nodeId, e);
            handleNodeDisconnection(nodeId);
            return false;
        }
    }

    private void handleNodeJoin(Message message) {
        try {
            JSONObject joinData = new JSONObject(message.getMessageData());
            String newNodeId = joinData.getString("nodeId");
            String newNodeName = joinData.getString("nodeName"); 
            int hopCount = message.getHopCount() + 1;
            
            Log.d(TAG, "🎯 HANDLE NODE JOIN: " + newNodeId + " from " + message.getFromNodeId() + 
                " (hopCount: " + hopCount + ")");
            
            MeshNode newNode = meshNodes.get(newNodeId);
            if (newNode == null) {
                newNode = new MeshNode(newNodeId, newNodeName, hopCount);
                meshNodes.put(newNodeId, newNode);
                Log.i(TAG, "✅ HANDLE NODE JOIN: Added NEW node to mesh: " + newNodeId);
            } else {
                newNode.setHopCount(hopCount);
                newNode.setLastSeen(System.currentTimeMillis());
                Log.i(TAG, "✅ HANDLE NODE JOIN: Updated existing node: " + newNodeId);
            }
            
            // Add neighbor relationships
            String sourceNodeId = message.getFromNodeId();
            
            // Relationship: local node ↔ source node
            MeshNode localNode = meshNodes.get(localNodeId);
            if (localNode != null) {
                localNode.addNeighbor(sourceNodeId);
                Log.i(TAG, "✅ HANDLE NODE JOIN: Local node added neighbor: " + sourceNodeId);
            }
            
            // Relationship: source node ↔ local node  
            MeshNode sourceNode = meshNodes.get(sourceNodeId);
            if (sourceNode != null) {
                sourceNode.addNeighbor(localNodeId);
                Log.i(TAG, "✅ HANDLE NODE JOIN: Source node added neighbor: " + localNodeId);
            }
            
            // Relationship: local node ↔ new node (jika berbeda dari source)
            if (!newNodeId.equals(sourceNodeId) && localNode != null) {
                localNode.addNeighbor(newNodeId);
                newNode.addNeighbor(localNodeId);
                Log.i(TAG, "✅ HANDLE NODE JOIN: Local node added direct neighbor: " + newNodeId);
            }
            
            sendMeshEvent("NODE_ADDED", "New node joined: " + newNodeName);
            
            // Forward join message jika perlu
            if (message.getHopCount() == 0) {
                Message forwardMessage = new Message(
                    localNodeId,
                    "BROADCAST", 
                    "NODE_JOIN",
                    message.getMessageData(),
                    hopCount
                );
                
                if (localNode != null) {
                    for (String neighborId : localNode.getNeighbors()) {
                        if (!neighborId.equals(newNodeId) && !neighborId.equals(sourceNodeId)) {
                            sendMessageToNode(forwardMessage, neighborId);
                            Log.d(TAG, "📤 HANDLE NODE JOIN: Forwarded join to: " + neighborId);
                        }
                    }
                }
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "❌ HANDLE NODE JOIN: Invalid node join data", e);
        }
    }

    private void forwardMessage(Message message) {
        List<String> route = findRouteToNode(message.getToNodeId());
        if (route != null && route.size() > 1) {
            message.setHopCount(message.getHopCount() + 1);
            sendMessageViaRoute(message, route);
        } else {
            Log.w(TAG, "⚠️ FORWARD MESSAGE: No route to forward message to " + message.getToNodeId());
        }
    }

    private boolean sendMessageViaRoute(Message message, List<String> route) {
        if (route.size() < 2) return false;
        String nextHop = route.get(1);
        return sendMessageToNode(message, nextHop);
    }

    // ✅ PERBAIKAN: Enhanced message delivery dengan event
    private void deliverMessageToApp(Message message) {
        try {
            Log.d(TAG, "📱 DELIVER TO APP: " + message.getMessageData());
            
            // Parse message data jika JSON
            String messageContent = message.getMessageData();
            try {
                JSONObject messageData = new JSONObject(messageContent);
                // Jika berhasil parse, gunakan structured data
                messageContent = messageData.toString();
            } catch (JSONException e) {
                // Jika bukan JSON, gunakan sebagai plain text
                Log.d(TAG, "Message is plain text, not JSON");
            }
            
            // ✅ PERBAIKAN: Kirim event dengan structured data
            JSONObject eventData = new JSONObject();
            eventData.put("deviceId", message.getFromNodeId());
            eventData.put("deviceName", "Mesh Node");
            eventData.put("message", message.getMessageData()); // Raw message
            eventData.put("parsedMessage", messageContent);     // Parsed/raw content
            eventData.put("timestamp", System.currentTimeMillis());
            eventData.put("hopCount", message.getHopCount());
            eventData.put("messageType", message.getMessageType());
            
            sendMeshEvent("MESSAGE_RECEIVED", eventData.toString());
            
            Log.i(TAG, "✅ Message delivered to app: " + message.getMessageData());
            
        } catch (JSONException e) {
            Log.e(TAG, "❌ Error creating delivery event", e);
            
            // Fallback: kirim minimal event
            try {
                JSONObject fallbackEvent = new JSONObject();
                fallbackEvent.put("deviceId", message.getFromNodeId());
                fallbackEvent.put("message", message.getMessageData());
                fallbackEvent.put("timestamp", System.currentTimeMillis());
                
                sendMeshEvent("MESSAGE_RECEIVED", fallbackEvent.toString());
            } catch (JSONException e2) {
                Log.e(TAG, "❌ Even fallback event failed", e2);
            }
        }
    }

    private void updateNodeStatus(String nodeId, boolean isAlive) {
        MeshNode node = meshNodes.get(nodeId);
        if (node != null) {
            node.setConnected(isAlive);
            node.setLastSeen(System.currentTimeMillis());
            Log.d(TAG, "Updated node " + nodeId + " status: " + isAlive);
        } else {
            Log.w(TAG, "Attempted to update non-existent node: " + nodeId);
        }
    }

    private void startMessageProcessor() {
        meshExecutor.execute(() -> {
            while (isMeshActive.get()) {
                try {
                    if (!messageQueue.isEmpty()) {
                        Message message = messageQueue.remove(0);
                        processIncomingMessage(message.getFromNodeId(), 
                            new JSONObject()
                                .put("to", message.getToNodeId())
                                .put("type", message.getMessageType())
                                .put("data", message.getMessageData())
                                .put("hopCount", message.getHopCount())
                        );
                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(TAG, "Message processor error", e);
                }
            }
        });
    }

    private void debugConnectionStatus() {
        Log.d(TAG, "🔍 DEBUG CONNECTION STATUS:");
        Log.d(TAG, "   - Local Node: " + localNodeId);
        Log.d(TAG, "   - Mesh Active: " + isMeshActive.get());
        Log.d(TAG, "   - Node Connections: " + nodeConnections.size());
        Log.d(TAG, "   - Mesh Nodes: " + meshNodes.size());
        
        for (Map.Entry<String, BluetoothSocket> entry : nodeConnections.entrySet()) {
            String nodeId = entry.getKey();
            BluetoothSocket socket = entry.getValue();
            
            boolean hasSocket = socket != null;
            boolean isConnected = hasSocket && socket.isConnected();
            
            Log.d(TAG, "   - Connection " + nodeId + 
                  ": socket=" + hasSocket + 
                  ", connected=" + isConnected);
                  
            MeshNode meshNode = meshNodes.get(nodeId);
            if (meshNode != null) {
                Log.d(TAG, "     MeshNode: connected=" + meshNode.isConnected() +
                      ", neighbors=" + meshNode.getNeighbors().size());
            }
        }
        
        MeshNode localNode = meshNodes.get(localNodeId);
        if (localNode != null) {
            Log.d(TAG, "   - Local Node Neighbors: " + localNode.getNeighbors());
        }
    }

    private String generateNodeId() {
        return bluetoothAdapter != null ? 
            bluetoothAdapter.getAddress().replace(":", "") + "_" + System.currentTimeMillis() : 
            "LOCAL_" + System.currentTimeMillis();
    }

    private String getDeviceName() {
        return bluetoothAdapter != null ? bluetoothAdapter.getName() : "Unknown Device";
    }

    private void sendMeshDiscoveryEvent(String deviceName, String deviceId, String eventType) {
        if (meshEventCallback != null) {
            try {
                JSONObject event = new JSONObject();
                event.put("type", "discovery");
                event.put("event", eventType);
                event.put("deviceName", deviceName);
                event.put("deviceId", deviceId);
                event.put("timestamp", System.currentTimeMillis());
                
                PluginResult result = new PluginResult(PluginResult.Status.OK, event);
                result.setKeepCallback(true);
                meshEventCallback.sendPluginResult(result);
                
            } catch (JSONException e) {
                Log.e(TAG, "Error sending discovery event", e);
            }
        }
    }

    private void cleanupDiscovery(BroadcastReceiver receiver) {
        try {
            if (bluetoothAdapter != null) {
                bluetoothAdapter.cancelDiscovery();
            }
            if (receiver != null && context != null) {
                context.unregisterReceiver(receiver);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error in discovery cleanup", e);
        }
    }

    private void sendSuccess(CallbackContext callbackContext, JSONObject result) {
        if (callbackContext != null) {
            callbackContext.success(result);
        }
    }

    private void sendError(CallbackContext callbackContext, String message) {
        try {
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("error", message);
            error.put("code", "MESH_ERROR");
            if (callbackContext != null) {
                callbackContext.error(error);
            }
        } catch (JSONException e) {
            if (callbackContext != null) {
                callbackContext.error("{\"success\":false,\"error\":\"" + message + "\",\"code\":\"MESH_ERROR\"}");
            }
        }
    }

    public void destroy() {
        isMeshActive.set(false);
        
        // Cleanup discovery receiver
        if (discoveryReceiver != null) {
            cleanupDiscovery(discoveryReceiver);
        }
        
        // Clean up connections
        for (BluetoothSocket socket : nodeConnections.values()) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing mesh socket", e);
            }
        }
        nodeConnections.clear();
        meshNodes.clear();
        pendingAcks.clear();
        messageQueue.clear();
        
        // Shutdown executors
        if (meshExecutor != null && !meshExecutor.isShutdown()) {
            meshExecutor.shutdown();
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        Log.i(TAG, "BluetoothMeshManager destroyed");
    }

    // ==================== INNER CLASSES ====================

    private static class MeshNode {
        private final String nodeId;
        private final String nodeName;
        private int hopCount;
        private boolean connected;
        private long lastSeen;
        private final List<String> neighbors;
        
        public MeshNode(String nodeId, String nodeName, int hopCount) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.hopCount = hopCount;
            this.connected = true;
            this.lastSeen = System.currentTimeMillis();
            this.neighbors = new ArrayList<>();
        }
        
        public String getNodeId() { return nodeId; }
        public String getNodeName() { return nodeName; }
        public int getHopCount() { return hopCount; }
        public void setHopCount(int hopCount) { this.hopCount = hopCount; }
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
        public long getLastSeen() { return lastSeen; }
        public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
        public List<String> getNeighbors() { return neighbors; }
        public void addNeighbor(String neighborId) { 
            if (!neighbors.contains(neighborId)) {
                neighbors.add(neighborId);
            }
        }
    }

    private static class Message {
        private final String fromNodeId;
        private final String toNodeId;
        private final String messageType;
        private final String messageData;
        private int hopCount;
        private final String messageId;
        private final boolean requiresAck;
        
        public Message(String fromNodeId, String toNodeId, String messageType, 
                      String messageData, int hopCount) {
            this(fromNodeId, toNodeId, messageType, messageData, hopCount, 
                 UUID.randomUUID().toString(), false);
        }
        
        public Message(String fromNodeId, String toNodeId, String messageType, 
                      String messageData, int hopCount, String messageId, boolean requiresAck) {
            this.fromNodeId = fromNodeId;
            this.toNodeId = toNodeId;
            this.messageType = messageType;
            this.messageData = messageData;
            this.hopCount = hopCount;
            this.messageId = messageId;
            this.requiresAck = requiresAck;
        }
        
        public String getFromNodeId() { return fromNodeId; }
        public String getToNodeId() { return toNodeId; }
        public String getMessageType() { return messageType; }
        public String getMessageData() { return messageData; }
        public int getHopCount() { return hopCount; }
        public void setHopCount(int hopCount) { this.hopCount = hopCount; }
        public String getMessageId() { return messageId; }
        public boolean requiresAck() { return requiresAck; }
    }
}