import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class StateCapsuleTest {

    /*
    Rules:
    - Inserting a Coin Unlocks the CandyMachine and increases the coins by 1
    - Knob can be turned when at least 1 candy there and unlocked
    - inserting a coin in unlocked state does nothing
    - turning the knob in locked state does nothing
    - turning the knob in unlocked state releases 1 candy and locks again
     */

    data class CandyMachineState(
        val locked: Boolean,
        val candies: Int,
        val coins: Int,
    )

    sealed interface Action {
        object InsertCoin : Action
        object TurnKnob : Action
        object RefillCandies : Action
    }

    private val initialState = CandyMachineState(
        locked = true,
        candies = 10,
        coins = 0,
    )

    private val capsule = StateCapsuleImpl(
        initialState = initialState,
        transitions = mapOf(
            Action.InsertCoin to {
                if (locked && candies > 0) {
                    copy(locked = false, coins = coins + 1)()
                } else {
                    only()
                }
            },
            Action.TurnKnob to {
                if (!locked && candies > 0) {
                    copy(locked = true, candies = candies - 1) also
                            { _: Action -> println("You can now collect your candy.") } and
                            { println("") }
                } else {
                    only()
                }
            }
        )
    )

    @Test
    fun `statemachine contains initial state after creation`() = runTest {
        capsule.state.test {
            assert(awaitItem() == initialState)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state is unlocked after inserting the coin`() = runTest {
        capsule.state.test {
            awaitItem()

            capsule.run(Action.InsertCoin)

            val state = awaitItem()
            assert(!state.locked)
            assert(state.coins == initialState.coins + 1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state is locked after inserting the coin and turning the knob`() = runTest {
        capsule.state.test {
            awaitItem()

            capsule.run(Action.InsertCoin)
            awaitItem()

            capsule.run(Action.TurnKnob)

            val state = awaitItem()
            assert(state.locked)
            assert(state.candies == initialState.candies - 1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state does not change after turning knob in locked state`() = runTest {
        capsule.state.test {
            awaitItem()
            capsule.run(Action.TurnKnob)

            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state does not change after inserting coin in unlocked state`() = runTest {
        capsule.state.test {
            awaitItem()
            capsule.run(Action.InsertCoin)
            awaitItem()
            capsule.run(Action.InsertCoin)

            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `statemachine throws exception on unknown action`() = runTest {
        try {
            capsule.state.test {
                awaitItem()
                capsule.run(Action.RefillCandies)
                awaitItem()
                assert(false)
            }
        } catch (e: Exception) {
            assert(true)
        }
    }
}