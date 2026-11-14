package app.crowdhub.provider.torn

object TornRules {
    data class DisclosureRow(
        val label: String,
        val value: String
    )

    // VERIFY wording against Torn's current API rules table
    val API_DISCLOSURE = listOf(
        DisclosureRow("Data storage", "Only locally on the device."),
        DisclosureRow("Data sharing", "Nobody (exception: FFScouter, only if the user enables it)."),
        DisclosureRow("Purpose of use", "Personal, non-commercial companion app."),
        DisclosureRow("Key storage", "Locally, encrypted (Android Keystore)."),
        DisclosureRow("Key sharing", "Not shared (exception: FFScouter, opt-in)."),
        DisclosureRow("Access level required", "Limited.")
    )

    const val UNOFFICIAL_DISCLAIMER =
        "Unofficial app. Not affiliated with or endorsed by Torn."

    const val COMPLIANCE_REMINDER =
        "Complying with Torn's rules on accounts, scripting, and API usage is your responsibility. " +
            "Crowd Hub never automates gameplay actions or fetches non-API Torn pages in the background."
}
