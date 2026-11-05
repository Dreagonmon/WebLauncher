"use strict";
// global class
class ApplicationInfo {
  name = "";
  package = "";
  activity = "";
  user = ""; // 用户 serial number（工作资料/分身），主应用也有值
}
class LauncherResponseError extends Error {
  constructor (msg) {
    super("LauncherResponseError: " + msg);
  }
}

// fake server
class Random {
  seed;
  constructor (seed = Math.PI * Math.PI) {
    this.seed = seed;
  };

  nextInt (min, max) {
    const rand = Number.parseFloat("0." + Math.sin(this.seed++).toString().substr(6));
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(rand * (max - min)) + min;
  };
}
const ICON_SIZE = 128;
const WORD_LIST = [
  "otren", "nlttvwrarc", "fhoiaii", "eehednt", "xeomlea", "ennsheey", "msapeg", "etauwilash",
  "ytfi", "hosodad", "cemehn", "inra", "oeeow", "dubeto", "mauetehs", "deetrtre",
  "lelsiid", "nkghs", "admiat", "iadh", "hoyu", "mhyiiltaafm", "hrwjaw",
];
const COLOR_LIST = [
  "#f44336", "#e91e63", "#e91e63", "#7e57c2", "#3f51b5", "#2196f3", "#03a9f4", "#00bcd4",
  "#009688", "#4caf50", "#8bc34a", "#cddc39", "#ffeb3b", "#ffc107", "#ff9800", "#ff5722",
  // "#795548", "#9e9e9e", "#9e9e9e",
];
const getRandomWord = (random) => {
  const pos = random.nextInt(0, WORD_LIST.length);
  return WORD_LIST[pos];
};
const getRandomColor = (random) => {
  const pos = random.nextInt(0, COLOR_LIST.length);
  return COLOR_LIST[pos];
};
const toFirstUpperCase = (text) => {
  return text.replace(/^./u, text.charAt(0).toUpperCase());
};
const generateRandomAppList = (seed = Math.PI * Math.PI, size = 50) => {
  const apps = [];
  const colors = {};
  const r = new Random(seed);
  for (let i = 0; i < size; i++) {
    const length = r.nextInt(1, 4); // name word count [1, 4)
    const app = { name: "", activity: "", package: "" };
    for (let k = 0; k < length; k++) {
      const word = getRandomWord(r);
      app.name += toFirstUpperCase(word) + " ";
      app.package += word + ".";
    }
    app.activity = app.package + "MainActivity";
    app.name = app.name.substr(0, app.name.length - 1);
    app.package = app.package.substr(0, app.package.length - 1);
    const theme = [getRandomColor(r), getRandomColor(r), getRandomColor(r)];
    colors[app.package] = theme;
    apps.push(app);
  }
  return [apps, colors];
};
const [APP_LIST, APP_THEME] = generateRandomAppList();
class MockServer {
  permission = {}; // permission only request once
  hasLaunchPermission = () => {
    return !!this.permission.launcher;
  };

  getApplicationList = async () => {
    if (!this.hasLaunchPermission()) {
      console.error("launchApplication: You need request permission first!");
      return Promise.reject(new LauncherResponseError("No Permission"));
    }
    return APP_LIST;
  };

  getApplicationIconSrc = (packageName, activity, size = ICON_SIZE) => {
    let theme;
    if (packageName != null) {
      theme = APP_THEME[packageName];
    } else {
      const r = new Random(Math.random() * Number.MAX_SAFE_INTEGER);
      theme = [getRandomColor(r), getRandomColor(r), getRandomColor(r)];
    }
    const SVG_IMAGE = `<svg width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg">
    <g>
     <rect x="-1" y="-1" width="${size + 2}" height="${size + 2}" id="canvas_background" fill="none"/>
    </g>
    <g>
     <ellipse ry="${size / 2.2}" rx="${size / 2.2}" id="svg_3" cy="${size / 2}" cx="${size / 2}" stroke-width="0" stroke="#000" fill="${theme[0]}"/>
     <ellipse ry="${size / 3}" rx="${size / 3}" id="svg_3" cy="${size / 2}" cx="${size / 2}" stroke-width="0.5" stroke="#000" fill="${theme[1]}"/>
     <ellipse ry="${size / 5}" rx="${size / 5}" id="svg_3" cy="${size / 2}" cx="${size / 2}" stroke-width="0" stroke="#000" fill="${theme[2]}"/>
    </g>
    </svg>`;
    return "data:image/svg+xml;base64," + btoa(SVG_IMAGE);
  };

  launchApplication = async (packageName, activity) => {
    if (!this.hasLaunchPermission()) {
      console.error("launchApplication: You need request permission first!");
      return Promise.reject(new LauncherResponseError("No Permission"));
    }
    const frame = document.createElement("div");
    frame.style.width = "100%";
    frame.style.height = "100%";
    frame.style.background = "black";
    frame.style.position = "fixed";
    frame.style.top = 0;
    frame.style.right = 0;
    frame.style.bottom = 0;
    frame.style.left = 0;
    frame.style.textAlign = "center";
    frame.style.padding = "1em";
    frame.style.boxSizing = "border-box";
    frame.onclick = (event) => {
      event.preventDefault();
      event.stopPropagation();
      document.body.removeChild(frame);
    };
    const title = document.createElement("h1");
    title.innerText = "Application";
    title.style.color = "white";
    title.style.wordBreak = "break-all";
    frame.appendChild(title);
    const pkg = document.createElement("h3");
    pkg.innerText = packageName;
    pkg.style.color = "white";
    pkg.style.wordBreak = "break-all";
    frame.appendChild(pkg);
    const pkg2 = document.createElement("h3");
    pkg2.innerText = activity;
    pkg2.style.color = "white";
    pkg2.style.wordBreak = "break-all";
    frame.appendChild(pkg2);
    document.body.appendChild(frame);
    return true;
  };

  /**
   * 订阅服务端事件（SSE，单连接多事件类型）。
   * handler(eventType /* 如 "apps-changed" *\/, payload)
   * 返回取消订阅函数。EventSource 自带断线重连。
   */
  subscribeEvents = (handler, statusHandler) => {
    if (this.server) {
      return this.server.subscribeEvents(handler);
    }
    const es = new EventSource(`${this.address}events?token=${this.token}`);
    const status = (s) => { if (statusHandler) statusHandler(s); };
    es.onopen = () => status("open");
    es.onerror = () => {
      // readyState: 0 连接中 1 已连接(会自动重连) 2 已关闭
      status("error readyState=" + es.readyState);
    };
    const names = ["apps-changed"]; // 已知事件类型；新事件在服务端加入后在此登记
    const listeners = names.map((name) => {
      const fn = (e) => {
        let data = null;
        try { data = JSON.parse(e.data); } catch { /* 保留 null */ }
        handler(name, data);
      };
      es.addEventListener(name, fn);
      return [name, fn];
    });
    return () => {
      for (const [name, fn] of listeners) es.removeEventListener(name, fn);
      es.close();
    };
  };

  requestPermission = async (name) => {
    const NAME_MAP = {
      launcher: "Launcher",
    };    if (!["launcher"].includes(name)) {
      console.error("requestPermission: Permission name not support!");
      return Promise.reject(new LauncherResponseError("Permission name not support"));
    }
    if (this.permission[name]) {
      return true;
    }
    const result = confirm(`Do you want to grant ${NAME_MAP[name]} permission to ${window.location.origin} ?`);
    if (result) {
      this.permission[name] = true;
      return true;
    }
    return Promise.reject(new LauncherResponseError("Permission denided"));
  }

  subscribeEvents = () => () => {};
}

// launcher api
const getParameterByName = (url, name) => {
  name = name.replace(/[[\]]/g, "\\$&");
  var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)");
  var results = regex.exec(url);
  if (!results) return null;
  if (!results[2]) return "";
  return decodeURIComponent(results[2].replace(/\+/g, " "));
};
class Launcher {
  PERMISSION_LAUNCHER = "launcher";
  constructor (port = -1) {
    if (port < 0) {
      // try to get the port
      try {
        const re = /WebLauncher\/\d*? \(port (\d*?)\)/g;
        const portText = re.exec(navigator.userAgent)[1];
        port = Number.parseInt(portText);
        if (portText == null || port <= 0 || port > 65535) {
          port = 10801;
        }
      } catch {
        console.log("error, default port!", port);
        port = 10801;
      }
      console.log("using port:", port);
    }
    let protocol = window.location.protocol;
    if (protocol !== "http:") {
      protocol = "https:";
    }
    this.port = port;
    this.server = undefined;
    this.token = "";
    this.address = `${protocol}//127.0.0.1:${port}/api/`;
  };

  getApplicationList = async () => {
    if (this.server) {
      return await this.server.getApplicationList();
    }
    const url = `${this.address}app-list-all?t=${new Date().getTime()}`;
    const resp = await fetch(url, { headers: { "X-Requested-With": "WebLauncher" } });
    if (resp.status !== 200) {
      return Promise.reject(new LauncherResponseError("status code not 200!"));
    }
    const arrays = JSON.parse(await resp.text());
    const appList = arrays.map(item => {
      const app = new ApplicationInfo();
      app.name = item.name;
      app.package = item.package;
      app.activity = item.activity;
      app.user = item.user == null ? "" : String(item.user);
      return app;
    });
    return appList;
  };

  getApplicationIconSrc = (packageName, activity, size = 128, user = "") => {
    if (this.server) {
      return this.server.getApplicationIconSrc(packageName, activity, size);
    }
    const userQuery = user === "" || user == null ? "" : `&user=${encodeURIComponent(user)}`;
    return `${this.address}icon?package=${packageName}&activity=${activity}&size=${size}&token=${this.token}${userQuery}&t=${new Date().getTime()}`;
  };

  launchApplication = async (packageName, activity, user = "") => {
    if (this.server) {
      return await this.server.launchApplication(packageName, activity);
    }
    const url = `${this.address}launch`;
    const body = { package: packageName, activity: activity };
    if (user !== "" && user != null) body.user = String(user);
    const resp = await fetch(url, {
      method: "POST",
      headers: {
        "X-Requested-With": "WebLauncher",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });
    if (resp.status !== 200) {
      // 服务器 500 的 body 带失败原因（如跨 profile 启动被拒）
      let detail = "";
      try { detail = " " + (await resp.text()); } catch { /* ignore */ }
      return Promise.reject(new LauncherResponseError("status code not 200!" + detail));
    }
    return true;
  };

  openWebpage = async (url) => {
      if (this.server) {
        window.open(url, "_blank");
        return true;
      }
      const mURL = `${this.address}open-webpage`;
      const resp = await fetch(mURL, {
        method: "POST",
        headers: {
          "X-Requested-With": "WebLauncher",
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ href: url }),
      });
      if (resp.status !== 200) {
        return Promise.reject(new LauncherResponseError("status code not 200!"));
      }
      return true;
    };

  /**
   * 订阅服务端事件（SSE，单连接多事件类型）。
   * handler(eventType /* 如 "apps-changed" *\/, payload)
   * 返回取消订阅函数。EventSource 自带断线重连。
   */
  subscribeEvents = (handler, statusHandler) => {
    if (this.server) {
      return this.server.subscribeEvents(handler);
    }
    const es = new EventSource(`${this.address}events?token=${this.token}`);
    const status = (s) => { if (statusHandler) statusHandler(s); };
    es.onopen = () => status("open");
    es.onerror = () => {
      // readyState: 0 连接中 1 已连接(会自动重连) 2 已关闭
      status("error readyState=" + es.readyState);
    };
    const names = ["apps-changed"]; // 已知事件类型；新事件在服务端加入后在此登记
    const listeners = names.map((name) => {
      const fn = (e) => {
        let data = null;
        try { data = JSON.parse(e.data); } catch { /* 保留 null */ }
        handler(name, data);
      };
      es.addEventListener(name, fn);
      return [name, fn];
    });
    return () => {
      for (const [name, fn] of listeners) es.removeEventListener(name, fn);
      es.close();
    };
  };

  requestPermission = async (name) => {
    if (this.server) {
      return await this.server.requestPermission(name);
    }
    const url = `${this.address}request-permission`;
    const resp = await fetch(url, {
      method: "POST",
      headers: {
        "X-Requested-With": "WebLauncher",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ name: name }),
    });
    if (resp.status !== 200) {
      return Promise.reject(new LauncherResponseError("status code not 200!"));
    }
    // body 是 icon 鉴权用的 token（img 请求带不了自定义头）
    this.token = (await resp.text()).trim();
    if (this.token === "200 OK") this.token = ""; // 旧服务器无 token
    return true;
  };

  startDebugSession = () => {
    this.server = new MockServer();
  };
};

// export
 window['Launcher'] = Launcher;
 window['ApplicationInfo'] = ApplicationInfo;
 window['LauncherResponseError'] = LauncherResponseError;
//export { Launcher, ApplicationInfo, LauncherResponseError };
