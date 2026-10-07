# CRM

> Tài liệu tổng hợp hướng dẫn cài đặt và chạy **Backend** + **Frontend** cho dự án CRM.

## Tổng quan

Dự án gồm 2 phần chính:

- **Backend**: Java 21, Spring Boot, Maven, JOOQ, MySQL, Redis, MinIO
- **Frontend**: React 19, TypeScript, Vite, pnpm

## Cấu trúc thư mục

```text
CRM/
├─ backend/                      # Backend Spring Boot đa module
│  ├─ base/                      # Common config, security, utilities
│  ├─ entity/                    # Entity + JOOQ generated code
│  ├─ crm/                       # Module nghiệp vụ chính
│  ├─ docker-compose.yml         # Hạ tầng local/dev
│  └─ pom.xml                    # Maven parent project
└─ frontend/                     # Frontend React + Vite
   ├─ src/                       # Source code chính của ứng dụng
   │  ├─ assets/                 # Ảnh, icon, CSS, JS, dữ liệu tĩnh
   │  ├─ components/             # Component dùng chung và theo feature
   │  ├─ hooks/                  # Custom hooks
   │  ├─ lib/                    # API, axios instance, stores, types, utils
   │  ├─ routes/                 # Route definitions bằng TanStack Router
   │  ├─ index.css               # Style global
   │  └─ main.tsx                # Entry point của ứng dụng
   ├─ public/                    # Tài nguyên public tĩnh
   ├─ index.html                  # HTML template của Vite
   ├─ package.json                # Script và dependencies (dùng pnpm)
   ├─ tsconfig*.json              # Cấu hình TypeScript
   ├─ vite.config.ts              # Cấu hình Vite
   └─ eslint.config.*             # Cấu hình lint
```

---

## Công nghệ sử dụng

### Backend

- **Java 21**
- **Spring Boot 3.2.3**
- **Maven**
- **JOOQ 3.18.6**
- **MySQL 8**
- **Redis 7**
- **MinIO**
- **JWT**
- **MapStruct**
- **Docker / Docker Compose**

### Frontend

- **React 19**
- **TypeScript 5**
- **Vite 7**
- **pnpm**
- **TanStack Router / Query / Form / Table**
- **Bootstrap 5**
- **Axios**
- **Zustand**

---

## Yêu cầu môi trường

Cần cài sẵn các công cụ sau:

- **Git**
- **Java 21**
- **Maven 3.9+**
- **Node.js 20+**
- **pnpm**
- **Docker Desktop**

Cài `pnpm` nhanh bằng một trong các cách sau:

```bash
npm install -g pnpm
```

Hoặc nếu đã cài Node.js qua Corepack:

```bash
corepack enable
corepack prepare pnpm@latest --activate
```

Có thể kiểm tra nhanh:

```bash
java -version
mvn -version
node -v
pnpm -v
docker -v
```

---

## Cài đặt nhanh

### 1) Clone project

```bash
git clone <repo-url>
cd CRM-Bach
```

### 2) Cài dependencies frontend bằng pnpm

```bash
cd frontend
pnpm install
```

### 3) Build dependencies backend

```bash
cd ../backend
mvn clean install -DskipTests
```

---

## Chạy dự án ở môi trường local

## Cách 1: Chạy thủ công Backend + Frontend

Phù hợp khi phát triển hằng ngày.

### Bước 1. Khởi động services cho backend

Từ thư mục `backend`:

```bash
docker-compose up -d mysql redis minio phpmyadmin
```

Nếu muốn kiểm tra container đang chạy:

```bash
docker-compose ps
```

### Bước 2. Chạy backend

Từ thư mục `backend/crm`:

```bash
mvn spring-boot:run
```

### Bước 3. Chạy frontend

Mở terminal mới, vào thư mục `frontend`:

```bash
pnpm dev
```

### Địa chỉ truy cập mặc định

- **Frontend**: `http://localhost:4200`
- **Backend API**: `http://localhost:8080`
- **phpMyAdmin**: `http://localhost:8081`
- **MinIO Console**: `http://localhost:9001`
- **MySQL**: `localhost:3306`
- **Redis**: `localhost:6379`

---

## Cách 2: Chạy bằng Docker Compose toàn bộ hệ thống

Phù hợp khi muốn chạy gần giống môi trường đóng gói.

Từ thư mục `backend`:

```bash
docker-compose up -d --build
```

Sau khi chạy xong, các service chính sẽ gồm:

- `mysql`
- `redis`
- `minio`
- `phpmyadmin`
- `backend`
- `frontend`
- `nginx`

Dừng hệ thống:

```bash
docker-compose down
```

Dừng và xoá volume tạm nếu cần:

```bash
docker-compose down -v
```

---

## Hướng dẫn cấu hình backend

## Backend modules

### `base`
Chứa các thành phần dùng chung như:
- cấu hình hệ thống
- security
- exception handler
- utilities

### `entity`
Chứa:
- database entities
- JOOQ generated code

### `crm`
Chứa nghiệp vụ chính:
- controller
- service
- repository
- business logic

---

## Cấu hình backend mặc định

### Database

- **Host**: `localhost`
- **Port**: `3306`
- **Database**: lấy từ biến `MYSQL_DATABASE`
- **Username**: `root`
- **Password**: lấy từ biến `MYSQL_ROOT_PASSWORD`

### Application

- **Backend Port**: thường là `8080` khi chạy local bằng Maven
- **Docker Backend Port**: `8088`
- **Socket Port**: `9099`

> Lưu ý: cổng chạy thực tế có thể phụ thuộc vào file cấu hình environment hoặc cấu hình trong project.

---

## Hướng dẫn cấu hình frontend

### Cấu trúc thư mục frontend

```text
frontend/
├─ public/                    # Tài nguyên public tĩnh
├─ src/
│  ├─ assets/                 # Ảnh, icon, CSS, JS, dữ liệu tĩnh
│  ├─ components/             # Component dùng chung và theo feature
│  ├─ hooks/                  # Custom hooks
│  ├─ lib/                    # API, axios instance, stores, types, utils
│  ├─ routes/                 # Route definitions bằng TanStack Router
│  ├─ index.css               # Style global
│  └─ main.tsx                # Entry point của ứng dụng
├─ index.html                 # HTML template của Vite
├─ package.json               # Script và dependencies (dùng pnpm)
├─ tsconfig*.json             # Cấu hình TypeScript
├─ vite.config.ts             # Cấu hình Vite
└─ eslint.config.*            # Cấu hình lint
```

Frontend sử dụng **Vite** và mặc định chạy ở:

```text
http://localhost:4200
```

Theo cấu hình hiện tại trong `frontend/vite.config.ts`:

- dev server chạy cổng `4200`
- hỗ trợ `host: true`
- một số cấu hình host phụ thuộc vào biến `VITE_SERVER_URL`
- package manager chuẩn của frontend là **pnpm**

### Cài dependencies

```bash
cd frontend
pnpm install
```

### Chạy frontend

```bash
pnpm dev
```

### Build production frontend

```bash
pnpm build
```

### Preview bản build

```bash
pnpm preview
```

---

## Quy trình chạy đề xuất cho developer mới

Nếu bạn mới pull code về, nên chạy theo thứ tự sau:

### Backend

```bash
cd backend
docker-compose up -d mysql redis minio phpmyadmin
mvn clean install -DskipTests
cd crm
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
pnpm install
pnpm dev
```

---

## Một số lệnh hữu ích

### Backend

Build toàn bộ backend:

```bash
cd backend
mvn clean install -DskipTests
```

Chạy riêng module CRM:

```bash
cd backend/crm
mvn spring-boot:run
```

Generate / compile lại phần entity hoặc JOOQ code:

```bash
cd backend/entity
mvn clean compile
```

### Frontend

Cài package:

```bash
cd frontend
pnpm install
```

Chạy dev:

```bash
pnpm dev
```

Lint:

```bash
pnpm lint
```

Build:

```bash
pnpm build
```

---

## Dữ liệu và dịch vụ hỗ trợ

### MySQL
Dùng cho dữ liệu chính của hệ thống.

### Redis
Dùng cho cache hoặc các tác vụ realtime/session nếu project sử dụng.

### MinIO
Dùng cho object storage, lưu file/media.

### phpMyAdmin
Giao diện web để kiểm tra và quản trị MySQL nhanh.

---

## Luồng phát triển tính năng backend

### Thêm bảng mới

1. Tạo hoặc cập nhật script SQL
2. Áp dụng lên database
3. Generate / compile lại JOOQ code trong module `entity`
4. Sử dụng generated classes trong `crm` hoặc `base`

### Thêm API mới

1. Tạo `Controller` trong `backend/crm/src/main/java/vn/aeoc/crm/controller/`
2. Tạo `Service` trong `backend/crm/src/main/java/vn/aeoc/crm/service/`
3. Tạo / cập nhật `Repository` nếu cần
4. Kết nối frontend qua API tương ứng

---

## Khắc phục lỗi thường gặp

### 1. `mvn clean install` bị lỗi Java version

Kiểm tra lại Java:

```bash
java -version
```

Project backend đang dùng **Java 21**.

### 2. Frontend không chạy được sau khi `pnpm install`

Xoá thư mục `node_modules` và cài lại:

```bash
cd frontend
rm -rf node_modules pnpm-lock.yaml
pnpm install
```

> Trên Windows có thể xoá thủ công hoặc dùng PowerShell tương đương.

### 3. Backend không kết nối được MySQL

Kiểm tra:

- container `mysql` đã chạy chưa
- port `3306` có bị chiếm không
- biến môi trường database có đúng không

### 4. Frontend gọi API lỗi CORS / sai URL

Kiểm tra lại:

- URL backend đang dùng trong frontend
- cấu hình môi trường Vite
- cổng backend local đang chạy là `8080` hay `8088`

---

## Gợi ý môi trường chạy thực tế

### Dev local
- Chạy **backend bằng Maven**
- Chạy **frontend bằng Vite**
- Dùng **Docker** cho MySQL / Redis / MinIO

### Demo / staging đơn giản
- Dùng `docker-compose up -d --build`
- Chạy full stack bằng container

---

## Ghi chú

- Backend trong thư mục `backend` là dự án **multi-module Maven**.
- Frontend trong thư mục `frontend` là ứng dụng **React + Vite**.
- Nếu project có file `.env` riêng cho từng môi trường, hãy cấu hình đầy đủ trước khi chạy.
- Nếu cần mình có thể tiếp tục tạo thêm bản **README gốc ở thư mục root** để người mới vào repo nhìn là biết cách chạy toàn hệ thống ngay.
