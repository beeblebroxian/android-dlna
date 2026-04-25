package com.cajor.dk.dlna

import android.content.res.Resources
import android.net.Uri
import net.mm2d.upnp.Service

class ItemModel(
    private val res: Resources,
    icon: Int,
    val service: Service,
    private val item: DidlParser.DidlItem
) : CustomListItem(icon) {

    private var showExtension: Boolean = false

    fun getUrl(): String {
        if (item.url.isNullOrEmpty()) return "N/A"
        return item.url!!
    }

    fun setShowExtension(show: Boolean) {
        this.showExtension = show
    }

    fun getItem(): DidlParser.DidlItem? {
        return if (isContainer()) null else item
    }

    fun getContainer(): DidlParser.DidlItem? {
        return if (!isContainer()) null else item
    }


    fun isContainer(): Boolean = item.isContainer

    override fun getId(): String = item.id ?: ""

    override var title: String?
        get() {
            return if (showExtension) {
                item.title + "." + MimeTypeMap.getFileExtensionFromUrl(getUrl())
            } else {
                item.title
            }
        }
        set(value) { super.title = value }

    override var description: String?
        get() {
            if (isContainer()) {
                val children = item.childCount
                return if (children != null) {
                    "$children ${res.getString(R.string.info_items)}"
                } else {
                    res.getString(R.string.info_folder)
                }
            }

            val date = item.date
            if (!date.isNullOrEmpty()) return date

            val resolution = item.resolution
            if (!resolution.isNullOrEmpty()) return resolution

            val creator = item.creator
            if (!creator.isNullOrEmpty()) {
                return if (creator.startsWith("Unknown")) null else creator
            }

            return res.getString(R.string.info_file)
        }
        set(value) { super.description = value }

    override var description2: String?
        get() {
            if (!isContainer()) {
                val uri = Uri.parse(getUrl())
                val mime = MimeTypeMap.getSingleton()
                return mime.getMimeTypeFromUrl(uri.toString())
            }

            val genre = item.genre
            return if (genre.isNullOrEmpty() || genre.startsWith("Unknown")) null else genre
        }
        set(value) { super.description2 = value }
}
