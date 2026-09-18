package com.example.metrogo

/**
 * Single source of truth for available bus routes, used by both PurchaseTicket
 * (browse everything) and JourneyPlanner (filter by From/To).
 *
 * TODO: replace sampleRoutes with a real API/database call once one exists.
 * Keeping this in one place means every screen automatically stays in sync --
 * e.g. the station names JourneyPlanner lets you pick from are guaranteed to
 * actually match a route's origin/destination, instead of two hand-typed
 * lists silently drifting apart.
 */
object BusRouteRepository {

    val sampleRoutes: List<BusRoute> = listOf(
        BusRoute(1, "T9 Vea Raya", "Park Station", "Soshanguve", "08:15", "08:57", 32, "GP 814-263", 42),
        BusRoute(2, "Vea Raya", "Rosebank", "Midrand", "09:42", "10:24", 27, "GP 392-715", 42),
        BusRoute(3, "T2 Vea Raya", "Soweto", "Parktown", "10:18", "10:51", 24, "GP 641-908", 33),
        BusRoute(4, "T7 Vea Raya", "Centurion", "Sandton", "11:35", "12:48", 29, "GP 527-384", 73),
        BusRoute(5, "T3 Vea Raya", "Sandton", "Rosebank", "12:10", "12:37", 22, "GP 176-529", 27),
        BusRoute(6, "Vea Raya", "Rosebank", "Parktown", "13:27", "14:08", 26, "GP 903-417", 41),
        BusRoute(7, "T9 Vea Raya", "Park Station", "Soshanguve", "14:52", "15:36", 31, "GP 458-672", 44),
        BusRoute(8, "T2 Vea Raya", "Soweto", "Parktown", "15:41", "16:16", 23, "GP 735-194", 35),
        BusRoute(9, "T7 Vea Raya", "Centurion", "Sandton", "16:25", "17:39", 28, "GP 286-843", 74),
        BusRoute(10, "T3 Vea Raya", "Sandton", "Rosebank", "17:08", "17:34", 25, "GP 619-357", 26),
        BusRoute(11, "Vea Raya", "Rosebank", "Midrand", "18:14", "18:58", 30, "GP 842-516", 44),
        BusRoute(12, "T2 Vea Raya", "Soweto", "Parktown", "19:36", "20:09", 21, "GP 374-825", 33)
    )

    /** Every station that appears as an origin or destination, alphabetised, no duplicates. */
    val stationNames: List<String> by lazy {
        (sampleRoutes.map { it.origin } + sampleRoutes.map { it.destination })
            .distinct()
            .sorted()
    }

    fun routesBetween(origin: String, destination: String): List<BusRoute> =
        sampleRoutes.filter { it.origin == origin && it.destination == destination }

    fun routesExcluding(origin: String, destination: String): List<BusRoute> =
        sampleRoutes.filterNot { it.origin == origin && it.destination == destination }
}