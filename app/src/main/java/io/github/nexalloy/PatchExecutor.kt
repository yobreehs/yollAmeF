@file:OptIn(DexKitExperimentalApi::class)

package io.github.nexalloy

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.Utils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface
import io.github.nexalloy.BuildConfig.DEBUG
import io.github.nexalloy.morphe.Fingerprint
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.DexKitCacheBridge
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.wrap.DexClass
import org.luckypray.dexkit.wrap.DexField
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty0
import kotlin.system.measureTimeMillis

/**
 * References to alive activities, tracked for debugging (view hierarchy dump).
 * Kept as weak references to avoid leaking activities.
 */
private val aliveActivities = java.util.Collections.synchronizedList(mutableListOf<WeakReference<Activity>>())

/** Snapshots the currently alive activities, oldest first. */
fun snapshotAliveActivities(): List<Activity> {
    aliveActivities.removeAll { it.get() == null }
    synchronized(aliveActivities) {
        return aliveActivities.mapNotNull { it.get() }
    }
}

typealias FindFunc = DexKitBridge.() -> Any
typealias FindClassFunc = DexKitBridge.() -> ClassData
typealias FindMethodFunc = DexKitBridge.() -> MethodData
typealias FindMethodListFunc = DexKitBridge.() -> List<MethodData>
typealias FindFieldFunc = DexKitBridge.() -> FieldData

fun patch(
    name: String = "",
    description: String = "",
    use: Boolean = true,
    /**
     * Declarative part of the patch: the preferences it contributes to the Morphe settings
     * screen. Evaluated after the patch phase, before the settings screen is built and
     * regardless of whether this patch is enabled.
     *
     * The patcher version injects `morphe_prefs.xml` into the host and therefore always shows
     * every patch's settings. This module builds the screen at runtime from the executed patch
     * bodies instead, so keeping the registration inside [func] meant that disabling a patch
     * removed its whole section (or single rows) from the settings UI.
     */
    settings: () -> Unit = {},
    func: PatchExecutor.() -> Unit
): Patch {
    enqueueSettingsRegistration(name, settings)
    return Patch(name, description, use, func)
}

/**
 * Settings registrations are collected while patch objects are created (including the ones
 * created lazily by `dependsOn`) and applied once the patch phase is over. Deferring them is
 * required because some of them depend on state set up by other patches, for example the
 * YouTube version flags.
 */
private val pendingSettingsRegistrations = mutableListOf<Pair<String, () -> Unit>>()

private fun enqueueSettingsRegistration(name: String, settings: () -> Unit) {
    synchronized(pendingSettingsRegistrations) {
        pendingSettingsRegistrations += name to settings
    }
}

/**
 * Applies every queued settings registration. Patches created while applying the queue are
 * picked up as well, so dependency patches and patch factories do not have to be listed twice.
 */
fun applySettingsRegistrations() {
    var rounds = 0
    while (true) {
        val pending = synchronized(pendingSettingsRegistrations) {
            if (pendingSettingsRegistrations.isEmpty()) return
            pendingSettingsRegistrations.toList().also { pendingSettingsRegistrations.clear() }
        }
        pending.forEach { (name, settings) ->
            runCatching(settings).onFailure { e ->
                Logger.printException({ "Settings registration failed for patch '$name'" }, e)
            }
        }
        if (++rounds > 100) return // Guard against a patch factory that keeps creating patches.
    }
}

class Patch(
    val name: String,
    val description: String,
    /** Default state on a fresh install. Can be changed for a whole app before it is used. */
    var use: Boolean,
    val run: PatchExecutor.() -> Unit
) {
    /**
     * Name for the user. The internal patches of the module have no name, and the toast used to
     * show an empty line for them, which looked like a bug of its own.
     */
    val displayName: String
        get() = name.ifEmpty {
            description.lineSequence().firstOrNull()?.take(80)?.ifEmpty { null }
                ?: "FemAlloy patch ${System.identityHashCode(this)}"
        }
}

abstract class IHook(val xposed: XposedInterface) : XposedInterface by xposed {
    abstract val classLoader: ClassLoader

    fun DexMethod.hookMethod(callback: XC_MethodHook) {
        toMember().hookMethod(callback)
    }

    fun DexMethod.hookMethod(block: HookDsl<IHookCallback>.() -> Unit) {
        toMember().hookMethod(block)
    }

    fun DexClass.toClass() = getInstance(classLoader)
    fun DexMethod.toMethod(): Method {
        var clz = classLoader.loadClass(className)
        do {
            return XposedHelpers.findMethodExactIfExists(clz, name, *paramTypeNames.toTypedArray())
                ?: continue
        } while (clz.superclass.also { clz = it } != null)
        throw NoSuchMethodException("Method $this not found")
    }

    fun DexMethod.toConstructor(): Constructor<*> {
        var clz = classLoader.loadClass(className)
        do {
            return XposedHelpers.findConstructorExactIfExists(clz, *paramTypeNames.toTypedArray())
                ?: continue
        } while (clz.superclass.also { clz = it } != null)
        throw NoSuchMethodException("Method $this not found")
    }

    fun DexMethod.toMember(): Member {
        return when {
            isMethod -> toMethod()
            isConstructor -> toConstructor()
            else -> throw NotImplementedError()
        }
    }

    fun DexField.toField() = getFieldInstance(classLoader)

    override fun getApiVersion(): Int = xposed.apiVersion
}

@Suppress("UNCHECKED_CAST")
class SharedPrefCache(app: Application) : DexKitCacheBridge.Cache {
    val pref = app.getSharedPreferences("xpmorphe", MODE_PRIVATE)!!
    private val map = mutableMapOf<String, String>().apply {
        putAll(pref.all as Map<String, String>)
    }

    override fun clearAll() {
        map.clear()
    }

    override fun getString(key: String, default: String?): String? = map.getOrDefault(key, default)

    override fun getAllKeys(): Collection<String> = map.keys

    override fun getStringList(
        key: String, default: List<String>?
    ): List<String>? =
        map.getOrDefault(key, null)?.takeIf(String::isNotBlank)?.split('|') ?: default

    override fun putString(key: String, value: String) {
        map.put(key, value)
    }

    override fun putStringList(key: String, value: List<String>) {
        map.put(key, value.joinToString("|"))
    }

    override fun remove(key: String) {
        map.remove(key)
    }

    fun saveCache() {
        val edit = pref.edit()
        edit.clear()
        map.forEach { (k, v) ->
            edit.putString(k, v)
        }
        edit.commit()
    }
}

class DependedHookFailedException(
    subHookName: String, exception: Throwable
) : Exception("Depended hook $subHookName failed.", exception)

@SuppressLint("CommitPrefEdits")
class PatchExecutor(
    val appContext: Application,
    val lpparam: XposedModuleInterface.PackageReadyParam,
    xposed: XposedInterface
) : IHook(xposed) {
    override val classLoader = lpparam.classLoader

    /**
     * @see io.github.nexalloy.activity.AppPatchSettingsActivity.AppPatchSettingsFragment.onCreate
     * */
    private val patchPreferences = xposed.getRemotePreferences(lpparam.packageName)

    private lateinit var patches: Array<Patch>
    private val appliedPatches = mutableSetOf<Patch>()
    private val failedPatches = mutableListOf<Patch>()

    // cache
    private val moduleRel = BuildConfig.COMMIT_HASH
    private var cache = SharedPrefCache(appContext)
    private lateinit var dexkit: DexKitCacheBridge.RecyclableBridge

    /**
     * DexKit (a native library + APK scanning) is only needed when at least one
     * patch is actually enabled. Loading it regardless was fine everywhere except
     * for apps whose native protection (e.g. PairIP) detects foreign libraries in
     * the process and crashes even before any hook runs.
     */
    private fun ensureDexkitInitialized() {
        if (::dexkit.isInitialized) return
        System.loadLibrary("dexkit")
        DexKitCacheBridge.init(cache)
        dexkit = DexKitCacheBridge.create("", lpparam.applicationInfo.sourceDir)
    }

    fun applyPatches(patches: Array<Patch>) {
        val anyEnabled = patches.any {
            patchPreferences?.getBoolean(it.name, it.use) ?: it.use
        }
        if (!anyEnabled) {
            Logger.printDebug { "${lpparam.packageName} handleLoadPackage: no enabled patches, skipping initialization" }
            return
        }
        this.patches = patches
        val t = measureTimeMillis {
            ensureDexkitInitialized()
            loadCacheIfValid()
            try {
                executePatches()
                applySettingsRegistrations()
                finalizePatching()
                logDebugInfo()
            } finally {
                dexkit.close()
            }
        }
        Logger.printDebug { "${lpparam.packageName} handleLoadPackage: ${t}ms" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadCacheIfValid() {
        // cache by host update time + module version
        // also no cache if is DEBUG
        val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)

        val id = "${packageInfo.lastUpdateTime}-$moduleRel"
        val cachedId = cache.getString("id", null)
        val isCached = cachedId.equals(id) && !DEBUG

        Logger.printInfo { "cache ID : $id" }
        Logger.printInfo { "cached ID: ${cachedId ?: ""}" }
        Logger.printInfo { "Using cached keys: $isCached" }

        if (!isCached) {
            cache.clearAll()
            cache.putString("id", id)
            Utils.showToastLong("FemAlloy is initializing, please wait...")
        }
    }

    private fun executePatches() {
        patches.forEach { hook ->
            if (appliedPatches.contains(hook)) return@forEach
            /**
             * @see io.github.nexalloy.activity.AppPatchSettingsActivity.AppPatchSettingsFragment.onCreate
             * */
            val isEnabled = patchPreferences?.getBoolean(hook.name, hook.use) ?: hook.use
            if (!isEnabled) return@forEach // Pref Key
            runCatching { hook.run(this) }.onFailure { err ->
                XposedBridge.log(err)
                // Also write to logcat so patch failures can be diagnosed without root/LSPosed logs.
                android.util.Log.e("FemAlloy", "Patch '${hook.name}' failed to apply", err)
                failedPatches.add(hook)
            }.onSuccess {
                appliedPatches.add(hook)
            }
        }
    }

    private fun finalizePatching() {
        cache.saveCache()
        val success = failedPatches.isEmpty()
        if (!success) {
            XposedBridge.log("${lpparam.applicationInfo.packageName} version: ${getAppVersion()}")
            Utils.showToastLong("Error while apply following patches:\n${failedPatches.joinToString("\n") { it.displayName }}")
        }
    }

    private fun logDebugInfo() {
        val success = failedPatches.isEmpty()
        if (DEBUG) {
            XposedBridge.log("${lpparam.applicationInfo.packageName} version: ${getAppVersion()}")
            if (success) {
                Utils.showToastLong("apply patches success")
            }
        }
    }

    private fun getAppVersion(): String {
        val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        val versionName = packageInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION") packageInfo.versionCode
        }
        return "$versionName ($versionCode)"
    }

    fun dependsOn(vararg patches: Patch) {
        patches.forEach { hook ->
            if (appliedPatches.contains(hook)) return@forEach
            runCatching { (hook.run(this)) }.onFailure { err ->
                throw DependedHookFailedException(hook.name, err)
            }.onSuccess {
                appliedPatches.add(hook)
            }
        }
    }

    val KProperty0<FindMethodFunc>.dexMethod
        get() = getDexMethod(this.name, this.get())

    val KProperty0<FindMethodFunc>.method
        get() = dexMethod.toMethod()

    val KProperty0<FindMethodFunc>.constructor
        get() = dexMethod.toConstructor()

    val KProperty0<FindMethodFunc>.member
        get() = dexMethod.toMember()

    val KProperty0<FindMethodFunc>.memberOrNull
        get() = runCatching { this.member }.getOrNull()

    fun KProperty0<FindMethodFunc>.hookMethod(block: HookDsl<IHookCallback>.() -> Unit) {
        dexMethod.hookMethod(block)
    }

    fun KProperty0<FindMethodFunc>.hookMethod(callback: XC_MethodHook) {
        dexMethod.hookMethod(callback)
    }

    val KProperty0<FindMethodListFunc>.dexMethodList
        get() = getDexMethods(this.name, this.get())

    val KProperty0<FindFieldFunc>.dexField
        get() = getDexField(this.name, this.get())

    val KProperty0<FindFieldFunc>.field
        get() = dexField.toField()

    val KProperty0<FindFieldFunc>.declaredClass
        get() = classLoader.loadClass(dexField.declaredClassName)

    val KProperty0<FindFieldFunc>.type
        get() = classLoader.loadClass(dexField.className)

    val KProperty0<FindClassFunc>.dexClass
        get() = getDexClass(this.name, this.get())

    val KProperty0<FindClassFunc>.clazz
        get() = dexClass.toClass()

    // Fingerprint object extensions

    private val Fingerprint.cacheKey
        get() = this::class.simpleName ?: error("Anonymous Fingerprint has no cache key")

    fun Fingerprint.hookMethod(block: HookDsl<IHookCallback>.() -> Unit) {
        getDexMethod(cacheKey) { this@hookMethod.run() }.hookMethod(block)
    }

    fun Fingerprint.hookMethod(callback: XC_MethodHook) {
        getDexMethod(cacheKey) { this@hookMethod.run() }.hookMethod(callback)
    }

    val Fingerprint.dexMethod get() = getDexMethod(cacheKey) { this@dexMethod.run() }

    /**
     * Every method the fingerprint matches, for hooks that are safe to apply to all of them.
     * When the fingerprint does not match, the list is empty (the failure is logged).
     */
    val Fingerprint.dexMethodList get() = getDexMethods(cacheKey) { this@dexMethodList.runAll() }

    val Fingerprint.member get() = dexMethod.toMember()

    val Fingerprint.memberOrNull get() = runCatching { this.member }.getOrNull()

    val Fingerprint.method get() = dexMethod.toMethod()

    val Fingerprint.declaredClass get() = classLoader.loadClass(dexMethod.declaredClassName)

    val Fingerprint.constructor get() = dexMethod.toConstructor()

    private inline fun <reified T : Any> wrapFind(
        key: String,
        crossinline funcFunc: DexKitBridge.() -> T,
        crossinline serializer: (T) -> String
    ): DexKitBridge.() -> T? {
        return {
            try {
                funcFunc().also { Logger.printInfo { "$key Matches: ${serializer(it)}" } }
            } catch (e: Exception) {
                Logger.printInfo({ "Fingerprint $key Not Found" }, e)
                null
            }
        }
    }

    private inline fun <reified T : Any> wrapFindList(
        key: String,
        crossinline funcFunc: DexKitBridge.() -> List<T>,
        crossinline serializer: (T) -> String
    ): DexKitBridge.() -> List<T> {
        return {
            try {
                funcFunc().also {
                    Logger.printInfo { "$key Matches: ${it.joinToString { serializer(it) }}" }
                }
            } catch (e: Exception) {
                Logger.printInfo({ "Fingerprint $key Not Found" }, e)
                emptyList()
            }
        }
    }

    private inline fun getDexClass(
        key: String, crossinline findFunc: DexKitBridge.() -> ClassData
    ): DexClass = requireFound(key, dexkit.getClassDirectOrNull(key, wrapFind(key, findFunc) { it.descriptor }))

    private inline fun getDexMethod(
        key: String, crossinline findFunc: DexKitBridge.() -> MethodData
    ): DexMethod = requireFound(key, dexkit.getMethodDirectOrNull(key, wrapFind(key, findFunc) { it.descriptor }))

    private inline fun getDexField(
        key: String, crossinline findFunc: DexKitBridge.() -> FieldData
    ): DexField = requireFound(key, dexkit.getFieldDirectOrNull(key, wrapFind(key, findFunc) { it.descriptor }))

    /**
     * The `!!` here used to produce a bare NullPointerException with the obfuscated message
     * "getClass() on a null reference" after the fingerprint itself was already logged as not
     * found, which hid which patch failed. The failure is the same, but it names the fingerprint.
     */
    private fun <T : Any> requireFound(key: String, value: T?): T =
        value ?: throw IllegalStateException("Fingerprint '$key' was not found")

    private inline fun getDexMethods(
        key: String, crossinline findFunc: DexKitBridge.() -> List<MethodData>
    ): List<DexMethod> = dexkit.getMethodsDirectOrEmpty(
        key, wrapFindList(key, findFunc) { it.descriptor })
}

val ExtensionResourceHook = patch {
    appContext.addModuleAssets()
    ResourceUtils.fallbackPackageName = BuildConfig.APPLICATION_ID

    appContext.callMethod(
        "registerActivityLifecycleCallbacks", object : Application.ActivityLifecycleCallbacks {
            var handleWebView: Boolean = false

            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                Logger.printDebug { "onActivityCreated $activity" }
                synchronized(aliveActivities) { aliveActivities.add(WeakReference(activity)) }
                if (!handleWebView) {
                    WebView(activity).destroy()
                    appContext.addModuleAssets()
                    handleWebView = true
                }

                activity.addModuleAssets()
            }

            override fun onActivityDestroyed(activity: Activity) {
                Logger.printDebug { "onActivityDestroyed $activity" }
                synchronized(aliveActivities) {
                    aliveActivities.removeAll { it.get() === activity }
                }
            }

            override fun onActivityPaused(activity: Activity) {
                Logger.printDebug { "onActivityPaused $activity" }
            }

            override fun onActivityResumed(activity: Activity) {
                Logger.printDebug { "onActivityResumed $activity" }
                activity.addModuleAssets()
            }

            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
                Logger.printDebug { "onActivitySaveInstanceState $activity" }
            }

            override fun onActivityStarted(activity: Activity) {
                Logger.printDebug { "onActivityStarted $activity" }
            }

            override fun onActivityStopped(activity: Activity) {
                Logger.printDebug { "onActivityStopped $activity" }
            }
        })
}
