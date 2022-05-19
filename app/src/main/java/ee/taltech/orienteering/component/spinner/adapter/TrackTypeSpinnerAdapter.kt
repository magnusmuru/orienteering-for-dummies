package ee.taltech.orienteering.component.spinner.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import ee.taltech.orienteering.R
import ee.taltech.orienteering.component.imageview.TrackTypeIcons


class TrackTypeSpinnerAdapter(context: Context) :
    ArrayAdapter<String>(context, R.layout.track_type_spinner_item, R.id.txt_text, TrackTypeIcons.OPTIONS) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view = super.getDropDownView(position, convertView, parent!!)
        applyChanges(view, position)
        return view
    }

   override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        applyChanges(view, position)
        return view
    }

    private fun applyChanges(view: View, position: Int) {
        val txt = view.findViewById<TextView>(R.id.txt_text)

        txt.text = TrackTypeIcons.OPTIONS[position]

        val icon = view.findViewById<ImageView>(R.id.img_icon)
        icon.setBackgroundResource(TrackTypeIcons.getIcon(TrackTypeIcons.getTrackType(TrackTypeIcons.OPTIONS[position])))
    }
}