package org.apache.cordova.resqpeernet.modules;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log; 
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.graphics.Color;

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * DisplayManagerBridge - Manajemen tampilan layar dan fitur fullscreen
 * Handles: fullscreen mode, screen orientation, display metrics, wake locks, screenshot protection
 */
public class DisplayManagerBridge {
    private static final String TAG = "DisplayManagerBridge";
    
    private Context context;
    private Activity activity;
    private Window window;
    private boolean isFullscreen = false;
    
    // Orientation constants
    public static final String ORIENTATION_PORTRAIT = "portrait";
    public static final String ORIENTATION_LANDSCAPE = "landscape";
    public static final String ORIENTATION_SENSOR = "sensor";
    public static final String ORIENTATION_UNSPECIFIED = "unspecified";
    
    // Orientation numeric constants
    public static final int ORIENTATION_PORTRAIT_VALUE = 1;
    public static final int ORIENTATION_LANDSCAPE_VALUE = 0;
    public static final int ORIENTATION_SENSOR_VALUE = 4;
    public static final int ORIENTATION_UNSPECIFIED_VALUE = -1;
    
    public DisplayManagerBridge(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        this.window = activity.getWindow();
    }
    
    /**
     * Masuk ke mode fullscreen - sembunyikan status bar dan navigation bar secara permanen
     */
    public void enterFullscreen(CallbackContext callbackContext) {
        try {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Buat variabel final untuk digunakan dalam inner class
                        final int uiOptions = buildFullscreenUiOptions();
                        
                        // Untuk Android Pie (API 28) dan di atasnya - sembunyikan cutout/notch
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            WindowManager.LayoutParams params = window.getAttributes();
                            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                            window.setAttributes(params);
                        }
                        
                        window.getDecorView().setSystemUiVisibility(uiOptions);
                        
                        // Tambahkan listener untuk menangani ketika system UI muncul kembali
                        window.getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                            @Override
                            public void onSystemUiVisibilityChange(int visibility) {
                                // Jika system UI terlihat, sembunyikan lagi
                                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                                    window.getDecorView().setSystemUiVisibility(uiOptions);
                                }
                            }
                        });
                        
                        isFullscreen = true;
                        
                        JSONObject result = new JSONObject();
                        result.put("status", "success");
                        result.put("mode", "fullscreen");
                        result.put("immersive", true);
                        callbackContext.success(result);
                        
                    } catch (Exception e) {
                        callbackContext.error("Error entering fullscreen: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callbackContext.error("Error: " + e.getMessage());
        }
    }
    
    /**
     * Keluar dari mode fullscreen - tampilkan system UI
     */
	/* 
    public void exitFullscreen(CallbackContext callbackContext) {
        try {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                        
                        window.getDecorView().setSystemUiVisibility(uiOptions);
                        
                        // Hapus listener
                        window.getDecorView().setOnSystemUiVisibilityChangeListener(null);
                        
                        isFullscreen = false;
                        
                        JSONObject result = new JSONObject();
                        result.put("status", "success");
                        result.put("mode", "normal");
                        callbackContext.success(result);
                        
                    } catch (Exception e) {
                        callbackContext.error("Error exiting fullscreen: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callbackContext.error("Error: " + e.getMessage());
        }
    }
	*/
	
	/**
	 * Keluar dari semua mode fullscreen
	 */
	public void exitFullscreen(CallbackContext callbackContext) {
		try {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						// 1. Clear system UI flags
						int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
								| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
						
						window.getDecorView().setSystemUiVisibility(uiOptions);
						
						// 2. Clear window flags
						window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
						
						// 3. Show action bars (hanya yang tersedia)
						if (activity.getActionBar() != null) {
							activity.getActionBar().show();
						}
						// Hapus getSupportActionBar() karena tidak tersedia di Activity biasa
						
						// 4. Reset display cutout
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
							WindowManager.LayoutParams params = window.getAttributes();
							params.layoutInDisplayCutoutMode = 
								WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
							window.setAttributes(params);
						}
						
						// 5. Remove all listeners
						final View decorView = window.getDecorView();
						decorView.setOnSystemUiVisibilityChangeListener(null);
						decorView.setOnTouchListener(null);
						
						isFullscreen = false;
						
						JSONObject result = new JSONObject();
						result.put("status", "success");
						result.put("mode", "normal");
						result.put("statusBarHidden", false);
						result.put("navBarHidden", false);
						callbackContext.success(result);
						
					} catch (Exception e) {
						callbackContext.error("Error exiting fullscreen: " + e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			callbackContext.error("Error: " + e.getMessage());
		}
	}
    
    /**
     * Toggle antara fullscreen dan normal mode
     */
    public void toggleFullscreen(CallbackContext callbackContext) {
        if (isFullscreen) {
            exitFullscreen(callbackContext);
        } else {
            enterFullscreen(callbackContext);
        }
    }
    
    /**
     * Mode fullscreen permanen - navbar benar-benar disembunyikan dan tidak muncul kembali
     */
	/* 
    public void enterPermanentFullscreen(CallbackContext callbackContext) {
        try {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Gunakan kombinasi flag yang lebih agresif
                        final int uiOptions = buildFullscreenUiOptions();
                        
                        final View decorView = window.getDecorView();
                        decorView.setSystemUiVisibility(uiOptions);
                        
                        // Setup continuous hiding
                        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                            @Override
                            public void onSystemUiVisibilityChange(int visibility) {
                                // Delay sedikit kemudian sembunyikan lagi
                                decorView.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        decorView.setSystemUiVisibility(uiOptions);
                                    }
                                }, 100);
                            }
                        });
                        
                        isFullscreen = true;
                        
                        JSONObject result = new JSONObject();
                        result.put("status", "success");
                        result.put("mode", "permanent_fullscreen");
                        result.put("immersive", true);
                        callbackContext.success(result);
                        
                    } catch (Exception e) {
                        callbackContext.error("Error entering permanent fullscreen: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callbackContext.error("Error: " + e.getMessage());
        }
    }
    */
	
	

	/**
	 * /**
	 * Mode fullscreen permanen - status bar dan navbar 100% disembunyikan
	 */
/*
	public void enterPermanentFullscreen(CallbackContext callbackContext) {
		try {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						// 1. SYSTEM UI FLAGS (Modern Approach)
						final int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
								| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
								| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_FULLSCREEN
								| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
						
						// 2. WINDOW FLAGS (Legacy Approach - untuk kompatibilitas)
						window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
						
						// 3. DISABLE ACTION BAR (hanya yang tersedia)
						if (activity.getActionBar() != null) {
							activity.getActionBar().hide();
						}
						// Hapus getSupportActionBar() karena tidak tersedia di Activity biasa
						
						// 4. HANDLE DISPLAY CUTOUT (Android 9.0+)
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
							WindowManager.LayoutParams params = window.getAttributes();
							params.layoutInDisplayCutoutMode = 
								WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
							window.setAttributes(params);
						}
						
						final View decorView = window.getDecorView();
						decorView.setSystemUiVisibility(uiOptions);
						
						// 5. CONTINUOUS HIDING MECHANISM
						decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
							@Override
							public void onSystemUiVisibilityChange(int visibility) {
								// Jika system UI terlihat (status bar/navbar muncul)
								if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
									// Delay dan sembunyikan kembali
									decorView.postDelayed(new Runnable() {
										@Override
										public void run() {
											// Re-apply semua flags
											decorView.setSystemUiVisibility(uiOptions);
											window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
										}
									}, 50); // Delay sangat singkat
								}
							}
						});
						
						// 6. TOUCH LISTENER untuk immediate hide
						decorView.setOnTouchListener(new View.OnTouchListener() {
							@Override
							public boolean onTouch(View v, MotionEvent event) {
								// Segera sembunyikan system UI jika muncul
								decorView.setSystemUiVisibility(uiOptions);
								return false;
							}
						});
						
						isFullscreen = true;
						
						JSONObject result = new JSONObject();
						result.put("status", "success");
						result.put("mode", "permanent_fullscreen");
						result.put("immersive", true);
						result.put("statusBarHidden", true);
						result.put("navBarHidden", true);
						callbackContext.success(result);
						
					} catch (Exception e) {
						callbackContext.error("Error entering permanent fullscreen: " + e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			callbackContext.error("Error: " + e.getMessage());
		}
	}
*/

	/**
	 * Mode fullscreen permanen - status bar dan navbar 100% disembunyikan
	 */
	public void enterPermanentFullscreen(CallbackContext callbackContext) {
		try {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						// BUAT VARIABLE NON-FINAL DULU
						int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
								| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
								| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_FULLSCREEN
								| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
						
						// SEKARANG BUAT FINAL UNTUK DIGUNAKAN DI INNER CLASS
						final int finalUiOptions = uiOptions;
						
						// WINDOW FLAGS YANG LEBIH AGRESIF
						window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
						window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
						
						// HIDE ACTION BAR
						if (activity.getActionBar() != null) {
							activity.getActionBar().hide();
						}
						
						// DISABLE CUTOUT/NOTCH SECARA TOTAL
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
							WindowManager.LayoutParams params = window.getAttributes();
							params.layoutInDisplayCutoutMode = 
								WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
							window.setAttributes(params);
						}
						
						final View decorView = window.getDecorView();
						
						// APPLY SEMUA APPROACH SEKALIGUS
						decorView.setSystemUiVisibility(finalUiOptions);
						
						// ? APPROACH KHUSUS: BLOCK SWIPE GESTURE SECARA TOTAL
						decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
							@Override
							public void onSystemUiVisibilityChange(int visibility) {
								// ??LANGSUNG SEMBUNYIKAN TANPA DELAY MESKI SEDIKIT MUNCUL
								decorView.setSystemUiVisibility(finalUiOptions);
								window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
								
								// ? FORCE HIDE LANGSUNG TANPA MENUNGGU
								decorView.post(new Runnable() {
									@Override
									public void run() {
										decorView.setSystemUiVisibility(finalUiOptions);
									}
								});
							}
						});
						
						// ? TOUCH LISTENER YANG SUPER AGRESIF
						decorView.setOnTouchListener(new View.OnTouchListener() {
							@Override
							public boolean onTouch(View v, MotionEvent event) {
								// ? PADA SETIAP TOUCH EVENT, PASTIKAN TETAP HIDDEN
								decorView.setSystemUiVisibility(finalUiOptions);
								window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
								return false;
							}
						});
						
						// ? FORCE HIDE BERKALA YANG LEBIH INTENSIF
						for (int i = 1; i <= 15; i++) {
							decorView.postDelayed(new Runnable() {
								@Override
								public void run() {
									decorView.setSystemUiVisibility(finalUiOptions);
									window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
								}
							}, i * 200); // ? SETIAP 200ms SELAMA 3 DETIK
						}
						
						// ? EXTRA: DISABLE GESTURE NAVIGATION (Android 10+)
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
							window.setSystemGestureExclusionRects(java.util.Arrays.asList(
								new android.graphics.Rect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE)
							));
						}
						
						isFullscreen = true;
						
						JSONObject result = new JSONObject();
						result.put("status", "success");
						result.put("mode", "permanent_fullscreen");
						result.put("immersive", true);
						result.put("statusBarHidden", true);
						result.put("navBarHidden", true);
						result.put("gestureBlocked", true); // ? GESTURE DIBLOCK
						result.put("swipeDisabled", true); // ? SWIPE DINONAKTIFKAN
						callbackContext.success(result);
						
					} catch (Exception e) {
						callbackContext.error("Error entering permanent fullscreen: " + e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			callbackContext.error("Error: " + e.getMessage());
		}
	}



	/**
	 * Method khusus untuk menyembunyikan status bar dengan lebih agresif
	 */
	private void hideStatusBarPermanently() {
		try {
			// Approach 1: Window flags
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			
			// Approach 2: System UI flags  
			int flags = View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
			
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				flags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			}
			
			window.getDecorView().setSystemUiVisibility(flags);
			
			// Approach 3: Untuk Android 4.0-4.1
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
							  WindowManager.LayoutParams.FLAG_FULLSCREEN);
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Error hiding status bar: " + e.getMessage());
		}
	}	
	
    /**
     * Set orientasi layar
     */
    public void setScreenOrientation(JSONObject args, CallbackContext callbackContext) {
        try {
            String orientation = args.optString("orientation", ORIENTATION_UNSPECIFIED);
            int activityOrientation;
            
            switch (orientation) {
                case ORIENTATION_PORTRAIT:
                    activityOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case ORIENTATION_LANDSCAPE:
                    activityOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case ORIENTATION_SENSOR:
                    activityOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                    break;
                case ORIENTATION_UNSPECIFIED:
                    activityOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                    break;
                default:
                    activityOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                    break;
            }
            
            activity.setRequestedOrientation(activityOrientation);
            
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("orientation", orientation);
            result.put("activityOrientation", activityOrientation);
            callbackContext.success(result);
            
        } catch (Exception e) {
            callbackContext.error("Error setting orientation: " + e.getMessage());
        }
    }
    
    /**
     * Jaga layar tetap menyala
     */
    public void keepScreenOn(JSONObject args, CallbackContext callbackContext) {
        try {
            boolean keepOn = args.optBoolean("keepOn", true);
            
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (keepOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                    
                    try {
                        JSONObject result = new JSONObject();
                        result.put("status", "success");
                        result.put("screenKeepOn", keepOn);
                        callbackContext.success(result);
                    } catch (JSONException e) {
                        callbackContext.error("Error creating response: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            callbackContext.error("Error setting screen keep on: " + e.getMessage());
        }
    }
    
    /**
     * Dapatkan metrik layar device
     */
    public void getDisplayMetrics(CallbackContext callbackContext) {
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            
            JSONObject result = new JSONObject();
            result.put("widthPixels", metrics.widthPixels);
            result.put("heightPixels", metrics.heightPixels);
            result.put("density", metrics.density);
            result.put("densityDpi", metrics.densityDpi);
            result.put("scaledDensity", metrics.scaledDensity);
            result.put("xdpi", metrics.xdpi);
            result.put("ydpi", metrics.ydpi);
            
            // Additional info
            result.put("screenSize", getScreenSize());
            result.put("orientation", getCurrentOrientation());
            result.put("isFullscreen", isFullscreen);
            
            callbackContext.success(result);
            
        } catch (Exception e) {
            callbackContext.error("Error getting display metrics: " + e.getMessage());
        }
    }
    
    /**
     * Dapatkan status fullscreen saat ini
     */
    public void getFullscreenStatus(CallbackContext callbackContext) {
        try {
            JSONObject result = new JSONObject();
            result.put("isFullscreen", isFullscreen);
            result.put("currentOrientation", getCurrentOrientation());
            result.put("screenSize", getScreenSize());
            callbackContext.success(result);
            
        } catch (Exception e) {
            callbackContext.error("Error getting fullscreen status: " + e.getMessage());
        }
    }
    
    /**
     * Mode immersive (hide system UI temporarily)
     */
	/* 
    public void enterImmersiveMode(CallbackContext callbackContext) {
        try {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN;
                        
                        window.getDecorView().setSystemUiVisibility(uiOptions);
                        
                        JSONObject result = new JSONObject();
                        result.put("status", "success");
                        result.put("mode", "immersive");
                        callbackContext.success(result);
                        
                    } catch (Exception e) {
                        callbackContext.error("Error entering immersive mode: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callbackContext.error("Error: " + e.getMessage());
        }
    }
	*/
	
	/**
	 * Mode immersive - status bar dan navbar disembunyikan tapi bisa diakses dengan gesture
	 */
	/* sukses 
	public void enterImmersiveMode(CallbackContext callbackContext) {
		try {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						// APPROACH 1: SYSTEM UI FLAGS (Modern - Android 4.1+)
						final int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
								| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
								| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_FULLSCREEN
								| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
							//	| View.SYSTEM_UI_FLAG_IMMERSIVE;
						
						// APPROACH 2: WINDOW FLAGS (Legacy - lebih agresif)
						window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
						window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
						
						// APPROACH 3: HIDE ACTION BAR (jika ada)
						if (activity.getActionBar() != null) {
							activity.getActionBar().hide();
						}
						
						// APPROACH 4: DISABLE NOTCH/CUTOUT (Android 9.0+)
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
							WindowManager.LayoutParams params = window.getAttributes();
							params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;

							window.setAttributes(params);
						}
						
						final View decorView = window.getDecorView();
						
						// APPLY SEMUA APPROACH SEKALIGUS
						decorView.setSystemUiVisibility(uiOptions);
						
						// APPROACH 5: CONTINUOUS HIDING YANG LEBIH AGGRESIF
						decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
							@Override
							public void onSystemUiVisibilityChange(int visibility) {
								// Jika system UI muncul (status bar/navbar terlihat)
								if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
									// Sembunyikan kembali dengan delay sangat singkat
									decorView.postDelayed(new Runnable() {
										@Override
										public void run() {
											// Re-apply semua flags dengan kombinasi yang lebih kuat
											final int enhancedUiOptions = uiOptions; // BUAT FINAL
											
											// Tambahkan flag tambahan untuk Android versi lebih baru
											if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
												// enhancedUiOptions sudah final, tidak bisa di-modify
												// Gunakan uiOptions langsung saja
											}
											
											decorView.setSystemUiVisibility(enhancedUiOptions);
											window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
											
											// Force hide sekali lagi setelah 100ms
											decorView.postDelayed(new Runnable() {
												@Override
												public void run() {
													decorView.setSystemUiVisibility(enhancedUiOptions);
												}
											}, 100);
										}
									}, 10); // Delay sangat singkat 10ms
								}
							}
						});
						
						// APPROACH 6: TOUCH LISTENER YANG LEBIH RESPONSIF
						decorView.setOnTouchListener(new View.OnTouchListener() {
							@Override
							public boolean onTouch(View v, MotionEvent event) {
								// Segera sembunyikan system UI pada setiap touch event
								if (event.getAction() == MotionEvent.ACTION_DOWN) {
									decorView.setSystemUiVisibility(uiOptions);
									window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
								}
								return false;
							}
						});
						
						// APPROACH 7: FORCE HIDE BERKALA (setiap 500ms selama 5 detik pertama)
						for (int i = 1; i <= 10; i++) {
							decorView.postDelayed(new Runnable() {
								@Override
								public void run() {
									decorView.setSystemUiVisibility(uiOptions);
								}
							}, i * 500); // Setiap 500ms
						}
						
						isFullscreen = true;
						
						JSONObject result = new JSONObject();
						result.put("status", "success");
						result.put("mode", "permanent_fullscreen");
						result.put("immersive", true);
						result.put("statusBarHidden", true);
						result.put("navBarHidden", true);
						callbackContext.success(result);
						
					} catch (Exception e) {
						callbackContext.error("Error entering permanent fullscreen: " + e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			callbackContext.error("Error: " + e.getMessage());
		}
	}
	
	*/
	
	public void enterImmersiveMode(CallbackContext callbackContext) {
		try {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						// BUAT VARIABLE NON-FINAL DULU
						int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
								| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
								| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
								| View.SYSTEM_UI_FLAG_FULLSCREEN
								| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
						
						// SEKARANG BUAT FINAL UNTUK DIGUNAKAN DI INNER CLASS
						final int finalUiOptions = uiOptions;
						
						// APPROACH 2: WINDOW FLAGS (Legacy - lebih agresif)
						window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
						window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
						
						// APPROACH 3: HIDE ACTION BAR (jika ada)
						if (activity.getActionBar() != null) {
							activity.getActionBar().hide();
						}
						
						// APPROACH 4: DISABLE NOTCH/CUTOUT (Android 9.0+)
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
							WindowManager.LayoutParams params = window.getAttributes();
							params.layoutInDisplayCutoutMode = 
								WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
							window.setAttributes(params);
						}
						
						final View decorView = window.getDecorView();
						
						// APPLY SEMUA APPROACH SEKALIGUS
						decorView.setSystemUiVisibility(finalUiOptions);
						
						// APPROACH 5: CONTINUOUS HIDING YANG LEBIH AGGRESIF
						decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
							@Override
							public void onSystemUiVisibilityChange(int visibility) {
								// Jika system UI muncul (status bar/navbar terlihat)
								if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
									// Sembunyikan kembali dengan delay sangat singkat
									decorView.postDelayed(new Runnable() {
										@Override
										public void run() {
											// Re-apply semua flags
											decorView.setSystemUiVisibility(finalUiOptions);
											window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
											
											// Force hide sekali lagi setelah 100ms
											decorView.postDelayed(new Runnable() {
												@Override
												public void run() {
													decorView.setSystemUiVisibility(finalUiOptions);
												}
											}, 100);
										}
									}, 10); // Delay sangat singkat 10ms
								}
							}
						});
						
						// APPROACH 6: TOUCH LISTENER YANG LEBIH RESPONSIF
						decorView.setOnTouchListener(new View.OnTouchListener() {
							@Override
							public boolean onTouch(View v, MotionEvent event) {
								// Segera sembunyikan system UI pada setiap touch event
								if (event.getAction() == MotionEvent.ACTION_DOWN) {
									decorView.setSystemUiVisibility(finalUiOptions);
									window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
								}
								return false;
							}
						});
						
						// APPROACH 7: FORCE HIDE BERKALA (setiap 500ms selama 5 detik pertama)
						for (int i = 1; i <= 10; i++) {
							decorView.postDelayed(new Runnable() {
								@Override
								public void run() {
									decorView.setSystemUiVisibility(finalUiOptions);
								}
							}, i * 500); // Setiap 500ms
						}
						
						isFullscreen = true;
						
						JSONObject result = new JSONObject();
						result.put("status", "success");
						result.put("mode", "immersive"); // Tetap immersive mode
						result.put("immersive", true);
						result.put("statusBarHidden", true);
						result.put("navBarHidden", true);
						callbackContext.success(result);
						
					} catch (Exception e) {
						callbackContext.error("Error entering immersive mode: " + e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			callbackContext.error("Error: " + e.getMessage());
		}
	}
    
    /**
     * Setup touch listener untuk menyembunyikan navbar ketika layar disentuh
     */
    public void setupTouchToHide(final CallbackContext callbackContext) {
        try {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final View decorView = window.getDecorView();
                        final int uiOptions = buildFullscreenUiOptions();
                        
                        decorView.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                if (isFullscreen) {
                                    decorView.setSystemUiVisibility(uiOptions);
                                }
                                return false;
                            }
                        });
                        
                        JSONObject result = new JSONObject();
                        result.put("status", "success");
                        result.put("message", "Touch to hide enabled");
                        callbackContext.success(result);
                        
                    } catch (Exception e) {
                        callbackContext.error("Error setting up touch to hide: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callbackContext.error("Error: " + e.getMessage());
        }
    }
    
    /**
     * Enable/disable screenshot dan screen recording
     */
    public void setScreenshotAllowed(JSONObject args, CallbackContext callbackContext) {
        try {
            boolean allowScreenshot = args.optBoolean("allow", true);
            
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            if (allowScreenshot) {
                                // Allow screenshot dan screen recording
                                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                            } else {
                                // Block screenshot dan screen recording
                                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                            }
                        }
                        
                        JSONObject result = new JSONObject();
                        result.put("status", "success");
                        result.put("screenshotAllowed", allowScreenshot);
                        result.put("screenRecordingAllowed", allowScreenshot);
                        result.put("sdkVersion", Build.VERSION.SDK_INT);
                        callbackContext.success(result);
                        
                    } catch (Exception e) {
                        callbackContext.error("Error setting screenshot permission: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            callbackContext.error("Error: " + e.getMessage());
        }
    }
    
    /**
     * Dapatkan status screenshot permission
     */
    public void getScreenshotStatus(CallbackContext callbackContext) {
        try {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean isScreenshotAllowed = true;
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            int flags = window.getAttributes().flags;
                            isScreenshotAllowed = (flags & WindowManager.LayoutParams.FLAG_SECURE) == 0;
                        }
                        
                        JSONObject result = new JSONObject();
                        result.put("status", "success");
                        result.put("screenshotAllowed", isScreenshotAllowed);
                        result.put("screenRecordingAllowed", isScreenshotAllowed);
                        result.put("sdkVersion", Build.VERSION.SDK_INT);
                        callbackContext.success(result);
                        
                    } catch (Exception e) {
                        callbackContext.error("Error getting screenshot status: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            callbackContext.error("Error: " + e.getMessage());
        }
    }
    
    /**
     * Secure content - Mencegah screenshot dan screen recording dengan additional security
     */
    public void enableContentProtection(JSONObject args, CallbackContext callbackContext) {
        try {
            boolean enableProtection = args.optBoolean("enable", true);
            boolean hideFromRecents = args.optBoolean("hideFromRecents", false);
            boolean preventViewCapture = args.optBoolean("preventViewCapture", false);
            
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject result = new JSONObject();
                        
                        // 1. Block screenshot dan screen recording
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            if (enableProtection) {
                                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                            } else {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                            }
                            result.put("secureFlag", enableProtection);
                        }
                        
                        // 2. Hide from recent apps (Android 5.0+)
                        // Fitur ini dihapus karena membutuhkan import yang kompleks
                        // dan tidak essential untuk screenshot protection
                        result.put("hideFromRecents", false);
                        
                        // 3. Prevent view capture (Android 11+)
                        if (preventViewCapture && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            window.setDecorFitsSystemWindows(!enableProtection);
                            if (enableProtection) {
                                window.setFlags(
                                    WindowManager.LayoutParams.FLAG_SECURE,
                                    WindowManager.LayoutParams.FLAG_SECURE
                                );
                            }
                            result.put("preventViewCapture", preventViewCapture);
                        } else {
                            result.put("preventViewCapture", false);
                        }
                        
                        result.put("status", "success");
                        result.put("contentProtection", enableProtection);
                        result.put("sdkVersion", Build.VERSION.SDK_INT);
                        callbackContext.success(result);
                        
                    } catch (Exception e) {
                        callbackContext.error("Error enabling content protection: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            callbackContext.error("Error: " + e.getMessage());
        }
    }
    
    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================
    
    /**
     * Build UI options for fullscreen mode
     */
    private int buildFullscreenUiOptions() {
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        
        // Untuk Android KitKat dan di atasnya
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        
        return uiOptions;
    }
    
    private String getScreenSize() {
        int screenLayout = context.getResources().getConfiguration().screenLayout;
        screenLayout &= Configuration.SCREENLAYOUT_SIZE_MASK;
        
        switch (screenLayout) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                return "small";
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                return "normal";
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                return "large";
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                return "xlarge";
            default:
                return "undefined";
        }
    }
    
    private String getCurrentOrientation() {
        int orientation = context.getResources().getConfiguration().orientation;
        
        switch (orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return ORIENTATION_PORTRAIT;
            case Configuration.ORIENTATION_LANDSCAPE:
                return ORIENTATION_LANDSCAPE;
            default:
                return ORIENTATION_UNSPECIFIED;
        }
    }
    
    /**
     * Cleanup resources
     */
    public void destroy() {
        // Clear any flags when destroying
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                // Restore normal UI visibility
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                // Hapus listener
                window.getDecorView().setOnSystemUiVisibilityChangeListener(null);
                window.getDecorView().setOnTouchListener(null);
            }
        });
    }
}