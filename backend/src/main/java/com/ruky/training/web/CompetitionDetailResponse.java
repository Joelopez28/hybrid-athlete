package com.ruky.training.web;

import java.util.List;

public record CompetitionDetailResponse(
        CompetitionSummaryResponse competition,
        List<CompetitionDivisionResponse> divisions,
        List<CompetitionEventResponse> events,
        CompetitionRegistrationResponse myRegistration,
        List<MyResultResponse> myResults,
        List<LeaderboardResponse> leaderboards) {

    public CompetitionDetailResponse {
        divisions = List.copyOf(divisions);
        events = List.copyOf(events);
        myResults = List.copyOf(myResults);
        leaderboards = List.copyOf(leaderboards);
    }
}
