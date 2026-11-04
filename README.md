# WebLauncher

A minimal Android home screen (launcher) that renders a web page as its
desktop. The app embeds a WebView and a local HTTPS server; any web page —
local or remote — can become your launcher UI and talk to the device through
a small HTTP API (list apps, launch them, open webpages).

- **minSdk** 26 (Android 8.0) · **targetSdk** 36
- Only permission: `INTERNET`
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

List launchable applications. Requires permission + header.

`200` → `application/json`:

```json
[
  { "name": "Firefox", "package": "org.mozilla.firefox", "activity": "org.mozilla.firefox.App" }
]
```

### `GET /api/icon?package=<pkg>&activity=<act>&size=<px>&token=<token>`

Rendered launcher icon as PNG. Requires the `token` from
`request-permission` (no header check — safe for `<img src>`).

| Param | Required | Notes |
|---|---|---|
| `package` | yes | package name |
| `activity` | yes | activity name |
| `size` | no | pixel size, default `128` |
| `token` | yes | from `request-permission` |

`403` if the token is missing/invalid; `404` if the icon cannot be resolved.

### `POST /api/launch`
```
Headers:  X-Requested-With: WebLauncher
          Content-Type: application/json
Body:     {"package": "...", "activity": "..."}
```

Start an activity (launched the same way a real home screen does:
`ACTION_MAIN` + `CATEGORY_LAUNCHER` + `NEW_TASK`). Requires permission +
header. `200` on success, `500` if the system refused to start it.

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

### CORS

Full CORS support: the server echoes `Origin`, reflects preflight
methods/headers, and answers Private Network Access prechecks with
`Access-Control-Allow-Private-Network: true`, so cross-origin calls from
public HTTPS pages work in modern Chromium.

### Known limitations

- Only apps of the current Android user are listed/launchable: **work
  profile and cloned (dual) apps do not appear**. Full support would need
  the `LauncherApps` API and a `user` field in the protocol.
- Icons are rendered per request (no cache yet).

## JavaScript client

[`android-web-launcher`](https://www.npmjs.com/package/android-web-launcher)
([source](https://github.com/Dreagonmon/web-launcher)) wraps the whole API,
including port discovery, the header and token handling, and the POST/JSON
protocol (use v0.4.0+ for current servers):

```js
import { Launcher } from "android-web-launcher";

const launcher = new Launcher();               // https://127.0.0.1:10801/api/
await launcher.requestPermission(launcher.PERMISSION_LAUNCHER);
const apps = await launcher.getApplicationList();
for (const app of apps) {
  img.src = launcher.getApplicationIconSrc(app.package, app.activity);
  img.onclick = () => launcher.launchApplication(app.package, app.activity);
}
```

A dependency-free copy lives in
[`app/src/main/assets/wallpaper/simple/js/launcher.js`](app/src/main/assets/wallpaper/simple/js/launcher.js)
(the default desktop uses it; feel free to vendor it into your own page).

Example desktops:

- bundled "simple" page — served at `/index.html` by the app itself
- [paper-desktop](https://github.com/Dreagonmon/paper-desktop) — a Vue-based
  live wallpaper desktop, published at
  <https://dreagonmon.github.io/paper-desktop/>

## Sidebar

Swipe from the left edge (a small handle marks the drawer) to access:

- reload / open the default page
- set the home page URL (must be `https://…`)
- set the local server port (10000–65535)
- open the app-list permission settings
- app list permission management

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
