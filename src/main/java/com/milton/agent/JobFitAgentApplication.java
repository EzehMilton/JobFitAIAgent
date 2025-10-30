package com.milton.agent;

import com.embabel.agent.config.annotation.EnableAgents;
import com.embabel.agent.config.annotation.LoggingThemes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAgents(loggingTheme = LoggingThemes.STAR_WARS)
@EnableScheduling
public class JobFitAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobFitAgentApplication.class, args);
    }

}
