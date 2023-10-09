package com.capsule

fun <State, Action> capsule(
    block: StateCapsuleBuilder<State, Action>.() -> Unit
): StateCapsule<State, Action> {
    return StateCapsuleBuilder<State, Action>().apply { block() }.build()
}

@DslMarker
annotation class StateCapsuleMarker

@StateCapsuleMarker
class StateCapsuleBuilder<State, Action>{
    private var initState: State? = null
    private var transitions: Map<Action, Transition<State, Action>> = emptyMap()

    fun initial(block: () -> State) {
        initState = block()
    }

    fun transitions(block: TransitionBuilder<State, Action>.() -> Unit) {
        transitions = TransitionBuilder<State, Action>().apply { block() }.transitions
    }

    fun build(): StateCapsule<State, Action> =
        StateCapsuleImpl(
            initialState = initState ?: throw IllegalStateException("Missing Initial State"),
            transitions = transitions
        )
}

@StateCapsuleMarker
class TransitionBuilder<State, Action> {
    internal var transitions: MutableMap<Action, Transition<State, Action>> = mutableMapOf()

    infix fun Action.runs(transition: Transition<State, Action>) {
        transitions[this] = transition
    }
}


operator fun <State, Action> State.invoke() = Effect.Pure<State, Action>(this)

fun <State, Action> State.only() = Effect.Pure<State, Action>(this)

operator fun <State, Action> Effect.Pure<State, Action>.plus(sideEffect: SideEffect<State, Action>) =
    Effect.Impure(this.state, listOf(sideEffect))

infix fun <State, Action> Effect.Pure<State, Action>.also(sideEffect: SideEffect<State, Action>) =
    Effect.Impure(this.state, listOf(sideEffect))

operator fun <State, Action> State.plus(sideEffects: List<SideEffect<State, Action>>) =
    Effect.Impure(this, sideEffects)

infix fun <State, Action> State.also(sideEffects: List<SideEffect<State, Action>>) =
    Effect.Impure(this, sideEffects)

operator fun <State, Action> State.plus(sideEffect: SideEffect<State, Action>) =
    Effect.Impure(this, listOf(sideEffect))

infix fun <State, Action> State.also(sideEffect: SideEffect<State, Action>) =
    Effect.Impure(this, listOf(sideEffect))

operator fun <State, Action> Effect.Impure<State, Action>.plus(sideEffect: SideEffect<State, Action>) =
    this.copy(sideEffects = this.sideEffects + sideEffect)

infix fun <State, Action> Effect.Impure<State, Action>.and(sideEffect: SideEffect<State, Action>) =
    this.copy(sideEffects = this.sideEffects + sideEffect)