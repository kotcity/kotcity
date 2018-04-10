package kotcity.jmx

import javax.management.*
import java.lang.management.*

interface KotCityMBean {
    var contractsSigned: Int
    var pendingContracts: Int
}

object KotCity : KotCityMBean {
    override var contractsSigned: Int = 0
    override var pendingContracts: Int = 0
}

class SimpleAgent {

    // Get the platform MBeanServer
    private val mBeanServer: MBeanServer = ManagementFactory.getPlatformMBeanServer()

    init {
        // Uniquely identify the MBeans and register them with the platform MBeanServer
        val beanName = ObjectName("kotcity:name=KotCityBean")
        mBeanServer.registerMBean(KotCity, beanName)
    }
}
