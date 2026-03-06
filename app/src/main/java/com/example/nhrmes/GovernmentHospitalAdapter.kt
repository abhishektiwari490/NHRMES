package com.example.nhrmes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GovernmentHospitalAdapter(
    private val list: List<Hospital>
) : RecyclerView.Adapter<GovernmentHospitalAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txtHospitalName)
        val txtResources: TextView = view.findViewById(R.id.txtResources)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gov_hospital, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val hospital = list[position]

        holder.txtName.text = hospital.name
        holder.txtResources.text =
            "ICU: ${hospital.icuBedsAvailable} | " +
                    "Oxygen: ${hospital.oxygenBedsAvailable} | " +
                    "Vent: ${hospital.ventilatorsAvailable}"
    }
}