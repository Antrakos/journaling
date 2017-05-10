package io.antrakos

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.mongodb.MongoWriteException
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoClients
import com.mongodb.rx.client.MongoDatabase
import io.antrakos.exception.Error
import io.antrakos.repository.impl.RecordRepository
import io.antrakos.repository.impl.UserRepository
import io.antrakos.security.BasicAuthenticator
import io.antrakos.service.*
import io.antrakos.web.JacksonCodecProvider
import org.bson.codecs.configuration.CodecRegistries
import org.pac4j.core.profile.UserProfile
import org.pac4j.http.client.direct.DirectBasicAuthClient
import org.pac4j.http.credentials.authenticator.UsernamePasswordAuthenticator
import org.pac4j.http.credentials.password.BasicSaltedSha512PasswordEncoder
import org.pac4j.http.credentials.password.PasswordEncoder
import ratpack.error.ClientErrorHandler
import ratpack.error.ServerErrorHandler
import ratpack.handling.RequestLogger
import ratpack.jackson.Jackson.fromJson
import ratpack.jackson.Jackson.json
import ratpack.pac4j.RatpackPac4j
import ratpack.rx.RxRatpack
import ratpack.session.SessionModule
import java.time.LocalDate
import java.util.*


/**
 * @author Taras Zubrei
 */
object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        val kodein = Kodein {
            bind() from singleton {
                ObjectMapper().findAndRegisterModules()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
            bind() from singleton { JacksonCodecProvider(instance()) }
            bind() from singleton {
                CodecRegistries.fromRegistries(
                        MongoClients.getDefaultCodecRegistry(),
                        CodecRegistries.fromProviders(instance<JacksonCodecProvider>())
                )
            }
            bind() from singleton { MongoClients.create() }
            bind() from singleton { instance<MongoClient>().getDatabase("journaling").withCodecRegistry(instance()) }
            bind() from singleton { instance<MongoDatabase>().getCollection("record", Record::class.java) }
            bind() from singleton { instance<MongoDatabase>().getCollection("user", User::class.java).apply { createIndex(Indexes.ascending("username"), IndexOptions().unique(true)).toBlocking().subscribe() } }
            bind() from singleton { RecordRepository(instance()) }
            bind() from singleton { UserRepository(instance()) }
            bind<PasswordEncoder>() with singleton { BasicSaltedSha512PasswordEncoder("salt") }
            bind<UsernamePasswordAuthenticator>() with singleton { BasicAuthenticator(instance(), instance()) }
            bind() from singleton { ResourceBundle.getBundle("messages", Locale.ENGLISH) }
            bind<ClientErrorHandler>() with singleton { io.antrakos.exception.ClientErrorHandler(instance()) }
            bind<ServerErrorHandler>() with singleton { io.antrakos.exception.ServerErrorHandler() }
            bind() from singleton { DayStatisticsService(instance()) }
            bind() from singleton { MonthStatisticsService(instance()) }
            bind() from singleton { AuthenticationService(instance(), instance()) }
            bind() from singleton { WorkService(instance()) }
            bind() from singleton { UserService(instance(), instance()) }
        }

        RxRatpack.initialize();
        serverStart {
            ServerConfig {
                env()
            }
            GuiceRegistry {
                module(SessionModule::class.java)
                bindInstance(Kodein::class.java, kodein)
                bindInstance(ObjectMapper::class.java, kodein.instance())
                bindInstance(ClientErrorHandler::class.java, kodein.instance())
                bindInstance(ServerErrorHandler::class.java, kodein.instance())
            }
            Handlers {
                all(RequestLogger.ncsa())
                all(RatpackPac4j.authenticator(DirectBasicAuthClient(kodein.instance())))
                Prefix("auth") {
                    Post("register") {
                        instance<AuthenticationService>()
                                .register(parse(fromJson(User::class.java)).toSingle())
                                .toPromise()
                                .onError {
                                    when (it) {
                                        is MongoWriteException -> ClientError(Error.DUPLICATE_USERNAME)
                                    }
                                }
                                .then { render(json(it)) }
                    }
                }
                Prefix("user") {
                    Get(":id/status") {
                        instance<UserService>()
                                .getStatus(pathTokens["id"]!!)
                                .toPromise()
                                .then { render(json(it)) }
                    }
                }
                all(RatpackPac4j.requireAuth(DirectBasicAuthClient::class.java))
                Prefix("record") {
                    Get("daily/:date") {
                        instance<DayStatisticsService>()
                                .get(LocalDate.parse(pathTokens["date"]), instance<UserProfile>().id)
                                .toPromise()
                                .then { render(json(it)) }
                    }
                    Get("monthly/:month") {
                        instance<MonthStatisticsService>()
                                .get(pathTokens["month"]!!.toInt(), instance<UserProfile>().id)
                                .toPromise()
                                .then { render(json(it)) }
                    }
                }
                Post("work/check-in") {
                    instance<WorkService>()
                            .checkIn(instance<UserProfile>().id)
                            .toPromise()
                            .then { render(json(mapOf("status" to it))) }
                }
                Prefix("user") {
                    Get("search") {
                        val username = request.queryParams["username"] ?: throw IllegalArgumentException("Username param is required")
                        instance<UserService>()
                                .searchByUsername(username)
                                .toPromise()
                                .then { render(json(it)) }
                    }
                }
            }
        }
    }
}