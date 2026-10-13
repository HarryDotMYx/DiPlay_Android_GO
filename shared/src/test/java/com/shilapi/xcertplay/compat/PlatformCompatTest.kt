package com.shilapi.xcertplay.compat

import android.content.res.Configuration
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.LocaleList
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PlatformCompatTest {
    @Test fun concurrentSetAddsRemovesAndFiltersLikeAKeySet() {
        val set = concurrentSetCompat<String>()
        assertTrue(set.add("a"))
        assertFalse(set.add("a"))
        set += "b"
        assertTrue(set.removeAll { it == "a" })
        assertEquals(setOf("b"), set)
    }

    @Test fun getOrCreateMakesOneValuePerKeyAcrossThreads() {
        val map = ConcurrentHashMap<Int, Any>()
        val created = AtomicInteger()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(8)
        try {
            val results = (1..32).map {
                pool.submit<Any> {
                    start.await()
                    map.getOrCreateCompat(1) { created.incrementAndGet(); Any() }
                }
            }
            start.countDown()
            val values = results.map { it.get(5, TimeUnit.SECONDS) }
            assertEquals(1, created.get())
            values.forEach { assertSame(values[0], it) }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test fun raiseToKeepsTheLargestValue() {
        val value = AtomicLong(5)
        assertEquals(5, value.raiseToCompat(3))
        assertEquals(5, value.get())
        assertEquals(9, value.raiseToCompat(9))
        assertEquals(9, value.get())
    }

    // Android 6 has no underrun counter; calling it there would stop the app.
    @Config(sdk = [23])
    @Test fun android6ReportsNoUnderruns() {
        @Suppress("DEPRECATION")
        val track = AudioTrack(AudioManager.STREAM_MUSIC, 48_000, AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT, 4_096, AudioTrack.MODE_STREAM)
        try {
            assertEquals(0, track.underrunCountCompat())
        } finally {
            track.release()
        }
    }

    @Config(sdk = [23])
    @Test fun android6ReadsAndCopiesItsSingleLocale() {
        @Suppress("DEPRECATION")
        val from = Configuration().apply { locale = Locale.JAPAN }
        assertEquals(Locale.JAPAN, from.primaryLocaleCompat())
        val copy = Configuration().apply { copyLocalesCompat(from) }
        assertEquals(Locale.JAPAN, copy.primaryLocaleCompat())
    }

    @Config(sdk = [25, 28])
    @Test fun android7CopiesTheWholeLocaleList() {
        val from = Configuration().apply { setLocales(LocaleList(Locale.JAPAN, Locale.US)) }
        val copy = Configuration().apply { copyLocalesCompat(from) }
        assertEquals(LocaleList(Locale.JAPAN, Locale.US), copy.locales)
        assertEquals(Locale.JAPAN, copy.primaryLocaleCompat())
    }

    // InterfaceSize copies into a fresh configuration, which may carry no locale at all.
    @Config(sdk = [23, 28])
    @Test fun anEmptyConfigurationCopiesAsLeftToRightWithoutALocale() {
        val copy = Configuration().apply { copyLocalesCompat(Configuration()) }
        assertEquals(android.view.View.LAYOUT_DIRECTION_LTR, copy.layoutDirection)
    }
}
