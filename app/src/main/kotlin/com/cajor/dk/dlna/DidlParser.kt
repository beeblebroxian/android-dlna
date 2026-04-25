package com.cajor.dk.dlna

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.StringReader

object DidlParser {

    class DidlItem {
        var id: String? = null
        var parentID: String? = null
        var title: String? = null
        var creator: String? = null
        var isContainer: Boolean = false
        var childCount: Int? = null
        var url: String? = null
        var resolution: String? = null
        var albumArtUri: String? = null
        var iconUri: String? = null
        var date: String? = null
        var genre: String? = null
        private val resources = ArrayList<Resource>()

        fun addResource(resource: Resource) {
            resources.add(resource)
        }

        fun getResources(): List<Resource> = resources
    }

    open class Resource(
        val url: String,
        val resolution: String?,
        val protocolInfo: String?
    ) {
        fun isThumbnail(): Boolean {
            if (protocolInfo != null && protocolInfo.contains("image/jpeg")) {
                return true
            }
            if (resolution != null && (resolution.startsWith("0x") || resolution.startsWith("1") || resolution.startsWith("2"))) {
                return true
            }
            return false
        }

        fun getSize(): Int {
            if (resolution.isNullOrEmpty()) return 0
            return try {
                val parts = resolution.split("x")
                if (parts.size >= 2) {
                    val width = parts[0].toInt()
                    val height = parts[1].toInt()
                    width * height
                } else {
                    0
                }
            } catch (e: Exception) {
                0
            }
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    fun parse(didlXml: String): List<DidlItem> {
        val items = ArrayList<DidlItem>()

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(didlXml))

        var currentItem: DidlItem? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName == "container" || tagName == "item") {
                        currentItem = DidlItem()
                        currentItem.id = parser.getAttributeValue(null, "id")
                        currentItem.parentID = parser.getAttributeValue(null, "parentID")
                        currentItem.isContainer = (tagName == "container")

                        val childCount = parser.getAttributeValue(null, "childCount")
                        if (childCount != null) {
                            try {
                                currentItem.childCount = childCount.toInt()
                            } catch (e: NumberFormatException) {
                                // Ignore
                            }
                        }
                    } else if (currentItem != null) {
                        when (tagName) {
                            "dc:title" -> currentItem.title = parser.nextText()
                            "dc:creator" -> currentItem.creator = parser.nextText()
                            "res" -> {
                                val resUrl = parser.nextText()
                                val resolution = parser.getAttributeValue(null, "resolution")
                                val protocolInfo = parser.getAttributeValue(null, "protocolInfo")
                                currentItem.addResource(Resource(resUrl, resolution, protocolInfo))
                            }
                            "upnp:albumArtURI" -> currentItem.albumArtUri = parser.nextText()
                            "upnp:icon" -> currentItem.iconUri = parser.nextText()
                            "dc:date" -> currentItem.date = parser.nextText()
                            "upnp:genre" -> currentItem.genre = parser.nextText()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if ((tagName == "container" || tagName == "item") && currentItem != null) {
                        selectBestResource(currentItem)
                        items.add(currentItem)
                        currentItem = null
                    }
                }
            }

            eventType = parser.next()
        }

        return items
    }

    private fun selectBestResource(item: DidlItem) {
        if (item.getResources().isEmpty()) {
            return
        }

        // Find the best non-thumbnail resource
        var bestResource: Resource? = null
        var maxSize = 0

        for (resource in item.getResources()) {
            if (resource.isThumbnail()) {
                continue
            }

            val size = resource.getSize()
            if (size > maxSize) {
                maxSize = size
                bestResource = resource
            }
        }

        // If no non-thumbnail resource found, use the first one
        if (bestResource == null && item.getResources().isNotEmpty()) {
            bestResource = item.getResources()[0]
        }

        if (bestResource != null) {
            item.url = bestResource.url
            item.resolution = bestResource.resolution
        }
    }
}
