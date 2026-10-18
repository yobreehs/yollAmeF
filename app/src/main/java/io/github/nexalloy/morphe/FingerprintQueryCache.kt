package io.github.nexalloy.morphe

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.MethodData
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Query results belong to a bridge, even when another bridge opens the same APK.
 * Fingerprint conditions must remain stable while that bridge is open.
 */
internal class FingerprintQueryCache(private val capacity: Int = 512) {
    init {
        require(capacity >= 0)
    }

    private class Key(val fingerprint: Fingerprint) {
        override fun equals(other: Any?) = other is Key && fingerprint === other.fingerprint
        override fun hashCode() = System.identityHashCode(fingerprint)
    }

    private class Queries(private val capacity: Int) :
        LinkedHashMap<Key, List<MethodData>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, List<MethodData>>) =
            size > capacity
    }

    private val bridges = IdentityHashMap<DexKitBridge, Queries>()

    fun getOrQuery(
        bridge: DexKitBridge,
        fingerprint: Fingerprint,
        query: () -> List<MethodData>,
    ): List<MethodData> {
        if (capacity == 0) return query()

        val queries = synchronized(bridges) {
            // Also discard bridges released by DexKitCacheBridge's idle timeout.
            bridges.keys.removeAll { !it.isValid }
            check(bridge.isValid) { "DexKitBridge is closed" }
            bridges.getOrPut(bridge) { Queries(capacity) }
        }

        // Reentrant for parent fingerprints; different bridges query independently.
        return synchronized(queries) {
            check(bridge.isValid) { "DexKitBridge is closed" }
            val key = Key(fingerprint)
            queries[key] ?: Collections.unmodifiableList(ArrayList(query())).also {
                queries[key] = it
            }
        }
    }

    fun clearClosedBridges() {
        synchronized(bridges) {
            bridges.keys.removeAll { !it.isValid }
        }
    }

    companion object {
        val shared = FingerprintQueryCache()
    }
}
