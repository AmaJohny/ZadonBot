package ru.ama.zadon.zadonbot.content;

import ru.ama.zadon.zadonbot.Utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
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
                         @Nullable String message, @Nullable Path filepath, @Nullable String fileId,
                         long timeout ) {
        this.name = name;
        this.type = type;
        this.prompt = prompt;
        this.keywords = keywords;
        this.message = message;
        this.filepath = filepath;
        this.fileId = fileId;
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

    public void saveFileId( String fileId ) {
        assert filepath != null;
            try {
                Utils.setFileContent( Utils.getFileIdFilepath( filepath ), fileId );
                this.fileId = fileId;
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
    }
}
