package com.example.infinite_track.presentation.screen.attendance.preparation

import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.CurrentLocationResult

data class AttendancePreparationState(
    val selectedMode: WorkMode = WorkMode.WFO,
    val targetResolution: TargetLocationResolution = TargetLocationResolution.Resolving(WorkMode.WFO),
    val currentLocation: CurrentLocationResult? = null,
    val rangeStatus: TargetRangeStatus? = null,
    val eligibility: AttendancePreparationEligibility = AttendancePreparationEligibility.Resolving
)
