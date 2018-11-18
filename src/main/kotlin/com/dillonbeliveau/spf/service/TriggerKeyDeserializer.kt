package com.dillonbeliveau.spf.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.ObjectMapper

class TriggerKeyDeserializer: KeyDeserializer() {
    val fromWords = """^FromWords\(words=(.+)\)$""".toRegex(RegexOption.DOT_MATCHES_ALL)
    val fromBeginning = """FromBeginning""".toRegex()

    private val objectMapper = ObjectMapper()
    private val listTypeReference = object: TypeReference<ArrayList<String>>() {}

    private fun doDeserialization(key: String): Trigger {
        val fromWordsMatch = fromWords.matchEntire(key)
        if (fromWordsMatch != null) {
            val wordsJson = fromWordsMatch.groupValues[1]
            val wordsList = objectMapper.readValue<ArrayList<String>>(wordsJson, listTypeReference)
            return FromWords(wordsList)
        }

        if (fromBeginning.matches(key)) {
            return FromBeginning
        }
        throw RuntimeException("Something went wrong, $key matched no regex")
    }

    override fun deserializeKey(key: String?, context: DeserializationContext?): Trigger {
        val result = doDeserialization(key!!)

        if (result.toString() != key) {
            throw RuntimeException("Something went wrong during deserialization, got different value out. $result != $key")
        }

        return result
    }
}