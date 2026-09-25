const CACHE = "rvh-shell-v9";
const SHELL = [
  "/",
  "/app.js",
  "/studio-ui.js",
  "/advertisement-ui.js",
  "/discoverability-ui.js",
  "/video-options.js",
  "/devanagari.ttf",
  "/styles.css",
  "/manifest.webmanifest",
  "/icon.svg",
  "/icon-192.png",
  "/icon-512.png",
];
self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(SHELL)));
  // Activate this version immediately instead of waiting for every open tab
  // to close first, so a deploy takes effect on the next reload, not the
  // next time the browser happens to fully release the old worker.
  self.skipWaiting();
});
self.addEventListener("activate", (event) =>
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((k) => k.startsWith("rvh-shell-") && k !== CACHE)
            .map((k) => caches.delete(k)),
        ),
      )
      .then(() => self.clients.claim()),
  ),
);
self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  if (
    event.request.method !== "GET" ||
    url.origin !== self.location.origin ||
    url.pathname.startsWith("/api/")
  )
    return;
  if (!SHELL.includes(url.pathname)) return;
  event.respondWith(
    fetch(event.request).catch(() => caches.match(url.pathname)),
  );
});
