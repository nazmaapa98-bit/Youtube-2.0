// Prevent YouTube background suspension when in Audio Mode
window._ytAudioBackgroundMode = false;

(function() {
    const originalPause = HTMLMediaElement.prototype.pause;
    HTMLMediaElement.prototype.pause = function() {
        if (window._ytAudioBackgroundMode && !this.ended) {
            // When user explicitly requested audio background mode, ignore OS background blur pauses
            console.log('Ignored pause call during background audio mode');
            return;
        }
        return originalPause.apply(this, arguments);
    };
})();

function ytPauseVideo() {
    try {
        window._ytAudioBackgroundMode = false;
        const moviePlayer = document.getElementById('movie_player');
        if (moviePlayer && typeof moviePlayer.pauseVideo === 'function') {
            moviePlayer.pauseVideo();
        } else {
            const video = document.querySelector('video');
            if (video) HTMLMediaElement.prototype.pause.call(video);
        }
    } catch (e) {
        console.error('Error pausing video:', e);
    }
}

function ytPlayVideo() {
    try {
        const moviePlayer = document.getElementById('movie_player');
        if (moviePlayer && typeof moviePlayer.playVideo === 'function') {
            moviePlayer.playVideo();
        } else {
            const video = document.querySelector('video');
            if (video) video.play();
        }
    } catch (e) {
        console.error('Error playing video:', e);
    }
}

function ytGetCurrentTime() {
    try {
        let time = 0;
        const moviePlayer = document.getElementById('movie_player');
        if (moviePlayer && typeof moviePlayer.getCurrentTime === 'function') {
            time = moviePlayer.getCurrentTime();
        } else {
            const video = document.querySelector('video');
            if (video) time = video.currentTime;
        }
        if (window.AndroidBridge && typeof window.AndroidBridge.onCurrentTime === 'function') {
            window.AndroidBridge.onCurrentTime(time);
        }
        return time;
    } catch (e) {
        console.error('Error getting current time:', e);
        return 0;
    }
}

function ytSeekTo(seconds) {
    try {
        const moviePlayer = document.getElementById('movie_player');
        if (moviePlayer && typeof moviePlayer.seekTo === 'function') {
            moviePlayer.seekTo(seconds, true);
        } else {
            const video = document.querySelector('video');
            if (video) video.currentTime = seconds;
        }
    } catch (e) {
        console.error('Error seeking:', e);
    }
}

function ytSetLowQuality() {
    try {
        window._ytAudioBackgroundMode = true;
        const moviePlayer = document.getElementById('movie_player');
        if (moviePlayer && typeof moviePlayer.setPlaybackQualityRange === 'function') {
            moviePlayer.setPlaybackQualityRange('tiny', 'tiny');
        }
    } catch (e) {
        console.error('Error setting low quality:', e);
    }
}

function ytSetPipFullscreen(enable) {
    try {
        const video = document.querySelector('video');
        if (video) {
            if (enable) {
                video.style.position = 'fixed';
                video.style.top = '0';
                video.style.left = '0';
                video.style.width = '100vw';
                video.style.height = '100vh';
                video.style.zIndex = '9999999';
                video.style.objectFit = 'contain';
                video.style.backgroundColor = '#000';
            } else {
                video.style.position = '';
                video.style.top = '';
                video.style.left = '';
                video.style.width = '';
                video.style.height = '';
                video.style.zIndex = '';
                video.style.objectFit = '';
                video.style.backgroundColor = '';
            }
        }
    } catch (e) {
        console.error('Error adjusting PiP layout:', e);
    }
}

function ytGetPlaybackState() {
    try {
        let state = {
            isPlaying: false,
            currentTime: 0,
            duration: 0,
            videoId: ''
        };
        
        const moviePlayer = document.getElementById('movie_player');
        if (moviePlayer) {
            if (typeof moviePlayer.getPlayerState === 'function') {
                state.isPlaying = moviePlayer.getPlayerState() === 1;
            }
            if (typeof moviePlayer.getCurrentTime === 'function') {
                state.currentTime = moviePlayer.getCurrentTime();
            }
            if (typeof moviePlayer.getDuration === 'function') {
                state.duration = moviePlayer.getDuration();
            }
            if (typeof moviePlayer.getVideoData === 'function') {
                const data = moviePlayer.getVideoData();
                if (data) state.videoId = data.video_id;
            }
        }
        
        return JSON.stringify(state);
    } catch (e) {
        console.error('Error getting playback state:', e);
        return JSON.stringify({});
    }
}
