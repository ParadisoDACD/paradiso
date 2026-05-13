package org.ulpgc.paradiso.ticketmaster.model;

public class TicketmasterEvent {

    private String externalEventId;
    private String name;
    private String classificationName;
    private String segment;
    private String genre;
    private String city;
    private String countryCode;
    private String venueName;
    private String eventUrl;
    private String localDate;
    private String localTime;
    private String dateTimeIso;
    private String sourceCountry;
    private String sourceCity;
    private String sourceCategory;
    private String captureBatchId;
    private String capturedAt;

    public String getExternalEventId() { return externalEventId; }
    public void setExternalEventId(String v) { this.externalEventId = v; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public String getClassificationName() { return classificationName; }
    public void setClassificationName(String v) { this.classificationName = v; }

    public String getSegment() { return segment; }
    public void setSegment(String v) { this.segment = v; }

    public String getGenre() { return genre; }
    public void setGenre(String v) { this.genre = v; }

    public String getCity() { return city; }
    public void setCity(String v) { this.city = v; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String v) { this.countryCode = v; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String v) { this.venueName = v; }

    public String getEventUrl() { return eventUrl; }
    public void setEventUrl(String v) { this.eventUrl = v; }

    public String getLocalDate() { return localDate; }
    public void setLocalDate(String v) { this.localDate = v; }

    public String getLocalTime() { return localTime; }
    public void setLocalTime(String v) { this.localTime = v; }

    public String getDateTimeIso() { return dateTimeIso; }
    public void setDateTimeIso(String v) { this.dateTimeIso = v; }

    public String getSourceCountry() { return sourceCountry; }
    public void setSourceCountry(String v) { this.sourceCountry = v; }

    public String getSourceCity() { return sourceCity; }
    public void setSourceCity(String v) { this.sourceCity = v; }

    public String getSourceCategory() { return sourceCategory; }
    public void setSourceCategory(String v) { this.sourceCategory = v; }

    public String getCaptureBatchId() { return captureBatchId; }
    public void setCaptureBatchId(String v) { this.captureBatchId = v; }

    public String getCapturedAt() { return capturedAt; }
    public void setCapturedAt(String v) { this.capturedAt = v; }
}