package com.cajor.dk.dlna

import android.app.Activity
import android.app.Fragment
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import net.mm2d.upnp.ControlPoint
import net.mm2d.upnp.Device
import net.mm2d.upnp.Service
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.Stack

class ContentDirectoryBrowseTaskFragment : Fragment() {

    interface Callbacks {
        fun onDisplayDevices()
        fun onDisplayDirectories()
        fun onDisplayItems(items: ArrayList<ItemModel>)
        fun onDisplayItemsError(error: String)
        fun onDeviceAdded(device: DeviceModel)
        fun onDeviceRemoved(device: DeviceModel)
    }

    private var mCallbacks: Callbacks? = null
    private var mService: DlnaService? = null
    private var mControlPoint: ControlPoint? = null
    private var mDiscoveryListener: ControlPoint.DiscoveryListener? = null
    private val mFolders = Stack<ItemModel>()
    private var mIsShowingDeviceList: Boolean = true
    private var mCurrentDevice: DeviceModel? = null
    private var mActivity: Activity? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)

        mActivity = activity
        mCallbacks = activity as Callbacks
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        bindServiceConnection()
    }

    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
    }

    override fun onDestroy() {
        super.onDestroy()

        unbindServiceConnection()
    }

    fun navigateTo(model: Any) {
        if (model is DeviceModel) {
            val conDir = model.getContentDirectory()

            if (conDir != null) {
                browseContentDirectory(conDir, "0")
                mCallbacks?.onDisplayDirectories()

                mIsShowingDeviceList = false
                mCurrentDevice = model
            }
        }

        if (model is ItemModel) {
            if (model.isContainer()) {
                if (mFolders.isEmpty()) {
                    mFolders.push(model)
                } else {
                    if (mFolders.peek().getId() != model.getId()) {
                        mFolders.push(model)
                    }
                }

                browseContentDirectory(model.service, model.getId())
            } else {
                try {
                    val uri = Uri.parse(model.getUrl())
                    val mime = MimeTypeMap.getSingleton()
                    val type = mime.getMimeTypeFromUrl(uri.toString())
                    val intent = Intent()
                    intent.action = android.content.Intent.ACTION_VIEW
                    intent.setDataAndType(uri, type)
                    startActivity(intent)
                } catch (ex: NullPointerException) {
                    Toast.makeText(mActivity, R.string.info_could_not_start_activity, Toast.LENGTH_SHORT)
                        .show()
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(mActivity, R.string.info_no_handler, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    fun goBack(): Boolean {
        if (mFolders.empty()) {
            if (!mIsShowingDeviceList) {
                mIsShowingDeviceList = true
                mCallbacks?.onDisplayDevices()
            } else {
                return true
            }
        } else {
            val item = mFolders.pop()
            browseContentDirectory(item.service, item.getContainer()?.parentID ?: "0")
        }

        return false
    }

    fun refreshDevices() {
        mControlPoint?.search(null)
    }

    fun refreshCurrent() {
        if (mControlPoint == null) return

        if (mIsShowingDeviceList) {
            mCallbacks?.onDisplayDevices()
            mControlPoint?.search(null)
        } else {
            if (!mFolders.empty()) {
                val item = mFolders.peek()
                if (item == null) return
                browseContentDirectory(item.service, item.getId())
            } else {
                mCurrentDevice?.let {
                    val service = it.getContentDirectory()
                    if (service != null) {
                        browseContentDirectory(service, "0")
                    }
                }
            }
        }
    }

    private fun browseContentDirectory(service: Service, objectId: String) {
        if (mControlPoint == null) return

        val browseAction = service.findAction("Browse")
        if (browseAction == null) {
            mCallbacks?.onDisplayItemsError("Browse action not found")
            return
        }

        val arguments = HashMap<String, String>()
        arguments["ObjectID"] = objectId
        arguments["BrowseFlag"] = "BrowseDirectChildren"
        arguments["Filter"] = "*"
        arguments["StartingIndex"] = "0"
        arguments["RequestedCount"] = "99999"
        arguments["SortCriteria"] = ""

        browseAction.invoke(arguments, false, { result ->
            val resultXml = result["Result"]
            if (resultXml != null) {
                try {
                    val didlItems = DidlParser.parse(resultXml)
                    val items = ArrayList<ItemModel>()

                    for (didlItem in didlItems) {
                        val itemModel = createItemModel(service, didlItem)
                        items.add(itemModel)
                    }

                    // Log scan result
                    if (items.isNotEmpty()) {
                        val sb = StringBuilder()
                        sb.append("Scan result: ")
                        sb.append(items.size).append(" items\n")
                        for (item in items) {
                            sb.append(item.toString()).append("\n")
                        }
                        Log.d("DLNA_SCAN", sb.toString())
                    }

                    mCallbacks?.let {
                        mainHandler.post { it.onDisplayItems(items) }
                    }
                } catch (e: IOException) {
                    Log.e("DLNA", "Failed to parse DIDL: " + e.message)
                    mCallbacks?.let {
                        mainHandler.post { it.onDisplayItemsError("Failed to parse content: " + e.message) }
                    }
                } catch (e: XmlPullParserException) {
                    Log.e("DLNA", "Failed to parse DIDL: " + e.message)
                    mCallbacks?.let {
                        mainHandler.post { it.onDisplayItemsError("Failed to parse content: " + e.message) }
                    }
                }
            } else {
                mCallbacks?.let {
                    mainHandler.post { it.onDisplayItemsError("No result returned") }
                }
            }
        }, { error ->
            Log.e("DLNA", "Browse action failed: $error")
            mCallbacks?.let {
                mainHandler.post { it.onDisplayItemsError("Browse failed: $error") }
            }
        })
    }

    private fun createItemModel(service: Service, didlItem: DidlParser.DidlItem): ItemModel {
        val itemModel = ItemModel(activity.resources, R.drawable.ic_folder, service, didlItem)

        var usableIcon = didlItem.iconUri
        if (usableIcon.isNullOrEmpty()) {
            usableIcon = didlItem.albumArtUri
        }
        if (!usableIcon.isNullOrEmpty()) {
            itemModel.iconUrl = usableIcon
        }

        if (!didlItem.isContainer) {
            itemModel.icon = R.drawable.ic_file

            val prefs = PreferenceManager.getDefaultSharedPreferences(mActivity)

            if (prefs.getBoolean("settings_hide_file_icons", false)) {
                itemModel.hideIcon = true
            }

            if (prefs.getBoolean("settings_show_extensions", false)) {
                itemModel.setShowExtension(true)
            }
        }

        return itemModel
    }

    private fun bindServiceConnection(): Boolean {
        val context = mActivity?.applicationContext ?: return false

        context.bindService(
            Intent(mActivity, DlnaService::class.java),
            serviceConnection, Context.BIND_AUTO_CREATE
        )

        return true
    }

    private fun unbindServiceConnection(): Boolean {
        val listener = mDiscoveryListener
        if (mControlPoint != null && listener != null) {
            mControlPoint!!.removeDiscoveryListener(listener)
        }

        val context = mActivity?.applicationContext ?: return false

        context.unbindService(serviceConnection)
        return true
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            if (service !is DlnaService.LocalBinder) {
                Log.e("DLNA", "Service is not an instance of DlnaService!")
                return
            }
            val binder = service
            mService = binder.getService()
            mControlPoint = mService?.getControlPoint()

            if (mControlPoint == null) {
                Log.e("DLNA", "ControlPoint is null in onServiceConnected!")
                return
            }

            mDiscoveryListener = object : ControlPoint.DiscoveryListener {
                override fun onDiscover(device: Device) {
                    deviceAdded(device)
                }

                override fun onLost(device: Device) {
                    deviceRemoved(device)
                }
            }

            val listener = mDiscoveryListener
            if (listener != null) {
                mControlPoint?.addDiscoveryListener(listener)
            }

            // Add already discovered devices
            val devices = mControlPoint?.deviceList
            if (devices != null) {
                for (device in devices) {
                    deviceAdded(device)
                }
            }

            mControlPoint?.search(null)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mService = null
            mControlPoint = null
        }
    }

    private fun deviceAdded(device: Device) {
        val deviceModel = DeviceModel(R.drawable.ic_device, device)

        val conDir = deviceModel.getContentDirectory()
        if (conDir != null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(mActivity)
            if (prefs.getBoolean("settings_validate_devices", false)) {
                // Test browse to validate device
                browseContentDirectory(conDir, "0")
            }
            mCallbacks?.let {
                mainHandler.post { it.onDeviceAdded(deviceModel) }
            }
        }
    }

    private fun deviceRemoved(device: Device) {
        mCallbacks?.let {
            mainHandler.post { it.onDeviceRemoved(DeviceModel(R.drawable.ic_device, device)) }
        }
    }
}
