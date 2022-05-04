package dev.brella.ktornea.results

import dev.brella.kornea.errors.common.KorneaResult
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.util.reflect.*
import kotlin.reflect.KClass

/**
 * Specifies how the exception should be handled.
 */
public typealias KorneaHandlerFunction = suspend (call: ApplicationCall, failure: KorneaResult.Failure) -> Unit

/**
 * A plugin that handles exceptions and status codes. Useful to configure default error pages.
 */
public val KorneaResultPages: ApplicationPlugin<KorneaResultPagesConfig> = createApplicationPlugin(
    "KorneaResultPages",
    ::KorneaResultPagesConfig
) {
    val resultPhase = PipelinePhase("KorneaResultParsing")

    this.application.sendPipeline.insertPhaseBefore(ApplicationSendPipeline.Transform, resultPhase)

    val exceptions = HashMap(pluginConfig.failures)
    val defaultHandler = pluginConfig.defaultErrorHandler

    fun findHandlerByValue(failure: KorneaResult.Failure): KorneaHandlerFunction? {
        val key = exceptions.keys.find { failure.instanceOf(it) } ?: return null
        return exceptions[key]
    }

    this.application.sendPipeline.intercept(resultPhase) {subject ->
        val failure = when (subject) {
            is KorneaResult.Failure -> subject
            is KorneaResult<*> -> {
                val failure = subject.failureOrNull()
                if (failure == null) {
                    this.subject = subject.getOrNull()!!
                    return@intercept
                }

                failure
            }
            else -> return@intercept
        }

        findHandlerByValue(failure)?.invoke(call, failure)

        if (!call.isHandled) defaultHandler?.invoke(call, failure)
        if (!call.isHandled) call.respondText("Unhandled failure of type ${failure} / ${failure::class}", status = HttpStatusCode.InternalServerError)

        this.finish()
    }
}

/**
 * A [KorneaResultPages] plugin configuration.
 */
@KtorDsl
public class KorneaResultPagesConfig {
    /**
     * Provides access to exception handlers of the exception class.
     */
    public val failures: MutableMap<KClass<*>, KorneaHandlerFunction> = mutableMapOf()

    /**
     * Provides access to a default handler if no specific is registered
     */
    public var defaultErrorHandler: KorneaHandlerFunction? = null

    /**
     * Register an exception [handler] for the exception type [T] and its children.
     */
    public inline fun <reified T : KorneaResult.Failure> failure(
        noinline handler: suspend (call: ApplicationCall, failure: T) -> Unit
    ): Unit = failure(T::class, handler)

    /**
     * Register an exception [handler] for the exception class [klass] and its children.
     */
    public fun <T : KorneaResult.Failure> failure(
        klass: KClass<T>,
        handler: suspend (call: ApplicationCall, T) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        val cast = handler as suspend (ApplicationCall, KorneaResult.Failure) -> Unit

        failures[klass] = cast
    }

    public fun defaultHandler(handler: suspend (call: ApplicationCall, KorneaResult.Failure) -> Unit) {
        this.defaultErrorHandler = handler
    }
}