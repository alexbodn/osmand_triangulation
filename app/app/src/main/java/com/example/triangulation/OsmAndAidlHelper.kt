package com.example.triangulation

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.KeyEvent
import net.osmand.aidlapi.IOsmAndAidlInterface
import net.osmand.aidlapi.IOsmAndAidlCallback
import net.osmand.aidlapi.contextmenu.ContextMenuButtonsParams
import net.osmand.aidlapi.contextmenu.AContextMenuButton
import net.osmand.aidlapi.gpx.ImportGpxParams
import net.osmand.aidlapi.gpx.RemoveGpxParams
import net.osmand.aidlapi.search.SearchResult
import net.osmand.aidlapi.gpx.AGpxBitmap
import net.osmand.aidlapi.navigation.ADirectionInfo
import net.osmand.aidlapi.navigation.OnVoiceNavigationParams
import net.osmand.aidlapi.logcat.OnLogcatMessageParams
import net.osmand.aidlapi.customization.OsmandSettingsParams
import net.osmand.aidlapi.maplayer.point.AMapPoint
import net.osmand.aidlapi.plugins.PluginParams
import net.osmand.aidlapi.maplayer.AMapLayer
import net.osmand.aidlapi.maplayer.AddMapLayerParams
import java.util.ArrayList

class OsmAndAidlHelper(private val application: Application, private val listener: OsmAndAidlListener?) {

    private var osmandService: IOsmAndAidlInterface? = null
    private var isBound = false

    interface OsmAndAidlListener {
        fun onOsmAndServiceConnected()
        fun onOsmAndServiceDisconnected()
        fun onContextMenuButtonClicked(buttonId: Int, pointId: String?, layerId: String?)
    }

    private val callback = object : IOsmAndAidlCallback.Stub() {
        override fun onContextMenuButtonClicked(buttonId: Int, pointId: String?, layerId: String?) {
            Log.d(TAG, "onContextMenuButtonClicked: $buttonId, pointId: $pointId")
            listener?.onContextMenuButtonClicked(buttonId, pointId, layerId)
        }
        override fun onUpdate() {}
        override fun onSearchComplete(result: MutableList<SearchResult>?) {}
        override fun onAppInitialized() {}
        override fun onGpxBitmapCreated(bitmap: AGpxBitmap?) {}
        override fun updateNavigationInfo(info: ADirectionInfo?) {}
        override fun onVoiceRouterNotify(params: OnVoiceNavigationParams?) {}
        override fun onLogcatMessage(params: OnLogcatMessageParams?) {}
        override fun onKeyEvent(event: KeyEvent?) {}
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "OsmAnd AIDL Service Connected")
            osmandService = IOsmAndAidlInterface.Stub.asInterface(service)

            listener?.onOsmAndServiceConnected()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "OsmAnd AIDL Service Disconnected")
            osmandService = null
            listener?.onOsmAndServiceDisconnected()
        }
    }

    fun bindService(): Boolean {
        if (isBound) return true

        try {
            val intent = Intent("net.osmand.aidl.OsmandAidlServiceV2")
            intent.setPackage("net.osmand.plus")
            var bound = application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

            if (!bound) {
                intent.setPackage("net.osmand")
                bound = application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }

            isBound = bound
            Log.d(TAG, "Binding to OsmAnd Service: $bound")
            return bound
        } catch (e: Exception) {
            Log.e(TAG, "Error binding to OsmAnd Service", e)
            return false
        }
    }

    fun unbindService() {
        if (isBound) {
            try {
                application.unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Service was not registered or already unbound", e)
            }
            isBound = false
        }
        osmandService = null
    }

    fun addContextMenuButton(buttonId: Int, leftText: String, rightText: String, iconName: String = "ic_action_marker_dark"): Boolean {
        if (osmandService == null) {
            Log.e(TAG, "Service not bound, cannot add context menu button")

            return false
        }

        try {
            val layerId = "triangulation_layer"

            // First explicitly register the map layer we are attaching to, like the official demo does
            val layer = AMapLayer(layerId, "Triangulation Readings", 5.5f, null)
            val layerParams = AddMapLayerParams(layer)
            val layerAdded = osmandService?.addMapLayer(layerParams)
            Log.d(TAG, "addMapLayer result: $layerAdded")

            val leftButton = AContextMenuButton(buttonId, leftText, leftText, iconName, iconName, false, true)
            val rightButton = AContextMenuButton(buttonId + 1, rightText, rightText, iconName, iconName, false, true)

            val params = ContextMenuButtonsParams(
                leftButton,
                rightButton,
                "triangulation_context_menu_id",
                application.packageName,
                layerId,
                1L, // callbackId
                ArrayList<String>() // pointsIds
            )

            val resultId = osmandService?.addContextMenuButtons(params, callback)
            Log.d(TAG, "addContextMenuButtons resultId: $resultId")
            return resultId != null && resultId >= 0L

        } catch (e: RemoteException) {
            Log.e(TAG, "Error adding context menu button", e)

            return false
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error adding context menu button", e)

            return false
        }
    }

    fun importGpxFromData(gpxData: String, fileName: String, color: String = "red", show: Boolean = true): Boolean {
        if (osmandService == null) {
            Log.e(TAG, "Service not bound, cannot import GPX")

            return false
        }

        try {
            val params = ImportGpxParams(
                FileProviderHelper.getGpxUri(application, fileName, gpxData),
                fileName,
                color,
                show
            )
            val result = osmandService?.importGpx(params)
            Log.d(TAG, "importGpx result: $result")
            return result ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error importing GPX via AIDL", e)

            return false
        }
    }

    fun removeGpx(fileName: String): Boolean {
        if (osmandService == null) {
            return false
        }

        try {
            val params = RemoveGpxParams(fileName)
            val result = osmandService?.removeGpx(params)
            Log.d(TAG, "removeGpx result: $result")
            return result ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error removing GPX via AIDL", e)

            return false
        }
    }

    fun setMapLocation(lat: Double, lon: Double, zoom: Int = 15): Boolean {
        if (osmandService == null) {
            return false
        }
        try {
            val params = net.osmand.aidlapi.map.SetMapLocationParams(lat, lon, zoom, 0f, true)
            return osmandService?.setMapLocation(params) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error setting map location via AIDL", e)
            return false
        }
    }

    companion object {
        private const val TAG = "OsmAndAidlHelper"

        fun isOsmAndInstalled(context: Context): Boolean {
            val pm = context.packageManager
            var osmandInstalled = false
            try {
                pm.getPackageInfo("net.osmand", 0)
                osmandInstalled = true
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {}

            var osmandPlusInstalled = false
            try {
                pm.getPackageInfo("net.osmand.plus", 0)
                osmandPlusInstalled = true
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {}

            return osmandInstalled || osmandPlusInstalled
        }
    }
}
