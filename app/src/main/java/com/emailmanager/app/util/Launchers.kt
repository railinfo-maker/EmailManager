package com.emailmanager.app.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

enum class LaunchResult { APP, BROWSER, FAILED }

/**
 * Only opens other apps / web pages. Nothing is typed, posted or submitted on the user's behalf.
 */
object Launchers {
    private const val GMAIL_PACKAGE = "com.google.android.gm"
    private const val MAPS_PACKAGE = "com.google.android.apps.maps"
    const val ACCOUNTS_URL = "https://accounts.google.com/"
    const val MAPS_URL = "https://maps.google.com/"
    private const val GMAIL_WEB_URL = "https://mail.google.com/"

    private fun start(context: Context, intent: Intent): Boolean {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    private fun viewUrl(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    fun openAccounts(context: Context): LaunchResult =
        if (start(context, viewUrl(ACCOUNTS_URL))) LaunchResult.BROWSER else LaunchResult.FAILED

    fun openGmail(context: Context): LaunchResult {
        val launch = context.packageManager.getLaunchIntentForPackage(GMAIL_PACKAGE)
        if (launch != null && start(context, launch)) return LaunchResult.APP
        return if (start(context, viewUrl(GMAIL_WEB_URL))) LaunchResult.BROWSER else LaunchResult.FAILED
    }

    fun openMaps(context: Context): LaunchResult {
        val inApp = viewUrl(MAPS_URL).setPackage(MAPS_PACKAGE)
        if (start(context, inApp)) return LaunchResult.APP
        return if (start(context, viewUrl(MAPS_URL))) LaunchResult.BROWSER else LaunchResult.FAILED
    }
}
