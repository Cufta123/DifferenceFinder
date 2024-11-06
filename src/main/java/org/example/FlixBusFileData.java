
package org.example;

import java.util.List;

public class FlixBusFileData {
    private List<FlixBusRecord> records;
    private List<FlixBusRecord> feeRecords;

    public FlixBusFileData(List<FlixBusRecord> records, List<FlixBusRecord> feeRecords) {
        this.records = records;
        this.feeRecords = feeRecords;
    }

    public List<FlixBusRecord> getRecords() {
        return records;
    }

    public List<FlixBusRecord> getFeeRecords() {
        return feeRecords;
    }
}