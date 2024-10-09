package ru.ama.zadon.zadonbot.yaml.content;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

public class ContentEntryYaml {

    private String name;
    private String type;
    private Set<String> keywords;
    private String prompt;
    private String message;
    private String filename;
    private String timeout;

    @Nonnull
    public String getName() {
        return name;
    }

    public void setName( @Nonnull String name ) {
        this.name = name;
    }

    @Nonnull
    public String getType() {
        return type;
    }

    public void setType( @Nonnull String type ) {
        this.type = type;
    }

    @Nullable
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt( @Nullable String prompt ) {
        this.prompt = prompt;
    }

    @Nullable
    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords( @Nullable Set<String> keywords ) {
        this.keywords = keywords;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public void setMessage( @Nullable String message ) {
        this.message = message;
    }

    @Nullable
    public String getFilename() {
        return filename;
    }

    public void setFilename( @Nullable String filename ) {
        this.filename = filename;
    }

    @Nullable
    public String getTimeout() {
        return timeout;
    }

    public void setTimeout( @Nullable String timeout ) {
        this.timeout = timeout;
    }
}
