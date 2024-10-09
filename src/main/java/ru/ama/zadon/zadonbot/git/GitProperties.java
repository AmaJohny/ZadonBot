package ru.ama.zadon.zadonbot.git;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ama.git")
public class GitProperties {

    private String repoDir;
    private String repoUrl;
    private String token;
    private String configUpdatePeriod;

    public String getRepoDir() {
        return repoDir;
    }

    public void setRepoDir( String repoDir ) {
        this.repoDir = repoDir;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl( String repoUrl ) {
        this.repoUrl = repoUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken( String token ) {
        this.token = token;
    }

    public String getConfigUpdatePeriod() {
        return configUpdatePeriod;
    }

    public void setConfigUpdatePeriod( String configUpdatePeriod ) {
        this.configUpdatePeriod = configUpdatePeriod;
    }
}
