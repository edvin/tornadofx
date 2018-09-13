package tornadofx

/**
 * A task executor to run some routines before graceful shutdown of JVM
 * @author Anindya Chatterjee
 */
internal object ShutdownExecutor {
    private val taskList = mutableListOf<() -> Unit>()

    init {
        // Create the shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread { runBeforeShutdown() })
    }

    /**
     * Adds the [task] to the [taskList]
     * @return `true` if the [task] was added successfully, `false` otherwise
     */
    fun registerTask(task: () -> Unit): Boolean = taskList.add(task)

    /**
     * Run each task from the [taskList] before shutdown
     */
    private fun runBeforeShutdown() = taskList.forEach { it() }
}

// TODO Test throw exception
fun beforeShutdown(task: () -> Unit) {
    ShutdownExecutor.registerTask(task)
}
