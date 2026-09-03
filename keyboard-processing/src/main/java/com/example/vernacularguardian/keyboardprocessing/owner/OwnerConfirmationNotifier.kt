package com.example.vernacularguardian.keyboardprocessing.owner

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.vernacularguardian.keyboardprocessing.R
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DeveloperDiagnosticCounters
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DiagnosticEventType

/**
 * The Sprint 5 "lightweight notification prompt". Contains no typed text,
 * password text, clipboard text, package name, app name, or typing metrics —
 * only a fixed, generic confirmation question (PRD Section 25).
 *
 * [canNotify] is the fail-closed check: if notification permission is
 * unavailable, callers must treat the session as UNCONFIRMED and must not
 * capture. This class never requests or grants POST_NOTIFICATIONS itself —
 * that remains the owning app/onboarding's responsibility outside this
 * module (PRD Section 8).
 */
class OwnerConfirmationNotifier(private val context: Context) {

    fun canNotify(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Shows the Yes/Not-me prompt if (and only if) notifications are currently permitted. */
    fun showConfirmationPrompt() {
        if (!canNotify()) {
            // Sprint 8 observational hook: the fail-closed path itself is
            // unchanged (the prompt still never shows) — this only counts it.
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_PERMISSION_DENIED_FAIL_CLOSED)
            return
        }
        // Redundant with canNotify() on every real device/API level, but satisfies lint's
        // MissingPermission check, which only recognizes an explicit checkSelfPermission
        // guard immediately preceding the notify() call itself.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.owner_confirmation_title))
            .setContentText(context.getString(R.string.owner_confirmation_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(0, context.getString(R.string.owner_confirmation_action_yes), actionPendingIntent(ACTION_YES))
            .addAction(0, context.getString(R.string.owner_confirmation_action_not_me), actionPendingIntent(ACTION_NOT_ME))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancelPrompt() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.owner_confirmation_channel_name), NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun actionPendingIntent(action: String): PendingIntent {
        val intent = Intent(context, OwnerConfirmationActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "owner_confirmation_v2"
        const val NOTIFICATION_ID = 1001
        const val ACTION_YES = "com.example.vernacularguardian.keyboardprocessing.owner.ACTION_YES"
        const val ACTION_NOT_ME = "com.example.vernacularguardian.keyboardprocessing.owner.ACTION_NOT_ME"
    }
}
