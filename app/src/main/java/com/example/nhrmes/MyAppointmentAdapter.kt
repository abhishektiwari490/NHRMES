package com.example.nhrmes

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MyAppointmentAdapter(
    private val list: List<Appointment>
) : RecyclerView.Adapter<MyAppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtSpecialist: TextView = view.findViewById(R.id.txtSpecialist)
        val txtDate: TextView = view.findViewById(R.id.txtDate)
        val txtTime: TextView = view.findViewById(R.id.txtTime)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val appointment = list[position]

        holder.txtSpecialist.text =
            "Specialist: ${appointment.specialist}"

        holder.txtDate.text =
            if (appointment.appointmentDate.isNotEmpty())
                "Date: ${appointment.appointmentDate}"
            else "Date: Waiting"

        holder.txtTime.text =
            if (appointment.appointmentTime.isNotEmpty())
                "Time: ${appointment.appointmentTime}"
            else "Time: Waiting"

        holder.txtStatus.text = appointment.status

        when (appointment.status) {
            "Approved" -> holder.txtStatus.setTextColor(Color.parseColor("#2E7D32"))
            "Rejected" -> holder.txtStatus.setTextColor(Color.parseColor("#C62828"))
            else -> holder.txtStatus.setTextColor(Color.parseColor("#F9A825"))
        }
    }
}