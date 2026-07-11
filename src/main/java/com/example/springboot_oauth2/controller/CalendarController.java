package com.example.springboot_oauth2.controller;

import com.example.springboot_oauth2.response.CalendarListResponse;
import com.example.springboot_oauth2.response.EventsResponse;
import com.example.springboot_oauth2.service.GoogleCalendarService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Controller
public class CalendarController {

    private final GoogleCalendarService calendarService;

    public CalendarController(GoogleCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /**
     * Access token lấy từ OAuth2AuthorizedClient do Spring quản lý.
     * Nếu chưa ủy quyền -> Spring tự redirect sang Google rồi quay lại /calendars.
     * Nếu access token hết hạn và có refresh token -> Spring tự refresh (trong suốt).
     */
    @GetMapping("/calendars")
    public String calendars(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
            Model model) {
        String accessToken = client.getAccessToken().getTokenValue();

        try {
            CalendarListResponse calendars = calendarService.listCalendars(accessToken);
            model.addAttribute("calendars", calendars.items());
        } catch (RestClientResponseException ex) {
            // In nguyên body lỗi của Google ra console để biết chính xác lý do (vd: API chưa Enable)
            System.err.println(">>> Calendar API lỗi " + ex.getStatusCode().value()
                    + " khi lấy danh sách lịch:\n" + ex.getResponseBodyAsString());
            model.addAttribute("error", "Google Calendar API trả về lỗi "
                    + ex.getStatusCode().value() + " khi lấy danh sách lịch:\n"
                    + ex.getResponseBodyAsString());
            return "error-page";
        }

        return "calendars";
    }

    @GetMapping("/events")
    public String events(
            @RequestParam String calendarId,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
            Model model) {
        String accessToken = client.getAccessToken().getTokenValue();

        // timeMin = thời điểm hiện tại (dạng RFC3339, vd 2026-07-08T14:00:00Z)
        String now = Instant.now().toString();

        EventsResponse events;
        try {
            events = calendarService.listUpcomingEvents(accessToken, calendarId, now);
        } catch (RestClientResponseException ex) {
            System.err.println(">>> Calendar API lỗi " + ex.getStatusCode().value()
                    + " khi lấy sự kiện của lịch " + calendarId + ":\n" + ex.getResponseBodyAsString());
            model.addAttribute("error", "Google Calendar API trả về lỗi "
                    + ex.getStatusCode().value()
                    + " khi lấy sự kiện của lịch: " + calendarId + "\n"
                    + ex.getResponseBodyAsString());
            return "error-page";
        }

        model.addAttribute("calendarId", calendarId);
        model.addAttribute("events", events.items());

        return "events";
    }
}
