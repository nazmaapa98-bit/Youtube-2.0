// Background Playback & Visibility Spoofing
// Prevents YouTube from detecting screen off or app minimizing
try {
    Object.defineProperty(document, 'hidden', {
        get: function() { return false; },
        configurable: true
    });
    Object.defineProperty(document, 'visibilityState', {
        get: function() { return 'visible'; },
        configurable: true
    });
    Object.defineProperty(document, 'webkitVisibilityState', {
        get: function() { return 'visible'; },
        configurable: true
    });

    // Intercept and stop propagation of visibilitychange, blur, and pagehide events
    ['visibilitychange', 'webkitvisibilitychange', 'blur', 'pagehide'].forEach(function(evt) {
        window.addEventListener(evt, function(e) {
            e.stopImmediatePropagation();
        }, true);
        document.addEventListener(evt, function(e) {
            e.stopImmediatePropagation();
        }, true);
    });
} catch(e) {
    console.error('Error spoofing visibility:', e);
}

if (!window._ytBridgeInitialized) {
    window._ytBridgeInitialized = true;

    function notifyVideoChanged() {
        if (!window.AndroidBridge) return;

        const url = window.location.href;
        let videoId = null;

        if (url.includes('/watch?v=')) {
            videoId = new URLSearchParams(window.location.search).get('v');
        } else if (url.includes('/shorts/')) {
            videoId = url.split('/shorts/')[1].split('?')[0];
        } else if (url.includes('/embed/')) {
            videoId = url.split('/embed/')[1].split('?')[0];
        }

        if (videoId) {
            let title = '';
            let duration = -1;
            const moviePlayer = document.getElementById('movie_player');
            
            if (moviePlayer && typeof moviePlayer.getVideoData === 'function') {
                const data = moviePlayer.getVideoData();
                if (data) title = data.title;
            }
            if (!title) {
                const titleEl = document.querySelector('h2.slim-video-metadata-title') || document.querySelector('h1.title');
                if (titleEl) title = titleEl.textContent.trim();
            }
            if (!title) {
                const metaTitle = document.querySelector('meta[name="title"]');
                if (metaTitle) title = metaTitle.content;
            }
            if (!title) {
                title = document.title;
            }

            if (moviePlayer && typeof moviePlayer.getDuration === 'function') {
                duration = moviePlayer.getDuration();
            } else {
                const video = document.querySelector('video');
                if (video && video.duration) {
                    duration = video.duration;
                }
            }

            window.AndroidBridge.onVideoDetected(videoId, url, title, duration);
        }
    }

    window.addEventListener('yt-navigate-finish', notifyVideoChanged);
    window.addEventListener('yt-page-data-updated', notifyVideoChanged);
    window.addEventListener('popstate', notifyVideoChanged);

    const originalPushState = history.pushState;
    history.pushState = function() {
        originalPushState.apply(this, arguments);
        setTimeout(notifyVideoChanged, 300);
    };

    const originalReplaceState = history.replaceState;
    history.replaceState = function() {
        originalReplaceState.apply(this, arguments);
        setTimeout(notifyVideoChanged, 300);
    };

    setTimeout(notifyVideoChanged, 1000);
}
