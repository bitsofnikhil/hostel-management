(() => {
  const isSecure = location.protocol === 'https:' || location.hostname === 'localhost' || location.hostname === '127.0.0.1';
  if ('serviceWorker' in navigator && isSecure) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/sw.js').catch(err => console.warn('Service worker registration failed:', err));
    });
  }

  let deferredPrompt = null;
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    deferredPrompt = event;
    const btn = document.getElementById('installPwaBtn');
    if (btn) btn.hidden = false;
  });

  window.installPwa = async function () {
    if (!deferredPrompt) {
      alert('Install option is available from the browser menu after the app is served from localhost or HTTPS.');
      return;
    }
    deferredPrompt.prompt();
    await deferredPrompt.userChoice;
    deferredPrompt = null;
    const btn = document.getElementById('installPwaBtn');
    if (btn) btn.hidden = true;
  };
})();
