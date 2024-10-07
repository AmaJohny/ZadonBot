package ru.ama.zadon.zadonbot.yaml.content.pic;

public class PicYaml {

    private String name;
    private String prompt;
    private String message;
    private String filename;
    private String timeout;

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt( String prompt ) {
        this.prompt = prompt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage( String message ) {
        this.message = message;
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
