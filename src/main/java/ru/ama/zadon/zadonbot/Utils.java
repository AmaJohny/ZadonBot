package ru.ama.zadon.zadonbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class Utils {

    public static Logger LOGGER = LoggerFactory.getLogger( Utils.class );

    public static final String FILE_ID_FILE_SUFFIX = ".fid";

    public static long toMilliseconds( String strDuration ) {
        if ( strDuration == null )
            return -1;
        strDuration = strDuration.replaceAll( "\\s+", "" ).replaceFirst( "(\\d+d)", "P$1T" );
        if ( strDuration.charAt( 0 ) != 'P' ) {
            strDuration = "PT" + strDuration;
        } else if ( strDuration.endsWith( "T" ) )
            strDuration = strDuration.substring( 0, strDuration.length() - 1 );
        Duration duration = Duration.parse( strDuration );
        return duration.getSeconds() * 1000L;
    }

    public static String getFileContent( Path filepath ) throws IOException {
        if ( filepath == null || !Files.exists( filepath ) )
            return null;
        return Files.readString( filepath, StandardCharsets.UTF_8 );
    }

    public static void setFileContent( Path filepath, String content ) throws IOException {
        Files.writeString( filepath, content, StandardCharsets.UTF_8 );
    }

    public static Path getFileIdFilepath( Path filepath ) {
        if ( filepath == null )
            return null;
        return Paths.get( filepath + FILE_ID_FILE_SUFFIX );
    }

    public static double toProbability( String stringProbability, String entryName ) {
        double probability;
        if ( !StringUtils.hasText( stringProbability ) ) {
            probability = 1;
        } else if ( stringProbability.trim().endsWith( "%" ) ) {
            stringProbability = stringProbability.substring( 0, stringProbability.length() - 1 );
            probability = Float.parseFloat( stringProbability ) / 100f;
        } else {
            probability = Float.parseFloat( stringProbability );
        }
        if ( probability < 0 || probability > 1 ) {
            LOGGER.warn( "Probability value out of range [0,1]: {} for entry {}", stringProbability, entryName );
            probability = 1;
        }
        return probability;
    }

    public static String toPercents( double probability ) {
        return Math.round( probability * 100.0 ) + "%";
    }
}
