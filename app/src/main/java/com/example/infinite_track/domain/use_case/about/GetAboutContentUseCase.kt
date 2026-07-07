package com.example.infinite_track.domain.use_case.about

import com.example.infinite_track.data.soucre.local.about.AboutDummyContentProvider
import com.example.infinite_track.domain.model.about.AboutContent
import javax.inject.Inject

class GetAboutContentUseCase @Inject constructor(
    private val aboutDummyContentProvider: AboutDummyContentProvider
) {
    operator fun invoke(): AboutContent = aboutDummyContentProvider.getContent()
}
