package com.dillonbeliveau.spf


import com.fasterxml.jackson.databind.ObjectMapper
import me.ramswaroop.jbot.core.slack.models.Event
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.util.*
import javax.annotation.PostConstruct
import kotlin.collections.ArrayList

interface Transition
object ToEnd : Transition
data class ToWord(val word: String) : Transition

interface Trigger
object FromBeginning: Trigger
data class FromWords(val words: List<String>) : Trigger

@Component
class MarkovModelService {
    @Value("\${logDir}")
    internal var logDir: String? = null

    @Value("\${markovDegree:1}")
    internal var markovDegree: Int? = null

    private val random = Random()

    private val mapper = ObjectMapper()

    @Throws(IOException::class)
    private fun trainOnLine(line: String) {
        val event = mapper.readValue<Event>(line, Event::class.java)
        trainOnEvent(event)

    }

    internal var transitions: Map<Trigger, Map<Transition, Int>> = HashMap()

    private fun trainOnMessage(text: String?) {
        if (text == null) {
            return
        }

        val words = text.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }

        if (words.isEmpty()) {
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
                .plus(FromBeginning to ToWord(words.first()))
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


    public fun trainOnEvent(event: Event) {
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

    @PostConstruct
    @Throws(IOException::class)
    fun loadModel() {
        for (file in getLogFiles()) {
            trainOnLogfile(file)
        }
    }

    fun getNext(trigger: Trigger): Transition {
        val map = transitions[trigger]!!
        val total = map.values.sum()
        var randomVal = random.nextInt(total)

        return map.entries.dropWhile { entry ->
            randomVal -= entry.value
            randomVal > 0
        }.first().key
    }

    fun generateMessage(): String {
        var transition = getNext(FromBeginning)

        var message: List<String> = ArrayList()

        while (transition !is ToEnd) {
            when (transition) {
                is ToWord -> message = message.plus(transition.word)
            }

            transition = getNext(FromWords(message.takeLast(markovDegree!!)))
        }

        return message.joinToString(" ")
    }
}