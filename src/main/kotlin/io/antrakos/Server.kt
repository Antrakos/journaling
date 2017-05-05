package io.antrakos

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoClients
import com.mongodb.rx.client.MongoCollection
import com.mongodb.rx.client.MongoDatabase
import io.antrakos.repository.JacksonCodecProvider
import io.antrakos.repository.impl.RecordRepository
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import ratpack.jackson.Jackson.json
import ratpack.rx.RxRatpack
import java.time.LocalDate


/**
 * @author Taras Zubrei
 */
object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        val kodein = Kodein {
            bind<ObjectMapper>() with singleton {
                ObjectMapper().registerKotlinModule()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
            bind<JacksonCodecProvider>() with singleton { JacksonCodecProvider(instance()) }
            bind<CodecRegistry>() with singleton {
                CodecRegistries.fromRegistries(
                        MongoClients.getDefaultCodecRegistry(),
                        CodecRegistries.fromProviders(instance<JacksonCodecProvider>())
                )
            }
            bind<MongoClient>() with singleton { MongoClients.create() }
            bind<MongoDatabase>() with singleton { instance<MongoClient>().getDatabase("journaling").withCodecRegistry(instance()) }
            bind<MongoCollection<Record>>() with singleton { instance<MongoDatabase>().getCollection("record", Record::class.java) }
            bind<RecordRepository>() with singleton { RecordRepository(instance()) }
        }

        RxRatpack.initialize();
        serverStart {
            registryOf { it.add(ObjectMapper::class.java, kodein.instance()) }
            kHandlers {
                prefix("record") {
                    get("daily/:date") {
                        val recordRepository = kodein.instance<RecordRepository>()
                        val day = LocalDate.parse(pathTokens["date"])
                        RxRatpack.promise(recordRepository.findWithin(day.atStartOfDay(), day.plusDays(1).atStartOfDay()))
                                .then { render(json(it)) }
                    }
                }
                post("check") {
                    val recordRepository = kodein.instance<RecordRepository>()
                    RxRatpack.promiseSingle(recordRepository.insert(Record()).toObservable())
                            .then { render(json(it)) }
                }
                get("hello") {
                    render("hello")
                }
            }
        }
    }
}