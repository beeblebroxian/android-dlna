package com.cajor.dk.dlna

open class CustomListItem(icon: Int) {
    open var icon: Int = icon
    open var title: String? = null
    open var description: String? = null
    open var description2: String? = null
    open var iconUrl: String? = null
    open var hideIcon: Boolean? = null

    constructor(icon: Int, title: String?, description: String?) : this(icon) {
        this.title = title
        this.description = description
    }

    constructor(icon: Int, iconUrl: String?, hideIcon: Boolean?, title: String?, description: String?, description2: String?) : this(icon) {
        this.iconUrl = iconUrl
        this.hideIcon = hideIcon
        this.title = title
        this.description = description
        this.description2 = description2
    }

    open fun getId(): String = ""
}
