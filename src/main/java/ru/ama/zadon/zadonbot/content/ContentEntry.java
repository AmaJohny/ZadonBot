package ru.ama.zadon.zadonbot.content;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class ContentEntry {

    private final String name;
    private final ContentType type;
    private final String prompt;
    private final Set<String> keywords;
    private final String message;
    private final Path filepath;
    private final long timeout;

    private final AtomicLong lastUseTime = new AtomicLong( 0 );

    private volatile String fileId;

    public ContentEntry( @Nonnull String name, @Nonnull ContentType type,
                         @Nullable String prompt, @Nullable Set<String> keywords,
                         @Nullable String message, @Nullable Path filepath,
                         long timeout ) {
        this.name = name;
        this.type = type;
        this.prompt = prompt;
        this.keywords = keywords;
        this.message = message;
        this.filepath = filepath;
        this.timeout = timeout;
    }

    public String getName() {
        return name;
    }

    public ContentType getType() {
        return type;
    }

    public String getPrompt() {
        return prompt;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public String getMessage() {
        return message;
    }

    public Path getFilepath() {
        return filepath;
    }

    public boolean onTimeout() {
        if ( timeout < 0L )
            return false;

        long lastPastedTimeLong = lastUseTime.get();
        long currentTime = System.currentTimeMillis();
        return !(currentTime > lastPastedTimeLong + timeout &&
                 lastUseTime.compareAndSet( lastPastedTimeLong, currentTime ));
    }

    public long getTimeout() {
        return timeout;
    }

    public AtomicLong getLastUseTime() {
        return lastUseTime;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId( String fileId ) {
        this.fileId = fileId;
    }
}
