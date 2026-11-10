package com.example.myautomationapp.task

import com.example.myautomationapp.planner.EditingPlan
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class TaskManager {
    enum class State { PENDING, VALIDATING, RUNNING, WAITING_FOR_USER, VERIFYING, COMPLETED, FAILED, CANCELLED }
    data class Task(
        val id: String = UUID.randomUUID().toString(),
        val plan: EditingPlan,
        var state: State = State.PENDING,
        var message: String = "Waiting to start",
        var error: String? = null
    )

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val listeners = CopyOnWriteArrayList<(Task) -> Unit>()
    private var active: Future<*>? = null
    @Volatile private var current: Task? = null

    fun addListener(listener: (Task) -> Unit) { listeners += listener }
    fun currentTask(): Task? = current

    fun submit(plan: EditingPlan, execute: (Task) -> Unit): Task {
        cancel()
        val task = Task(plan = plan)
        current = task
        publish(task)
        active = executor.submit {
            update(task, State.VALIDATING, "Validating plan")
            if (plan.requiresConfirmation) {
                update(task, State.WAITING_FOR_USER, "Waiting for approval")
                return@submit
            }
            execute(task)
        }
        return task
    }

    fun approveAndRun(execute: (Task) -> Unit) {
        val task = current ?: return
        if (task.state != State.WAITING_FOR_USER) return
        active = executor.submit { execute(task) }
    }

    fun update(task: Task, state: State, message: String, error: String? = null) {
        task.state = state
        task.message = message
        task.error = error
        publish(task)
    }

    fun cancel() {
        active?.cancel(true)
        current?.let { update(it, State.CANCELLED, "Task cancelled") }
        active = null
    }

    private fun publish(task: Task) = listeners.forEach { it(task.copy()) }
}
