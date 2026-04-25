package com.cajor.dk.dlna

import android.app.FragmentManager
import android.app.ListActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class MainActivity : ListActivity(), ContentDirectoryBrowseTaskFragment.Callbacks,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var mFragment: ContentDirectoryBrowseTaskFragment? = null
    private lateinit var mDeviceListAdapter: CustomListAdapter
    private lateinit var mItemListAdapter: CustomListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentManager = fragmentManager
        mFragment = fragmentManager.findFragmentByTag("task") as? ContentDirectoryBrowseTaskFragment

        mDeviceListAdapter = CustomListAdapter(this)
        mItemListAdapter = CustomListAdapter(this)

        listAdapter = mDeviceListAdapter

        if (mFragment == null) {
            mFragment = ContentDirectoryBrowseTaskFragment()
            fragmentManager.beginTransaction().add(mFragment, "task").commit()
        } else {
            mFragment?.refreshDevices()
            mFragment?.refreshCurrent()
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)

        val filter = IntentFilter()
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
        registerReceiver(receiver, filter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                Toast.makeText(this, R.string.info_searching, Toast.LENGTH_SHORT).show()
                mFragment?.refreshCurrent()
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        mFragment?.refreshCurrent()
    }

    override fun onBackPressed() {
        if (mFragment?.goBack() == true) {
            super.onBackPressed()
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        mFragment?.navigateTo(l.getItemAtPosition(position))
        super.onListItemClick(l, v, position, id)
    }

    override fun onDisplayDevices() {
        runOnUiThread {
            listAdapter = mDeviceListAdapter
        }
    }

    override fun onDisplayDirectories() {
        runOnUiThread {
            mItemListAdapter.clear()
            listAdapter = mItemListAdapter
        }
    }

    override fun onDisplayItems(items: ArrayList<ItemModel>) {
        runOnUiThread {
            mItemListAdapter.clear()
            mItemListAdapter.addAll(items)
        }
    }

    override fun onDisplayItemsError(error: String) {
        runOnUiThread {
            mItemListAdapter.clear()
            mItemListAdapter.add(
                CustomListItem(
                    R.drawable.ic_warning,
                    resources.getString(R.string.info_errorlist_folders),
                    error
                )
            )
        }
    }

    override fun onDeviceAdded(device: DeviceModel) {
        runOnUiThread {
            val position = mDeviceListAdapter.getPosition(device)
            if (position >= 0) {
                mDeviceListAdapter.remove(device)
                mDeviceListAdapter.insert(device, position)
            } else {
                mDeviceListAdapter.add(device)
            }
        }
    }

    override fun onDeviceRemoved(device: DeviceModel) {
        runOnUiThread {
            mDeviceListAdapter.remove(device)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                val state = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN
                )

                val wifiWarning = findViewById<TextView>(R.id.wifi_warning)

                when (state) {
                    WifiManager.WIFI_STATE_ENABLED -> {
                        wifiWarning.visibility = View.GONE

                        mFragment?.apply {
                            refreshDevices()
                            refreshCurrent()
                        }
                    }
                    WifiManager.WIFI_STATE_DISABLED -> {
                        wifiWarning.visibility = View.VISIBLE
                        mDeviceListAdapter.clear()
                        mItemListAdapter.clear()
                    }
                    WifiManager.WIFI_STATE_UNKNOWN -> {
                        wifiWarning.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}
