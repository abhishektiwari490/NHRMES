package com.example.nhrmes

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AppointmentActivity : AppCompatActivity() {

    private lateinit var spinnerHospital: Spinner
    private lateinit var spinnerSpecialist: Spinner
    private lateinit var spinnerDoctor: Spinner
    private lateinit var spinnerTimeSlot: Spinner
    private lateinit var tvDoctorFees: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var btnBook: Button

    private val hospitalList = mutableListOf<Pair<String, String>>()
    private val doctorList = mutableListOf<Doctor>()
    private val timeSlots = mutableListOf<String>()
    private var selectedDate: String = ""
    private var selectedDoctor: Doctor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)

        spinnerHospital = findViewById(R.id.spinnerHospital)
        spinnerSpecialist = findViewById(R.id.spinnerSpecialist)
        spinnerDoctor = findViewById(R.id.spinnerDoctor)
        spinnerTimeSlot = findViewById(R.id.spinnerTimeSlot)
        tvDoctorFees = findViewById(R.id.tvDoctorFees)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnBook = findViewById(R.id.btnBookAppointment)

        loadHospitals()
        setupSpecialistSpinner()

        // Trigger doctor loading when hospital or specialist changes
        val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadDoctors()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerHospital.onItemSelectedListener = itemSelectedListener
        spinnerSpecialist.onItemSelectedListener = itemSelectedListener

        spinnerDoctor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (doctorList.isNotEmpty() && position < doctorList.size) {
                    selectedDoctor = doctorList[position]
                    tvDoctorFees.text = "Fees: ₹${selectedDoctor?.fees}"
                    if (selectedDate.isNotEmpty()) {
                        loadAvailableSlots()
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSelectDate.setOnClickListener { showDatePicker() }
        btnBook.setOnClickListener { bookAppointment() }
    }

    private fun loadHospitals() {
        FirebaseDatabase.getInstance().getReference("Hospitals")
            .get().addOnSuccessListener { snapshot ->
                hospitalList.clear()
                val hospitalNames = mutableListOf<String>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val name = child.child("name").value?.toString() ?: "Unknown"
                    hospitalList.add(Pair(id, name))
                    hospitalNames.add(name)
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, hospitalNames)
                spinnerHospital.adapter = adapter
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load hospitals: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSpecialistSpinner() {
        val specialists = listOf("Cardiologist", "Eye Specialist", "Skin Specialist", "Orthopedic", "Neurologist", "General Physician", "Pediatrician")
        spinnerSpecialist.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, specialists)
    }

    private fun loadDoctors() {
        if (hospitalList.isEmpty()) return

        val selectedHospPos = spinnerHospital.selectedItemPosition
        if (selectedHospPos < 0) return

        val hospitalId = hospitalList[selectedHospPos].first
        val specialist = spinnerSpecialist.selectedItem.toString()

        // Fetch all doctors and filter locally to avoid indexing issues
        FirebaseDatabase.getInstance().getReference("Doctors")
            .get().addOnSuccessListener { snapshot ->
                doctorList.clear()
                val doctorNames = mutableListOf<String>()

                for (child in snapshot.children) {
                    val doc = child.getValue(Doctor::class.java)
                    if (doc != null && doc.hospitalId == hospitalId && doc.specialist == specialist) {
                        doctorList.add(doc)
                        doctorNames.add(doc.name)
                    }
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, doctorNames)
                spinnerDoctor.adapter = adapter

                if (doctorList.isEmpty()) {
                    tvDoctorFees.text = "Fees: ₹0"
                    spinnerTimeSlot.adapter = null
                    selectedDoctor = null
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load doctors: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            btnSelectDate.text = selectedDate
            loadAvailableSlots()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadAvailableSlots() {
        val doctor = selectedDoctor ?: return
        if (selectedDate.isEmpty()) return

        // Fetch all appointments and filter locally to avoid indexing issues in Firebase
        FirebaseDatabase.getInstance().getReference("Appointments")
            .get().addOnSuccessListener { snapshot ->
                val bookedSlots = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val aptDate = child.child("appointmentDate").value?.toString() ?: ""
                    val aptDocId = child.child("doctorId").value?.toString() ?: ""
                    val aptTime = child.child("appointmentTime").value?.toString() ?: ""

                    if (aptDate == selectedDate && aptDocId == doctor.id) {
                        bookedSlots.add(aptTime)
                    }
                }
                generateSlots(doctor, bookedSlots)
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to check booked slots: ${it.message}", Toast.LENGTH_SHORT).show()
                generateSlots(doctor, emptySet())
            }
    }

    private fun generateSlots(doctor: Doctor, bookedSlots: Set<String>) {
        timeSlots.clear()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        try {
            val start = Calendar.getInstance().apply { time = sdf.parse(doctor.startTime)!! }
            val end = Calendar.getInstance().apply { time = sdf.parse(doctor.endTime)!! }

            if (!start.before(end)) {
                Toast.makeText(this, "Doctor working hours are invalid (Start must be before End)", Toast.LENGTH_LONG).show()
                return
            }

            while (start.before(end)) {
                val slotStart = sdf.format(start.time)
                start.add(Calendar.MINUTE, 5)
                val slotEnd = sdf.format(start.time)
                val slotRange = "$slotStart to $slotEnd"

                if (!bookedSlots.contains(slotRange)) {
                    timeSlots.add(slotRange)
                }
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timeSlots)
            spinnerTimeSlot.adapter = adapter

            if (timeSlots.isEmpty()) {
                Toast.makeText(this, "No available slots for this date", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error generating slots. Please check Doctor time format in Database (HH:mm)", Toast.LENGTH_LONG).show()
        }
    }

    private fun bookAppointment() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val doctor = selectedDoctor ?: return
        val hospitalId = hospitalList[spinnerHospital.selectedItemPosition].first
        val timeSlot = spinnerTimeSlot.selectedItem?.toString() ?: return

        val appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments").push()
        val appointment = mapOf(
            "userId" to user.uid,
            "hospitalId" to hospitalId,
            "doctorId" to doctor.id,
            "doctorName" to doctor.name,
            "specialist" to doctor.specialist,
            "appointmentDate" to selectedDate,
            "appointmentTime" to timeSlot,
            "fees" to doctor.fees,
            "status" to "Paid",
            "timestamp" to ServerValue.TIMESTAMP
        )

        appointmentRef.setValue(appointment).addOnSuccessListener {
            Toast.makeText(this, "Appointment Booked Successfully!", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}