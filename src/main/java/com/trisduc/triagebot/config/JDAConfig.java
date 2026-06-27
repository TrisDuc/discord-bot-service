package com.trisduc.triagebot.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

@Configuration
public class JDAConfig {

    @Bean
    public JDA jda(@Value("${discord.bot.token}") String botToken) throws InterruptedException {
        JDA jda = JDABuilder.createLight(botToken, EnumSet.noneOf(GatewayIntent.class))
                .build();
        jda.awaitReady();
        return jda;
    }
}