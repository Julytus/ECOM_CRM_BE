package vn.aeoc.entity.data.mysql;

import lombok.Data;

@Data
public class Resource {
    private Integer id;

    private String name;
    private String description;

    private String code;

    private String uri;
    private String actions;

    public Resource() {

    }

    public Resource(Resource p) {
        this.id = p.id;
        this.name = p.name;
        this.description = p.description;
        this.code = p.code;
        this.uri = p.uri;
        this.actions = p.actions;
    }
}
