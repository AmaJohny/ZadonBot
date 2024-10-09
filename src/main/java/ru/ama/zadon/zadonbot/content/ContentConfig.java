package ru.ama.zadon.zadonbot.content;

import java.util.List;
import java.util.Map;

public record ContentConfig( Map<String, ContentEntry> promptedContents, List<ContentEntry> allContents ) {

}
