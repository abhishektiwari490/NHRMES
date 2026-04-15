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
    private val list: List<Pair<String, Any>>
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

        val (id, item) = list[position]

        when (item) {

            // ===================================
            // 🚑 EMERGENCY REQUEST
            // ===================================
            is EmergencyRequest -> {

                loadHospital(holder, item.hospitalId)

                holder.txtBedType.text = "Bed Type: ${item.bedType}"

                val sdf = SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
                )

                holder.txtDate.text = sdf.format(Date(item.timestamp))

                if (item.status == "Ambulance On The Way") {

                    val eta = item.ambulanceETA ?: ""
                    val distance = item.ambulanceDistance ?: ""

                    holder.txtStatus.text =
                        "🚑 Ambulance On The Way\nETA: $eta\nDistance: $distance"
                    
                    // NEW: Track Ambulance Button
                    holder.btnMap.text = "Track Live"
                    holder.btnMap.setOnClickListener {
                        val intent = Intent(holder.itemView.context, AmbulanceTrackingActivity::class.java)
                        intent.putExtra("REQUEST_ID", id)
                        holder.itemView.context.startActivity(intent)
                    }

                } else {
                    holder.txtStatus.text = item.status
                    holder.btnMap.text = "View Map"
                }

                setStatusColor(holder.txtStatus, item.status)

                if (item.status == "Pending") {

                    holder.btnCancel.visibility = View.VISIBLE

                    holder.btnCancel.setOnClickListener {

                        FirebaseDatabase.getInstance()
                            .getReference("EmergencyRequests")
                            .child(id)
                            .child("status")
                            .setValue("Cancelled")
                    }

                } else {

                    holder.btnCancel.visibility = View.GONE
                }
            }

            // ===================================
            // 🩺 APPOINTMENT
            // ===================================
            is Appointment -> {

                loadHospital(holder, item.hospitalId)

                holder.txtBedType.text = "Doctor: ${item.specialist}"

                holder.txtDate.text =
                    if (item.appointmentDate.isNotEmpty())
                        "${item.appointmentDate} at ${item.appointmentTime}"
                    else "Waiting for Admin Schedule"

                holder.txtStatus.text = item.status

                setStatusColor(holder.txtStatus, item.status)

                if (item.status == "Pending") {

                    holder.btnCancel.visibility = View.VISIBLE

                    holder.btnCancel.setOnClickListener {

                        FirebaseDatabase.getInstance()
                            .getReference("Appointments")
                            .child(id)
                            .child("status")
                            .setValue("Cancelled")
                    }

                } else {

                    holder.btnCancel.visibility = View.GONE
                }
            }
        }
    }

    // ===================================
    // Load Hospital Info
    // ===================================
    private fun loadHospital(holder: ViewHolder, hospitalId: String) {

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
                
                // Only set default map click if status is NOT tracking
                // If it is tracking, it's already handled in onBindViewHolder
                val currentStatus = holder.txtStatus.text.toString()
                if (!currentStatus.contains("Ambulance On The Way")) {
                    holder.btnMap.setOnClickListener {
                        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($hospitalName)")
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                        holder.itemView.context.startActivity(mapIntent)
                    }
                }
            }
    }

    // ===================================
    // Status Color
    // ===================================
    private fun setStatusColor(view: TextView, status: String) {

        when (status) {

            "Approved", "Completed" ->
                view.setBackgroundColor(Color.parseColor("#4CAF50"))

            "Rejected", "Cancelled" ->
                view.setBackgroundColor(Color.parseColor("#F44336"))

            "Ambulance On The Way" ->
                view.setBackgroundColor(Color.parseColor("#1976D2"))

            else ->
                view.setBackgroundColor(Color.parseColor("#FFA000"))
        }

        view.setTextColor(Color.WHITE)
    }
}