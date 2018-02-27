package kotcity.ui.charts

import javafx.application.Application
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.chart.XYChart.Series
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import tornadofx.App
import tornadofx.View

class BarChartSample : View() {

    override val root: BorderPane by fxml("/QueryWindow.fxml")

    init {
        val stage = primaryStage
        stage.title = "Bar Chart Sample"
        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()
        val bc = BarChart(xAxis, yAxis)
        bc.title = "Country Summary"
        xAxis.label = "Country"
        yAxis.label = "Value"

        val series1 = Series<String, Number>()
        series1.setName("2003")
        series1.getData().add(XYChart.Data(austria, 25601.34))
        series1.getData().add(XYChart.Data(brazil, 20148.82))
        series1.getData().add(XYChart.Data(france, 10000))
        series1.getData().add(XYChart.Data(italy, 35407.15))
        series1.getData().add(XYChart.Data(usa, 12000))

        val series2 = Series<String, Number>()
        series2.setName("2004")
        series2.getData().add(XYChart.Data(austria, 57401.85))
        series2.getData().add(XYChart.Data(brazil, 41941.19))
        series2.getData().add(XYChart.Data(france, 45263.37))
        series2.getData().add(XYChart.Data(italy, 117320.16))
        series2.getData().add(XYChart.Data(usa, 14845.27))

        val series3 = Series<String, Number>()
        series3.setName("2005")
        series3.getData().add(XYChart.Data(austria, 45000.65))
        series3.getData().add(XYChart.Data(brazil, 44835.76))
        series3.getData().add(XYChart.Data(france, 18722.18))
        series3.getData().add(XYChart.Data(italy, 17557.31))
        series3.getData().add(XYChart.Data(usa, 92633.68))

        val scene = Scene(bc, 800.0, 600.0)
        bc.data.addAll(series1, series2, series3)
        stage.scene = scene
        stage.show()
    }

    companion object {
        internal val austria = "Austria"
        internal val brazil = "Brazil"
        internal val france = "France"
        internal val italy = "Italy"
        internal val usa = "USA"
    }
}