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
            updateChart()
        }

    override val root: BorderPane by fxml("/TradeableChart.fxml")
    private val xAxis = CategoryAxis()
    private val yAxis = NumberAxis()
    private val barChart = BarChart(xAxis, yAxis)

    init {
        title = "Supply and Demand"

        barChart.title = "Supply and Demand"
        xAxis.label = "Tradeable"
        yAxis.label = "Quantity"
        root.center = barChart
        xAxis.tickLabelRotation = 45.0

        updateChart()

    }

    private fun updateChart() {
        census?.let {

            barChart.data.clear()

            val supplySeries = XYChart.Series<String, Number>()
            supplySeries.name = "Supply"

            val demandSeries = XYChart.Series<String, Number>()
            demandSeries.name = "Demand"

            it.resourceCounts.totals().forEach { economyReport ->

                if (economyReport.tradeable != Tradeable.MONEY) {
                    supplySeries.data.add(XYChart.Data(economyReport.tradeable.name, economyReport.supply))
                    demandSeries.data.add(XYChart.Data(economyReport.tradeable.name, economyReport.demand))
                }

            }
            barChart.data.addAll(supplySeries, demandSeries)
        }
    }

}