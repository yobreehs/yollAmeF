package de.robv.android.xposed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.Objects;

import io.github.libxposed.api.XposedInterface;

public abstract class XC_MethodHook implements XposedInterface.Hooker {
    public final int priority;

    public XC_MethodHook() {
        this(XposedInterface.PRIORITY_DEFAULT);
    }

    public XC_MethodHook(int priority) {
        this.priority = priority;
    }

    /**
     * Called before the invocation of the method.
     * <p>
     * You can use {@link MethodHookParam#setResult(Object)}
     * to prevent the original method from being called.
     *
     * <p>Note that implementations shouldn't call {@code super(param)}, it's not necessary.
     *
     * @param param Information about the method call.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    }

    /**
     * Called after the invocation of the method.
     * <p>
     * You can use {@link MethodHookParam#setResult(Object)}
     * to modify the return value of the original method.
     * <p>
     * Note that implementations shouldn't call {@code super(param)}, it's not necessary.
     *
     * @param param Information about the method call.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    @Override
    public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
        MethodHookParam param = new MethodHookParam(chain);

        try {
            beforeHookedMethod(param);
        } catch (Throwable t) {
            XposedBridge.log(t);

            // reset result (ignoring what the unexpectedly exiting callback did)
            param.setResult(null);
            param.returnEarly = false;
        }

        if (!param.returnEarly) {
            try {
                param.setResult(chain.proceed(param.args));
            } catch (Throwable e) {
                param.setThrowable(e);
            }
        }

        Object lastResult = param.getResult();
        Throwable lastThrowable = param.getThrowable();
        try {
            afterHookedMethod(param);
        } catch (Throwable t) {
            XposedBridge.log(t);
            // reset to last result (ignoring what the unexpectedly exiting callback did)
            param.setResult(lastResult);
            param.setThrowable(lastThrowable);
        }

        // re-throw a proceed() Throwable won't be logged.
        return param.getResultOrThrowable();
    }

    public static class MethodHookParam implements XposedInterface.Chain {
        private final XposedInterface.Chain chain;

        /**
         * Arguments to the method call.
         */
        public final Object[] args;

        private boolean returnEarly = false;

        private Object result = null;

        private Throwable throwable = null;

        public MethodHookParam(XposedInterface.Chain chain) {
            this.chain = chain;
            this.args = chain.getArgs().toArray();
        }

        public Executable getMethod() {
            return chain.getExecutable();
        }

        /**
         * Returns the result of the method call.
         */
        public Object getResult() {
            return result;
        }

        /**
         * Modify the result of the method call.
         *
         * <p>If called from {@link #beforeHookedMethod}, it prevents the call to the original method.
         */
        public void setResult(Object result) {
            this.result = result;
            this.throwable = null;
            this.returnEarly = true;
        }

        /**
         * Returns the {@link Throwable} thrown by the method, or {@code null}.
         */
        public Throwable getThrowable() {
            return throwable;
        }

        /**
         * Returns true if an exception was thrown by the method.
         */
        public boolean hasThrowable() {
            return throwable != null;
        }

        /**
         * Modify the exception thrown of the method call.
         *
         * <p>If called from {@link #beforeHookedMethod}, it prevents the call to the original method.
         */
        void setThrowable(Throwable throwable) {
            /*
             * This compatibility class intentionally make `setThrowable` package-only accessibility.
             * If you want to implement `setThrowable` to override throwable,
             * `setExceptionMode(PASSTHROUGH)` is necessary.
             * */

            this.throwable = throwable;
            this.result = null;
            this.returnEarly = true;
        }

        /**
         * Returns the result of the method call, or throws the Throwable caused by it.
         */
        public Object getResultOrThrowable() throws Throwable {
            if (throwable != null)
                throw throwable;
            return result;
        }

        public Object invokeOriginalMethod() throws Throwable {
            return proceed();
        }

        @NonNull
        @Override
        public Executable getExecutable() {
            return chain.getExecutable();
        }

        @Override
        public Object getThisObject() {
            return chain.getThisObject();
        }

        @NonNull
        @Override
        public List<Object> getArgs() {
            return chain.getArgs();
        }

        @Override
        public Object getArg(int index) throws IndexOutOfBoundsException, ClassCastException {
            return chain.getArg(index);
        }

        @Override
        public Object proceed() throws Throwable {
            return chain.proceed();
        }

        @Override
        public Object proceed(@NonNull Object[] args) throws Throwable {
            return chain.proceed(args);
        }

        @Override
        public Object proceedWith(@NonNull Object thisObject) throws Throwable {
            return chain.proceedWith(thisObject);
        }

        @Override
        public Object proceedWith(@NonNull Object thisObject, @NonNull Object[] args) throws Throwable {
            return chain.proceedWith(thisObject, args);
        }
    }

    public static final class Unhook implements XposedInterface.HookHandle {
        private final XposedInterface.HookHandle handle;

        public Unhook(XposedInterface.HookHandle handle) {
            this.handle = handle;
        }

        public XposedInterface.HookHandle getHandle() {
            return handle;
        }

        @NonNull
        @Override
        public Executable getExecutable() {
            return handle.getExecutable();
        }

        @Override
        public void unhook() {
            handle.unhook();
        }

        @Nullable
        @Override
        public String getId() {
            return handle.getId();
        }

        @NonNull
        @Override
        public XposedInterface.HookHandle replaceHook(@NonNull XposedInterface.Hooker hooker) {
            return handle.replaceHook(hooker);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Unhook unhook = (Unhook) o;
            return Objects.equals(handle, unhook.handle);
        }

        @Override
        public int hashCode() {
            return Objects.hash(handle);
        }

        @NonNull
        @Override
        public String toString() {
            return "Unhook(handle=" + handle + ')';
        }
    }
}