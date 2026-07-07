package com.example.infinite_track.domain.model.about

data class AboutContent(
    val hero: AboutHero,
    val overview: AboutOverview,
    val timeline: List<AboutTimelineItem>,
    val creator: AboutCreator,
    val achievement: AboutAchievement,
    val impacts: List<AboutImpactItem>,
    val footerNote: String
)

data class AboutHero(
    val appName: String,
    val tagline: String,
    val description: String,
    val badgeLabel: String
)

data class AboutOverview(
    val title: String,
    val description: String,
    val featureChips: List<AboutFeatureChip>
)

data class AboutFeatureChip(
    val label: String
)

data class AboutTimelineItem(
    val period: String,
    val title: String,
    val description: String
)

data class AboutCreator(
    val name: String,
    val role: String,
    val university: String,
    val program: String,
    val contribution: String,
    val skillTags: List<String>
)

data class AboutAchievement(
    val title: String,
    val subtitle: String,
    val description: String,
    val note: String,
    val verificationStatus: VerificationStatus
)

data class AboutImpactItem(
    val title: String,
    val description: String,
    val semantic: AboutImpactSemantic
)

enum class VerificationStatus {
    PreviewOnly,
    NeedsVerification,
    Verified
}

enum class AboutImpactSemantic {
    Attendance,
    Visibility,
    DecisionSupport
}
