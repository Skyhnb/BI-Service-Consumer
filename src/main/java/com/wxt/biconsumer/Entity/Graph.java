package com.wxt.biconsumer.Entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.wxt.biconsumer.Entity.MongoEntity.RelationById;

import java.util.*;

public class Graph {
    @JSONField(name = "nodes")
    private Map<Integer, Node> nodes;


    public Map<Integer, Node> getNodes() {
        return nodes;
    }

    public void setNodes(Map<Integer, Node> nodes) {
        this.nodes = nodes;
    }

    private Graph() {}

    public static Graph generateGraph(Map<Integer, String> idToNames, Map<Integer, Set<RelationById>> relations){
        Graph g = new Graph();
        g.nodes = new HashMap<>(idToNames.size());
        idToNames.forEach((id, name) -> {
            Node n = new Node();
            n.name = name;
            n.uniqueId = id;
            g.nodes.put(id, n);
        });
        g.nodes.forEach((id, node) -> {
            relations.computeIfPresent(id, (k, v) -> {
                if(node.links == null){
                    node.links = new HashSet<>(16);
                }
                v.forEach(rbi -> {
                    Link link = new Link();
                    int direction, otherId;
                    if(rbi.getStartUniqueId() == id){
                        direction = 1;
                        otherId = rbi.getEndUniqueId();
                    }else{
                        direction = -1;
                        otherId = rbi.getStartUniqueId();
                    }
                    link.setDirection(direction);
                    link.setLinkedUniqueId(otherId);
                    node.links.add(link);
                });
                return v;
            });
        });
        return g;
    }

    public static class Node{
        private String name;
        private Integer uniqueId;
        private Set<Link> links;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getUniqueId() {
            return uniqueId;
        }

        public void setUniqueId(Integer uniqueId) {
            this.uniqueId = uniqueId;
        }

        public Set<Link> getLinks() {
            return links;
        }

        public void setLinks(Set<Link> links) {
            this.links = links;
        }
    }

    public static class Link{
        @JSONField(name = "d")
        private int direction;
        @JSONField(name = "u")
        private int linkedUniqueId;

        public int getDirection() {
            return direction;
        }

        public void setDirection(int direction) {
            this.direction = direction;
        }

        public int getLinkedUniqueId() {
            return linkedUniqueId;
        }

        public void setLinkedUniqueId(int linkedUniqueId) {
            this.linkedUniqueId = linkedUniqueId;
        }
    }
}
