package com.example.infinite_track.data.repository.attendance

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.infinite_track.data.soucre.network.retrofit.ApiService
import com.example.infinite_track.domain.model.attendance.AttendanceReportPdfResult
import com.example.infinite_track.domain.repository.AttendanceReportPdfRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceReportPdfRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) : AttendanceReportPdfRepository {

    override suspend fun previewAttendanceReportPdf(
        period: String,
        startDate: String?,
        endDate: String?,
        timezone: String?
    ): Result<AttendanceReportPdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.previewAttendanceReportPdf(period, startDate, endDate, timezone)
            savePdfResponse(response, defaultPrefix = "attendance-report-preview")
        }
    }

    override suspend fun exportAttendanceReportPdf(
        period: String,
        startDate: String?,
        endDate: String?,
        timezone: String?
    ): Result<AttendanceReportPdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.exportAttendanceReportPdf(period, startDate, endDate, timezone)
            savePdfResponse(response, defaultPrefix = "attendance-report-export")
        }
    }

    private fun savePdfResponse(
        response: Response<ResponseBody>,
        defaultPrefix: String
    ): AttendanceReportPdfResult {
        if (!response.isSuccessful) {
            throw IOException("Unable to fetch attendance report PDF (${response.code()})")
        }

        val body = response.body() ?: throw IOException("Attendance report PDF body is empty")
        val fileName = response.extractFileName(defaultPrefix)
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { output ->
            body.byteStream().use { input ->
                input.copyTo(output)
            }
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return AttendanceReportPdfResult(
            fileName = fileName,
            localUri = uri
        )
    }
}

private fun Response<ResponseBody>.extractFileName(defaultPrefix: String): String {
    val header = headers()["Content-Disposition"].orEmpty()
    val quoted = Regex("filename=\"([^\"]+)\"").find(header)?.groupValues?.getOrNull(1)
    val plain = Regex("filename=([^;]+)").find(header)?.groupValues?.getOrNull(1)?.trim()
    return (quoted ?: plain ?: "$defaultPrefix.pdf").trim('"')
}
