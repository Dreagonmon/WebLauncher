package top.dreagonmon.app.dreamoonlauncher.window;

import android.app.AlertDialog;
import android.os.Looper;
import android.widget.EditText;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;
import top.dreagonmon.app.dreamoonlauncher.R;
import top.dreagonmon.app.dreamoonlauncher.utils.FileLog;

/**
 * 子线程同步对话框：HTTP 处理线程需要用户选择的返回值，所以保留阻塞式 API。
 *
 * 要点：
 * 1. Dialog 必须在主线程创建——AlertDialog 构造函数会 new Handler，
 *    在没有 Looper 的 NanoHttpd 工作线程上 create() 会抛
 *    "Can't create handler inside thread ... that has not called Looper.prepare()"。
 *    因此这里传的是 dialog 工厂，创建和 show 都在主线程完成。
 * 2. 用 CountDownLatch 等关闭事件：先关后等也不丢信号（旧 wait 会永久挂起）。
 * 3. dialog 注册到 MainActivity，Activity 销毁时统一 dismiss 唤醒线程返回默认值。
 * 4. 兜底超时，任何路径失守线程都不会永久挂死。
 * 5. 误在 UI 线程调用时只弹出不等待，不会自锁。
 */
public class SubThreadDialogWindow {
    /** 兜底超时：正常靠 dismiss/销毁唤醒，这只防极端情况 */
    private static final long FALLBACK_TIMEOUT_MS = 10 * 60 * 1000L;

    private final MainActivity viewContext;

    public SubThreadDialogWindow(MainActivity viewContext) {
        this.viewContext = viewContext;
    }

    /** 在主线程创建并显示 dialog，阻塞当前子线程直到它关闭（用户操作或 Activity 销毁）。 */
    private void showAndWait(Supplier<AlertDialog> dialogFactory) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<AlertDialog> holder = new AtomicReference<>();
        FileLog.write(viewContext, "dialog wait start (thread "
                + Thread.currentThread().getName() + ")");
        viewContext.runOnMainThread(() -> {
            try {
                AlertDialog dialog = dialogFactory.get();
                dialog.setOnDismissListener(d -> latch.countDown());
                viewContext.registerPendingDialog(dialog);
                holder.set(dialog);
                dialog.show();
                FileLog.write(viewContext, "dialog shown ok");
            } catch (Exception e) {
                // 创建/显示失败：直接唤醒等待线程，返回默认值
                FileLog.write(viewContext, "dialog show FAILED", e);
                latch.countDown();
            }
        });
        boolean dismissed = false;
        try {
            dismissed = latch.await(FALLBACK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            FileLog.write(viewContext, "dialog wait end, dismissed=" + dismissed);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        AlertDialog dialog = holder.get();
        if (dialog != null) {
            viewContext.unregisterPendingDialog(dialog);
            if (!dismissed && dialog.isShowing()) {
                viewContext.runOnMainThread(dialog::dismiss);
            }
        }
        viewContext.enterFullScreen();
    }

    /** 主线程调用时只弹出不阻塞。 */
    private void showOnly(Supplier<AlertDialog> dialogFactory) {
        viewContext.runOnMainThread(() -> {
            try {
                dialogFactory.get().show();
            } catch (Exception e) {
                FileLog.write(viewContext, "dialog showOnly FAILED", e);
            }
        });
    }

    public void alert(String msg) {
        Supplier<AlertDialog> factory = () -> new AlertDialog.Builder(viewContext)
                .setTitle(R.string.dialog_alert_title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_confirm, null)
                .create();
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showOnly(factory); // UI 线程不能等自己
            return;
        }
        showAndWait(factory);
    }

    public boolean confirm(String msg) {
        final boolean[] result = {false};
        Supplier<AlertDialog> factory = () -> new AlertDialog.Builder(viewContext)
                .setTitle(R.string.dialog_confirm_title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_confirm,
                        (d, which) -> result[0] = true)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showOnly(factory);
            return false; // 无法等待，按用户未同意处理
        }
        showAndWait(factory);
        return result[0];
    }

    public String prompt(String msg) {
        final AtomicReference<EditText> input = new AtomicReference<>();
        Supplier<AlertDialog> factory = () -> {
            EditText edit = new EditText(viewContext);
            input.set(edit);
            return new AlertDialog.Builder(viewContext)
                    .setTitle(R.string.dialog_prompt_title)
                    .setMessage(msg)
                    .setView(edit)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_confirm, null)
                    .create();
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showOnly(factory);
            return ""; // 无法等待，按空输入处理
        }
        showAndWait(factory);
        EditText edit = input.get();
        return edit == null ? "" : edit.getText().toString();
    }

    /** 回调式 prompt：UI 线程调用请用这个（阻塞式 prompt 在 UI 线程无法等待）。 */
    public void promptAsync(String msg, Consumer<String> onConfirm) {
        showOnly(() -> {
            EditText edit = new EditText(viewContext);
            return new AlertDialog.Builder(viewContext)
                    .setTitle(R.string.dialog_prompt_title)
                    .setMessage(msg)
                    .setView(edit)
                    .setCancelable(true)
                    .setPositiveButton(R.string.dialog_confirm,
                            (d, which) -> onConfirm.accept(edit.getText().toString()))
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .create();
        });
    }

    /** 回调式 confirm：UI 线程调用请用这个。 */
    public void confirmAsync(String msg, Consumer<Boolean> onResult) {
        showOnly(() -> new AlertDialog.Builder(viewContext)
                .setTitle(R.string.dialog_confirm_title)
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton(R.string.dialog_confirm,
                        (d, which) -> onResult.accept(true))
                .setNegativeButton(R.string.dialog_cancel,
                        (d, which) -> onResult.accept(false))
                .create());
    }
}
