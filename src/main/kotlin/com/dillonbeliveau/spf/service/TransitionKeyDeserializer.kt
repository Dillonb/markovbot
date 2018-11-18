package com.dillonbeliveau.spf.service

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

class TransitionKeyDeserializer: KeyDeserializer() {
    val toWord = """^ToWord\(word=(.*)\)$""".toRegex(RegexOption.DOT_MATCHES_ALL)
    val toWords = """^ToWords\(words=(.+)\)$""".toRegex(RegexOption.DOT_MATCHES_ALL)
    val toEnd = """ToEnd""".toRegex()

    private val objectMapper = ObjectMapper()
    private val listTypeReference = object: TypeReference<ArrayList<String>>() {}

    private fun doDeserialization(key: String): Transition {
        val toWordMatch = toWord.matchEntire(key)
        if (toWordMatch != null) {
            val result = ToWord(toWordMatch.groupValues[1])
            return result
        }

        if (toEnd.matches(key)) {
            return ToEnd
        }

        val toWordsMatch = toWords.matchEntire(key)
        if (toWordsMatch != null) {
            val wordsJson = toWordsMatch.groupValues[1]
            val wordsList = objectMapper.readValue<ArrayList<String>>(wordsJson, listTypeReference)
            return ToWords(wordsList)
        }
        throw RuntimeException("Something went wrong, $key did not match any regex")
    }

    override fun deserializeKey(key: String?, context: DeserializationContext?): Transition {
        val result = doDeserialization(key!!)

        if (result.toString() != key) {
            throw RuntimeException("Something went wrong during deserialization, got different value out. $result != $key")
        }

        return result
    }

}