package ru.ama.zadon.zadonbot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class Utils {

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
}
