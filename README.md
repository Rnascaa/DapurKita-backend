# DapurKita Backend

REST API untuk pencarian dan pengelolaan resep masakan Indonesia.
Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway. Kontrak: **API v2.3.1** (`openapi.yaml`, base path `/api/v1`).

## Menjalankan (lokal)

Prasyarat: JDK 21, Maven 3.9+, Docker.

```bash
docker compose up -d                       # PostgreSQL 16 (db/user/password: dapurkita)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Di PowerShell, beri tanda kutip pada argumen Maven: `mvn spring-boot:run "-Dspring-boot.run.profiles=dev"`.

Profil `dev` memakai JWT secret dev, cookie tanpa `Secure`, dan membuat admin
`admin@dapurkita.local` / `Admin12345!`. Jangan dipakai di produksi.

Profil `dev` juga mengisi **16 resep contoh** lewat `SampleRecipeSeeder`, hanya jika tabel resep kosong
(`app.seed.sample-recipes=true`). Semua `imageUrl` contoh bernilai `null`.

Untuk mengosongkan database dev (semua data dev terhapus): `docker compose down -v`, lalu `docker compose up -d`.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs (hasil generate, bukan acuan kontrak; acuannya `openapi.yaml`)

## Produksi

Set environment variable (lihat `.env.example`): `DB_URL`, `DB_USER`, `DB_PASSWORD`,
`JWT_SECRET` (wajib, tidak ada default), `ADMIN_EMAIL`, `ADMIN_PASSWORD`.
Jalankan dengan `--spring.profiles.active=prod` (Swagger dimatikan, cookie `Secure`).
Layani frontend dan API dari origin yang sama lewat reverse proxy, karena refresh token
adalah cookie `SameSite=Strict` dengan `Path=/api/v1/auth`.

## Tes

```bash
mvn test        # butuh Docker (Testcontainers menjalankan PostgreSQL 16 asli)
```

`ApiIntegrationTest` mencakup: register/login/refresh (rotasi dan deteksi pemakaian ulang)/logout,
TOKEN_EXPIRED vs UNAUTHORIZED, akses ADMIN (401/403/201), validasi bersarang `ingredients[0].name`,
`categoryId` tidak ada, pencarian (`q`, `categoryId`, `ingredient` berulang sebagai AND), pagination
dan sort, page/size/sort di luar batas (400), PUT mengganti seluruh isi, DELETE lalu 404, serta favorit idempoten.

## Struktur

Package-by-feature di `com.dapurkita`: `auth`, `user`, `category`, `recipe`, `favorite`,
`security`, `config`, `common`. Skema ada di `src/main/resources/db/migration` (Flyway);
Hibernate hanya memvalidasi (`ddl-auto=validate`).
