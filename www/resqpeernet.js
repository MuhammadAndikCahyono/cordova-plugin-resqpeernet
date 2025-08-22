var exec = require('cordova/exec');

var ResqPeerNet = {
    /**
     * Daftar listener event
     */
    _listeners: {},

    /**
     * Register listener events dari native
     */
    listenEvents: function () {
        exec(function (event) {
            if (event && event.type && ResqPeerNet._listeners[event.type]) {
                ResqPeerNet._listeners[event.type].forEach(cb => cb(event.data));
            }
        }, function (err) {
            console.error("ResqPeerNet.listenEvents error:", err);
        }, 'ResqPeerNet', 'listenEvents', []);
    },

    /**
     * Tambahkan listener
     */
    on: function (eventName, callback) {
        if (!this._listeners[eventName]) {
            this._listeners[eventName] = [];
        }
        this._listeners[eventName].push(callback);
    },

    /**
     * Hapus listener
     */
    off: function (eventName, callback) {
        if (this._listeners[eventName]) {
            this._listeners[eventName] = this._listeners[eventName].filter(cb => cb !== callback);
        }
    },

    /**
     * General exec wrapper
     */
    _exec: function (action, args, success, error) {
        exec(success || function () {}, error || function (err) {
            console.error("ResqPeerNet error on " + action, err);
        }, 'ResqPeerNet', action, args || []);
    },

    // -------------------
    // Method API General
    // -------------------

    connect: function (options, success, error) {
        this._exec("connect", [options], success, error);
    },

    disconnect: function (success, error) {
        this._exec("disconnect", [], success, error);
    },

    sendMessage: function (msg, success, error) {
        this._exec("sendMessage", [msg], success, error);
    },

    discoverPeers: function (success, error) {
        this._exec("discoverPeers", [], success, error);
    },

    getPeers: function (success, error) {
        this._exec("getPeers", [], success, error);
    },

    // -------------------
    // mDNS
    // -------------------

    startMDNSServer: function (name, port, success, error) {
        this._exec("startMDNSServer", [name, port], success, error);
    },

    stopMDNSServer: function (success, error) {
        this._exec("stopMDNSServer", [], success, error);
    },

    startMDNSDiscovery: function (success, error) {
        this._exec("startMDNSDiscovery", [], success, error);
    },

    startMDNSListener: function (success, error) {
        this._exec("startMDNSListener", [], success, error);
    },

    sendMDNS: function (msg, success, error) {
        this._exec("sendMDNS", [msg], success, error);
    },

    // -------------------
    // BLE
    // -------------------

    startBLEScan: function (success, error) {
        this._exec("startBLEScan", [], success, error);
    },

    stopBLEScan: function (success, error) {
        this._exec("stopBLEScan", [], success, error);
    },

    // -------------------
    // Mesh Hybrid
    // -------------------

    startMeshHybrid: function (success, error) {
        this._exec("startMeshHybrid", [], success, error);
    },

    stopMeshHybrid: function (success, error) {
        this._exec("stopMeshHybrid", [], success, error);
    },

    sendMeshHybrid: function (msg, success, error) {
        this._exec("sendMeshHybrid", [msg], success, error);
    },

    // -------------------
    // WebRTC
    // -------------------

    initWebRTC: function (iceServers, success, error) {
        this._exec("initWebRTC", [iceServers], success, error);
    },

    createOffer: function (success, error) {
        this._exec("createOffer", [], success, error);
    },

    createAnswer: function (success, error) {
        this._exec("createAnswer", [], success, error);
    },

    setRemoteDescription: function (sdp, success, error) {
        this._exec("setRemoteDescription", [sdp], success, error);
    },

    addIceCandidate: function (candidate, success, error) {
        this._exec("addIceCandidate", [candidate], success, error);
    },

    createDataChannel: function (label, success, error) {
        this._exec("createDataChannel", [label], success, error);
    },

    sendWebRTC: function (msg, success, error) {
        this._exec("sendWebRTC", [msg], success, error);
    },

    sendFile: function (filename, base64Data, success, error) {
        this._exec("sendFile", [filename, base64Data], success, error);
    },

    // -------------------
    // WebTorrent Bridge
    // -------------------

    webtorrentSignal: function (jsonString, success, error) {
        this._exec("webtorrentSignal", [jsonString], success, error);
    },

    webtorrentSendPiece: function (peerId, pieceIndex, base64Data, success, error) {
        this._exec("webtorrentSendPiece", [peerId, pieceIndex, base64Data], success, error);
    },

    webtorrentRequestPiece: function (peerId, pieceIndex, success, error) {
        this._exec("webtorrentRequestPiece", [peerId, pieceIndex], success, error);
    }
};

// Daftarkan ke global window
module.exports = ResqPeerNet;
window.ResqPeerNet = ResqPeerNet;
