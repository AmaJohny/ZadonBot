package ru.ama.zadon.zadonbot.git;

import ru.ama.zadon.zadonbot.Utils;
import ru.ama.zadon.zadonbot.async.UpdateContentTask;
import ru.ama.zadon.zadonbot.content.ContentConfig;
import ru.ama.zadon.zadonbot.content.ContentConfigProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GitContentConfigProvider implements ContentConfigProvider {

    private final GitProperties gitProperties;
    private final String configFile;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1 );

    private volatile ContentConfig contentConfig;

    public GitContentConfigProvider( GitProperties gitProperties, String configFile ) {
        this.gitProperties = gitProperties;
        this.configFile = configFile;
    }

    public void init() {
        UpdateConfigFiles filesUpdater = new UpdateConfigFiles( gitProperties );
        UpdateContentTask updateContentTask = new UpdateContentTask( configFile,
                                                                     ( config ) -> contentConfig = config,
                                                                     filesUpdater::update );
        updateContentTask.run();
        long period = Utils.toMilliseconds( gitProperties.getConfigUpdatePeriod() );
        scheduler.scheduleAtFixedRate( updateContentTask, period, period, TimeUnit.MILLISECONDS );
    }

    @Override
    public ContentConfig getContentConfig() {
        return contentConfig;
    }

    private static class UpdateConfigFiles {

        private static final Logger LOGGER = LoggerFactory.getLogger( UpdateConfigFiles.class );

        private final GitProperties gitProperties;

        public UpdateConfigFiles( GitProperties gitProperties ) {
            this.gitProperties = gitProperties;
        }

        public boolean update() {
            boolean needUpdate = false;
            Path repoDir = Paths.get( gitProperties.getRepoDir() );
            if ( Files.exists( repoDir ) && Files.exists( Paths.get( gitProperties.getRepoDir(), ".git" ) ) ) {
                needUpdate = pullAndUpdate();
            } else {
                cloneRepo( repoDir );
                needUpdate = true;
            }
            return needUpdate;
        }

        private void cloneRepo( Path repoDir ) {
            LOGGER.debug( "Cloning repo in folder: {}", repoDir );
            try {
                Files.createDirectories( repoDir );
                GitActions.cloneRepo( gitProperties.getRepoDir(), gitProperties.getRepoUrl(), gitProperties.getToken() );
            } catch ( IOException e ) {
                LOGGER.error( "Error while creating repo", e );
            }
        }

        private boolean pullAndUpdate() {
            LOGGER.debug( "Repo exists, pulling updates" );
            boolean upToDate = false;
            try {
                upToDate = GitActions.pullFromRepo( gitProperties.getRepoDir() );
            } catch ( IOException e ) {
                LOGGER.error( "Error while pulling from repo", e );
            }
            if ( !upToDate ) {
                LOGGER.debug( "Repo updated" );
            } else {
                LOGGER.debug( "Repo is up to date" );
            }
            return upToDate;
        }
    }
}
