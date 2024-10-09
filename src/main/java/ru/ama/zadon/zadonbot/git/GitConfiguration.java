package ru.ama.zadon.zadonbot.git;

import ru.ama.zadon.zadonbot.BotProperties;
import ru.ama.zadon.zadonbot.content.ContentConfigProvider;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties( GitProperties.class )
public class GitConfiguration {

    //TODO Добавить conditional на бин при использовании другого провайдера (пока их нет)
    @Bean
    public ContentConfigProvider contentConfigProvider( BotProperties botProperties, GitProperties gitProperties ) {
        GitContentConfigProvider gitContentConfigProvider = new GitContentConfigProvider( gitProperties, botProperties.getConfigFile() );
        gitContentConfigProvider.init();
        return gitContentConfigProvider;
    }
}
