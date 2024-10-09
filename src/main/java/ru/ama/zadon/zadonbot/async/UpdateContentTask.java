package ru.ama.zadon.zadonbot.async;

import ru.ama.zadon.zadonbot.Utils;
import ru.ama.zadon.zadonbot.content.ContentConfig;
import ru.ama.zadon.zadonbot.content.ContentEntry;
import ru.ama.zadon.zadonbot.content.ContentType;
import ru.ama.zadon.zadonbot.yaml.YamlParser;
import ru.ama.zadon.zadonbot.yaml.content.ContentConfigYaml;
import ru.ama.zadon.zadonbot.yaml.content.ContentEntryYaml;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UpdateContentTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger( UpdateContentTask.class );

    private final String configFilename;
    private final String configPath;
    private final Consumer<ContentConfig> updatedConfigConsumer;
    private final Supplier<Boolean> configFilesUpdater;
    private boolean isInitialized = false;

    public UpdateContentTask( String configFilename,
                              Consumer<ContentConfig> updatedConfigConsumer,
                              Supplier<Boolean> configFilesUpdater ) {
        this.configFilename = configFilename;
        this.configPath = getConfigPath( configFilename );
        this.updatedConfigConsumer = updatedConfigConsumer;
        this.configFilesUpdater = configFilesUpdater;
    }

    private static String getConfigPath( String configFilename ) {
        return Paths.get( configFilename ).toAbsolutePath().getParent().toString();
    }

    @Override
    public void run() {
        if ( configFilesUpdater.get() || !isInitialized ) {
            updatedConfigConsumer.accept( updateConfig() );
            isInitialized = true;
            LOGGER.debug( "Content config updated" );
        }
    }

    private ContentConfig updateConfig() {
        LOGGER.debug( "Updating content config" );
        ContentConfigYaml contentConfigYaml = YamlParser.parseConfig( configFilename );
        Map<String, ContentEntry> promptedContentEntries = new HashMap<>();
        List<ContentEntry> allContentEntries = new ArrayList<>();
        for ( ContentEntryYaml contentEntryYaml : contentConfigYaml.getContent().getList() ) {
            ContentEntry contentEntry = getContentEntry( contentEntryYaml );
            if ( contentEntry != null ) {
                allContentEntries.add( contentEntry );
                if (contentEntry.getPrompt() != null)
                    promptedContentEntries.put( contentEntry.getPrompt(), contentEntry );
            }
        }
        return new ContentConfig( promptedContentEntries, allContentEntries );
    }

    @Nullable
    private ContentEntry getContentEntry( ContentEntryYaml contentEntryYaml ) {
        if ( contentEntryYaml.getPrompt() == null && contentEntryYaml.getKeywords() == null ) {
            LOGGER.error( "Content entry with name {} has no prompt or keywords and will never get triggered",
                          contentEntryYaml.getName() );
            return null;
        }
        return new ContentEntry( contentEntryYaml.getName(), ContentType.valueOf( contentEntryYaml.getType() ),
                                 contentEntryYaml.getPrompt(), contentEntryYaml.getKeywords(),
                                 contentEntryYaml.getMessage(), Paths.get( configPath, contentEntryYaml.getFilename() ),
                                 Utils.toMilliseconds( contentEntryYaml.getTimeout() ) );
    }
}
