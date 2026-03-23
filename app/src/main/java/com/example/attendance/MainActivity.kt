package com.example.attendance

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendance.data.local.AppDatabase
import com.example.attendance.data.local.Employee
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.databinding.ActivityMainBinding
import com.example.attendance.ui.adapter.EmployeeAdapter
import com.example.attendance.ui.view.EmployeeDetailActivity
import com.example.attendance.ui.view.ReportsActivity
import com.example.attendance.ui.viewmodel.MainViewModel
import com.example.attendance.ui.viewmodel.MainViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(AttendanceRepository(AppDatabase.getDatabase(this).attendanceDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Handle Edge-to-Edge window insets for safe area
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val adapter = EmployeeAdapter(
            onClick = { employee ->
                val intent = Intent(this, EmployeeDetailActivity::class.java).apply {
                    putExtra("EMPLOYEE_ID", employee.id)
                    putExtra("EMPLOYEE_NAME", employee.name)
                }
                startActivity(intent)
            },
            onDeleteClick = { employee ->
                showDeleteConfirmationDialog(employee)
            }
        )

        binding.rvEmployees.layoutManager = LinearLayoutManager(this)
        binding.rvEmployees.adapter = adapter

        viewModel.allEmployees.observe(this) { employees ->
            adapter.submitList(employees)
        }

        binding.btnAddEmployee.setOnClickListener {
            showAddEmployeeDialog()
        }

        binding.btnViewReports.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }
    }

    private fun showAddEmployeeDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Employee")

        val input = EditText(this)
        input.hint = "Enter Employee Name"
        builder.setView(input)

        builder.setPositiveButton("Add") { _, _ ->
            val name = input.text.toString()
            if (name.isNotEmpty()) {
                viewModel.addEmployee(name)
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun showDeleteConfirmationDialog(employee: Employee) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Employee")
        builder.setMessage("Enter password to delete ${employee.name}")

        val input = EditText(this)
        input.hint = "Password"
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Delete") { _, _ ->
            val password = input.text.toString()
            if (password == "1234") {
                viewModel.deleteEmployee(employee)
                Toast.makeText(this, "Employee deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}
