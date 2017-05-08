import io.antrakos.DayStatistics
import io.antrakos.RecordDto
import io.antrakos.Status
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * @author Taras Zubrei
 */

object DayStatisticsSpec : Spek({
    on("durantion") {
        listOf(
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 10))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 40))),
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 45))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(16, 30)))
                ) to Duration.ofMinutes(75),
                listOf(
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 40))),
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 45))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(16, 30)))
                ) to Duration.ofMinutes(685),
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 10))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 40))),
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 45)))
                ) to Duration.ofMinutes(30).plus(Duration.between(LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 45)), LocalDateTime.now())),
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 10))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 40))),
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(15, 45)))
                ) to Duration.ofMinutes(525),
                listOf<RecordDto>() to Duration.ZERO,
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 10))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 40)))
                ) to Duration.ofMinutes(30),
                listOf(
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 40)))
                ) to Duration.ofMinutes(640),
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 45)))
                ) to Duration.between(LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 45)), LocalDateTime.now()),
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(15, 45)))
                ) to Duration.ofMinutes(495)
        ).forEachIndexed { i, testCase ->
            it("should calculate work time: $i") {
                assertEquals(testCase.second, DayStatistics.calculateDuration(DayStatistics.fillInGaps(testCase.first)))
            }
        }
    }
    on("hours") {
        listOf(
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 10))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 40))),
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 45))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(16, 30)))
                ) to booleanArrayOf(false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false),
                listOf(
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 40))),
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 45))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(16, 30)))
                ) to booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, true, false, false, false, false, false, false, false),
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 10))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 40))),
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 45)))
                ) to booleanArrayOf(),
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 10))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 40))),
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(15, 45)))
                ) to booleanArrayOf(false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, true, true, true, true, true, true, true),
                listOf<RecordDto>() to booleanArrayOf(false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false),
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 10))),
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 40)))
                ) to booleanArrayOf(false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false),
                listOf(
                        RecordDto(Status.STOP, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 40)))
                ) to booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false),
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 45)))
                ) to booleanArrayOf(),
                listOf(
                        RecordDto(Status.START, LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(15, 45)))
                ) to booleanArrayOf(false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, true, true, true, true, true)
        ).forEachIndexed { i, testCase ->
            it("should mark work hours: $i") {
                assertTrue(Arrays.equals(testCase.second, DayStatistics.markHours(DayStatistics.fillInGaps(testCase.first))))
            }
        }
    }
})