package com.hytale2mc.ecs.system

import com.hytale2mc.ecs.query.ExecutionContext
import com.hytale2mc.ecs.query.Query

fun <R> exec(
    exec: ExecutionContext.(Query) -> R
): Executable<R> {
    val query = object : Query() {}
    return Executable(
        listOf(query),
    ) {
        exec.invoke(this, query)
    }
}

fun <R, Q : Query> exec(
    query: Q,
    exec: ExecutionContext.(Q) -> R
): Executable<R> {
    return Executable(
        listOf(query),
    ) {
        exec.invoke(this, query)
    }
}

fun <R, Q1 : Query, Q2 : Query> exec(
    query1: Q1,
    query2: Q2,
    exec: ExecutionContext.(Q1, Q2) -> R
): Executable<R> {
    return Executable(
        listOf(query1, query2),
    ) {
        exec.invoke(this, query1, query2)
    }
}

fun <R, Q1 : Query, Q2 : Query, Q3 : Query> exec(
    query1: Q1,
    query2: Q2,
    query3: Q3,
    exec: ExecutionContext.(Q1, Q2, Q3) -> R
): Executable<R> {
    return Executable(
        listOf(query1, query2, query3),
    ) {
        exec.invoke(this, query1, query2, query3)
    }
}

fun <R, Q1 : Query, Q2 : Query, Q3 : Query, Q4 : Query> exec(
    query1: Q1,
    query2: Q2,
    query3: Q3,
    query4: Q4,
    exec: ExecutionContext.(Q1, Q2, Q3, Q4) -> R
): Executable<R> {
    return Executable(
        listOf(query1, query2, query3, query4),
    ) {
        exec.invoke(this, query1, query2, query3, query4)
    }
}