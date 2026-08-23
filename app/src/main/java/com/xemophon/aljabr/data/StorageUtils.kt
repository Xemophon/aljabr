package com.xemophon.aljabr.data

import android.content.Context
import android.util.Log
import java.io.File

object StorageUtils {
    private const val TAG = "StorageUtils"

    /**
     * Clears the application's internal cache directory.
     */
    fun clearAppCache(context: Context) {
        try {
            val cacheDir = context.cacheDir
            if (cacheDir.exists() && cacheDir.isDirectory) {
                deleteDir(cacheDir)
                Log.d(TAG, "Application cache cleared successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache: ${e.message}")
        }
    }

    /**
     * Clears the application's internal files directory, excluding DataStore settings.
     */
    fun clearAppData(context: Context) {
        try {
            val filesDir = context.filesDir
            if (filesDir.exists() && filesDir.isDirectory) {
                filesDir.listFiles()?.forEach { file ->
                    // Avoid deleting the 'datastore' folder to preserve settings
                    if (file.name != "datastore") {
                        if (file.isDirectory) {
                            deleteDir(file)
                        } else {
                            file.delete()
                        }
                    }
                }
                Log.d(TAG, "Application user data (excluding settings) cleared.")
            }
            clearAppCache(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear app data: ${e.message}")
        }
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (child in children) {
                    val success = deleteDir(File(dir, child))
                    if (!success) return false
                }
            }
        }
        return dir.delete()
    }
}
