package wskim.aos.appMonitoring

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build


object AppFinder {

    private fun isForeGroundEvent(event: UsageEvents.Event?): Boolean {
        if (event == null) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
        }
    }

    fun getPackageName(context: Context): String {

        // UsageStatsManager 선언
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        var lastRunAppTimeStamp = 0L

        // 얼마만큼의 시간동안 수집한 앱의 이름을 가져오는지 정하기 (begin ~ end 까지의 앱 이름을 수집한다)
        val interval: Long = 10000
        val end = System.currentTimeMillis()
        // 1 minute ago
        val begin = end - interval

        //
        val packageNameMap = HashMap<Long, String>()

        // 수집한 이벤트들을 담기 위한 UsageEvents
         val usageEvents = usageStatsManager.queryEvents(begin, end)

        // 이벤트가 여러개 있을 경우 (최소 존재는 해야 hasNextEvent가 null이 아니니까)
        while (usageEvents.hasNextEvent()) {

            // 현재 이벤트를 가져오기
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)

            // 현재 이벤트가 포그라운드 상태라면 = 현재 화면에 보이는 앱이라면
            if (isForeGroundEvent(event)) {
                // 해당 앱 이름을 packageNameMap에 넣는다.
                packageNameMap[event.timeStamp] = event.packageName
                // 가장 최근에 실행 된 이벤트에 대한 타임스탬프를 업데이트 해준다.
                if (event.timeStamp > lastRunAppTimeStamp) {
                    lastRunAppTimeStamp = event.timeStamp
                }
            }
        }

        // 가장 마지막까지 있는 앱의 이름을 리턴해준다.
        return packageNameMap[lastRunAppTimeStamp]?:""
    }
}