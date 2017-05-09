package io.antrakos

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.instance
import io.antrakos.exception.Error
import ratpack.exec.Promise
import ratpack.guice.BindingsSpec
import ratpack.guice.Guice
import ratpack.handling.Chain
import ratpack.handling.Context
import ratpack.registry.Registry
import ratpack.registry.RegistrySpec
import ratpack.rx.RxRatpack
import ratpack.server.RatpackServer
import ratpack.server.RatpackServerSpec
import ratpack.server.ServerConfigBuilder
import rx.Observable
import rx.Single
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * @author Taras Zubrei
 */
fun Date.toLocalDateTime(): LocalDateTime = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

fun LocalDateTime.toDate(): Date = Date.from(this.atZone(ZoneId.systemDefault()).toInstant())

fun <T> Single<T>.toPromise(): Promise<T> = RxRatpack.promiseSingle(this.toObservable())
fun <T> Observable<T>.toPromise(): Promise<List<T>> = RxRatpack.promise(this)
fun <T> Promise<T>.toSingle(): Single<T> = RxRatpack.observe(this).toSingle()
fun <T> Promise<T>.toObservable(): Observable<T> = RxRatpack.observe(this)
/*
 * Helper functions to make the example more Kotlin like.
 */



fun serverOf(cb: KServerSpec.() -> Unit) = RatpackServer.of { KServerSpec(it).cb() }
fun serverStart(cb: KServerSpec.() -> Unit) = RatpackServer.start { KServerSpec(it).cb() }

class KChain(val delegate: Chain) : Chain by delegate {
    fun FileSystem(path: String = "", cb: KChain.() -> Unit) = delegate.fileSystem(path) { KChain(it).cb() }

    fun Prefix(path: String = "", cb: KChain.() -> Unit) = delegate.prefix(path) { KChain(it).cb() }

    fun All(cb: KContext.() -> Unit) = delegate.all { KContext(it).cb() }
    fun Path(path: String = "", cb: KContext.() -> Unit) = delegate.path(path) { KContext(it).cb() }

    @Suppress("ReplaceGetOrSet")
    fun Get(path: String = "", cb: KContext.() -> Unit) = delegate.get(path) { KContext(it).cb() }

    fun Put(path: String = "", cb: KContext.() -> Unit) = delegate.put(path) { KContext(it).cb() }
    fun Post(path: String = "", cb: KContext.() -> Unit) = delegate.post(path) { KContext(it).cb() }
    fun Delete(path: String = "", cb: KContext.() -> Unit) = delegate.delete(path) { KContext(it).cb() }
    fun Options(path: String = "", cb: KContext.() -> Unit) = delegate.options(path) { KContext(it).cb() }
    fun Patch(path: String = "", cb: KContext.() -> Unit) = delegate.patch(path) { KContext(it).cb() }
}

class KContext(val delegate: Context) : Context by delegate {
    fun ClientError(error: Error) = delegate.clientError(error.ordinal)
    inline fun <reified T : Any> instance(): T = try {
        delegate.context[Kodein::class.java].instance()
    } catch (ex: Kodein.NotFoundException) {
        delegate.context[T::class.java]
    }
}

class KServerSpec(val delegate: RatpackServerSpec) : RatpackServerSpec by delegate {
    fun ServerConfig(cb: ServerConfigBuilder.() -> Unit) = delegate.serverConfig { it.cb() }
    fun GuiceRegistry(cb: BindingsSpec.() -> Unit) = delegate.registry(Guice.registry(cb))
    fun Registry(cb: RegistrySpec.() -> Unit) = delegate.registry(Registry.of(cb))
    fun Handlers(cb: KChain.() -> Unit) = delegate.handlers { KChain(it).cb() }
}