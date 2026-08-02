package com.example.infinite_track.data.soucre.network.retrofit

import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.Query

class WfaApiContractTest {

    @Test
    fun `recommendation endpoint declares lat lng and schedule date queries`() {
        val method = ApiService::class.java.declaredMethods.single {
            it.name == "getWfaRecommendations"
        }
        val queryNames = method.parameterAnnotations
            .flatMap { annotations -> annotations.filterIsInstance<Query>() }
            .map(Query::value)

        assertEquals(listOf("lat", "lng", "schedule_date"), queryNames)
    }
}
