# INF-238 Places search verification

The app uses Places SDK for Android (New), not direct Places Web Service calls.
The API key remains injected from `local.properties`; no key value is stored in
the repository.

## Live probe result

- Package: `com.example.infinite_track`
- Debug signing SHA-1: `C9:B9:00:8E:47:01:94:25:69:44:10:E5:8A:F0:9C:26:47:59:0F:8C`
- Autocomplete request: `Kopi Palu`
- Result: Places status `9011` (`REQUEST_DENIED`), with the message that requests
  from this Android client application are blocked.

## Google Cloud action required

1. Enable **Places API (New)** for the Google Cloud project used by this build.
2. Confirm billing is active for that project.
3. On the Android-restricted API key, authorize the package and SHA-1 above.
4. Ensure the key's API restrictions allow **Places API (New)**.
5. Add each release signing certificate separately when testing a release build.

After Cloud configuration propagates, verify search on a device with a query
such as `Kopi Palu`, select a prediction, and confirm the map preview moves to
the resolved place.
