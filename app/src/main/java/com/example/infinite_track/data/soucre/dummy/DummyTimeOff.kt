package com.example.infinite_track.data.soucre.dummy

import com.example.infinite_track.R

data class Inbox(
    val id: Int,
    val DateTime: String,
    val Name: String,
    val TypeLeave: String,
    val CardImage: Int,
    val CardStatus: String,
    val AvailableDays: String,
    val onClick: () -> Unit,
    val onDelete: () -> Unit
)

val dummyTimeOff = listOf(
    Inbox(
        id = 1,
        DateTime = "11 Aug 2023, 19:39",
        Name = "Raja Muhammad Farhan Zahputra",
        TypeLeave = "Cuti Melahirkan",
        CardImage = R.drawable.ic_profile,
        CardStatus = "Decline",
        AvailableDays = "3 days",
        onClick = { println("Card 1 clicked!") },
        onDelete = { println("Card 1 deleted!") }
    ),
    Inbox(
        id = 2,
        DateTime = "15 Feb 2024, 10:15",
        Name = "Dewi Kartika Sari",
        TypeLeave = "Cuti Tahunan",
        CardImage = android.R.drawable.ic_menu_report_image,
        CardStatus = "Approve",
        AvailableDays = "5 days",
        onClick = { println("Card 2 clicked!") },
        onDelete = { println("Card 2 deleted!") }
    ),
    Inbox(
        id = 3,
        DateTime = "20 Feb 2024, 08:25",
        Name = "Indra Saputra",
        TypeLeave = "Cuti Sakit",
        CardImage = android.R.drawable.ic_menu_report_image,
        CardStatus = "Pending",
        AvailableDays = "2 days",
        onClick = { println("Card 3 clicked!") },
        onDelete = { println("Card 3 deleted!") }
    )
)

