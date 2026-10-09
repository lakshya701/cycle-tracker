if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("/sw.js").catch(() => {
    // Installing as an app is optional; the site works without it.
  });
}
