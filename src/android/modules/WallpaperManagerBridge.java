package org.apache.cordova.resqpeernet.modules;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri; 
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WallpaperManagerBridge - Fixed Compilation Errors
 */
public class WallpaperManagerBridge {

    private static final String TAG = "WallpaperManagerBridge";
    private static final int MAX_IMAGE_SIZE = 4096;
    private static final int JPEG_QUALITY = 100;

    private final Context context;
    private final CallbackSender callbackSender;
    private final ExecutorService executorService;
    private final Handler mainHandler; // FIXED: Add main handler for UI thread operations
    private WeakReference<CallbackContext> wallpaperCallbackRef;
    private BroadcastReceiver wallpaperReceiver;
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
	
	/**
	 * Constants untuk jenis wallpaper yang didukung
	 * Digunakan untuk menentukan di mana wallpaper akan diterapkan
	 */
	
	public static final int WALLPAPER_HOME = 1;      /* Untuk home screen saja */
	public static final int WALLPAPER_LOCK = 2;      /* Untuk lock screen saja (Android N+) */
	public static final int WALLPAPER_BOTH = 3;      /* Untuk kedua screen (Android N+) */
	
	/**
	 * Set wallpaper dari file image di storage device
	 * Method ini menerima path file langsung dari storage
	 * 
	 * @param filePath Path lengkap ke file image (contoh: "/storage/emulated/0/Pictures/wallpaper.jpg")
	 * @param wallpaperType Jenis wallpaper (WALLPAPER_HOME, WALLPAPER_LOCK, WALLPAPER_BOTH)
	 * @param cb CallbackContext untuk mengembalikan hasil
	 */

	public void setWallpaperFromFile(final String filePath, final int wallpaperType, final CallbackContext cb) {
		if (isDestroyed.get()) {
			sendError(cb, "WallpaperManagerBridge has been destroyed");
			return;
		}

		if (filePath == null || filePath.isEmpty()) {
			sendError(cb, "File path tidak boleh kosong");
			return;
		}

		executorService.execute(() -> {
			Bitmap bitmap = null;
			try {
				// Cek apakah file exists
				File imageFile = new File(filePath);
				if (!imageFile.exists() || !imageFile.isFile()) {
					sendError(cb, "File image tidak ditemukan: " + filePath);
					return;
				}

				// Cek ekstensi file
				String fileName = imageFile.getName().toLowerCase();
				if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && 
					!fileName.endsWith(".png") && !fileName.endsWith(".bmp")) {
					sendError(cb, "Format file tidak didukung. Gunakan JPG, PNG, atau BMP");
					return;
				}

				// Decode file image
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(filePath, options);
				
				// Validasi ukuran image
				if (options.outWidth > 4096 || options.outHeight > 4096) {
					sendError(cb, "Ukuran image terlalu besar. Maksimal 4096x4096 pixels");
					return;
				}

				// Load full bitmap
				options.inJustDecodeBounds = false;
				options.inSampleSize = calculateInSampleSize(options, 2048, 2048);
				bitmap = BitmapFactory.decodeFile(filePath, options);
				
				if (bitmap == null) {
					sendError(cb, "Gagal decode file image");
					return;
				}

				// Set wallpaper
				WallpaperManager wm = WallpaperManager.getInstance(context);
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					int androidWallpaperType = convertToAndroidWallpaperFlag(wallpaperType);
					wm.setBitmap(bitmap, null, true, androidWallpaperType);
				} else {
					// Perbaiki: gunakan constant yang sudah dipindahkan
					if (wallpaperType == WALLPAPER_LOCK) {
						sendError(cb, "Lock screen wallpaper hanya didukung di Android 7.0+");
						return;
					}
					wm.setBitmap(bitmap);
				}

				bitmap.recycle();

				// Kirim hasil sukses
				JSONObject result = new JSONObject();
				result.put("success", true);
				result.put("message", "Wallpaper berhasil diatur");
				result.put("filePath", filePath);
				result.put("wallpaperType", wallpaperType);
				result.put("imageWidth", options.outWidth);
				result.put("imageHeight", options.outHeight);
				
				sendSuccessOnMainThread(cb, result);

			} catch (Exception e) {
				Log.e(TAG, "setWallpaperFromFile failed", e);
				sendError(cb, "Gagal mengatur wallpaper: " + e.getMessage());
				
				if (bitmap != null && !bitmap.isRecycled()) {
					bitmap.recycle();
				}
			}
		});
	}
	
	/**
	 * Set wallpaper dari URI (support gallery picks, content providers, dll)
	 * Method ini berguna untuk image yang dipilih dari gallery atau file picker
	 * 
	 * @param uriString URI ke image (contoh: "content://media/external/images/media/123")
	 * @param wallpaperType Jenis wallpaper (WALLPAPER_HOME, WALLPAPER_LOCK, WALLPAPER_BOTH)
	 * @param cb CallbackContext untuk mengembalikan hasil
	 */

	public void setWallpaperFromUri(final String uriString, final int wallpaperType, final CallbackContext cb) {
		if (isDestroyed.get()) {
			sendError(cb, "WallpaperManagerBridge has been destroyed");
			return;
		}

		if (uriString == null || uriString.isEmpty()) {
			sendError(cb, "URI tidak boleh kosong");
			return;
		}

		executorService.execute(() -> {
			InputStream inputStream = null;
			Bitmap bitmap = null;
			
			try {
				// Parse URI
				Uri imageUri = Uri.parse(uriString);
				ContentResolver resolver = context.getContentResolver();
				
				// Buka input stream
				inputStream = resolver.openInputStream(imageUri);
				if (inputStream == null) {
					sendError(cb, "Tidak dapat membuka stream dari URI: " + uriString);
					return;
				}

				// Decode image dari stream
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				
				// Copy stream untuk membaca bounds
				byte[] buffer = new byte[8192];
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					baos.write(buffer, 0, bytesRead);
				}
				
				byte[] imageData = baos.toByteArray();
				BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
				
				// Validasi ukuran image
				if (options.outWidth > 4096 || options.outHeight > 4096) {
					sendError(cb, "Ukuran image terlalu besar. Maksimal 4096x4096 pixels");
					return;
				}

				// Decode full bitmap
				options.inJustDecodeBounds = false;
				options.inSampleSize = calculateInSampleSize(options, 2048, 2048);
				bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
				
				if (bitmap == null) {
					sendError(cb, "Gagal decode image dari URI");
					return;
				}

				// Set wallpaper
				WallpaperManager wm = WallpaperManager.getInstance(context);
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					int androidWallpaperType = convertToAndroidWallpaperFlag(wallpaperType);
					wm.setBitmap(bitmap, null, true, androidWallpaperType);
				} else {
					// Perbaiki: gunakan constant yang sudah dipindahkan
					if (wallpaperType == WALLPAPER_LOCK) {
						sendError(cb, "Lock screen wallpaper hanya didukung di Android 7.0+");
						return;
					}
					wm.setBitmap(bitmap);
				}

				// Cleanup
				bitmap.recycle();
				baos.close();

				// Kirim hasil
				JSONObject result = new JSONObject();
				result.put("success", true);
				result.put("message", "Wallpaper berhasil diatur dari URI");
				result.put("uri", uriString);
				result.put("wallpaperType", wallpaperType);
				result.put("imageWidth", options.outWidth);
				result.put("imageHeight", options.outHeight);
				
				sendSuccessOnMainThread(cb, result);

			} catch (Exception e) {
				Log.e(TAG, "setWallpaperFromUri failed", e);
				sendError(cb, "Gagal mengatur wallpaper dari URI: " + e.getMessage());
				
				if (bitmap != null && !bitmap.isRecycled()) {
					bitmap.recycle();
				}
			} finally {
				if (inputStream != null) {
					try { inputStream.close(); } catch (Exception e) { 
						Log.w(TAG, "Error closing input stream", e);
					}
				}
			}
		});
	}
	
	/**
	 * Helper method untuk convert custom wallpaper type ke Android system flag
	 * 
	 * @param wallpaperType WALLPAPER_HOME, WALLPAPER_LOCK, atau WALLPAPER_BOTH
	 * @return Android system wallpaper flag
	 */

	private int convertToAndroidWallpaperFlag(int wallpaperType) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			// Perbaiki: gunakan constants yang sudah dipindahkan
			switch (wallpaperType) {
				case WALLPAPER_HOME: // <- Sekarang constant ini accessible
					return WallpaperManager.FLAG_SYSTEM;
				case WALLPAPER_LOCK: // <- Sekarang constant ini accessible
					return WallpaperManager.FLAG_LOCK;
				case WALLPAPER_BOTH: // <- Sekarang constant ini accessible
					return WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
				default:
					return WallpaperManager.FLAG_SYSTEM;
			}
		}
		return WallpaperManager.FLAG_SYSTEM;
	}

	/**
	 * Helper method untuk calculate sampling size untuk avoid OOM
	 * Method ini menghitung seberapa banyak image akan di-downsample
	 * 
	 * @param options BitmapFactory.Options yang berisi dimensi original
	 * @param reqWidth Lebar yang diinginkan
	 * @param reqHeight Tinggi yang diinginkan
	 * @return Sample size yang optimal
	 */

	private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		final int width = options.outWidth;
		final int height = options.outHeight;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			while ((halfHeight / inSampleSize) >= reqHeight
					&& (halfWidth / inSampleSize) >= reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	/**
	 * Method untuk mendapatkan informasi tentang supported wallpaper types
	 * Berguna untuk UI yang menampilkan opsi yang available
	 * 
	 * @param cb CallbackContext untuk mengembalikan hasil
	 */
	public void getSupportedWallpaperTypes(final CallbackContext cb) {
		try {
			JSONObject types = new JSONObject();
			types.put("homeScreen", true);
			
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				types.put("lockScreen", true);
				types.put("bothScreens", true);
			} else {
				types.put("lockScreen", false);
				types.put("bothScreens", false);
			}
			
			types.put("minSdkVersion", Build.VERSION_CODES.N);
			types.put("currentSdkVersion", Build.VERSION.SDK_INT);
			
			sendSuccessOnMainThread(cb, types);
		} catch (JSONException e) {
			sendError(cb, "Gagal mendapatkan supported wallpaper types: " + e.getMessage());
		}
	}

	/*============================================ End setWallpaperFromFile, setWallpaperFromUri ===================================================*/

    public interface CallbackSender {
        void sendEvent(String eventName, JSONObject data);
    }

    public WallpaperManagerBridge(Context ctx, CallbackSender sender) {
        this.context = ctx.getApplicationContext();
        this.callbackSender = sender;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper()); // FIXED: Initialize main handler
    }

    /**
     * Ambil wallpaper saat ini - FIXED: Remove cordova references
     */
    public void getWallpaper(final CallbackContext cb) {
        if (isDestroyed.get()) {
            sendError(cb, "WallpaperManagerBridge has been destroyed");
            return;
        }

        executorService.execute(() -> {
            Bitmap bitmap = null;
            Bitmap optimizedBitmap = null;
            ByteArrayOutputStream baos = null;
            
            try {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                Drawable drawable = wm.getDrawable();
                
                if (drawable == null) {
                    sendError(cb, "No wallpaper found");
                    return;
                }

                bitmap = drawableToBitmap(drawable);
                if (bitmap == null) {
                    sendError(cb, "Failed to convert wallpaper to bitmap");
                    return;
                }

                optimizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);
                if (optimizedBitmap == null) {
                    sendError(cb, "Failed to resize wallpaper");
                    return;
                }
                
                baos = new ByteArrayOutputStream();
                boolean compressSuccess = optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
                
                if (!compressSuccess) {
                    sendError(cb, "Failed to compress wallpaper");
                    return;
                }

                String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                
                JSONObject result = new JSONObject();
                result.put("base64", base64);
                result.put("format", "jpeg");
                result.put("size", baos.size());
                result.put("width", optimizedBitmap.getWidth());
                result.put("height", optimizedBitmap.getHeight());
                
                // FIXED: Use mainHandler instead of cordova
                sendSuccessOnMainThread(cb, result);

            } catch (Exception e) {
                Log.e(TAG, "getWallpaper failed", e);
                sendError(cb, "Failed to get wallpaper: " + e.getMessage());
            } finally {
                // Cleanup resources
                if (optimizedBitmap != null && optimizedBitmap != bitmap) {
                    optimizedBitmap.recycle();
                }
                if (baos != null) {
                    try { baos.close(); } catch (Exception e) { /* ignore */ }
                }
            }
        });
    }

    /**
     * Ambil wallpaper info tanpa gambar - FIXED
     */
    public void getWallpaperInfo(final CallbackContext cb) {
        if (isDestroyed.get()) {
            sendError(cb, "WallpaperManagerBridge has been destroyed");
            return;
        }

        executorService.execute(() -> {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                JSONObject info = new JSONObject();
                
                info.put("desiredMinWidth", wm.getDesiredMinimumWidth());
                info.put("desiredMinHeight", wm.getDesiredMinimumHeight());
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    info.put("isWallpaperSupported", wm.isWallpaperSupported());
                    info.put("isSetWallpaperAllowed", wm.isSetWallpaperAllowed());
                }
                
                // FIXED: Use mainHandler instead of cordova
                sendSuccessOnMainThread(cb, info);
                
            } catch (Exception e) {
                Log.e(TAG, "getWallpaperInfo failed", e);
                sendError(cb, "Failed to get wallpaper info: " + e.getMessage());
            }
        });
    }

    /**
     * Helper method untuk send success on main thread - NEW METHOD
     */
    private void sendSuccessOnMainThread(final CallbackContext cb, final JSONObject result) {
        mainHandler.post(() -> {
            if (!isDestroyed.get() && cb != null) {
                cb.success(result);
            }
        });
    }

    private void sendSuccessOnMainThread(final CallbackContext cb, final String message) {
        mainHandler.post(() -> {
            if (!isDestroyed.get() && cb != null) {
                cb.success(message);
            }
        });
    }

/**
 * Mulai mendengarkan event perubahan wallpaper - DIPERBAIKI untuk multiple events
 */
public void listenWallpaperChanged(final CallbackContext cb) {
    if (isDestroyed.get()) {
        sendError(cb, "WallpaperManagerBridge has been destroyed");
        return;
    }

    if (!isListening.compareAndSet(false, true)) {
        // Jika sudah listening, tetap setup callback baru untuk replace yang lama
        Log.d(TAG, "Already listening, replacing callback");
    }

    // Selalu update callback reference
    this.wallpaperCallbackRef = new WeakReference<>(cb);
    
    try {
        /* Hapus receiver lama jika ada */
        if (wallpaperReceiver != null) {
            try {
                context.unregisterReceiver(wallpaperReceiver);
            } catch (Exception e) {
                /* Ignore jika belum terdaftar */
            }
        }
        
        /* Buat receiver baru */
        wallpaperReceiver = new WallpaperChangeReceiver();
        
        /* Intent filter yang comprehensive */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
        filter.addAction(Intent.ACTION_SET_WALLPAPER);
        filter.addAction("android.intent.action.WALLPAPER_CHANGED");
        filter.addAction("com.android.launcher.action.WALLPAPER_CHANGED");
        
        /* Register receiver */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(wallpaperReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(wallpaperReceiver, filter);
        }

        /* PENTING: Kirim plugin result dengan KEEP_CALLBACK */
        PluginResult result = new PluginResult(PluginResult.Status.OK, "🔥 Listening for wallpaper changes,...");
        result.setKeepCallback(true); // ⭐️ INI YANG PENTING ⭐️
        cb.sendPluginResult(result);

        Log.i(TAG, "Wallpaper change listener registered successfully - READY FOR MULTIPLE EVENTS");

    } catch (Exception e) {
        Log.e(TAG, "Failed to register wallpaper listener", e);
        sendError(cb, "Failed to register wallpaper listener: " + e.getMessage());
        isListening.set(false);
    }
}


/**
 * Custom BroadcastReceiver - DIPERBAIKI dengan better logging
 */
private class WallpaperChangeReceiver extends BroadcastReceiver {
    private long lastEventTime = 0;
    private static final long DEBOUNCE_DELAY = 5000; // 1 second debounce
    
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        
        String action = intent.getAction();
        long currentTime = System.currentTimeMillis();
        
        /* Debounce untuk hindari duplicate events */
        if (currentTime - lastEventTime < DEBOUNCE_DELAY) {
            Log.d(TAG, "Debounced duplicate wallpaper event");
            return;
        }
        
        lastEventTime = currentTime;
        
        Log.d(TAG, "🎯 Wallpaper change receiver triggered - Action: " + action);
        Log.d(TAG, "📡 Receiver state - isListening: " + isListening.get() + ", isDestroyed: " + isDestroyed.get());
        
        /* Cek callback status */
        CallbackContext cb = wallpaperCallbackRef != null ? wallpaperCallbackRef.get() : null;
        if (cb == null) {
            Log.w(TAG, "❌ No callback available in receiver");
            return;
        }
        
        /* Handle wallpaper change */
        if (action.equals(Intent.ACTION_WALLPAPER_CHANGED) || 
            action.equals(Intent.ACTION_SET_WALLPAPER) ||
            action.contains("WALLPAPER_CHANGED")) {
            
            /* Delay sedikit untuk pastikan wallpaper benar-benar sudah berubah */
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "🔄 Processing wallpaper change after delay...");
                handleWallpaperChange();
            }, 1000);
        }
    }
}

/**
 * Kirim event dengan data lengkap - DIPERBAIKI dengan KEEP_CALLBACK
 */
private void sendWallpaperEventWithFullData(String base64, int size, int width, int height) {
    if (isDestroyed.get()) {
        Log.w(TAG, "Cannot send event - bridge destroyed");
        return;
    }

    try {
        CallbackContext cb = wallpaperCallbackRef != null ? wallpaperCallbackRef.get() : null;
        if (cb == null) {
            Log.w(TAG, "Wallpaper callback is null, cannot send event");
            return;
        }

        JSONObject result = new JSONObject();
        result.put("base64", base64);
        result.put("format", "jpeg");
        result.put("size", size);
        result.put("width", width);
        result.put("height", height);
        result.put("type", "wallpaperChanged");
        result.put("timestamp", System.currentTimeMillis());
        result.put("eventCount", getNextEventCount()); // Untuk tracking

        /* PENTING: Gunakan PluginResult dengan setKeepCallback(true) */
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
        pluginResult.setKeepCallback(true); // ⭐️ INI YANG PENTING ⭐️
        
        mainHandler.post(() -> {
            if (!isDestroyed.get() && cb != null) {
                cb.sendPluginResult(pluginResult);
                Log.d(TAG, "📨 Event sent successfully (callback kept alive)");
            } else {
                Log.w(TAG, "Cannot send event - callback null or bridge destroyed");
            }
        });

    } catch (JSONException e) {
        Log.e(TAG, "Error creating wallpaper event JSON", e);
    }
}

/**
 * Kirim event error - DIPERBAIKI dengan KEEP_CALLBACK
 */
private void sendWallpaperEventWithError(String errorMessage) {
    if (isDestroyed.get()) return;

    try {
        CallbackContext cb = wallpaperCallbackRef != null ? wallpaperCallbackRef.get() : null;
        if (cb == null) return;

        JSONObject error = new JSONObject();
        error.put("error", errorMessage);
        error.put("code", "WALLPAPER_ERROR");
        error.put("type", "wallpaperChanged");
        error.put("timestamp", System.currentTimeMillis());

        PluginResult result = new PluginResult(PluginResult.Status.ERROR, error);
        result.setKeepCallback(true); // ⭐️ JAGA CALLBACK TETAP HIDUP ⭐️
        
        mainHandler.post(() -> {
            if (!isDestroyed.get() && cb != null) {
                cb.sendPluginResult(result);
            }
        });

    } catch (JSONException e) {
        Log.e(TAG, "Error creating error event JSON", e);
    }
}
/**
 * Quick event - DIPERBAIKI dengan KEEP_CALLBACK
 */
private void sendQuickWallpaperEvent(String message) {
    if (isDestroyed.get()) return;

    try {
        CallbackContext cb = wallpaperCallbackRef != null ? wallpaperCallbackRef.get() : null;
        if (cb == null) return;

        JSONObject event = new JSONObject();
        event.put("type", "wallpaperChanged");
        event.put("timestamp", System.currentTimeMillis());
        event.put("message", message);
        event.put("processing", true);
        event.put("quickEvent", true);
       
        PluginResult result = new PluginResult(PluginResult.Status.OK, event);
        result.setKeepCallback(true); // ⭐️ JAGA CALLBACK TETAP HIDUP ⭐️
        
        mainHandler.post(() -> {
            if (!isDestroyed.get() && cb != null) {
                cb.sendPluginResult(result);
                Log.d(TAG, "⚡ Quick event: " + message);
            }
        });

    } catch (JSONException e) {
        Log.e(TAG, "Error creating quick event JSON", e);
    }
}

// Helper untuk tracking events
private int eventCounter = 0;
private int getNextEventCount() {
    return ++eventCounter;
}

private void handleWallpaperChange() {
    if (isDestroyed.get() || !isListening.get()) {
        Log.w(TAG, "handleWallpaperChange - Bridge destroyed or not listening");
        return;
    }

    Log.d(TAG, "🔄 Wallpaper change detected, starting processing...");

    /* Kirim quick event notification dulu */
    sendQuickWallpaperEvent("🔥 Wallpaper change detected 👍");

    /* Process wallpaper data */
    executorService.execute(() -> {
        Bitmap bitmap = null;
        Bitmap optimizedBitmap = null;
        ByteArrayOutputStream baos = null;
        
        try {
            Log.d(TAG, "Step 1: Getting WallpaperManager instance...");
            WallpaperManager wm = WallpaperManager.getInstance(context);
            
            Log.d(TAG, "Step 2: Getting wallpaper drawable...");
            Drawable drawable = wm.getDrawable();
            
            if (drawable == null) {
                Log.e(TAG, "❌ Drawable is NULL - no wallpaper available");
                sendWallpaperEventWithError("No wallpaper drawable available");
                return;
            }

            Log.d(TAG, "✅ Drawable obtained: " + drawable.getClass().getSimpleName());
            Log.d(TAG, "Step 3: Converting drawable to bitmap...");
            
            bitmap = drawableToBitmap(drawable);
            if (bitmap == null) {
                Log.e(TAG, "❌ Failed to convert drawable to bitmap");
                sendWallpaperEventWithError("Failed to convert wallpaper to bitmap");
                return;
            }

            Log.d(TAG, "✅ Bitmap created: " + bitmap.getWidth() + "x" + bitmap.getHeight() + 
                      ", recycled: " + bitmap.isRecycled());
            
            Log.d(TAG, "Step 4: Resizing bitmap...");
            optimizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);
            if (optimizedBitmap == null) {
                Log.w(TAG, "⚠️ Failed to resize wallpaper, using original");
                optimizedBitmap = bitmap;
            } else {
                Log.d(TAG, "✅ Bitmap resized: " + optimizedBitmap.getWidth() + "x" + optimizedBitmap.getHeight());
            }
            
            Log.d(TAG, "Step 5: Compressing to JPEG...");
            baos = new ByteArrayOutputStream();
            boolean compressSuccess = optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
            
            if (!compressSuccess) {
                Log.e(TAG, "❌ Failed to compress bitmap");
                sendWallpaperEventWithError("Failed to compress wallpaper");
                return;
            }

            Log.d(TAG, "✅ Bitmap compressed: " + baos.size() + " bytes");
            
            Log.d(TAG, "Step 6: Converting to base64...");
            
            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            Log.d(TAG, "✅ Base64 created, length: " + base64.length());
            
            /* Kirim data lengkap */
            Log.d(TAG, "Step 7: Sending full data event...");
            sendWallpaperEventWithFullData(
                base64, 
                baos.size(),
                optimizedBitmap.getWidth(),
                optimizedBitmap.getHeight()
            );
            
            Log.i(TAG, "🎉 SUCCESS: Wallpaper change processing completed");

        } catch (Exception e) {
            Log.e(TAG, "❌ ERROR in handleWallpaperChange: " + e.getMessage(), e);
            sendWallpaperEventWithError("Error processing wallpaper: " + e.getMessage());
        } finally {
            /* Cleanup resources */
            Log.d(TAG, "Step 8: Cleaning up resources...");
            if (optimizedBitmap != null && optimizedBitmap != bitmap) {
                optimizedBitmap.recycle();
                Log.d(TAG, "✅ Optimized bitmap recycled");
            }
            if (baos != null) {
                try { 
                    baos.close(); 
                    Log.d(TAG, "✅ ByteArrayOutputStream closed");
                } catch (Exception e) { 
                    Log.w(TAG, "⚠️ Error closing stream", e);
                }
            }
            Log.d(TAG, "🔄 Wallpaper change processing finished");
        }
    });
}

/**
 * Kirim result sebagai event (bukan success biasa)
 */
private void sendEventResult(final CallbackContext cb, final JSONObject result) {
    mainHandler.post(() -> {
        if (!isDestroyed.get() && cb != null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
            pluginResult.setKeepCallback(true); // PENTING: keep callback untuk event selanjutnya
            cb.sendPluginResult(pluginResult);
            Log.d(TAG, "Wallpaper change event sent successfully");
        }
    });
}

/**
 * Kirim event wallpaper change dengan data lengkap seperti getWallpaper()
 */
private void sendWallpaperEventWithData(String base64Image, int size, int width, int height, String error) {
    if (isDestroyed.get()) return;

    try {
        CallbackContext cb = wallpaperCallbackRef != null ? wallpaperCallbackRef.get() : null;
        if (cb == null) {
            Log.w(TAG, "Wallpaper callback is null, cannot send event");
            return;
        }

        JSONObject event = new JSONObject();
        event.put("type", "wallpaperChanged");
        event.put("timestamp", System.currentTimeMillis());
        
        if (error != null) {
            event.put("error", error);
            event.put("hasData", false);
        } else {
            /* DATA LENGKAP MIRIP GETWALLPAPER() */
            event.put("base64", base64Image != null ? base64Image : "");
            event.put("format", "jpeg");
            event.put("size", size);
            event.put("width", width);
            event.put("height", height);
            event.put("hasData", base64Image != null && !base64Image.isEmpty());
        }

        PluginResult result = new PluginResult(PluginResult.Status.OK, event);
        result.setKeepCallback(true);
        
        mainHandler.post(() -> {
            if (!isDestroyed.get() && cb != null) {
                cb.sendPluginResult(result);
                Log.d(TAG, "Wallpaper event sent with full data: " + width + "x" + height);
            }
        });

        if (callbackSender != null) {
            callbackSender.sendEvent("onWallpaperChanged", event);
        }

    } catch (JSONException e) {
        Log.e(TAG, "Error creating wallpaper event JSON", e);
    }
}

/**
 * Kirim quick event notification tanpa processing image
 */
/* 
private void sendQuickWallpaperEvent(String message, boolean processing) {
    if (isDestroyed.get()) return;

    try {
        CallbackContext cb = wallpaperCallbackRef != null ? wallpaperCallbackRef.get() : null;
        if (cb == null) {
            Log.w(TAG, "Wallpaper callback is null, cannot send quick event");
            return;
        }

        JSONObject event = new JSONObject();
        event.put("type", "wallpaperChanged");
        event.put("timestamp", System.currentTimeMillis());
        event.put("message", message);
        event.put("processing", processing);
        event.put("hasData", false);
        event.put("quickEvent", true);

        PluginResult result = new PluginResult(PluginResult.Status.OK, event);
        result.setKeepCallback(true);
        
        mainHandler.post(() -> {
            if (!isDestroyed.get() && cb != null) {
                cb.sendPluginResult(result);
                Log.d(TAG, "Quick wallpaper event sent: " + message);
            }
        });

    } catch (JSONException e) {
        Log.e(TAG, "Error creating quick event JSON", e);
    }
} */

private void sendWallpaperEvent(String base64Image, String error) {
    if (isDestroyed.get()) return;

    try {
        CallbackContext cb = wallpaperCallbackRef != null ? wallpaperCallbackRef.get() : null;
        if (cb == null) {
            Log.w(TAG, "Wallpaper callback is null, cannot send event");
            return;
        }

        JSONObject event = new JSONObject();
        event.put("type", "wallpaperChanged");
        event.put("timestamp", System.currentTimeMillis());
        
        if (error != null) {
            event.put("error", error);
            event.put("hasData", false);
        } else {
            /* KIRIM DATA LENGKAP seperti di getWallpaper() */
            event.put("base64", base64Image != null ? base64Image : "");
            event.put("format", "jpeg");
            event.put("size", base64Image != null ? base64Image.length() : 0);
            event.put("hasData", base64Image != null && !base64Image.isEmpty());
            
            /* Tambahkan dimension info jika available */
            // Note: Untuk dimension, kita perlu menyimpan info dari bitmap sebelumnya
            // atau decode ulang dari base64 (tidak optimal)
            // Solusi: Modifikasi handleWallpaperChange untuk menyimpan dimension
        }

        PluginResult result = new PluginResult(PluginResult.Status.OK, event);
        result.setKeepCallback(true);
        
        mainHandler.post(() -> {
            if (!isDestroyed.get() && cb != null) {
                cb.sendPluginResult(result);
                Log.d(TAG, "Wallpaper change event sent with full data");
            }
        });

        if (callbackSender != null) {
            callbackSender.sendEvent("onWallpaperChanged", event);
        }

    } catch (JSONException e) {
        Log.e(TAG, "Error creating wallpaper event JSON", e);
    }
}
	

    /**
     * Hentikan pendengaran wallpaper - FIXED
     */
    public void stopListening() {
        if (isListening.compareAndSet(true, false)) {
            if (wallpaperReceiver != null) {
                try {
                    context.unregisterReceiver(wallpaperReceiver);
                    Log.i(TAG, "Wallpaper listener unregistered successfully");
                } catch (Exception e) {
                    Log.w(TAG, "Error unregistering wallpaper receiver", e);
                }
            }
        }
        
        if (wallpaperCallbackRef != null) {
            wallpaperCallbackRef.clear();
        }
    }

    /**
     * Convert Drawable to Bitmap safely
     */

/*     private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            Log.w(TAG, "Drawable is null");
            return null;
        }

        try {
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap != null && !bitmap.isRecycled()) {
                    return bitmap;
                }
            }

            int width = Math.max(drawable.getIntrinsicWidth(), 1);
            int height = Math.max(drawable.getIntrinsicHeight(), 1);
            
            // Ensure reasonable dimensions to prevent OOM
            if (width > 4096 || height > 4096) {
                width = 1024;
                height = 1024;
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            if (bitmap == null) {
                Log.e(TAG, "Failed to create bitmap");
                return null;
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            
            return bitmap;
            
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError in drawableToBitmap", e);
            System.gc();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error converting drawable to bitmap", e);
            return null;
        }
    } */

/**
 * Convert Drawable to Bitmap safely - DIPERBAIKI dengan better error handling
 */
private Bitmap drawableToBitmap(Drawable drawable) {
    if (drawable == null) {
        Log.w(TAG, "❌ Drawable is null in drawableToBitmap");
        return null;
    }

    try {
        Log.d(TAG, "🔧 Converting drawable to bitmap...");
        
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                Log.d(TAG, "✅ Using existing bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                return bitmap;
            } else {
                Log.w(TAG, "⚠️ BitmapDrawable has null or recycled bitmap");
            }
        }

        int width = Math.max(drawable.getIntrinsicWidth(), 1);
        int height = Math.max(drawable.getIntrinsicHeight(), 1);
        
        Log.d(TAG, "📐 Drawable dimensions: " + width + "x" + height);
        
        // Ensure reasonable dimensions to prevent OOM
        if (width > 4096 || height > 4096) {
            Log.w(TAG, "⚠️ Large dimensions, scaling down to 1024x1024");
            width = 1024;
            height = 1024;
        }

        Log.d(TAG, "🎨 Creating new bitmap with ARGB_8888 config...");
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        
        if (bitmap == null) {
            Log.e(TAG, "❌ Failed to create bitmap - Bitmap.createBitmap returned null");
            return null;
        }

        Log.d(TAG, "🖌️ Drawing drawable to canvas...");
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        
        Log.d(TAG, "✅ Successfully created bitmap from drawable: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        return bitmap;
        
    } catch (OutOfMemoryError e) {
        Log.e(TAG, "❌ OutOfMemoryError in drawableToBitmap", e);
        System.gc();
        return null;
    } catch (Exception e) {
        Log.e(TAG, "❌ Exception in drawableToBitmap: " + e.getMessage(), e);
        return null;
    }
}

    /**
     * Resize bitmap untuk optimasi performance
     */
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        if (bitmap == null || bitmap.isRecycled()) {
            Log.w(TAG, "Bitmap is null or recycled in resizeBitmap");
            return null;
        }

        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            if (width <= maxSize && height <= maxSize) {
                return bitmap; // Return original, don't modify
            }

            float ratio = (float) width / height;
            int newWidth, newHeight;

            if (width > height) {
                newWidth = maxSize;
                newHeight = Math.max((int) (maxSize / ratio), 1);
            } else {
                newHeight = maxSize;
                newWidth = Math.max((int) (maxSize * ratio), 1);
            }

            // Ensure dimensions are reasonable
            if (newWidth <= 0 || newHeight <= 0 || newWidth > 4096 || newHeight > 4096) {
                Log.w(TAG, "Invalid dimensions for resize: " + newWidth + "x" + newHeight);
                return bitmap;
            }

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            if (resizedBitmap == null) {
                Log.w(TAG, "Bitmap.createScaledBitmap returned null");
                return bitmap; // Return original on failure
            }
            
            return resizedBitmap;
            
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError in resizeBitmap", e);
            System.gc();
            return bitmap; // Return original on OOM
        } catch (Exception e) {
            Log.e(TAG, "Error resizing bitmap", e);
            return bitmap; // Return original on error
        }
    }

    /**
     * Helper untuk send error response - FIXED: Use mainHandler
     */
    private void sendError(CallbackContext cb, String message) {
        if (isDestroyed.get() || cb == null) return;
        
        mainHandler.post(() -> {
            if (!isDestroyed.get() && cb != null) {
                try {
                    JSONObject error = new JSONObject();
                    error.put("error", message);
                    error.put("code", "WALLPAPER_ERROR");
                    cb.error(error);
                } catch (JSONException e) {
                    cb.error("{\"error\":\"" + message.replace("\"", "\\\"") + "\",\"code\":\"WALLPAPER_ERROR\"}");
                }
            }
        });
    }

    /**
     * Clean up resources
     */
    public void destroy() {
        if (isDestroyed.compareAndSet(false, true)) {
            stopListening();
            
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            
            if (mainHandler != null) {
                mainHandler.removeCallbacksAndMessages(null);
            }
            
            wallpaperCallbackRef = null;
            wallpaperReceiver = null;
            
            Log.i(TAG, "WallpaperManagerBridge destroyed successfully");
        }
    }
}