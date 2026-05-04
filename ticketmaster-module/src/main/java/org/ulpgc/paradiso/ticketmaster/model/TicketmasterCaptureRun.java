package org.ulpgc.paradiso.ticketmaster.model;

public class TicketmasterCaptureRun {

    private String captureBatchId;
    private String startedAt;
    private String finishedAt;
    private String status;
    private String scopeSummary;
    private int recordsFetched;
    private int recordsInserted;
    private String errorMessage;

    public String getCaptureBatchId() { return captureBatchId; }
    public void setCaptureBatchId(String v) { this.captureBatchId = v; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String v) { this.startedAt = v; }

    public String getFinishedAt() { return finishedAt; }
    public void setFinishedAt(String v) { this.finishedAt = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getScopeSummary() { return scopeSummary; }
    public void setScopeSummary(String v) { this.scopeSummary = v; }

    public int getRecordsFetched() { return recordsFetched; }
    public void setRecordsFetched(int v) { this.recordsFetched = v; }

    public int getRecordsInserted() { return recordsInserted; }
    public void setRecordsInserted(int v) { this.recordsInserted = v; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
}