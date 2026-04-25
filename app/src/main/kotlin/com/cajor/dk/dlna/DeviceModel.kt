package com.cajor.dk.dlna

import net.mm2d.upnp.Device
import net.mm2d.upnp.Icon
import net.mm2d.upnp.Service

class DeviceModel(icon: Int, val device: Device) : CustomListItem(icon) {

    init {
        iconUrl = calculateIconUrl()
    }

    private fun calculateIconUrl(): String? {
        val icons = device.iconList
        val baseUrl = device.baseUrl

        // First try to find a large icon (>= 64x64)
        for (icon in icons) {
            if (icon.width >= 64 && icon.height >= 64 && isUsableImageType(icon.mimeType)) {
                return resolveUrl(icon.url, baseUrl)
            }
        }

        // If no large icon found, try any usable image type
        for (icon in icons) {
            if (isUsableImageType(icon.mimeType)) {
                return resolveUrl(icon.url, baseUrl)
            }
        }

        // If still no icon, return the first available icon
        for (icon in icons) {
            return resolveUrl(icon.url, baseUrl)
        }

        return null
    }

    private fun resolveUrl(url: String?, baseUrl: String?): String? {
        if (url == null) return null
        if (baseUrl == null) return url

        // If URL is already absolute, return it
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        }

        // Resolve relative URL against base URL
        try {
            val baseUri = java.net.URI(baseUrl)
            val resolvedUri = baseUri.resolve(url)
            return resolvedUri.toString()
        } catch (e: Exception) {
            return url
        }
    }

    private fun isUsableImageType(mimeType: String?): Boolean {
        if (mimeType == null) return false
        return mimeType.startsWith("image/") &&
                (mimeType == "image/png" || mimeType == "image/jpg" ||
                        mimeType == "image/jpeg" || mimeType == "image/gif")
    }


    fun getContentDirectory(): Service? {
        val services = device.serviceList

        for (current in services) {
            if (current.serviceType.contains("ContentDirectory")) {
                return current
            }
        }

        return null
    }

    override var title: String?
        get() = toString()
        set(value) { super.title = value }

    override var description: String?
        get() {
            val manufacturer = device.manufacture
            return if (manufacturer.isNullOrEmpty()) "N/A" else manufacturer
        }
        set(value) { super.description = value }

    override var description2: String?
        get() {
            val manufacturerUrl = device.manufactureUrl
            return if (manufacturerUrl.isNullOrEmpty()) "N/A" else manufacturerUrl
        }
        set(value) { super.description2 = value }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as DeviceModel
        return device == that.device
    }

    override fun hashCode(): Int = device.hashCode()

    override fun toString(): String {
        val friendlyName = device.friendlyName
        if (!friendlyName.isNullOrEmpty()) return friendlyName

        val modelName = device.modelName
        if (!modelName.isNullOrEmpty()) return modelName

        return device.udn
    }
}
