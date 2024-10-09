package ru.ama.zadon.zadonbot.content.pasta;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Deprecated
public class PastaEntry {

    private final String name;
    private final Set<String> keywords;
    private final String filename;
    private final long timeout;
    private final AtomicLong lastUseTime = new AtomicLong(0);

    public PastaEntry( String name, Set<String> keywords, String filename, long timeout ) {
        this.name = name;
        this.keywords = keywords;
        this.filename = filename;
        this.timeout = timeout;
    }

    public String getName() {
        return name;
    }

    public Set<String> getKeywords() {
        return keywords;
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
