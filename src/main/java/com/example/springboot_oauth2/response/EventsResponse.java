package com.example.springboot_oauth2.response;

import java.util.List;

public record EventsResponse (
    List<Event> items
){
    public record Event(
            String summary, // Tiêu đề sự kiện
            EventDateTime start, // Thời gian bắt đầu
            EventDateTime end    // Thời gian kết thúc
    ){}

    public record EventDateTime(
            String dateTime,
            String date
    ) {
        // Hàm tiện tích, lấy giá trị nào không rỗng
        public String display(){
            return dateTime != null ? dateTime : date;
        }
    }
}
