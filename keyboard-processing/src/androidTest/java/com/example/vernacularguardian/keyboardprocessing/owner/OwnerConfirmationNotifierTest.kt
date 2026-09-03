package com.example.vernacularguardian.keyboardprocessing.owner

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [OwnerConfirmationNotifier] and [OwnerConfirmationActionReceiver]
 * against the real, on-device [NotificationManager] (PRD master-prompt
 * Section 28). As with the other keyboard-processing instrumented tests,
 * this test APK is self-instrumenting: `targetContext` is this test APK's
 * own package, so these checks exercise a real notification channel/posting
 * pipeline, just not the production `:app` package specifically (that is
 * validated separately via real AVD evidence in Sprint_5.md).
 *
 * POST_NOTIFICATIONS (API 33+) is a runtime permission; it is granted here via
 * a `pm grant` shell command purely as test setup — this is test-harness
 * setup, not production behavior, and the production module never grants this
 * permission itself.
 *
 * There is deliberately no in-process "permission revoked" test here: revoking
 * an already-granted runtime permission from a live process makes the OS kill
 * that process immediately to enforce the change (confirmed with real logcat
 * evidence: `ActivityManager: Killing <pid>:...test/u0a217 (adj 0): permissions
 * revoked`), which would kill this very test process mid-suite. That "fail
 * closed on revoked permission" behavior is instead validated against the real
 * production `:app` process from outside (adb), where the same kill is
 * expected and harmless — see Sprint_5.md Section 15/21 (AVD scenario G).
 */
@RunWith(AndroidJUnit4::class)
class OwnerConfirmationNotifierTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notifier = OwnerConfirmationNotifier(context)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        grantPostNotifications()
        OwnerSessionManager.resetForNewSession()
        notifier.cancelPrompt()
    }

    @After
    fun tearDown() {
        notifier.cancelPrompt()
        OwnerSessionManager.resetForNewSession()
    }

    @Test
    fun showConfirmationPromptPostsANotificationWhenPermissionIsAvailable() {
        assertTrue(notifier.canNotify())

        notifier.showConfirmationPrompt()

        val active = notificationManager.activeNotifications
        assertTrue(active.any { it.id == OwnerConfirmationNotifier.NOTIFICATION_ID })
    }

    @Test
    fun showConfirmationPromptCreatesTheDedicatedChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return // channels do not exist pre-O

        notifier.showConfirmationPrompt()

        assertNotNull(notificationManager.getNotificationChannel(OwnerConfirmationNotifier.CHANNEL_ID))
    }

    @Test
    fun callingShowConfirmationPromptTwiceLeavesExactlyOneActiveNotification() {
        notifier.showConfirmationPrompt()
        notifier.showConfirmationPrompt()

        val matching = notificationManager.activeNotifications.count { it.id == OwnerConfirmationNotifier.NOTIFICATION_ID }
        assertTrue("expected exactly one active prompt notification, found $matching", matching == 1)
    }

    @Test
    fun yesActionConfirmsTheCurrentSessionAndCancelsTheNotification() {
        notifier.showConfirmationPrompt()
        assertTrue(notificationManager.activeNotifications.any { it.id == OwnerConfirmationNotifier.NOTIFICATION_ID })

        OwnerConfirmationActionReceiver().onReceive(context, Intent(OwnerConfirmationNotifier.ACTION_YES))

        assertTrue(OwnerSessionManager.isConfirmed())
        assertFalse(notificationManager.activeNotifications.any { it.id == OwnerConfirmationNotifier.NOTIFICATION_ID })
    }

    @Test
    fun notMeActionRevokesAPreviouslyConfirmedSession() {
        OwnerSessionManager.confirmCurrentSession()

        OwnerConfirmationActionReceiver().onReceive(context, Intent(OwnerConfirmationNotifier.ACTION_NOT_ME))

        assertFalse(OwnerSessionManager.isConfirmed())
    }

    private fun grantPostNotifications() {
        if (Build.VERSION.SDK_INT >= 33) runShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
    }

    private fun runShellCommand(command: String) {
        val pfd: ParcelFileDescriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        // Reading to EOF blocks until the shell command has actually completed.
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }
}
