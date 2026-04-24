package com.example.triangulation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileProviderHelper {
    fun getGpxUri(context: Context, fileName: String, gpxData: String): Uri {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use {
            it.write(gpxData.toByteArray())
        }

        // Grant read URI permission to OsmAnd packages
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.grantUriPermission("net.osmand.plus", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        context.grantUriPermission("net.osmand", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        return uri
    }
}
