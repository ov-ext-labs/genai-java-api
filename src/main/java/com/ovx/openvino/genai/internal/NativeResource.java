package com.ovx.openvino.genai.internal;

import java.lang.ref.Cleaner;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

public abstract class NativeResource implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();

    private final AtomicBoolean closed = new AtomicBoolean();
    private final State state;
    private final Cleaner.Cleanable cleanable;

    protected NativeResource(long nativeHandle, LongConsumer disposer) {
        if (nativeHandle == 0L) {
            throw new IllegalStateException("Native handle must be non-zero");
        }
        this.state = new State(nativeHandle, Objects.requireNonNull(disposer, "disposer"));
        this.cleanable = CLEANER.register(this, state);
    }

    protected final long nativeHandle() {
        if (closed.get()) {
            throw new IllegalStateException(getClass().getSimpleName() + " is already closed");
        }
        return state.handle();
    }

    @Override
    public final void close() {
        if (closed.compareAndSet(false, true)) {
            cleanable.clean();
        }
    }

    private static final class State implements Runnable {
        private final LongConsumer disposer;
        private long handle;

        private State(long handle, LongConsumer disposer) {
            this.handle = handle;
            this.disposer = disposer;
        }

        private synchronized long handle() {
            if (handle == 0L) {
                throw new IllegalStateException("Native handle already disposed");
            }
            return handle;
        }

        @Override
        public synchronized void run() {
            if (handle != 0L) {
                disposer.accept(handle);
                handle = 0L;
            }
        }
    }
}
