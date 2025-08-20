## Core Service

| API                     | Tipe              | Deskripsi                                                 | Contoh                                                                          |
| ----------------------- | ----------------- | --------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `startService(options)` | Method            | Menyalakan service background agar tetap hidup.           | `cordova.plugins.resqpeernet.startService({ keepAlive: true })`                       |
| `stopService()`         | Method            | Mematikan service background.                             | `cordova.plugins.resqpeernet.stopService()`                                           |
| `isRunning()`           | Method            | Mengecek apakah service sedang aktif.                     | `cordova.plugins.resqpeernet.isRunning(status => console.log(status))`                |
| `onServiceStarted(cb)`  | Event             | Listener saat service berhasil start.                     | `cordova.plugins.resqpeernet.onServiceStarted(() => console.log("Service aktif"))`    |
| `onServiceStopped(cb)`  | Event             | Listener saat service berhenti.                           | `cordova.plugins.resqpeernet.onServiceStopped(() => console.log("Service berhenti"))` |
| `onError(cb)`           | Event             | Listener ketika terjadi error di plugin.                  | `cordova.plugins.resqpeernet.onError(err => console.error(err))`                      |
| `startAuto(options)`    | Method (opsional) | Wrapper otomatis `startService()` + setup listener dasar. | `cordova.plugins.resqpeernet.startAuto({ keepAlive: true })`                          |


## Peer-to-Peer (P2P)

| API                        | Tipe   | Deskripsi                           | Contoh                                                              |
| -------------------------- | ------ | ----------------------------------- | ------------------------------------------------------------------- |
| `connect(peerId, options)` | Method | Membuka koneksi ke peer tertentu.   | `cordova.plugins.resqpeernet.connect("peer-123", { secure: true })`       |
| `disconnect(peerId)`       | Method | Memutus koneksi ke peer tertentu.   | `cordova.plugins.resqpeernet.disconnect("peer-123")`                      |
| `getPeers()`               | Method | Mendapatkan daftar peer yang aktif. | `cordova.plugins.resqpeernet.getPeers(peers => console.log(peers))`       |
| `onPeerDiscovered(cb)`     | Event  | Listener saat peer baru ditemukan.  | `cordova.plugins.resqpeernet.onPeerDiscovered(peer => console.log(peer))` |

## Messaging

| API                        | Tipe   | Deskripsi                           | Contoh                                                       |
| -------------------------- | ------ | ----------------------------------- | ------------------------------------------------------------ |
| `sendMessage(peerId, msg)` | Method | Mengirim pesan ke peer tertentu.    | `cordova.plugins.resqpeernet.sendMessage("peer-123", "Halo!")`     |
| `broadcastMessage(msg)`    | Method | Mengirim pesan ke semua peer aktif. | `cordova.plugins.resqpeernet.broadcastMessage("Halo semua peer!")` |
| `onMessage(cb)`            | Event  | Listener saat pesan masuk diterima. | `cordova.plugins.resqpeernet.onMessage(msg => console.log(msg))`   |

## File / Data Transfer

| API                         | Tipe   | Deskripsi                                        | Contoh                                                            |
| --------------------------- | ------ | ------------------------------------------------ | ----------------------------------------------------------------- |
| `sendFile(peerId, fileUri)` | Method | Mengirim file ke peer tertentu.                  | `cordova.plugins.resqpeernet.sendFile("peer-123", "/path/to/file")`     |
| `onFileReceived(cb)`        | Event  | Listener saat file diterima.                     | `cordova.plugins.resqpeernet.onFileReceived(file => console.log(file))` |
| `openFileChooser(cb)`       | Method | Membuka file chooser Android untuk memilih file. | `cordova.plugins.resqpeernet.openFileChooser(uri => console.log(uri))`  |

## Tracker & Discovery

| API               | Tipe   | Deskripsi                                      | Contoh                                                           |
| ----------------- | ------ | ---------------------------------------------- | ---------------------------------------------------------------- |
| `enableWSS(urls)` | Method | Aktifkan tracker TLS/WSS untuk peer discovery. | `cordova.plugins.resqpeernet.enableWSS(["wss://tracker.example.com"])` |
| `disableWSS()`    | Method | Nonaktifkan tracker TLS/WSS.                   | `cordova.plugins.resqpeernet.disableWSS()`                             |

## Logging & Monitoring

| API         | Tipe   | Deskripsi                       | Contoh                                                       |
| ----------- | ------ | ------------------------------- | ------------------------------------------------------------ |
| `getLogs()` | Method | Mengambil log aktivitas plugin. | `cordova.plugins.resqpeernet.getLogs(logs => console.table(logs))` |

