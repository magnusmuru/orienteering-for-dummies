package ee.taltech.orienteering.component.spinner.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import ee.taltech.orienteering.R
import ee.taltech.orienteering.component.spinner.ReplaySpinnerItems


class HistorySpinnerAdapter(context: Context) :
    ArrayAdapter<String>(context, R.layout.replay_spinner_item, R.id.txt_text, ReplaySpinnerItems.OPTIONS) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view = super.getDropDownView(position, convertView, parent!!)
        applyChangesColor(view, position)
        return view
    }

   override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        applyChangesColor(view, position)
        return view
    }

    private fun applyChangesColor(view: View, position: Int) {
        val txt = view.findViewById<TextView>(R.id.txt_text)
        txt.text = ReplaySpinnerItems.OPTIONS[position]

        val lineImg = view.findViewById<ImageView>(R.id.img_line)
        lineImg.setBackgroundColor(ReplaySpinnerItems.COLORS[txt.text as? String ?: ReplaySpinnerItems.NONE]!!.toInt())
    }
}