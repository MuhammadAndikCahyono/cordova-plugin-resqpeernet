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
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PeerNetworkingBridge - Bluetooth P2P Communication Module
 * Focus on stable Bluetooth functionality first
 */
public class PeerNetworkingBridge {

    private static final String TAG = "PeerNetworkingBridge";
    
    // UUID untuk service kita
    private static final UUID RESQPEERNET_UUID = UUID.fromString("a5a5a5a5-1111-2222-3333-444444444444");
    private static final String SERVICE_NAME = "ResqPeerNet";
    
    private final Context context;
    private final Handler mainHandler;
    private final ExecutorService executorService;
    private final Map<String, ConnectedDevice> connectedDevices;
    private final AtomicBoolean isDestroyed;
    
    // Bluetooth components
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket bluetoothServerSocket;
    private AcceptThread bluetoothAcceptThread;
    
    // Callbacks
    private WeakReference<CallbackContext> discoveryCallbackRef;
    private WeakReference<CallbackContext> connectionCallbackRef;
    private WeakReference<CallbackContext> messageCallbackRef;
    
    // Discovery state
    private BroadcastReceiver bluetoothDiscoveryReceiver;
    private boolean isDiscovering = false;
    
    public PeerNetworkingBridge(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newCachedThreadPool();
        this.connectedDevices = new ConcurrentHashMap<>();
        this.isDestroyed = new AtomicBoolean(false);
        
        initializeBluetooth();
    }
    
    private void initializeBluetooth() {
        try {
            // Initialize Bluetooth
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                Log.i(TAG, "Bluetooth adapter initialized - Name: " + bluetoothAdapter.getName());
                Log.i(TAG, "Bluetooth address: " + bluetoothAdapter.getAddress());
                Log.i(TAG, "Bluetooth enabled: " + bluetoothAdapter.isEnabled());
            } else {
                Log.w(TAG, "Bluetooth not available on this device");
            }
            
            Log.i(TAG, "PeerNetworkingBridge (Bluetooth Only) initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Bluetooth", e);
        }
    }

    // ==================== BLUETOOTH STATUS METHODS ====================
    
    /**
     * Check Bluetooth availability and status
     */
    public void getBluetoothStatus(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        executorService.execute(() -> {
            try {
                JSONObject status = new JSONObject();
                
                status.put("available", bluetoothAdapter != null);
                status.put("enabled", bluetoothAdapter != null && bluetoothAdapter.isEnabled());
                status.put("discovering", isDiscovering);
                status.put("connectedDevices", connectedDevices.size());
                
                if (bluetoothAdapter != null) {
                    status.put("name", bluetoothAdapter.getName());
                    status.put("address", bluetoothAdapter.getAddress());
                }
                
                status.put("timestamp", System.currentTimeMillis());
                
                sendSuccess(callbackContext, status);
                Log.d(TAG, "Bluetooth status retrieved");
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting Bluetooth status", e);
                sendError(callbackContext, "Failed to get Bluetooth status: " + e.getMessage());
            }
        });
    }

    /**
     * Enable Bluetooth (if not enabled)
     */
    public void enableBluetooth(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        if (bluetoothAdapter == null) {
            sendError(callbackContext, "Bluetooth not available on this device");
            return;
        }
        
        executorService.execute(() -> {
            try {
                if (bluetoothAdapter.isEnabled()) {
                    sendSuccess(callbackContext, createResult(true, "Bluetooth already enabled", "BLUETOOTH"));
                    return;
                }
                
                boolean enabled = bluetoothAdapter.enable();
                
                JSONObject result = new JSONObject();
                result.put("success", enabled);
                result.put("message", enabled ? "Bluetooth enable requested" : "Failed to enable Bluetooth");
                result.put("timestamp", System.currentTimeMillis());
                
                sendSuccess(callbackContext, result);
                
            } catch (Exception e) {
                Log.e(TAG, "Error enabling Bluetooth", e);
                sendError(callbackContext, "Failed to enable Bluetooth: " + e.getMessage());
            }
        });
    }

    /**
     * Get paired Bluetooth devices
     */
    public void getPairedDevices(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        executorService.execute(() -> {
            try {
                JSONArray devicesArray = new JSONArray();
                
                if (bluetoothAdapter != null) {
                    for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                        JSONObject deviceInfo = new JSONObject();
                        deviceInfo.put("name", device.getName());
                        deviceInfo.put("address", device.getAddress());
                        devicesArray.put(deviceInfo);
                    }
                }
                
                JSONObject result = new JSONObject();
                result.put("devices", devicesArray);
                result.put("count", devicesArray.length());
                result.put("timestamp", System.currentTimeMillis());
                
                sendSuccess(callbackContext, result);
                Log.d(TAG, "Retrieved " + devicesArray.length() + " paired devices");
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting paired devices", e);
                sendError(callbackContext, "Failed to get paired devices: " + e.getMessage());
            }
        });
    }

    // ==================== BLUETOOTH DISCOVERY METHODS ====================
    
    /**
     * Start Bluetooth device discovery
     */
    public void startBluetoothDiscovery(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        if (bluetoothAdapter == null) {
            sendError(callbackContext, "Bluetooth not available on this device");
            return;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            sendError(callbackContext, "Bluetooth is not enabled");
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Cancel any existing discovery
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                    Log.d(TAG, "Cancelled existing discovery");
                }
                
                // Register receiver for device discovery
                registerBluetoothDiscoveryReceiver();
                
                // Start discovery
                boolean started = bluetoothAdapter.startDiscovery();
                
                JSONObject result = new JSONObject();
                result.put("success", started);
                result.put("type", "BLUETOOTH");
                result.put("message", started ? "Bluetooth discovery started" : "Failed to start Bluetooth discovery");
                result.put("timestamp", System.currentTimeMillis());
                
                isDiscovering = started;
                
                sendSuccess(callbackContext, result);
                Log.i(TAG, "Bluetooth discovery " + (started ? "started" : "failed"));
                
            } catch (Exception e) {
                Log.e(TAG, "Error starting Bluetooth discovery", e);
                sendError(callbackContext, "Failed to start Bluetooth discovery: " + e.getMessage());
            }
        });
    }

    /**
     * Stop Bluetooth device discovery
     */
    public void stopBluetoothDiscovery(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        executorService.execute(() -> {
            try {
                boolean stopped = false;
                
                if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                    stopped = bluetoothAdapter.cancelDiscovery();
                }
                
                // Unregister receiver
                unregisterBluetoothDiscoveryReceiver();
                
                JSONObject result = new JSONObject();
                result.put("success", stopped);
                result.put("message", stopped ? "Bluetooth discovery stopped" : "No active discovery to stop");
                result.put("timestamp", System.currentTimeMillis());
                
                isDiscovering = false;
                
                sendSuccess(callbackContext, result);
                Log.i(TAG, "Bluetooth discovery stopped");
                
            } catch (Exception e) {
                Log.e(TAG, "Error stopping Bluetooth discovery", e);
                sendError(callbackContext, "Failed to stop Bluetooth discovery: " + e.getMessage());
            }
        });
    }

    // ==================== BLUETOOTH CONNECTION METHODS ====================

    /**
     * Connect to Bluetooth device
     */
    public void connectToBluetoothDevice(final String deviceAddress, final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        executorService.execute(() -> {
            try {
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                if (device == null) {
                    sendError(callbackContext, "Device not found: " + deviceAddress);
                    return;
                }
                
                // Cancel discovery to improve connection stability
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                
                ConnectThread connectThread = new ConnectThread(device);
                connectThread.start();
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("deviceAddress", deviceAddress);
                result.put("deviceName", device.getName());
                result.put("message", "Connecting to Bluetooth device");
                result.put("timestamp", System.currentTimeMillis());
                
                sendSuccess(callbackContext, result);
                Log.i(TAG, "Initiating connection to: " + device.getName());
                
            } catch (Exception e) {
                Log.e(TAG, "Error connecting to Bluetooth device", e);
                sendError(callbackContext, "Failed to connect to Bluetooth device: " + e.getMessage());
            }
        });
    }

    /**
     * Start Bluetooth server for incoming connections
     */
    public void startBluetoothServer(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Stop existing server
                if (bluetoothAcceptThread != null) {
                    bluetoothAcceptThread.cancel();
                    bluetoothAcceptThread = null;
                }
                
                // Start new server
                bluetoothAcceptThread = new AcceptThread();
                bluetoothAcceptThread.start();
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "Bluetooth server started");
                result.put("serviceName", SERVICE_NAME);
                result.put("uuid", RESQPEERNET_UUID.toString());
                result.put("timestamp", System.currentTimeMillis());
                
                sendSuccess(callbackContext, result);
                Log.i(TAG, "Bluetooth server started");
                
            } catch (Exception e) {
                Log.e(TAG, "Error starting Bluetooth server", e);
                sendError(callbackContext, "Failed to start Bluetooth server: " + e.getMessage());
            }
        });
    }

    /**
     * Stop Bluetooth server
     */
    public void stopBluetoothServer(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        executorService.execute(() -> {
            try {
                if (bluetoothAcceptThread != null) {
                    bluetoothAcceptThread.cancel();
                    bluetoothAcceptThread = null;
                }
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("message", "Bluetooth server stopped");
                result.put("timestamp", System.currentTimeMillis());
                
                sendSuccess(callbackContext, result);
                Log.i(TAG, "Bluetooth server stopped");
                
            } catch (Exception e) {
                Log.e(TAG, "Error stopping Bluetooth server", e);
                sendError(callbackContext, "Failed to stop Bluetooth server: " + e.getMessage());
            }
        });
    }

    // ==================== MESSAGING METHODS ====================

    /**
     * Send message to connected device
     */
    public void sendMessage(final String deviceId, final String message, final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        executorService.execute(() -> {
            try {
                ConnectedDevice device = connectedDevices.get(deviceId);
                if (device == null) {
                    sendError(callbackContext, "Device not connected: " + deviceId);
                    return;
                }
                
                boolean sent = device.sendMessage(message);
                
                JSONObject result = new JSONObject();
                result.put("success", sent);
                result.put("deviceId", deviceId);
                result.put("deviceName", device.getDeviceName());
                result.put("messageLength", message.length());
                result.put("timestamp", System.currentTimeMillis());
                
                if (sent) {
                    sendSuccess(callbackContext, result);
                    Log.d(TAG, "Message sent to: " + device.getDeviceName());
                } else {
                    sendError(callbackContext, "Failed to send message to device: " + deviceId);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                sendError(callbackContext, "Failed to send message: " + e.getMessage());
            }
        });
    }

    /**
     * Broadcast message to all connected devices
     */
    public void broadcastMessage(final String message, final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        executorService.execute(() -> {
            try {
                int successCount = 0;
                int totalDevices = connectedDevices.size();
                
                for (ConnectedDevice device : connectedDevices.values()) {
                    if (device.sendMessage(message)) {
                        successCount++;
                    }
                }
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("sentTo", successCount);
                result.put("totalDevices", totalDevices);
                result.put("message", "Broadcasted to " + successCount + "/" + totalDevices + " devices");
                result.put("timestamp", System.currentTimeMillis());
                
                sendSuccess(callbackContext, result);
                Log.d(TAG, "Broadcasted message to " + successCount + "/" + totalDevices + " devices");
                
            } catch (Exception e) {
                Log.e(TAG, "Error broadcasting message", e);
                sendError(callbackContext, "Failed to broadcast message: " + e.getMessage());
            }
        });
    }

    // ==================== MANAGEMENT METHODS ====================

    /**
     * Get connected devices
     */
    public void getConnectedDevices(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        executorService.execute(() -> {
            try {
                JSONArray devicesArray = new JSONArray();
                
                for (ConnectedDevice device : connectedDevices.values()) {
                    JSONObject deviceInfo = new JSONObject();
                    deviceInfo.put("deviceId", device.getDeviceId());
                    deviceInfo.put("deviceName", device.getDeviceName());
                    deviceInfo.put("connectionType", device.getConnectionType());
                    deviceInfo.put("isConnected", device.isConnected());
                    deviceInfo.put("lastActivity", device.getLastActivity());
                    
                    devicesArray.put(deviceInfo);
                }
                
                JSONObject result = new JSONObject();
                result.put("devices", devicesArray);
                result.put("count", connectedDevices.size());
                result.put("timestamp", System.currentTimeMillis());
                
                sendSuccess(callbackContext, result);
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting connected devices", e);
                sendError(callbackContext, "Failed to get connected devices: " + e.getMessage());
            }
        });
    }

    /**
     * Disconnect from device
     */
    public void disconnectDevice(final String deviceId, final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        executorService.execute(() -> {
            try {
                ConnectedDevice device = connectedDevices.get(deviceId);
                if (device != null) {
                    device.disconnect();
                    connectedDevices.remove(deviceId);
                    Log.i(TAG, "Disconnected from device: " + deviceId);
                }
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("deviceId", deviceId);
                result.put("message", "Device disconnected");
                result.put("timestamp", System.currentTimeMillis());
                
                sendSuccess(callbackContext, result);
                
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting device", e);
                sendError(callbackContext, "Failed to disconnect device: " + e.getMessage());
            }
        });
    }

    /**
     * Disconnect all devices
     */
    public void disconnectAllDevices(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        executorService.execute(() -> {
            try {
                int disconnectedCount = 0;
                
                for (ConnectedDevice device : connectedDevices.values()) {
                    device.disconnect();
                    disconnectedCount++;
                }
                
                connectedDevices.clear();
                
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("disconnectedCount", disconnectedCount);
                result.put("message", "Disconnected " + disconnectedCount + " devices");
                result.put("timestamp", System.currentTimeMillis());
                
                sendSuccess(callbackContext, result);
                Log.i(TAG, "Disconnected all devices: " + disconnectedCount);
                
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting all devices", e);
                sendError(callbackContext, "Failed to disconnect all devices: " + e.getMessage());
            }
        });
    }

    // ==================== EVENT MANAGEMENT ====================

    /**
     * Start listening for device discovery events
     */
    public void startDiscoveryListener(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        this.discoveryCallbackRef = new WeakReference<>(callbackContext);
        
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        
        Log.i(TAG, "Bluetooth discovery listener started");
    }

    /**
     * Start listening for connection events
     */
    public void startConnectionListener(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        this.connectionCallbackRef = new WeakReference<>(callbackContext);
        
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        
        Log.i(TAG, "Connection listener started");
    }

    /**
     * Start listening for message events
     */
    public void startMessageListener(final CallbackContext callbackContext) {
        if (isDestroyed.get()) {
            sendError(callbackContext, "PeerNetworkingBridge has been destroyed");
            return;
        }
        
        this.messageCallbackRef = new WeakReference<>(callbackContext);
        
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        
        Log.i(TAG, "Message listener started");
    }

    // ==================== PRIVATE CLASSES ====================

    /**
     * Thread for accepting incoming Bluetooth connections
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;
        private final AtomicBoolean isRunning;
        
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            isRunning = new AtomicBoolean(true);
            
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, RESQPEERNET_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Error creating Bluetooth server socket", e);
            }
            serverSocket = tmp;
        }
        
        public void run() {
            BluetoothSocket socket = null;
            
            while (isRunning.get()) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Error accepting Bluetooth connection", e);
                    break;
                }
                
                if (socket != null) {
                    // Connection accepted
                    ConnectedDevice device = new ConnectedDevice(socket);
                    connectedDevices.put(device.getDeviceId(), device);
                    device.start();
                    
                    sendConnectionEvent(device, "CONNECTED");
                }
            }
        }
        
        public void cancel() {
            isRunning.set(false);
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing server socket", e);
            }
        }
    }

    /**
     * Thread for connecting to Bluetooth device
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        
        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            
            try {
                tmp = device.createRfcommSocketToServiceRecord(RESQPEERNET_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Error creating Bluetooth socket", e);
            }
            socket = tmp;
        }
        
        public void run() {
            bluetoothAdapter.cancelDiscovery();
            
            try {
                socket.connect();
                
                ConnectedDevice connectedDevice = new ConnectedDevice(socket);
                connectedDevices.put(connectedDevice.getDeviceId(), connectedDevice);
                connectedDevice.start();
                
                sendConnectionEvent(connectedDevice, "CONNECTED");
                
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to Bluetooth device", e);
                sendConnectionErrorEvent(device.getAddress(), "Connection failed: " + e.getMessage());
                
                try {
                    socket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Error closing socket", closeException);
                }
            }
        }
    }

    /**
     * Represents a connected device
     */
    private class ConnectedDevice {
        private final String deviceId;
        private final String deviceName;
        private final String connectionType;
        private final BluetoothSocket bluetoothSocket;
        private final long connectTime;
        private volatile boolean isConnected;
        
        private InputStream inputStream;
        private OutputStream outputStream;
        
        public ConnectedDevice(BluetoothSocket socket) {
            this.bluetoothSocket = socket;
            this.deviceId = socket.getRemoteDevice().getAddress();
            this.deviceName = socket.getRemoteDevice().getName();
            this.connectionType = "BLUETOOTH";
            this.connectTime = System.currentTimeMillis();
            this.isConnected = true;
            
            try {
                this.inputStream = socket.getInputStream();
                this.outputStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error getting socket streams", e);
            }
        }
        
        public void start() {
            // Start listening for incoming messages
            executorService.execute(this::listenForMessages);
        }
        
        private void listenForMessages() {
            byte[] buffer = new byte[1024];
            int bytes;
            
            while (isConnected) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String message = new String(buffer, 0, bytes);
                        sendMessageEvent(this, message);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from input stream", e);
                    disconnect();
                    break;
                }
            }
        }
        
        public boolean sendMessage(String message) {
            if (!isConnected || outputStream == null) {
                return false;
            }
            
            try {
                outputStream.write(message.getBytes());
                outputStream.flush();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error sending message", e);
                disconnect();
                return false;
            }
        }
        
        public void disconnect() {
            isConnected = false;
            
            try {
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
            
            connectedDevices.remove(deviceId);
            sendConnectionEvent(this, "DISCONNECTED");
        }
        
        // Getters
        public String getDeviceId() { return deviceId; }
        public String getDeviceName() { return deviceName; }
        public String getConnectionType() { return connectionType; }
        public boolean isConnected() { return isConnected; }
        public long getLastActivity() { return System.currentTimeMillis(); }
    }

    // ==================== HELPER METHODS ====================

    private void registerBluetoothDiscoveryReceiver() {
        if (bluetoothDiscoveryReceiver != null) {
            unregisterBluetoothDiscoveryReceiver();
        }
        
        bluetoothDiscoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        sendDeviceDiscoveryEvent(device, "BLUETOOTH");
                        Log.d(TAG, "Discovered device: " + device.getName() + " (" + device.getAddress() + ")");
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    sendDiscoveryFinishedEvent("BLUETOOTH");
                    isDiscovering = false;
                    Log.i(TAG, "Bluetooth discovery finished");
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(bluetoothDiscoveryReceiver, filter);
    }
    
    private void unregisterBluetoothDiscoveryReceiver() {
        if (bluetoothDiscoveryReceiver != null) {
            try {
                context.unregisterReceiver(bluetoothDiscoveryReceiver);
                bluetoothDiscoveryReceiver = null;
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering Bluetooth discovery receiver", e);
            }
        }
    }

    // ==================== EVENT SENDING METHODS ====================

    private void sendDeviceDiscoveryEvent(BluetoothDevice device, String discoveryType) {
        try {
            CallbackContext cb = discoveryCallbackRef != null ? discoveryCallbackRef.get() : null;
            if (cb == null) return;
            
            JSONObject event = new JSONObject();
            event.put("type", "deviceDiscovered");
            event.put("discoveryType", discoveryType);
            event.put("deviceId", device.getAddress());
            event.put("deviceName", device.getName());
            event.put("timestamp", System.currentTimeMillis());
            
            PluginResult result = new PluginResult(PluginResult.Status.OK, event);
            result.setKeepCallback(true);
            cb.sendPluginResult(result);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating discovery event", e);
        }
    }
    
    private void sendDiscoveryFinishedEvent(String discoveryType) {
        try {
            CallbackContext cb = discoveryCallbackRef != null ? discoveryCallbackRef.get() : null;
            if (cb == null) return;
            
            JSONObject event = new JSONObject();
            event.put("type", "discoveryFinished");
            event.put("discoveryType", discoveryType);
            event.put("timestamp", System.currentTimeMillis());
            
            PluginResult result = new PluginResult(PluginResult.Status.OK, event);
            result.setKeepCallback(true);
            cb.sendPluginResult(result);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating discovery finished event", e);
        }
    }
    
    private void sendConnectionEvent(ConnectedDevice device, String connectionStatus) {
        try {
            CallbackContext cb = connectionCallbackRef != null ? connectionCallbackRef.get() : null;
            if (cb == null) return;
            
            JSONObject event = new JSONObject();
            event.put("type", "connectionStatusChanged");
            event.put("deviceId", device.getDeviceId());
            event.put("deviceName", device.getDeviceName());
            event.put("connectionType", device.getConnectionType());
            event.put("status", connectionStatus);
            event.put("timestamp", System.currentTimeMillis());
            
            PluginResult result = new PluginResult(PluginResult.Status.OK, event);
            result.setKeepCallback(true);
            cb.sendPluginResult(result);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating connection event", e);
        }
    }
    
    private void sendConnectionErrorEvent(String deviceId, String error) {
        try {
            CallbackContext cb = connectionCallbackRef != null ? connectionCallbackRef.get() : null;
            if (cb == null) return;
            
            JSONObject event = new JSONObject();
            event.put("type", "connectionError");
            event.put("deviceId", deviceId);
            event.put("error", error);
            event.put("timestamp", System.currentTimeMillis());
            
            PluginResult result = new PluginResult(PluginResult.Status.OK, event);
            result.setKeepCallback(true);
            cb.sendPluginResult(result);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating connection error event", e);
        }
    }
    
    private void sendMessageEvent(ConnectedDevice device, String message) {
        try {
            CallbackContext cb = messageCallbackRef != null ? messageCallbackRef.get() : null;
            if (cb == null) return;
            
            JSONObject event = new JSONObject();
            event.put("type", "messageReceived");
            event.put("deviceId", device.getDeviceId());
            event.put("deviceName", device.getDeviceName());
            event.put("message", message);
            event.put("timestamp", System.currentTimeMillis());
            
            PluginResult result = new PluginResult(PluginResult.Status.OK, event);
            result.setKeepCallback(true);
            cb.sendPluginResult(result);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating message event", e);
        }
    }

    // ==================== UTILITY METHODS ====================

    private JSONObject createResult(boolean success, String message, String type) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("success", success);
        result.put("message", message);
        result.put("type", type);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    private void sendSuccess(CallbackContext callbackContext, JSONObject result) {
        if (isDestroyed.get() || callbackContext == null) return;
        
        mainHandler.post(() -> {
            if (!isDestroyed.get() && callbackContext != null) {
                callbackContext.success(result);
            }
        });
    }
    
    private void sendError(CallbackContext callbackContext, String message) {
        if (isDestroyed.get() || callbackContext == null) return;
        
        mainHandler.post(() -> {
            if (!isDestroyed.get() && callbackContext != null) {
                try {
                    JSONObject error = new JSONObject();
                    error.put("error", message);
                    error.put("code", "P2P_ERROR");
                    callbackContext.error(error);
                } catch (JSONException e) {
                    callbackContext.error("{\"error\":\"" + message.replace("\"", "\\\"") + "\",\"code\":\"P2P_ERROR\"}");
                }
            }
        });
    }

    // ==================== CLEANUP ====================
    
    public void destroy() {
        if (isDestroyed.compareAndSet(false, true)) {
            Log.i(TAG, "Destroying PeerNetworkingBridge");
            
            // Stop discovery
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            
            // Unregister receiver
            unregisterBluetoothDiscoveryReceiver();
            
            // Stop Bluetooth server
            if (bluetoothAcceptThread != null) {
                bluetoothAcceptThread.cancel();
                bluetoothAcceptThread = null;
            }
            
            // Disconnect all devices
            for (ConnectedDevice device : connectedDevices.values()) {
                device.disconnect();
            }
            connectedDevices.clear();
            
            // Shutdown executor
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            
            // Clear callbacks
            discoveryCallbackRef = null;
            connectionCallbackRef = null;
            messageCallbackRef = null;
            
            Log.i(TAG, "PeerNetworkingBridge (Bluetooth) destroyed successfully");
        }
    }
}