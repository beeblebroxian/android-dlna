package com.cajor.dk.dlna

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import java.net.URL

class CustomListAdapter(context: Context) : ArrayAdapter<CustomListItem>(context, 0) {
    private val layout = R.layout.list
    private val context = context

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = getWorkingView(convertView)
        val viewHolder = getViewHolder(view)
        val entry = getItem(position) ?: return view

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        if (!prefs.getBoolean("settings_show_icons", true)) {
            viewHolder.imageView.setImageResource(android.R.color.transparent)
            viewHolder.imageView.visibility = View.GONE
            viewHolder.containerView.setPadding(
                16,
                viewHolder.containerView.paddingTop,
                viewHolder.containerView.paddingRight,
                viewHolder.containerView.paddingBottom
            )
        } else {
            if (entry.hideIcon == true) {
                viewHolder.imageView.setImageResource(android.R.color.transparent)
                viewHolder.imageView.visibility = View.GONE
                viewHolder.containerView.setPadding(
                    16,
                    viewHolder.containerView.paddingTop,
                    viewHolder.containerView.paddingRight,
                    viewHolder.containerView.paddingBottom
                )
            } else {
                val iconUrl = entry.iconUrl

                if (iconUrl != null && prefs.getBoolean("settings_show_device_icons", true)) {
                    // Native async image loading from URL
                    viewHolder.imageView.setImageResource(entry.icon) // placeholder
                    Thread {
                        try {
                            val url = URL(iconUrl)
                            val bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                            viewHolder.imageView.post { viewHolder.imageView.setImageBitmap(bmp) }
                        } catch (e: Exception) {
                            viewHolder.imageView.post { viewHolder.imageView.setImageResource(entry.icon) }
                        }
                    }.start()
                } else {
                    viewHolder.imageView.setImageResource(entry.icon)
                }

                viewHolder.imageView.visibility = View.VISIBLE
                viewHolder.containerView.setPadding(
                    0,
                    viewHolder.containerView.paddingTop,
                    viewHolder.containerView.paddingRight,
                    viewHolder.containerView.paddingBottom
                )
            }
        }

        viewHolder.titleView.text = entry.title

        val description = entry.description
        if (description == null) {
            viewHolder.descriptionView.visibility = View.GONE
        } else {
            viewHolder.descriptionView.visibility = View.VISIBLE
            viewHolder.descriptionView.text = description
        }

        val description2 = entry.description2
        if (description2 == null) {
            viewHolder.description2View.visibility = View.GONE
        } else {
            viewHolder.description2View.visibility = View.VISIBLE
            viewHolder.description2View.text = description2
        }

        return view
    }

    private fun getWorkingView(convertView: View?): View {
        return convertView ?: run {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            inflater.inflate(layout, null)
        }
    }

    private fun getViewHolder(workingView: View): ViewHolder {
        // The viewHolder allows us to avoid re-looking up view references
        // Since views are recycled, these references will never change
        val tag = workingView.tag
        val viewHolder: ViewHolder

        if (tag == null || tag !is ViewHolder) {
            viewHolder = ViewHolder()

            viewHolder.imageView = workingView.findViewById(R.id.icon)
            viewHolder.titleView = workingView.findViewById(R.id.title)
            viewHolder.containerView = workingView.findViewById(R.id.container)
            viewHolder.descriptionView = workingView.findViewById(R.id.description)
            viewHolder.description2View = workingView.findViewById(R.id.description2)

            workingView.tag = viewHolder
        } else {
            viewHolder = tag
        }

        return viewHolder
    }

    private class ViewHolder {
        lateinit var titleView: TextView
        lateinit var containerView: RelativeLayout
        lateinit var descriptionView: TextView
        lateinit var description2View: TextView
        lateinit var imageView: ImageView
    }
}
