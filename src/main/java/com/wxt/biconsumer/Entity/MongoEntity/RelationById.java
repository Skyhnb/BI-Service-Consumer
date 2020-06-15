package com.wxt.biconsumer.Entity.MongoEntity;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;
import java.util.Objects;

public class RelationById implements Serializable {
    @JSONField(name = "s")
    private int startUniqueId;
    @JSONField(name = "e")
    private int endUniqueId;
    @JSONField(name = "r")
    private String relation;

    public RelationById(int startUniqueId, int endUniqueId, String relation) {
        this.startUniqueId = startUniqueId;
        this.endUniqueId = endUniqueId;
        this.relation = relation;
    }

    public RelationById() { }

    public int getStartUniqueId() {
        return startUniqueId;
    }

    public void setStartUniqueId(int startUniqueId) {
        this.startUniqueId = startUniqueId;
    }

    public int getEndUniqueId() {
        return endUniqueId;
    }

    public void setEndUniqueId(int endUniqueId) {
        this.endUniqueId = endUniqueId;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationById that = (RelationById) o;
        return startUniqueId == that.startUniqueId &&
                endUniqueId == that.endUniqueId &&
                relation.equals(that.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startUniqueId, endUniqueId, relation);
    }
}
