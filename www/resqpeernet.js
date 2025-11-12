/**
 * ResqPeerNet Cordova Plugin JavaScript Bridge
 * Version: 1.0.0 - Enhanced Event System
 */

var ResqPeerNet = {
    // Plugin initialization
    init: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'initialize',
            []
        );
    },

    // Basic plugin methods
    getPluginInfo: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getPluginInfo',
            []
        );
    },

    checkStatus: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'checkStatus',
            []
        );
    },

    testConnection: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'testConnection',
            []
        );
    },

    getSystemInfo: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getSystemInfo',
            []
        );
    },

    // Permission management methods
    getPermissionStatus: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getPermissionStatus',
            []
        );
    },
	
	/**
	 * Request single permission
	 */
	requestPermission: function(args, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'requestPermission',
			[args]
		);
	},

    requestPermissions: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'requestPermissions',
            [args]
        );
    },
	
    hasStoragePermission: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'hasStoragePermission',
            []
        );
    },

    hasNetworkPermissions: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'hasNetworkPermissions',
            []
        );
    },
	/*
	* File manager Bridge
	**/
	readFileAsText: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'readFileAsText',
            [args]
        );
    },
	
	getStorageInfo: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getStorageInfo',
            []
        );
    },
	
	searchFiles: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'searchFiles',
            [args]
        );
    },
	
	getFileInfo: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getFileInfo',
            [args]
        );
    },
    // Wallpaper manager methods
    getWallpaper: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getWallpaper',
            [args]
        );
    },

    getWallpaperInfo: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getWallpaperInfo',
            [args]
        );
    },
	
    setWallpaperFromFile: function(args, successCallback, errorCallback) {
	
	    // Validasi basic
		if (!args.filePath) {
			errorCallback("filePath is required");
			return;
		}
		
		// Set default value jika tidak provided
		if (!args.wallpaperType) {
			args.wallpaperType = 1; // Default to HOME
		}
		
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'setWallpaperFromFile',
            [args]
        );
    },	

    setWallpaperFromUri: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'setWallpaperFromUri',
            [args]
        );
    },	
	
    listenWallpaperChanged: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'listenWallpaperChanged',
            [args]
        );
    },

    stopListeningWallpaper: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'stopListeningWallpaper',
            []
        );
    },
	
	getNetworkStatus: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getNetworkStatus',
            []
        );
    },

    getWifiStatus: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getWifiStatus',
            []
        );
    },

    getMobileStatus: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getMobileStatus',
            []
        );
    },

    getBluetoothStatus: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getBluetoothStatus',
            []
        );
    },

    enableBluetooth: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'enableBluetooth',
            []
        );
    },

    getPairedDevices: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getPairedDevices',
            []
        );
    },

    startBluetoothDiscovery: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'startBluetoothDiscovery',
            []
        );
    },

    stopBluetoothDiscovery: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'stopBluetoothDiscovery',
            []
        );
    },

    connectToBluetoothDevice: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'connectToBluetoothDevice',
            [args]
        );
    },

    startBluetoothServer: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'startBluetoothServer',
            []
        );
    },

    stopBluetoothServer: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'stopBluetoothServer',
            []
        );
    },

    sendMessage: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'sendMessage',
            [args]
        );
    },

    broadcastMessage: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'broadcastMessage',
            [args]
        );
    },

    getConnectedDevices: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getConnectedDevices',
            []
        );
    },

    disconnectDevice: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'disconnectDevice',
            [args]
        );
    },

    disconnectAllDevices: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'disconnectAllDevices',
            []
        );
    },

    startDiscoveryListener: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'startDiscoveryListener',
            []
        );
    },

    startConnectionListener: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'startConnectionListener',
            []
        );
    },

    startMessageListener: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'startMessageListener',
            []
        );
    },
	
	// MESH NETWORK METHODS
    initializeMesh: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'initializeMesh',
            []
        );
    },

    joinMesh: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'joinMesh',
            [args]
        );
    },

    sendMeshMessage: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'sendMeshMessage',
            [args]
        );
    },

    broadcastToMesh: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'broadcastToMesh',
            [args]
        );
    },

    getMeshTopology: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getMeshTopology',
            []
        );
    },

    startMeshEventListener: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'startMeshEventListener',
            []
        );
    },
	
	discoverMeshNodes: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'discoverMeshNodes',
            []
        );
    },

    autoJoinMesh: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'autoJoinMesh',
            []
        );
    },

    getAvailableGateways: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getAvailableGateways',
            []
        );
    },
	
	getDeviceInfo: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getDeviceInfo',
            []
        );
    },
	
	// Battery Methods
	getBatteryStatus: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'getBatteryStatus',
			[]
		);
	},

	startBatteryListener: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'startBatteryListener',
			[]
		);
	},

	stopBatteryListener: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'stopBatteryListener',
			[]
		);
	},

    // =========================================================================
    // MEDIA METHODS - NEW: Audio Playback and Media Capture
    // =========================================================================
	
	loadCameraUI: function() {
		return new Promise(function(resolve, reject) {
			// Implementasi loading camera UI HTML
			const xhr = new XMLHttpRequest();
			xhr.open('GET', 'camera-ui.html', true);
			xhr.onreadystatechange = function() {
				if (xhr.readyState === 4) {
					if (xhr.status === 200) {
						resolve(xhr.responseText);
					} else {
						resolve(this.getEmbeddedCameraUI());
					}
				}
			}.bind(this);
			xhr.send();
		});
	},

	getEmbeddedCameraUI: function() {
		return `
			<div class="pro-camera-ui">
				<!-- Embedded camera UI HTML -->
			</div>
		`;
	},
	
	captureImageWithChoice: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'captureImageWithChoice',
			[options]
		);
	},
	
	captureAudioWithChoice: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'captureAudioWithChoice',
			[options]
		);
	},
	
	openCustomCamera: function(options, successCallback, errorCallback) {
		this.loadCameraUI()
			.then(function(html) {
				// Inject dan initialize custom camera
				const container = document.getElementById('resqpeernet-camera-container') || 
								this.createUIContainer('camera');
				container.innerHTML = html;
				
				setTimeout(() => {
					if (typeof ProfessionalCamera !== 'undefined') {
						window.resqpeernetCamera = new ProfessionalCamera(options);
						if (successCallback) successCallback({ status: 'custom_camera_loaded' });
					} else {
						throw new Error('ProfessionalCamera class not found');
					}
				}, 100);
			}.bind(this))
			.catch(function(error) {
				if (errorCallback) errorCallback('Failed to load custom camera: ' + error.message);
			});
	},
	
	openCustomAudioRecorder: function(options, successCallback, errorCallback) {
		this.loadAudioRecorderUI()
			.then(function(html) {
				// Inject dan initialize audio recorder
				const container = document.getElementById('resqpeernet-audio-container') || 
								this.createUIContainer('audio');
				container.innerHTML = html;
				
				setTimeout(() => {
					if (typeof AudioRecorderUI !== 'undefined') {
						window.resqpeernetAudioRecorder = new AudioRecorderUI(options);
						if (successCallback) successCallback({ status: 'custom_audio_loaded' });
					} else {
						throw new Error('AudioRecorderUI class not found');
					}
				}, 100);
			}.bind(this))
			.catch(function(error) {
				if (errorCallback) errorCallback('Failed to load audio recorder: ' + error.message);
			});
	},
	
	loadAudioRecorderUI: function() {
		return new Promise(function(resolve, reject) {
			// Similar implementation to loadCameraUI
			// Try different methods to load the HTML
			const xhr = new XMLHttpRequest();
			xhr.open('GET', 'audio-recorder-ui.html', true);
			xhr.onreadystatechange = function() {
				if (xhr.readyState === 4) {
					if (xhr.status === 200) {
						resolve(xhr.responseText);
					} else {
						// Embedded fallback
						resolve(this.getEmbeddedAudioRecorderUI());
					}
				}
			}.bind(this);
			xhr.send();
		});
	},
	
	getEmbeddedAudioRecorderUI: function() {
		return `
			<div class="pro-audio-recorder-ui">
				<!-- Embedded audio recorder UI HTML -->
			</div>
		`;
	},
	
	createUIContainer: function(type) {
		const container = document.createElement('div');
		container.id = `resqpeernet-${type}-container`;
		container.style.cssText = `
			position: fixed;
			top: 0;
			left: 0;
			width: 100%;
			height: 100%;
			z-index: 9999;
			background: #000;
		`;
		document.body.appendChild(container);
		return container;
	},
	
	closeCustomUI: function(successCallback, errorCallback) {
		// Remove all custom UI containers
		const containers = document.querySelectorAll('[id^="resqpeernet-"]');
		containers.forEach(container => container.remove());
		
		// Cleanup instances
		if (window.resqpeernetCamera) {
			window.resqpeernetCamera.destroy();
			window.resqpeernetCamera = null;
		}
		
		if (window.resqpeernetAudioRecorder) {
			window.resqpeernetAudioRecorder.destroy();
			window.resqpeernetAudioRecorder = null;
		}
		
		if (successCallback) successCallback({ status: 'custom_ui_closed' });
	},

    // Audio Playback Methods (from cordova-plugin-media)
    createAudio: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'createAudio',
            [args]
        );
    },

    playAudio: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'playAudio',
            [args]
        );
    },

    pauseAudio: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'pauseAudio',
            [args]
        );
    },

    stopAudio: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'stopAudio',
            [args]
        );
    },

    seekAudio: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'seekAudio',
            [args]
        );
    },

    getAudioDuration: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getAudioDuration',
            [args]
        );
    },

    getAudioPosition: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getAudioPosition',
            [args]
        );
    },

    setAudioVolume: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'setAudioVolume',
            [args]
        );
    },

    releaseAudio: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'releaseAudio',
            [args]
        );
    },

    captureAudio: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'captureAudio',
            [args]
        );
    },

    captureImage: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'captureImage',
            [args]
        );
    },

    captureVideo: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'captureVideo',
            [args]
        );
    },

    getSupportedFormats: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getSupportedFormats',
            [args]
        );
    },

    // =========================================================================
    // LOCATION METHODS - NEW: Location Services
    // =========================================================================

    // Location Services Methods
    getCurrentLocation: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getCurrentLocation',
            [args]
        );
    },

    startLocationTracking: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'startLocationTracking',
            [args]
        );
    },

    stopLocationTracking: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'stopLocationTracking',
            []
        );
    },

    getLocationPermissions: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getLocationPermissions',
            []
        );
    },

    calculateDistance: function(args, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'calculateDistance',
            [args]
        );
    },

    getAvailableProviders: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getAvailableProviders',
            []
        );
    },

    /**
     * Start auto-mesh network (One-click setup)
     * @param {function} successCallback - Success callback
     * @param {function} errorCallback - Error callback
     */
    startAutoMesh: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback, 
            errorCallback, 
            'ResqPeerNet', 
            'startAutoMesh', 
            []
        );
    },

    /**
     * Stop auto-mesh network
     * @param {function} successCallback - Success callback
     * @param {function} errorCallback - Error callback
     */
    stopAutoMesh: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback, 
            errorCallback, 
            'ResqPeerNet', 
            'stopAutoMesh', 
            []
        );
    },

    /**
     * Broadcast message to entire auto-mesh network
     * @param {Object} options - Message options
     * @param {function} successCallback - Success callback
     * @param {function} errorCallback - Error callback
     */
    broadcastToAutoMesh: function(options, successCallback, errorCallback) {
        cordova.exec(
            successCallback, 
            errorCallback, 
            'ResqPeerNet', 
            'broadcastToAutoMesh', 
            [options]
        );
    },

    /**
     * Get auto-mesh network status
     * @param {function} successCallback - Success callback
     * @param {function} errorCallback - Error callback
     */
    getAutoMeshStatus: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback, 
            errorCallback, 
            'ResqPeerNet', 
            'getAutoMeshStatus', 
            []
        );
    },
	
	/**
	 * START HYBRID MESH - Full Auto Connect
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	startHybridMesh: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'startHybridMesh', []);
	},

	/**
	 * Send message to all connected devices in hybrid mesh
	 * @param {Object} options - Message options
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	sendHybridMessage: function(options, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'sendHybridMessage', [options]);
	},

	/**
	 * Get hybrid mesh status
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	getHybridStatus: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'getHybridStatus', []);
	},

	/**
	 * Stop hybrid mesh
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	stopHybridMesh: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'stopHybridMesh', []);
	},
	
	// =========================================================================
    // WiFi Mesh event
    // =========================================================================
	
	initializeWiFiMesh: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'initializeWiFiMesh', []);
	},
	
	createWiFiMeshGroup: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'createWiFiMeshGroup', []);
	},
	
	discoverWiFiPeers: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'discoverWiFiPeers', []);
	},
	
	connectToWiFiDevice: function(options, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'connectToWiFiDevice', [options]);
	},
	
	getWiFiPeers: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'getWiFiPeers', []);
	},
	
	sendWiFiMeshMessage: function(options, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'sendWiFiMeshMessage', [options]);
	},
	
	getWiFiMeshTopology: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'getWiFiMeshTopology', []);
	},
	
	removeWiFiMeshGroup: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'removeWiFiMeshGroup', []);
	},
	
	// =========================================================================
    // Local Network Mesh event Implementations
    // =========================================================================
	
	initializeLocalMesh: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'initializeLocalMesh', []);
	},
	
	discoverLocalPeers: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'discoverLocalPeers', []);
	},
	
	getLocalPeers: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'getLocalPeers', []);
	},	
	
	autoConnectLocalPeers: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'autoConnectLocalPeers', []);
	},	
	
	sendLocalMeshMessage: function(options, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'sendLocalMeshMessage', [options]);
	},	
	
	getLocalMeshTopology: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'getLocalMeshTopology', []);
	},	
	
	stopLocalMesh: function(successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, 'ResqPeerNet', 'stopLocalMesh', []);
	},
	
    // =========================================================================
    // EVENT SYSTEM - ENHANCED WITH ALL METHOD EVENTS
    // =========================================================================

    // Event listeners storage
    _eventListeners: {},

    /**
     * Add event listener for ResqPeerNet events
     * @param {string} eventName - Event name to listen for
     * @param {function} callback - Callback function
     */
    on: function(eventName, callback) {
        if (!this._eventListeners[eventName]) {
            this._eventListeners[eventName] = [];
        }
        this._eventListeners[eventName].push(callback);
        console.log('Event listener added for:', eventName);
    },

    /**
     * Remove event listener
     * @param {string} eventName - Event name
     * @param {function} callback - Callback function to remove
     */
    off: function(eventName, callback) {
        if (this._eventListeners[eventName]) {
            if (callback) {
                var index = this._eventListeners[eventName].indexOf(callback);
                if (index > -1) {
                    this._eventListeners[eventName].splice(index, 1);
                }
            } else {
                delete this._eventListeners[eventName];
            }
        }
    },

    /**
     * Trigger event with data
     * @param {string} eventName - Event name
     * @param {object} data - Event data
     */
    trigger: function(eventName, data) {
        // Trigger custom event on document
        var event = new CustomEvent('resqpeernet-' + eventName, { 
            detail: data,
            bubbles: true,
            cancelable: true
        });
        document.dispatchEvent(event);

        // Call registered callbacks
        if (this._eventListeners[eventName]) {
            this._eventListeners[eventName].forEach(function(callback) {
                try {
                    callback(data);
                } catch (error) {
                    console.error('Error in event callback for', eventName, error);
                }
            });
        }

        // Also call global onEvent if defined
        if (typeof this.onEvent === 'function') {
            try {
                this.onEvent(eventName, data);
            } catch (error) {
                console.error('Error in global onEvent callback:', error);
            }
        }
    },

    /**
     * Enhanced method wrapper that triggers events
     */
    _wrapMethodWithEvents: function(methodName, originalMethod) {
        return function() {
            var args = Array.prototype.slice.call(arguments);
            var successCallback = args[args.length - 2];
            var errorCallback = args[args.length - 1];
            
            // Create enhanced callbacks that trigger events
            var enhancedSuccess = function(result) {
                // Trigger success event
                ResqPeerNet.trigger(methodName + 'Success', {
                    method: methodName,
                    result: result,
                    timestamp: new Date().toISOString()
                });
                
                ResqPeerNet.trigger(methodName + 'Complete', {
                    method: methodName,
                    status: 'success',
                    result: result,
                    timestamp: new Date().toISOString()
                });
                
                if (successCallback) {
                    successCallback(result);
                }
            };
            
            var enhancedError = function(error) {
                // Trigger error event
                ResqPeerNet.trigger(methodName + 'Error', {
                    method: methodName,
                    error: error,
                    timestamp: new Date().toISOString()
                });
                
                ResqPeerNet.trigger(methodName + 'Complete', {
                    method: methodName,
                    status: 'error',
                    error: error,
                    timestamp: new Date().toISOString()
                });
                
                if (errorCallback) {
                    errorCallback(error);
                }
            };
            
            // Replace callbacks in args
            if (args.length >= 2) {
                args[args.length - 2] = enhancedSuccess;
                args[args.length - 1] = enhancedError;
            }
            
            // Trigger start event
            ResqPeerNet.trigger(methodName + 'Start', {
                method: methodName,
                args: args.slice(0, -2), // Exclude callbacks
                timestamp: new Date().toISOString()
            });
            
            // Call original method
            return originalMethod.apply(this, args);
        };
    },

    /**
     * Get comprehensive system resources information
     * Includes: Memory, CPU, Storage, Thermal status
     * @param {function} successCallback - Callback for successful response
     * @param {function} errorCallback - Callback for error response
     */
    getSystemResources: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getSystemResources',
            []
        );
    },

    /**
     * Get list of all available sensors on the device
     * @param {function} successCallback - Callback for successful response
     * @param {function} errorCallback - Callback for error response
     */
    getAvailableSensors: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getAvailableSensors',
            []
        );
    },

     /**
     * Get detailed information about specific sensor types
     * @param {function} successCallback - Callback for successful response
     * @param {function} errorCallback - Callback for error response
     */
    getSensorCapabilities: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getSensorCapabilities',
            []
        );
    },

    /**
     * Run comprehensive device health diagnostics
     * @param {function} successCallback - Callback for successful response
     * @param {function} errorCallback - Callback for error response
     */
    runDeviceDiagnostics: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'runDeviceDiagnostics',
            []
        );
    },

	// =========================================================================
    // EXISTING UTILITY METHODS FOR DEVICE INFORMATION
    // =========================================================================
    
    // Utility methods for device information
    getDeviceSummary: function(successCallback, errorCallback) {
        var self = this;
        // Get real device info first, then create summary
        this.getDeviceInfo(function(deviceInfo) {
            // Safe data access dengan fallback values
            var summary = {
                platform: deviceInfo.platform || 'Unknown',
                model: deviceInfo.model || 'Unknown',
                manufacturer: deviceInfo.manufacturer || 'Unknown',
                version: deviceInfo.version || 'Unknown',
                sdkVersion: deviceInfo.sdkVersion || 0,
                isTablet: deviceInfo.isTablet || false,
                isVirtual: deviceInfo.isVirtual || false,
                screen: deviceInfo.screen || { width: 0, height: 0, density: 0 },
                hardware: {
                    cpuArch: (deviceInfo.hardware && deviceInfo.hardware.cpuArch) || 'Unknown',
                    numCores: (deviceInfo.hardware && deviceInfo.hardware.numCores) || 0,
                    ramTotalMB: (deviceInfo.hardware && deviceInfo.hardware.ramTotalMB) || 0,
                    storageTotalMB: (deviceInfo.hardware && deviceInfo.hardware.storageTotalMB) || 0,
                    batteryLevel: (deviceInfo.hardware && deviceInfo.hardware.batteryLevel) || 0
                },
                network: deviceInfo.network || { type: 'unknown', isConnected: false },
                language: deviceInfo.language || 'Unknown',
                region: deviceInfo.region || 'Unknown'
            };
            successCallback(summary);
        }, errorCallback);
    },

    // Check device capabilities - IMPROVED
    checkDeviceCapabilities: function(successCallback, errorCallback) {
        var self = this;
        this.getDeviceInfo(function(deviceInfo) {
            // Safe data access dengan fallback values
            var networkType = (deviceInfo.network && deviceInfo.network.type) || 'unknown';
            var sdkVersion = deviceInfo.sdkVersion || 0;
            var numCores = (deviceInfo.hardware && deviceInfo.hardware.numCores) || 0;
            var ramTotalMB = (deviceInfo.hardware && deviceInfo.hardware.ramTotalMB) || 0;
            var screenDensity = (deviceInfo.screen && deviceInfo.screen.density) || 0;
            var storageTotalMB = (deviceInfo.hardware && deviceInfo.hardware.storageTotalMB) || 0;
            
            var capabilities = {
                hasBluetooth: true, // Assuming true since we have Bluetooth modules
                hasWifi: true,
                hasCellular: networkType === 'cellular',
                isTablet: deviceInfo.isTablet || false,
                isVirtual: deviceInfo.isVirtual || false,
                sdkVersion: sdkVersion,
                supportsMesh: sdkVersion >= 21, // Android 5.0+ for basic BLE mesh
                supportsNFC: sdkVersion >= 16, // Android 4.1+ for NFC
                supportsBiometric: sdkVersion >= 23, // Android 6.0+ for fingerprint
                supportsDarkMode: sdkVersion >= 29, // Android 10+ for dark mode
                highPerformance: numCores >= 4 && ramTotalMB >= 2000,
                largeStorage: storageTotalMB >= 32000, // 32GB or more
                highResScreen: screenDensity >= 2.5,
                multiCore: numCores > 1,
                hasGPS: true, // Most Android devices have GPS
                hasAccelerometer: true,
                hasGyroscope: sdkVersion >= 9, // Android 2.3+
                isEmulator: deviceInfo.isVirtual || false
            };
            successCallback(capabilities);
        }, errorCallback);
    },

    /**
     * Get comprehensive system health report including all Phase 1 features
     * @param {function} successCallback - Callback for successful response
     * @param {function} errorCallback - Callback for error response
     */
    getComprehensiveSystemReport: function(successCallback, errorCallback) {
        console.log('🔄 Generating comprehensive system report...');
        var self = this;
        
        // Convert callbacks to promises for easier handling
        function promiseWrapper(method) {
            return new Promise(function(resolve, reject) {
                method.call(self, resolve, reject);
            });
        }
        
        // Collect data from multiple sources
        Promise.all([
            promiseWrapper(self.getSystemResources),
            promiseWrapper(self.getDeviceInfo),
            promiseWrapper(self.getBatteryStatus),
            promiseWrapper(self.getNetworkStatus),
            promiseWrapper(self.runDeviceDiagnostics)
        ]).then(function(results) {
            var report = {
                timestamp: new Date().toISOString(),
                systemResources: results[0],
                deviceInfo: results[1],
                batteryStatus: results[2],
                networkStatus: results[3],
                healthDiagnostics: results[4],
                reportVersion: '2.1.0'
            };
            if (successCallback) successCallback(report);
        }).catch(function(error) {
            if (errorCallback) errorCallback('Error generating comprehensive report: ' + error);
        });
    },

    /**
     * Start real-time system monitoring with all Phase 1 features
     * @param {function} updateCallback - Callback for each monitoring update
     * @param {number} interval - Monitoring interval in milliseconds (default: 5000)
     * @return {number} monitorId - ID that can be used to stop monitoring
     */
    startSystemMonitoring: function(updateCallback, interval) {
        if (!updateCallback) {
            console.error('Update callback is required for system monitoring');
            return null;
        }

        var monitorInterval = interval || 5000; // Default 5 seconds
        var monitorId = null;
        
        console.log('🚀 Starting comprehensive system monitoring with interval: ' + monitorInterval + 'ms');
        
        monitorId = setInterval(function() {
            ResqPeerNet.getSystemResources(
                function(systemData) {
                    // Combine with other real-time data
                    var monitoringData = {
                        timestamp: new Date().toISOString(),
                        systemResources: systemData,
                        isMonitoring: true
                    };
                    
                    updateCallback(monitoringData);
                },
                function(error) {
                    console.error('System monitoring error:', error);
                    updateCallback({
                        timestamp: new Date().toISOString(),
                        error: error,
                        isMonitoring: false
                    });
                }
            );
        }, monitorInterval);
        
        return monitorId;
    },

    /**
     * Stop real-time system monitoring
     * @param {number} monitorId - ID returned by startSystemMonitoring
     */
    stopSystemMonitoring: function(monitorId) {
        if (monitorId) {
            clearInterval(monitorId);
            console.log('🛑 System monitoring stopped');
        } else {
            console.warn('No active system monitoring to stop');
        }
    },

    /**
     * Get sensor information with detailed capabilities
     * @param {function} successCallback - Callback for successful response
     * @param {function} errorCallback - Callback for error response
     */
    getDetailedSensorInfo: function(successCallback, errorCallback) {
        console.log('📡 Getting detailed sensor information...');
        var self = this;
        
        // Convert callbacks to promises
        function promiseWrapper(method) {
            return new Promise(function(resolve, reject) {
                method.call(self, resolve, reject);
            });
        }
        
        Promise.all([
            promiseWrapper(self.getAvailableSensors),
            promiseWrapper(self.getSensorCapabilities)
        ]).then(function(results) {
            var sensorInfo = {
                availableSensors: results[0],
                capabilities: results[1],
                totalSensors: results[0].sensors ? results[0].sensors.length : 0,
                timestamp: new Date().toISOString()
            };
            if (successCallback) successCallback(sensorInfo);
        }).catch(function(error) {
            if (errorCallback) errorCallback('Error getting sensor information: ' + error);
        });
    },

    /**
     * Quick health check - lightweight version of diagnostics
     * @param {function} successCallback - Callback for successful response
     * @param {function} errorCallback - Callback for error response
     */
    quickHealthCheck: function(successCallback, errorCallback) {
        console.log('🏥 Performing quick health check...');
        var self = this;
        
        // Convert callbacks to promises
        function promiseWrapper(method) {
            return new Promise(function(resolve, reject) {
                method.call(self, resolve, reject);
            });
        }
        
        Promise.all([
            promiseWrapper(self.getBatteryStatus),
            promiseWrapper(self.getNetworkStatus),
            promiseWrapper(self.getSystemResources)
        ]).then(function(results) {
            var battery = results[0];
            var network = results[1];
            var system = results[2];
            
            var healthReport = {
                timestamp: new Date().toISOString(),
                battery: {
                    level: battery.level,
                    isCharging: battery.isCharging,
                    health: battery.health,
                    status: battery.level > 20 ? 'good' : 'low'
                },
                network: {
                    type: network.type,
                    isConnected: network.isConnected,
                    status: network.isConnected ? 'connected' : 'disconnected'
                },
                memory: {
                    usagePercentage: system.memory ? system.memory.usagePercentage : 0,
                    status: system.memory && system.memory.usagePercentage < 80 ? 'good' : 'high'
                },
                overallStatus: 'healthy' // Simplified assessment
            };
            
            if (successCallback) successCallback(healthReport);
        }).catch(function(error) {
            if (errorCallback) errorCallback('Error performing quick health check: ' + error);
        });
    },

    // Test methods for device information
    testGetDeviceInfo: function(successCallback, errorCallback) {
        console.log('Getting Device info...', 'info');
        
        this.getDeviceInfo(
            function(hasil) {
                console.log('Device Info: ' + JSON.stringify(hasil, null, 2), 'success');
                if (successCallback) successCallback(hasil);
            },
            function(error) {
                console.log('Error getting Device info: ' + JSON.stringify(error), 'error');
                if (errorCallback) errorCallback(error);
            }
        );
    },
    
    testGetDeviceSummary: function(successCallback, errorCallback) {
        console.log('Getting Device summary...', 'info');
        
        this.getDeviceSummary(
            function(hasil) {
                console.log('Device Summary: ' + JSON.stringify(hasil, null, 2), 'success');
                if (successCallback) successCallback(hasil);
            },
            function(error) {
                console.log('Error getting Device summary: ' + JSON.stringify(error), 'error');
                if (errorCallback) errorCallback(error);
            }
        );
    },

    testGetDeviceCapabilities: function(successCallback, errorCallback) {
        console.log('Getting Device capabilities...', 'info');
        
        this.checkDeviceCapabilities(
            function(hasil) {
                console.log('Device Capabilities: ' + JSON.stringify(hasil, null, 2), 'success');
                if (successCallback) successCallback(hasil);
            },
            function(error) {
                console.log('Error getting Device capabilities: ' + JSON.stringify(error), 'error');
                if (errorCallback) errorCallback(error);
            }
        );
    },

    /**
     * Test Phase 1 system resources functionality
     */
    testSystemResources: function(successCallback, errorCallback) {
        console.log('🧪 Testing System Resources...', 'info');
        
        this.getSystemResources(
            function(hasil) {
                console.log('✅ System Resources: ' + JSON.stringify(hasil, null, 2), 'success');
                if (successCallback) successCallback(hasil);
            },
            function(error) {
                console.log('❌ Error getting System Resources: ' + JSON.stringify(error), 'error');
                if (errorCallback) errorCallback(error);
            }
        );
    },

    /**
     * Test Phase 1 sensor capabilities functionality
     */
    testSensorCapabilities: function(successCallback, errorCallback) {
        console.log('🧪 Testing Sensor Capabilities...', 'info');
        
        this.getSensorCapabilities(
            function(hasil) {
                console.log('✅ Sensor Capabilities: ' + JSON.stringify(hasil, null, 2), 'success');
                if (successCallback) successCallback(hasil);
            },
            function(error) {
                console.log('❌ Error getting Sensor Capabilities: ' + JSON.stringify(error), 'error');
                if (errorCallback) errorCallback(error);
            }
        );
    },

    /**
     * Test Phase 1 device diagnostics functionality
     */
    testDeviceDiagnostics: function(successCallback, errorCallback) {
        console.log('🧪 Testing Device Diagnostics...', 'info');
        
        this.runDeviceDiagnostics(
            function(hasil) {
                console.log('✅ Device Diagnostics: ' + JSON.stringify(hasil, null, 2), 'success');
                if (successCallback) successCallback(hasil);
            },
            function(error) {
                console.log('❌ Error running Device Diagnostics: ' + JSON.stringify(error), 'error');
                if (errorCallback) errorCallback(error);
            }
        );
    },

    /**
     * Comprehensive test of all Phase 1 features
     */
    testAllPhase1Features: function() {
        console.log('🚀=== TESTING ALL PHASE 1 FEATURES ===', 'info');
        
        this.testSystemResources();
        setTimeout(function() {
            ResqPeerNet.testSensorCapabilities();
        }, 1000);
        setTimeout(function() {
            ResqPeerNet.testDeviceDiagnostics();
        }, 2000);
        setTimeout(function() {
            ResqPeerNet.getDetailedSensorInfo(
                function(result) {
                    console.log('✅ Detailed Sensor Info: ' + JSON.stringify(result, null, 2), 'success');
                },
                function(error) {
                    console.log('❌ Error getting detailed sensor info: ' + error, 'error');
                }
            );
        }, 3000);
    },

    // Comprehensive test all
    testAllDeviceFunctions: function() {
        console.log('🚀=== TESTING ALL DEVICE FUNCTIONS (Including Phase 1) ===', 'info');
        
        this.testGetDeviceInfo();
        setTimeout(function() {
            ResqPeerNet.testGetDeviceSummary();
        }, 1000);
        setTimeout(function() {
            ResqPeerNet.testGetDeviceCapabilities();
        }, 2000);
        setTimeout(function() {
            ResqPeerNet.testAllPhase1Features();
        }, 3000);
    },
    
    // =========================================================================
    // MEDIA UTILITY METHODS
    // =========================================================================

    /**
     * Quick audio player utility
     */
    createQuickAudioPlayer: function(src, successCallback, errorCallback) {
        var audioId = 'quick_audio_' + Date.now();
        var args = {
            id: audioId,
            src: src
        };
        
        this.createAudio(args, 
            function(result) {
                if (successCallback) successCallback({
                    id: audioId,
                    player: {
                        play: function() {
                            ResqPeerNet.playAudio({id: audioId}, successCallback, errorCallback);
                        },
                        pause: function() {
                            ResqPeerNet.pauseAudio({id: audioId}, successCallback, errorCallback);
                        },
                        stop: function() {
                            ResqPeerNet.stopAudio({id: audioId}, successCallback, errorCallback);
                        },
                        seek: function(position) {
                            ResqPeerNet.seekAudio({id: audioId, position: position}, successCallback, errorCallback);
                        },
                        setVolume: function(volume) {
                            ResqPeerNet.setAudioVolume({id: audioId, volume: volume}, successCallback, errorCallback);
                        },
                        getDuration: function() {
                            ResqPeerNet.getAudioDuration({id: audioId}, successCallback, errorCallback);
                        },
                        getPosition: function() {
                            ResqPeerNet.getAudioPosition({id: audioId}, successCallback, errorCallback);
                        },
                        release: function() {
                            ResqPeerNet.releaseAudio({id: audioId}, successCallback, errorCallback);
                        }
                    }
                });
            },
            errorCallback
        );
    },

    /**
     * Quick media capture utility
     */
    quickCaptureImage: function(options, successCallback, errorCallback) {
        var defaultOptions = {
            quality: 100,
            destinationType: 0, // DATA_URL
            sourceType: 1, // CAMERA
            encodingType: 0, // JPEG
            mediaType: 0, // PICTURE
            allowEdit: false,
            correctOrientation: true,
            saveToPhotoAlbum: false
        };
        
        var captureOptions = Object.assign({}, defaultOptions, options || {});
        
        this.captureImage(captureOptions, successCallback, errorCallback);
    },

    quickCaptureAudio: function(options, successCallback, errorCallback) {
        var defaultOptions = {
            duration: 60, // 60 seconds max
            quality: 1, // High quality
            channels: 2, // Stereo
            sampleRate: 44100, // CD quality
            bitRate: 128000 // 128 kbps
        };
        
        var captureOptions = Object.assign({}, defaultOptions, options || {});
        
        this.captureAudio(captureOptions, successCallback, errorCallback);
    },

    quickCaptureVideo: function(options, successCallback, errorCallback) {
        var defaultOptions = {
            duration: 300, // 5 minutes max
            quality: 1, // High quality
            bitRate: 1000000 // 1 Mbps
        };
        
        var captureOptions = Object.assign({}, defaultOptions, options || {});
        
        this.captureVideo(captureOptions, successCallback, errorCallback);
    },

    /**
     * Check media permissions
     */
    hasMediaPermissions: function(successCallback, errorCallback) {
        this.getPermissionStatus(function(status) {
            var hasMedia = status.media && status.media.allGranted;
            if (successCallback) successCallback(hasMedia);
        }, errorCallback);
    },

    /**
     * Request media permissions
     */
    requestMediaPermissions: function(successCallback, errorCallback) {
        this.requestPermissions({type: 'MEDIA'}, successCallback, errorCallback);
    },

    /**
     * Test media functionality
     */
    testMediaFunctionality: function(successCallback, errorCallback) {
        console.log('Testing Media Functionality...');
        var self = this;
        
        var testResults = {
            audioPlayback: false,
            audioCapture: false,
            imageCapture: false,
            videoCapture: false,
            formatsSupported: false
        };
        
        // Test supported formats
        this.getSupportedFormats({type: 'audio'}, 
            function(formats) {
                testResults.formatsSupported = true;
                console.log('Audio formats supported:', formats);
                
                // Test audio creation
                ResqPeerNet.createAudio({id: 'test_audio', src: 'test.mp3'}, 
                    function(result) {
                        testResults.audioPlayback = true;
                        console.log('Audio playback test passed');
                        
                        // Return final results
                        if (successCallback) successCallback(testResults);
                    },
                    function(error) {
                        console.log('Audio playback test failed:', error);
                        if (successCallback) successCallback(testResults);
                    }
                );
            },
            function(error) {
                console.log('Formats test failed:', error);
                if (successCallback) successCallback(testResults);
            }
        );
    },
	
	// =========================================================================
	// PRO CAMERA METHODS
	// =========================================================================

	/**
	 * Open Pro Camera with advanced controls
	 * @param {object} options - Camera options {resolution, raw, camera}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	proCameraOpen: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'proCameraOpen',
			[options]
		);
	},

	/**
	 * Capture photo with Pro Camera
	 * @param {object} options - Capture options
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	proCameraCapture: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'proCameraCapture',
			[options]
		);
	},

	/**
	 * Set manual ISO value
	 * @param {object} options - {value: 100-3200}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	proCameraSetISO: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'proCameraSetISO',
			[options]
		);
	},

	/**
	 * Set manual shutter speed
	 * @param {object} options - {value: 0.0-1.0 (seconds)}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	proCameraSetShutterSpeed: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'proCameraSetShutterSpeed',
			[options]
		);
	},

	/**
	 * Set white balance mode
	 * @param {object} options - {mode: 'auto'|'daylight'|'cloudy'|'tungsten'|'fluorescent'|'kelvin:5000'}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	proCameraSetWhiteBalance: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'proCameraSetWhiteBalance',
			[options]
		);
	},

	/**
	 * Set manual focus distance
	 * @param {object} options - {distance: 0.0-1.0}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	proCameraSetFocus: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'proCameraSetFocus',
			[options]
		);
	},

	/**
	 * Set exposure compensation
	 * @param {object} options - {value: -3 to +3}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	proCameraSetExposureCompensation: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'proCameraSetExposureCompensation',
			[options]
		);
	},

	/**
	 * Enable/disable RAW capture
	 * @param {object} options - {enable: true|false}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	proCameraEnableRAW: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'proCameraEnableRAW',
			[options]
		);
	},

	/**
	 * Close Pro Camera
	 * @param {object} options - Close options
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	proCameraClose: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'proCameraClose',
			[options]
		);
	},

	/**
	 * Get current camera settings
	 * @param {object} options - Options
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	proCameraGetSettings: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'proCameraGetSettings',
			[options]
		);
	},
	
	// =========================================================================
	// PRO CAMERA UTILITY METHODS
	// =========================================================================

	/**
	 * Quick Pro Camera setup with sensible defaults
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	quickProCameraSetup: function(successCallback, errorCallback) {
		this.proCameraOpen({
			resolution: '1920x1080',
			raw: false,
			camera: 'back'
		}, successCallback, errorCallback);
	},

	/**
	 * Test all Pro Camera features
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	testProCameraFeatures: function(successCallback, errorCallback) {
		console.log('Testing Pro Camera Features...');
		var self = this;
		
		var testResults = {
			cameraOpen: false,
			isoControl: false,
			shutterControl: false,
			whiteBalance: false,
			focusControl: false,
			exposureControl: false,
			rawSupport: false,
			capture: false,
			cameraClose: false
		};
		
		// Test sequence
		this.proCameraOpen(
			{ resolution: '1920x1080', raw: false, camera: 'back' },
			function(openResult) {
				testResults.cameraOpen = true;
				console.log('? Camera opened');
				
				// Test ISO
				setTimeout(function() {
					ResqPeerNet.proCameraSetISO(
						{ value: 400 },
						function(isoResult) {
							testResults.isoControl = true;
							console.log('? ISO control working');
							
							// Test shutter speed
							ResqPeerNet.proCameraSetShutterSpeed(
								{ value: 0.1 },
								function(shutterResult) {
									testResults.shutterControl = true;
									console.log('? Shutter control working');
									
									// Test white balance
									ResqPeerNet.proCameraSetWhiteBalance(
										{ mode: 'daylight' },
										function(wbResult) {
											testResults.whiteBalance = true;
											console.log('? White balance working');
											
											// Test capture
											ResqPeerNet.proCameraCapture(
												{},
												function(captureResult) {
													testResults.capture = true;
													console.log('? Capture working');
													
													// Close camera
													ResqPeerNet.proCameraClose(
														{},
														function(closeResult) {
															testResults.cameraClose = true;
															console.log('? Camera close working');
															
															if (successCallback) successCallback(testResults);
														},
														errorCallback
													);
												},
												errorCallback
											);
										},
										errorCallback
									);
								},
								errorCallback
							);
						},
						errorCallback
					);
				}, 1000);
			},
			errorCallback
		);
	},

	/**
	 * Create Pro Camera controller with chainable methods
	 * @param {object} options - Initial options
	 * @returns {object} Camera controller instance
	 */
	createProCameraController: function(options) {
		var controller = {
			options: options || {},
			isOpen: false,
			
			open: function(callback) {
				var self = this;
				ResqPeerNet.proCameraOpen(this.options, 
					function(result) {
						self.isOpen = true;
						if (callback) callback(null, result);
					},
					function(error) {
						if (callback) callback(error);
					}
				);
				return this;
			},
			
			setISO: function(iso, callback) {
				ResqPeerNet.proCameraSetISO({value: iso}, callback, callback);
				return this;
			},
			
			setShutterSpeed: function(speed, callback) {
				ResqPeerNet.proCameraSetShutterSpeed({value: speed}, callback, callback);
				return this;
			},
			
			setWhiteBalance: function(mode, callback) {
				ResqPeerNet.proCameraSetWhiteBalance({mode: mode}, callback, callback);
				return this;
			},
			
			setFocus: function(distance, callback) {
				ResqPeerNet.proCameraSetFocus({distance: distance}, callback, callback);
				return this;
			},
			
			setExposureCompensation: function(ev, callback) {
				ResqPeerNet.proCameraSetExposureCompensation({value: ev}, callback, callback);
				return this;
			},
			
			enableRAW: function(enable, callback) {
				ResqPeerNet.proCameraEnableRAW({enable: enable}, callback, callback);
				return this;
			},
			
			capture: function(callback) {
				ResqPeerNet.proCameraCapture({}, callback, callback);
				return this;
			},
			
			getSettings: function(callback) {
				ResqPeerNet.proCameraGetSettings({}, callback, callback);
				return this;
			},
			
			close: function(callback) {
				var self = this;
				ResqPeerNet.proCameraClose({}, 
					function(result) {
						self.isOpen = false;
						if (callback) callback(null, result);
					},
					function(error) {
						if (callback) callback(error);
					}
				);
				return this;
			}
		};
		
		return controller;
	},
	
	// =========================================================================
	// DISPLAY MANAGEMENT METHODS - NEW
	// =========================================================================

	/**
	 * Enter fullscreen immersive mode
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	enterFullscreen: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'enterFullscreen',
			[]
		);
	},
	
	enterPermanentFullscreen: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'enterPermanentFullscreen',
			[]
		);
	},
	
	setScreenshotAllowed: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'setScreenshotAllowed',
			[]
		);
	},
	
	getScreenshotStatus: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'getScreenshotStatus',
			[]
		);
	},
	
	enableContentProtection: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'enableContentProtection',
			[]
		);
	},

	/**
	 * Exit fullscreen mode
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	exitFullscreen: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'exitFullscreen',
			[]
		);
	},

	/**
	 * Toggle between fullscreen and normal mode
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	toggleFullscreen: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'toggleFullscreen',
			[]
		);
	},

	/**
	 * Set screen orientation
	 * @param {object} options - {orientation: 'portrait'|'landscape'|'sensor'|'unspecified'}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	setScreenOrientation: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'setScreenOrientation',
			[options]
		);
	},

	/**
	 * Keep screen on (prevent screen timeout)
	 * @param {object} options - {keepOn: true|false}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	keepScreenOn: function(options, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'keepScreenOn',
			[options]
		);
	},

	/**
	 * Get display metrics and information
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	getDisplayMetrics: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'getDisplayMetrics',
			[]
		);
	},

	/**
	 * Get current fullscreen status
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	getFullscreenStatus: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'getFullscreenStatus',
			[]
		);
	},

	/**
	 * Enter immersive mode (temporary fullscreen)
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	enterImmersiveMode: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'enterImmersiveMode',
			[]
		);
	},
	
	// =========================================================================
	// DISPLAY MANAGEMENT TEST METHODS - NEW
	// =========================================================================

	/**
	 * Test fullscreen functionality
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	testFullscreenFunctionality: function(successCallback, errorCallback) {
		console.log('Testing Fullscreen Functionality...');
		var self = this;
		
		var testResults = {
			enterFullscreen: false,
			getStatus: false,
			getMetrics: false,
			setOrientation: false,
			keepScreenOn: false,
			exitFullscreen: false,
			toggle: false
		};
		
		// Test sequence
		this.enterFullscreen(
			function(enterResult) {
				testResults.enterFullscreen = true;
				console.log('Enter fullscreen test passed');
				
				// Test get status
				setTimeout(function() {
					ResqPeerNet.getFullscreenStatus(
						function(statusResult) {
							testResults.getStatus = true;
							console.log('Get fullscreen status test passed');
							
							// Test get metrics
							ResqPeerNet.getDisplayMetrics(
								function(metricsResult) {
									testResults.getMetrics = true;
									console.log('Get display metrics test passed');
									
									// Test set orientation
									ResqPeerNet.setScreenOrientation(
										{ orientation: 'landscape' },
										function(orientationResult) {
											testResults.setOrientation = true;
											console.log('Set orientation test passed');
											
											// Test keep screen on
											ResqPeerNet.keepScreenOn(
												{ keepOn: true },
												function(keepOnResult) {
													testResults.keepScreenOn = true;
													console.log('Keep screen on test passed');
													
													// Test toggle
													ResqPeerNet.toggleFullscreen(
														function(toggleResult) {
															testResults.toggle = true;
															console.log('Toggle fullscreen test passed');
															
															// Final exit
															setTimeout(function() {
																ResqPeerNet.exitFullscreen(
																	function(exitResult) {
																		testResults.exitFullscreen = true;
																		console.log('Exit fullscreen test passed');
																		
																		if (successCallback) successCallback(testResults);
																	},
																	errorCallback
																);
															}, 1000);
														},
														errorCallback
													);
												},
												errorCallback
											);
										},
										errorCallback
									);
								},
								errorCallback
							);
						},
						errorCallback
					);
				}, 500);
			},
			errorCallback
		);
	},

	/**
	 * Test immersive mode functionality
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	testImmersiveMode: function(successCallback, errorCallback) {
		console.log('Testing Immersive Mode...');
		
		this.enterImmersiveMode(
			function(result) {
				console.log('Immersive mode entered:', result);
				
				// Auto exit after 3 seconds for testing
				setTimeout(function() {
					// Immersive mode usually exits automatically on user interaction
					console.log('Immersive mode test completed');
					if (successCallback) successCallback({
						immersiveTest: true,
						result: result
					});
				}, 3000);
			},
			errorCallback
		);
	},

	/**
	 * Comprehensive display features test
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	testAllDisplayFeatures: function(successCallback, errorCallback) {
		console.log('=== TESTING ALL DISPLAY FEATURES ===');
		var self = this;
		
		var allTestResults = {
			fullscreenTest: false,
			immersiveTest: false,
			orientationTest: false,
			screenOnTest: false,
			metricsTest: false,
			utilityTest: false
		};
		
		// Run fullscreen test first
		this.testFullscreenFunctionality(
			function(fullscreenResults) {
				allTestResults.fullscreenTest = true;
				console.log('Fullscreen features test completed');
				
				// Test immersive mode
				setTimeout(function() {
					ResqPeerNet.testImmersiveMode(
						function(immersiveResults) {
							allTestResults.immersiveTest = true;
							console.log('Immersive mode test completed');
							
							// Test orientation utilities
							ResqPeerNet.setLandscapeMode(
								function(landscapeResult) {
									console.log('Landscape mode test completed');
									
									setTimeout(function() {
										ResqPeerNet.setPortraitMode(
											function(portraitResult) {
												console.log('Portrait mode test completed');
												allTestResults.orientationTest = true;
												
												// Test screen keep on
												ResqPeerNet.keepScreenOnForDuration(
													2000, // 2 seconds
													function(screenOnResult) {
														allTestResults.screenOnTest = true;
														console.log('Screen keep on test completed');
														
														// Test metrics
														ResqPeerNet.getSimpleDisplayInfo(
															function(metricsResult) {
																allTestResults.metricsTest = true;
																console.log('Display metrics test completed');
																
																// Test utility functions
																ResqPeerNet.toggleScreenKeepOn(
																	function(toggleResult) {
																		allTestResults.utilityTest = true;
																		console.log('Utility functions test completed');
																		
																		if (successCallback) successCallback(allTestResults);
																	},
																	errorCallback
																);
															},
															errorCallback
														);
													},
													errorCallback
												);
											},
											errorCallback
										);
									}, 1000);
								},
								errorCallback
							);
						},
						errorCallback
					);
				}, 1000);
			},
			errorCallback
		);
	},

	/**
	 * Demo display features with visual feedback
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	demoDisplayFeatures: function(successCallback, errorCallback) {
		console.log('Starting Display Features Demo...');
		var self = this;
		
		var demoSteps = [
			{ name: 'Get current display info', method: 'getSimpleDisplayInfo' },
			{ name: 'Enter fullscreen', method: 'enterFullscreen' },
			{ name: 'Set landscape orientation', method: 'setLandscapeMode' },
			{ name: 'Keep screen on', method: 'keepScreenOn', args: [{ keepOn: true }] },
			{ name: 'Enter immersive mode', method: 'enterImmersiveMode' },
			{ name: 'Restore portrait orientation', method: 'setPortraitMode' },
			{ name: 'Exit fullscreen', method: 'exitFullscreen' },
			{ name: 'Restore screen timeout', method: 'keepScreenOn', args: [{ keepOn: false }] }
		];
		
		var currentStep = 0;
		var demoResults = [];
		
		function executeNextStep() {
			if (currentStep >= demoSteps.length) {
				console.log('Display demo completed!');
				if (successCallback) successCallback({
					completed: true,
					steps: demoSteps.length,
					results: demoResults
				});
				return;
			}
			
			var step = demoSteps[currentStep];
			console.log('Demo Step ' + (currentStep + 1) + ': ' + step.name);
			
			var method = self[step.method];
			var args = step.args || [];
			
			// Add callbacks
			args.push(function(result) {
				demoResults.push({
					step: step.name,
					result: result,
					success: true
				});
				console.log('? ' + step.name + ' - SUCCESS');
				
				currentStep++;
				setTimeout(executeNextStep, 1500); // Wait 1.5 seconds between steps
			});
			
			args.push(function(error) {
				demoResults.push({
					step: step.name,
					error: error,
					success: false
				});
				console.error('? ' + step.name + ' - FAILED:', error);
				
				currentStep++;
				setTimeout(executeNextStep, 1000);
			});
			
			// Execute the method
			method.apply(self, args);
		}
		
		// Start demo
		executeNextStep();
	},

    // =========================================================================
    // LOCATION UTILITY METHODS
    // =========================================================================

    /**
     * Quick location utility - get current location with default options
     */
    getQuickLocation: function(successCallback, errorCallback) {
        var args = {
            useLastKnown: false,
            timeout: 15000,
            desiredAccuracy: 50
        };
        
        this.getCurrentLocation(args, successCallback, errorCallback);
    },

    /**
     * Start location tracking with sensible defaults
     */
    startQuickTracking: function(successCallback, errorCallback) {
        var args = {
            updateInterval: 5000,
            minDistance: 10,
            provider: 'best'
        };
        
        this.startLocationTracking(args, successCallback, errorCallback);
    },

    /**
     * Check if location services are available and permissions granted
     */
    isLocationAvailable: function(successCallback, errorCallback) {
        this.getLocationPermissions(function(permissions) {
            var isAvailable = permissions.hasAnyLocation && permissions.locationServicesEnabled;
            if (successCallback) successCallback({
                available: isAvailable,
                permissions: permissions
            });
        }, errorCallback);
    },

    /**
     * Calculate distance from current location to target coordinates
     */
    calculateDistanceFromCurrent: function(targetLat, targetLon, successCallback, errorCallback) {
        // First get current location
        this.getCurrentLocation(
            { useLastKnown: true },
            function(currentLocation) {
                // Then calculate distance
                ResqPeerNet.calculateDistance(
                    {
                        lat1: currentLocation.latitude,
                        lon1: currentLocation.longitude,
                        lat2: targetLat,
                        lon2: targetLon
                    },
                    function(distanceResult) {
                        var result = {
                            currentLocation: currentLocation,
                            targetLocation: {
                                latitude: targetLat,
                                longitude: targetLon
                            },
                            distance: distanceResult.distance,
                            distanceKm: (distanceResult.distance / 1000).toFixed(2)
                        };
                        if (successCallback) successCallback(result);
                    },
                    errorCallback
                );
            },
            errorCallback
        );
    },

    /**
     * Monitor proximity to a target location
     */
    startProximityMonitor: function(targetLat, targetLon, proximityRadius, callback) {
        var isMonitoring = false;
        var checkInterval = null;
        
        var monitor = {
            start: function() {
                if (isMonitoring) return;
                
                isMonitoring = true;
                checkInterval = setInterval(function() {
                    ResqPeerNet.calculateDistanceFromCurrent(
                        targetLat, 
                        targetLon,
                        function(result) {
                            var isInRange = result.distance <= proximityRadius;
                            callback({
                                inRange: isInRange,
                                distance: result.distance,
                                currentLocation: result.currentLocation,
                                targetLocation: result.targetLocation
                            });
                        },
                        function(error) {
                            callback({
                                inRange: false,
                                error: error,
                                distance: null
                            });
                        }
                    );
                }, 5000); // Check every 5 seconds
            },
            
            stop: function() {
                isMonitoring = false;
                if (checkInterval) {
                    clearInterval(checkInterval);
                    checkInterval = null;
                }
            },
            
            isMonitoring: function() {
                return isMonitoring;
            }
        };
        
        return monitor;
    },

    /**
     * Get location with retry mechanism
     */
    getLocationWithRetry: function(maxRetries, successCallback, errorCallback) {
        var retries = 0;
        var self = this;
        
        function attemptGetLocation() {
            ResqPeerNet.getCurrentLocation(
                { 
                    useLastKnown: false,
                    timeout: 10000,
                    desiredAccuracy: 100 
                },
                function(location) {
                    if (successCallback) successCallback(location);
                },
                function(error) {
                    retries++;
                    if (retries < maxRetries) {
                        console.log('Location attempt ' + retries + ' failed, retrying...');
                        setTimeout(attemptGetLocation, 2000); // Retry after 2 seconds
                    } else {
                        if (errorCallback) errorCallback('Failed after ' + maxRetries + ' attempts: ' + error);
                    }
                }
            );
        }
        
        attemptGetLocation();
    },

    /**
     * Test location functionality
     */
    testLocationFunctionality: function(successCallback, errorCallback) {
        console.log('Testing Location Functionality...');
        var self = this;
        
        var testResults = {
            permissions: false,
            providers: false,
            currentLocation: false,
            distanceCalculation: false,
            tracking: false
        };
        
        // Test permissions first
        this.getLocationPermissions(
            function(permissions) {
                testResults.permissions = true;
                console.log('Location permissions:', permissions);
                
                // Test available providers
                ResqPeerNet.getAvailableProviders(
                    function(providers) {
                        testResults.providers = true;
                        console.log('Available providers:', providers);
                        
                        // Test current location
                        ResqPeerNet.getCurrentLocation(
                            { useLastKnown: true, timeout: 10000 },
                            function(location) {
                                testResults.currentLocation = true;
                                console.log('Current location test passed:', location);
                                
                                // Test distance calculation
                                ResqPeerNet.calculateDistance(
                                    {
                                        lat1: location.latitude,
                                        lon1: location.longitude,
                                        lat2: location.latitude + 0.001,
                                        lon2: location.longitude + 0.001
                                    },
                                    function(distance) {
                                        testResults.distanceCalculation = true;
                                        console.log('Distance calculation test passed:', distance);
                                        
                                        // Return final results
                                        if (successCallback) successCallback(testResults);
                                    },
                                    function(distanceError) {
                                        console.log('Distance calculation test failed:', distanceError);
                                        if (successCallback) successCallback(testResults);
                                    }
                                );
                            },
                            function(locationError) {
                                console.log('Current location test failed:', locationError);
                                if (successCallback) successCallback(testResults);
                            }
                        );
                    },
                    function(providersError) {
                        console.log('Providers test failed:', providersError);
                        if (successCallback) successCallback(testResults);
                    }
                );
            },
            function(permissionsError) {
                console.log('Permissions test failed:', permissionsError);
                if (successCallback) successCallback(testResults);
            }
        );
    },
	
	// =========================================================================
	// APP DISCOVERY & MANAGEMENT METHODS
	// =========================================================================

    /**
     * Get installed applications (default: user apps only)
     * @param {function} successCallback - Success callback
     * @param {function} errorCallback - Error callback  
     */
    getInstalledApps: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getInstalledApps',
            []
        );
    },

    /**
     * Get only USER applications (non-system)
     * @param {function} successCallback - Success callback
     * @param {function} errorCallback - Error callback  
     */
    getUserApps: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getUserApps',
            []
        );
    },

    /**
     * Get ALL applications (including system apps)
     * @param {function} successCallback - Success callback
     * @param {function} errorCallback - Error callback
     */
    getAllInstalledApps: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getAllInstalledApps',
            []
        );
    },

    /**
     * Get only SYSTEM applications
     * @param {function} successCallback - Success callback
     * @param {function} errorCallback - Error callback
     */
    getSystemApps: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ResqPeerNet',
            'getSystemApps',
            []
        );
    },

	/**
	 * Get detailed information about specific application
	 * @param {object} args - {packageName: 'com.example.app'}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	getAppInfo: function(args, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'getAppInfo',
			[args]
		);
	},

	/**
	 * Uninstall an application (opens system uninstall dialog)
	 * @param {object} args - {packageName: 'com.example.app'}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	uninstallApp: function(args, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'uninstallApp',
			[args]
		);
	},

	/**
	 * Launch an application if installed
	 * @param {object} args - {packageName: 'com.example.app'} 
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	launchApp: function(args, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'launchApp',
			[args]
		);
	},

	/**
	 * Open app in Play Store
	 * @param {object} args - {packageName: 'com.example.app'}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	openAppInPlayStore: function(args, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'openAppInPlayStore',
			[args]
		);
	},

	/**
	 * Check if specific app is installed
	 * @param {object} args - {packageName: 'com.example.app'}
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	isAppInstalled: function(args, successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'isAppInstalled',
			[args]
		);
	},

	/**
	 * Start real-time monitoring for app install/uninstall events
	 * @param {function} successCallback - Success callback (will keep callback alive for events)
	 * @param {function} errorCallback - Error callback
	 */
	startAppMonitoring: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'startAppMonitoring',
			[]
		);
	},

	/**
	 * Stop real-time app monitoring
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	stopAppMonitoring: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'stopAppMonitoring',
			[]
		);
	},

	/**
	 * Get current app monitoring status
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	getMonitoringStatus: function(successCallback, errorCallback) {
		cordova.exec(
			successCallback,
			errorCallback,
			'ResqPeerNet',
			'getMonitoringStatus',
			[]
		);
	},
	
	// =========================================================================
	// APP DISCOVERY UTILITY METHODS - NEW
	// =========================================================================

	/**
	 * Quick utility to search apps by name
	 * @param {string} query - Search query
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	searchApps: function(query, successCallback, errorCallback) {
		this.getInstalledApps(
			function(apps) {
				var filteredApps = apps.apps.filter(function(app) {
					return app.name.toLowerCase().includes(query.toLowerCase()) || 
						   app.packageName.toLowerCase().includes(query.toLowerCase());
				});
				
				var result = {
					query: query,
					results: filteredApps,
					totalFound: filteredApps.length,
					totalApps: apps.count
				};
				
				if (successCallback) successCallback(result);
			},
			errorCallback
		);
	},

	/**
	 * Get apps by category (basic categorization)
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	getAppsByCategory: function(successCallback, errorCallback) {
		this.getInstalledApps(
			function(apps) {
				var categories = {
					system: [],
					user: [],
					updatedSystem: [],
					social: [],
					tools: [],
					games: [],
					media: [],
					other: []
				};
				
				apps.apps.forEach(function(app) {
					// Categorize based on package name and system flags
					if (app.isSystemApp) {
						categories.system.push(app);
					} else if (app.isUpdatedSystemApp) {
						categories.updatedSystem.push(app);
					} else {
						categories.user.push(app);
						
						// Further categorization for user apps
						var pkg = app.packageName.toLowerCase();
						var name = app.name.toLowerCase();
						
						if (pkg.includes('whatsapp') || pkg.includes('facebook') || 
							pkg.includes('instagram') || pkg.includes('twitter') ||
							pkg.includes('messenger') || pkg.includes('social')) {
							categories.social.push(app);
						} else if (pkg.includes('game') || name.includes('game')) {
							categories.games.push(app);
						} else if (pkg.includes('camera') || pkg.includes('gallery') || 
								   pkg.includes('photo') || pkg.includes('video') ||
								   pkg.includes('music') || pkg.includes('media')) {
							categories.media.push(app);
						} else if (pkg.includes('tool') || pkg.includes('utility') ||
								   pkg.includes('calculator') || pkg.includes('file')) {
							categories.tools.push(app);
						} else {
							categories.other.push(app);
						}
					}
				});
				
				if (successCallback) successCallback({
					categories: categories,
					summary: {
						total: apps.count,
						system: categories.system.length,
						user: categories.user.length,
						social: categories.social.length,
						games: categories.games.length,
						media: categories.media.length,
						tools: categories.tools.length
					}
				});
			},
			errorCallback
		);
	},

	/**
	 * Get app usage statistics (basic)
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	getAppStats: function(successCallback, errorCallback) {
		this.getInstalledApps(
			function(apps) {
				var stats = {
					totalApps: apps.count,
					systemApps: 0,
					userApps: 0,
					updatedSystemApps: 0,
					averageNameLength: 0,
					nameLengths: [],
					packageDomains: {}
				};
				
				var totalNameLength = 0;
				
				apps.apps.forEach(function(app) {
					if (app.isSystemApp) {
						stats.systemApps++;
					} else {
						stats.userApps++;
					}
					
					if (app.isUpdatedSystemApp) {
						stats.updatedSystemApps++;
					}
					
					// Name length analysis
					var nameLength = app.name.length;
					totalNameLength += nameLength;
					stats.nameLengths.push(nameLength);
					
					// Package domain analysis
					var domain = app.packageName.split('.')[0];
					if (stats.packageDomains[domain]) {
						stats.packageDomains[domain]++;
					} else {
						stats.packageDomains[domain] = 1;
					}
				});
				
				stats.averageNameLength = totalNameLength / apps.count;
				stats.mostCommonDomains = Object.entries(stats.packageDomains)
					.sort((a, b) => b[1] - a[1])
					.slice(0, 5);
				
				if (successCallback) successCallback(stats);
			},
			errorCallback
		);
	},

	/**
	 * Quick app info by package name
	 * @param {string} packageName - Package name to check
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	quickAppInfo: function(packageName, successCallback, errorCallback) {
		var self = this;
		
		this.isAppInstalled(
			{ packageName: packageName },
			function(installedResult) {
				if (installedResult.installed) {
					// App is installed, get detailed info
					ResqPeerNet.getAppInfo(
						{ packageName: packageName },
						function(appInfo) {
							var combinedInfo = {
								installed: true,
								packageName: packageName,
								appName: installedResult.appName,
								detailedInfo: appInfo,
								canLaunch: true,
								canUninstall: !appInfo.isSystemApp
							};
							if (successCallback) successCallback(combinedInfo);
						},
						function(error) {
							// Fallback if detailed info fails
							var basicInfo = {
								installed: true,
								packageName: packageName,
								appName: installedResult.appName,
								detailedInfo: null,
								canLaunch: true,
								canUninstall: true // Assume can uninstall if we don't know
							};
							if (successCallback) successCallback(basicInfo);
						}
					);
				} else {
					// App not installed
					if (successCallback) successCallback({
						installed: false,
						packageName: packageName,
						appName: '',
						canLaunch: false,
						canUninstall: false
					});
				}
			},
			errorCallback
		);
	},

	/**
	 * Batch check multiple apps
	 * @param {array} packageNames - Array of package names
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	batchCheckApps: function(packageNames, successCallback, errorCallback) {
		var results = {};
		var completed = 0;
		
		packageNames.forEach(function(packageName) {
			ResqPeerNet.isAppInstalled(
				{ packageName: packageName },
				function(result) {
					results[packageName] = result;
					completed++;
					
					if (completed === packageNames.length) {
						if (successCallback) successCallback({
							checked: packageNames.length,
							results: results,
							installedCount: Object.values(results).filter(r => r.installed).length
						});
					}
				},
				function(error) {
					results[packageName] = { error: error, installed: false };
					completed++;
					
					if (completed === packageNames.length) {
						if (successCallback) successCallback({
							checked: packageNames.length,
							results: results,
							installedCount: Object.values(results).filter(r => r.installed).length,
							hasErrors: true
						});
					}
				}
			);
		});
	},
	
	// =========================================================================
	// APP DISCOVERY TEST METHODS
	// =========================================================================

	/**
	 * Test app discovery functionality
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	testAppDiscovery: function(successCallback, errorCallback) {
		console.log('Testing App Discovery Features...', 'info');
		
		var testResults = {
			getInstalledApps: false,
			getAppInfo: false,
			isAppInstalled: false,
			searchApps: false,
			getAppStats: false,
			monitoring: false
		};
		
		// Test getInstalledApps
		this.getInstalledApps(
			function(apps) {
				testResults.getInstalledApps = true;
				console.log('? getInstalledApps test passed:', apps.count + ' apps found');
				
				// Test getAppInfo with first app
				if (apps.apps.length > 0) {
					var firstApp = apps.apps[0];
					ResqPeerNet.getAppInfo(
						{ packageName: firstApp.packageName },
						function(appInfo) {
							testResults.getAppInfo = true;
							console.log('? getAppInfo test passed for:', firstApp.packageName);
							
							// Test isAppInstalled
							ResqPeerNet.isAppInstalled(
								{ packageName: firstApp.packageName },
								function(installedResult) {
									testResults.isAppInstalled = true;
									console.log('? isAppInstalled test passed');
									
									// Test search
									ResqPeerNet.searchApps(
										firstApp.name.substring(0, 3),
										function(searchResult) {
											testResults.searchApps = true;
											console.log('? searchApps test passed');
											
											// Test app stats
											ResqPeerNet.getAppStats(
												function(stats) {
													testResults.getAppStats = true;
													console.log('? getAppStats test passed');
													
													// Test monitoring
													ResqPeerNet.getMonitoringStatus(
														function(monitoringStatus) {
															testResults.monitoring = true;
															console.log('? monitoring status test passed');
															
															if (successCallback) successCallback(testResults);
														},
														errorCallback
													);
												},
												errorCallback
											);
										},
										errorCallback
									);
								},
								errorCallback
							);
						},
						errorCallback
					);
				} else {
					console.log('? No apps found to test with');
					if (successCallback) successCallback(testResults);
				}
			},
			errorCallback
		);
	},

	/**
	 * Test app management functionality
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	testAppManagement: function(successCallback, errorCallback) {
		console.log('Testing App Management Features...', 'info');
		
		// Test with a common app (Chrome)
		var testPackage = 'com.android.chrome';
		
		this.quickAppInfo(
			testPackage,
			function(appInfo) {
				console.log('App management test completed:', {
					package: testPackage,
					installed: appInfo.installed,
					canLaunch: appInfo.canLaunch,
					canUninstall: appInfo.canUninstall
				});
				
				if (successCallback) successCallback({
					testedPackage: testPackage,
					appInfo: appInfo,
					managementAvailable: true
				});
			},
			errorCallback
		);
	},

	/**
	 * Demo app discovery features
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	demoAppDiscovery: function(successCallback, errorCallback) {
		console.log('Starting App Discovery Demo...');
		
		var demoResults = {
			steps: [],
			summary: {}
		};
		
		var steps = [
			{ name: 'Get installed apps count', method: 'getInstalledApps' },
			{ name: 'Get app statistics', method: 'getAppStats' },
			{ name: 'Categorize apps', method: 'getAppsByCategory' },
			{ name: 'Check monitoring status', method: 'getMonitoringStatus' }
		];
		
		var currentStep = 0;
		
		function executeNextStep() {
			if (currentStep >= steps.length) {
				console.log('? App discovery demo completed!');
				
				// Create summary
				demoResults.summary = {
					totalSteps: steps.length,
					completedSteps: demoResults.steps.filter(step => step.success).length,
					demoTime: new Date().toISOString()
				};
				
				if (successCallback) successCallback(demoResults);
				return;
			}
			
			var step = steps[currentStep];
			console.log('Demo Step ' + (currentStep + 1) + ': ' + step.name);
			
			ResqPeerNet[step.method](
				function(result) {
					demoResults.steps.push({
						step: step.name,
						result: result,
						success: true,
						timestamp: new Date().toISOString()
					});
					
					console.log('? ' + step.name + ' - SUCCESS');
					currentStep++;
					setTimeout(executeNextStep, 1000);
				},
				function(error) {
					demoResults.steps.push({
						step: step.name,
						error: error,
						success: false,
						timestamp: new Date().toISOString()
					});
					
					console.error('? ' + step.name + ' - FAILED:', error);
					currentStep++;
					setTimeout(executeNextStep, 500);
				}
			);
		}
		
		// Start demo
		executeNextStep();
	},

	/**
	 * Start real-time app monitoring demo
	 * @param {function} eventCallback - Callback for app change events
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	startAppMonitoringDemo: function(eventCallback, successCallback, errorCallback) {
		console.log('Starting Real-time App Monitoring Demo...');
		
		// Setup event listener for app changes
		ResqPeerNet.on('app_installed', function(event) {
			console.log('NEW APP INSTALLED:', event.packageName, event.appInfo.name);
			if (eventCallback) eventCallback('installed', event);
		});
		
		ResqPeerNet.on('app_uninstalled', function(event) {
			console.log('APP UNINSTALLED:', event.packageName);
			if (eventCallback) eventCallback('uninstalled', event);
		});
		
		ResqPeerNet.on('app_updated', function(event) {
			console.log('APP UPDATED:', event.packageName, event.appInfo.name);
			if (eventCallback) eventCallback('updated', event);
		});
		
		// Start monitoring
		this.startAppMonitoring(
			function(result) {
				console.log('? App monitoring started:', result);
				if (successCallback) successCallback({
					monitoring: true,
					message: 'Real-time app monitoring is now active'
				});
			},
			errorCallback
		);
	},

	/**
	 * Stop app monitoring demo
	 * @param {function} successCallback - Success callback
	 * @param {function} errorCallback - Error callback
	 */
	stopAppMonitoringDemo: function(successCallback, errorCallback) {
		console.log('Stopping App Monitoring Demo...');
		
		// Remove event listeners
		ResqPeerNet.off('app_installed');
		ResqPeerNet.off('app_uninstalled');
		ResqPeerNet.off('app_updated');
		
		// Stop monitoring
		this.stopAppMonitoring(
			function(result) {
				console.log('? App monitoring stopped:', result);
				if (successCallback) successCallback({
					monitoring: false,
					message: 'Real-time app monitoring stopped'
				});
			},
			errorCallback
		);
	},

    // =========================================================================
    // AUTOMATIC EVENT SYSTEM INITIALIZATION
    // =========================================================================

    /**
     * Initialize event system and wrap all methods with event triggers
     */
	_initializeEventSystem: function() {
		console.log('Initializing ResqPeerNet Event System...');
		
		// List of methods to wrap with events - 100% COMPLETE VERSION
		var methodsToWrap = [
			// =========================================================================
			// BASIC PLUGIN METHODS
			// =========================================================================
			'init', 'getPluginInfo', 'checkStatus', 'testConnection', 'getSystemInfo',
			
			// =========================================================================
			// PERMISSION MANAGEMENT
			// =========================================================================
			'getPermissionStatus', 'requestPermission', 'requestPermissions',
			'hasStoragePermission', 'hasNetworkPermissions',
			
			// =========================================================================
			// FILE MANAGEMENT
			// =========================================================================
			'readFileAsText', 'getStorageInfo', 'searchFiles', 'getFileInfo',
				
			// =========================================================================
			// WALLPAPER MANAGEMENT
			// =========================================================================
			'getWallpaper', 'getWallpaperInfo', 'setWallpaperFromFile', 
			'setWallpaperFromUri', 'listenWallpaperChanged', 'stopListeningWallpaper',
			
			// =========================================================================
			// NETWORK STATUS
			// =========================================================================
			'getNetworkStatus', 'getWifiStatus', 'getMobileStatus',
			
			// =========================================================================
			// BLUETOOTH
			// =========================================================================
			'getBluetoothStatus', 'enableBluetooth', 'getPairedDevices', 
			'startBluetoothDiscovery', 'stopBluetoothDiscovery', 'connectToBluetoothDevice', 
			'startBluetoothServer', 'stopBluetoothServer', 'sendMessage', 'broadcastMessage', 
			'getConnectedDevices', 'disconnectDevice', 'disconnectAllDevices', 
			'startDiscoveryListener', 'startConnectionListener', 'startMessageListener',
			
			// =========================================================================
			// MESH NETWORK
			// =========================================================================
			'initializeMesh', 'joinMesh', 'sendMeshMessage', 'broadcastToMesh', 
			'getMeshTopology', 'startMeshEventListener', 'discoverMeshNodes', 
			'autoJoinMesh', 'getAvailableGateways',
			
			// =========================================================================
			// DEVICE INFO & BATTERY
			// =========================================================================
			'getDeviceInfo', 'getBatteryStatus', 'startBatteryListener', 'stopBatteryListener',
			
			// =========================================================================
			// MEDIA CAPTURE - BASIC
			// =========================================================================
			'captureImageWithChoice', 'captureAudioWithChoice', 'openCustomCamera', 
						'openCustomAudioRecorder', 'closeCustomUI', 'captureAudio', 'captureImage', 
			'captureVideo', 'getSupportedFormats',
			
			// =========================================================================
			// AUDIO PLAYBACK
			// =========================================================================
			'createAudio', 'playAudio', 'pauseAudio', 'stopAudio', 'seekAudio', 
			'getAudioDuration', 'getAudioPosition', 'setAudioVolume', 'releaseAudio',
			
			// =========================================================================
			// LOCATION SERVICES - BASIC
			// =========================================================================
			'getCurrentLocation', 'startLocationTracking', 'stopLocationTracking', 
			'getLocationPermissions', 'calculateDistance', 'getAvailableProviders',
			
			// =========================================================================
			// SYSTEM MONITORING - CORE
			// =========================================================================
			'getSystemResources', 'getAvailableSensors', 'getSensorCapabilities', 
			'runDeviceDiagnostics', 'getDeviceSummary', 'checkDeviceCapabilities', 
			'getComprehensiveSystemReport', 'getDetailedSensorInfo', 'quickHealthCheck',
			'startSystemMonitoring', 'stopSystemMonitoring',
			
			// =========================================================================
			// PRO CAMERA - CORE METHODS
			// =========================================================================
			'proCameraOpen', 'proCameraCapture', 'proCameraSetISO',
			'proCameraSetShutterSpeed', 'proCameraSetWhiteBalance', 'proCameraSetFocus',
			'proCameraSetExposureCompensation', 'proCameraEnableRAW', 'proCameraClose',
			'proCameraGetSettings',
			
			// =========================================================================
			// PRO CAMERA - UTILITIES
			// =========================================================================
			'quickProCameraSetup', 'testProCameraFeatures', 'createProCameraController',
			
			// =========================================================================
			// DISPLAY MANAGEMENT - CORE
			// =========================================================================
			'enterFullscreen', 'exitFullscreen', 'toggleFullscreen', 'setScreenOrientation',
			'keepScreenOn', 'getDisplayMetrics', 'getFullscreenStatus', 'enterImmersiveMode',
			'enterPermanentFullscreen', 'setScreenshotAllowed', 'getScreenshotStatus', 
			'enableContentProtection',
			
			// =========================================================================
			// APP DISCOVERY - CORE
			// =========================================================================
			'getInstalledApps', 'getAppInfo', 'uninstallApp', 'launchApp',
			'openAppInPlayStore', 'isAppInstalled', 'startAppMonitoring',
			'stopAppMonitoring', 'getMonitoringStatus', 'searchApps',
			'getAppsByCategory', 'getAppStats', 'quickAppInfo', 'batchCheckApps',
			'getUserApps', 'getAllInstalledApps', 'getSystemApps',
			
			// =========================================================================
			// MESH NETWORK VARIANTS
			// =========================================================================
			'initializeWiFiMesh', 'createWiFiMeshGroup', 'discoverWiFiPeers',
			'connectToWiFiDevice', 'getWiFiPeers', 'sendWiFiMeshMessage',
			'getWiFiMeshTopology', 'removeWiFiMeshGroup',
			'initializeLocalMesh', 'discoverLocalPeers', 'getLocalPeers',
			'autoConnectLocalPeers', 'sendLocalMeshMessage', 'getLocalMeshTopology',
			'stopLocalMesh',
			'startAutoMesh', 'stopAutoMesh', 'broadcastToAutoMesh', 'getAutoMeshStatus',
			'startHybridMesh', 'sendHybridMessage', 'getHybridStatus', 'stopHybridMesh',
			
			// =========================================================================
			// MEDIA UTILITIES
			// =========================================================================
			'createQuickAudioPlayer', 'quickCaptureImage', 'quickCaptureAudio', 
			'quickCaptureVideo', 'hasMediaPermissions', 'requestMediaPermissions',
			'testMediaFunctionality',
			
			// =========================================================================
			// LOCATION UTILITIES
			// =========================================================================
			'getQuickLocation', 'startQuickTracking', 'isLocationAvailable',
			'calculateDistanceFromCurrent', 'getLocationWithRetry', 'testLocationFunctionality',
			
			// =========================================================================
			// TEST & DEMO METHODS
			// =========================================================================
			'testGetDeviceInfo', 'testGetDeviceSummary', 'testGetDeviceCapabilities',
			'testSystemResources', 'testSensorCapabilities', 'testDeviceDiagnostics',
			'testAllPhase1Features', 'testAllDeviceFunctions',
			'testFullscreenFunctionality', 'testImmersiveMode', 'testAllDisplayFeatures',
			'demoDisplayFeatures', 'testProCameraFeatures', 'testAllDisplayFeatures', 'demoAppDiscovery',
			'startAppMonitoringDemo', 'stopAppMonitoringDemo', 'testAppDiscovery', 'testAppManagement'
		];
		
		// Wrap each method
		var wrappedCount = 0;
		methodsToWrap.forEach(function(methodName) {
			if (typeof ResqPeerNet[methodName] === 'function' && 
				!methodName.startsWith('_') && 
				methodName !== 'on' && 
				methodName !== 'off' && 
				methodName !== 'trigger' &&
				methodName !== '_initializeEventSystem') {
				
				var originalMethod = ResqPeerNet[methodName];
				ResqPeerNet[methodName] = ResqPeerNet._wrapMethodWithEvents(methodName, originalMethod);
				wrappedCount++;
			} else {
				console.warn('Method tidak bisa di-wrap:', methodName);
			}
		});
		
		console.log('Event system initialized for ' + wrappedCount + ' methods out of ' + methodsToWrap.length + ' total');
	},

	// =========================================================================
	// DISPLAY UTILITY METHODS - BARU DITAMBAHKAN
	// =========================================================================

	/**
	 * Set landscape orientation (utility method)
	 */
	setLandscapeMode: function(successCallback, errorCallback) {
		this.setScreenOrientation(
			{ orientation: 'landscape' },
			successCallback,
			errorCallback
		);
	},

	/**
	 * Set portrait orientation (utility method)
	 */
	setPortraitMode: function(successCallback, errorCallback) {
		this.setScreenOrientation(
			{ orientation: 'portrait' },
			successCallback,
			errorCallback
		);
	},

	/**
	 * Keep screen on for specific duration
	 */
	keepScreenOnForDuration: function(durationMs, successCallback, errorCallback) {
		// First enable keep screen on
		this.keepScreenOn(
			{ keepOn: true },
			function() {
				// Auto disable after duration
				setTimeout(function() {
					ResqPeerNet.keepScreenOn(
						{ keepOn: false },
						successCallback,
						errorCallback
					);
				}, durationMs);
				
				if (successCallback) successCallback({ 
					enabled: true, 
					duration: durationMs,
					willAutoDisable: true 
				});
			},
			errorCallback
		);
	},

	/**
	 * Toggle screen keep on state
	 */
	toggleScreenKeepOn: function(successCallback, errorCallback) {
		var self = this;
		
		// First get current state
		this.getFullscreenStatus(
			function(status) {
				var newState = !status.keepScreenOn;
				
				ResqPeerNet.keepScreenOn(
					{ keepOn: newState },
					function(result) {
						if (successCallback) successCallback({
							enabled: newState,
							previousState: status.keepScreenOn
						});
					},
					errorCallback
				);
			},
			errorCallback
		);
	},

	/**
	 * Get simple display info (utility method)
	 */
	getSimpleDisplayInfo: function(successCallback, errorCallback) {
		this.getDisplayMetrics(
			function(metrics) {
				var simpleInfo = {
					width: metrics.widthPixels,
					height: metrics.heightPixels,
					density: metrics.density,
					sizeInches: metrics.sizeInches,
					orientation: metrics.orientation,
					isLandscape: metrics.orientation === 'landscape',
					resolution: metrics.widthPixels + 'x' + metrics.heightPixels
				};
				if (successCallback) successCallback(simpleInfo);
			},
			errorCallback
		);
	},

	// =========================================================================
	// PRO CAMERA TEST METHODS
	// =========================================================================

	/**
	 * Test Pro Camera functionality
	 */
	testProCamera: function(successCallback, errorCallback) {
		console.log('Testing Pro Camera...');
		
		this.quickProCameraSetup(
			function(result) {
				console.log('Pro Camera setup successful');
				if (successCallback) successCallback({
					proCameraTest: true,
					result: result
				});
			},
			errorCallback
		);
	},

	/**
	 * Demo Pro Camera features
	 */
	demoProCamera: function(successCallback, errorCallback) {
		console.log('Starting Pro Camera Demo...');
		
		var controller = this.createProCameraController({
			resolution: '1920x1080',
			raw: false,
			camera: 'back'
		});
		
		controller
			.open(function(err, result) {
				if (err) {
					if (errorCallback) errorCallback(err);
					return;
				}
				
				console.log('Camera opened, starting demo sequence...');
				
				// Demo sequence
				setTimeout(function() {
					controller.setISO(400, function() {
						console.log('ISO set to 400');
						
						setTimeout(function() {
							controller.setWhiteBalance('daylight', function() {
								console.log('White balance set to daylight');
								
								setTimeout(function() {
									controller.capture(function(err, photo) {
										if (err) {
											console.error('Capture failed:', err);
										} else {
											console.log('Photo captured successfully');
										}
										
										// Close camera
										setTimeout(function() {
											controller.close(function() {
												console.log('Camera closed');
												if (successCallback) successCallback({
													demoCompleted: true,
													steps: 4
												});
											});
										}, 1000);
									});
								}, 1000);
							});
						}, 1000);
					});
				}, 1000);
			});
	}
};

// =========================================================================
// AUTOMATIC EVENT CATEGORIES AND TYPES
// =========================================================================

/**
 * Event categories for easier listening
 */
ResqPeerNet.EVENTS = {
	// System Events
	SYSTEM: {
		INIT: 'init',
		STATUS: 'status',
		BATTERY: 'battery',
		RESOURCES: 'systemResources',
		DIAGNOSTICS: 'diagnostics',
		SENSORS: 'sensors',
		PLUGIN_READY: 'pluginReady',
		DEVICE_INFO: 'deviceInfo',
		SYSTEM_REPORT: 'systemReport',
		HEALTH_CHECK: 'healthCheck',
		MONITORING_START: 'monitoringStart',
		MONITORING_STOP: 'monitoringStop',
		MONITORING_UPDATE: 'monitoringUpdate'
	},
	
	// Network Events
	NETWORK: {
		STATUS: 'networkStatus',
		WIFI: 'wifiStatus',
		BLUETOOTH: 'bluetooth',
		MESH: 'mesh',
		CONNECTION: 'connection',
		MESSAGE: 'message',
		BLUETOOTH_DISCOVERY: 'bluetoothDiscovery',
		BLUETOOTH_CONNECTION: 'bluetoothConnection',
		BLUETOOTH_SERVER: 'bluetoothServer',
		MESH_TOPOLOGY: 'meshTopology',
		MESH_NODE_DISCOVERY: 'meshNodeDiscovery',
		AUTO_MESH: 'autoMesh',
		HYBRID_MESH: 'hybridMesh',
		WIFI_MESH: 'wifiMesh',
		LOCAL_MESH: 'localMesh',
		GATEWAY_DISCOVERY: 'gatewayDiscovery'
	},
	
	// Media Events
	MEDIA: {
		AUDIO: 'audio',
		CAPTURE: 'capture',
		CAMERA: 'camera',
		FORMATS: 'formats',
		AUDIO_PLAYBACK: 'audioPlayback',
		AUDIO_RECORDING: 'audioRecording',
		VIDEO_CAPTURE: 'videoCapture',
		MEDIA_PERMISSIONS: 'mediaPermissions',
		CUSTOM_UI: 'customUI'
	},
	
	// Pro Camera Events
	PRO_CAMERA: {
		OPENED: 'proCameraOpened',
		CLOSED: 'proCameraClosed',
		CAPTURE: 'proCameraCapture',
		RAW_CAPTURE: 'proCameraRawCapture',
		SETTINGS_UPDATED: 'proCameraSettingsUpdated',
		PREVIEW_STARTED: 'proCameraPreviewStarted',
		CAPTURE_START: 'proCameraCaptureStart',
		CAPTURE_COMPLETE: 'proCameraCaptureComplete',
		ERROR: 'proCameraError',
		DISCONNECTED: 'proCameraDisconnected'
	},
	
	// Location Events
	LOCATION: {
		POSITION: 'location',
		TRACKING: 'tracking',
		PROVIDERS: 'providers',
		DISTANCE: 'distance'
	},
	
	// Display Events
	DISPLAY: {
		FULLSCREEN_ENTER: 'fullscreenEnter',
		FULLSCREEN_EXIT: 'fullscreenExit', 
		FULLSCREEN_TOGGLE: 'fullscreenToggle',
		ORIENTATION_SET: 'orientationSet',
		SCREEN_KEEP_ON: 'screenKeepOn',
		DISPLAY_METRICS: 'displayMetrics',
		FULLSCREEN_STATUS: 'fullscreenStatus',
		IMMERSIVE_MODE: 'immersiveMode',
		SCREENSHOT_STATUS: 'screenshotStatus',
		CONTENT_PROTECTION: 'contentProtection',
		DISPLAY_INFO: 'displayInfo'
	},
	
	// App Discovery Events
	APP_DISCOVERY: {
		INSTALLED: 'app_installed',
		UNINSTALLED: 'app_uninstalled', 
		UPDATED: 'app_updated',
		MONITORING_STARTED: 'app_monitoring_started',
		MONITORING_STOPPED: 'app_monitoring_stopped',
		LIST_UPDATED: 'app_list_updated'
	},
	
	// Permission Events
	PERMISSION: {
		STATUS: 'permission',
		REQUEST: 'permissionRequest',
		STORAGE: 'storagePermission',
		NETWORK: 'networkPermission', 
		LOCATION: 'locationPermission',
		MEDIA: 'mediaPermission',
		BLUETOOTH: 'bluetoothPermission'
	},
	
	// Wallpaper Events
	WALLPAPER: {
		CHANGED: 'wallpaperChanged',
		INFO: 'wallpaperInfo'
	},

	// File Management Events
	FILE_MANAGEMENT: {
		FILE_READ: 'fileRead',
		FILE_SEARCH: 'fileSearch',
		FILE_INFO: 'fileInfo',
		STORAGE_INFO: 'storageInfo',
		FILE_OPERATION: 'fileOperation'
	},

	// Utility & Testing Events
	UTILITY: {
		TEST_START: 'testStart',
		TEST_COMPLETE: 'testComplete',
		TEST_ERROR: 'testError',
		DEMO_START: 'demoStart',
		DEMO_COMPLETE: 'demoComplete',
		METHOD_CALL: 'methodCall',
		METHOD_SUCCESS: 'methodSuccess',
		METHOD_ERROR: 'methodError'
	},

	// Plugin Lifecycle Events
	LIFECYCLE: {
		PLUGIN_LOADED: 'pluginLoaded',
		PLUGIN_READY: 'pluginReady',
		PLUGIN_ERROR: 'pluginError',
		BRIDGE_READY: 'bridgeReady'
	},

	// Error & Debug Events
	DEBUG: {
		ERROR: 'error',
		WARNING: 'warning',
		INFO: 'info',
		DEBUG: 'debug',
		PERFORMANCE: 'performance'
	}
};

/**
 * Pre-defined event listeners for common scenarios
 */
ResqPeerNet.setupCommonListeners = function() {
	var self = this;
	
	// System monitoring events
	this.on('batteryStatusSuccess', function(data) {
		console.log('Battery status updated:', data.result.level + '%');
	});
	
	this.on('networkStatusSuccess', function(data) {
		console.log('Network status:', data.result.type, data.result.isConnected ? 'Connected' : 'Disconnected');
	});
	
	this.on('getCurrentLocationSuccess', function(data) {
		console.log('Location obtained:', data.result.latitude + ', ' + data.result.longitude);
	});
	
	this.on('captureImageSuccess', function(data) {
		console.log('Image captured successfully:', data.result.mediaFiles);
	});
	
	this.on('bluetoothStatusSuccess', function(data) {
		console.log('Bluetooth status:', data.result.enabled ? 'Enabled' : 'Disabled');
	});
	
	// App Discovery Events
	this.on('app_installed', function(event) {
		console.log('App Installed:', event.packageName, event.appInfo.name);
		// Auto-refresh app list if needed
		ResqPeerNet.trigger('app_list_updated', { action: 'install', app: event });
	});
	
	this.on('app_uninstalled', function(event) {
		console.log('App Uninstalled:', event.packageName);
		// Auto-refresh app list if needed  
		ResqPeerNet.trigger('app_list_updated', { action: 'uninstall', app: event });
	});
	
	this.on('app_updated', function(event) {
		console.log('App Updated:', event.packageName, event.appInfo.name);
	});
	
	// NEW LISTENERS FOR ADDED CATEGORIES
	this.on('fileReadSuccess', function(data) {
		console.log('File read completed:', data.result.filePath);
	});
	
	this.on('testComplete', function(data) {
		console.log('Test completed:', data.testName, data.result);
	});
	
	this.on('pluginReady', function(data) {
		console.log('ResqPeerNet plugin ready - Version', data.version);
	});
	
	// Error events
	this.on('Error', function(data) {
		console.error('ResqPeerNet Error:', data.method, data.error);
	});
	
	console.log('Common event listeners setup completed');
};

// Initialize event system when plugin loads
ResqPeerNet._initializeEventSystem();

// Register with cordova
if (typeof cordova !== 'undefined') {
	cordova.addConstructor(function() {
		if (!window.plugins) {
			window.plugins = {};
		}
		window.plugins.ResqPeerNet = ResqPeerNet;
		
		// Also make it available globally for easier access
		window.ResqPeerNet = ResqPeerNet;
		
		console.log('ResqPeerNet plugin JavaScript bridge initialized - Version 1.0.0 with Enhanced Event System');
		
		// Setup common listeners automatically
		setTimeout(function() {
			ResqPeerNet.setupCommonListeners();
		}, 1000);
	});
} else {
	console.warn('Cordova not available - ResqPeerNet plugin will not work');
}

// Export for module systems
if (typeof module !== 'undefined' && module.exports) {
	module.exports = ResqPeerNet;
}
