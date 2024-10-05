package ru.ama.zadon.zadonbot.yaml.pasta;

public class Pasta {

    private String name;
    private String regex;
    private String filename;
    private String timeout;

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex( String regex ) {
        this.regex = regex;
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
