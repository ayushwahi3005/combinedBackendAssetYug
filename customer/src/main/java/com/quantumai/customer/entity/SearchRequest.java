package com.quantumai.customer.entity;

public class SearchRequest {
    private Object filter;

    public Object getFilter() {
        return filter;
    }

    public void setFilter(Object filter) {
        this.filter = filter;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getSearchData() {
        return searchData;
    }

    public void setSearchData(String searchData) {
        this.searchData = searchData;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    private Integer pageNumber;
    private Integer pageSize;
    private String searchData;
    private String category;



}