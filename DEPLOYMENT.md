# Deploy Docker Compose + Cloudflare Tunnel

Luồng: HTTPS domain → Cloudflare → cloudflared → frontend (Nginx:80) → backend-app:8080.
Nginx phục vụ React SPA, chuyển `/api/*` và WebSocket `/ws` sang Spring.
PostgreSQL, Redis, RabbitMQ không mở cổng ra host. Không cần EC2, IP tĩnh,
Certbot, Let's Encrypt hoặc Watchtower. Máy chạy Docker phải luôn bật và có Internet.
Hostinger giữ vai trò đăng ký domain; DNS do Cloudflare quản lý.

Stack dùng project name `roomhub-prod`, tự sinh tên container, network và volume
riêng. Có thể chạy song song với `chargeops` trên máy hiện tại: production không
publish cổng host nên không tranh các cổng 8080, 5432, 6379 đang dùng.
Tạo Tunnel riêng và dùng hostname riêng cho RoomHub; không sửa route/token Tunnel
của dự án kia. Hai stack vẫn dùng chung RAM/CPU/disk của Docker Desktop.
Không chạy cấu hình dev của RoomHub song song với ChargeOps khi chưa đổi cổng dev.

## 1. Chuẩn bị

- Docker Engine/Desktop đang chạy Linux containers và Compose v2.24 trở lên.
- Đặt hai repo cạnh nhau, đúng tên `room-booking` và `room-booking-frontend`.
- Chạy mọi lệnh dưới đây trong thư mục `room-booking`.
- Sao chép `.env.prod.example` thành `.env.prod`, điền mật khẩu và API keys thực.
  Không ghi đè `.env.prod` đang có; đối chiếu và bổ sung biến còn thiếu.
- `PUBLIC_APP_URL=https://roomhub.online` và `PUBLIC_WS_URL=wss://roomhub.online/ws`
  là ví dụ theo domain xuất hiện trong source. Thay bằng hostname thực bạn muốn dùng.
  URL không có dấu `/` cuối; `PUBLIC_APP_URL` không chứa path.
- `GOOGLE_CLIENT_ID` dùng chung cho backend và frontend build. Không có secret nào
  được đưa vào Vite. Các giá trị Vite được chốt lúc build, đổi URL/Google ID phải rebuild.
- Đặt RSA keys hiện có vào `certs/private.pem` và `certs/public.pem`.
  Nếu cài mới, tạo bằng OpenSSL (không chạy đè keys hiện có):

```sh
mkdir -p certs
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out certs/private.pem
openssl pkey -in certs/private.pem -pubout -out certs/public.pem
```

Trong Google Cloud OAuth client, thêm Authorized JavaScript origin đúng
`PUBLIC_APP_URL`, và Authorized redirect URI `PUBLIC_APP_URL/auth/google-callback`.
Ví dụ redirect là `https://roomhub.online/auth/google-callback`.

## 2. Cloudflare và DNS

1. Xác nhận domain trên Cloudflare có trạng thái Active và nameserver tại Hostinger
   đã trỏ đúng nameserver Cloudflare cấp.
2. Trong Cloudflare One, tạo Cloudflare Tunnel loại cloudflared, chọn Docker.
   Lấy tunnel token điền vào `CLOUDFLARE_TUNNEL_TOKEN` trong `.env.prod`.
3. Thêm Published application route với hostname trùng `PUBLIC_APP_URL`:
   Service type **HTTP**, URL **frontend:80**, không đặt path để toàn bộ site đi qua Nginx.
   Không dùng `localhost` vì connector chạy trong container riêng.
4. Nếu hostname còn A/AAAA/CNAME trỏ EC2 hoặc Vercel, thay record xung đột bằng
   record do Tunnel tạo. Không thay MX/TXT của email Hostinger.
5. Bật Always Use HTTPS. Không tạo cache rule cho `/api/*`; nếu có cache-all rule,
   thêm bypass cho `/api/*`. Dùng hostname canonical đã chọn cho OAuth/WebSocket.
   Nếu cần `www`, redirect về hostname canonical ở Cloudflare.

Hướng dẫn chính thức: https://developers.cloudflare.com/tunnel/get-started/

## 3. Kiểm tra và khởi động

```sh
docker compose --env-file .env.prod config --quiet
docker compose --env-file .env.prod up -d --build
docker compose --env-file .env.prod ps
docker compose --env-file .env.prod logs --tail=100 backend-app cloudflared
```

Hoặc PowerShell: `./run.ps1 -Env prod`; Linux: `make prod`.
File `docker-compose.yml` mặc định include base + prod; không trộn thêm dev/local.
Không cần mở inbound 80/443/5432 hoặc port forwarding trên router.
Firewall phải cho phép cloudflared kết nối outbound (port 7844 TCP/UDP).
Không đưa output `docker compose config` đầy đủ lên public vì có thể chứa secrets.

Kiểm tra `https://<domain>/healthz` trả `ok`, reload trang con vẫn mở SPA,
đăng nhập thường/Google, gọi API, upload file, gửi email và nhận thông báo realtime.
Trong DevTools, WebSocket `wss://<domain>/ws` phải có status 101.
`healthz` chỉ xác nhận Nginx; backend có thể còn đang chạy Flyway khi frontend healthy.

Lỗi 502: xem backend logs và trạng thái DB/Redis/RabbitMQ; xác nhận Tunnel route
là `http://frontend:80`. Lỗi OAuth: kiểm tra origin/redirect trong Google Console.
Lỗi WebSocket 403: hostname trình duyệt phải trùng `PUBLIC_APP_URL`.

## 4. Cập nhật, dữ liệu và rollback

```sh
docker compose --env-file .env.prod up -d --build
docker compose --env-file .env.prod logs --tail=100 backend-app
```

CI vẫn test và publish image backend lên GHCR, nhưng không tự deploy máy này.
Stack production build từ source hai repo trên host. Các file `nginx/`,
`nginx.local.conf`, `docker-compose.local.yml` là cấu hình cũ/local, không tham gia
stack production mới. Không dùng hướng dẫn EC2/Watchtower cũ trong README.

DB/Redis/RabbitMQ dùng named volumes dưới project `roomhub-prod`.
Không chạy `down -v` trừ khi muốn xóa dữ liệu. Giữ nguyên tên thư mục/project Compose
nếu đang tái sử dụng volume cũ; kiểm tra `docker volume ls` trước khi chuyển stack.
Volume từ project `room-booking` cũ không tự chuyển sang `roomhub-prod`: cần backup/
restore sang volume mới để tránh nhầm là dữ liệu cũ đã mất.
Mật khẩu mới trong env không tự đổi mật khẩu của PostgreSQL/RabbitMQ đã có dữ liệu.

Nếu DB cũ chỉ nằm trên EC2: Docker mới không tự lấy lại dữ liệu; cần bản backup
hoặc quyền truy cập DB cũ để export/import. Flyway chỉ tạo/migrate schema.
V3 đã được sửa để bỏ qua booking mẫu khi user/room tham chiếu không tồn tại
(V2 xóa users mẫu). DB mới migrate bình thường. Nếu restore DB đã áp dụng V3 cũ,
Flyway sẽ báo checksum mismatch: đối chiếu chính xác thay đổi V3 và backup trước
khi dùng Flyway repair cập nhật checksum; không tắt validation hoặc repair mù.
Trước khi deploy phiên bản mới, backup DB bằng pg_dump (nên dùng định dạng custom
`-Fc`) và lưu ngoài host; kiểm tra restore định kỳ. Giữ nguyên RSA keys khi di chuyển.
Rollback source/image phải đi kèm phương án restore DB nếu migration không tương thích.

Để dừng: `docker compose --env-file .env.prod down` (giữ volumes).
Token Tunnel và secrets chỉ lưu trong `.env.prod` đã gitignore; không commit hoặc gửi log.
