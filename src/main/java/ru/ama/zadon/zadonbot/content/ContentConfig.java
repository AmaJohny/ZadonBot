package ru.ama.zadon.zadonbot.content;

import ru.ama.zadon.zadonbot.content.pasta.PastaEntry;
import ru.ama.zadon.zadonbot.content.pic.PicEntry;

import java.util.List;

public class ContentConfig {

    private final List<PastaEntry> pastas;
    private final List<PicEntry> pictures;

    public ContentConfig( List<PastaEntry> pastas, List<PicEntry> pictures ) {
        this.pastas = pastas;
        this.pictures = pictures;
    }

    public List<PastaEntry> getPastas() {
        return pastas;
    }


    public List<PicEntry> getPictures() {
        return pictures;
    }

}
