package ru.ama.zadon.zadonbot.content.pic;

import java.util.concurrent.atomic.AtomicLong;

@Deprecated
public class PicEntry {

    private final String name;
    private final String prompt;
    private final String message;
    private final String filename;
    private final long timeout;
    private volatile String fileId;
    private final AtomicLong lastUseTime = new AtomicLong(0);

    public PicEntry( String name, String prompt, String message, String filename, long timeout ) {
        this.name = name;
        this.prompt = prompt;
        this.message = message;
        this.filename = filename;
        this.timeout = timeout;
    }

    public String getName() {
        return name;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getMessage() {
        return message;
    }

    public String getFilename() {
        return filename;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId( String fileId ) {
        this.fileId = fileId;
    }

    public AtomicLong getLastUseTime() {
        return lastUseTime;
    }

}
