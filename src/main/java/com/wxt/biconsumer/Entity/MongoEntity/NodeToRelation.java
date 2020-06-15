package com.wxt.biconsumer.Entity.MongoEntity;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Objects;

public class NodeToRelation{
    @JSONField(name = "u")
    private Integer uniqueId = null;
    @JSONField(name = "n")
    private String node;
    @JSONField(name = "r")
    private String relation;
    @JSONField(name = "d")
    private int direction;

    public NodeToRelation() { }

    public NodeToRelation(String node, String relation, int direction) {
        this.node = node;
        this.relation = relation;
        this.direction = direction;
    }

    public NodeToRelation(int uniqueId, String node, String relation, int direction) {
        this.uniqueId = uniqueId;
        this.node = node;
        this.relation = relation;
        this.direction = direction;
    }

    public Integer getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(Integer uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeToRelation that = (NodeToRelation) o;
        return direction == that.direction &&
                node.equals(that.node) &&
                relation.equals(that.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, relation, direction);
    }
}