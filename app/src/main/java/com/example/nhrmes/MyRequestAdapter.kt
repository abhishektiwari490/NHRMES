package com.example.nhrmes

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class MyRequestAdapter(
    private val list: List<Any> // Can contain EmergencyRequest OR Appointment
) : RecyclerView.Adapter<MyRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtHospital: TextView = view.findViewById(R.id.txtHospital)
        val txtBedType: TextView = view.findViewById(R.id.txtBedType)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val txtDate: TextView = view.findViewById(R.id.txtDate)
        val btnCall: Button = view.findViewById(R.id.btnCall)
        val btnMap: Button = view.findViewById(R.id.btnMap)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]

        when (item) {

            // =========================
            // EMERGENCY REQUEST
            // =========================
            is EmergencyRequest -> {

                loadHospitalDetails(holder, item.hospitalId)

                holder.txtBedType.text = "Bed Type: ${item.bedType}"

                val sdf = SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
                )
                holder.txtDate.text = sdf.format(Date(item.timestamp))

                holder.txtStatus.text = item.status
                setStatusColor(holder, item.status)

                if (item.status == "Pending") {
                    holder.btnCancel.visibility = View.VISIBLE
                    holder.btnCancel.setOnClickListener {
                        FirebaseDatabase.getInstance()
                            .getReference("EmergencyRequests")
                            .child(getRequestId(position))
                            .child("status")
                            .setValue("Cancelled")
                    }
                } else {
                    holder.btnCancel.visibility = View.GONE
                }
            }

            // =========================
            // APPOINTMENT
            // =========================
            is Appointment -> {

                loadHospitalDetails(holder, item.hospitalId)

                holder.txtBedType.text = "Doctor: ${item.specialist}"

                holder.txtDate.text =
                    if (item.appointmentDate.isNotEmpty())
                        "${item.appointmentDate} at ${item.appointmentTime}"
                    else "Waiting for Admin Schedule"

                holder.txtStatus.text = item.status
                setStatusColor(holder, item.status)

                if (item.status == "Pending") {
                    holder.btnCancel.visibility = View.VISIBLE
                    holder.btnCancel.setOnClickListener {
                        FirebaseDatabase.getInstance()
                            .getReference("Appointments")
                            .child(getAppointmentId(position))
                            .child("status")
                            .setValue("Cancelled")
                    }
                } else {
                    holder.btnCancel.visibility = View.GONE
                }
            }
        }
    }

    // =========================
    // Load Hospital Details
    // =========================
    private fun loadHospitalDetails(holder: ViewHolder, hospitalId: String) {

        FirebaseDatabase.getInstance()
            .getReference("Hospitals")
            .child(hospitalId)
            .get()
            .addOnSuccessListener { snapshot ->

                val hospitalName =
                    snapshot.child("name").value?.toString() ?: "Unknown"

                val phone =
                    snapshot.child("phone").value?.toString() ?: ""

                val lat =
                    snapshot.child("latitude").getValue(Double::class.java) ?: 0.0

                val lng =
                    snapshot.child("longitude").getValue(Double::class.java) ?: 0.0

                holder.txtHospital.text = hospitalName

                holder.btnCall.setOnClickListener {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:$phone")
                    holder.itemView.context.startActivity(intent)
                }

                holder.btnMap.setOnClickListener {
                    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($hospitalName)")
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    holder.itemView.context.startActivity(mapIntent)
                }
            }
    }

    // =========================
    // Status Color Logic
    // =========================
    private fun setStatusColor(holder: ViewHolder, status: String) {

        when (status) {
            "Approved" -> holder.txtStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
            "Rejected" -> holder.txtStatus.setBackgroundColor(Color.parseColor("#F44336"))
            "Cancelled" -> holder.txtStatus.setBackgroundColor(Color.parseColor("#9E9E9E"))
            else -> holder.txtStatus.setBackgroundColor(Color.parseColor("#FFA000"))
        }

        holder.txtStatus.setTextColor(Color.WHITE)
    }

    // =========================
    // Safe ID Helpers
    // =========================
    private fun getRequestId(position: Int): String {
        return FirebaseDatabase.getInstance().reference
            .child("EmergencyRequests")
            .push().key ?: ""
    }

    private fun getAppointmentId(position: Int): String {
        return FirebaseDatabase.getInstance().reference
            .child("Appointments")
            .push().key ?: ""
    }
}