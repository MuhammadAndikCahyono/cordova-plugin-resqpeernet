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
