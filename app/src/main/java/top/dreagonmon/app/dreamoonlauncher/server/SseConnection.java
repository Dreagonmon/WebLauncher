package top.dreagonmon.app.dreamoonlauncher.server;

import org.json.JSONObject;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

import top.dreagonmon.app.dreamoonlauncher.utils.EventBus;

/**
 * 一条 SSE 订阅：把 EventBus 的事件按 text/event-stream 帧写给一个
 * PipedOutputStream（对端由 NanoHTTPD 以 chunked 响应持续发出）。
 * 客户端断开时 pipe 写入抛 IOException，自清理并退订。
 *
 * 协议：event: <name>\ndata: <json>\n\n；每 15s 发一行 ": ping" 注释心跳，
 * 防中间层空闲超时掐线，同时让 EventSource 感知连接存活。
 */
public class SseConnection implements EventBus.Sink {
    private static final long HEARTBEAT_MS = 15_000;

    private final EventBus bus;
    private final PipedOutputStream out;
    private final Thread writer;
    private volatile boolean alive = true;
    private long subscriptionId = -1;

    /** 返回给 NanoHTTPD 的流（对端是 out）。 */
    public final PipedInputStream input;

    public SseConnection(EventBus bus) throws IOException {
        this.bus = bus;
        this.out = new PipedOutputStream();
        this.input = new PipedInputStream(this.out, 8192);
        this.writer = new Thread(this::loop, "sse-writer");
        this.writer.setDaemon(true);
    }

    public void start() {
        subscriptionId = bus.subscribe(this);
        writer.start();
    }

    private void loop() {
        long lastWrite = System.currentTimeMillis();
        while (alive) {
            try {
                Thread.sleep(500);
                if (System.currentTimeMillis() - lastWrite >= HEARTBEAT_MS) {
                    writeRaw(": ping\n\n");
                    lastWrite = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                break; // 客户端断开
            }
        }
        close();
    }

    private synchronized void writeRaw(String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    @Override
    public void onEvent(String name, JSONObject payload) {
        if (!alive) return;
        try {
            // id 行让 EventSource 断线重连时能带 Last-Event-ID（将来可用）
            writeRaw("event: " + name + "\ndata: " + payload + "\n\n");
        } catch (IOException e) {
            alive = false;
        }
    }

    /** 服务器感知连接结束时（Response.close → input.close）退订并停写。幂等。 */
    public void close() {
        alive = false;
        if (subscriptionId >= 0) {
            bus.unsubscribe(subscriptionId);
            subscriptionId = -1;
        }
        try {
            out.close();
        } catch (IOException ignore) {
        }
        writer.interrupt();
    }
}
