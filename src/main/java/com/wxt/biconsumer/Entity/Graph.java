package com.wxt.biconsumer.Entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.wxt.biconsumer.Entity.MongoEntity.RelationById;

import java.util.*;

public class Graph {
    @JSONField(name = "nodes")
    private Map<String, Node> nodes;


    public Map<String, Node> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, Node> nodes) {
        this.nodes = nodes;
    }

    private Graph() {}

    public static Graph generateGraph(Map<Integer, String> idToNames, Map<Integer, Set<RelationById>> relations){
        Graph g = new Graph();
        g.nodes = new HashMap<>(idToNames.size());
        idToNames.forEach((id, name) -> {
            Node n = new Node();
            n.name = name;
            n.links = new HashSet<>(4);
            g.nodes.put(id.toString(), n);
        });
        relations.values().forEach(s -> s.forEach(relationById -> {
            g.nodes.compute(String.valueOf(relationById.getStartUniqueId()), (k, v) -> {
                assert v != null && v.links != null;
                Link link = new Link();
                link.setLinkedUniqueId(relationById.getEndUniqueId());
                link.setRelation(relationById.getRelation());
                link.setName(idToNames.get(relationById.getEndUniqueId()));
                v.links.add(link);
                return v;
            });
        }));
        return g;
    }

    public static class Node{
        @JSONField(name = "n")
        private String name;

        private Set<Link> links;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Set<Link> getLinks() {
            return links;
        }

        public void setLinks(Set<Link> links) {
            this.links = links;
        }
    }

    public static class Link{
        @JSONField(name = "u")
        private int linkedUniqueId;
        @JSONField(name = "n")
        private String name;
        @JSONField(name = "r")
        private String relation;

        public String getRelation() {
            return relation;
        }

        public void setRelation(String relation) {
            this.relation = relation;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getLinkedUniqueId() {
            return linkedUniqueId;
        }

        public void setLinkedUniqueId(int linkedUniqueId) {
            this.linkedUniqueId = linkedUniqueId;
        }
    }


    public static void main(String[] args) {
        Graph g = new Graph();
        g.setNodes(new HashMap<>());
        g.getNodes().put("1", new Node());
        System.out.println(JSON.toJSONString(g));
    }
}
