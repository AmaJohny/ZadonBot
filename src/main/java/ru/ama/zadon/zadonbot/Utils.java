package ru.ama.zadon.zadonbot;

import java.time.Duration;

public class Utils {

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

}
