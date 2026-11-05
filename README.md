# WebLauncher

A minimal Android home screen (launcher) that renders a web page as its
desktop. The app embeds a WebView and a local HTTPS server; any web page —
local or remote — can become your launcher UI and talk to the device through
a small HTTP API (list apps, launch them, open webpages).

- **minSdk** 26 (Android 8.0) · **targetSdk** 36
- Permissions: `INTERNET`, `READ_INSTALLED_APPS` (app-change events),
  `INTERACT_ACROSS_PROFILES` (granted to home-screen roles; harmless
  otherwise)
- No Google services, no tracking, no native binaries

## How it works

```
┌─────────────────────────── WebLauncher app ───────────────────────────┐
│                                                                       │
│  WebView (the "desktop")          Local HTTPS server (NanoHTTPD)      │
│  ┌──────────────────┐             ┌─────────────────────────────┐     │
│  │ home page        │  fetch()    │ /api/*        launcher API  │     │
│  │ (default: assets │ ──────────► │ /*            static assets │     │
│  │  simple desktop) │  ◄────────  │ (serves the bundled pages)  │     │
│  └──────────────────┘             └─────────────────────────────┘     │
└───────────────────────────────────────────────────────────────────────┘
```

- The app starts an HTTPS server on `https://127.0.0.1:<port>`
  (default port `10801`, changeable in the sidebar).
- The WebView loads the configured home page. The default page is served by
  the local server itself (`https://127.0.0.1:<port>/index.html`), so it is
  same-origin with the API.
- Any remote page (e.g. a GitHub Pages site) can also be set as the home
  page and use the API cross-origin.
- The self-signed certificate is trusted app-wide via Network Security
  Config, so Service Workers and other platform-network paths work too.

## Launcher API

Base URL: `https://127.0.0.1:<port>/api/`

> **Client compatibility:** this protocol (POST + JSON action endpoints,
> `X-Requested-With` header, icon token, required `user` field, SSE)
> matches **`android-web-launcher` v0.5.0+**. Older client versions speak
> the legacy protocol and will get `405`/`403`/`400` from current builds.

### Discovering the port

The server port is user-configurable (default `10801`), so pages should not
hard-code it. The WebView's User-Agent is extended with:

```
... WebLauncher/<versionCode> (port <port>)
```

Pages running inside WebLauncher can parse the port from
`navigator.userAgent` (regex: `/WebLauncher\/\d*? \(port (\d*?)\)/`), and
fall back to `10801` when the marker is absent (external browsers).
`android-web-launcher` does exactly this in its constructor; you can also
pass the port explicitly.

### Authentication model

Each web origin must be granted access by the user (a native confirmation
dialog appears on first use). Two defenses apply:

1. **`X-Requested-With: WebLauncher` header is required** on all
   permission-checked endpoints. Browsers silently strip custom headers from
   "simple" requests (`no-cors` fetch, `<img>`, iframe), which closes the
   CSRF hole where such requests carry no `Origin` and would otherwise be
   attributed to the local origin.
2. **Icon requests use a token** instead: `<img>` cannot send custom headers,
   so `request-permission` returns a per-origin token in its response body,
   and `/api/icon` requires it as a query parameter.

Requests from a new origin trigger a confirmation dialog on the device;
denied origins get `403`. Grants are remembered per origin.

Action endpoints (`request-permission`, `launch`, `open-webpage`) are
**POST-only** and take a **JSON request body**; GET returns `405`. This is
not just REST hygiene: a POST fetch always carries a real `Origin` (through
the CORS preflight), keeping permission attribution honest.

### `POST /api/request-permission`
```
Headers:  X-Requested-With: WebLauncher
          Content-Type: application/json
Body:     {"name": "launcher"}
```

Ask the user to grant permission `launcher` to the calling origin.

| Status | Meaning |
|---|---|
| `200` | Granted. **Body is the origin's icon-auth token** (a 32-char hex string). |
| `403` | Denied by the user (or missing header). |
| `405` | Wrong method (use POST). |

### `GET /api/app-list-all`
```
Headers:  X-Requested-With: WebLauncher
```

List launchable applications across the current user and all accessible
profiles (work profile, MIUI app clones). Requires permission + header.

`200` → `application/json`:

```json
[
  { "name": "Firefox", "package": "org.mozilla.firefox", "activity": "org.mozilla.firefox.App", "user": "0" }
]
```

`user` is the Android user's **serial number** as a string (`"0"` is the
owner user; profiles such as work profiles or clones have their own
serials). Pass it back to `launch` / `icon` to target that profile.

### `GET /api/icon?package=<pkg>&activity=<act>&user=<serial>&size=<px>&token=<token>`

Rendered launcher icon as PNG. Requires the `token` from
`request-permission` (no header check — safe for `<img src>`).

| Param | Required | Notes |
|---|---|---|
| `package` | yes | package name |
| `activity` | yes | activity name |
| `user` | yes | user serial from `app-list-all`; profile icons carry a work badge |
| `size` | no | pixel size, default `128` |
| `token` | yes | from `request-permission` |

`400` if `user` is missing; `403` if the token is missing/invalid; `404`
if the icon cannot be resolved.

### `POST /api/launch`
```
Headers:  X-Requested-With: WebLauncher
          Content-Type: application/json
Body:     {"package": "...", "activity": "...", "user": "..."}
```

Start an activity via `LauncherApps.startMainActivity` (the same path a
real home screen uses), on the profile given by `user` (user serial from
`app-list-all`; required). Requires permission + header. `200` on
success; `400` if `user` is missing; `500` with the failure reason as
body if the system refused to start it.

### `GET /api/events?token=<token>`
```
Response: text/event-stream (Server-Sent Events)
```

Live event stream over a single long-lived connection. Requires the
`token` as a query parameter (EventSource cannot send custom headers).
Each event is `event: <name>` + `data: <json>`; a `: ping` comment is
sent every 15 s as keep-alive. Currently defined events:

| Event | Payload | When |
|---|---|---|
| `apps-changed` | `{"reason": "..."}` | packages added/removed/changed on any visible user, or a profile was added/removed (API 31+); debounced 1 s |

Future events reuse the same connection. `EventSource` reconnects
automatically on drop.

### `POST /api/open-webpage`
```
Headers:  X-Requested-With: WebLauncher
          Content-Type: application/json
Body:     {"href": "https://..."}
```

Open a URL in the system browser. Requires permission + header.
`200` on success.

### Method summary

| Endpoint | Method | Params |
|---|---|---|
| `request-permission` | POST | JSON body |
| `app-list-all` | GET | — |
| `icon` | GET | query (incl. `token`) |
| `launch` | POST | JSON body |
| `open-webpage` | POST | JSON body |
| `events` | GET | query `token` (SSE stream) |
| `test-event` | POST | — (diagnostic) |

`test-event` broadcasts a synthetic `apps-changed {"reason":"test"}` to all
SSE subscribers — handy for debugging the event pipeline from the bundled
simple page.

### CORS

Full CORS support: the server echoes `Origin`, reflects preflight
methods/headers, and answers Private Network Access prechecks with
`Access-Control-Allow-Private-Network: true`, so cross-origin calls from
public HTTPS pages work in modern Chromium.

### Known limitations

- **Cloned (dual) apps on MIUI/HyperOS**: they appear in the list as
  profile users and launch via `LauncherApps`, but MIUI injects its own
  "main app or clone?" chooser for *any* launch of a cloned package —
  including ours. This is a system-level interceptor with no public
  bypass; work-profile launches are not affected (they start directly).
- Profile add/remove events rely on `LauncherApps.Callback.onUserConfigChanged`
  (API 31+); on Android 8–10 they are not delivered (`onProfilesChanged` was
  removed before a replacement existed). Frontends should also refresh on
  `visibilitychange` — the app forwards its lifecycle to the WebView
  (`WebView.onPause/onResume`), so the standard Page Visibility API
  works inside the built-in WebView.
- Icons are rendered per request (no cache yet).

## JavaScript client

[`android-web-launcher`](https://www.npmjs.com/package/android-web-launcher)
([source](https://github.com/Dreagonmon/web-launcher)) wraps the whole API,
including port discovery, the header and token handling, the POST/JSON
protocol, the `user` field and the SSE stream (use v0.5.0+ for current
servers):

```js
import { Launcher } from "android-web-launcher";

const launcher = new Launcher();               // https://127.0.0.1:10801/api/
await launcher.requestPermission(launcher.PERMISSION_LAUNCHER);
const apps = await launcher.getApplicationList();
for (const app of apps) {                      // app.user: profile serial
  img.src = launcher.getApplicationIconSrc(app.package, app.activity, 128, app.user);
  img.onclick = () => launcher.launchApplication(app.package, app.activity, app.user);
}
const unsubscribe = launcher.subscribeEvents((name, payload) => {
  if (name === "apps-changed") refreshAppList();
});
```

A dependency-free copy lives in
[`app/src/main/assets/wallpaper/simple/js/launcher.js`](app/src/main/assets/wallpaper/simple/js/launcher.js)
(the default desktop uses it; feel free to vendor it into your own page).

Example desktops:

- bundled pages, served by the app itself: `/index.html` (default portal
  page) and `/simple.html` (a minimal launcher example built on the
  bundled client, with SSE diagnostics)
- [paper-desktop](https://github.com/Dreagonmon/paper-desktop) — a Vue-based
  live wallpaper desktop, published at
  <https://dreagonmon.github.io/paper-desktop/>

## Sidebar

Swipe from the left edge (a small handle marks the drawer) to access:

- set the home page URL (must be `https://…`)
- set the local server port (10000–65535)
- reset all origin permissions
- open the system app-list permission settings
- restart the launcher

## Building

Requirements: JDK 17, Android SDK 36, Gradle (wrapper included).

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Notes:

- `app/src/main/assets/server.bks` holds the HTTPS keystore (alias `launcher`,
  password `123456`); the matching certificate is trusted via
  `network_security_config.xml`. If you re-sign the certificate, update both
  `server.bks` and `res/raw/server.pem`.
- NanoHTTPD is vendored as source under `app/src/main/java/org/nanohttpd`.

## Related repositories

| Repo | Purpose |
|---|---|
| [web-launcher](https://github.com/Dreagonmon/web-launcher) | `android-web-launcher` npm package (JS API client) |
| [paper-desktop](https://github.com/Dreagonmon/paper-desktop) | example desktop UI (Vue 2) |

## License

MIT © Dreagonmon
