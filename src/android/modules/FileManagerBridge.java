package org.apache.cordova.resqpeernet.modules;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileManagerBridge {
    private static final String TAG = "FileManagerBridge";
    
    private Context context;
    private CallbackSender callbackSender;
    
    // Error codes consistent dengan cordova-plugin-file
    public static class FileError {
        public static final int NOT_FOUND_ERR = 1;
        public static final int SECURITY_ERR = 2;
        public static final int ABORT_ERR = 3;
        public static final int NOT_READABLE_ERR = 4;
        public static final int ENCODING_ERR = 5;
        public static final int NO_MODIFICATION_ALLOWED_ERR = 6;
        public static final int INVALID_STATE_ERR = 7;
        public static final int SYNTAX_ERR = 8;
        public static final int INVALID_MODIFICATION_ERR = 9;
        public static final int QUOTA_EXCEEDED_ERR = 10;
        public static final int TYPE_MISMATCH_ERR = 11;
        public static final int PATH_EXISTS_ERR = 12;
    }
    
    /**
     * CALLBACK INTERFACE untuk event system
     * Digunakan untuk mengirim event ke JavaScript layer
     */
    public interface CallbackSender {
        void sendEvent(String eventName, JSONObject data);
    }
    
    /**
     * CONSTRUCTOR FileManagerBridge
     * 
     * @param context Android Context untuk akses file system
     * @param callbackSender Interface untuk mengirim event ke JavaScript
     */
    public FileManagerBridge(Context context, CallbackSender callbackSender) {
        this.context = context;
        this.callbackSender = callbackSender;
        Log.i(TAG, "FileManagerBridge initialized successfully");
    }

    public void getStorageInfo(CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Getting comprehensive storage information");
            
            JSONObject storageInfo = new JSONObject();
            
            // Internal storage info
            JSONObject internalStorage = getStorageVolumeInfo(Environment.getDataDirectory());
            storageInfo.put("internal", internalStorage);
            
            // External storage info (jika available)
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                JSONObject externalStorage = getStorageVolumeInfo(Environment.getExternalStorageDirectory());
                storageInfo.put("external", externalStorage);
            }
            
            // Cache directories info
            JSONObject cacheInfo = new JSONObject();
            cacheInfo.put("internalCache", getDirectorySize(context.getCacheDir()));
            cacheInfo.put("externalCache", getDirectorySize(context.getExternalCacheDir()));
            storageInfo.put("cache", cacheInfo);
            
            // Timestamp
            storageInfo.put("timestamp", System.currentTimeMillis());
            storageInfo.put("status", "success");
            
            callbackContext.success(storageInfo);
            Log.d(TAG, "Storage info retrieved successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting storage info", e);
            callbackContext.error(createErrorResponse(FileError.NOT_READABLE_ERR, 
                "Cannot read storage information: " + e.getMessage()));
        }
    }
    
    /**
     * GET FREE DISK SPACE - Available space information
     * 
     * Mengembalikan informasi space available di semua storage volumes
     * Berguna untuk checking storage sebelum operasi file besar.
     * 
     * @param callbackContext Callback untuk mengembalikan hasil  
     */
    public void getFreeDiskSpace(CallbackContext callbackContext) {
        try {
            Log.d(TAG, "Getting free disk space information");
            
            JSONObject spaceInfo = new JSONObject();
            
            // Internal storage space
            File internalPath = Environment.getDataDirectory();
            StatFs internalStat = new StatFs(internalPath.getPath());
            long internalBlockSize = internalStat.getBlockSizeLong();
            long internalAvailable = internalStat.getAvailableBlocksLong() * internalBlockSize;
            long internalTotal = internalStat.getBlockCountLong() * internalBlockSize;
            
            JSONObject internalSpace = new JSONObject();
            internalSpace.put("free", internalAvailable);
            internalSpace.put("total", internalTotal);
            internalSpace.put("used", internalTotal - internalAvailable);
            spaceInfo.put("internal", internalSpace);
            
            // External storage space (jika available)
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File externalPath = Environment.getExternalStorageDirectory();
                StatFs externalStat = new StatFs(externalPath.getPath());
                long externalBlockSize = externalStat.getBlockSizeLong();
                long externalAvailable = externalStat.getAvailableBlocksLong() * externalBlockSize;
                long externalTotal = externalStat.getBlockCountLong() * externalBlockSize;
                
                JSONObject externalSpace = new JSONObject();
                externalSpace.put("free", externalAvailable);
                externalSpace.put("total", externalTotal);
                externalSpace.put("used", externalTotal - externalAvailable);
                spaceInfo.put("external", externalSpace);
            }
            
            spaceInfo.put("timestamp", System.currentTimeMillis());
            callbackContext.success(spaceInfo);
            Log.d(TAG, "Free disk space info retrieved successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting free disk space", e);
            callbackContext.error(createErrorResponse(FileError.NOT_READABLE_ERR,
                "Cannot read disk space: " + e.getMessage()));
        }
    }
    
    /**
     * HELPER METHOD: Get storage volume information
     * 
     * @param path Path ke storage volume
     * @return JSONObject dengan info storage volume
     */
    private JSONObject getStorageVolumeInfo(File path) throws JSONException {
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        
        JSONObject volumeInfo = new JSONObject();
        volumeInfo.put("path", path.getAbsolutePath());
        volumeInfo.put("totalSpace", totalBlocks * blockSize);
        volumeInfo.put("freeSpace", availableBlocks * blockSize);
        volumeInfo.put("usedSpace", (totalBlocks - availableBlocks) * blockSize);
        volumeInfo.put("isReadOnly", !path.canWrite());
        
        return volumeInfo;
    }

    /**
     * READ FILE AS TEXT - Membaca file sebagai string text
     * 
     * Compatible dengan cordova-plugin-file readAsText()
     * Support berbagai encoding: UTF-8, ASCII, ISO-8859-1
     * 
     * @param filePath Path lengkap ke file
     * @param encoding Encoding yang digunakan (default: UTF-8)
     * @param callbackContext Callback untuk mengembalikan hasil
     */
    public void readFileAsText(String filePath, String encoding, CallbackContext callbackContext) {
        Log.d(TAG, "Reading file as text: " + filePath);
        
        try {
            File file = new File(filePath);
            
            // Validation checks
            if (!file.exists()) {
                callbackContext.error(createErrorResponse(FileError.NOT_FOUND_ERR,
                    "File not found: " + filePath));
                return;
            }
            
            if (!file.canRead()) {
                callbackContext.error(createErrorResponse(FileError.NOT_READABLE_ERR,
                    "File not readable: " + filePath));
                return;
            }
            
            // Determine encoding
            String charset = (encoding != null && !encoding.isEmpty()) ? encoding : "UTF-8";
            
            // Read file content
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset));
            
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            // Prepare response
            JSONObject result = new JSONObject();
            result.put("content", content.toString());
            result.put("encoding", charset);
            result.put("fileSize", file.length());
            result.put("filePath", filePath);
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.d(TAG, "File read successfully: " + filePath);
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading file: " + filePath, e);
            callbackContext.error(createErrorResponse(FileError.NOT_READABLE_ERR,
                "Error reading file: " + e.getMessage()));
        }
    }
    
    /**
     * WRITE FILE - Menulis content ke file
     * 
     * Support create new file atau overwrite existing file
     * Compatible dengan cordova-plugin-file write()
     * 
     * @param filePath Path lengkap ke file
     * @param content Content yang akan ditulis
     * @param append Mode append (true) atau overwrite (false)
     * @param encoding Encoding yang digunakan
     * @param callbackContext Callback untuk mengembalikan hasil
     */
    public void writeFile(String filePath, String content, boolean append, 
                         String encoding, CallbackContext callbackContext) {
        Log.d(TAG, "Writing file: " + filePath + ", append: " + append);
        
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            
            // Create parent directories jika belum ada
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    callbackContext.error(createErrorResponse(FileError.NOT_FOUND_ERR,
                        "Cannot create parent directories: " + parentDir.getAbsolutePath()));
                    return;
                }
            }
            
            // Determine encoding
            String charset = (encoding != null && !encoding.isEmpty()) ? encoding : "UTF-8";
            
            // Write file content
            FileOutputStream fos = new FileOutputStream(file, append);
            OutputStreamWriter writer = new OutputStreamWriter(fos, charset);
            writer.write(content);
            writer.flush();
            writer.close();
            fos.close();
            
            // Prepare success response
            JSONObject result = new JSONObject();
            result.put("filePath", filePath);
            result.put("fileSize", file.length());
            result.put("bytesWritten", content.getBytes(charset).length);
            result.put("appendMode", append);
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.d(TAG, "File written successfully: " + filePath);
            
            // Send file created/modified event
            sendFileEvent(append ? "file_appended" : "file_created", filePath);
            
        } catch (Exception e) {
            Log.e(TAG, "Error writing file: " + filePath, e);
            callbackContext.error(createErrorResponse(FileError.NO_MODIFICATION_ALLOWED_ERR,
                "Error writing file: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE FILE - Menghapus file dari storage
     * 
     * Compatible dengan cordova-plugin-file remove()
     * Validation: exists check, permission check
     * 
     * @param filePath Path lengkap ke file yang akan dihapus
     * @param callbackContext Callback untuk mengembalikan hasil
     */
    public void deleteFile(String filePath, CallbackContext callbackContext) {
        Log.d(TAG, "Deleting file: " + filePath);
        
        try {
            File file = new File(filePath);
            
            // Validation checks
            if (!file.exists()) {
                callbackContext.error(createErrorResponse(FileError.NOT_FOUND_ERR,
                    "File not found: " + filePath));
                return;
            }
            
            if (!file.isFile()) {
                callbackContext.error(createErrorResponse(FileError.TYPE_MISMATCH_ERR,
                    "Path is not a file: " + filePath));
                return;
            }
            
            if (!file.canWrite()) {
                callbackContext.error(createErrorResponse(FileError.NO_MODIFICATION_ALLOWED_ERR,
                    "File not writable: " + filePath));
                return;
            }
            
            // Delete file
            boolean success = file.delete();
            
            if (success) {
                JSONObject result = new JSONObject();
                result.put("filePath", filePath);
                result.put("deleted", true);
                result.put("timestamp", System.currentTimeMillis());
                
                callbackContext.success(result);
                Log.d(TAG, "File deleted successfully: " + filePath);
                
                // Send file deleted event
                sendFileEvent("file_deleted", filePath);
                
            } else {
                callbackContext.error(createErrorResponse(FileError.NO_MODIFICATION_ALLOWED_ERR,
                    "Cannot delete file: " + filePath));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + filePath, e);
            callbackContext.error(createErrorResponse(FileError.NO_MODIFICATION_ALLOWED_ERR,
                "Error deleting file: " + e.getMessage()));
        }
    }

    /**
     * =========================================================================
     * DIRECTORY OPERATIONS
     * Method untuk management directory: create, list, delete, navigate
     * =========================================================================
     */
    
    /**
     * CREATE DIRECTORY - Membuat directory baru
     * 
     * Support nested directory creation (mkdirs)
     * Compatible dengan cordova-plugin-file getDirectory()
     * 
     * @param dirPath Path lengkap ke directory yang akan dibuat
     * @param createParents Buat parent directories jika belum ada
     * @param callbackContext Callback untuk mengembalikan hasil
     */
    public void createDirectory(String dirPath, boolean createParents, CallbackContext callbackContext) {
        Log.d(TAG, "Creating directory: " + dirPath + ", createParents: " + createParents);
        
        try {
            File directory = new File(dirPath);
            boolean success;
            
            if (createParents) {
                success = directory.mkdirs(); // Create all parent directories
            } else {
                success = directory.mkdir(); // Create only this directory
            }
            
            if (success) {
                JSONObject result = new JSONObject();
                result.put("dirPath", dirPath);
                result.put("created", true);
                result.put("absolutePath", directory.getAbsolutePath());
                result.put("createParents", createParents);
                result.put("timestamp", System.currentTimeMillis());
                
                callbackContext.success(result);
                Log.d(TAG, "Directory created successfully: " + dirPath);
                
                // Send directory created event
                sendDirectoryEvent("directory_created", dirPath);
                
            } else {
                callbackContext.error(createErrorResponse(FileError.PATH_EXISTS_ERR,
                    "Cannot create directory (may already exist): " + dirPath));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating directory: " + dirPath, e);
            callbackContext.error(createErrorResponse(FileError.NO_MODIFICATION_ALLOWED_ERR,
                "Error creating directory: " + e.getMessage()));
        }
    }
    
    /**
     * LIST DIRECTORY - Mendapatkan daftar file dan subdirectory
     * 
     * Compatible dengan cordova-plugin-file readEntries()
     * Support filtering dan sorting
     * 
     * @param dirPath Path ke directory yang akan di-list
     * @param includeHidden Include hidden files (dimulai dengan .)
     * @param callbackContext Callback untuk mengembalikan hasil
     */
    public void listDirectory(String dirPath, boolean includeHidden, CallbackContext callbackContext) {
        Log.d(TAG, "Listing directory: " + dirPath + ", includeHidden: " + includeHidden);
        
        try {
            File directory = new File(dirPath);
            
            // Validation checks
            if (!directory.exists()) {
                callbackContext.error(createErrorResponse(FileError.NOT_FOUND_ERR,
                    "Directory not found: " + dirPath));
                return;
            }
            
            if (!directory.isDirectory()) {
                callbackContext.error(createErrorResponse(FileError.TYPE_MISMATCH_ERR,
                    "Path is not a directory: " + dirPath));
                return;
            }
            
            if (!directory.canRead()) {
                callbackContext.error(createErrorResponse(FileError.NOT_READABLE_ERR,
                    "Directory not readable: " + dirPath));
                return;
            }
            
            // Get directory contents
            File[] files = directory.listFiles();
            JSONArray contents = new JSONArray();
            
            if (files != null) {
                for (File file : files) {
                    // Skip hidden files jika tidak di-include
                    if (!includeHidden && file.getName().startsWith(".")) {
                        continue;
                    }
                    
                    JSONObject item = new JSONObject();
                    item.put("name", file.getName());
                    item.put("path", file.getAbsolutePath());
                    item.put("isDirectory", file.isDirectory());
                    item.put("isFile", file.isFile());
                    item.put("isHidden", file.getName().startsWith("."));
                    item.put("size", file.length());
                    item.put("lastModified", file.lastModified());
                    item.put("canRead", file.canRead());
                    item.put("canWrite", file.canWrite());
                    item.put("canExecute", file.canExecute());
                    
                    contents.put(item);
                }
            }
            
            // Prepare response
            JSONObject result = new JSONObject();
            result.put("dirPath", dirPath);
            result.put("contents", contents);
            result.put("totalItems", contents.length());
            result.put("includeHidden", includeHidden);
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.d(TAG, "Directory listed successfully: " + dirPath + ", items: " + contents.length());
            
        } catch (Exception e) {
            Log.e(TAG, "Error listing directory: " + dirPath, e);
            callbackContext.error(createErrorResponse(FileError.NOT_READABLE_ERR,
                "Error listing directory: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE DIRECTORY RECURSIVELY - Menghapus directory dan semua isinya
     * 
     * Compatible dengan cordova-plugin-file removeRecursively()
     * Dangerous operation - perlu confirmation di UI layer
     * 
     * @param dirPath Path ke directory yang akan dihapus
     * @param callbackContext Callback untuk mengembalikan hasil
     */
    public void deleteDirectoryRecursively(String dirPath, CallbackContext callbackContext) {
        Log.d(TAG, "Deleting directory recursively: " + dirPath);
        
        try {
            File directory = new File(dirPath);
            
            // Validation checks
            if (!directory.exists()) {
                callbackContext.error(createErrorResponse(FileError.NOT_FOUND_ERR,
                    "Directory not found: " + dirPath));
                return;
            }
            
            if (!directory.isDirectory()) {
                callbackContext.error(createErrorResponse(FileError.TYPE_MISMATCH_ERR,
                    "Path is not a directory: " + dirPath));
                return;
            }
            
            // Delete recursively
            boolean success = deleteRecursive(directory);
            
            if (success) {
                JSONObject result = new JSONObject();
                result.put("dirPath", dirPath);
                result.put("deleted", true);
                result.put("recursive", true);
                result.put("timestamp", System.currentTimeMillis());
                
                callbackContext.success(result);
                Log.d(TAG, "Directory deleted recursively: " + dirPath);
                
                // Send directory deleted event
                sendDirectoryEvent("directory_deleted", dirPath);
                
            } else {
                callbackContext.error(createErrorResponse(FileError.NO_MODIFICATION_ALLOWED_ERR,
                    "Cannot delete directory (may contain locked files): " + dirPath));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting directory recursively: " + dirPath, e);
            callbackContext.error(createErrorResponse(FileError.NO_MODIFICATION_ALLOWED_ERR,
                "Error deleting directory: " + e.getMessage()));
        }
    }
    
    /**
     * HELPER: Recursive directory deletion
     */
    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursive(child)) {
                        return false;
                    }
                }
            }
        }
        return fileOrDirectory.delete();
    }

    /**
     * =========================================================================
     * FILE SEARCH & ADVANCED OPERATIONS
     * Method untuk pencarian file dan operasi lanjutan
     * =========================================================================
     */
    
    /**
     * SEARCH FILES - Mencari file berdasarkan pattern
     * 
     * Support wildcard patterns dan multiple criteria
     * Recursive search melalui subdirectories
     * 
     * @param searchDir Directory root untuk pencarian
     * @param searchPattern Pattern pencarian (bisa menggunakan *)
     * @param searchInSubdirs Search recursively dalam subdirectories
     * @param caseSensitive Case sensitive search
     * @param callbackContext Callback untuk mengembalikan hasil
     */
    public void searchFiles(String searchDir, String searchPattern, 
                           boolean searchInSubdirs, boolean caseSensitive, 
                           CallbackContext callbackContext) {
        Log.d(TAG, "Searching files: " + searchPattern + " in " + searchDir);
        
        try {
            File directory = new File(searchDir);
            
            if (!directory.exists() || !directory.isDirectory()) {
                callbackContext.error(createErrorResponse(FileError.NOT_FOUND_ERR,
                    "Search directory not found: " + searchDir));
                return;
            }
            
            // Convert pattern to regex
            String regexPattern = searchPattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
            
            if (!caseSensitive) {
                regexPattern = "(?i)" + regexPattern; // Case insensitive
            }
            
            // Perform search
            List<JSONObject> searchResults = new ArrayList<>();
            performSearch(directory, regexPattern, searchInSubdirs, searchResults);
            
            // Prepare response
            JSONObject result = new JSONObject();
            result.put("searchPattern", searchPattern);
            result.put("searchDir", searchDir);
            result.put("results", new JSONArray(searchResults));
            result.put("totalFound", searchResults.size());
            result.put("searchInSubdirs", searchInSubdirs);
            result.put("caseSensitive", caseSensitive);
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.d(TAG, "File search completed: " + searchResults.size() + " files found");
            
        } catch (Exception e) {
            Log.e(TAG, "Error searching files", e);
            callbackContext.error(createErrorResponse(FileError.NOT_READABLE_ERR,
                "Error searching files: " + e.getMessage()));
        }
    }
    
    /**
     * HELPER: Recursive file search
     */
    private void performSearch(File directory, String regexPattern, 
                             boolean searchInSubdirs, List<JSONObject> results) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            try {
                if (file.isDirectory()) {
                    if (searchInSubdirs) {
                        performSearch(file, regexPattern, searchInSubdirs, results);
                    }
                } else {
                    if (file.getName().matches(regexPattern)) {
                        JSONObject fileInfo = new JSONObject();
                        fileInfo.put("name", file.getName());
                        fileInfo.put("path", file.getAbsolutePath());
                        fileInfo.put("size", file.length());
                        fileInfo.put("lastModified", file.lastModified());
                        fileInfo.put("directory", file.getParent());
                        results.add(fileInfo);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error processing file in search: " + file.getAbsolutePath(), e);
            }
        }
    }
    
    /**
     * GET FILE INFORMATION - Mendapatkan metadata lengkap file
     * 
     * Compatible dengan cordova-plugin-file getMetadata()
     * Mengembalikan semua informasi yang available tentang file
     * 
     * @param filePath Path ke file
     * @param callbackContext Callback untuk mengembalikan hasil
     */
    public void getFileInfo(String filePath, CallbackContext callbackContext) {
        Log.d(TAG, "Getting file info: " + filePath);
        
        try {
            File file = new File(filePath);
            
            if (!file.exists()) {
                callbackContext.error(createErrorResponse(FileError.NOT_FOUND_ERR,
                    "File not found: " + filePath));
                return;
            }
            
            JSONObject fileInfo = new JSONObject();
            fileInfo.put("name", file.getName());
            fileInfo.put("path", file.getAbsolutePath());
            fileInfo.put("size", file.length());
            fileInfo.put("lastModified", file.lastModified());
            fileInfo.put("isDirectory", file.isDirectory());
            fileInfo.put("isFile", file.isFile());
            fileInfo.put("isHidden", file.isHidden());
            fileInfo.put("canRead", file.canRead());
            fileInfo.put("canWrite", file.canWrite());
            fileInfo.put("canExecute", file.canExecute());
            fileInfo.put("parent", file.getParent());
            
            // Additional metadata
            fileInfo.put("fileSeparator", File.separator);
            fileInfo.put("freeSpace", file.getFreeSpace());
            fileInfo.put("totalSpace", file.getTotalSpace());
            fileInfo.put("usableSpace", file.getUsableSpace());
            
            // Format dates
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            fileInfo.put("lastModifiedFormatted", sdf.format(new Date(file.lastModified())));
            
            callbackContext.success(fileInfo);
            Log.d(TAG, "File info retrieved: " + filePath);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting file info: " + filePath, e);
            callbackContext.error(createErrorResponse(FileError.NOT_READABLE_ERR,
                "Error getting file info: " + e.getMessage()));
        }
    }

    /**
     * =========================================================================
     * UTILITY & HELPER METHODS
     * Method bantuan untuk internal operations
     * =========================================================================
     */
    
    /**
     * CREATE ERROR RESPONSE - Membuat response error yang standardized
     * 
     * @param code Error code sesuai FileError constants
     * @param message Descriptive error message
     * @return JSONObject error response
     */
    private JSONObject createErrorResponse(int code, String message) {
        try {
            JSONObject error = new JSONObject();
            error.put("code", code);
            error.put("message", message);
            error.put("timestamp", System.currentTimeMillis());
            return error;
        } catch (JSONException e) {
            Log.e(TAG, "Error creating error response", e);
            return new JSONObject();
        }
    }
    
    /**
     * SEND FILE EVENT - Mengirim event ke JavaScript layer
     * 
     * @param eventType Jenis event: file_created, file_modified, file_deleted
     * @param filePath Path file yang terkait event
     */
    private void sendFileEvent(String eventType, String filePath) {
        try {
            if (callbackSender != null) {
                JSONObject eventData = new JSONObject();
                eventData.put("eventType", eventType);
                eventData.put("filePath", filePath);
                eventData.put("timestamp", System.currentTimeMillis());
                
                callbackSender.sendEvent("file_system_event", eventData);
                Log.d(TAG, "File event sent: " + eventType + " - " + filePath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending file event", e);
        }
    }
    
    /**
     * SEND DIRECTORY EVENT - Mengirim event directory ke JavaScript
     */
    private void sendDirectoryEvent(String eventType, String dirPath) {
        try {
            if (callbackSender != null) {
                JSONObject eventData = new JSONObject();
                eventData.put("eventType", eventType);
                eventData.put("dirPath", dirPath);
                eventData.put("timestamp", System.currentTimeMillis());
                
                callbackSender.sendEvent("file_system_event", eventData);
                Log.d(TAG, "Directory event sent: " + eventType + " - " + dirPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending directory event", e);
        }
    }
    
    /**
     * GET DIRECTORY SIZE - Menghitung total size directory recursively
     * 
     * @param directory Directory yang akan dihitung size-nya
     * @return Total size dalam bytes
     */
    private long getDirectorySize(File directory) {
        long size = 0;
        if (directory == null || !directory.exists()) return 0;
        
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else {
                        size += getDirectorySize(file);
                    }
                }
            }
        } else {
            size = directory.length();
        }
        return size;
    }
    
    /**
     * DESTROY - Cleanup resources
     */
    public void destroy() {
        Log.i(TAG, "FileManagerBridge destroyed");
        // Cleanup resources jika ada
    }


}