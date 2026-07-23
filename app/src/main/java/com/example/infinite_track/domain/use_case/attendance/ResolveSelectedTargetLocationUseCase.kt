package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.SelectedTargetLocation
import com.example.infinite_track.domain.model.attendance.WorkMode
import javax.inject.Inject

class ResolveSelectedTargetLocationUseCase @Inject constructor() {
    operator fun invoke(
        mode: WorkMode,
        wfoLocation: Location?,
        wfhLocation: Location?,
        approvedWfaLocation: Location?
    ): SelectedTargetLocation {
        return when (mode) {
            WorkMode.WFO -> resolveFixedLocation(
                mode = mode,
                location = wfoLocation,
                displayName = wfoLocation?.description ?: "Lokasi kantor belum tersedia",
                unavailableReason = "Lokasi WFO belum tersedia. Coba muat ulang status attendance terlebih dahulu."
            )

            WorkMode.WFH -> resolveFixedLocation(
                mode = mode,
                location = wfhLocation,
                displayName = wfhLocation?.description ?: "Lokasi WFH belum tersedia",
                unavailableReason = "Lokasi WFH belum tersedia. Pilih WFO atau perbarui lokasi WFH terlebih dahulu."
            )

            WorkMode.WFA -> {
                SelectedTargetLocation(
                    mode = mode,
                    location = approvedWfaLocation,
                    displayName = approvedWfaLocation?.description ?: "Booking WFA belum disetujui",
                    description = approvedWfaLocation?.category,
                    isAvailable = approvedWfaLocation != null,
                    unavailableReason = if (approvedWfaLocation == null) {
                        "Booking WFA yang disetujui untuk hari ini belum tersedia."
                    } else {
                        null
                    }
                )
            }
        }
    }

    private fun resolveFixedLocation(
        mode: WorkMode,
        location: Location?,
        displayName: String,
        unavailableReason: String
    ): SelectedTargetLocation {
        return SelectedTargetLocation(
            mode = mode,
            location = location,
            displayName = displayName,
            description = location?.category,
            isAvailable = location != null,
            unavailableReason = if (location == null) unavailableReason else null
        )
    }
}
