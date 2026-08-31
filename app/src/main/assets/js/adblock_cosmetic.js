if (!window._ytAdBlockInitialized) {
    window._ytAdBlockInitialized = true;

    const style = document.createElement('style');
    style.innerHTML = `
        .ad-container, 
        .ad-showing, 
        .ad-interrupting, 
        ytm-promoted-sparkles-web-renderer, 
        ytd-promoted-video-renderer, 
        ytm-companion-ad-renderer, 
        .video-ads, 
        .ytp-ad-overlay-container, 
        .ytp-ad-message-container, 
        ytd-banner-promo-renderer, 
        #masthead-ad, 
        .ytd-mealbar-promo-renderer {
            display: none !important;
        }
    `;
    document.head.appendChild(style);

    setInterval(() => {
        const player = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
        if (player && (player.classList.contains('ad-showing') || player.classList.contains('ad-interrupting'))) {
            const video = document.querySelector('video');
            if (video) {
                video.muted = true;
                video.playbackRate = 16.0;
                if (!isNaN(video.duration)) {
                    video.currentTime = video.duration;
                }
            }
            const skipButtons = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytm-ad-skip-button');
            skipButtons.forEach(btn => btn.click());
            
            const skipSlotButtons = document.querySelectorAll('.ytp-ad-skip-button-slot button');
            skipSlotButtons.forEach(btn => btn.click());
        }
    }, 250);
}
