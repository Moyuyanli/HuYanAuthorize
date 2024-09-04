package cn.chahuyun.authorize.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import java.lang.management.ManagementFactory
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

object ContinuationUtil {

    /**
     * 创建一个新协程
     */
    fun getContinuation(): Continuation<Unit> {
        return object : Continuation<Unit> {
            private val job = Job()
            override val context: CoroutineContext
                get() = Dispatchers.Default + job

            override fun resumeWith(result: Result<Unit>) {
                if (result.isFailure) {
                    result.exceptionOrNull()?.printStackTrace()
                } else {
                    println("Continuation resumed.")
                }
                job.complete()
            }
        }
    }

    suspend fun closeContinuation(continuation: Continuation<Unit>) {
        continuation.context[Job]?.join()
    }

}


object EventUtil {

    /**
     * 获取at的用户
     *
     * @return 第一个at的用户，可能为null
     */
    fun getAtMember(event: GroupMessageEvent): NormalMember? {
        val message = event.message
        for (singleMessage in message) {
            if (singleMessage is At) {
                return event.group[singleMessage.target]
            }
        }

        val contentToString = message.contentToString()

        val find = Regex("@(\\d+)").find(contentToString)
        find?.let {
            return event.group[it.groupValues[1].toLong()]
        }
        return null
    }


}

fun getSystemInfo(): String {
    val osBean = ManagementFactory.getOperatingSystemMXBean()
    val memoryBean = ManagementFactory.getMemoryMXBean()
    val runtimeBean = ManagementFactory.getRuntimeMXBean()

    // 获取操作系统名称
    val osName = osBean.name

    // 获取系统的总内存
    val heapMemoryUsage = memoryBean.heapMemoryUsage
    val totalMemory = heapMemoryUsage.max
    val usedMemory = heapMemoryUsage.used
    val freeMemory = heapMemoryUsage.max - heapMemoryUsage.used
    val memoryUsagePercentage = (usedMemory.toDouble() / totalMemory.toDouble()) * 100

    // 获取当前进程的启动时间
    val startTime = runtimeBean.startTime
    val upTime = Duration.between(Instant.ofEpochMilli(startTime), Instant.now()).toMinutes()

    // CPU 使用率
    var cpuUsage = 0.0
    if (osBean.javaClass.name == "com.sun.management.OperatingSystemMXBean") {
        // 如果是 Sun/Oracle JDK，我们可以使用扩展的 CPU 使用率方法
        val sunOsBean = osBean as com.sun.management.OperatingSystemMXBean
        cpuUsage = sunOsBean.systemCpuLoad * 100
    }

    // 格式化内存使用率
    val formattedMemoryUsagePercentage = "%.2f".format(memoryUsagePercentage)
    val formattedCpuUsage = if (cpuUsage > 0.0) "${cpuUsage}%" else "N/A"

    return """
        操作系统: $osName
        运行时间: $upTime 分钟
        CPU 使用率: $formattedCpuUsage
        内存:
          - 总大小: ${totalMemory / (1024 * 1024)} MB
          - 已使用: ${usedMemory / (1024 * 1024)} MB
          - 未使用: ${freeMemory / (1024 * 1024)} MB
          - 使用率: ${formattedMemoryUsagePercentage}%
    """.trimIndent()
}



