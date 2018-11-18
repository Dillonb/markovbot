package com.dillonbeliveau.spf.service


import com.fasterxml.jackson.databind.ObjectMapper
import me.ramswaroop.jbot.core.slack.models.Event
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.*
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.collections.ArrayList

interface Transition
object ToEnd : Transition
data class ToWord(val word: String) : Transition
data class ToWords(val words: List<String>) : Transition

interface Trigger
object FromBeginning: Trigger
data class FromWords(val words: List<String>) : Trigger

@Component
class MarkovModelService {
    val log = LoggerFactory.getLogger(javaClass)

    @Value("\${logDir}")
    internal var logDir: String? = null

    @Value("\${modelCachePath:}")
    val modelCachePath: String? = null

    @Value("\${markovDegree:2}")
    internal var markovDegree: Int? = null

    val nonAlphaNumeric = Regex("[^A-Za-z0-9 ]")

    private val random = Random()

    private val mapper = ObjectMapper()

    // TODO move me to the config file, or include a text file with a pre-done much larger list
    internal val bannedWords: Set<String> = setOf("rape", "raped", "raping", "rapist", "rayped")
    internal var transitions: Map<Trigger, Map<Transition, Int>> = HashMap()

    @Throws(IOException::class)
    private fun trainOnLine(line: String) {
        val event = mapper.readValue<Event>(line, Event::class.java)
        trainOnEvent(event)

    }

    private fun stripNonAlphaNumeric(str: String): String {
        return nonAlphaNumeric.replace(str, "")
    }

    // TODO I am huge, break me up
    private fun trainOnMessage(text: String?) {
        if (text == null || text.length < 20) {
            return
        }

        val words = text.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }

        if (words.isEmpty()) {
            return
        }

        // If the message contains a banned word, don't train on it at all.
        // Chances are there's other nasty stuff we don't want the bot learning in it.
        if (words.any { word -> bannedWords.contains(stripNonAlphaNumeric(word)) }) {
            return
        }

        val lastWords = words.takeLast(markovDegree!!)

        val markovSets = words.windowed(markovDegree!! + 1) {
            set ->
            val priorWords = set.subList(0, set.size - 1).toList()
            val nextWord = set[set.size - 1]

            val result: Pair<Trigger, Transition> = Pair(FromWords(priorWords), ToWord(nextWord))

            result
        }
                .plus(FromBeginning to ToWords(words.take(markovDegree!!)))
                .plus(FromWords(lastWords) to ToEnd)

        transitions = markovSets.fold(transitions) {
            transitions, newTransition ->

            val trigger: Trigger = newTransition.first
            val transition = newTransition.second


            val existingTransitions: Map<Transition, Int> = transitions.getOrDefault(trigger, HashMap())
            val existingOccurrences = existingTransitions.getOrDefault(transition, 0)

            val newTransitions = existingTransitions.plus(Pair(transition, existingOccurrences + 1))

            transitions.plus(Pair(trigger, newTransitions))
        }
    }


    fun trainOnEvent(event: Event) {
        if ("message" == event.type && event.subtype == null) {
            trainOnMessage(event.text)
        }
    }

    private fun getLogFiles(): Array<File> {
        // For now, retrain the model every launch. Eventually we want to cache the model
        val logDirectory = File(logDir)

        if (!logDirectory.isDirectory) {
            throw RuntimeException("Log directory not a directory!")
        }

        return logDirectory.listFiles()
    }

    @Throws(IOException::class)
    private fun trainOnLogfile(file: File) {
        val lineIterator = FileUtils.lineIterator(file)

        while (lineIterator.hasNext()) {
            val eventStr = lineIterator.nextLine()
            trainOnLine(eventStr)
        }
    }

    private fun generateModelFromScratch() {
        for (file in getLogFiles()) {
            log.info("Loading " + file.getName());
            trainOnLogfile(file)
        }

        saveModel()
    }

    @PostConstruct
    @Throws(IOException::class)
    fun loadModel() {
        if (modelCachePath.isNullOrBlank()) {
            generateModelFromScratch()
        }
        else {
            val modelCacheFile = File(modelCachePath)

            if (modelCacheFile.isFile) {
                transitions = mapper.readValue(modelCacheFile, transitions.javaClass)
            }
            else {
                generateModelFromScratch()
            }
        }

    }

    private fun getWriter(filename: String): PrintWriter {
        val fw = FileWriter(filename, false)
        val bw = BufferedWriter(fw)
        return PrintWriter(bw)
    }

    @PreDestroy
    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    fun saveModel() {
        if (!modelCachePath.isNullOrBlank()) {
            val modelWriter = getWriter(modelCachePath!!)
            mapper.writeValue(modelWriter, transitions)
            log.info("Wrote model to disk")
        }
        else {
            log.info("No model cache path provided, not saving model.")
        }
    }

    fun getNext(trigger: Trigger): Transition {
        val map = transitions[trigger]!!
        val total = map.values.sum()
        var randomVal = random.nextInt(total)

        return map.entries.shuffled().dropWhile { entry ->
            randomVal -= entry.value
            randomVal > 0
        }.first().key
    }

    fun generateMessage(): String {
        var transition = getNext(FromBeginning)

        var message: List<String> = ArrayList()

        // TODO This loop here needs to be improved. Lots of edge cases where this can break
        while (transition !== ToEnd) {
            when (transition) {
                is ToWord -> message = message.plus(transition.word)
                is ToWords -> message = message.plus(transition.words)
            }

            transition = getNext(FromWords(message.takeLast(markovDegree!!)))
        }

        return message.joinToString(" ")
    }
}