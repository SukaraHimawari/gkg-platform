package edu.gkg.common;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Swing 后台任务基类：把耗时操作丢到 EDT 之外，通过 publishProgress 回报进度。
 * 子类只需实现 doWork()；UI 拿到结果后在 onDone() 里更新界面。
 */
public abstract class GuiTask<T> extends SwingWorker<T, GuiTask.Progress> {

    public record Progress(int percent, String message) {}

    private final Component owner;
    private final Consumer<Progress> progressSink;
    private final Consumer<T>        successSink;
    private final BiConsumer<Component, Throwable> errorSink;

    protected GuiTask(Component owner,
                      Consumer<Progress> progressSink,
                      Consumer<T> successSink,
                      BiConsumer<Component, Throwable> errorSink) {
        this.owner = owner;
        this.progressSink = progressSink != null ? progressSink : p -> {};
        this.successSink  = successSink  != null ? successSink  : r -> {};
        this.errorSink    = errorSink    != null ? errorSink    : (c, e) -> UiUtil.error(c, "任务失败", e);
    }

    protected abstract T doWork() throws Exception;

    @Override
    protected final T doInBackground() throws Exception {
        return doWork();
    }

    protected final void report(int percent, String message) {
        publish(new Progress(Math.max(0, Math.min(100, percent)), message));
    }

    @Override
    protected final void process(List<Progress> chunks) {
        if (!chunks.isEmpty()) progressSink.accept(chunks.get(chunks.size() - 1));
    }

    @Override
    protected final void done() {
        try {
            T result = get();
            successSink.accept(result);
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            errorSink.accept(owner, cause);
        }
    }
}
