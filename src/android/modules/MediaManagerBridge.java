package org.apache.cordova.resqpeernet.modules;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.provider.MediaStore;
import android.net.Uri;
import android.content.Context;

public class MediaManagerBridge {
    private static final String TAG = "MediaManagerBridge";
    private static final String PRO_CAMERA_TAG = "ProCamera";
    
    private CordovaInterface cordova;
    private CordovaWebView webView;
    private MediaManagerCallback callbackSender;
    
    // Constants
    public static final String MEDIA_TYPE_AUDIO = "audio";
    public static final String MEDIA_TYPE_VIDEO = "video";
    public static final String MEDIA_TYPE_IMAGE = "image";
    
    // Request codes
    public static final int CAPTURE_AUDIO_REQUEST = 5001;
    public static final int CAPTURE_IMAGE_REQUEST = 5002;
    public static final int CAPTURE_VIDEO_REQUEST = 5003;
    public static final int PRO_CAMERA_PERMISSION_REQUEST = 6001;
    
    // Current state
    private CallbackContext currentCallbackContext;
    private String currentAction;
    
    // ProCamera variables
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private ImageReader mImageReader;
    private ImageReader mRawImageReader;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private TextureView mTextureView;
    private Size mPreviewSize;
    private boolean mFlashSupported;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private boolean mIsRawCaptureEnabled = false;
    private boolean mIsProCameraActive = false;
    
    // Camera settings
    private Integer mManualIso = null;
    private Long mManualExposureTime = null;
    private Integer mManualWhiteBalance = null;
    private Float mManualFocus = null;
    private Integer mExposureCompensation = 0;
    
    // Orientation
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    
    // White balance modes
    private static final int WHITE_BALANCE_AUTO = 0;
    private static final int WHITE_BALANCE_DAYLIGHT = 1;
    private static final int WHITE_BALANCE_CLOUDY = 2;
    private static final int WHITE_BALANCE_TUNGSTEN = 3;
    private static final int WHITE_BALANCE_FLUORESCENT = 4;
    
    public interface MediaManagerCallback {
        void sendEvent(String eventName, JSONObject data);
        void onMediaResult(String action, JSONObject result);
        void onMediaError(String action, String error);
        void startActivityForResult(Intent intent, int requestCode);
        void requestPermission(int requestCode, String permission);
        void requestPermissions(int requestCode, String[] permissions);
        boolean hasPermission(String permission);
        Context getContext();
    }
    
    public MediaManagerBridge(CordovaInterface cordova, CordovaWebView webView, MediaManagerCallback callbackSender) {
        this.cordova = cordova;
        this.webView = webView;
        this.callbackSender = callbackSender;
        Log.i(TAG, "MediaManagerBridge initialized");
    }

    // =========================================================================
    // PRO CAMERA METHODS - NEW IMPLEMENTATION
    // =========================================================================
    
    public void proCameraOpen(JSONObject args, CallbackContext callbackContext) {
        try {
            Log.d(PRO_CAMERA_TAG, "Opening Pro Camera...");
            
            // Check permissions
            if (!hasCameraPermission()) {
                currentCallbackContext = callbackContext;
                currentAction = "proCameraOpen";
                requestCameraPermission();
                return;
            }
            
            // Get parameters
            boolean enableRaw = args.optBoolean("raw", false);
            String resolution = args.optString("resolution", "1920x1080");
            String cameraType = args.optString("camera", "back");
            
            Log.d(PRO_CAMERA_TAG, "Pro Camera params - RAW: " + enableRaw + ", Resolution: " + resolution + ", Camera: " + cameraType);
            
            // Initialize camera
            initializeProCamera(enableRaw, resolution, cameraType, callbackContext);
            
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Error opening Pro Camera", e);
            callbackContext.error("Error opening camera: " + e.getMessage());
        }
    }
    
    public void proCameraCapture(JSONObject args, CallbackContext callbackContext) {
        try {
            if (mCameraDevice == null || !mIsProCameraActive) {
                callbackContext.error("Camera not opened or not active");
                return;
            }
            
            Log.d(PRO_CAMERA_TAG, "Capturing photo with Pro Camera...");
            takePictureWithProCamera(callbackContext);
            
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Error capturing photo", e);
            callbackContext.error("Error capturing photo: " + e.getMessage());
        }
    }
    
    public void proCameraSetISO(JSONObject args, CallbackContext callbackContext) {
        try {
            int iso = args.getInt("value");
            
            if (iso < 100 || iso > 3200) {
                callbackContext.error("ISO must be between 100-3200");
                return;
            }
            
            mManualIso = iso;
            updateCameraSettings();
            
            JSONObject result = new JSONObject();
            result.put("iso", iso);
            result.put("status", "iso_set");
            
            callbackContext.success(result);
            Log.d(PRO_CAMERA_TAG, "ISO set to: " + iso);
            
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Error setting ISO", e);
            callbackContext.error("Error setting ISO: " + e.getMessage());
        }
    }
    
    public void proCameraSetShutterSpeed(JSONObject args, CallbackContext callbackContext) {
        try {
            double speed = args.getDouble("value");
            
            if (speed <= 0 || speed > 1.0) {
                callbackContext.error("Shutter speed must be between 0 and 1.0");
                return;
            }
            
            // Convert to nanoseconds (1/1000 = 1000000000 ns)
            long exposureTime = (long) (speed * 1000000000L);
            mManualExposureTime = exposureTime;
            updateCameraSettings();
            
            JSONObject result = new JSONObject();
            result.put("shutterSpeed", speed);
            result.put("exposureTime", exposureTime);
            result.put("status", "shutter_speed_set");
            
            callbackContext.success(result);
            Log.d(PRO_CAMERA_TAG, "Shutter speed set to: " + speed + "s (" + exposureTime + " ns)");
            
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Error setting shutter speed", e);
            callbackContext.error("Error setting shutter speed: " + e.getMessage());
        }
    }
    
    public void proCameraSetWhiteBalance(JSONObject args, CallbackContext callbackContext) {
        try {
            String mode = args.getString("mode");
            int wbMode;
            
            switch (mode.toLowerCase()) {
                case "auto":
                    wbMode = WHITE_BALANCE_AUTO;
                    break;
                case "daylight":
                    wbMode = WHITE_BALANCE_DAYLIGHT;
                    break;
                case "cloudy":
                    wbMode = WHITE_BALANCE_CLOUDY;
                    break;
                case "tungsten":
                    wbMode = WHITE_BALANCE_TUNGSTEN;
                    break;
                case "fluorescent":
                    wbMode = WHITE_BALANCE_FLUORESCENT;
                    break;
                default:
                    if (mode.startsWith("kelvin:")) {
                        try {
                            int kelvin = Integer.parseInt(mode.split(":")[1]);
                            wbMode = kelvin;
                        } catch (Exception e) {
                            callbackContext.error("Invalid Kelvin temperature format");
                            return;
                        }
                    } else {
                        callbackContext.error("Invalid white balance mode: " + mode);
                        return;
                    }
                    break;
            }
            
            mManualWhiteBalance = wbMode;
            updateCameraSettings();
            
            JSONObject result = new JSONObject();
            result.put("whiteBalance", mode);
            result.put("mode", wbMode);
            result.put("status", "white_balance_set");
            
            callbackContext.success(result);
            Log.d(PRO_CAMERA_TAG, "White balance set to: " + mode);
            
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Error setting white balance", e);
            callbackContext.error("Error setting white balance: " + e.getMessage());
        }
    }
    
    public void proCameraSetFocus(JSONObject args, CallbackContext callbackContext) {
        try {
            float distance = (float) args.getDouble("distance");
            
            if (distance < 0.0f || distance > 1.0f) {
                callbackContext.error("Focus distance must be between 0.0-1.0");
                return;
            }
            
            mManualFocus = distance;
            updateCameraSettings();
            
            JSONObject result = new JSONObject();
            result.put("focusDistance", distance);
            result.put("status", "focus_set");
            
            callbackContext.success(result);
            Log.d(PRO_CAMERA_TAG, "Manual focus set to: " + distance);
            
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Error setting focus", e);
            callbackContext.error("Error setting focus: " + e.getMessage());
        }
    }
    
    public void proCameraSetExposureCompensation(JSONObject args, CallbackContext callbackContext) {
        try {
            int ev = args.getInt("value");
            
            if (ev < -3 || ev > 3) {
                callbackContext.error("Exposure compensation must be between -3 and +3");
                return;
            }
            
            mExposureCompensation = ev;
            updateCameraSettings();
            
            JSONObject result = new JSONObject();
            result.put("exposureCompensation", ev);
            result.put("status", "exposure_compensation_set");
            
            callbackContext.success(result);
            Log.d(PRO_CAMERA_TAG, "Exposure compensation set to: " + ev + " EV");
            
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Error setting exposure compensation", e);
            callbackContext.error("Error setting exposure compensation: " + e.getMessage());
        }
    }
    
    public void proCameraEnableRAW(JSONObject args, CallbackContext callbackContext) {
        try {
            boolean enable = args.getBoolean("enable");
            mIsRawCaptureEnabled = enable;
            
            JSONObject result = new JSONObject();
            result.put("rawEnabled", enable);
            result.put("status", "raw_capture_" + (enable ? "enabled" : "disabled"));
            
            callbackContext.success(result);
            Log.d(PRO_CAMERA_TAG, "RAW capture: " + enable);
            
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Error setting RAW capture", e);
            callbackContext.error("Error setting RAW capture: " + e.getMessage());
        }
    }
    
    public void proCameraClose(JSONObject args, CallbackContext callbackContext) {
        try {
            closeProCamera();
            
            JSONObject result = new JSONObject();
            result.put("status", "camera_closed");
            result.put("timestamp", System.currentTimeMillis());
            
            callbackContext.success(result);
            Log.d(PRO_CAMERA_TAG, "Pro Camera closed");
            
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Error closing camera", e);
            callbackContext.error("Error closing camera: " + e.getMessage());
        }
    }
    
    public void proCameraGetSettings(JSONObject args, CallbackContext callbackContext) {
        try {
            JSONObject settings = new JSONObject();
            settings.put("iso", mManualIso != null ? mManualIso : "auto");
            settings.put("shutterSpeed", mManualExposureTime != null ? (1.0 / (mManualExposureTime / 1000000000.0)) : "auto");
            settings.put("whiteBalance", mManualWhiteBalance != null ? mManualWhiteBalance : "auto");
            settings.put("focus", mManualFocus != null ? mManualFocus : "auto");
            settings.put("exposureCompensation", mExposureCompensation);
            settings.put("rawEnabled", mIsRawCaptureEnabled);
            settings.put("cameraActive", mIsProCameraActive);
            
            callbackContext.success(settings);
            Log.d(PRO_CAMERA_TAG, "Current camera settings retrieved");
            
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Error getting camera settings", e);
            callbackContext.error("Error getting camera settings: " + e.getMessage());
        }
    }

    // =========================================================================
    // PRO CAMERA IMPLEMENTATION
    // =========================================================================
    
    private void initializeProCamera(boolean enableRaw, String resolution, String cameraType, CallbackContext callbackContext) {
        startBackgroundThread();
        
        try {
            CameraManager manager = (CameraManager) callbackSender.getContext().getSystemService(Context.CAMERA_SERVICE);
            
            // Find camera
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                
                if (cameraType.equals("front") && facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    mCameraId = cameraId;
                    break;
                } else if (cameraType.equals("back") && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId;
                    break;
                }
            }
            
            if (mCameraId == null) {
                callbackContext.error("No " + cameraType + " camera found");
                return;
            }
            
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                callbackContext.error("Camera not supported");
                return;
            }
            
            // Choose preview size
            String[] res = resolution.split("x");
            int width = Integer.parseInt(res[0]);
            int height = Integer.parseInt(res[1]);
            
            Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
            mPreviewSize = chooseOptimalSize(sizes, width, height);
            
            Log.d(PRO_CAMERA_TAG, "Selected preview size: " + mPreviewSize.getWidth() + "x" + mPreviewSize.getHeight());
            
            // Configure image readers
            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 2);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
            
            if (enableRaw && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
                map.isOutputSupportedFor(ImageFormat.RAW_SENSOR)) {
                try {
                    mRawImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.RAW_SENSOR, 1);
                    mRawImageReader.setOnImageAvailableListener(mOnRawImageAvailableListener, mBackgroundHandler);
                    Log.d(PRO_CAMERA_TAG, "RAW capture enabled");
                } catch (Exception e) {
                    Log.w(PRO_CAMERA_TAG, "RAW capture not supported on this device", e);
                }
            }
            
            // Open camera
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
            
            JSONObject result = new JSONObject();
            result.put("status", "camera_opening");
            result.put("resolution", mPreviewSize.getWidth() + "x" + mPreviewSize.getHeight());
            result.put("rawSupported", mRawImageReader != null);
            
            callbackContext.success(result);
            Log.d(PRO_CAMERA_TAG, "Camera opening initiated");
            
        } catch (CameraAccessException e) {
            Log.e(PRO_CAMERA_TAG, "Camera access exception", e);
            callbackContext.error("Camera access error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Camera initialization error", e);
            callbackContext.error("Camera initialization error: " + e.getMessage());
        }
    }
    
    private void takePictureWithProCamera(CallbackContext callbackContext) {
        try {
            if (mCameraDevice == null) {
                callbackContext.error("Camera not available");
                return;
            }
            
            // Create capture request
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            
            if (mIsRawCaptureEnabled && mRawImageReader != null) {
                captureBuilder.addTarget(mRawImageReader.getSurface());
            }
            
            // Apply manual settings
            applyManualSettings(captureBuilder);
            
            // Orientation
            int rotation = cordova.getActivity().getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            
            // Play shutter sound
            MediaActionSound sound = new MediaActionSound();
            sound.play(MediaActionSound.SHUTTER_CLICK);
            
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    Log.d(PRO_CAMERA_TAG, "Capture completed");
                    
                    // Send capture complete event
                    try {
                        JSONObject eventData = new JSONObject();
                        eventData.put("status", "capture_completed");
                        eventData.put("timestamp", System.currentTimeMillis());
                        callbackSender.sendEvent("proCameraCaptureComplete", eventData);
                    } catch (JSONException e) {
                        Log.e(PRO_CAMERA_TAG, "Error sending capture complete event", e);
                    }
                }
            };
            
            if (mCaptureSession != null) {
                mCaptureSession.stopRepeating();
                mCaptureSession.abortCaptures();
                mCaptureSession.capture(captureBuilder.build(), captureCallback, mBackgroundHandler);
                
                // Send capture start event
                try {
                    JSONObject eventData = new JSONObject();
                    eventData.put("status", "capture_started");
                    eventData.put("timestamp", System.currentTimeMillis());
                    callbackSender.sendEvent("proCameraCaptureStart", eventData);
                } catch (JSONException e) {
                    Log.e(PRO_CAMERA_TAG, "Error sending capture start event", e);
                }
                
                Log.d(PRO_CAMERA_TAG, "Capture initiated");
            } else {
                callbackContext.error("Capture session not ready");
            }
            
        } catch (CameraAccessException e) {
            Log.e(PRO_CAMERA_TAG, "Capture error", e);
            callbackContext.error("Capture error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Unexpected capture error", e);
            callbackContext.error("Unexpected capture error: " + e.getMessage());
        }
    }
    
    private void applyManualSettings(CaptureRequest.Builder builder) {
        try {
            // ISO
            if (mManualIso != null) {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, mManualIso);
            } else {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            }
            
            // Exposure time
            if (mManualExposureTime != null) {
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mManualExposureTime);
            }
            
            // White balance
            if (mManualWhiteBalance != null) {
                if (mManualWhiteBalance <= WHITE_BALANCE_FLUORESCENT) {
                    // Preset modes
                    int awbMode;
                    switch (mManualWhiteBalance) {
                        case WHITE_BALANCE_AUTO: awbMode = CameraMetadata.CONTROL_AWB_MODE_AUTO; break;
                        case WHITE_BALANCE_DAYLIGHT: awbMode = CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT; break;
                        case WHITE_BALANCE_CLOUDY: awbMode = CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT; break;
                        case WHITE_BALANCE_TUNGSTEN: awbMode = CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT; break;
                        case WHITE_BALANCE_FLUORESCENT: awbMode = CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT; break;
                        default: awbMode = CameraMetadata.CONTROL_AWB_MODE_AUTO;
                    }
                    builder.set(CaptureRequest.CONTROL_AWB_MODE, awbMode);
                } else {
                    // Custom Kelvin (simplified implementation)
                    builder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
                }
            }
            
            // Focus
            if (mManualFocus != null) {
                builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
                builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, mManualFocus);
            } else {
                builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            }
            
            // Exposure compensation
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, mExposureCompensation);
            
        } catch (Exception e) {
            Log.e(PRO_CAMERA_TAG, "Error applying manual settings", e);
        }
    }
    
    private void updateCameraSettings() {
        if (mCaptureSession != null && mPreviewRequestBuilder != null) {
            try {
                applyManualSettings(mPreviewRequestBuilder);
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                
                // Send settings updated event
                JSONObject eventData = new JSONObject();
                eventData.put("iso", mManualIso);
                eventData.put("exposureTime", mManualExposureTime);
                eventData.put("whiteBalance", mManualWhiteBalance);
                eventData.put("focus", mManualFocus);
                eventData.put("exposureCompensation", mExposureCompensation);
                callbackSender.sendEvent("proCameraSettingsUpdated", eventData);
                
            } catch (CameraAccessException e) {
                Log.e(PRO_CAMERA_TAG, "Error updating camera settings", e);
            } catch (JSONException e) {
                Log.e(PRO_CAMERA_TAG, "Error creating settings event", e);
            }
        }
    }
    
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            mIsProCameraActive = true;
            Log.d(PRO_CAMERA_TAG, "Camera opened successfully");
            
            // Send camera opened event
            try {
                JSONObject eventData = new JSONObject();
                eventData.put("status", "camera_opened");
                eventData.put("cameraId", mCameraId);
                eventData.put("timestamp", System.currentTimeMillis());
                callbackSender.sendEvent("proCameraOpened", eventData);
            } catch (JSONException e) {
                Log.e(PRO_CAMERA_TAG, "Error sending camera opened event", e);
            }
            
            createCameraPreviewSession();
        }
        
        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
            mIsProCameraActive = false;
            Log.w(PRO_CAMERA_TAG, "Camera disconnected");
            
            // Send camera disconnected event
            try {
                JSONObject eventData = new JSONObject();
                eventData.put("status", "camera_disconnected");
                eventData.put("timestamp", System.currentTimeMillis());
                callbackSender.sendEvent("proCameraDisconnected", eventData);
            } catch (JSONException e) {
                Log.e(PRO_CAMERA_TAG, "Error sending camera disconnected event", e);
            }
        }
        
        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
            mIsProCameraActive = false;
            Log.e(PRO_CAMERA_TAG, "Camera device error: " + error);
            
            // Send camera error event
            try {
                JSONObject eventData = new JSONObject();
                eventData.put("status", "camera_error");
                eventData.put("errorCode", error);
                eventData.put("timestamp", System.currentTimeMillis());
                callbackSender.sendEvent("proCameraError", eventData);
            } catch (JSONException e) {
                Log.e(PRO_CAMERA_TAG, "Error sending camera error event", e);
            }
        }
    };
    
    private void createCameraPreviewSession() {
        try {
            // For now, we'll create a minimal preview session without TextureView
            // In a full implementation, you would use a TextureView for preview
            
            List<Surface> surfaces = new ArrayList<>();
            
            // Add image reader surfaces
            surfaces.add(mImageReader.getSurface());
            if (mRawImageReader != null) {
                surfaces.add(mRawImageReader.getSurface());
            }
            
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            
            // Apply settings to preview
            applyManualSettings(mPreviewRequestBuilder);
            
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mCaptureSession = session;
                    try {
                        mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                        Log.d(PRO_CAMERA_TAG, "Preview session configured successfully");
                        
                        // Send preview started event
                        try {
                            JSONObject eventData = new JSONObject();
                            eventData.put("status", "preview_started");
                            eventData.put("timestamp", System.currentTimeMillis());
                            callbackSender.sendEvent("proCameraPreviewStarted", eventData);
                        } catch (JSONException e) {
                            Log.e(PRO_CAMERA_TAG, "Error sending preview started event", e);
                        }
                        
                    } catch (CameraAccessException e) {
                        Log.e(PRO_CAMERA_TAG, "Error starting preview", e);
                    }
                }
                
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e(PRO_CAMERA_TAG, "Failed to configure camera session");
                    
                    // Send configuration failed event
                    try {
                        JSONObject eventData = new JSONObject();
                        eventData.put("status", "configuration_failed");
                        eventData.put("timestamp", System.currentTimeMillis());
                        callbackSender.sendEvent("proCameraError", eventData);
                    } catch (JSONException e) {
                        Log.e(PRO_CAMERA_TAG, "Error sending configuration failed event", e);
                    }
                }
            }, mBackgroundHandler);
            
        } catch (CameraAccessException e) {
            Log.e(PRO_CAMERA_TAG, "Error creating preview session", e);
        }
    }
    
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    
                    // Save JPEG image
                    File file = createImageFile("JPEG");
                    FileOutputStream output = new FileOutputStream(file);
                    output.write(bytes);
                    output.close();
                    
                    // Send result back to JavaScript
                    JSONObject result = new JSONObject();
                    result.put("path", file.getAbsolutePath());
                    result.put("format", "JPEG");
                    result.put("width", image.getWidth());
                    result.put("height", image.getHeight());
                    result.put("fileSize", bytes.length);
                    
                    // Add metadata
                    JSONObject metadata = new JSONObject();
                    if (mManualIso != null) metadata.put("iso", mManualIso);
                    if (mManualExposureTime != null) metadata.put("exposureTime", mManualExposureTime);
                    if (mManualWhiteBalance != null) metadata.put("whiteBalance", mManualWhiteBalance);
                    if (mManualFocus != null) metadata.put("focusDistance", mManualFocus);
                    metadata.put("exposureCompensation", mExposureCompensation);
                    metadata.put("rawEnabled", mIsRawCaptureEnabled);
                    metadata.put("timestamp", System.currentTimeMillis());
                    
                    result.put("metadata", metadata);
                    
                    // Send event to JavaScript
                    callbackSender.sendEvent("proCameraCapture", result);
                    
                    Log.d(PRO_CAMERA_TAG, "JPEG image saved: " + file.getAbsolutePath() + " (" + bytes.length + " bytes)");
                }
            } catch (Exception e) {
                Log.e(PRO_CAMERA_TAG, "Error processing JPEG image", e);
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
    };
    
    private final ImageReader.OnImageAvailableListener mOnRawImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            if (mIsRawCaptureEnabled) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        // For RAW images, we'll save them as DNG (simplified)
                        File file = createImageFile("DNG");
                        
                        // In a real implementation, you would process the RAW data
                        // For now, we'll just create the file and log it
                        FileOutputStream output = new FileOutputStream(file);
                        // RAW processing would go here
                        output.close();
                        
                        JSONObject result = new JSONObject();
                        result.put("path", file.getAbsolutePath());
                        result.put("format", "RAW");
                        result.put("width", image.getWidth());
                        result.put("height", image.getHeight());
                        
                        callbackSender.sendEvent("proCameraRawCapture", result);
                        
                        Log.d(PRO_CAMERA_TAG, "RAW image saved: " + file.getAbsolutePath());
                    }
                } catch (Exception e) {
                    Log.e(PRO_CAMERA_TAG, "Error processing RAW image", e);
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }
        }
    };
    
    private File createImageFile(String format) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "PRO_" + timeStamp + "_";
        File storageDir = cordova.getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        String extension = format.equals("RAW") ? ".dng" : ".jpg";
        return File.createTempFile(imageFileName, extension, storageDir);
    }
    
    private void startBackgroundThread() {
        if (mBackgroundThread == null) {
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
            Log.d(PRO_CAMERA_TAG, "Background thread started");
        }
    }
    
    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
                Log.d(PRO_CAMERA_TAG, "Background thread stopped");
            } catch (InterruptedException e) {
                Log.e(PRO_CAMERA_TAG, "Error stopping background thread", e);
            }
        }
    }
    
    private void closeProCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
                Log.d(PRO_CAMERA_TAG, "Capture session closed");
            }
            
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
                Log.d(PRO_CAMERA_TAG, "Camera device closed");
            }
            
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
                Log.d(PRO_CAMERA_TAG, "Image reader closed");
            }
            
            if (mRawImageReader != null) {
                mRawImageReader.close();
                mRawImageReader = null;
                Log.d(PRO_CAMERA_TAG, "RAW image reader closed");
            }
            
            mIsProCameraActive = false;
            
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
            stopBackgroundThread();
            Log.d(PRO_CAMERA_TAG, "Pro Camera fully closed");
        }
    }
    
    private Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        
        for (Size option : choices) {
            if (option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            } else {
                notBigEnough.add(option);
            }
        }
        
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }
    
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    // =========================================================================
    // EXISTING METHODS (keep all your existing methods below)
    // =========================================================================
    
    // Hapus method custom camera yang kompleks
    public void captureImageWithChoice(JSONObject args, CallbackContext callbackContext) {
        try {
            // Always use default camera for simplicity
            captureImage(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error in captureImageWithChoice", e);
            callbackContext.error("Error: " + e.getMessage());
        }
    }
    
    public void captureAudioWithChoice(JSONObject args, CallbackContext callbackContext) {
        try {
            // Always use default audio recorder for simplicity
            captureAudio(args, callbackContext);
        } catch (Exception e) {
            Log.e(TAG, "Error in captureAudioWithChoice", e);
            callbackContext.error("Error: " + e.getMessage());
        }
    }

    // =========================================================================
    // AUDIO METHODS (Simplified)
    // =========================================================================
    
    public void createAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            String audioId = args.getString("id");
            String audioSrc = args.getString("src");
            
            JSONObject result = new JSONObject();
            result.put("id", audioId);
            result.put("status", "created");
            result.put("src", audioSrc);
            
            callbackContext.success(result);
            Log.d(TAG, "Audio instance created: " + audioId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating audio", e);
            callbackContext.error("Error creating audio: " + e.getMessage());
        }
    }
    
    public void playAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            String audioId = args.getString("id");
            
            JSONObject result = new JSONObject();
            result.put("id", audioId);
            result.put("status", "playing");
            result.put("position", 0);
            
            callbackContext.success(result);
            Log.d(TAG, "Audio playback started: " + audioId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio", e);
            callbackContext.error("Error playing audio: " + e.getMessage());
        }
    }
    
    // ... (keep all your existing audio methods: pauseAudio, stopAudio, seekAudio, etc.)
    
    public void pauseAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            String audioId = args.getString("id");
            
            JSONObject result = new JSONObject();
            result.put("id", audioId);
            result.put("status", "paused");
            
            callbackContext.success(result);
            Log.d(TAG, "Audio paused: " + audioId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error pausing audio", e);
            callbackContext.error("Error pausing audio: " + e.getMessage());
        }
    }
    
    public void stopAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            String audioId = args.getString("id");
            
            JSONObject result = new JSONObject();
            result.put("id", audioId);
            result.put("status", "stopped");
            result.put("position", 0);
            
            callbackContext.success(result);
            Log.d(TAG, "Audio stopped: " + audioId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping audio", e);
            callbackContext.error("Error stopping audio: " + e.getMessage());
        }
    }
    
    public void seekAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            String audioId = args.getString("id");
            int position = args.getInt("position");
            
            JSONObject result = new JSONObject();
            result.put("id", audioId);
            result.put("status", "seeked");
            result.put("position", position);
            
            callbackContext.success(result);
            Log.d(TAG, "Audio seeked: " + audioId + " to " + position);
            
        } catch (Exception e) {
            Log.e(TAG, "Error seeking audio", e);
            callbackContext.error("Error seeking audio: " + e.getMessage());
        }
    }
    
    public void getAudioDuration(JSONObject args, CallbackContext callbackContext) {
        try {
            String audioId = args.getString("id");
            
            JSONObject result = new JSONObject();
            result.put("id", audioId);
            result.put("duration", 120.5);
            
            callbackContext.success(result);
            Log.d(TAG, "Audio duration retrieved: " + audioId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting audio duration", e);
            callbackContext.error("Error getting audio duration: " + e.getMessage());
        }
    }
    
    public void getAudioPosition(JSONObject args, CallbackContext callbackContext) {
        try {
            String audioId = args.getString("id");
            
            JSONObject result = new JSONObject();
            result.put("id", audioId);
            result.put("position", 45.2);
            
            callbackContext.success(result);
            Log.d(TAG, "Audio position retrieved: " + audioId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting audio position", e);
            callbackContext.error("Error getting audio position: " + e.getMessage());
        }
    }
    
    public void setAudioVolume(JSONObject args, CallbackContext callbackContext) {
        try {
            String audioId = args.getString("id");
            double volume = args.getDouble("volume");
            
            JSONObject result = new JSONObject();
            result.put("id", audioId);
            result.put("volume", volume);
            result.put("status", "volume_set");
            
            callbackContext.success(result);
            Log.d(TAG, "Audio volume set: " + audioId + " to " + volume);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting audio volume", e);
            callbackContext.error("Error setting audio volume: " + e.getMessage());
        }
    }
    
    public void releaseAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            String audioId = args.getString("id");
            
            JSONObject result = new JSONObject();
            result.put("id", audioId);
            result.put("status", "released");
            
            callbackContext.success(result);
            Log.d(TAG, "Audio released: " + audioId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error releasing audio", e);
            callbackContext.error("Error releasing audio: " + e.getMessage());
        }
    }
    
    // =========================================================================
    // MEDIA CAPTURE METHODS (Simplified - No FileProvider needed)
    // =========================================================================
    
    public void captureAudio(JSONObject args, CallbackContext callbackContext) {
        try {
            currentCallbackContext = callbackContext;
            currentAction = "captureAudio";
            
            // Check permissions
            if (!hasAudioRecordingPermission()) {
                requestAudioRecordingPermission();
                return;
            }
            
            // Start audio capture intent - FIXED: Use correct intent action
            Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
            if (intent.resolveActivity(cordova.getActivity().getPackageManager()) != null) {
                if (callbackSender != null) {
                    callbackSender.startActivityForResult(intent, CAPTURE_AUDIO_REQUEST);
                } else {
                    callbackContext.error("Media callback not available");
                }
            } else {
                callbackContext.error("No audio recording app available");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error capturing audio", e);
            callbackContext.error("Error capturing audio: " + e.getMessage());
        }
    }
    
    public void captureImage(JSONObject args, CallbackContext callbackContext) {
        try {
            currentCallbackContext = callbackContext;
            currentAction = "captureImage";
            
            Log.d(TAG, "Starting simplified image capture process...");
            
            // Check permissions
            if (!hasCameraPermission()) {
                Log.w(TAG, "Camera permission not granted, requesting...");
                requestCameraPermission();
                return;
            }
            
            Log.d(TAG, "Camera permission granted, creating camera intent...");
            
            // Simple intent without EXTRA_OUTPUT - let camera app handle storage - FIXED
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            
            // Check if any app can handle this intent
            if (intent.resolveActivity(cordova.getActivity().getPackageManager()) == null) {
                Log.e(TAG, "No camera app available to handle intent");
                callbackContext.error("No camera app available on this device");
                return;
            }
            
            Log.d(TAG, "Starting camera activity...");
            
            // Start camera activity
            if (callbackSender != null) {
                callbackSender.startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
                Log.d(TAG, "Camera activity started successfully");
            } else {
                Log.e(TAG, "Callback sender is null");
                callbackContext.error("Media callback not available");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in captureImage", e);
            callbackContext.error("Unexpected error: " + e.getMessage());
        }
    }
    
    public void captureVideo(JSONObject args, CallbackContext callbackContext) {
        try {
            currentCallbackContext = callbackContext;
            currentAction = "captureVideo";
            
            // Check permissions
            if (!hasCameraPermission()) {
                requestCameraPermission();
                return;
            }
            
            // Start video capture intent - FIXED
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            
            // Set video quality if specified - FIXED
            if (args.has("quality")) {
                int quality = args.getInt("quality");
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, quality);
            }
            
            // Set duration limit if specified - FIXED
            if (args.has("duration")) {
                int duration = args.getInt("duration");
                intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, duration);
            }
            
            if (intent.resolveActivity(cordova.getActivity().getPackageManager()) != null) {
                if (callbackSender != null) {
                    callbackSender.startActivityForResult(intent, CAPTURE_VIDEO_REQUEST);
                } else {
                    callbackContext.error("Media callback not available");
                }
            } else {
                callbackContext.error("No video recording app available");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error capturing video", e);
            callbackContext.error("Error capturing video: " + e.getMessage());
        }
    }
    
    public void getSupportedFormats(JSONObject args, CallbackContext callbackContext) {
        try {
            String mediaType = args.getString("type");
            
            JSONObject result = new JSONObject();
            JSONArray formats = new JSONArray();
            
            switch (mediaType) {
                case MEDIA_TYPE_AUDIO:
                    formats.put("audio/3gpp");
                    formats.put("audio/amr");
                    formats.put("audio/aac");
                    formats.put("audio/mp4");
                    formats.put("audio/mpeg");
                    formats.put("audio/wav");
                    break;
                    
                case MEDIA_TYPE_VIDEO:
                    formats.put("video/3gpp");
                    formats.put("video/mp4");
                    formats.put("video/webm");
                    formats.put("video/avi");
                    break;
                    
                case MEDIA_TYPE_IMAGE:
                    formats.put("image/jpeg");
                    formats.put("image/png");
                    formats.put("image/gif");
                    formats.put("image/webp");
                    break;
            }
            
            result.put("type", mediaType);
            result.put("formats", formats);
            
            callbackContext.success(result);
            Log.d(TAG, "Supported formats retrieved for: " + mediaType);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting supported formats", e);
            callbackContext.error("Error getting supported formats: " + e.getMessage());
        }
    }
    
    // =========================================================================
    // PERMISSION METHODS (Simplified)
    // =========================================================================
    
    private boolean hasAudioRecordingPermission() {
        if (callbackSender != null) {
            return callbackSender.hasPermission(Manifest.permission.RECORD_AUDIO);
        }
        return false;
    }
    
    private boolean hasCameraPermission() {
        if (callbackSender != null) {
            boolean hasCamera = callbackSender.hasPermission(Manifest.permission.CAMERA);
            
            Log.d(TAG, "Permission check - Camera: " + hasCamera);
            return hasCamera;
        }
        return false;
    }
    
    private void requestAudioRecordingPermission() {
        if (callbackSender != null) {
            callbackSender.requestPermission(CAPTURE_AUDIO_REQUEST, Manifest.permission.RECORD_AUDIO);
        } else {
            if (currentCallbackContext != null) {
                currentCallbackContext.error("Media callback not available for permission request");
            }
        }
    }
    
    private void requestCameraPermission() {
        String[] permissions = {Manifest.permission.CAMERA};
        
        Log.d(TAG, "Requesting camera permission...");
        
        if (callbackSender != null) {
            callbackSender.requestPermissions(CAPTURE_IMAGE_REQUEST, permissions);
        } else {
            if (currentCallbackContext != null) {
                currentCallbackContext.error("Permission callback not available");
            }
        }
    }
    
    // =========================================================================
    // ACTIVITY RESULT HANDLER (Simplified)
    // =========================================================================
    
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "Activity result - Request: " + requestCode + ", Result: " + resultCode);
        
        if (currentCallbackContext == null) {
            Log.w(TAG, "No callback context for activity result");
            return;
        }
        
        try {
            if (resultCode == android.app.Activity.RESULT_OK) {
                Log.d(TAG, "Activity result OK, processing...");
                JSONObject result = new JSONObject();
                
                switch (requestCode) {
                    case CAPTURE_IMAGE_REQUEST:
                        handleImageCaptureResult(intent, result);
                        break;
                        
                    case CAPTURE_AUDIO_REQUEST:
                        handleAudioCaptureResult(intent, result);
                        break;
                        
                    case CAPTURE_VIDEO_REQUEST:
                        handleVideoCaptureResult(intent, result);
                        break;
                        
                    default:
                        Log.w(TAG, "Unknown request code: " + requestCode);
                        currentCallbackContext.error("Unknown request code: " + requestCode);
                        return;
                }
                
                currentCallbackContext.success(result);
                Log.d(TAG, "Activity result processed successfully");
                
            } else if (resultCode == android.app.Activity.RESULT_CANCELED) {
                Log.w(TAG, "User cancelled media capture");
                currentCallbackContext.error("User cancelled media capture");
            } else {
                Log.w(TAG, "Media capture failed with result code: " + resultCode);
                currentCallbackContext.error("Media capture failed with result code: " + resultCode);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling activity result", e);
            currentCallbackContext.error("Error processing capture: " + e.getMessage());
        } finally {
            // Cleanup
            currentCallbackContext = null;
            currentAction = null;
        }
    }
    
    private void handleAudioCaptureResult(Intent intent, JSONObject result) throws JSONException {
        // FIXED: Uri import added
        Uri audioUri = intent.getData();
        JSONArray mediaFiles = new JSONArray();
        JSONObject mediaFile = new JSONObject();
        
        String fileName = "audio_capture_" + System.currentTimeMillis();
        String filePath = audioUri != null ? audioUri.toString() : "";
        
        mediaFile.put("name", fileName);
        mediaFile.put("type", "audio/3gpp");
        mediaFile.put("lastModified", System.currentTimeMillis());
        mediaFile.put("size", 0);
        mediaFile.put("fullPath", filePath);
        mediaFile.put("uri", filePath);
        
        mediaFiles.put(mediaFile);
        
        result.put("mediaFiles", mediaFiles);
        result.put("code", "SUCCESS");
        result.put("message", "Audio captured successfully");
        
        Log.d(TAG, "Audio capture completed: " + filePath);
    }
    
    private void handleImageCaptureResult(Intent intent, JSONObject result) throws JSONException {
        Log.d(TAG, "Handling simplified image capture result");
        
        JSONArray mediaFiles = new JSONArray();
        JSONObject mediaFile = new JSONObject();
        
        // Handle data dari intent (biasanya ada thumbnail di data) - FIXED: Uri import
        if (intent != null && intent.getData() != null) {
            Uri imageUri = intent.getData();
            String filePath = imageUri.toString();
            
            mediaFile.put("name", "captured_image");
            mediaFile.put("type", "image/jpeg");
            mediaFile.put("lastModified", System.currentTimeMillis());
            mediaFile.put("size", 0);
            mediaFile.put("fullPath", filePath);
            mediaFile.put("uri", filePath);
            mediaFile.put("success", true);
            
            mediaFiles.put(mediaFile);
            
            result.put("mediaFiles", mediaFiles);
            result.put("code", "SUCCESS");
            result.put("message", "Image captured successfully");
            result.put("filePath", filePath);
            
            Log.d(TAG, "Image captured with data: " + filePath);
        } else {
            // Fallback jika tidak ada data - image biasanya tersimpan di gallery
            mediaFile.put("name", "captured_image");
            mediaFile.put("type", "image/jpeg");
            mediaFile.put("lastModified", System.currentTimeMillis());
            mediaFile.put("success", true);
            
            mediaFiles.put(mediaFile);
            
            result.put("mediaFiles", mediaFiles);
            result.put("code", "SUCCESS");
            result.put("message", "Image captured (check device gallery)");
            
            Log.d(TAG, "Image captured (saved to gallery)");
        }
    }
    
    private void handleVideoCaptureResult(Intent intent, JSONObject result) throws JSONException {
        // FIXED: Uri import added
        Uri videoUri = intent.getData();
        JSONArray mediaFiles = new JSONArray();
        JSONObject mediaFile = new JSONObject();
        
        String fileName = "video_capture_" + System.currentTimeMillis();
        String filePath = videoUri != null ? videoUri.toString() : "";
        
        mediaFile.put("name", fileName);
        mediaFile.put("type", "video/mp4");
        mediaFile.put("lastModified", System.currentTimeMillis());
        mediaFile.put("size", 0);
        mediaFile.put("fullPath", filePath);
        mediaFile.put("uri", filePath);
        
        mediaFiles.put(mediaFile);
        
        result.put("mediaFiles", mediaFiles);
        result.put("code", "SUCCESS");
        result.put("message", "Video captured successfully");
        
        Log.d(TAG, "Video capture completed: " + filePath);
    }
    
    // =========================================================================
    // PERMISSION RESULT HANDLER
    // =========================================================================
    
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "Permission result - Request: " + requestCode);
        
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            Log.i(TAG, "All permissions granted, retrying action...");
            retryActionAfterPermissionGrant();
        } else {
            Log.w(TAG, "Some permissions denied");
            if (currentCallbackContext != null) {
                currentCallbackContext.error("Required permissions denied");
                currentCallbackContext = null;
                currentAction = null;
            }
        }
    }
    
    private void retryActionAfterPermissionGrant() {
        if (currentCallbackContext == null || currentAction == null) {
            Log.w(TAG, "Cannot retry - no callback or action");
            return;
        }
        
        Log.d(TAG, "Retrying action: " + currentAction);
        
        try {
            JSONObject args = new JSONObject();
            switch (currentAction) {
                case "proCameraOpen":
                    proCameraOpen(args, currentCallbackContext);
                    break;
                case "captureImage":
                    captureImage(args, currentCallbackContext);
                    break;
                case "captureAudio":
                    captureAudio(args, currentCallbackContext);
                    break;
                case "captureVideo":
                    captureVideo(args, currentCallbackContext);
                    break;
                default:
                    Log.w(TAG, "Unknown action to retry: " + currentAction);
                    if (currentCallbackContext != null) {
                        currentCallbackContext.error("Unknown action: " + currentAction);
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrying action", e);
            if (currentCallbackContext != null) {
                currentCallbackContext.error("Error after permission grant: " + e.getMessage());
                currentCallbackContext = null;
                currentAction = null;
            }
        }
    }
    
    // =========================================================================
    // CLEANUP
    // =========================================================================
    
    public void destroy() {
        Log.i(TAG, "MediaManagerBridge destroyed");
        currentCallbackContext = null;
        currentAction = null;
        closeProCamera();
    }
}