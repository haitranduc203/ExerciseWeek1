package com.example.notificationpractice

import android.content.Context
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Kiểm tra token thật và Compose UI sau khi hệ thống thực hiện PendingIntent. */
@RunWith(AndroidJUnit4::class)
class PendingIntentBehaviorTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var context: Context
    private lateinit var notifications: MessageNotifications

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        notifications = MessageNotifications(context)
        ReadStateStore(context).reset()
        notifications.cancelAll()
    }

    @Test
    fun separateRequestCodes_keepOriginalMessage_andHandleNewIntent() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            val tokenA = notifications.contentPendingIntent(DemoMessages.a, brokenMode = false)
            val tokenB = notifications.contentPendingIntent(DemoMessages.b, brokenMode = false)
            assertNotEquals(tokenA, tokenB)
            tokenA.send()
            compose.onNodeWithTag("messageId").assertTextEquals(context.getString(R.string.detail_id, "A"))
            tokenB.send()
            compose.onNodeWithTag("messageId").assertTextEquals(context.getString(R.string.detail_id, "B"))
            compose.onNodeWithTag("backToList").performScrollTo().performClick()
        }
    }

    @Test
    fun sharedRequestCode_updatesOldTokensExtras_evenWhenImmutable() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            val oldTokenA = notifications.contentPendingIntent(DemoMessages.a, brokenMode = true)
            val tokenB = notifications.contentPendingIntent(DemoMessages.b, brokenMode = true)
            assertEquals(oldTokenA, tokenB)
            oldTokenA.send()
            compose.onNodeWithTag("messageId").assertTextEquals(context.getString(R.string.detail_id, "B"))
            compose.onNodeWithTag("backToList").performScrollTo().performClick()
        }
    }

    @Test
    fun broadcastAction_marksOnlyItsMessageRead_andRecomposesResumedScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            val tokenA = notifications.readPendingIntent(DemoMessages.a)
            val tokenB = notifications.readPendingIntent(DemoMessages.b)
            assertNotEquals(tokenA, tokenB)
            tokenA.send()
            val store = ReadStateStore(context)
            compose.waitUntil(timeoutMillis = 5000) { store.isRead("A") }
            assertTrue(store.isRead("A"))
            assertFalse(store.isRead("B"))
            compose.onNodeWithTag("statusA").performScrollTo().assertTextEquals(
                context.getString(R.string.message_status, "A", context.getString(R.string.read)),
            )
            compose.onNodeWithTag("statusB").performScrollTo().assertTextEquals(
                context.getString(R.string.message_status, "B", context.getString(R.string.unread)),
            )
        }
    }

    @Test
    fun brokenMode_survivesActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithTag("brokenMode").performScrollTo().performClick().assertIsOn()
            scenario.recreate()
            compose.onNodeWithTag("brokenMode").performScrollTo().assertIsOn()
        }
    }
}