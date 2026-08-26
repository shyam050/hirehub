package com.hirehub.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    private OpenAi openai = new OpenAi();
    private Analysis analysis = new Analysis();

    @Getter
    @Setter
    public static class OpenAi {
        private String apiKey = "";
        private String model = "gpt-4o";
        private String baseUrl = "https://api.openai.com";
    }

    @Getter
    @Setter
    public static class Analysis {
        /** How long a cached analysis is considered fresh (in days) */
        private int cacheDays = 7;
        /** Maximum text length sent to AI (characters) */
        private int maxTextLength = 8000;
    }
}
