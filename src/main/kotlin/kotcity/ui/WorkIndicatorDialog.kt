package kotcity.ui


import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
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
import java.util.function.Consumer
import java.util.function.ToIntFunction

/**
 * Used to show a spinner when we are doing some work...
 * P = Input parameter type. Given to the closure as parameter. Return type is always Integer.
 * (cc) @imifos
 */
class WorkIndicatorDialog<P>(owner: Window, label: String) {

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
    var resultNotificationList: ObservableList<Int> = FXCollections.observableArrayList<Int>()

    /**
     * For those that like beans :)
     */
    var resultValue: Int? = null

    init {
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(owner)
        dialog.isResizable = false
        this.label.text = label
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
     * Executes the work indicator dialog. If an exception occurs during execution, it will be thrown.
     * @param parameter An input parameter for the payload function
     * @param func The payload function which should be executed
     */
    fun exec(parameter: P, func: ToIntFunction<P>) {
        exec(parameter, func, Consumer {throw it} )
    }

    /**
     * Executes the work indicator dialog. If an exception occurs during execution, it will not be thrown but passed to
     * the error handling function
     * @param parameter An input parameter for the payload function
     * @param func The payload function which should be executed
     * @param errFunc The error handling function, which will be called in case an exception occurs
     */
    fun exec(parameter: P, func: ToIntFunction<P>, errFunc: Consumer<Exception>) {
        setupDialog()
        setupAnimationThread()
        setupWorkerThread(parameter, func, errFunc)
    }

    /**
     *
     */
    private fun setupDialog() {
        root.children.add(mainPane)
        vbox.spacing = 5.0
        vbox.alignment = Pos.CENTER
        vbox.minWidth = 330.0
        vbox.minHeight = 120.0
        vbox.children.addAll(label, progressIndicator)
        mainPane.top = vbox
        dialog.scene = scene

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

            it.messageProperty().addListener({ observable, oldValue, newValue ->
                // Do something when the animation value ticker has changed
            })

            Thread(animationWorker).start()
        }


    }

    /**
     *
     */
    private fun setupWorkerThread(parameter: P, func: ToIntFunction<P>, errFunc: Consumer<Exception>) {

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
                errFunc.accept(e)
            }

        }

        taskWorker!!.setOnSucceeded { e -> eh(e) }
        taskWorker!!.setOnFailed { e -> eh(e) }

        Thread(taskWorker).start()
    }

}