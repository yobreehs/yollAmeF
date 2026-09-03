package de.robv.android.xposed;

import static io.github.libxposed.api.XposedInterface.ExceptionMode;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;

public class XposedBridge {
    public static final ClassLoader BOOTCLASSLOADER = ClassLoader.getSystemClassLoader();

    public static final String TAG = "LSPosed-Bridge";

    public static XposedInterface xposedInterface;

    public static void log(String text) {
        Log.i(TAG, text);
    }

    public static void log(Throwable t) {
        String logStr = Log.getStackTraceString(t);
        Log.i(TAG, logStr);
    }

    public static void register(XposedInterface xposedInterface) {
        XposedBridge.xposedInterface = xposedInterface;
    }

    public static XC_MethodHook.Unhook hookMethod(Member member, XC_MethodHook callback) {
        if (xposedInterface == null) {
            throw new IllegalStateException("xposedInterface has not been initialized. Call register() first.");
        }

        if (!(member instanceof Executable executable)) {
            throw new IllegalStateException();
        }

        var hookMethod = xposedInterface.hook(executable)
                .setPriority(callback.priority)
                .intercept(callback);
        return new XC_MethodHook.Unhook(hookMethod);
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Constructor<?> constructor : hookClass.getDeclaredConstructors())
            unhooks.add(hookMethod(constructor, callback));
        return unhooks;
    }

}