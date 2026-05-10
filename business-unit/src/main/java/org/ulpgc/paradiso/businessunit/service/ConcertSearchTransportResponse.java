package org.ulpgc.paradiso.businessunit.service;

import java.util.List;

public record ConcertSearchTransportResponse(
        String query,
        boolean found,
        int matches,
        List<ConcertTransportResponse> results
) {

    public static ConcertSearchTransportResponse empty(String query) {
        return new ConcertSearchTransportResponse(
                query,
                false,
                0,
                List.of()
        );
    }

    public static ConcertSearchTransportResponse of(String query,
                                                    List<ConcertTransportResponse> results) {
        return new ConcertSearchTransportResponse(
                query,
                !results.isEmpty(),
                results.size(),
                results
        );
    }
}