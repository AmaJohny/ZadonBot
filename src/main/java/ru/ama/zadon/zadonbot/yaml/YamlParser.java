package ru.ama.zadon.zadonbot.yaml;

import ru.ama.zadon.zadonbot.yaml.content.ContentConfigYaml;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class YamlParser {

    public static ContentConfigYaml parseConfig( String filename ) {
        return readYaml( filename, ContentConfigYaml.class ).getFirst();
    }

    private static <T> List<T> readYaml( String fileName, Class<T> contentClass ) {
        List<T> parsedContents = new ArrayList<>();

        Yaml yaml = new Yaml( new Constructor( contentClass, new LoaderOptions() ) );
        try ( var fileInputStream = new FileInputStream( fileName ) ) {
            for ( Object obj : yaml.loadAll( fileInputStream ) ) {
                T parsedContent = (T) obj;
                parsedContents.add( parsedContent );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( "Unable to read file: " + fileName, e );
        }

        return parsedContents;
    }
}
