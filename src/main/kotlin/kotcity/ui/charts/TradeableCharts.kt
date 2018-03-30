package kotcity.ui.charts

import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.layout.BorderPane
import kotcity.automata.CensusTaker
import kotcity.data.Tradeable
import tornadofx.View

class SupplyDemandChart : View() {

    var census: CensusTaker? = null
        set(value) {
            field = value
            value?.addUpdateListener { updateChart() }
            updateChart()
        }

    override val root: BorderPane by fxml("/TradeableChart.fxml")
    private val xAxis = CategoryAxis()
    private val yAxis = NumberAxis()
    private val barChart = BarChart(xAxis, yAxis)
    private val supplySeries = XYChart.Series<String, Number>()
    private val demandSeries = XYChart.Series<String, Number>()

    init {
        title = "Supply and Demand"

        barChart.title = "Supply and Demand"
        barChart.animated = false
        xAxis.label = "Tradeable"
        yAxis.label = "Quantity"
        root.center = barChart
        xAxis.tickLabelRotation = 45.0

        supplySeries.name = "Supply"
        demandSeries.name = "Demand"

        barChart.data.addAll(demandSeries, supplySeries)

        updateChart()
    }

    private fun updateChart() {
        census?.let {
            supplySeries.data.clear()
            demandSeries.data.clear()

            it.resourceCounts.totals().forEach { economyReport ->
                if (economyReport.tradeable != Tradeable.MONEY && economyReport.tradeable != Tradeable.RAW_MATERIALS) {
                    supplySeries.data.add(XYChart.Data(economyReport.tradeable.name, economyReport.supply))
                    demandSeries.data.add(XYChart.Data(economyReport.tradeable.name, economyReport.demand))
                }
            }
        }
    }
}
