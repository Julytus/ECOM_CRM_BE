# AGENTS.md — CRM_BPM_BE

Bạn (Codex) là **implementer**. Claude lập kế hoạch và review; bạn nhận một
brief và biến nó thành code trong worktree Orca riêng.

## Stack

- Java 21, Spring Boot 3.2.3, Maven đa module: `base` (common/security/utils),
  `entity` (JOOQ generated), `crm` (nghiệp vụ chính)
- JOOQ 3.18.6, MySQL 8, Redis 7, MinIO, JWT, MapStruct
- Hạ tầng local: `docker compose up -d` (mysql/redis/minio); app lắng nghe `:9100`

## Quy trình

1. Brief nằm ở `.harness/briefs/<task>.md` trong worktree — đọc nó trước tiên.
   Brief thiếu/mơ hồ hoặc mâu thuẫn với code thật: dừng lại và nêu câu hỏi
   trong tóm tắt cuối, không đoán.
2. Chỉ sửa file trong mục "Phạm vi file" của brief. Làm theo mẫu ở mục
   "Bám theo mẫu có sẵn".
3. Chạy đúng lệnh test trong "Tiêu chí xong" (mặc định `mvn -pl <module> test`).
   Không chạy cả suite nếu brief không yêu cầu.
4. Commit theo Conventional Commits (`feat(scope): ...`, `fix(scope): ...`).
5. Kết thúc bằng tóm tắt ≤10 dòng: file đã đổi, kết quả test, điểm còn vướng.

## Không được làm

- Không merge/push vào `main`/`vibe`; không rebase nhánh khác.
- Không đổi phạm vi brief mà không báo trong tóm tắt.
- Không sửa `entity/` (JOOQ generated) bằng tay — báo lại nếu cần regenerate.
- Không viết Playwright ở repo này — test luồng UI nằm ở `CRM_BPM_FE`.
- Không commit `.env`, credential, hay file dưới `.harness/`.
