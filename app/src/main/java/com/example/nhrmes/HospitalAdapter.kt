package com.example.nhrmes

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class HospitalAdapter(private val list: List<Hospital>) :
    RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder>() {

    class HospitalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.txtHospitalName)
        val location: TextView = view.findViewById(R.id.txtLocation)
        val icuBeds: TextView = view.findViewById(R.id.txtICU)
        val oxygenBeds: TextView = view.findViewById(R.id.txtOxygen)
        val emergency: TextView = view.findViewById(R.id.txtEmergency)
        val distance: TextView = view.findViewById(R.id.txtDistance)
        val callBtn: Button = view.findViewById(R.id.btnCall)
        val mapBtn: Button = view.findViewById(R.id.btnMap)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HospitalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hospital, parent, false)
        return HospitalViewHolder(view)
    }

    override fun onBindViewHolder(holder: HospitalViewHolder, position: Int) {

        val hospital = list[position]
        val df = DecimalFormat("#.##")

        holder.name.text = hospital.name
        holder.location.text = "Location: ${hospital.location}"
        holder.icuBeds.text = "ICU Beds: ${hospital.icuBedsAvailable}"
        holder.oxygenBeds.text = "Oxygen Beds: ${hospital.oxygenBedsAvailable}"
        holder.distance.text = "Distance: ${df.format(hospital.distance)} km"

        // Low capacity warning
        if (hospital.icuBedsAvailable < 2) {
            holder.icuBeds.setTextColor(Color.RED)
        } else {
            holder.icuBeds.setTextColor(Color.BLACK)
        }

        holder.emergency.text =
            if (hospital.emergencyReady) "🟢 Emergency Ready"
            else "🔴 Limited Emergency"

        holder.callBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${hospital.phone}")
            holder.itemView.context.startActivity(intent)
        }

        holder.mapBtn.setOnClickListener {
            val uri = Uri.parse(
                "geo:${hospital.latitude},${hospital.longitude}?q=${hospital.latitude},${hospital.longitude}(${hospital.name})"
            )
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            holder.itemView.context.startActivity(mapIntent)
        }
    }

    override fun getItemCount(): Int = list.size
}