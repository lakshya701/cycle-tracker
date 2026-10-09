// Service worker: makes the app installable and caches static files (CSS, icons).
// Pages with personal data are never cached — they always come from the server.
const CACHE = "cycle-tracker-v1";
const STATIC_FILES = ["/css/app.css", "/icons/icon.svg", "/icons/icon-192.png", "/icons/icon-512.png", "/manifest.json"];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(STATIC_FILES)));
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
  );
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  if (event.request.method !== "GET" || url.origin !== location.origin || !STATIC_FILES.includes(url.pathname)) {
    return; // Let the browser handle it normally (network only).
  }
  event.respondWith(caches.match(event.request).then((cached) => cached || fetch(event.request)));
});
