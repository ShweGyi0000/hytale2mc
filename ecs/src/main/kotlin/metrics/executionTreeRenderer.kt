package com.hytale2mc.ecs.metrics

import com.hytale2mc.ecs.phase.Phase
import kotlin.time.Duration

private object Ansi {
    const val RESET = "\u001B[0m"
    const val BOLD = "\u001B[1m"
    const val DIM = "\u001B[2m"
    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val ORANGE = "\u001B[38;5;208m"
    const val PURPLE = "\u001B[35m"
    const val CYAN = "\u001B[36m"
}

fun printExecutionTree(execution: ExecutionNode.Space, targetTickDuration: Duration) {
    val durationStr = formatDuration(execution.duration, targetTickDuration)
    println("${Ansi.BOLD}Tick Report${Ansi.RESET} $durationStr")

    execution.executions.forEachIndexed { index, node ->
        printRecursive(node, "", index == execution.executions.lastIndex, targetTickDuration)
    }
}

private fun printRecursive(
    node: ExecutionNode,
    prefix: String,
    isLast: Boolean,
    targetTick: Duration
) {
    val marker = if (isLast) "└── " else "├── "
    val childPrefix = prefix + if (isLast) "    " else "│   "

    when (node) {
        is ExecutionNode.Phase -> {
            println("$prefix$marker${Ansi.BOLD}🔄 Phase: ${getPhaseName(node.phase)}${Ansi.RESET} ${formatDuration(node.duration, targetTick)}")
            node.batches.forEachIndexed { index, batch ->
                printRecursive(batch, childPrefix, index == node.batches.lastIndex, targetTick)
            }
        }

        is ExecutionNode.CommandFlush -> {
            println("$prefix$marker${Ansi.YELLOW}⚡ Command Flush${Ansi.RESET} ${formatDuration(node.duration, targetTick)}")
            node.commands.forEachIndexed { index, command ->
                printRecursive(command, childPrefix, index == node.commands.lastIndex, targetTick)
            }
        }

        is ExecutionNode.SystemBatch -> {
            val calculatedDuration = calculateBatchDuration(node)
            val durationStr = formatDuration(calculatedDuration, targetTick)

            val shouldInline = node.systems.size == 1 && node.systems.first().id == node.id

            if (shouldInline) {
                val system = node.systems.first()
                val statusStr = formatSystemStatus(system.status, targetTick)
                println("$prefix$marker${Ansi.CYAN}⚙️ System: ${node.id}${Ansi.RESET} $statusStr $durationStr")
            } else {
                val batchStatusStr = when (node.status) {
                    is ExecutionNode.BatchStatus.ConditionFailed -> "${Ansi.RED}⛔ [Cond. Failed]${Ansi.RESET}"
                    ExecutionNode.BatchStatus.NotReached -> "${Ansi.DIM}⏭️ [Skip]${Ansi.RESET}"
                    else -> ""
                }
                println("$prefix$marker${Ansi.CYAN}📦 Batch: ${node.id}${Ansi.RESET} $batchStatusStr $durationStr")

                node.systems.forEachIndexed { index, system ->
                    printRecursive(system, childPrefix, index == node.systems.lastIndex, targetTick)
                }
            }
        }

        is ExecutionNode.System -> {
            val statusStr = formatSystemStatus(node.status, targetTick)
            println("$prefix$marker${Ansi.RESET}⚙️ ${node.id} $statusStr")
        }

        is ExecutionNode.Command -> {
            println("$prefix$marker${Ansi.RESET}📜 ${node.commandId} ${Ansi.RESET}${formatDuration(node.duration, targetTick)}")
        }

        else -> {}
    }
}

private fun calculateBatchDuration(batch: ExecutionNode.SystemBatch): Duration {
    return when (val status = batch.status) {
        is ExecutionNode.BatchStatus.ConditionFailed -> getDurationFromSystem(status.condition)
        else -> batch.systems.fold(Duration.ZERO) { acc, sys -> acc + getDurationFromSystem(sys) }
    }
}

private fun getDurationFromSystem(system: ExecutionNode.System): Duration {
    return when (val status = system.status) {
        is ExecutionNode.SystemStatus.Completed -> status.duration
        is ExecutionNode.SystemStatus.ConditionFailed -> getDurationFromSystem(status.condition)
        else -> Duration.ZERO
    }
}

private fun formatSystemStatus(status: ExecutionNode.SystemStatus, targetTick: Duration): String {
    return when (status) {
        is ExecutionNode.SystemStatus.Completed ->
            "${Ansi.DIM}[${status.thread}]${Ansi.RESET} ${formatDuration(status.duration, targetTick)}"
        is ExecutionNode.SystemStatus.ConditionFailed ->
            "${Ansi.RED}⛔ [Condition Failed]${Ansi.RESET}"
        ExecutionNode.SystemStatus.NotReached ->
            "${Ansi.DIM}⏭️ [Not Reached]${Ansi.RESET}"
    }
}

private fun formatDuration(duration: Duration, targetTick: Duration): String {
    if (duration == Duration.ZERO) return "${Ansi.DIM}0ms${Ansi.RESET}"

    val ms = duration.inWholeMicroseconds / 1000.0
    val text = ms.toString()

    val ratio = if (targetTick > Duration.ZERO) duration.inWholeNanoseconds.toDouble() / targetTick.inWholeNanoseconds.toDouble() else 0.0

    return when {
        ratio > 1.0 -> "${Ansi.PURPLE}$text${Ansi.RESET}"
        ratio > 0.75 -> "${Ansi.RED}$text${Ansi.RESET}"
        ratio > 0.5 -> "${Ansi.ORANGE}$text${Ansi.RESET}"
        ratio > 0.25 -> "${Ansi.YELLOW}$text${Ansi.RESET}"
        else -> "${Ansi.GREEN}$text${Ansi.RESET}"
    }
}

private fun getPhaseName(phase: Phase): String {
    return when (phase) {
        Phase.StartUp -> "start_up"
        Phase.StateTransition -> "state_transition"
        is Phase.StateTransitions.Transition<*, *, *> -> "transitioning: ${phase.from} -> ${phase.to}"
        is Phase.StateTransitions.Enter<*> -> "entering -> ${phase.state}"
        is Phase.StateTransitions.Exit<*> -> "exiting -> ${phase.state}"
        Phase.Before -> "before"
        Phase.Update -> "update"
        Phase.After -> "after"
    }
}