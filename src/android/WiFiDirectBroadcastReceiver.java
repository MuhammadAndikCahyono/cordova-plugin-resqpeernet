package org.apache.cordova.resqpeernet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/** Receiver khusus Wi-Fi Direct, meneruskan event ke plugin */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final ResqPeerNet plugin;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager,
                                       WifiP2pManager.Channel channel,
                                       ResqPeerNet plugin) {
        this.manager = manager;
        this.channel = channel;
        this.plugin = plugin;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            Log.d("ResqPeerNet", state == WifiP2pManager.WIFI_P2P_STATE_ENABLED ?
                    "Wi-Fi Direct enabled" : "Wi-Fi Direct disabled");

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (manager != null) {
                manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                    @Override public void onPeersAvailable(WifiP2pDeviceList list) {
                        plugin.onPeersAvailable(list);
                    }
                });
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (manager == null) return;
            NetworkInfo n = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (n != null && n.isConnected()) {
                manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
                    @Override public void onConnectionInfoAvailable(WifiP2pInfo info) {
                        plugin.onConnectionInfoAvailable(info);
                    }
                });
            } else {
                Log.d("ResqPeerNet", "Wi-Fi Direct disconnected");
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d("ResqPeerNet", "This device Wi-Fi Direct state changed");
        }
    }
}
