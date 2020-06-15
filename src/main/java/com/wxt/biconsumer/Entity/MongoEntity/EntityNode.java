package com.wxt.biconsumer.Entity.MongoEntity;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.*;

public class EntityNode {

    @JSONField(deserialize = false)
    private String _id;

    private String name;

    private int uniqueId;

    private Set<NodeToRelation> links;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<NodeToRelation> getLinks() {
        return links;
    }

    public void setLinks(List<String> nodes, List<String> relations, List<Integer> directions) {
        assert nodes.size() == relations.size();
        this.links = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            links.add(new NodeToRelation(nodes.get(i), relations.get(i), directions.get(i)));
        }
    }

    public void addLinks(Set<NodeToRelation> set){
        if(this.links == null){
            this.links = new HashSet<>();
        }
        this.links.addAll(set);
    }

    public void addLink(String node, String relation, int direction){
        if(this.links == null){
            this.links = new HashSet<>();
        }
        this.links.add(new NodeToRelation(node, relation, direction));
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityNode that = (EntityNode) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(links, that.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, links);
    }
}
