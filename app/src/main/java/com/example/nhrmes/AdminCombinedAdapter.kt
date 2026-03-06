package com.example.nhrmes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class AdminCombinedAdapter(
    private val list: List<AdminItem>
) : RecyclerView.Adapter<AdminCombinedAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtHospital: TextView = view.findViewById(R.id.txtHospital)
        val txtBed: TextView = view.findViewById(R.id.txtBed)
        val txtTime: TextView = view.findViewById(R.id.txtTime)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val btnApprove: Button = view.findViewById(R.id.btnApprove)
        val btnReject: Button = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_request, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]

        // Reset state (important for RecyclerView)
        holder.btnApprove.isEnabled = true
        holder.btnReject.isEnabled = true
        holder.btnReject.visibility = View.VISIBLE

        // =====================================================
        // 🚑 EMERGENCY REQUEST
        // =====================================================
        if (item.type == "REQUEST" && item.request != null) {

            val request = item.request

            FirebaseDatabase.getInstance()
                .getReference("Hospitals")
                .child(request.hospitalId)
                .get()
                .addOnSuccessListener {

                    val hospitalName =
                        it.child("name").value?.toString()
                            ?: request.hospitalId

                    holder.txtHospital.text = "Hospital: $hospitalName"
                }

            holder.txtBed.text =
                "Bed: ${request.bedType} | Priority: ${request.priority}"

            holder.txtTime.text = SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            ).format(Date(request.timestamp))

            holder.txtStatus.text = request.status
            setStatusColor(holder.txtStatus, request.status)

            when (request.status) {

                "Pending" -> {
                    holder.btnApprove.text = "Dispatch"
                    holder.btnReject.visibility = View.VISIBLE
                }

                "Ambulance On The Way" -> {
                    holder.btnApprove.text = "Mark Reached"
                    holder.btnReject.visibility = View.GONE
                }

                "Reached" -> {
                    holder.btnApprove.text = "Complete"
                    holder.btnReject.visibility = View.GONE
                }

                "Completed", "Rejected" -> {
                    holder.btnApprove.isEnabled = false
                    holder.btnReject.visibility = View.GONE
                }
            }

            holder.btnApprove.setOnClickListener {

                val newStatus = when (request.status) {

                    "Pending" -> "Ambulance On The Way"

                    "Ambulance On The Way" -> "Reached"

                    "Reached" -> "Completed"

                    else -> request.status
                }

                FirebaseDatabase.getInstance()
                    .getReference("EmergencyRequests")
                    .child(item.id)
                    .child("status")
                    .setValue(newStatus)
            }

            holder.btnReject.setOnClickListener {

                if (request.status == "Pending") {

                    FirebaseDatabase.getInstance()
                        .getReference("EmergencyRequests")
                        .child(item.id)
                        .child("status")
                        .setValue("Rejected")
                }
            }
        }

        // =====================================================
        // 🩺 APPOINTMENT REQUEST
        // =====================================================
        else if (item.type == "APPOINTMENT" && item.appointment != null) {

            val appointment = item.appointment

            FirebaseDatabase.getInstance()
                .getReference("Hospitals")
                .child(appointment.hospitalId)
                .get()
                .addOnSuccessListener {

                    val hospitalName =
                        it.child("name").value?.toString()
                            ?: appointment.hospitalId

                    holder.txtHospital.text =
                        "Hospital: $hospitalName\nDoctor: ${appointment.specialist}"
                }

            holder.txtBed.text = "Patient ID: ${appointment.userId}"

            holder.txtTime.text =
                if (appointment.appointmentDate.isNotEmpty())
                    "${appointment.appointmentDate} at ${appointment.appointmentTime}"
                else
                    "Waiting for Schedule"

            holder.txtStatus.text = appointment.status
            setStatusColor(holder.txtStatus, appointment.status)

            if (appointment.status != "Pending") {
                holder.btnApprove.isEnabled = false
                holder.btnReject.isEnabled = false
            }

            holder.btnApprove.setOnClickListener {

                if (appointment.status == "Pending") {

                    val calendar = Calendar.getInstance()

                    DatePickerDialog(
                        holder.itemView.context,
                        { _, year, month, day ->

                            TimePickerDialog(
                                holder.itemView.context,
                                { _, hour, minute ->

                                    val date = "$day/${month + 1}/$year"
                                    val time = "$hour:$minute"

                                    val ref = FirebaseDatabase.getInstance()
                                        .getReference("Appointments")
                                        .child(item.id)

                                    ref.child("status").setValue("Approved")
                                    ref.child("appointmentDate").setValue(date)
                                    ref.child("appointmentTime").setValue(time)

                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()

                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            }

            holder.btnReject.setOnClickListener {

                if (appointment.status == "Pending") {

                    FirebaseDatabase.getInstance()
                        .getReference("Appointments")
                        .child(item.id)
                        .child("status")
                        .setValue("Rejected")
                }
            }
        }
    }

    private fun setStatusColor(view: TextView, status: String) {

        when (status) {

            "Pending" ->
                view.setBackgroundColor(Color.parseColor("#FFA000"))

            "Ambulance On The Way" ->
                view.setBackgroundColor(Color.parseColor("#1976D2"))

            "Reached" ->
                view.setBackgroundColor(Color.parseColor("#0097A7"))

            "Completed", "Approved" ->
                view.setBackgroundColor(Color.parseColor("#4CAF50"))

            "Rejected" ->
                view.setBackgroundColor(Color.parseColor("#F44336"))
        }

        view.setTextColor(Color.WHITE)
    }
}