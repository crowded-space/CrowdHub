package app.crowdhub.provider.torn

enum class TravelPlace(val displayName: String) {
    MEXICO("Mexico"),
    CAYMAN_ISLANDS("Cayman Islands"),
    CANADA("Canada"),
    HAWAII("Hawaii"),
    UNITED_KINGDOM("United Kingdom"),
    ARGENTINA("Argentina"),
    SWITZERLAND("Switzerland"),
    JAPAN("Japan"),
    CHINA("China"),
    UAE("UAE"),
    SOUTH_AFRICA("South Africa"),
    UNKNOWN("Unknown");

    companion object {
        fun parseFromText(raw: String?): TravelPlace {
            if (raw.isNullOrBlank()) return UNKNOWN
            val norm = raw.trim().lowercase()
            return when {
                norm.contains("mexico") || norm.contains("ciudad juarez") -> MEXICO
                norm.contains("cayman") -> CAYMAN_ISLANDS
                norm.contains("canada") || norm.contains("toronto") -> CANADA
                norm.contains("hawaii") || norm.contains("honolulu") -> HAWAII
                norm.contains("united kingdom") || norm.contains("london") || Regex("\\buk\\b").containsMatchIn(norm) -> UNITED_KINGDOM
                norm.contains("argentina") || norm.contains("buenos aires") -> ARGENTINA
                norm.contains("switzerland") || norm.contains("zurich") -> SWITZERLAND
                norm.contains("japan") || norm.contains("tokyo") -> JAPAN
                norm.contains("china") || norm.contains("beijing") -> CHINA
                norm.contains("united arab emirates") || norm.contains("dubai") || Regex("\\buae\\b").containsMatchIn(norm) -> UAE
                norm.contains("south africa") || norm.contains("johannesburg") -> SOUTH_AFRICA
                else -> UNKNOWN
            }
        }
    }
}

enum class PlaneCategory(val label: String, val index: Int) {
    STANDARD("Standard", 0),
    AIRSTRIP("Airstrip", 1),
    WLT("WLT (Private)", 2),
    BUSINESS("Business", 3),
    UNKNOWN("Unknown", -1);

    companion object {
        fun fromRaw(raw: String?): PlaneCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
    }
}

enum class TravelDirection {
    OUT,
    RETURN,
    ABROAD,
    UNKNOWN
}

data class ParsedTravelDescription(
    val direction: TravelDirection,
    val place: TravelPlace
)

object FlightTable {
    // Source of truth: crowd-hub-spec/03_providers/torn/flight-times.md (minutes WITHOUT book)
    // Columns: [Standard, Airstrip, WLT, Business]
    val tableMinutes: Map<TravelPlace, IntArray> = mapOf(
        TravelPlace.MEXICO to intArrayOf(24, 17, 12, 7),
        TravelPlace.CAYMAN_ISLANDS to intArrayOf(33, 23, 17, 10),
        TravelPlace.CANADA to intArrayOf(39, 27, 19, 12),
        TravelPlace.HAWAII to intArrayOf(127, 89, 63, 38),
        TravelPlace.UNITED_KINGDOM to intArrayOf(151, 106, 75, 45),
        TravelPlace.ARGENTINA to intArrayOf(158, 111, 79, 47),
        TravelPlace.SWITZERLAND to intArrayOf(166, 116, 83, 50),
        TravelPlace.JAPAN to intArrayOf(213, 149, 107, 64),
        TravelPlace.CHINA to intArrayOf(229, 160, 114, 69),
        TravelPlace.UAE to intArrayOf(257, 180, 128, 77),
        TravelPlace.SOUTH_AFRICA to intArrayOf(282, 197, 141, 85)
    )

    fun baseMinutes(place: TravelPlace, category: PlaneCategory): Int? {
        val row = tableMinutes[place] ?: return null
        if (category == PlaneCategory.UNKNOWN || category.index !in row.indices) return null
        return row[category.index]
    }

    fun allCategoryMinutes(place: TravelPlace): IntArray? = tableMinutes[place]

    fun parseDescription(description: String?): ParsedTravelDescription {
        if (description.isNullOrBlank()) {
            return ParsedTravelDescription(TravelDirection.UNKNOWN, TravelPlace.UNKNOWN)
        }
        val lower = description.trim().lowercase()
        val place = TravelPlace.parseFromText(description)
        val direction = when {
            lower.startsWith("returning to torn") || lower.contains("returning from") -> TravelDirection.RETURN
            lower.startsWith("traveling to") || lower.startsWith("travelling to") -> TravelDirection.OUT
            lower.startsWith("in ") -> TravelDirection.ABROAD
            else -> TravelDirection.UNKNOWN
        }
        return ParsedTravelDescription(direction, place)
    }
}
