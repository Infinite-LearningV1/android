package com.example.infinite_track.presentation.screen.attendance.permission

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeReconciliationRequestDeliveryTest {

    @Test
    fun `cancelling a blocked consumer leaves its token pending without acknowledgement`() = runTest {
        val pending = MutableStateFlow<RuntimeReconciliationRequest?>(RuntimeReconciliationRequest(1))
        val coordinatorStarted = CompletableDeferred<Unit>()
        val coordinatorRelease = CompletableDeferred<Unit>()
        val acknowledged = mutableListOf<Long>()
        val consumer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            deliverRuntimeReconciliationRequests(
                requests = pending,
                reconcile = {
                    coordinatorStarted.complete(Unit)
                    coordinatorRelease.await()
                },
                acknowledge = acknowledged::add
            )
        }

        coordinatorStarted.await()
        consumer.cancelAndJoin()

        assertEquals(1L, pending.value?.token)
        assertTrue(acknowledged.isEmpty())
    }

    @Test
    fun `newer token remains queued and runs once after a blocked older reconciliation`() = runTest {
        val pending = MutableStateFlow<RuntimeReconciliationRequest?>(RuntimeReconciliationRequest(1))
        val coordinator = BlockingCoordinator()
        val acknowledgements = mutableListOf<Long>()
        val consumer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            deliverRuntimeReconciliationRequests(
                requests = pending,
                reconcile = coordinator::reconcile,
                acknowledge = { token ->
                    acknowledgements += token
                    if (pending.value?.token == token) pending.value = null
                }
            )
        }

        coordinator.firstStarted.await()
        pending.value = RuntimeReconciliationRequest(2)
        coordinator.allowFirst.complete(Unit)
        coordinator.secondStarted.await()

        assertEquals(1, coordinator.maxConcurrentExecutions)
        assertEquals(2L, pending.value?.token)
        assertEquals(listOf(1L), acknowledgements)

        coordinator.allowSecond.complete(Unit)
        advanceUntilIdle()

        assertEquals(2, coordinator.executionCount)
        assertEquals(1, coordinator.maxConcurrentExecutions)
        assertEquals(listOf(1L, 2L), acknowledgements)
        assertNull(pending.value)
        consumer.cancelAndJoin()
    }

    private class BlockingCoordinator {
        val firstStarted = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val allowFirst = CompletableDeferred<Unit>()
        val allowSecond = CompletableDeferred<Unit>()
        var executionCount = 0
            private set
        var maxConcurrentExecutions = 0
            private set
        private var activeExecutions = 0

        suspend fun reconcile() {
            executionCount++
            activeExecutions++
            maxConcurrentExecutions = maxOf(maxConcurrentExecutions, activeExecutions)
            try {
                when (executionCount) {
                    1 -> {
                        firstStarted.complete(Unit)
                        allowFirst.await()
                    }
                    2 -> {
                        secondStarted.complete(Unit)
                        allowSecond.await()
                    }
                }
            } finally {
                activeExecutions--
            }
        }
    }
}
