package org.example;

public class MatchedRecord {
    private ESPRecord espRecord;
    private FlixBusRecord flixBusRecord;

    public MatchedRecord(ESPRecord espRecord, FlixBusRecord flixBusRecord) {
        this.espRecord = espRecord;
        this.flixBusRecord = flixBusRecord;
    }

    public ESPRecord getEspRecord() {
        return espRecord;
    }

    public FlixBusRecord getFlixBusRecord() {
        return flixBusRecord;
    }
}