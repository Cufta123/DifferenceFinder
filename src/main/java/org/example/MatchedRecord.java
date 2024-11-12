package org.example;

public class MatchedRecord {
    private ESPRecord espRecord;
    private Record record;

    public MatchedRecord(ESPRecord espRecord, Record record) {
        this.espRecord = espRecord;
        this.record = record;
    }

    public ESPRecord getEspRecord() {
        return espRecord;
    }

    public Record getRecord() {
        return record;
    }
}