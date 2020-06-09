package com.wxt.biconsumer.Entity.Neo4jEntity;

public class QueryEntity {
    private Integer start;
    private Integer end;
    private Integer skip_num;
    private Integer limit_num;

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
}