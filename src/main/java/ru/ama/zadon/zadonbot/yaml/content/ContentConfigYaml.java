package ru.ama.zadon.zadonbot.yaml.content;

import ru.ama.zadon.zadonbot.yaml.content.pasta.PastasYaml;
import ru.ama.zadon.zadonbot.yaml.content.pic.PicsYaml;

public class ContentConfigYaml {

    public PastasYaml pastas;
    public PicsYaml pictures;

    public PastasYaml getPastas() {
        return pastas;
    }

    public void setPastas( PastasYaml pastas ) {
        this.pastas = pastas;
    }

    public PicsYaml getPictures() {
        return pictures;
    }

    public void setPictures( PicsYaml pictures ) {
        this.pictures = pictures;
    }
}
