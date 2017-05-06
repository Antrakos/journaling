package io.antrakos

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
fun <T> Observable<T>.toPromise(): Promise<T> = RxRatpack.promiseSingle(this)
fun <T> Promise<T>.toSingle(): Single<T> = RxRatpack.observe(this).toSingle()
fun <T> Promise<T>.toObservable(): Observable<T> = RxRatpack.observe(this)
/*
 * Helper functions to make the example more Kotlin like.
 */



fun serverOf(cb: KServerSpec.() -> Unit) = RatpackServer.of { KServerSpec(it).cb() }
fun serverStart(cb: KServerSpec.() -> Unit) = RatpackServer.start { KServerSpec(it).cb() }

class KChain(val delegate: Chain) : Chain by delegate {
    fun fileSystem(path: String = "", cb: KChain.() -> Unit) = delegate.fileSystem(path) { KChain(it).cb() }

    fun kPrefix(path: String = "", cb: KChain.() -> Unit) = delegate.prefix(path) { KChain(it).cb() }

    fun all(cb: Context.() -> Unit) = delegate.all { it.cb() }
    fun path(path: String = "", cb: Context.() -> Unit) = delegate.path(path) { it.cb() }

    @Suppress("ReplaceGetOrSet")
    fun get(path: String = "", cb: Context.() -> Unit) = delegate.get(path) { it.cb() }

    fun put(path: String = "", cb: Context.() -> Unit) = delegate.put(path) { it.cb() }
    fun post(path: String = "", cb: Context.() -> Unit) = delegate.post(path) { it.cb() }
    fun delete(path: String = "", cb: Context.() -> Unit) = delegate.delete(path) { it.cb() }
    fun options(path: String = "", cb: Context.() -> Unit) = delegate.options(path) { it.cb() }
    fun patch(path: String = "", cb: Context.() -> Unit) = delegate.patch(path) { it.cb() }
}

class KContext(val delegate: Context) : Context by delegate

class KServerSpec(val delegate: RatpackServerSpec) : RatpackServerSpec by delegate {
    fun kServerConfig(cb: ServerConfigBuilder.() -> Unit) = delegate.serverConfig { it.cb() }
    fun guiceRegistry(cb: BindingsSpec.() -> Unit) = delegate.registry(Guice.registry(cb))
    fun kRegistry(cb: RegistrySpec.() -> Unit) = delegate.registry(Registry.of(cb))
    fun kHandlers(cb: KChain.() -> Unit) = delegate.handlers { KChain(it).cb() }
}