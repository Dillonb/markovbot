package com.dillonbeliveau.modeltodot

import com.dillonbeliveau.spf.service.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import java.io.PrintWriter
import java.lang.StringBuilder

@SpringBootApplication
@ComponentScan(basePackages = ["com.dillonbeliveau.spf.modeltodot"])
class ModelToDot {
    @Bean
    fun markovModelService(): MarkovModelService {
        return MarkovModelService(false)
    }
}

data class GraphEdge(val from: String, val to:String)

fun wordListToEdges(words: List<String>): List<GraphEdge> {
    return words
            .windowed(2)
            .map { window -> GraphEdge(window.first(), window.last()) }
}


fun transitionToEdges(from: Trigger, to: Set<Transition>): List<GraphEdge> {
    val lastWordInTrigger: String = when(from) {
        is FromWords ->  "'" + from.words.last() + "'"
        else -> {
            if (from == FromBeginning) {
                println("From beginning!")
                "BEGIN"
            }
            else {
                "???"
            }
        }
    }

    val transitionEdges: List<GraphEdge> = to.flatMap { transitionTo ->
        if (transitionTo == ToEnd) {
            listOf(GraphEdge(lastWordInTrigger, "END"))
        }
        else {
            val graphEdges: List<GraphEdge> = when (transitionTo) {
                is ToWord -> listOf(GraphEdge(lastWordInTrigger, "'" + transitionTo.word + "'"))
                is ToWords -> wordListToEdges(listOf(lastWordInTrigger) + transitionTo.words.map { word -> "'$word'" })
                else -> emptyList()
            }

            graphEdges
        }
    }

    val triggerEdges = triggerToEdges(from)

    return triggerEdges + transitionEdges
}

fun triggerToEdges(trigger: Trigger): List<GraphEdge> {
    return when (trigger) {
        is FromWords -> wordListToEdges(trigger.words.map { word -> "'$word'" })
        else -> emptyList()
    }
}

fun dotEscape(str: String): String {
    return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
}

fun toDot(edges: List<GraphEdge>): String {
    val output = StringBuilder()
            .append("digraph {\n")


    for (edge in edges) {
        val from = dotEscape(edge.from)
        val to = dotEscape(edge.to)

        output.append("\t")
                .append("\"")
                .append(from)
                .append("\"")
                .append(" -> ")
                .append("\"")
                .append(to)
                .append("\"")
                .append(";\n")
    }

    output.append("}\n")

    return output.toString()
}

fun main(args: Array<String>) {
    val ctx = SpringApplicationBuilder(ModelToDot::class.java).web(false).run()
    val markovModelService = ctx.getBean(MarkovModelService::class.java)

    val transitions = markovModelService.transitions


    val edges = transitions.keys.flatMap { from ->
        val to = transitions[from]!!.keys

        transitionToEdges(from, to)
    }.distinct()

    val dotOutput = toDot(edges)

    val writer = PrintWriter("model.dot")
    writer.print(dotOutput)
    writer.close()

    ctx.close()
}

