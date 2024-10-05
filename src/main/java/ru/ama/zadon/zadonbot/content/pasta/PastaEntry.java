package ru.ama.zadon.zadonbot.content.pasta;

import java.util.concurrent.atomic.AtomicLong;

public class PastaEntry {

    private final String name;
    private final String regex;
    private final String filename;
    private final long timeout;
    private final AtomicLong lastUseTime = new AtomicLong(0);

    public PastaEntry( String name, String regex, String filename, long timeout ) {
        this.name = name;
        this.regex = regex;
        this.filename = filename;
        this.timeout = timeout;
    }

    public String getName() {
        return name;
    }

    public String getRegex() {
        return regex;
    }

    public String getFilename() {
        return filename;
    }

    public long getTimeout() {
        return timeout;
    }

    public AtomicLong getLastUseTime() {
        return lastUseTime;
    }

}
