package ru.ama.zadon.zadonbot;

import ru.ama.zadon.zadonbot.git.GitProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties( {BotProperties.class, GitProperties.class} )
public class Application {

    public static void main( String[] args ) {
        SpringApplication.run( Application.class, args );
    }
}
