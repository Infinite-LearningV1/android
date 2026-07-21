package com.example.infinite_track.data.soucre.local.about

import com.example.infinite_track.domain.model.about.AboutAchievement
import com.example.infinite_track.domain.model.about.AboutContent
import com.example.infinite_track.domain.model.about.AboutCreator
import com.example.infinite_track.domain.model.about.AboutFeatureChip
import com.example.infinite_track.domain.model.about.AboutHero
import com.example.infinite_track.domain.model.about.AboutImpactItem
import com.example.infinite_track.domain.model.about.AboutImpactSemantic
import com.example.infinite_track.domain.model.about.AboutOverview
import com.example.infinite_track.domain.model.about.AboutTimelineItem
import com.example.infinite_track.domain.model.about.VerificationStatus
import javax.inject.Inject

class AboutDummyContentProvider @Inject constructor() {

    fun getContent(): AboutContent = AboutContent(
        hero = AboutHero(
            appName = "Infinite Track",
            tagline = "Smart Attendance & Workforce Presence System",
            description = "Infinite Track is an internal attendance and workforce presence system designed to support employee check-in, check-out, location validation, WFA booking, attendance history, and personal work activity tracking.",
            badgeLabel = "Internship Project · 2025"
        ),
        overview = AboutOverview(
            title = "Project Overview",
            description = "Infinite Track was developed as a digital attendance solution for internal company operations. The project combines mobile attendance, location-based validation, face verification, WFA support, attendance reporting, and decision-support concepts using Fuzzy AHP while keeping backend services as the authoritative source of final attendance truth.",
            featureChips = listOf(
                AboutFeatureChip("Attendance Tracking"),
                AboutFeatureChip("Location Validation"),
                AboutFeatureChip("Fuzzy AHP Support")
            )
        ),
        timeline = listOf(
            AboutTimelineItem(
                period = "January 2025",
                title = "Project Discovery",
                description = "Requirement gathering, company workflow observation, and attendance problem analysis."
            ),
            AboutTimelineItem(
                period = "February 2025",
                title = "System Design",
                description = "Database design, mobile flow planning, dashboard concept, and Fuzzy AHP method mapping."
            ),
            AboutTimelineItem(
                period = "March 2025",
                title = "Core Development",
                description = "Attendance module, face verification, location validation, WFA booking, and backend API integration."
            ),
            AboutTimelineItem(
                period = "April 2025",
                title = "Testing & Refinement",
                description = "UI refinement, feature testing, report preparation, and partner feedback improvements."
            )
        ),
        creator = AboutCreator(
            name = "Febri",
            role = "Mobile & Fullstack Developer Intern",
            university = "Universitas Tadulako",
            program = "Informatics Engineering",
            contribution = "Designed and developed the Infinite Track ecosystem, including Android attendance flow, backend integration, dashboard concept, and Fuzzy AHP-based attendance decision-support analysis.",
            skillTags = listOf(
                "Android",
                "Backend API",
                "UI/UX Design",
                "Fuzzy AHP",
                "Attendance System"
            )
        ),
        achievement = AboutAchievement(
            title = "Achievement & Appreciation",
            subtitle = "Best Internship Project",
            description = "Infinite Track received appreciation from the internship partner as one of the best internship projects for its practical impact, digital attendance workflow, and potential to support internal company operations.",
            note = "Recognized by the internship partner for innovation, usability, and real-world implementation value.",
            verificationStatus = VerificationStatus.PreviewOnly
        ),
        impacts = listOf(
            AboutImpactItem(
                title = "Digital Attendance",
                description = "Supports structured employee presence recording.",
                semantic = AboutImpactSemantic.Attendance
            ),
            AboutImpactItem(
                title = "Operational Visibility",
                description = "Helps users and admins understand attendance activities.",
                semantic = AboutImpactSemantic.Visibility
            ),
            AboutImpactItem(
                title = "Decision Support",
                description = "Introduces Fuzzy AHP as a method-based analysis layer.",
                semantic = AboutImpactSemantic.DecisionSupport
            )
        ),
        footerNote = "Built with purpose for internship, research, and real company workflow improvement."
    )
}
