package app.crowdhub.provider.torn

import org.junit.Assert.assertEquals
import org.junit.Test

class FlightTableRealSamplesTest {

    @Test
    fun `parses real Torn v2 outbound and returning travel descriptions`() {
        val outUae = FlightTable.parseDescription("Traveling from Torn to UAE")
        assertEquals(TravelDirection.OUT, outUae.direction)
        assertEquals(TravelPlace.UAE, outUae.place)

        val outUk = FlightTable.parseDescription("Traveling from Torn to United Kingdom")
        assertEquals(TravelDirection.OUT, outUk.direction)
        assertEquals(TravelPlace.UNITED_KINGDOM, outUk.place)

        val retCh = FlightTable.parseDescription("Traveling from Switzerland to Torn")
        assertEquals(TravelDirection.RETURN, retCh.direction)
        assertEquals(TravelPlace.SWITZERLAND, retCh.place)

        val retZa = FlightTable.parseDescription("Traveling from South Africa to Torn")
        assertEquals(TravelDirection.RETURN, retZa.direction)
        assertEquals(TravelPlace.SOUTH_AFRICA, retZa.place)
    }

    @Test
    fun `parses real Abroad and Hospital abroad descriptions including demonyms`() {
        val abroad = FlightTable.parseDescription("In Cayman Islands")
        assertEquals(TravelDirection.ABROAD, abroad.direction)
        assertEquals(TravelPlace.CAYMAN_ISLANDS, abroad.place)

        val hawaiiHosp = FlightTable.parseDescription("In a Hawaiian hospital for 3 hrs 9 mins")
        assertEquals(TravelDirection.ABROAD, hawaiiHosp.direction)
        assertEquals(TravelPlace.HAWAII, hawaiiHosp.place)

        val japanHosp = FlightTable.parseDescription("In a Japanese hospital for 38 mins")
        assertEquals(TravelDirection.ABROAD, japanHosp.direction)
        assertEquals(TravelPlace.JAPAN, japanHosp.place)

        val saHosp = FlightTable.parseDescription("In a South African hospital for 18 mins")
        assertEquals(TravelDirection.ABROAD, saHosp.direction)
        assertEquals(TravelPlace.SOUTH_AFRICA, saHosp.place)

        val tornHosp = FlightTable.parseDescription("In hospital for 10 mins")
        assertEquals(TravelDirection.UNKNOWN, tornHosp.direction)
        assertEquals(TravelPlace.UNKNOWN, tornHosp.place)
    }

    @Test
    fun `maps observed plane_image_type defaults cleanly`() {
        assertEquals(PlaneCategory.STANDARD, FlightTable.defaultCategoryForPlaneType("airliner"))
        assertEquals(PlaneCategory.AIRSTRIP, FlightTable.defaultCategoryForPlaneType("light_aircraft"))
        assertEquals(PlaneCategory.WLT, FlightTable.defaultCategoryForPlaneType("private_jet"))
        assertEquals(PlaneCategory.UNKNOWN, FlightTable.defaultCategoryForPlaneType("other_unknown"))
    }
}
