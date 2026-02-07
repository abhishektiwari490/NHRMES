package com.example.nhrmes

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HospitalAdapter(private val list: List<Hospital>) :
    RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder>() {

    inner class HospitalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.txtHospitalName)
        val distance: TextView = view.findViewById(R.id.txtDistance)
        val beds: TextView = view.findViewById(R.id.txtBeds)
        val doctors: TextView = view.findViewById(R.id.txtDoctors)
        val emergency: TextView = view.findViewById(R.id.txtEmergency)
        val callBtn: Button = view.findViewById(R.id.btnCall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HospitalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hospital, parent, false)
        return HospitalViewHolder(view)
    }

    override fun onBindViewHolder(holder: HospitalViewHolder, position: Int) {
        val hospital = list[position]

        holder.name.text = hospital.name
        holder.distance.text = "📍 ${hospital.distanceKm} km away"
        holder.beds.text = "🛏 Beds Available: ${hospital.availableBeds}"
        holder.doctors.text = "👨‍⚕️ Doctors Available: ${hospital.doctorsAvailable}"

        holder.emergency.text =
            if (hospital.emergencyReady) "🚨 Emergency Ready" else "⚠️ Limited Emergency"

        holder.callBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${hospital.phone}")
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = list.size
}
