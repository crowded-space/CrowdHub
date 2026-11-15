package app.crowdhub.provider.torn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TornFactionMappersTest {

    @Test
    fun `mapFactionBasic parses v2 basic wrapper`() {
        val raw = """
            {
              "basic": {
                "id": 9999,
                "name": "Crow Syndicate",
                "tag": "CROW",
                "respect": 1234567,
                "capacity": 100,
                "members_count": 84,
                "best_chain": 25000,
                "leader_id": 111,
                "co_leader_id": 222,
                "is_enlisted": true
              }
            }
        """.trimIndent()

        val basic = TornMappers.mapFactionBasic(raw)
        assertNotNull(basic)
        assertEquals(9999L, basic?.factionId)
        assertEquals("Crow Syndicate", basic?.name)
        assertEquals("CROW", basic?.tag)
        assertEquals(1234567L, basic?.respect)
        assertEquals(100, basic?.capacity)
        assertEquals(84, basic?.memberCount)
        assertEquals(25000L, basic?.bestChain)
    }

    @Test
    fun `mapFactionMembers exposes position and daysInFaction`() {
        val raw = """
            {
              "members": [
                {
                  "id": 3962657,
                  "name": "Kagaa",
                  "level": 45,
                  "days_in_faction": 120,
                  "position": "Recruiter",
                  "is_revivable": true,
                  "status": {
                    "state": "Traveling",
                    "description": "Traveling to Switzerland",
                    "plane_image_type": "private_jet"
                  },
                  "last_action": {
                    "status": "Online",
                    "relative": "1 min ago",
                    "timestamp": 1700000000
                  }
                }
              ]
            }
        """.trimIndent()

        val members = TornMappers.mapFactionMembers(raw)
        assertEquals(1, members.size)
        val m = members.first()
        assertEquals(3962657L, m.playerId)
        assertEquals("Recruiter", m.position)
        assertEquals(120, m.daysInFaction)
        assertEquals(true, m.isRevivable)
        assertEquals("private_jet", m.status.planeImageType)
    }

    @Test
    fun `mapFactionChain parses active and cooldown states`() {
        val activeRaw = """
            {
              "chain": {
                "id": 555,
                "current": 248,
                "max": 250,
                "timeout": 195,
                "modifier": 1.62,
                "cooldown": 0,
                "start": 1700000000
              }
            }
        """.trimIndent()

        val active = TornMappers.mapFactionChain(activeRaw)
        assertNotNull(active)
        assertTrue(active!!.isActive)
        assertFalse(active.isInCooldown)
        assertEquals(248, active.current)
        assertEquals(250, active.max)
        assertEquals(195, active.timeoutSec)
        assertEquals(1.62, active.modifier, 0.001)
    }

    @Test
    fun `mapFactionAttacks parses nested v2 actors and modifiers`() {
        val raw = """
            {
              "attacks": [
                {
                  "id": 987654,
                  "started": 1700000100,
                  "ended": 1700000125,
                  "attacker": {
                    "id": 1001,
                    "name": "Alpha",
                    "faction": { "id": 9999, "name": "Crow Syndicate" }
                  },
                  "defender": {
                    "id": 2002,
                    "name": "Beta",
                    "faction": { "id": 8888, "name": "Rivals" }
                  },
                  "result": "Hospitalized",
                  "respect_gain": 4.75,
                  "chain": 42,
                  "modifiers": { "fair_fight": 3.0 },
                  "is_interrupted": false,
                  "is_stealthed": false
                }
              ]
            }
        """.trimIndent()

        val attacks = TornMappers.mapFactionAttacks(raw)
        assertNotNull(attacks)
        assertEquals(1, attacks!!.size)
        val a = attacks.first()
        assertEquals("Alpha", a.attackerName)
        assertEquals(9999L, a.attackerFactionId)
        assertEquals("Hospitalized", a.result)
        assertEquals(4.75, a.respectGain, 0.001)
        assertEquals(42, a.chainHit)
        assertEquals(3.0, a.modifierFairFight!!, 0.001)
    }

    @Test
    fun `mapFactionArmory supports v2 inventory and v1 armory selections`() {
        val v2Raw = """
            {
              "inventory": [
                {
                  "id": 206,
                  "name": "Xanax",
                  "type": "Drug",
                  "quantity": 120,
                  "available": 115,
                  "loaned": 5,
                  "market_price": 840000
                }
              ]
            }
        """.trimIndent()
        val v2 = TornMappers.mapFactionArmory(v2Raw)
        assertNotNull(v2)
        assertEquals(1, v2!!.size)
        assertEquals(ArmoryCategory.DRUGS, v2.first().category)
        assertEquals(115, v2.first().available)
        assertEquals(5, v2.first().loaned)

        val v1Raw = """
            {
              "medical": [
                { "ID": 67, "name": "First Aid Kit", "quantity": 50 }
              ],
              "weapons": [
                { "ID": 230, "name": "ArmaLite M-15A4", "quantity": 3, "loaned": 1 }
              ]
            }
        """.trimIndent()
        val v1 = TornMappers.mapFactionArmory(v1Raw)
        assertNotNull(v1)
        assertEquals(2, v1!!.size)
        assertTrue(v1.any { it.category == ArmoryCategory.MEDICAL && it.name == "First Aid Kit" })
        assertTrue(v1.any { it.category == ArmoryCategory.WEAPONS && it.available == 2 && it.loaned == 1 })
    }

    @Test
    fun `mapFactionCrimes and mapUserOrganizedCrime parse slots, CPR, and null user crime`() {
        val factionCrimesRaw = """
            {
              "crimes": [
                {
                  "id": 7711,
                  "name": "Blast from the Past",
                  "difficulty": 7,
                  "status": "Planning",
                  "ready_at": 1700050000,
                  "slots": [
                    {
                      "position": "Hacker",
                      "position_id": "HCK",
                      "checkpoint_pass_rate": 91.5,
                      "user": { "id": 3962657, "name": "Kagaa", "progress": 64.0 },
                      "item_requirement": { "id": 100, "name": "Laptop", "is_available": true }
                    },
                    {
                      "position": "Muscle",
                      "checkpoint_pass_rate": 0.0,
                      "user": null
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val crimes = TornMappers.mapFactionCrimes(factionCrimesRaw)
        assertNotNull(crimes)
        assertEquals(1, crimes!!.size)
        val c = crimes.first()
        assertEquals("Blast from the Past", c.name)
        assertEquals(7, c.difficulty)
        assertEquals(1700050000L, c.readyAtSec)
        assertEquals(2, c.slots.size)
        assertEquals("Hacker", c.slots[0].position)
        assertEquals(3962657L, c.slots[0].userId)
        assertEquals(91.5, c.slots[0].checkpointPassRate!!, 0.01)
        assertEquals(true, c.slots[0].itemAvailable)
        assertNull(c.slots[1].userId)

        val emptySelfOcRaw = """{ "organizedCrime": null }"""
        val emptySelf = TornMappers.mapUserOrganizedCrime(emptySelfOcRaw)
        assertNotNull(emptySelf)
        assertNull(emptySelf?.crime)
    }

    @Test
    fun `mapFactionCrimes preserves null userName when API omits slot user name`() {
        val raw = """
            {
              "crimes": [
                {
                  "id": 9001,
                  "name": "Stacking the Deck",
                  "status": "Recruiting",
                  "slots": [
                    {
                      "position": "Driver",
                      "user": { "id": 123456, "progress": 100.0 }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val crimes = TornMappers.mapFactionCrimes(raw)
        assertNotNull(crimes)
        assertEquals(123456L, crimes!!.first().slots.first().userId)
        assertNull(crimes.first().slots.first().userName)
    }

    @Test
    fun `mapUserList parses v2 user list array for friends targets and enemies`() {
        val raw = """
            {
              "list": [
                {
                  "id": 222333,
                  "name": "AllyBird",
                  "level": 68,
                  "days_in_torn": 900,
                  "note": "Reliable trader",
                  "status": {
                    "state": "Okay",
                    "description": "Okay"
                  },
                  "last_action": {
                    "status": "Online",
                    "relative": "2 minutes ago",
                    "timestamp": 1700001000
                  }
                }
              ]
            }
        """.trimIndent()
        val entries = TornMappers.mapUserList(raw)
        assertNotNull(entries)
        assertEquals(1, entries!!.size)
        val item = entries.first()
        assertEquals(222333L, item.playerId)
        assertEquals("AllyBird", item.name)
        assertEquals(68, item.level)
        assertEquals("Reliable trader", item.note)
        assertEquals("Online", item.lastActionStatus)
    }
}
