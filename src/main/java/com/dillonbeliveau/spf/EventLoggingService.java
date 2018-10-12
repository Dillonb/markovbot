package com.dillonbeliveau.spf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.ramswaroop.jbot.core.slack.models.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

@Component
public class EventLoggingService {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${logDir}")
    private String logDir;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private String getCurrentLogFilePath() {
        String date = simpleDateFormat.format(System.currentTimeMillis());

        return String.format("%s/%s.slack.log", logDir, date);
    }

    public EventLoggingService() {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private PrintWriter logWriter;
    private String activeLogFilePath;

    private PrintWriter getWriter(String filename) throws IOException {
        FileWriter fw = new FileWriter(filename, true);
        BufferedWriter bw = new BufferedWriter(fw);
        return new PrintWriter(bw);
    }

    private PrintWriter getWriter() throws IOException {
        String newLogFilePath = getCurrentLogFilePath();

        // If we don't have a logWriter yet or need to rotate a file
        if (logWriter == null || !newLogFilePath.equals(activeLogFilePath)) {
            // In case of rotating a log file, make sure the buffer is flushed
            if (logWriter != null) {
                logWriter.flush();
                logWriter.close();
            }

            logWriter = getWriter(newLogFilePath);
            activeLogFilePath = newLogFilePath;
        }

        return logWriter;
    }

    public void logEvent(Event event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);

            getWriter().println(eventJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    @Scheduled(initialDelay = 10_000, fixedDelay = 10_000)
    void flushWriter() throws IOException {
        getWriter().flush();
    }

}
