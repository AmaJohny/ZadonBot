package ru.ama.zadon.zadonbot.git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class GitActions {

    private static final Logger LOGGER = LoggerFactory.getLogger( GitActions.class );

    private static final String UP_TO_DATE_MESSAGE = "Already up to date.";

    public static boolean pullFromRepo( String dir ) throws IOException {
        // Вызов скрипта, который пулит данные
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory( new File( dir ) );
        pb.command( "git", "pull" );
        pb.redirectErrorStream(true);
        LOGGER.debug( "Starting command: {}", pb.command() );
        Process process = pb.start();
        try {
            process.waitFor();
        } catch ( InterruptedException e ) {
            throw new RuntimeException( e );
        }
        BufferedReader processResultReader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
        String line;
        while ( (line = processResultReader.readLine()) != null ) {
            LOGGER.debug( "Result string: {}", line );
            if ( line.contains( UP_TO_DATE_MESSAGE ) )
                return true;
        }
        return false;
    }

    public static void cloneRepo( String dir, String repoUrl, String accessToken ) throws IOException {
        // Вызов скрипта, который пулит данные
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory( new File( dir ) );
        pb.command( "git", "clone", repoUrl.replace( "{token}", accessToken ), "." );
        pb.redirectErrorStream(true);
        LOGGER.debug( "Starting command: {}", pb.command() );
        Process process = pb.start();
        try {
            process.waitFor();
        } catch ( InterruptedException e ) {
            throw new RuntimeException( e );
        }
        BufferedReader processResultReader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
        String line;
        while ( (line = processResultReader.readLine()) != null ) {
            LOGGER.debug( "Result string: {}", line );
        }
    }
}
