package com.dillonbeliveau.spf;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.ramswaroop.jbot.core.slack.models.Event;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@Component
public class MarkovModelTrainer {

    @Autowired
    MarkovModelService markovModelService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${logDir}")
    private String logDir;

    private static final Logger log = LoggerFactory.getLogger(MarkovModelTrainer.class);

    @PostConstruct
    public void init() throws IOException {
        File fLogDir = new File(logDir);

        if (!fLogDir.isDirectory()) {
            if (fLogDir.exists()) {
                throw new RuntimeException("Log dir exists, but isn't a directory!");
            }
            if (!fLogDir.mkdirs()) {
                throw new RuntimeException("Tried to make log dir, but failed!");
            }
        }

        File[] logFiles = fLogDir.listFiles();
        for (File file : logFiles) {
            log.info("Training on " + file.getAbsolutePath());
            LineIterator iter = FileUtils.lineIterator(file);

            while(iter.hasNext()) {
                String line = iter.next();
                try {
                    markovModelService.trainOnEvent(objectMapper.readValue(line, Event.class));
                } catch (Exception ex) {
                    log.warn("Failed to train on line [ " + line + " ]", ex);
                }
            }
        }
    }
}
