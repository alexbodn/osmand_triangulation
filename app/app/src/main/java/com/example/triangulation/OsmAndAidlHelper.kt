package com.example.triangulation

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import net.osmand.aidl.IOsmAndAidlCallback
import net.osmand.aidl.IOsmAndAidlInterface
import net.osmand.aidl.contextmenu.AContextMenuButton
import net.osmand.aidl.contextmenu.ContextMenuButtonsParams
import net.osmand.aidl.maplayer.AddMapLayerParams
import net.osmand.aidl.maplayer.point.AddMapPointParams

class OsmAndAidlHelper(
    private val application: Application,
    private val listener: OsmAndAidlListener?
) : ServiceConnection {

    interface OsmAndAidlListener {
        fun onOsmAndServiceConnected()
        fun onOsmAndServiceDisconnected()
        fun onContextMenuButtonClicked(buttonId: Int, pointId: String?, layerId: String?)
    }

    private var mOsmAndAidlInterface: IOsmAndAidlInterface? = null

    private val callback = object : IOsmAndAidlCallback.Stub() {
        override fun onUpdate() {}
        override fun onKeyboardAppeared() {}
        override fun onKeyboardDisappeared() {}
        override fun onContextMenuButtonClicked(buttonId: Int, pointId: String?, layerId: String?) {
            listener?.onContextMenuButtonClicked(buttonId, pointId, layerId)
        }
    }

    fun bindService() {
        val intent = Intent("net.osmand.aidl.OsmandAidlService")
        intent.setPackage("net.osmand.plus")
        val success = application.bindService(intent, this, Context.BIND_AUTO_CREATE)
        if (!success) {
            Log.e("OsmAndAidlHelper", "Failed to bind to OsmAnd plus service, trying free version")
            intent.setPackage("net.osmand")
            val successFree = application.bindService(intent, this, Context.BIND_AUTO_CREATE)
            if(!successFree){
                Log.e("OsmAndAidlHelper", "Failed to bind to OsmAnd service completely")
            }
        }
    }

    fun unbindService() {
        try {
            application.unbindService(this)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        mOsmAndAidlInterface = IOsmAndAidlInterface.Stub.asInterface(service)
        listener?.onOsmAndServiceConnected()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        mOsmAndAidlInterface = null
        listener?.onOsmAndServiceDisconnected()
    }

    fun addContextMenuButton(id: Int, text: String, layerId: String): Boolean {
        val button = AContextMenuButton(id, text, text, "ic_action_settings", "ic_action_settings", false, true)
        val params = ContextMenuButtonsParams(button, button, id.toString(), application.packageName, layerId, 1, emptyList())
        return try {
            mOsmAndAidlInterface?.addContextMenuButtons(params, callback)
            Log.d("OsmAndAidlHelper", "Successfully registered Context Menu Button: $text")
            true
        } catch (e: RemoteException) {
            Log.e("OsmAndAidlHelper", "Failed to register Context Menu Button", e)
            e.printStackTrace()
            false
        }
    }

    fun addMapLayer(layerId: String, layerName: String): Boolean {
        val params = AddMapLayerParams(layerId, layerName, 5.0f)
        return try {
            mOsmAndAidlInterface?.addMapLayer(params)
            Log.d("OsmAndAidlHelper", "Successfully registered Map Layer: $layerName")
            true
        } catch (e: RemoteException) {
            Log.e("OsmAndAidlHelper", "Failed to register Map Layer", e)
            e.printStackTrace()
            false
        }
    }

    fun addMapPoint(layerId: String, pointId: String, lat: Double, lon: Double, name: String, color: Int = Color.RED): Boolean {
        val params = AddMapPointParams(layerId, pointId, lat, lon, name, "ic_action_marker_dark", true, color, application.packageName)
        return try {
            mOsmAndAidlInterface?.addMapPoint(params)
            Log.d("OsmAndAidlHelper", "Successfully registered Map Point: $name")
            true
        } catch (e: RemoteException) {
            Log.e("OsmAndAidlHelper", "Failed to register Map Point", e)
            e.printStackTrace()
            false
        }
    }
}
