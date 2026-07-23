package com.example.infinite_track.data.location.address

import com.example.infinite_track.domain.model.location.AddressResolutionResult
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.repository.location.AddressResolver
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class AndroidGeocoderAddressResolver @Inject constructor(
    private val dataSource: AddressGeocoderDataSource,
    private val mapper: AndroidAddressMapper
) : AddressResolver {
    override suspend fun resolve(coordinate: GeoCoordinate): AddressResolutionResult {
        return try {
            val resolved = dataSource.getAddresses(coordinate)
                .firstNotNullOfOrNull { mapper.toDomain(coordinate, it) }

            if (resolved == null) {
                AddressResolutionResult.CoordinateOnly(coordinate)
            } else {
                AddressResolutionResult.Resolved(resolved)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: GeocoderUnavailableException) {
            AddressResolutionResult.Failed.ServiceUnavailable
        } catch (_: IllegalArgumentException) {
            AddressResolutionResult.Failed.InvalidCoordinate
        } catch (_: IOException) {
            AddressResolutionResult.Failed.ServiceUnavailable
        } catch (_: Throwable) {
            AddressResolutionResult.Failed.ProviderError
        }
    }
}
