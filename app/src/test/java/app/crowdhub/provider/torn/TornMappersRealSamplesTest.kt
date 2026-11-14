package app.crowdhub.provider.torn

import app.crowdhub.core.ui.components.StatusKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TornMappersRealSamplesTest {

    private fun loadSample(name: String): String {
        val candidates = listOf(
            File("crowd-hub-spec/03_providers/torn/_samples/$name"),
            File("../crowd-hub-spec/03_providers/torn/_samples/$name")
        )
        val file = candidates.firstOrNull { it.exists() }
        if (file != null) return file.readText()
        throw IllegalStateException("Could not locate sample $name from ${File(".").absolutePath}")
    }

    @Test
    fun `parses real user-basic sample`() {
        val info = TornMappers.mapBasic(loadSample("user-basic.json"))
        assertNotNull(info)
        assertEquals(3962657L, info!!.playerId)
        assertEquals("Kagaa", info.name)
        assertEquals(35, info.level)
        assertEquals(StatusKind.OKAY, info.status.state)
        assertNull(info.status.untilEpochSec)
    }

    @Test
    fun `parses real user-bars sample including snake_case full_time and tick_time`() {
        val bars = TornMappers.mapBars(loadSample("user-bars.json"))
        assertNotNull(bars)
        val e = bars!!.energy!!
        assertEquals(0, e.current)
        assertEquals(150, e.maximum)
        assertEquals(294L, e.tickTimeSec)
        assertEquals(17694L, e.fullTimeSec)
        assertEquals(19, bars.nerve!!.current)
        assertEquals(7794L, bars.nerve!!.fullTimeSec)
        assertEquals(4949, bars.happy!!.current)
        assertEquals(2031, bars.life!!.current)
        assertEquals(0L, bars.life!!.fullTimeSec)
    }

    @Test
    fun `parses real user-cooldowns sample`() {
        val cd = TornMappers.mapCooldowns(loadSample("user-cooldowns.json"))
        assertNotNull(cd)
        assertEquals(0L, cd!!.drugSec)
        assertEquals(0L, cd.medicalSec)
        assertEquals(722L, cd.boosterSec)
    }

    @Test
    fun `parses real user-travel landed sample and does not report actively flying when time_left is zero`() {
        val travel = TornMappers.mapTravel(loadSample("user-travel.json"))
        assertNotNull(travel)
        assertEquals("Torn", travel!!.destination)
        assertEquals("Airstrip", travel.method)
        assertEquals(1794656527L, travel.departedEpochSec)
        assertEquals(1794661867L, travel.arrivalEpochSec)
        assertEquals(0L, travel.timeLeftSec)
        assertFalse(travel.isActivelyFlying)
    }

    @Test
    fun `parses real user-money sample`() {
        val money = TornMappers.mapMoney(loadSample("user-money.json"))
        assertNotNull(money)
        assertEquals(8530229L, money!!.cashOnHand)
        assertEquals(0L, money.points)
        assertEquals(1075200L, money.cityBankAmount)
        assertEquals(236331439L, money.dailyNetworth)
    }

    @Test
    fun `parses real user-notifications sample`() {
        val notif = TornMappers.mapNotifications(loadSample("user-notifications.json"))
        assertEquals(0, notif.unreadMessages)
        assertEquals(0, notif.unreadEvents)
        assertEquals(0, notif.unreadAwards)
        assertEquals(0, notif.unreadCompetition)
    }

    @Test
    fun `parses real torn-stocks sample v2 nested market and bonus`() {
        val stocks = TornMappers.mapStocks(loadSample("torn-stocks.json"))
        assertEquals(35, stocks.size)
        val tsb = stocks.first { it.stockId == 1 }
        assertEquals("Torn & Shanghai Banking", tsb.name)
        assertEquals("TSB", tsb.acronym)
        assertEquals(1167.3, tsb.currentPrice, 0.001)
        assertEquals(34611281756826L, tsb.marketCap)
        assertEquals(29650716831L, tsb.totalShares)
        assertEquals(30917L, tsb.investors)
        assertEquals(false, tsb.bonusPassive)
        assertEquals(31, tsb.bonusFrequencyDays)
        assertEquals(3000000L, tsb.benefitRequirement)
        assertEquals("$50,000,000", tsb.benefitDescription)

        val tci = stocks.first { it.stockId == 2 }
        assertEquals("TCI", tci.acronym)
        assertEquals(true, tci.bonusPassive)
        assertEquals(7, tci.bonusFrequencyDays)
    }

    @Test
    fun `parses real faction-members sample including Traveling plane_image_type and Abroad`() {
        val members = TornMappers.mapFactionMembers(loadSample("faction-members.json"))
        assertEquals(99, members.size)

        val surgeon = members.first { it.playerId == 23278L }
        assertEquals("Surgeon", surgeon.name)
        assertEquals(StatusKind.TRAVELING, surgeon.status.state)
        assertEquals("Traveling from Torn to UAE", surgeon.status.description)
        assertEquals("private_jet", surgeon.status.planeImageType)
        assertNull(surgeon.status.untilEpochSec)

        val planeTypes = members.mapNotNull { it.status.planeImageType }.toSet()
        assertEquals(setOf("airliner", "light_aircraft", "private_jet"), planeTypes)

        val hosp = members.first { it.playerId == 2064073L }
        assertEquals(StatusKind.HOSPITAL, hosp.status.state)
        assertEquals(1794669605L, hosp.status.untilEpochSec)
        assertEquals(TravelPlace.SOUTH_AFRICA, TravelPlace.parseFromText(hosp.status.description))
    }

    @Test
    fun `parses real faction-rankedwars sample`() {
        val wars = TornMappers.mapRankedWars(
            rawJson = loadSample("faction-rankedwars.json"),
            ownFactionId = 9036L,
            nowSec = 1794668700L
        )
        assertEquals(100, wars.size)
        val first = wars.first()
        assertEquals(52345L, first.warId)
        assertEquals(9036L, first.ownFactionId)
        assertEquals(42055L, first.enemyFactionId)
        assertEquals("Dead Fragment", first.enemyFactionName)
        assertFalse(first.isActive)
    }

    @Test
    fun `parses real user-profile-other sample including nested profile faction_id and last_action`() {
        val profile = TornMappers.mapOtherProfile(
            rawJson = loadSample("user-profile-other.json"),
            fallbackPlayerId = 3960116L
        )
        assertNotNull(profile)
        assertEquals(3960116L, profile!!.playerId)
        assertEquals("Holo_", profile.name)
        assertEquals(26, profile.level)
        assertEquals(52815L, profile.factionId)
        assertEquals(StatusKind.HOSPITAL, profile.status.state)
        assertEquals(1794680893L, profile.status.untilEpochSec)
        assertEquals("Offline", profile.lastActionStatus)
        assertEquals(1794659514L, profile.lastActionTimestampSec)
        assertTrue(profile.status.description.startsWith("In hospital"))
    }
}
