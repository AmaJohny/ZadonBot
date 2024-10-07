package ru.ama.zadon.zadonbot.yaml.content.pasta;

import java.util.Set;

public class PastaYaml {

    private String name;
    private Set<String> keywords;
    private String filename;
    private String timeout;

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords( Set<String> keywords ) {
        this.keywords = keywords;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename( String filename ) {
        this.filename = filename;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout( String timeout ) {
        this.timeout = timeout;
    }
}
