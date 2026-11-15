package app.crowdhub.feature.browser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import app.crowdhub.provider.torn.TornUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserIconResolverTest {
    @Test
    fun `resolves Torn URLs to expected section icons and updated endpoints`() {
        assertEquals("https://www.torn.com/page.php?sid=attack&user2ID=12345", TornUrls.attack(12345L))
        assertEquals("https://www.torn.com/page.php?sid=gym", TornUrls.GYM)
        assertEquals("https://www.torn.com/page.php?sid=crimes", TornUrls.CRIMES)
        assertTrue(TornUrls.isHospitalListUrl(TornUrls.HOSPITAL_LIST))
        assertTrue(TornUrls.isHospitalListUrl("https://www.torn.com/page.php?sid=hospital"))

        assertEquals(Icons.Outlined.LocalHospital, BrowserIconResolver.resolve(TornUrls.HOSPITAL_LIST).icon)
        assertEquals(Icons.Outlined.LocalHospital, BrowserIconResolver.resolve("https://www.torn.com/page.php?sid=hospital").icon)
        assertEquals(Icons.Outlined.FitnessCenter, BrowserIconResolver.resolve(TornUrls.GYM).icon)
        assertEquals(Icons.Outlined.Psychology, BrowserIconResolver.resolve(TornUrls.CRIMES).icon)
        assertEquals(Icons.Outlined.Groups, BrowserIconResolver.resolve(TornUrls.FACTION).icon)
        assertEquals(Icons.Outlined.GpsFixed, BrowserIconResolver.resolve(TornUrls.attack(12345L)).icon)
        assertEquals(Icons.Outlined.Person, BrowserIconResolver.resolve(TornUrls.profile(12345L)).icon)
        assertEquals(Icons.Outlined.FlightTakeoff, BrowserIconResolver.resolve(TornUrls.TRAVEL).icon)
        assertEquals(Icons.Outlined.Home, BrowserIconResolver.resolve(TornUrls.HOME).icon)
        assertEquals(Icons.Outlined.Language, BrowserIconResolver.resolve("https://www.torn.com/unknownpage.php").icon)
    }
}
