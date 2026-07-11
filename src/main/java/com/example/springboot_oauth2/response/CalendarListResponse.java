package com.example.springboot_oauth2.response;

import java.util.List;

public record CalendarListResponse (
    List<CalendarItem> items
){
    public record CalendarItem (
            String id,
            String summary, // Tên lịch
            boolean primary // Có phải lịch chính không?
    ){

    }

}
