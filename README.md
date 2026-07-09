# Google Calendar OAuth2 Client Demo with Spring Boot + Thymeleaf

## Mô tả dự án

**Google Calendar OAuth2 Client Demo with Spring Boot + Thymeleaf** là một ứng dụng web server-side được xây dựng nhằm minh họa cơ chế **OAuth2 delegated authorization** thông qua việc kết nối với Google Calendar API . Ứng dụng cho phép người dùng cấp quyền để hệ thống truy cập dữ liệu lịch Google của họ, sau đó backend sử dụng token nhận được để gọi Google Calendar API và hiển thị danh sách calendar cũng như các sự kiện sắp tới.

Dự án này **không nhằm mục tiêu làm đăng nhập Google cho hệ thống nội bộ**, mà tập trung vào đúng bản chất của OAuth2: ứng dụng được người dùng ủy quyền để truy cập tài nguyên được bảo vệ thay cho họ. Vì vậy, đây là một project phù hợp để phân biệt với OIDC, nơi trọng tâm là xác thực danh tính người dùng thông qua ID token .

## Mục tiêu

Mục tiêu của dự án là giúp người học hiểu đầy đủ một luồng OAuth2 hiện đại trong bối cảnh thực tế với Google . Cụ thể, dự án sử dụng **Authorization Code Grant** làm luồng chính, kết hợp **PKCE**, **state**, **access token**, và **refresh token** để vừa đúng chuẩn hiện đại vừa đủ dễ quan sát khi triển khai trong Spring Boot.

Sau khi hoàn thành dự án, người học cần nắm được:
- Vai trò của client application, authorization server và resource server trong OAuth2.
- Cách hoạt động của **Authorization Code flow** từ lúc redirect sang Google cho đến khi nhận token.
- Vai trò của `state` trong việc chống CSRF ở redirect-based flow.
- Vai trò của **PKCE** với `code_verifier` và `code_challenge` để bảo vệ authorization code khỏi bị đánh cắp hoặc tái sử dụng trái phép.
- Cách sử dụng **access token** để gọi Google Calendar API.
- Cách dùng **refresh token** để xin access token mới khi token cũ hết hạn trong trường hợp cần truy cập lâu dài.

## Thành phần bảo mật chính

Dự án sử dụng **Authorization Code Grant** vì đây là luồng phù hợp cho ứng dụng web có backend và đang được xem là lựa chọn chuẩn thay cho các flow cũ kém an toàn hơn. Google web server flow cũng dựa trên mô hình này: người dùng được chuyển hướng tới trang consent, Google trả về authorization code, và backend dùng code đó để đổi lấy token.

Dự án có sử dụng **PKCE** với cặp `code_verifier` và `code_challenge`, trong đó `code_challenge_method` nên là `S256` để đảm bảo mức an toàn hiện đại. Theo định hướng OAuth 2.1, PKCE được yêu cầu cho tất cả OAuth clients dùng Authorization Code flow, không còn chỉ là tùy chọn cho public client như cách hiểu cũ.

Ngoài PKCE, dự án còn sử dụng tham số **`state`** trong authorization request để liên kết request và callback response, từ đó giúp giảm rủi ro CSRF hoặc callback giả mạo . Đây là thành phần quan trọng và **không bị thay thế bởi PKCE**, vì `state` và PKCE phục vụ hai mục tiêu bảo vệ khác nhau.

Sau khi exchange code thành công, backend nhận **access token** để gọi Google Calendar API. Nếu ứng dụng yêu cầu truy cập dài hạn mà không bắt người dùng consent lại liên tục, hệ thống có thể nhận **refresh token** bằng cách yêu cầu offline access theo hướng dẫn của Google, từ đó làm mới access token khi cần.

## Luồng hoạt động

Luồng xử lý tổng quát của hệ thống diễn ra như sau:
1. Người dùng truy cập trang chủ và bấm nút **Connect Google Calendar** trên giao diện Thymeleaf.
2. Backend khởi tạo authorization request theo **Authorization Code Grant**, kèm `scope`, `state`, `code_challenge`, `code_challenge_method=S256`, và redirect URI đã đăng ký trước với Google.
3. Trình duyệt được chuyển hướng đến Google để người dùng đăng nhập và cấp quyền truy cập Google Calendar cho ứng dụng .
4. Google chuyển hướng người dùng quay về ứng dụng cùng với authorization code và giá trị state phản hồi tương ứng.
5. Backend kiểm tra `state`, gửi authorization code cùng `code_verifier` tới token endpoint để đổi lấy access token, và có thể kèm refresh token nếu đủ điều kiện offline access.
6. Ứng dụng sử dụng access token để gọi Google Calendar API, ví dụ lấy danh sách calendar hoặc sự kiện sắp tới.
7. Kết quả được render ra giao diện Thymeleaf cho người dùng theo mô hình server-side rendering.

## Phạm vi chức năng

Phiên bản đơn giản của dự án nên bao gồm các chức năng sau:
- Trang chủ hiển thị nút kết nối Google Calendar.
- Khởi động OAuth2 Authorization Code flow với Google.
- Nhận callback sau consent.
- Dùng access token để lấy danh sách calendar của người dùng.
- Hiển thị các sự kiện sắp tới từ calendar `primary`. 

Nếu muốn mở rộng thêm nhưng vẫn giữ đúng tinh thần OAuth2 demo, có thể bổ sung:
- Tạo sự kiện mới.
- Làm mới access token bằng refresh token khi access token hết hạn.
- Hiển thị thời gian hết hạn token hoặc scopes đã được cấp. 

## Token và lưu trữ

Trong dự án này, **access token** là token ngắn hạn dùng để gọi Google Calendar API thay mặt người dùng [web:134]. **Refresh token** là token dài hạn hơn, cho phép ứng dụng xin access token mới mà không bắt người dùng thực hiện lại toàn bộ consent flow mỗi lần token hết hạn.

Ở phiên bản học tập đơn giản, token có thể được quản lý bởi Spring Security OAuth2 Client thông qua authorized client lưu trên server-side session hoặc bộ nhớ ứng dụng, vì mục tiêu chính là hiểu flow chứ chưa cần persistence phức tạp. Frontend Thymeleaf không trực tiếp giữ access token hay refresh token; trình duyệt chỉ đóng vai trò user-agent để thực hiện redirect và duy trì session của ứng dụng .

## Công nghệ sử dụng

Dự án sử dụng:
- Spring Boot để xây dựng ứng dụng web.
- Spring Security OAuth2 Client để triển khai Authorization Code flow với Google .
- Thymeleaf để xây dựng giao diện tối giản và render dữ liệu server-side
- Google Calendar API làm protected resource server 
- Các scope Google Calendar phù hợp như `calendar.readonly` hoặc `calendar.events.readonly` cho bản chỉ đọc, và `calendar.events` nếu muốn tạo sự kiện.

## Ý nghĩa của dự án

Dự án này giúp người học nhìn thấy đầy đủ mối liên hệ giữa **Authorization Code Grant**, **PKCE**, **state**, **access token**, và **refresh token** trong một ứng dụng thực tế sử dụng Google API . Đây là một project phù hợp để học nền tảng OAuth2 hiện đại trước khi chuyển sang các dự án nâng cao hơn như tích hợp nhiều provider, lưu token vào database, incremental scopes, hoặc so sánh trực tiếp với OIDC login flow .