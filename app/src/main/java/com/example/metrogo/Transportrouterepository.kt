package com.example.metrogo


object TransportRouteRepository {

    val busStops: List<BusStop> = listOf(
        BusStop("stop-park-station", "Park Station", -26.2023, 28.0436),
        BusStop("stop-soshanguve", "Soshanguve", -25.5209, 28.1041),
        BusStop("stop-rosebank", "Rosebank", -26.1467, 28.0436),
        BusStop("stop-midrand", "Midrand", -25.9992, 28.1123),
        BusStop("stop-soweto", "Soweto", -26.2678, 27.8585),
        BusStop("stop-parktown", "Parktown", -26.1858, 28.0436),
        BusStop("stop-centurion", "Centurion", -25.8603, 28.1894),
        BusStop("stop-sandton", "Sandton", -26.1076, 28.0567)
    )

    val transportRoutes: List<TransportRoute> = listOf(
        TransportRoute("route-1", "T9 Vea Raya", "stop-park-station", "stop-soshanguve"),
        TransportRoute("route-2", "Vea Raya", "stop-rosebank", "stop-midrand"),
        TransportRoute("route-3", "T2 Vea Raya", "stop-soweto", "stop-parktown"),
        TransportRoute("route-4", "T7 Vea Raya", "stop-centurion", "stop-sandton"),
        TransportRoute("route-5", "T3 Vea Raya", "stop-sandton", "stop-rosebank"),
        TransportRoute("route-6", "Vea Raya", "stop-rosebank", "stop-parktown")
    )

    val schedules: List<Schedule> = listOf(
        Schedule("sched-1", "route-1", "08:15", "08:57", 32, "GP 814-263", 42),
        Schedule("sched-2", "route-1", "14:52", "15:36", 31, "GP 458-672", 44),
        Schedule("sched-3", "route-2", "09:42", "10:24", 27, "GP 392-715", 42),
        Schedule("sched-4", "route-2", "18:14", "18:58", 30, "GP 842-516", 44),
        Schedule("sched-5", "route-3", "10:18", "10:51", 24, "GP 641-908", 33),
        Schedule("sched-6", "route-3", "15:41", "16:16", 23, "GP 735-194", 35),
        Schedule("sched-7", "route-3", "19:36", "20:09", 21, "GP 374-825", 33),
        Schedule("sched-8", "route-4", "11:35", "12:48", 29, "GP 527-384", 73),
        Schedule("sched-9", "route-4", "16:25", "17:39", 28, "GP 286-843", 74),
        Schedule("sched-10", "route-5", "12:10", "12:37", 22, "GP 176-529", 27),
        Schedule("sched-11", "route-5", "17:08", "17:34", 25, "GP 619-357", 26),
        Schedule("sched-12", "route-6", "13:27", "14:08", 26, "GP 903-417", 41)
    )

      data class ScheduleDetails(
        val schedule: Schedule,
        val route: TransportRoute,
        val originStop: BusStop,
        val destinationStop: BusStop
    )

    fun getStop(stopId: String): BusStop? = busStops.find { it.stopId == stopId }
    fun getRoute(routeId: String): TransportRoute? = transportRoutes.find { it.routeId == routeId }
    fun getSchedule(scheduleId: String): Schedule? = schedules.find { it.scheduleId == scheduleId }

    fun stopIdByName(stopName: String): String? =
        busStops.find { it.stopName.equals(stopName, ignoreCase = true) }?.stopId

     val stationNames: List<String> by lazy { busStops.map { it.stopName }.sorted() }

    fun scheduleDetails(scheduleId: String): ScheduleDetails? {
        val schedule = getSchedule(scheduleId) ?: return null
        val route = getRoute(schedule.routeId) ?: return null
        val origin = getStop(route.originStopId) ?: return null
        val destination = getStop(route.destinationStopId) ?: return null
        return ScheduleDetails(schedule, route, origin, destination)
    }

    fun allScheduleDetails(): List<ScheduleDetails> = schedules.mapNotNull { scheduleDetails(it.scheduleId) }

    fun scheduleDetailsBetween(originStopName: String, destinationStopName: String): List<ScheduleDetails> =
        allScheduleDetails().filter {
            it.originStop.stopName.equals(originStopName, ignoreCase = true) &&
                    it.destinationStop.stopName.equals(destinationStopName, ignoreCase = true)
        }

    fun scheduleDetailsExcluding(originStopName: String, destinationStopName: String): List<ScheduleDetails> =
        allScheduleDetails().filterNot {
            it.originStop.stopName.equals(originStopName, ignoreCase = true) &&
                    it.destinationStop.stopName.equals(destinationStopName, ignoreCase = true)
        }
}