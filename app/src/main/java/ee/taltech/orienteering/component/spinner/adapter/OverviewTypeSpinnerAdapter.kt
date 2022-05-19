package ee.taltech.orienteering.component.spinner.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import ee.taltech.orienteering.R
import ee.taltech.orienteering.component.spinner.OverviewMode


class OverviewTypeSpinnerAdapter(context: Context) :
    ArrayAdapter<String>(context, R.layout.overview_type_spinner_item, R.id.txt_text, OverviewMode.OPTIONS) {

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
        txt.text = OverviewMode.OPTIONS[position]
    }
}