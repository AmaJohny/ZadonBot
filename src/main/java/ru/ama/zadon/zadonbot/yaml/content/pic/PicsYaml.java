package ru.ama.zadon.zadonbot.yaml.content.pic;

import java.util.List;

public class PicsYaml {

    public String location;
    public List<PicYaml> list;

    public String getLocation() {
        return location;
    }

    public void setLocation( String location ) {
        this.location = location;
    }

    public List<PicYaml> getList() {
        return list;
    }

    public void setList( List<PicYaml> list ) {
        this.list = list;
    }
}
