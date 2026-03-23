package com.example.attendance.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.attendance.ui.view.AdvanceTabFragment
import com.example.attendance.ui.view.AttendanceTabFragment

class EmployeeDetailPagerAdapter(
    activity: FragmentActivity,
    private val employeeId: Int
) : FragmentStateAdapter(activity) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> AttendanceTabFragment.newInstance(employeeId)
        1 -> AdvanceTabFragment.newInstance(employeeId)
        else -> throw IllegalArgumentException("Invalid tab position: $position")
    }
}
