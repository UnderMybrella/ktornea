package dev.brella.ktornea.apache

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import org.apache.http.nio.IOControl

internal class KtorneaInterestControllerHolder {
    /**
     * Contains [IOControl] only when it is suspended. One should steal it first before requesting input again.
     */
    private val interestController = atomic<IOControl?>(null)

    private val waitingInput = atomic(false)
    private val waitingOutput = atomic(false)

    /**
     * Flag showing if input is suspended
     */
    public val inputSuspended: Boolean
        get() = waitingInput.value

    /**
     * Flag showing if output is suspended
     */
    public val outputSuspended: Boolean
        get() = waitingOutput.value

    /**
     * Suspend input using [ioControl] and remember it so we may resume later.
     * @throws IllegalStateException if there is another control saved before that wasn't resumed
     */
    public fun suspendInput(ioControl: IOControl) {
        waitingInput.value = true
        ioControl.suspendInput()
        interestController.update { before ->
            check(before == null || before === ioControl) { "IOControl is already published" }
            ioControl
        }
    }

    /**
     * Try to resume an io control previously saved. Does nothing if wasn't suspended or already resumed.
     * Stealing is atomic, so for every suspend invocation, only single resume is possible.
     */
    public fun resumeInputIfPossible() {
        interestController.getAndSet(null)?.requestInput()
        waitingInput.value = false
    }

    /**
     * Suspend output using [ioControl] and remember it so we may resume later.
     * @throws IllegalStateException if there is another control saved before that wasn't resumed
     */
    public fun suspendOutput(ioControl: IOControl) {
        waitingOutput.value = true
        ioControl.suspendOutput()
        interestController.update { before ->
            check(before == null || before === ioControl) { "IOControl is already published" }
            ioControl
        }
    }

    /**
     * Try to resume an io control previously saved. Does nothing if wasn't suspended or already resumed.
     * Stealing is atomic, so for every suspend invocation, only single resume is possible.
     */
    public fun resumeOutputIfPossible() {
        interestController.getAndSet(null)?.requestOutput()
        waitingOutput.value = false
    }
}