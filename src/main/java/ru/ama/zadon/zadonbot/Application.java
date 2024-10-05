package ru.ama.zadon.zadonbot;

import ru.ama.zadon.zadonbot.content.pasta.PastaProperties;
import ru.ama.zadon.zadonbot.content.pic.PicProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties( {BotProperties.class, PastaProperties.class, PicProperties.class} )
public class Application {

    public static void main( String[] args ) {
        SpringApplication.run( Application.class, args );
    }
}
