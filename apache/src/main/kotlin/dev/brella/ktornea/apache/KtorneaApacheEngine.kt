package dev.brella.ktornea.apache

import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.utils.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.apache.http.HttpHost
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.impl.nio.reactor.IOReactorConfig
import java.net.InetSocketAddress
import java.net.Proxy

private const val MAX_CONNECTIONS_COUNT = 1000
private const val IO_THREAD_COUNT_DEFAULT = 4

internal class KtorneaApacheEngine(override val config: ApacheEngineConfig) : HttpClientEngineBase("ktor-blaseball-apache") {
    @InternalAPI
    override val dispatcher by lazy {
        Dispatchers.clientDispatcher(
            config.threadsCount,
            "ktor-blaseball-apache-dispatcher"
        )
    }

    override val supportedCapabilities = setOf(HttpTimeout)

    private val engine: CloseableHttpAsyncClient = prepareClient().apply { start() }

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val callContext = callContext()

        val apacheRequest = KtorneaApacheRequestProducer(data, config, callContext)
        return engine.sendRequest(apacheRequest, callContext, data)
    }

    override fun close() {
        super.close()

        coroutineContext[Job]!!.invokeOnCompletion {
            engine.close()
        }
    }

    private fun prepareClient(): CloseableHttpAsyncClient {
        val clientBuilder = HttpAsyncClients.custom()
        with(clientBuilder) {
            setThreadFactory {
                Thread(it, "Ktor-client-apache").apply {
                    isDaemon = true
                    setUncaughtExceptionHandler { _, _ -> }
                }

            }
            disableAuthCaching()
            disableConnectionState()
            disableCookieManagement()
            setMaxConnPerRoute(MAX_CONNECTIONS_COUNT)
            setMaxConnTotal(MAX_CONNECTIONS_COUNT)
            setDefaultIOReactorConfig(
                IOReactorConfig.custom()
                                          .setIoThreadCount(IO_THREAD_COUNT_DEFAULT)
                                          .build())

            setupProxy()
        }

        with(config) {
            clientBuilder.customClient()
        }

        config.sslContext?.let { clientBuilder.setSSLContext(it) }
        return clientBuilder.build()!!
    }

    private fun HttpAsyncClientBuilder.setupProxy() {
        val proxy = config.proxy ?: return

        if (proxy.type() == Proxy.Type.DIRECT) {
            return
        }

        val address = proxy.address()
        check(proxy.type() == Proxy.Type.HTTP && address is InetSocketAddress) {
            "Only http proxy is supported for Apache engine."
        }

        setProxy(HttpHost.create("http://${address.hostName}:${address.port}"))
    }
}
