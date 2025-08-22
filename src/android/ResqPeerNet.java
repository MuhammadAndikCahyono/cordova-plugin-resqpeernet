package org.apache.cordova.resqpeernet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import android.util.Log;
import android.util.Base64;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

// WebRTC imports - requires org.webrtc dependency in your Gradle
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

/**
 * ResqPeerNet - Cordova plugin native (Android)
 * Added: basic WebRTC DataChannel plumbing + file transfer helpers (chunking + progress)
 * and WebTorrent bridge (signaling + piece transfer hooks) for JS-side WebTorrent.
 *
 * NOTE: You must add org.webrtc (Google WebRTC) dependency to your Android project.
 */
public class ResqPeerNet extends CordovaPlugin implements
        WifiP2pManager.PeerListListener,
        WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = "ResqPeerNet";

    // --- Wi-Fi Direct ---
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WiFiDirectBroadcastReceiver receiver;
    private IntentFilter intentFilter;

    // --- Sockets (Wi-Fi Direct TCP) ---
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ExecutorService ioPool = Executors.newCachedThreadPool();

    // --- Callbacks ke JS (event stream) ---
    private CallbackContext eventCallback;

    // --- BLE ---
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private ScanCallback bleScanCallback;

    // --- mDNS ---
    private JmDNS jmdns;
    private ServiceInfo serviceInfo;
    private MulticastSocket multicastSocket;
    private InetAddress mdnsGroup;
    private Thread mdnsListenThread;

    // --- Mesh-Hybrid ---
    private volatile boolean meshHybridRunning = false;

    // --- WebRTC ---
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private DataChannel dataChannel; // single default channel
    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();

    // --- File transfer state via DataChannel ---
    private final Map<String, FileOutputStream> fileStreams = new ConcurrentHashMap<>();
    private final Map<String, Long> fileExpectedSize = new ConcurrentHashMap<>();
    private final Queue<String> pendingChunkQueue = new LinkedList<>();
    private final Map<String, Long> fileBytesReceived = new ConcurrentHashMap<>();

    // --- WebTorrent piece pending queues (separate from file transfer)
    private final Queue<String> wtPendingChunkQueue = new LinkedList<>();
    private final Map<String, Integer> wtPendingPieceIndex = new HashMap<>();

    // Chunk size for sending (16KB)
    private static final int CHUNK_SIZE = 16 * 1024;

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();
        Context ctx = cordova.getActivity().getApplicationContext();

        // Wi-Fi Direct init
        manager = (WifiP2pManager) ctx.getSystemService(Context.WIFI_P2P_SERVICE);
        if (manager != null) {
            channel = manager.initialize(ctx, ctx.getMainLooper(), () -> Log.w(TAG, "P2P channel lost"));
            intentFilter = new IntentFilter();
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
            receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
            cordova.getActivity().registerReceiver(receiver, intentFilter);
        } else {
            Log.e(TAG, "WifiP2pManager not available");
        }

        initWebRTCFactory(ctx);

        Log.i(TAG, "ResqPeerNet initialized");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (receiver != null) cordova.getActivity().unregisterReceiver(receiver);
        } catch (Exception ignore) {}

        // Tutup semua resource
        safeClose(serverSocket);
        safeClose(clientSocket);
        stopBLE();
        stopMDNSServer();
        stopMDNSListener();
        closeWebRTC();
        ioPool.shutdownNow();
        Log.i(TAG, "ResqPeerNet destroyed");
    }

    // =========================================================
    // Cordova bridge
    // =========================================================
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext cb) throws JSONException {
        Log.d(TAG, "Action: " + action);

        switch (action) {
            case "listenEvents":
                eventCallback = cb;
                keepAlive(cb);
                return true;

            case "connect":
                connect(args, cb);
                return true;

            case "disconnect":
                disconnect(cb);
                return true;

            case "sendMessage":
                sendTCPMessage(args.getString(0), cb);
                return true;

            case "discoverPeers":
                discoverPeers(cb);
                return true;

            case "getPeers":
                cb.success("Peer discovery requested");
                return true;

            // mDNS
            case "startMDNSServer":
                startMDNSServer(args.getString(0), args.getInt(1), cb);
                return true;

            case "stopMDNSServer":
                stopMDNSServer();
                cb.success("mDNS server stopped");
                return true;

            case "startMDNSDiscovery":
                startMDNSDiscovery(cb);
                return true;

            case "startMDNSListener":
                startMDNSListener(cb);
                return true;

            case "sendMDNS":
                sendMDNSMessage(args.getString(0), cb);
                return true;

            // BLE
            case "startBLEScan":
                startBLEScan(cb);
                return true;

            case "stopBLEScan":
                stopBLE();
                cb.success("BLE scan stopped");
                return true;

            // Mesh-Hybrid
            case "startMeshHybrid":
                startMeshHybrid(cb);
                return true;

            case "stopMeshHybrid":
                stopMeshHybrid(cb);
                return true;

            case "sendMeshHybrid":
                sendMeshHybridMessage(args.getString(0), cb);
                return true;

            // --- WebRTC actions ---
            case "initWebRTC":
                initWebRTC(args.optJSONArray(0), cb);
                return true;

            case "createOffer":
                createOffer(cb);
                return true;

            case "createAnswer":
                createAnswer(cb);
                return true;

            case "setRemoteDescription":
                setRemoteDescription(args.getJSONObject(0), cb);
                return true;

            case "addIceCandidate":
                addRemoteIceCandidate(args.getJSONObject(0), cb);
                return true;

            case "createDataChannel":
                createDataChannel(args.optString(0, "resq-data"), cb);
                return true;

            case "sendWebRTC":
                sendDataChannelMessage(args.getString(0), cb);
                return true;

            case "sendFile":
                sendFileFromBase64(args.getString(0), args.getString(1), cb);
                return true;

            case "closeWebRTC":
                closeWebRTC();
                cb.success("WebRTC closed");
                return true;

            // --- WebTorrent bridge actions ---
            case "webtorrentSignal":
                handleWebTorrentSignal(args.getString(0), cb);
                return true;

            case "webtorrentSendPiece":
                sendWebRTCPiece(args, cb);
                return true;

            case "webtorrentRequestPiece":
                requestWebRTCPiece(args, cb);
                return true;

            case "startWebTorrentClient":
                sendEvent("onWebTorrentClientStarted", (JSONObject) null);
                cb.success("WebTorrent bridge ready");
                return true;

            case "stopWebTorrentClient":
                sendEvent("onWebTorrentClientStopped", (JSONObject) null);
                cb.success("WebTorrent bridge stopped");
                return true;

            default:
                return false;
        }
    }

    private void keepAlive(CallbackContext cb) {
        PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        pr.setKeepCallback(true);
        cb.sendPluginResult(pr);
    }

    private synchronized void sendEvent(String type, JSONObject data) {
        if (eventCallback == null) return;
        try {
            JSONObject envelope = new JSONObject();
            envelope.put("type", type);
            if (data != null) envelope.put("data", data);
            PluginResult pr = new PluginResult(PluginResult.Status.OK, envelope);
            pr.setKeepCallback(true);
            eventCallback.sendPluginResult(pr);
        } catch (JSONException e) {
            Log.e(TAG, "sendEvent JSON error", e);
        }
    }

    private synchronized void sendEvent(String type, String jsonString) {
        try {
            sendEvent(type, jsonString == null ? null : new JSONObject(jsonString));
        } catch (JSONException e) {
            Log.e(TAG, "sendEvent(str) JSON parse", e);
        }
    }

    // =========================================================
    // Wi-Fi Direct (Peer discovery + TCP messaging sederhana)
    // =========================================================
    private void discoverPeers(CallbackContext cb) {
        if (manager == null) { cb.error("WifiP2pManager null"); return; }
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() { cb.success("Peer discovery started"); }
            @Override public void onFailure(int reason) { cb.error("Peer discovery failed: " + reason); }
        });
    }

    private void connect(JSONArray args, CallbackContext cb) {
        String type = args.optJSONObject(0) != null ? args.optJSONObject(0).optString("type", "wifi-direct") : "wifi-direct";
        switch (type) {
            case "wifi-direct":
                discoverPeers(cb);
                break;
            case "ble":
                startBLEScan(cb);
                break;
            case "mesh-hybrid":
                startMeshHybrid(cb);
                break;
            default:
                cb.error("Unknown connect type: " + type);
        }
    }

    private void disconnect(CallbackContext cb) {
        if (manager == null) { cb.error("WifiP2pManager null"); return; }
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() { cb.success("Disconnected"); }
            @Override public void onFailure(int reason) { cb.error("Disconnect failed: " + reason); }
        });
    }

    // TCP kirim sederhana setelah terkoneksi (clientSocket sudah terset saat onConnectionInfoAvailable)
    private void sendTCPMessage(String message, CallbackContext cb) {
        ioPool.execute(() -> {
            try {
                if (clientSocket != null && clientSocket.isConnected()) {
                    PrintWriter w = new PrintWriter(clientSocket.getOutputStream(), true);
                    w.println(message);
                    cb.success("Message sent");
                } else {
                    cb.error("Not connected to peer");
                }
            } catch (Exception e) {
                cb.error("Send failed: " + e.getMessage());
            }
        });
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        try {
            JSONArray arr = new JSONArray();
            for (WifiP2pDevice d : peers.getDeviceList()) {
                JSONObject o = new JSONObject();
                o.put("deviceName", d.deviceName);
                o.put("deviceAddress", d.deviceAddress);
                arr.put(o);
            }
            JSONObject data = new JSONObject();
            data.put("peers", arr);
            sendEvent("onWiFiDirectPeers", data);
        } catch (JSONException e) {
            Log.e(TAG, "onPeersAvailable JSON", e);
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        try {
            JSONObject data = new JSONObject();
            data.put("isGroupOwner", info.isGroupOwner);
            data.put("groupFormed", info.groupFormed);
            data.put("ownerAddress", info.groupOwnerAddress != null ? info.groupOwnerAddress.getHostAddress() : null);
            sendEvent("onWiFiDirectConnection", data);
        } catch (JSONException ignore) {}

        if (!info.groupFormed) return;

        if (info.isGroupOwner) {
            // Server
            ioPool.execute(() -> {
                try {
                    safeClose(serverSocket);
                    serverSocket = new ServerSocket(8988);
                    Socket s = serverSocket.accept();
                    clientSocket = s;

                    BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String line;
                    while ((line = r.readLine()) != null) {
                        JSONObject d = new JSONObject();
                        d.put("message", line);
                        sendEvent("onWiFiDirectMessage", d);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Server socket error", e);
                }
            });
        } else {
            // Client
            ioPool.execute(() -> {
                try {
                    safeClose(clientSocket);
                    clientSocket = new Socket();
                    clientSocket.connect(new InetSocketAddress(info.groupOwnerAddress, 8988), 5000);

                    BufferedReader r = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String line;
                    while ((line = r.readLine()) != null) {
                        JSONObject d = new JSONObject();
                        d.put("message", line);
                        sendEvent("onWiFiDirectMessage", d);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Client socket error", e);
                }
            });
        }
    }

    private void safeClose(ServerSocket s) {
        if (s != null) try { s.close(); } catch (Exception ignore) {}
    }
    private void safeClose(Socket s) {
        if (s != null) try { s.close(); } catch (Exception ignore) {}
    }

    // =========================================================
    // BLE (scan minimal + event)
    // =========================================================
    private void startBLEScan(CallbackContext cb) {
        BluetoothManager bm = (BluetoothManager) cordova.getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bm != null ? bm.getAdapter() : null;

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            cb.error("Bluetooth not enabled");
            return;
        }
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanCallback == null) {
            bleScanCallback = new ScanCallback() {
                @Override public void onScanResult(int callbackType, ScanResult result) {
                    BluetoothDevice dev = result.getDevice();
                    try {
                        JSONObject d = new JSONObject();
                        d.put("name", dev != null ? dev.getName() : null);
                        d.put("address", dev != null ? dev.getAddress() : null);
                        sendEvent("onBLEDeviceFound", d);
                    } catch (JSONException ignore) {}
                }
            };
        }
        bluetoothLeScanner.startScan(bleScanCallback);
        cb.success("BLE scan started");
    }

    private void stopBLE() {
        if (bluetoothLeScanner != null && bleScanCallback != null) {
            try { bluetoothLeScanner.stopScan(bleScanCallback); } catch (Exception ignore) {}
        }
        if (bluetoothGatt != null) {
            try { bluetoothGatt.disconnect(); bluetoothGatt.close(); } catch (Exception ignore) {}
            bluetoothGatt = null;
        }
    }

    // =========================================================
    // mDNS (announce/discovery + multicast pesan)
    // =========================================================
    private void startMDNSServer(String serviceName, int port, CallbackContext cb) {
        try {
            WifiManager wifi = (WifiManager) cordova.getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiManager.MulticastLock lock = wifi.createMulticastLock("resqpeernet");
            lock.setReferenceCounted(true);
            lock.acquire();

            InetAddress addr = getLocalIpAddress();
            jmdns = JmDNS.create(addr);
            serviceInfo = ServiceInfo.create("_resqpeernet._tcp.local.", serviceName, port, "ResqPeerNet Service");
            jmdns.registerService(serviceInfo);

            JSONObject d = new JSONObject();
            d.put("name", serviceName);
            d.put("port", port);
            sendEvent("onMDNSServerStarted", d);
            cb.success("mDNS server started");
        } catch (Exception e) {
            cb.error("Start mDNS failed: " + e.getMessage());
        }
    }

    private void stopMDNSServer() {
        try {
            if (jmdns != null) {
                jmdns.unregisterAllServices();
                jmdns.close();
                jmdns = null;
                sendEvent("onMDNSServerStopped", (JSONObject) null);
            }
        } catch (Exception e) {
            Log.e(TAG, "stopMDNSServer", e);
        }
    }

    private void startMDNSDiscovery(CallbackContext cb) {
        try {
            if (jmdns == null) {
                InetAddress addr = getLocalIpAddress();
                jmdns = JmDNS.create(addr);
            }
            jmdns.addServiceListener("_resqpeernet._tcp.local.", new ServiceListener() {
                @Override public void serviceAdded(ServiceEvent event) {
                    jmdns.requestServiceInfo(event.getType(), event.getName(), true);
                }
                @Override public void serviceRemoved(ServiceEvent event) {
                    try {
                        JSONObject d = new JSONObject();
                        d.put("name", event.getName());
                        sendEvent("onMDNSPeerRemoved", d);
                    } catch (JSONException ignore) {}
                }
                @Override public void serviceResolved(ServiceEvent event) {
                    try {
                        String host = event.getInfo().getHostAddresses()[0];
                        int port = event.getInfo().getPort();
                        JSONObject d = new JSONObject();
                        d.put("name", event.getName());
                        d.put("host", host);
                        d.put("port", port);
                        sendEvent("onMDNSPeerFound", d);
                    } catch (JSONException ignore) {}
                }
            });
            cb.success("mDNS discovery started");
        } catch (Exception e) {
            cb.error("Start mDNS discovery failed: " + e.getMessage());
        }
    }

    private void startMDNSListener(CallbackContext cb) {
        try {
            if (multicastSocket == null) {
                mdnsGroup = InetAddress.getByName("224.0.0.251");
                multicastSocket = new MulticastSocket(5353);
                multicastSocket.joinGroup(mdnsGroup);
            }
            mdnsListenThread = new Thread(() -> {
                byte[] buf = new byte[2048];
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        DatagramPacket p = new DatagramPacket(buf, buf.length);
                        multicastSocket.receive(p);
                        String msg = new String(p.getData(), 0, p.getLength());
                        JSONObject d = new JSONObject();
                        try { d.put("message", msg); } catch (JSONException ignore) {}
                        // Emit generic MDNS data event
                        sendEvent("onMDNSData", d);
                        // Try parse signaling messages and specially emit webtorrent signals
                        try {
                            JSONObject parsed = new JSONObject(msg);
                            String t = parsed.optString("type", "");
                            if ("webtorrent-signal".equals(t)) {
                                // forward to JS as dedicated event
                                sendEvent("onWebTorrentSignal", parsed);
                            } else if ("webrtc-sdp".equals(t) || "webrtc-ice".equals(t)) {
                                // forward generic webrtc signaling too
                                sendEvent("onWebTorrentSignal", parsed);
                            }
                        } catch (Exception ignore) {}
                    } catch (Exception e) {
                        Log.e(TAG, "mDNS listen loop", e);
                        break;
                    }
                }
            }, "mdns-listener");
            mdnsListenThread.start();
            cb.success("mDNS listener started");
        } catch (Exception e) {
            cb.error("Start mDNS listener failed: " + e.getMessage());
        }
    }

    private void stopMDNSListener() {
        try {
            if (mdnsListenThread != null) mdnsListenThread.interrupt();
            if (multicastSocket != null) { multicastSocket.close(); multicastSocket = null; }
        } catch (Exception ignore) {}
    }

    private void sendMDNSMessage(String message, CallbackContext cb) {
        try {
            if (multicastSocket == null) {
                mdnsGroup = InetAddress.getByName("224.0.0.251");
                multicastSocket = new MulticastSocket(5353);
                multicastSocket.joinGroup(mdnsGroup);
            }
            DatagramPacket pkt = new DatagramPacket(message.getBytes(), message.length(), mdnsGroup, 5353);
            multicastSocket.send(pkt);
            cb.success("mDNS message sent");
        } catch (Exception e) {
            cb.error("Send mDNS failed: " + e.getMessage());
        }
    }

    // =========================================================
    // WebRTC implementation (basic) + file transfer helpers
    // =========================================================
    private void initWebRTCFactory(Context ctx) {
        try {
            PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(ctx)
                            .setEnableInternalTracer(true)
                            .createInitializationOptions());

            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            DefaultVideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(null, true, true);
            DefaultVideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(null);

            peerConnectionFactory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .setVideoEncoderFactory(encoderFactory)
                    .setVideoDecoderFactory(decoderFactory)
                    .createPeerConnectionFactory();

            iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

            Log.i(TAG, "WebRTC factory initialized");
        } catch (Exception e) {
            Log.e(TAG, "initWebRTCFactory error", e);
        }
    }

    private void initWebRTC(JSONArray iceArray, CallbackContext cb) {
        if (iceArray != null) {
            iceServers.clear();
            for (int i = 0; i < iceArray.length(); i++) {
                String url = iceArray.optString(i);
                if (url != null && !url.isEmpty()) {
                    iceServers.add(PeerConnection.IceServer.builder(url).createIceServer());
                }
            }
        }
        cb.success("WebRTC initialized");
    }

    private void createPeerConnectionIfNeeded() {
        if (peerConnection != null) return;

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override public void onSignalingChange(PeerConnection.SignalingState signalingState) {}
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {}
            @Override public void onIceConnectionReceivingChange(boolean b) {}
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}
            @Override public void onIceCandidate(IceCandidate iceCandidate) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("sdpMid", iceCandidate.sdpMid);
                    o.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                    o.put("candidate", iceCandidate.sdp);
                    sendEvent("onIceCandidate", o);
                } catch (JSONException ignore) {}
            }
            @Override public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}
            @Override public void onAddStream(org.webrtc.MediaStream mediaStream) {}
            @Override public void onRemoveStream(org.webrtc.MediaStream mediaStream) {}
            @Override public void onDataChannel(DataChannel dc) {
                dataChannel = dc;
                setupDataChannelHandlers(dc);
                sendEvent("onDataChannelOpen", (JSONObject) null);
            }
            @Override public void onRenegotiationNeeded() {}
            @Override public void onAddTrack(RtpReceiver rtpReceiver, org.webrtc.MediaStream[] mediaStreams) {}
        });
    }

    private void createDataChannel(String label, CallbackContext cb) {
        createPeerConnectionIfNeeded();
        if (peerConnection == null) { cb.error("PeerConnection not available"); return; }
        DataChannel.Init init = new DataChannel.Init();
        init.ordered = true;
        init.maxRetransmits = -1;
        dataChannel = peerConnection.createDataChannel(label != null ? label : "resq-data", init);
        setupDataChannelHandlers(dataChannel);
        cb.success("DataChannel created");
    }

    private void setupDataChannelHandlers(DataChannel dc) {
        dc.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {}

            @Override
            public void onStateChange() {
                try {
                    JSONObject s = new JSONObject();
                    s.put("state", dc.state().toString());
                    sendEvent("onDataChannelState", s);
                } catch (JSONException ignore) {}
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                try {
                    byte[] bytes = new byte[buffer.data.capacity()];
                    buffer.data.get(bytes);
                    if (buffer.binary) {
                        // Binary chunk — match to pending header queue (file transfer first)
                        String fileId;
                        synchronized (pendingChunkQueue) {
                            fileId = pendingChunkQueue.poll();
                        }
                        if (fileId != null) {
                            FileOutputStream fos = fileStreams.get(fileId);
                            if (fos != null) {
                                fos.write(bytes);
                                long newReceived = fileBytesReceived.getOrDefault(fileId, 0L) + bytes.length;
                                fileBytesReceived.put(fileId, newReceived);
                                long expected = fileExpectedSize.getOrDefault(fileId, -1L);
                                JSONObject progress = new JSONObject();
                                progress.put("id", fileId);
                                progress.put("received", newReceived);
                                progress.put("expected", expected);
                                sendEvent("onFileProgress", progress);
                                if (expected > 0 && newReceived >= expected) {
                                    try { fos.close(); } catch (Exception ignore) {}
                                    fileStreams.remove(fileId);
                                    fileExpectedSize.remove(fileId);
                                    fileBytesReceived.remove(fileId);
                                    JSONObject done = new JSONObject();
                                    done.put("id", fileId);
                                    done.put("path", getCacheFilePathForId(fileId));
                                    sendEvent("onFileReceived", done);
                                }
                            }
                        } else {
                            // maybe it's a WebTorrent piece
                            String wtId;
                            synchronized (wtPendingChunkQueue) {
                                wtId = wtPendingChunkQueue.poll();
                            }
                            if (wtId != null) {
                                Integer pieceIndex = wtPendingPieceIndex.remove(wtId);
                                if (pieceIndex != null) {
                                    // encode piece as base64 and emit event to JS
                                    String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                                    JSONObject evt = new JSONObject();
                                    evt.put("id", wtId);
                                    evt.put("pieceIndex", pieceIndex);
                                    evt.put("data", b64);
                                    sendEvent("onWebTorrentPiece", evt);
                                }
                            } else {
                                Log.w(TAG, "Received binary chunk but no pending header");
                            }
                        }
                    } else {
                        // Text control messages (JSON)
                        String msg = new String(bytes);
                        JSONObject o = new JSONObject(msg);
                        String t = o.optString("type", "");
                        if ("file-start".equals(t)) {
                            String id = o.getString("id");
                            String name = o.optString("name", "file");
                            long size = o.optLong("size", -1);
                            prepareReceivingFile(id, name, size);
                        } else if ("file-chunk".equals(t)) {
                            String id = o.getString("id");
                            synchronized (pendingChunkQueue) { pendingChunkQueue.add(id); }
                        } else if ("file-end".equals(t)) {
                            // noop — file completion handled on chunk receipt
                        } else if ("wt-piece-header".equals(t)) {
                            String id = o.getString("id");
                            int idx = o.getInt("pieceIndex");
                            synchronized (wtPendingChunkQueue) { wtPendingChunkQueue.add(id); }
                            wtPendingPieceIndex.put(id, idx);
                        } else if ("wt-request".equals(t)) {
                            // peer requested a piece; forward to JS
                            JSONObject ev = new JSONObject();
                            ev.put("id", o.getString("id"));
                            ev.put("pieceIndex", o.getInt("pieceIndex"));
                            sendEvent("onWebTorrentRequest", ev);
                        } else if ("wt-signal".equals(t)) {
                            sendEvent("onWebTorrentSignal", o);
                        } else {
                            JSONObject evt = new JSONObject();
                            evt.put("message", msg);
                            sendEvent("onDataChannelMessage", evt);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "dataChannel onMessage", e);
                }
            }
        });
    }

    private void createOffer(CallbackContext cb) {
        createPeerConnectionIfNeeded();
        if (peerConnection == null) { cb.error("PeerConnection not available"); return; }
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        peerConnection.createOffer(new SdpObserver() {
            @Override public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
                    @Override public void onSetSuccess() {
                        try {
                            JSONObject o = new JSONObject();
                            o.put("type", sessionDescription.type.canonicalForm());
                            o.put("sdp", sessionDescription.description);
                            sendEvent("onLocalDescription", o);
                            cb.success(o);
                        } catch (JSONException e) { cb.error(e.getMessage()); }
                    }
                    @Override public void onCreateFailure(String s) {}
                    @Override public void onSetFailure(String s) { cb.error(s); }
                }, sessionDescription);
            }
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String s) { cb.error(s); }
            @Override public void onSetFailure(String s) { cb.error(s); }
        }, constraints);
    }

    private void createAnswer(CallbackContext cb) {
        if (peerConnection == null) { cb.error("PeerConnection not available"); return; }
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        peerConnection.createAnswer(new SdpObserver() {
            @Override public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
                    @Override public void onSetSuccess() {
                        try {
                            JSONObject o = new JSONObject();
                            o.put("type", sessionDescription.type.canonicalForm());
                            o.put("sdp", sessionDescription.description);
                            sendEvent("onLocalDescription", o);
                            cb.success(o);
                        } catch (JSONException e) { cb.error(e.getMessage()); }
                    }
                    @Override public void onCreateFailure(String s) {}
                    @Override public void onSetFailure(String s) { cb.error(s); }
                }, sessionDescription);
            }
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String s) { cb.error(s); }
            @Override public void onSetFailure(String s) { cb.error(s); }
        }, constraints);
    }

    private void setRemoteDescription(JSONObject desc, CallbackContext cb) {
        try {
            String type = desc.getString("type");
            String sdp = desc.getString("sdp");
            final SessionDescription.Type sdptype = SessionDescription.Type.fromCanonicalForm(type);
            final SessionDescription sd = new SessionDescription(sdptype, sdp);
            createPeerConnectionIfNeeded();
            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
                @Override public void onSetSuccess() { cb.success("Remote description set"); }
                @Override public void onCreateFailure(String s) { cb.error(s); }
                @Override public void onSetFailure(String s) { cb.error(s); }
            }, sd);
        } catch (Exception e) { cb.error("setRemoteDescription failed: " + e.getMessage()); }
    }

    private void addRemoteIceCandidate(JSONObject cand, CallbackContext cb) {
        try {
            String sdpMid = cand.getString("sdpMid");
            int sdpMLineIndex = cand.getInt("sdpMLineIndex");
            String candidate = cand.getString("candidate");
            IceCandidate ic = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
            if (peerConnection != null) peerConnection.addIceCandidate(ic);
            cb.success("IceCandidate added");
        } catch (Exception e) { cb.error("addIceCandidate failed: " + e.getMessage()); }
    }

    private void sendDataChannelMessage(String message, CallbackContext cb) {
        if (dataChannel == null) { cb.error("No data channel"); return; }
        try {
            byte[] bytes = message.getBytes();
            DataChannel.Buffer buffer = new DataChannel.Buffer(java.nio.ByteBuffer.wrap(bytes), false);
            boolean ok = dataChannel.send(buffer);
            if (ok) cb.success("Sent"); else cb.error("Send failed");
        } catch (Exception e) { cb.error("Send failed: " + e.getMessage()); }
    }

    // sendFileFromBase64: JS supplies filename and base64 string; plugin will chunk and send
    private void sendFileFromBase64(String filename, String base64Data, CallbackContext cb) {
        if (dataChannel == null || dataChannel.state() != DataChannel.State.OPEN) { cb.error("DataChannel not open"); return; }
        try {
            byte[] data = Base64.decode(base64Data, Base64.DEFAULT);
            ioPool.execute(() -> {
                try {
                    sendFileChunksOverDataChannel(filename, data);
                    cb.success("File send started");
                } catch (Exception e) {
                    cb.error("sendFile failed: " + e.getMessage());
                }
            });
        } catch (Exception e) { cb.error("decode base64 failed: " + e.getMessage()); }
    }

    private void sendFileChunksOverDataChannel(String filename, byte[] data) throws Exception {
        String id = UUID.randomUUID().toString();
        long total = data.length;
        JSONObject start = new JSONObject();
        start.put("type", "file-start");
        start.put("id", id);
        start.put("name", filename);
        start.put("size", total);
        sendTextControl(start);

        int offset = 0;
        while (offset < data.length) {
            int len = Math.min(CHUNK_SIZE, data.length - offset);
            JSONObject chunkHeader = new JSONObject();
            chunkHeader.put("type", "file-chunk");
            chunkHeader.put("id", id);
            chunkHeader.put("length", len);
            sendTextControl(chunkHeader);
            byte[] chunk = new byte[len];
            System.arraycopy(data, offset, chunk, 0, len);
            DataChannel.Buffer buf = new DataChannel.Buffer(java.nio.ByteBuffer.wrap(chunk), true);
            dataChannel.send(buf);
            offset += len;
        }
        JSONObject end = new JSONObject();
        end.put("type", "file-end");
        end.put("id", id);
        sendTextControl(end);
    }

    private void sendTextControl(JSONObject o) throws JSONException {
        byte[] b = o.toString().getBytes();
        DataChannel.Buffer buf = new DataChannel.Buffer(java.nio.ByteBuffer.wrap(b), false);
        dataChannel.send(buf);
    }

    private void prepareReceivingFile(String id, String name, long size) {
        try {
            File cacheDir = cordova.getActivity().getCacheDir();
            File f = new File(cacheDir, id + "_" + name);
            if (f.exists()) f.delete();
            FileOutputStream fos = new FileOutputStream(f, true);
            fileStreams.put(id, fos);
            fileExpectedSize.put(id, size);
            fileBytesReceived.put(id, 0L);
            storeCacheFilePathForId(id, f.getAbsolutePath());
            JSONObject evt = new JSONObject();
            try { evt.put("id", id); evt.put("name", name); evt.put("expected", size); } catch (JSONException ignore) {}
            sendEvent("onFileStart", evt);
        } catch (Exception e) {
            Log.e(TAG, "prepareReceivingFile", e);
        }
    }

    // =========================================================
    // WebTorrent bridge methods
    // =========================================================
    // handleWebTorrentSignal: accept signaling JSON from JS and broadcast via mDNS
    private void handleWebTorrentSignal(String jsonStr, CallbackContext cb) {
        try {
            JSONObject j = new JSONObject(jsonStr);
            // tag message type for mDNS
            j.put("type", j.optString("type", "webtorrent-signal"));
            // broadcast via mDNS
            sendMDNSMessage(j.toString(), new CallbackContext("mdnsSendWT", null));
            // also emit back to JS that we forwarded it
            sendEvent("onWebTorrentSignal", j);
            cb.success("webtorrent signal forwarded");
        } catch (Exception e) {
            cb.error("webtorrentSignal failed: " + e.getMessage());
        }
    }

    // sendWebRTCPiece: [ peerId, pieceIndex, base64Data ]
    private void sendWebRTCPiece(JSONArray args, CallbackContext cb) {
        if (dataChannel == null || dataChannel.state() != DataChannel.State.OPEN) { cb.error("DataChannel not open"); return; }
        try {
            String peerId = args.getString(0);
            int pieceIndex = args.getInt(1);
            String base64 = args.getString(2);
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            ioPool.execute(() -> {
                try {
                    String id = UUID.randomUUID().toString();
                    JSONObject header = new JSONObject();
                    header.put("type", "wt-piece-header");
                    header.put("id", id);
                    header.put("pieceIndex", pieceIndex);
                    header.put("length", data.length);
                    header.put("to", peerId);
                    sendTextControl(header);
                    // send binary chunk
                    DataChannel.Buffer buf = new DataChannel.Buffer(java.nio.ByteBuffer.wrap(data), true);
                    dataChannel.send(buf);
                    cb.success("piece-sent");
                } catch (Exception e) {
                    cb.error("sendWebRTCPiece failed: " + e.getMessage());
                }
            });
        } catch (Exception e) { cb.error("sendWebRTCPiece parse failed: " + e.getMessage()); }
    }

    // requestWebRTCPiece: [ peerId, pieceIndex ] -> send request control
    private void requestWebRTCPiece(JSONArray args, CallbackContext cb) {
        if (dataChannel == null || dataChannel.state() != DataChannel.State.OPEN) { cb.error("DataChannel not open"); return; }
        try {
            String peerId = args.getString(0);
            int pieceIndex = args.getInt(1);
            JSONObject req = new JSONObject();
            req.put("type", "wt-request");
            req.put("id", UUID.randomUUID().toString());
            req.put("to", peerId);
            req.put("pieceIndex", pieceIndex);
            sendTextControl(req);
            cb.success("request-sent");
        } catch (Exception e) { cb.error("requestWebRTCPiece failed: " + e.getMessage()); }
    }

    // =========================================================
    // Util
    // =========================================================
    // Map id -> path
    private final Map<String, String> cachePaths = new HashMap<>();
    private void storeCacheFilePathForId(String id, String path) { synchronized (cachePaths) { cachePaths.put(id, path); } }
    private String getCacheFilePathForId(String id) { synchronized (cachePaths) { return cachePaths.get(id); } }

    private void closeWebRTC() {
        if (dataChannel != null) {
            try { dataChannel.close(); } catch (Exception ignore) {}
            dataChannel = null;
        }
        if (peerConnection != null) {
            try { peerConnection.close(); } catch (Exception ignore) {}
            peerConnection = null;
        }
        if (peerConnectionFactory != null) {
            try { peerConnectionFactory.dispose(); } catch (Exception ignore) {}
            peerConnectionFactory = null;
        }
    }

    // =========================================================
    // Mesh-Hybrid (starter)
    // =========================================================
    private void startMeshHybrid(CallbackContext cb) {
        if (meshHybridRunning) { cb.success("Mesh-Hybrid already running"); return; }
        startMDNSDiscovery(new CallbackContext("mdnsDisc", null));
        startMDNSListener(new CallbackContext("mdnsListen", null));
        try { discoverPeers(new CallbackContext("p2pDisc", null)); } catch (Exception ignore) {}
        startBLEScan(new CallbackContext("bleScan", null));
        meshHybridRunning = true;
        sendEvent("onMeshHybridStarted", (JSONObject) null);
        cb.success("Mesh-Hybrid started");
    }

    private void stopMeshHybrid(CallbackContext cb) {
        if (!meshHybridRunning) { cb.success("Mesh-Hybrid already stopped"); return; }
        stopMDNSListener();
        stopMDNSServer();
        stopBLE();
        meshHybridRunning = false;
        sendEvent("onMeshHybridStopped", (JSONObject) null);
        cb.success("Mesh-Hybrid stopped");
    }

    private void sendMeshHybridMessage(String message, CallbackContext cb) {
        sendMDNSMessage(message, new CallbackContext("mdnsSend", null));
        sendTCPMessage(message, new CallbackContext("tcpSend", null));
        if (dataChannel != null && dataChannel.state() == DataChannel.State.OPEN) {
            try { sendDataChannelMessage(message, new CallbackContext("dcSend", null)); } catch (Exception ignore) {}
        }
        cb.success("Mesh-Hybrid message dispatched");
    }

    @SuppressLint("WifiManagerLeak")
    private InetAddress getLocalIpAddress() throws Exception {
        WifiManager wm = (WifiManager) cordova.getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ip = wm.getConnectionInfo().getIpAddress();
        byte[] bytes = new byte[] {
                (byte) (ip & 0xff),
                (byte) (ip >> 8 & 0xff),
                (byte) (ip >> 16 & 0xff),
                (byte) (ip >> 24 & 0xff)
        };
        return InetAddress.getByAddress(bytes);
    }
}
