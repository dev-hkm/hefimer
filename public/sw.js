const CACHE_NAME = "hefimer-cache-v7";
const PRECACHE_ASSETS = [
  "/",
  "/index.html",
  "/manifest.json",
  "/hefimer-orbit.svg",
  "/hefimer-orbit.png"
];

// Install event - cache precache assets and skip waiting immediately
self.addEventListener("install", (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(PRECACHE_ASSETS);
    })
  );
});

// Activate event - clean up old caches and claim clients
self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    }).then(() => {
      return self.clients.claim();
    })
  );
});

// Fetch event with Network-First for HTML/Navigation, Cache-First for assets
self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;

  const url = new URL(event.request.url);

  // 1. Network-First strategy for HTML / navigation requests (prevent cache lock-in)
  if (
    event.request.mode === "navigate" ||
    url.pathname === "/" ||
    url.pathname === "/index.html"
  ) {
    event.respondWith(
      fetch(event.request)
        .then((networkResponse) => {
          if (networkResponse.status === 200) {
            const responseToCache = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => {
              cache.put(event.request, responseToCache);
            });
          }
          return networkResponse;
        })
        .catch(() => {
          // Offline fallback
          return caches.match(event.request);
        })
    );
    return;
  }

  // 2. Cache-First strategy for static assets, scripts, styles, and icons
  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      if (cachedResponse) {
        return cachedResponse;
      }
      return fetch(event.request)
        .then((networkResponse) => {
          // Cache successful responses from our own origin
          if (
            networkResponse.status === 200 &&
            url.origin === self.location.origin
          ) {
            // CRITICAL: NEVER cache API calls or dynamic routes
            if (!url.pathname.startsWith("/api/")) {
              const responseToCache = networkResponse.clone();
              caches.open(CACHE_NAME).then((cache) => {
                cache.put(event.request, responseToCache);
              });
            }
          }
          return networkResponse;
        })
        .catch(() => {
          // Return nothing if both offline and not cached
        });
    })
  );
});
