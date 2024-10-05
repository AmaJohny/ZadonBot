package ru.ama.zadon.zadonbot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ama.bot")
public class BotProperties {

    private String username;
    private String token;

    public String getUsername() {
        return username;
    }

    public void setUsername( String username ) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken( String token ) {
        this.token = token;
    }
}
