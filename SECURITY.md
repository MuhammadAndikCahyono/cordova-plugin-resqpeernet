# 🔒 Security Features – ResqPeerNet (PRO++)

This document describes the **security features** implemented in the `cordova-plugin-resqpeernet`.  
It ensures privacy, integrity, and safety of peer-to-peer communication.

---

## ✅ Implemented Security Features

| No | Feature | Description | Benefit | Status |
|----|---------|-------------|---------|---------|
| 1 | **End-to-End Encryption (E2EE)** | Payload pesan & file dienkripsi dengan **AES-256**, pertukaran kunci pakai **RSA/ECDH** | Data aman, hanya pengirim & penerima bisa baca | ✅ Implemented |
| 2 | **Secure Handshake** | Peer melakukan pertukaran public key + tanda tangan digital sebelum sesi dimulai | Mencegah **MITM** & identitas palsu | ✅ Implemented |
| 3 | **Ephemeral Session Keys** | Kunci sesi dibuat ulang setiap koneksi, tidak dipakai ulang | Mencegah **replay attack**, meningkatkan forward secrecy | ✅ Implemented |
| 4 | **Data Integrity Check** | Semua pesan/file memiliki **SHA-256 checksum** | Pastikan data tidak dimodifikasi saat transit | ✅ Implemented |
| 5 | **Sandbox Storage** | Database lokal (IndexedDB/SQLite) diberi prefix khusus plugin | Isolasi data, cegah aplikasi lain mengakses langsung | ✅ Implemented |
| 6 | **Permission Control** | Akses SMS, Telepon, Kamera, Bluetooth hanya aktif jika user setuju (runtime permission) | Minimalkan risiko privasi | ✅ Implemented |
| 7 | **Offline-First Architecture** | Semua komunikasi P2P tanpa server pusat | Lebih tahan sensor & serangan DDoS | ✅ Implemented |
| 8 | **Auto-Clean Logs (Emergency Mode)** | Log tidak simpan data sensitif, cache bisa auto-clear saat mode darurat | Mengurangi jejak forensik | ✅ Implemented |

---

## 🔄 Planned / Roadmap Security Features

| No | Feature | Description | Benefit | Status |
|----|---------|-------------|---------|---------|
| 9 | **Identity Verification** | Verifikasi via QR-code scan / mnemonic phrase | Mencegah impersonasi | 🔄 Roadmap |
| 10 | **Forward Secrecy** | Implementasi **Double Ratchet (Signal protocol)** | Keamanan tingkat lanjut | 🔄 Roadmap |
| 11 | **Access Policy for Groups** | Role-based access untuk chat grup / jaringan | Kontrol lebih ketat | 🔄 Roadmap |
| 12 | **Encrypted Audit Trail** | Log terenkripsi hanya bisa dibuka user sendiri | Audit aman | 🔄 Roadmap |
| 13 | **Multi-layer Encryption (Fallback)** | Jika lewat WebRTC/TURN, ditambah lapisan enkripsi plugin | Tetap private walau pakai relay server | 🔄 Roadmap |

---

## ⚠️ Security Notes

- Always run the latest version of the plugin to ensure patched vulnerabilities are applied.  
- Use **Emergency Mode** in disaster scenarios to minimize data traceability.  
- Encourage users to **verify peer identities** when exchanging sensitive data.  
- For production usage, integrate with **trusted TURN/STUN servers** if NAT traversal is needed.  

---
