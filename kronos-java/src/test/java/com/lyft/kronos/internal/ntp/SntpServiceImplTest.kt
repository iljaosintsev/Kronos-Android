package com.lyft.kronos.internal.ntp

import com.lyft.kronos.Clock
import com.lyft.kronos.SyncResponseCache
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions
import org.junit.Test
import java.io.IOException


class SntpServiceImplTest {

    private val ntpHosts = listOf("ntp hosts")

    @Test
    fun basicTest() {

        val t = 777777777L

        test(TestCase(name = "No ntp response",
                hasNtp = false,
                deviceTime = t,
                expectedCalculatedTime = t))

    }

    @Test
    fun test2() {
        test(TestCase(name = "Basic ntp response no time elapsed",
                ntpResponseTime = 100L,
                deviceTime = 77L,
                expectedCalculatedTime = 100L
        ))
    }


    fun test(case: TestCase) {

        val deviceClock: Clock = mock {
            on { getCurrentTimeMs() } doReturn (case.deviceTime)
            on { getElapsedTimeMs() } doReturn (case.deviceBootTime)
        }
        val clientResponse = SntpClient.Response(
                case.deviceTime,
                case.deviceBootTime,
                case.ntpResponseTime - case.deviceTime,
                deviceClock)

        val sntpClient: SntpClient = if (case.hasNtp) {
            mock {
                on { requestTime(any(), any()) } doReturn clientResponse
            }
        } else {
            mock {
                on { requestTime(any(), any()) } doThrow IOException()
            }
        }

        //TODO same device clock?
        val responseCache = SntpResponseCacheImpl(MockSyncResponseCache(), deviceClock)
        val sntpService = SntpServiceImpl(sntpClient, deviceClock, responseCache, null, ntpHosts)


        sntpService.sync()
        val calculatedTime = sntpService.currentTimeMs()

        Assertions.assertThat(calculatedTime).isEqualTo(case.expectedCalculatedTime)
    }
}

data class TestCase(
        val name: String,
        val hasNtp: Boolean = true,
        val ntpResponseTime: Long = 1L,
        val deviceTime: Long,
        val deviceBootTime: Long = 1L,
        val expectedCalculatedTime: Long
)

class MockSyncResponseCache : SyncResponseCache {
    override var currentTime: Long = 1L
    override var elapsedTime: Long = 1L
    override var currentOffset: Long = 999L
    override fun clear() {
        currentTime = 0L
        elapsedTime = 0L
        currentOffset = 0L
    }
}