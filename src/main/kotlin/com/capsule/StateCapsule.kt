package com.capsule

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface StateCapsule<State, Action> {
    val state: Flow<State>

    suspend fun run(action: Action)
}

typealias SideEffect<State, Action> = State.(Action) -> Unit

typealias Transition<State, Action> = State.(Action) -> Effect<State, Action>

sealed interface Effect<State, Action> {
    data class Pure<State, Action>(
        val state: State
    ) : Effect<State, Action>

    data class Impure<State, Action>(
        val state: State,
        val sideEffects: List<SideEffect<State, Action>>,
    ) : Effect<State, Action>
}

internal class StateCapsuleImpl<State, Action>(
    initialState: State,
    private val transitions: Map<Action, Transition<State, Action>>
) : StateCapsule<State, Action> {

    private val _state: MutableStateFlow<State> = MutableStateFlow(initialState)

    override val state: Flow<State>
        get() = _state

    override suspend fun run(action: Action) {
        transitions[action]?.run(_state.value, action)
            ?: throw Exception("No Transition found for Action $action in ${_state.value}")
    }

    private suspend fun Transition<State, Action>.run(state: State, action: Action) {
        when (val effect = invoke(state, action)) {
            is Effect.Impure -> {
                _state.value = effect.state
                supervisorScope {
                    effect.sideEffects.map { sideEffect ->
                        async { sideEffect.invoke(state, action) }
                    }.awaitAll() // TODO this will cancel ALL side effects when one fails. This doesn't seem right?
                }
            }

            is Effect.Pure -> {
                _state.value = effect.state
            }
        }
    }
}
