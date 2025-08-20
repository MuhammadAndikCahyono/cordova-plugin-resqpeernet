### User / Profile
```json
{
  "uid": "uuid-v4",         
  "username": "resq_user",  
  "fullname": "John Doe",
  "avatar": "base64/png or file://path",
  "status": "online | offline | busy",
  "lastSeen": 1713457890,   // epoch
  "publicKey": "..."        // E2E
}
```
### Message (Chat / Group)
```json
{
  "mid": "uuid-v4",         
  "from": "uid",            
  "to": "uid | groupId",    
  "type": "text | image | video | audio | location | file",
  "content": "Hello world!",  
  "mediaUrl": "file://path or magnet://hash",
  "timestamp": 1713457900,   // epoch
  "status": "sent | delivered | seen"
}

```
### Post (Sosial / Feed)
```json
{
  "pid": "uuid-v4",
  "author": "uid",
  "type": "text | image | video | audio",
  "caption": "Check this out!",
  "mediaUrl": "file://path or magnet://hash",
  "timestamp": 1713458000,
  "likes": [
    {"uid": "user1", "time": 1713458100}
  ],
  "comments": [
    {
      "cid": "uuid-v4",
      "author": "uid",
      "text": "Nice post!",
      "mediaUrl": null,
      "timestamp": 1713458200
    }
  ]
}
```
### Product (Marketplace)
```json
{
  "prodId": "uuid-v4",
  "seller": "uid",
  "name": "Coffee Beans 250g",
  "description": "Fresh roasted coffee",
  "price": 50000,
  "currency": "IDR",
  "stock": 20,
  "mediaUrl": ["file://img1.png", "file://img2.png"],
  "timestamp": 1713458300
}
```
### Emergency Alert
```json
{
  "eid": "uuid-v4",
  "from": "uid",
  "type": "sos | info | supply | location",
  "message": "Need water supply!",
  "location": {"lat": -6.2, "lng": 106.8},
  "timestamp": 1713458400,
  "priority": "high | medium | low"
}

```
### Navigator / Map Data
```json
{
  "nid": "uuid-v4",
  "author": "uid",
  "type": "poi | route | track",
  "name": "Safe Point",
  "description": "Emergency shelter",
  "location": {"lat": -6.2, "lng": 106.8},
  "mediaUrl": null,
  "timestamp": 1713458500
}

```
