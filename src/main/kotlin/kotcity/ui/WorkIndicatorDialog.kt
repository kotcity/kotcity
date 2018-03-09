package kotcity.ui

import javafx.collections.ListChangeListener
import java.util.function.Consumer
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.concurrent.WorkerStateEvent

import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import tornadofx.runLater


import java.util.function.ToIntFunction

/**
 * Public domain. Use as you like. No warranties.
 * P = Input parameter type. Given to the closure as parameter. Return type is always Integer.
 * (cc) @imifos
 */
class WorkIndicatorDialog<in P>
/**
 *
 */
(owner: Window, label: String) {

    private var animationWorker: Task<*>? = null
    private var taskWorker: Task<Int>? = null

    private val progressIndicator = ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS)
    private val dialog = Stage(StageStyle.UNDECORATED)
    private val label = Label()
    private val root = Group()
    private val scene = Scene(root, 330.0, 120.0, Color.WHITE)
    private val mainPane = BorderPane()
    private val vbox = VBox()

    /** Placing a listener on this list allows to get notified BY the result when the task has finished.  */
    var resultNotificationList = FXCollections.observableArrayList<Int>()

    /**
     * For those that like beans :)
     */
    var resultValue: Int? = null

    init {
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(owner)
        dialog.setResizable(false)
        this.label.setText(label)
    }

    /**
     *
     */
    fun addTaskEndNotification(c: Consumer<Int?>) {
        resultNotificationList.addListener({
            resultNotificationList.clear()
            c.accept(resultValue)
        } as ListChangeListener<in Int>)
    }

    /**
     *
     */
    fun exec(parameter: P, func: ToIntFunction<*>) {
        setupDialog()
        setupAnimationThread()
        setupWorkerThread(parameter, func as ToIntFunction<P>)
    }

    /**
     *
     */
    private fun setupDialog() {
        root.getChildren().add(mainPane)
        vbox.setSpacing(5.0)
        vbox.setAlignment(Pos.CENTER)
        vbox.setMinSize(330.0, 120.0)
        vbox.getChildren().addAll(label, progressIndicator)
        mainPane.setTop(vbox)
        dialog.setScene(scene)

        dialog.setOnHiding { /* Gets notified when task ended, but BEFORE
             result value is attributed. Using the observable list above is
             recommended. */ _ ->
        }

        dialog.show()
    }

    /**
     *
     */
    private fun setupAnimationThread() {

        progressIndicator.progress = ProgressBar.INDETERMINATE_PROGRESS
        progressIndicator.progressProperty().unbind()

        animationWorker = object : Task<Double>() {
            override fun call(): Double {
                return ProgressBar.INDETERMINATE_PROGRESS
            }

        }

        animationWorker?.let {
            progressIndicator.progressProperty().bind(it.progressProperty())

//            it.messageProperty().addListener({ observable, oldValue, newValue ->
//                // Do something when the animation value ticker has changed
//            })

            Thread(animationWorker).start()
        }


    }

    /**
     *
     */
    private fun setupWorkerThread(parameter: P, func: ToIntFunction<P>) {

        taskWorker = object : Task<Int>() {
            public override fun call(): Int? {
                return func.applyAsInt(parameter)
            }
        }

        val eh = { _: WorkerStateEvent ->
            animationWorker!!.cancel(true)
            progressIndicator.progressProperty().unbind()

            dialog.close()
            try {
                resultValue = taskWorker!!.get()
                resultNotificationList.add(resultValue)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

        }

        taskWorker!!.setOnSucceeded { e -> eh(e) }
        taskWorker!!.setOnFailed { e -> eh(e) }

        Thread(taskWorker).start()
    }

}