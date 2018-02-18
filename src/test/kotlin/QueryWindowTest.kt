import javafx.application.Application
import javafx.stage.Stage
import kotcity.data.BlockCoordinate
import kotcity.data.Building
import kotcity.data.BuildingType
import kotcity.data.CityMap
import kotcity.data.assets.AssetManager
import kotcity.ui.KotcityStyles
import kotcity.ui.QueryWindow
import tornadofx.App
import tornadofx.find

fun setBuilding(): Pair<CityMap, BlockCoordinate> {
    val map = CityMap.flatMap(512, 512)
    val assetManager = AssetManager(map)
    // let's plop a small house down...
    val slum = assetManager.buildingFor(BuildingType.RESIDENTIAL, "slum1")
    map.build(slum, BlockCoordinate(3, 5))
    return Pair(map, BlockCoordinate(3, 5))
}

class QueryWindowApp : App(QueryWindow::class, KotcityStyles::class) {
    override fun start(stage: Stage) {
        val queryWindow = find(QueryWindow::class)
        val buildingAndCity = setBuilding()
        queryWindow.mapAndCoordinate = buildingAndCity
        super.start(stage)
    }
}

fun main(args: Array<String>) {
    Application.launch(QueryWindowApp::class.java, *args)
}