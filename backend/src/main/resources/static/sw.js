const CACHE_NAME = "hostel-management-pwa-v9-room-filters-fast-login";
const STATIC_ASSETS = [
  "/",
  "/index.html",
  "/login.html",
  "/dashboard.html",
  "/attendance.html",
  "/students.html",
  "/rooms.html",
  "/mess.html",
  "/complaints.html",
  "/fees.html",
  "/offline.html",
  "/css/style.css",
  "/js/app.js",
  "/manifest.webmanifest",
  "/icons/icon-72.png",
  "/icons/icon-144.png",
  "/icons/icon-192.png",
  "/icons/icon-512.png"
];

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(STATIC_ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys().then(keys => Promise.all(
      keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key))
    )).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", event => {
  const request = event.request;
  const url = new URL(request.url);

  if (request.method !== "GET") return;

  // Do not cache API calls: attendance/student/menu changes must hit the server.
  if (url.pathname.startsWith("/api/")) {
    event.respondWith(fetch(request));
    return;
  }

  // Pages: network first, fallback to cache, then offline screen.
  if (request.mode === "navigate" || url.pathname.endsWith(".html") || url.pathname === "/") {
    event.respondWith(
      fetch(request)
        .then(response => {
          const copy = response.clone();
          caches.open(CACHE_NAME).then(cache => cache.put(request, copy));
          return response;
        })
        .catch(() => caches.match(request).then(cached => cached || caches.match("/offline.html")))
    );
    return;
  }

  // Static assets: cache first, update in background.
  event.respondWith(
    caches.match(request).then(cached => {
      const networkFetch = fetch(request).then(response => {
        const copy = response.clone();
        caches.open(CACHE_NAME).then(cache => cache.put(request, copy));
        return response;
      }).catch(() => cached);
      return cached || networkFetch;
    })
  );
});
