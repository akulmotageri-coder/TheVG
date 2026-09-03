package com.example.vernacularguardian.keyboardprocessing.owner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DeveloperDiagnosticCounters
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DiagnosticEventType

/**
 * Routes the notification's "Yes"/"Not me" actions to [OwnerSessionManager].
 * A fresh instance per broadcast (standard Android receiver lifecycle), so
 * all durable state lives in the [OwnerSessionManager] singleton, not here.
 */
class OwnerConfirmationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action}")
        when (intent.action) {
            OwnerConfirmationNotifier.ACTION_YES -> {
                OwnerSessionManager.confirmCurrentSession()
                DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_CONFIRMED)
            }
            OwnerConfirmationNotifier.ACTION_NOT_ME -> {
                OwnerSessionManager.revokeCurrentSession()
                DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_REVOKED)
            }
            else -> return
        }
        Log.d(TAG, "isConfirmed now = ${OwnerSessionManager.isConfirmed()}")
        NotificationManagerCompat.from(context).cancel(OwnerConfirmationNotifier.NOTIFICATION_ID)
    }

    private companion object {
        const val TAG = "OwnerConfirmationRx"
    }
}
