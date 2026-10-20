# Deploy: Vercel frontend + Docker backend + Cloudflare Tunnel

Frontend ở Vercel, domain chính `https://roomhub.online`.
API và WebSocket ở `https://api.roomhub.online` / `wss://api.roomhub.online/ws`.
Cloudflare Tunnel `roomhub-prod` route API tới `http://backend-app:8080`.

Docker production chỉ chạy backend-app, PostgreSQL, Redis, RabbitMQ và cloudflared.
Không có frontend/Nginx/Certbot/Watchtower, không publish cổng host.
Project `roomhub-prod` có network/volumes riêng, chạy song song với ChargeOps.
Máy chạy Docker phải luôn bật và có Internet; Hostinger chỉ giữ đăng ký domain.

## Cấu hình production

Trong `room-booking/.env.prod` (gitignored), dùng `.env.prod.example` làm mẫu,
không ghi đè file secrets hiện có:

- PUBLIC_APP_URL=https://roomhub.online
- PUBLIC_API_URL=https://api.roomhub.online
- CLOUDFLARE_TUNNEL_TOKEN: token riêng của RoomHub, không dùng Tunnel ChargeOps.
- Điền DB/RabbitMQ credentials, Google OAuth, Cloudinary, email và REGISTRATION_KEY.
- Giữ RSA keys trong `certs/private.pem`, `certs/public.pem`, không tạo đè key cũ.

```sh
docker compose --env-file .env.prod config --quiet
docker compose --env-file .env.prod up -d --build
docker compose --env-file .env.prod ps
docker compose --env-file .env.prod logs --tail=100 backend-app cloudflared
```

Hoặc PowerShell `./run.ps1 -Env prod`, Linux `make prod`.
Docker Compose v2.24+ hỗ trợ include và reset tên container.

## Vercel

Deploy repo `room-booking-frontend` lên project Vercel hiện có.
Cấu hình build: `npm run build`, output `dist`.
Các biến VITE là public, không điền client secret/API secret:

- VITE_API_URL=/api/v1 (dùng rewrite trong vercel.json) hoặc https://api.roomhub.online/api/v1
- VITE_STOMP_BROKER_URL=wss://api.roomhub.online/ws
- VITE_GOOGLE_CLIENT_ID: Google client ID đang dùng ở backend.

HTTP API có thể qua rewrite Vercel; WebSocket kết nối trực tiếp API domain.
Google OAuth cần origin `https://roomhub.online` và redirect URI
`https://roomhub.online/auth/google-callback`. Đổi biến Vite phải redeploy.

## DNS / Tunnel

- `api.roomhub.online`: proxied CNAME tới `<Tunnel-ID>.cfargotunnel.com`.
- Published application: `api.roomhub.online` → `http://backend-app:8080`, path trống.
- `roomhub.online`: DNS-only CNAME tới đích Vercel chỉ định trong Project Settings → Domains.
- `www`: cấu hình redirect về domain canonical trong Vercel nếu cần.
- Không trỏ root domain vào Tunnel nữa; không đụng MX/TXT email.
- Không cần port forwarding. Firewall cần outbound cloudflared TCP/UDP 7844.

Thứ tự chuyển từ frontend Docker: tạo API route và DNS, kiểm tra API/WebSocket,
deploy frontend Vercel, chuyển DNS root; sau đó mới vô hiệu hóa route root cũ và dừng
container frontend cũ bằng `docker compose --env-file .env.prod up -d --remove-orphans`.
Route root cũ hiện trả `http_status:404`, không trỏ frontend Docker nữa. DNS root đã
đi Vercel nên route này không nhận traffic website; giữ nó để tránh thao tác Delete
route của Cloudflare xóa kèm bản ghi DNS Vercel cùng hostname.
Không chạy `down -v`: dữ liệu nằm trong volumes hiện tại và phải được giữ nguyên.

## Kiểm tra và dữ liệu

Kiểm tra trang login, API chưa đăng nhập trả 401, WebSocket handshake thành công,
đăng nhập, tạo/sửa phòng, upload và email. Chưa đăng nhập không nên trả 502.
Backend chạy Flyway; V25 sửa trạng thái hai building seed chưa từng được admin sửa.
Building bị tắt có chủ đích vẫn bị chặn tạo phòng và trả HTTP 409 (CR_004).

DB hiện tại là PostgreSQL local của RoomHub, chưa restore dữ liệu EC2 cũ.
DB/Redis/RabbitMQ dùng named volumes project `roomhub-prod`; thay source/DNS không
xóa các volumes này. Backup bằng pg_dump và lưu ngoài máy trước khi nâng cấp.

V3 đã được sửa để bỏ qua booking mẫu thiếu user/room. Nếu restore DB đã áp dụng V3
cũ, đối chiếu checksum và backup trước khi Flyway repair; không tắt validation.

File `frontend/Dockerfile`, `nginx.conf` chỉ còn là tùy chọn tự host, không dùng trong
production Vercel. Tài liệu EC2/Watchtower cũ trong README không còn áp dụng.

Tài liệu: https://developers.cloudflare.com/tunnel/get-started/
Vercel rewrites: https://vercel.com/docs/routing/rewrites
