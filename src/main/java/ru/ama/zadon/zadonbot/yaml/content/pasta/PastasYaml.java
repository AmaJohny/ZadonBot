package ru.ama.zadon.zadonbot.yaml.content.pasta;

import java.util.List;

public class PastasYaml {

    public String location;
    public List<PastaYaml> list;

    public String getLocation() {
        return location;
    }

    public void setLocation( String location ) {
        this.location = location;
    }

    public List<PastaYaml> getList() {
        return list;
    }

    public void setList( List<PastaYaml> list ) {
        this.list = list;
    }
}
