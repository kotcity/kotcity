package kotcity.util

import com.natpryce.konfig.PropertyGroup
import com.natpryce.konfig.getValue
import com.natpryce.konfig.stringType

object Game : PropertyGroup() {
    val Name by stringType
    val Version by stringType
}
