package org.opennms.plugins.cloud.srv.appliance.cloud.api.entities;

import java.util.ArrayList;
import java.util.List;

public class PaginatedResponse<T> {
    private int totalRecords = 0;
    private List<T> pagedRecords = new ArrayList<>();

    public PaginatedResponse() {
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public List<T> getPagedRecords() {
        return pagedRecords;
    }

    public void setPagedRecords(List<T> pagedRecords) {
        this.pagedRecords = pagedRecords;
    }
}
