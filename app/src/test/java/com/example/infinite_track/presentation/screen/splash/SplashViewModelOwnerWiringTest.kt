package com.example.infinite_track.presentation.screen.splash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SplashViewModelOwnerWiringTest {
    private val projectRoot = File(requireNotNull(System.getProperty("user.dir")))

    @Test
    fun `startup splash route uses activity scoped splash view model owner`() {
        val mainActivity = source("main", "MainActivity.kt")
        val infiniteTrackApp = source("main", "InfiniteTrackApp.kt")
        val appNavGraph = source("navigation", "AppNavGraph.kt")
        val splashScreen = source("screen/splash", "SplashScreen.kt")

        assertTrue(
            "MainActivity must pass its activity-scoped SplashViewModel into InfiniteTrackApp",
            mainActivity.contains("splashViewModel = viewModel")
        )
        assertTrue(
            "InfiniteTrackApp must require and thread the shared SplashViewModel into the nav graph",
            infiniteTrackApp.contains("splashViewModel: SplashViewModel") &&
                infiniteTrackApp.contains("appNavGraph(") &&
                infiniteTrackApp.contains("splashViewModel = splashViewModel")
        )
        assertTrue(
            "AppNavGraph must require the shared SplashViewModel and pass it to SplashScreen",
            appNavGraph.contains("splashViewModel: SplashViewModel") &&
                appNavGraph.contains("SplashScreen(") &&
                appNavGraph.contains("splashViewModel = splashViewModel")
        )
        assertTrue(
            "SplashScreen must require its caller-provided SplashViewModel",
            splashScreen.contains("splashViewModel: SplashViewModel")
        )
        assertFalse(
            "SplashScreen must not create a second nav-entry-scoped SplashViewModel via hiltViewModel()",
            splashScreen.contains("splashViewModel: SplashViewModel = hiltViewModel()")
        )
    }

    private fun source(packagePath: String, fileName: String): String {
        return File(
            projectRoot,
            "src/main/java/com/example/infinite_track/presentation/$packagePath/$fileName"
        ).readText()
    }
}
