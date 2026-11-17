package app.reelguard.detection

/**
 * A narrowly scoped, locally stored set of selectors that has been verified on one
 * Instagram build. The starter build intentionally ships with no verified selectors.
 *
 * Selectors may only describe stable UI metadata such as package-owned resource IDs,
 * view roles, and structural relationships. Captions, usernames, message bodies, and
 * any other user-generated text must never be used as selectors or written to disk.
 */
data class CompatibilityProfile(
    val instagramVersionName: String,
    val interfaceLanguageTag: String,
    val chatRootResourceIds: Set<String>,
    val sharedReelActivationResourceIds: Set<String>,
    val reelViewerResourceIds: Set<String>,
    val contentMarkerResourceIds: Set<String>,
    val contentChangeDetectionVerified: Boolean,
) {
    val isGeneralBlockingVerified: Boolean
        get() = instagramVersionName.isNotBlank() && reelViewerResourceIds.isNotEmpty()

    val isDmExceptionVerified: Boolean
        get() =
            isGeneralBlockingVerified &&
                chatRootResourceIds.isNotEmpty() &&
                sharedReelActivationResourceIds.isNotEmpty() &&
                contentMarkerResourceIds.isNotEmpty() &&
                contentChangeDetectionVerified

    companion object {
        /**
         * Safe default: no Instagram screen is recognised until an on-device probe
         * has established selectors and content-change signals for the target build.
         */
        val UNVERIFIED = CompatibilityProfile(
            instagramVersionName = "",
            interfaceLanguageTag = "",
            chatRootResourceIds = emptySet(),
            sharedReelActivationResourceIds = emptySet(),
            reelViewerResourceIds = emptySet(),
            contentMarkerResourceIds = emptySet(),
            contentChangeDetectionVerified = false,
        )
    }
}
