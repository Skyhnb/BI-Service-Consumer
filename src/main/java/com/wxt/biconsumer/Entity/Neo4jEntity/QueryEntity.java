package com.wxt.biconsumer.Entity.Neo4jEntity;

public class QueryEntity {
    private Integer start;          //起点UniqueId
    private Integer end;            //终点UniqueId
    private Integer skip_num;       //跳过的边数
    private Integer limit_num;      //返回的边数
    private Integer max_jump_num;   //最大跳数
    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    public Integer getSkip_num() {
        return skip_num;
    }

    public void setSkip_num(Integer skip_num) {
        this.skip_num = skip_num;
    }

    public Integer getLimit_num() {
        return limit_num;
    }

    public void setLimit_num(Integer limit_num) {
        this.limit_num = limit_num;
    }

    public Integer getMax_jump_num() {
        return max_jump_num;
    }

    public void setMax_jump_num(Integer max_jump_num) {
        this.max_jump_num = max_jump_num;
    }

    public QueryEntity() {}

    @Override
    public String toString() {
        return "QueryEntity{" +
                "start=" + start +
                ", end=" + end +
                ", skip_num=" + skip_num +
                ", limit_num=" + limit_num +
                ", max_jump_num=" + max_jump_num +
                '}';
    }

    public QueryEntity(Integer start, Integer end, Integer skip_num, Integer limit_num, Integer max_jump_num) {
        this.start = start;
        this.end = end;
        this.skip_num = skip_num;
        this.limit_num = limit_num;
        this.max_jump_num = max_jump_num;
    }
}