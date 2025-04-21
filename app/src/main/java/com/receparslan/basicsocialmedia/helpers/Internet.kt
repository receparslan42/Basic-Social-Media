package com.receparslan.basicsocialmedia.helpers

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AlertDialog

object Internet {
    // Function to check internet connection
    fun checkConnection(activity: Activity) {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            AlertDialog.Builder(activity)
                .setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again.")
                .setPositiveButton("Retry") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    checkConnection(activity)
                }.setNegativeButton("Exit") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    activity.finish()
                }.setCancelable(false)
                .show()
        }
    }
}