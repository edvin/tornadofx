package tornadofx

/**
 *
 * @author Anindya Chatterjee
 */
internal object ShutdownExecutor {
    // a task executor to run some routines before
    // graceful shutdown of JVM

    private var taskList = ArrayList<() -> Unit>()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            // create the shutdown hook
            runBeforeShutdown()
        })
    }

    fun registerTask(task: () -> Unit) {
        // add the task to a list
        taskList.add(task)
    }

    private fun runBeforeShutdown() {
        // run the task from list before shutdown
        taskList.forEach { it.invoke() }
    }
}

fun beforeShutdown(task: () -> Unit) {
    ShutdownExecutor.registerTask(task)
}