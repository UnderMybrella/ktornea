package dev.brella.ktornea.apache

import io.ktor.client.request.*
import io.ktor.utils.io.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.http.HttpResponse
import org.apache.http.nio.ContentDecoder
import org.apache.http.nio.IOControl
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer
import org.apache.http.protocol.HttpContext
import kotlin.coroutines.CoroutineContext

@OptIn(InternalCoroutinesApi::class)
internal class KtorneaApacheResponseConsumer(
    val parentContext: CoroutineContext,
    private val requestData: HttpRequestData
) : HttpAsyncResponseConsumer<Unit>, CoroutineScope {
    private val interestController = KtorneaInterestControllerHolder()

    private val consumerJob = Job(parentContext[Job])
    override val coroutineContext: CoroutineContext = parentContext + consumerJob

    private val waiting = atomic(false)
    private val channel = ByteChannel(true).also {
        it.attachJob(consumerJob)
    }

    private val responseDeferred = CompletableDeferred<HttpResponse>()

    val responseChannel: ByteReadChannel = channel

    init {
        coroutineContext[Job]?.invokeOnCompletion(onCancelling = true) { cause ->
            if (cause != null) {
                responseDeferred.completeExceptionally(cause)
                responseChannel.cancel(cause)
            }
        }
    }

    override fun consumeContent(decoder: ContentDecoder, ioctrl: IOControl) {
        check(!waiting.value)

        var result: Int
        do {
            result = 0
            channel.writeAvailable {
                result = decoder.read(it)
            }
        } while (result > 0)

        if (result < 0 || decoder.isCompleted) {
            close()
            return
        }

        if (result == 0) {
            interestController.suspendInput(ioctrl)
            launch(Dispatchers.Unconfined) {
                check(!waiting.getAndSet(true))
                channel.awaitFreeSpace()
                check(waiting.getAndSet(false))
                interestController.resumeInputIfPossible()
            }
        }
    }

    override fun failed(cause: Exception) {
        val mappedCause = mapCause(cause, requestData)
        consumerJob.completeExceptionally(mappedCause)
        responseDeferred.completeExceptionally(mappedCause)
        responseChannel.cancel(mappedCause)
    }

    override fun cancel(): Boolean {
        return true
    }

    override fun close() {
        channel.close()
        consumerJob.complete()
    }

    override fun getException(): Exception? = channel.closedCause as? Exception

    override fun getResult() {
    }

    override fun isDone(): Boolean = channel.isClosedForWrite

    override fun responseCompleted(context: HttpContext) {
    }

    override fun responseReceived(response: HttpResponse) {
        responseDeferred.complete(response)
    }

    public suspend fun waitForResponse(): HttpResponse = responseDeferred.await()
}