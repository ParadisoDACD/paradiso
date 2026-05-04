package org.ulpgc.paradiso.tfl.model;

public class TflJourney {

    private String journeyHash;
    private String originName;
    private String destinationName;
    private String startDateTime;
    private String arrivalDateTime;
    private int durationMinutes;
    private int numberOfLegs;
    private String firstLegMode;
    private String captureDate;
    private String captureTime;
    private String sourceOrigin;
    private String sourceDestination;
    private String captureBatchId;
    private String capturedAt;

    public String getJourneyHash() { return journeyHash; }
    public void setJourneyHash(String v) { this.journeyHash = v; }

    public String getOriginName() { return originName; }
    public void setOriginName(String v) { this.originName = v; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String v) { this.destinationName = v; }

    public String getStartDateTime() { return startDateTime; }
    public void setStartDateTime(String v) { this.startDateTime = v; }

    public String getArrivalDateTime() { return arrivalDateTime; }
    public void setArrivalDateTime(String v) { this.arrivalDateTime = v; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int v) { this.durationMinutes = v; }

    public int getNumberOfLegs() { return numberOfLegs; }
    public void setNumberOfLegs(int v) { this.numberOfLegs = v; }

    public String getFirstLegMode() { return firstLegMode; }
    public void setFirstLegMode(String v) { this.firstLegMode = v; }

    public String getCaptureDate() { return captureDate; }
    public void setCaptureDate(String v) { this.captureDate = v; }

    public String getCaptureTime() { return captureTime; }
    public void setCaptureTime(String v) { this.captureTime = v; }

    public String getSourceOrigin() { return sourceOrigin; }
    public void setSourceOrigin(String v) { this.sourceOrigin = v; }

    public String getSourceDestination() { return sourceDestination; }
    public void setSourceDestination(String v) { this.sourceDestination = v; }

    public String getCaptureBatchId() { return captureBatchId; }
    public void setCaptureBatchId(String v) { this.captureBatchId = v; }

    public String getCapturedAt() { return capturedAt; }
    public void setCapturedAt(String v) { this.capturedAt = v; }
}