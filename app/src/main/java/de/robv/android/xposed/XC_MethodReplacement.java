package de.robv.android.xposed;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedInterface;

public abstract class XC_MethodReplacement implements XposedInterface.Hooker {

    public static final XposedInterface.Hooker DO_NOTHING = chain -> null;

    public static XposedInterface.Hooker returnConstant(Object value) {
        return chain -> value;
    }

    /**
     * Shortcut for replacing a method completely. Whatever is returned/thrown here is taken
     * instead of the result of the original method (which will not be called).
     *
     * <p>Note that implementations shouldn't call {@code super(param)}, it's not necessary.
     *
     * @param param Information about the method call.
     * @throws Throwable Anything that is thrown by the callback will be passed on to the original caller.
     */
    public abstract Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable;

    @Override
    public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
        XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam(chain);
        replaceHookedMethod(param);
        return param.getResultOrThrowable();
    }
}