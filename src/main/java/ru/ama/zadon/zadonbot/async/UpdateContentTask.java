package ru.ama.zadon.zadonbot.async;

import ru.ama.zadon.zadonbot.Utils;
import ru.ama.zadon.zadonbot.content.ContentConfig;
import ru.ama.zadon.zadonbot.content.pasta.PastaEntry;
import ru.ama.zadon.zadonbot.content.pic.PicEntry;
import ru.ama.zadon.zadonbot.git.GitActions;
import ru.ama.zadon.zadonbot.git.GitProperties;
import ru.ama.zadon.zadonbot.yaml.YamlParser;
import ru.ama.zadon.zadonbot.yaml.content.ContentConfigYaml;
import ru.ama.zadon.zadonbot.yaml.content.pasta.PastasYaml;
import ru.ama.zadon.zadonbot.yaml.content.pic.PicsYaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

public class UpdateContentTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger( UpdateContentTask.class );

    private final GitProperties gitProperties;
    private final String configFilename;
    private final Consumer<ContentConfig> updateConfigAction;
    private boolean initContent = true;

    public UpdateContentTask( GitProperties gitProperties, String configFilename,
                              Consumer<ContentConfig> updateConfigAction ) {
        this.gitProperties = gitProperties;
        this.configFilename = configFilename;
        this.updateConfigAction = updateConfigAction;
    }

    @Override
    public void run() {
        Path repoDir = Paths.get( gitProperties.getRepoDir() );
        if ( Files.exists( repoDir ) && Files.exists( Paths.get( gitProperties.getRepoDir(), ".git" ) ) ) {
            LOGGER.debug( "Directory exists: {}", repoDir );
            pullAndUpdate();
        } else {
            LOGGER.debug( "Directory not exists: {}", repoDir );
            cloneAndUpdate( repoDir );
        }
    }

    private void cloneAndUpdate( Path repoDir ) {
        try {
            Files.createDirectories( repoDir );
            GitActions.cloneRepo( gitProperties.getRepoDir(), gitProperties.getRepoUrl(), gitProperties.getToken() );
        } catch ( IOException e ) {
            LOGGER.error( "Error while creating repo", e );
        }
        updateConfig();
    }

    private void pullAndUpdate() {
        boolean upToDate = false;
        try {
            upToDate = GitActions.pullFromRepo( gitProperties.getRepoDir() );
        } catch ( IOException e ) {
            LOGGER.error( "Error while pulling from repo", e );
        }
        LOGGER.debug( "UpToDate: " + upToDate );
        if ( !upToDate || initContent ) {
            updateConfig();
        }
    }

    private void updateConfig() {
        initContent = false;
        ContentConfigYaml contentConfigYaml = YamlParser.parseConfig( configFilename );
        ContentConfig config = new ContentConfig( getPastas( contentConfigYaml.pastas ),
                                                  getPics( contentConfigYaml.pictures ) );
        updateConfigAction.accept( config );
    }

    private List<PastaEntry> getPastas( PastasYaml pastasYaml ) {
        String fileLocation = Paths.get( gitProperties.getRepoDir(), pastasYaml.location ).toAbsolutePath().toString();
        return pastasYaml.list.stream()
                              .map( (pastaYaml -> new PastaEntry( pastaYaml.getName(),
                                                                  pastaYaml.getKeywords(),
                                                                  Paths.get( fileLocation, pastaYaml.getFilename() )
                                                                       .toAbsolutePath().toString(),
                                                                  Utils.toMilliseconds( pastaYaml.getTimeout() ) )) )
                              .toList();
    }

    private List<PicEntry> getPics( PicsYaml picsYaml ) {
        String fileLocation = Paths.get( gitProperties.getRepoDir(), picsYaml.location ).toAbsolutePath().toString();
        return picsYaml.list.stream()
                            .map( (picYaml -> new PicEntry( picYaml.getName(),
                                                            picYaml.getPrompt(),
                                                            picYaml.getMessage(),
                                                            Paths.get( fileLocation, picYaml.getFilename() )
                                                                 .toAbsolutePath().toString(),
                                                            Utils.toMilliseconds( picYaml.getTimeout() ) )) )
                            .toList();
    }
}
