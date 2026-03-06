package com.example.nhrmes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class AdminRequestAdapter(
    private val list: List<Pair<String, EmergencyRequest>>
) : RecyclerView.Adapter<AdminRequestAdapter.ViewHolder>() {

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

        val (requestId, request) = list[position]

        // Fetch hospital name safely
        FirebaseDatabase.getInstance()
            .getReference("Hospitals")
            .child(request.hospitalId)
            .get()
            .addOnSuccessListener { snap ->

                val hospitalName =
                    snap.child("name").value?.toString() ?: request.hospitalId

                holder.txtHospital.text = "Hospital: $hospitalName"
            }

        holder.txtBed.text = "Bed Type: ${request.bedType}"

        val formattedTime = SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        ).format(Date(request.timestamp))

        holder.txtTime.text = formattedTime
        holder.txtStatus.text = request.status

        // Disable buttons if not pending
        if (request.status != "Pending") {
            holder.btnApprove.isEnabled = false
            holder.btnReject.isEnabled = false
        } else {
            holder.btnApprove.isEnabled = true
            holder.btnReject.isEnabled = true
        }

        holder.btnApprove.setOnClickListener {

            if (request.status == "Pending") {

                FirebaseDatabase.getInstance()
                    .getReference("EmergencyRequests")
                    .child(requestId)
                    .child("status")
                    .setValue("Approved")

                // Reduce ICU safely
                FirebaseDatabase.getInstance()
                    .getReference("Hospitals")
                    .child(request.hospitalId)
                    .child("icuBedsAvailable")
                    .get()
                    .addOnSuccessListener { snapshot ->

                        val currentBeds =
                            snapshot.getValue(Int::class.java) ?: 0

                        if (currentBeds > 0) {
                            FirebaseDatabase.getInstance()
                                .getReference("Hospitals")
                                .child(request.hospitalId)
                                .child("icuBedsAvailable")
                                .setValue(currentBeds - 1)
                        }
                    }
            }
        }

        holder.btnReject.setOnClickListener {

            if (request.status == "Pending") {

                FirebaseDatabase.getInstance()
                    .getReference("EmergencyRequests")
                    .child(requestId)
                    .child("status")
                    .setValue("Rejected")
            }
        }
    }
}