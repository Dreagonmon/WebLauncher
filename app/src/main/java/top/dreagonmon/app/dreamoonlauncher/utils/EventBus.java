package top.dreagonmon.app.dreamoonlauncher.utils;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 极简进程内事件总线：事件源（LauncherApps 回调等）广播，订阅者（SSE 连接等）消费。
 * 事件名对应当前只有 apps-changed；后续事件直接复用同一总线与同一条 SSE 连接。
 */
public class EventBus {
    public interface Sink {
        void onEvent(String name, JSONObject payload);
    }

    private final Map<Long, Sink> sinks = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public long subscribe(Sink sink) {
        long id = nextId.getAndIncrement();
        sinks.put(id, sink);
        return id;
    }

    public void unsubscribe(long id) {
        sinks.remove(id);
    }

    public void broadcast(String name, JSONObject payload) {
        for (Sink s : sinks.values()) {
            try {
                s.onEvent(name, payload);
            } catch (Exception ignore) {
                // 单个订阅者异常不影响其它订阅者
            }
        }
    }
}
