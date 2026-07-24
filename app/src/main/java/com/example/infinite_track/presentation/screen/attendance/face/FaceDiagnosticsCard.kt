package com.example.infinite_track.presentation.screen.attendance.face

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.round

/**
 * Formatted diagnostics for the success/not-matched card. Null when no score was computed.
 * Pure/JVM-testable. Similarity/threshold are shown intentionally (approved); embeddings never.
 */
data class FaceDiagnosticsUiModel(
    val similarityText: String,
    val thresholdText: String,
    val matched: Boolean
)

fun faceDiagnosticsUiModel(
    similarity: Float?,
    threshold: Float?,
    isMatch: Boolean?
): FaceDiagnosticsUiModel? {
    if (similarity == null || threshold == null || isMatch == null) return null
    fun fmt(v: Float): String =
        (round(v * 1000f) / 1000f).toString().trimEnd('0').trimEnd('.')
    return FaceDiagnosticsUiModel(fmt(similarity), fmt(threshold), isMatch)
}

/**
 * Dark rounded diagnostics card showing similarity, threshold, and result. Rendered whenever a
 * score was computed (verified & not-matched).
 */
@Composable
fun FaceDiagnosticsCard(
    model: FaceDiagnosticsUiModel,
    modifier: Modifier = Modifier
) {
    val positive = Color(0xFF56EBA1)
    val negative = Color(0xFFFF5C5C)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E24))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DiagnosticRow("Similarity Score", model.similarityText, positive)
        DiagnosticRow("Threshold", model.thresholdText, Color(0xFFFFCD29))
        DiagnosticRow(
            label = "Result",
            value = if (model.matched) "Matched" else "Not matched",
            valueColor = if (model.matched) positive else negative,
            matched = model.matched
        )
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    valueColor: Color,
    matched: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, color = valueColor, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = if (matched) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = if (matched) valueColor else Color(0xFFFF5C5C),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp)
            )
        }
    }
}
