package com.shilapi.xcertplay.compat

import android.content.res.Configuration
import android.location.LocationManager
import android.media.AudioTrack
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Releases the channel on API 27+. Earlier releases have no close call and free it when collected. */
fun WifiP2pManager.Channel.closeCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) close()
}

/** [LocationManager.isLocationEnabled] needs API 28; before that a location provider must be on. */
fun LocationManager.isLocationEnabledCompat(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        isLocationEnabled
    } else {
        isProviderEnabled(LocationManager.GPS_PROVIDER) || isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

/** [ConcurrentHashMap.newKeySet] needs API 24. A set view of a [ConcurrentHashMap] behaves the same. */
fun <T : Any> concurrentSetCompat(): MutableSet<T> = Collections.newSetFromMap(ConcurrentHashMap<T, Boolean>())

/**
 * [ConcurrentHashMap.computeIfAbsent] needs API 24. Like it, this creates at most one value per key:
 * creations through this call are serialized on the map, while plain reads and removals stay lock-free.
 */
inline fun <K : Any, V : Any> ConcurrentHashMap<K, V>.getOrCreateCompat(key: K, create: () -> V): V =
    get(key) ?: synchronized(this) { get(key) ?: create().also { put(key, it) } }

/** [AtomicLong.accumulateAndGet] needs API 24. Raises the value to [candidate] and returns the result. */
fun AtomicLong.raiseToCompat(candidate: Long): Long {
    while (true) {
        val current = get()
        if (candidate <= current || compareAndSet(current, candidate)) return maxOf(current, candidate)
    }
}

/** [AudioTrack.getUnderrunCount] needs API 24. Earlier releases do not count underruns, so this reports 0. */
fun AudioTrack.underrunCountCompat(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) underrunCount else 0

/** [Configuration.getLocales] needs API 24. Before it a configuration has a single locale. */
@Suppress("DEPRECATION")
fun Configuration.primaryLocaleCompat(): Locale =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) locales[0] else locale

/**
 * Copies the locales of [from] and the layout direction of its first one. [Configuration.setLocales] needs
 * API 24; before it a configuration has a single locale. An empty [from] leaves no locale and left-to-right.
 */
@Suppress("DEPRECATION")
fun Configuration.copyLocalesCompat(from: Configuration) {
    val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        setLocales(from.locales)
        from.locales[0]
    } else {
        locale = from.locale
        from.locale
    }
    setLayoutDirection(primary)
}
