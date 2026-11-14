package app.crowdhub.provider.torn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TornUserStocksMapperTest {

    @Test
    fun `parses Torn v1 object-map user stocks with numeric dividend ready and map transactions`() {
        val raw = """
            {
              "stocks": {
                "1": {
                  "stock_id": 1,
                  "total_shares": 3000000,
                  "dividend": {
                    "ready": 1,
                    "increment": 1,
                    "progress": 31,
                    "frequency": 31
                  },
                  "transactions": {
                    "101": {
                      "shares": 1000000,
                      "bought_price": 1000.0,
                      "time_bought": 1700000000
                    },
                    "102": {
                      "shares": 2000000,
                      "bought_price": 1150.0,
                      "time_bought": 1700500000
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val parsed = TornMappers.mapUserStocks(raw)
        assertNotNull(parsed)
        assertEquals(1, parsed!!.size)
        val tsb = parsed.first()
        assertEquals(1, tsb.stockId)
        assertEquals(3_000_000L, tsb.totalShares)
        assertTrue(tsb.dividend!!.ready)
        assertEquals(1, tsb.dividend!!.increment)
        assertEquals(31, tsb.dividend!!.progress)
        assertEquals(31, tsb.dividend!!.frequency)
        assertEquals(2, tsb.transactions.size)
        assertEquals(102L, tsb.transactions.first().id)
        assertEquals(2_000_000L, tsb.transactions.first().shares)
        assertEquals(1150.0, tsb.transactions.first().boughtPrice, 0.001)
    }

    @Test
    fun `parses Torn v2 array user stocks with boolean dividend ready and array transactions`() {
        val raw = """
            {
              "stocks": [
                {
                  "id": 16,
                  "shares": 1500000,
                  "bonus": {
                    "ready": false,
                    "increment": 2,
                    "progress": 4,
                    "frequency": 7
                  },
                  "transactions": [
                    {
                      "id": 5001,
                      "shares": 500000,
                      "price": 700.0,
                      "timestamp": 1710000000
                    },
                    {
                      "id": 5002,
                      "shares": 1000000,
                      "price": 725.0,
                      "timestamp": 1710500000
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = TornMappers.mapUserStocks(raw)
        assertNotNull(parsed)
        assertEquals(1, parsed!!.size)
        val sym = parsed.first()
        assertEquals(16, sym.stockId)
        assertEquals(1_500_000L, sym.totalShares)
        assertFalse(sym.dividend!!.ready)
        assertEquals(2, sym.dividend!!.increment)
        assertEquals(4, sym.dividend!!.progress)
        assertEquals(7, sym.dividend!!.frequency)
        assertEquals(2, sym.transactions.size)
        assertEquals(5002L, sym.transactions.first().id)
    }

    @Test
    fun `empty stocks array or object returns empty list and malformed json returns null`() {
        assertEquals(emptyList<TornUserStock>(), TornMappers.mapUserStocks("""{"stocks": []}"""))
        assertEquals(emptyList<TornUserStock>(), TornMappers.mapUserStocks("""{"stocks": {}}"""))
        assertNull(TornMappers.mapUserStocks("""{"error": {"code": 2}}"""))
    }
}
