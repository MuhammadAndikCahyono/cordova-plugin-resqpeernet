package org.apache.cordova.resqpeernet.modules;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LocationManagerBridge {
    private static final String TAG = "LocationManagerBridge";
    
    // Permission constants
    public static final String[] LOCATION_PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };
    
    // Location request constants
    private static final long MIN_TIME_MS = 1000; // 1 second
    private static final float MIN_DISTANCE_M = 1.0f; // 1 meter
    
    private Context context;
    private CordovaInterface cordova;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private CallbackContext locationCallbackContext;
    private boolean isTracking = false;
    private String currentProvider = null;
    
    // Callback interface for events
    public interface LocationCallback {
        void onLocationUpdate(JSONObject locationData);
        void onLocationError(String error);
        void onProviderStatusChanged(String provider, boolean enabled);
    }
    
    private LocationCallback locationCallback;

    public LocationManagerBridge(Context context, CordovaInterface cordova) {
        this.context = context;
        this.cordova = cordova;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        initializeLocationListener();
    }

    public void setLocationCallback(LocationCallback callback) {
        this.locationCallback = callback;
    }

    /**
     * Initialize the location listener
     */
    private void initializeLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
                sendLocationUpdate(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                String statusText = "UNKNOWN";
                switch (status) {
                    case LocationProvider.AVAILABLE:
                        statusText = "AVAILABLE";
                        break;
                    case LocationProvider.OUT_OF_SERVICE:
                        statusText = "OUT_OF_SERVICE";
                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        statusText = "TEMPORARILY_UNAVAILABLE";
                        break;
                }
                
                Log.d(TAG, "Location provider status changed: " + provider + " - " + statusText);
                
                if (locationCallback != null) {
                    locationCallback.onProviderStatusChanged(provider, status == LocationProvider.AVAILABLE);
                }
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(TAG, "Location provider enabled: " + provider);
                
                if (locationCallback != null) {
                    locationCallback.onProviderStatusChanged(provider, true);
                }
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(TAG, "Location provider disabled: " + provider);
                
                if (locationCallback != null) {
                    locationCallback.onProviderStatusChanged(provider, false);
                }
            }
        };
    }

    /**
     * Get current location with available providers
     */
    public void getCurrentLocation(JSONObject args, CallbackContext callbackContext) {
        try {
            boolean useLastKnown = args.optBoolean("useLastKnown", false);
            long timeout = args.optLong("timeout", 30000); // 30 seconds default
            float desiredAccuracy = (float) args.optDouble("desiredAccuracy", 50.0); // meters
            
            Log.d(TAG, "Getting current location, useLastKnown: " + useLastKnown + ", timeout: " + timeout);
            
            // Check location permissions first
            if (!hasLocationPermissions()) {
                callbackContext.error("Location permissions not granted");
                return;
            }
            
            // Try to get last known location first if requested
            if (useLastKnown) {
                Location lastLocation = getBestLastKnownLocation();
                if (lastLocation != null && isLocationAccurateEnough(lastLocation, desiredAccuracy)) {
                    JSONObject locationData = locationToJSON(lastLocation, "last_known");
                    callbackContext.success(locationData);
                    return;
                }
            }
            
            // Start temporary tracking for fresh location
            startTemporaryLocationTracking(callbackContext, timeout, desiredAccuracy);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting current location", e);
            callbackContext.error("Error getting location: " + e.getMessage());
        }
    }

    /**
     * Start continuous location tracking
     */
    public void startLocationTracking(JSONObject args, CallbackContext callbackContext) {
        try {
            if (isTracking) {
                callbackContext.error("Location tracking already active");
                return;
            }
            
            // Check location permissions first
            if (!hasLocationPermissions()) {
                callbackContext.error("Location permissions not granted");
                return;
            }
            
            long updateInterval = args.optLong("updateInterval", MIN_TIME_MS);
            float minDistance = (float) args.optDouble("minDistance", MIN_DISTANCE_M);
            String provider = args.optString("provider", "best"); // best, gps, network
            
            Log.d(TAG, "Starting location tracking, interval: " + updateInterval + ", distance: " + minDistance);
            
            this.locationCallbackContext = callbackContext;
            startLocationUpdates(provider, updateInterval, minDistance);
            isTracking = true;
            
            // Send initial success result
            JSONObject result = new JSONObject();
            result.put("status", "tracking_started");
            result.put("provider", currentProvider);
            result.put("interval", updateInterval);
            callbackContext.success(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting location tracking", e);
            callbackContext.error("Error starting location tracking: " + e.getMessage());
        }
    }

    /**
     * Stop location tracking
     */
    public void stopLocationTracking(CallbackContext callbackContext) {
        try {
            if (!isTracking) {
                callbackContext.error("Location tracking not active");
                return;
            }
            
            stopLocationUpdates();
            isTracking = false;
            
            JSONObject result = new JSONObject();
            result.put("status", "tracking_stopped");
            callbackContext.success(result);
            
            Log.d(TAG, "Location tracking stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping location tracking", e);
            callbackContext.error("Error stopping location tracking: " + e.getMessage());
        }
    }

    /**
     * Get location permissions status
     */
    public void getLocationPermissions(CallbackContext callbackContext) {
        try {
            JSONObject permissions = new JSONObject();
            
            // Check fine location permission - FIXED: Use cordova.hasPermission()
            boolean fineLocation = cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            boolean coarseLocation = cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            
            permissions.put("fineLocation", fineLocation);
            permissions.put("coarseLocation", coarseLocation);
            permissions.put("hasAnyLocation", fineLocation || coarseLocation);
            permissions.put("hasFineLocation", fineLocation);
            
            // Check location services enabled
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            
            permissions.put("gpsEnabled", gpsEnabled);
            permissions.put("networkEnabled", networkEnabled);
            permissions.put("locationServicesEnabled", gpsEnabled || networkEnabled);
            
            callbackContext.success(permissions);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting location permissions", e);
            callbackContext.error("Error getting location permissions: " + e.getMessage());
        }
    }

    /**
     * Calculate distance between two coordinates
     */
    public void calculateDistance(JSONObject args, CallbackContext callbackContext) {
        try {
            double lat1 = args.getDouble("lat1");
            double lon1 = args.getDouble("lon1");
            double lat2 = args.getDouble("lat2");
            double lon2 = args.getDouble("lon2");
            
            float[] results = new float[1];
            Location.distanceBetween(lat1, lon1, lat2, lon2, results);
            
            float distance = results[0];
            
            JSONObject result = new JSONObject();
            result.put("distance", distance);
            result.put("units", "meters");
            result.put("point1", createPointJSON(lat1, lon1));
            result.put("point2", createPointJSON(lat2, lon2));
            
            callbackContext.success(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance", e);
            callbackContext.error("Error calculating distance: " + e.getMessage());
        }
    }

    /**
     * Get available location providers
     */
    public void getAvailableProviders(CallbackContext callbackContext) {
        try {
            List<String> providers = locationManager.getProviders(true);
            JSONArray providersArray = new JSONArray();
            
            for (String provider : providers) {
                JSONObject providerInfo = new JSONObject();
                providerInfo.put("name", provider);
                providerInfo.put("enabled", locationManager.isProviderEnabled(provider));
                
                // Get provider requirements
                if (provider.equals(LocationManager.GPS_PROVIDER)) {
                    providerInfo.put("requires", "GPS hardware");
                    providerInfo.put("accuracy", "high");
                } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                    providerInfo.put("requires", "Network connection");
                    providerInfo.put("accuracy", "medium");
                } else if (provider.equals(LocationManager.PASSIVE_PROVIDER)) {
                    providerInfo.put("requires", "Other apps requesting location");
                    providerInfo.put("accuracy", "variable");
                } else {
                    providerInfo.put("requires", "unknown");
                    providerInfo.put("accuracy", "unknown");
                }
                
                providersArray.put(providerInfo);
            }
            
            JSONObject result = new JSONObject();
            result.put("providers", providersArray);
            result.put("count", providers.size());
            
            callbackContext.success(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting available providers", e);
            callbackContext.error("Error getting available providers: " + e.getMessage());
        }
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    /**
     * Check if location permissions are granted - FIXED METHOD
     */
    private boolean hasLocationPermissions() {
        return cordova.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
               cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private void startLocationUpdates(String providerPreference, long updateInterval, float minDistance) {
        try {
            stopLocationUpdates(); // Stop any existing updates
            
            String provider = determineBestProvider(providerPreference);
            if (provider == null) {
                throw new Exception("No location provider available");
            }
            
            locationManager.requestLocationUpdates(
                provider,
                updateInterval,
                minDistance,
                locationListener,
                Looper.getMainLooper()
            );
            
            currentProvider = provider;
            Log.d(TAG, "Location updates started with provider: " + provider);
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception - location permission required", e);
            if (locationCallback != null) {
                locationCallback.onLocationError("Location permission denied");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates", e);
            if (locationCallback != null) {
                locationCallback.onLocationError("Error starting location updates: " + e.getMessage());
            }
        }
    }

    private void stopLocationUpdates() {
        try {
            if (locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
            currentProvider = null;
            Log.d(TAG, "Location updates stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping location updates", e);
        }
    }

    private void startTemporaryLocationTracking(final CallbackContext callbackContext, final long timeout, final float desiredAccuracy) {
        try {
            final String tempProvider = determineBestProvider("best");
            if (tempProvider == null) {
                callbackContext.error("No location provider available");
                return;
            }
            
            final LocationListener tempListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.d(TAG, "Temporary location received: " + location.getAccuracy() + "m accuracy");
                    
                    if (isLocationAccurateEnough(location, desiredAccuracy)) {
                        locationManager.removeUpdates(this);
                        try {
                            JSONObject locationData = locationToJSON(location, tempProvider);
                            callbackContext.success(locationData);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error converting location to JSON", e);
                            callbackContext.error("Error processing location data");
                        }
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {
                    locationManager.removeUpdates(this);
                    callbackContext.error("Location provider disabled: " + provider);
                }
            };
            
            // Start listening for location updates
            locationManager.requestLocationUpdates(
                tempProvider,
                MIN_TIME_MS,
                MIN_DISTANCE_M,
                tempListener,
                Looper.getMainLooper()
            );
            
            // Set timeout handler
            new android.os.Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    locationManager.removeUpdates(tempListener);
                    
                    // Try last known location as fallback
                    Location lastLocation = getBestLastKnownLocation();
                    if (lastLocation != null) {
                        try {
                            JSONObject locationData = locationToJSON(lastLocation, "last_known_timeout");
                            callbackContext.success(locationData);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error converting last known location to JSON", e);
                            callbackContext.error("Error processing location data");
                        }
                    } else {
                        callbackContext.error("Location timeout - no location received");
                    }
                }
            }, timeout);
            
        } catch (SecurityException e) {
            callbackContext.error("Location permission denied");
        } catch (Exception e) {
            callbackContext.error("Error getting location: " + e.getMessage());
        }
    }

    private String determineBestProvider(String preference) {
        if ("gps".equals(preference) && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        
        if ("network".equals(preference) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        
        // Auto-detect best provider
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        
        if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            return LocationManager.PASSIVE_PROVIDER;
        }
        
        return null;
    }

    private Location getBestLastKnownLocation() {
        try {
            Location bestLocation = null;
            List<String> providers = locationManager.getProviders(true);
            
            for (String provider : providers) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location != null && (bestLocation == null || 
                    location.getAccuracy() < bestLocation.getAccuracy())) {
                    bestLocation = location;
                }
            }
            
            return bestLocation;
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting last known location", e);
            return null;
        }
    }

    private boolean isLocationAccurateEnough(Location location, float desiredAccuracy) {
        return location.hasAccuracy() && location.getAccuracy() <= desiredAccuracy;
    }
	/*
    private void sendLocationUpdate(Location location) {
        try {
            JSONObject locationData = locationToJSON(location, currentProvider);
            
            // Send to persistent callback context if tracking
            if (locationCallbackContext != null) {
                PluginResult result = new PluginResult(PluginResult.Status.OK, locationData);
                result.setKeepCallback(true);
                locationCallbackContext.sendPluginResult(result);
            }
            
            // Send to event callback
            if (locationCallback != null) {
                locationCallback.onLocationUpdate(locationData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending location update", e);
        }
    }
	*/
	private void sendLocationUpdate(Location location) {
		try {
			JSONObject locationData = locationToJSON(location, currentProvider);
			
			// ? Pastikan ini terpanggil
			Log.d(TAG, "Sending location update to JavaScript: " + locationData.toString());
			
			// Send to persistent callback context if tracking
			if (locationCallbackContext != null) {
				PluginResult result = new PluginResult(PluginResult.Status.OK, locationData);
				result.setKeepCallback(true);
				locationCallbackContext.sendPluginResult(result);
				Log.d(TAG, "Sent to callback context");
			}
			
			// Send to event callback - INI YANG PENTING!
			if (locationCallback != null) {
				locationCallback.onLocationUpdate(locationData);
				Log.d(TAG, "Sent to event callback");
			} else {
				Log.w(TAG, "Location callback is null - events won't work!");
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Error sending location update", e);
		}
	}
	

    private JSONObject locationToJSON(Location location, String provider) throws JSONException {
        JSONObject locationData = new JSONObject();
        
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("provider", provider);
        locationData.put("timestamp", location.getTime());
        
        if (location.hasAccuracy()) {
            locationData.put("accuracy", location.getAccuracy());
        }
        
        if (location.hasAltitude()) {
            locationData.put("altitude", location.getAltitude());
        }
        
        if (location.hasBearing()) {
            locationData.put("bearing", location.getBearing());
        }
        
        if (location.hasSpeed()) {
            locationData.put("speed", location.getSpeed());
        }
        
        locationData.put("timeFormatted", new java.util.Date(location.getTime()).toString());
        
        return locationData;
    }

    private JSONObject createPointJSON(double lat, double lon) throws JSONException {
        JSONObject point = new JSONObject();
        point.put("latitude", lat);
        point.put("longitude", lon);
        return point;
    }

    /**
     * Clean up resources
     */
    public void destroy() {
        stopLocationUpdates();
        locationCallbackContext = null;
        locationCallback = null;
        Log.i(TAG, "LocationManagerBridge destroyed");
    }
}