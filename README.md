# Price Radar

Proje iskeleti. Bu depoda **yalnızca teknoloji kurulumu** var: her katman
yüklenmiş, yapılandırılmış ve çalıştığı doğrulanmış durumda. Uygulama özellikleri
henüz yazılmadı.

## Kurulu teknolojiler

| Katman | Teknoloji | Sürüm |
|---|---|---|
| Backend | Java + Spring Boot | 25 (hedef) · 4.1.1 |
| Frontend | Next.js + React + TypeScript | 16.3.8 · 19.2.8 · 5.x |
| UI | Tailwind CSS + shadcn/ui | 4.x · radix-nova preset |
| Database | PostgreSQL | 18.6 |
| ORM | Spring Data JPA + Hibernate | Boot sürüm yönetimi |
| Cache | Redis | 8.10 |
| Search | Elasticsearch | 9.4.5 |
| Queue | RabbitMQ | 4.3 |
| Auth | Spring Security + JWT (jjwt) | 0.13.0 |
| DB Migration | Flyway | Boot sürüm yönetimi |
| API Docs | springdoc-openapi | 3.1.1 |
| Container | Docker + Compose | — |
| CI/CD | GitHub Actions | — |
| Testing | JUnit 5 + Mockito + Testcontainers | TC 2.0.5 |
| Code Quality | SonarQube + JaCoCo | 26.9 community · 0.8.15 |

Sürüm notları:

- `spring-boot-starter-parent` **4.1.1**. Spring Initializr bu sürümü
  `4.1.1.RELEASE` kimliğiyle gösterir; Maven artifact sürümü `4.1.1`'dir.
- Boot 4, Jackson 3'e (`tools.jackson`) geçti ve starter'ları yeniden adlandırdı:
  `spring-boot-starter-web` → `spring-boot-starter-webmvc`, ve test bağımlılıkları
  starter başına `-test` artifact'larına bölündü.
- Elasticsearch imaj etiketi (9.4.5), Boot'un yönettiği `elasticsearch-java`
  istemci sürümüyle hizalıdır.
- Kurulu JDK 26 olsa da derleme `release 25` ile yapılır. Lombok, JDK 26'da
  `sun.misc.Unsafe` için bir uyarı basar; derlemeyi etkilemez.

## Gereksinimler

- **JDK 25+** — Maven Wrapper (`./mvnw`) var, ayrıca Maven kurmaya gerek yok
- **Node.js 24+**
- **Docker Desktop** — backing servisler ve Testcontainers için şart

## Çalıştırma

```bash
# 1. Ortam değişkenleri
cp .env.example .env
openssl rand -base64 48      # çıktıyı .env içindeki JWT_SECRET alanına yaz

# 2. Backing servisler (Postgres, Redis, Elasticsearch, RabbitMQ)
docker compose up -d

# 3. Backend
cd backend && ./mvnw spring-boot:run

# 4. Frontend (ayrı terminal)
cd frontend && cp .env.example .env.local && npm install && npm run dev
```

| Servis | Adres |
|---|---|
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8080/api/v1/ping |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Actuator health | http://localhost:8080/actuator/health |
| RabbitMQ yönetim | http://localhost:15672 |
| Elasticsearch | http://localhost:9200 |

Frontend ana sayfası backend'e `/api/v1/ping` atar ve bağlantı durumunu gösterir —
kurulumun uçtan uca çalıştığını orada görebilirsiniz.

Her şeyi container'da çalıştırmak için:

```bash
docker compose --profile app up -d --build
```

### Port 5432 çakışması

Makinede kurulu bir PostgreSQL servisi varsa 5432'yi o sahiplenir. Windows'ta
container da aynı porta bağlanabildiği için hata alınmaz — backend sessizce
**yerel** PostgreSQL'e bağlanır ve `password authentication failed for user
"price_radar"` ile düşer.

Ya yerel servisi durdurun:

```powershell
Stop-Service postgresql-x64-18     # servis adı kurulumunuza göre değişir
```

ya da container'ı başka bir porta alın ve backend'i oraya yönlendirin:

```bash
# .env içinde
POSTGRES_PORT=5433

docker compose up -d postgres

# backend'i çalıştırırken (mvnw root .env dosyasını okumaz)
POSTGRES_URL=jdbc:postgresql://localhost:5433/price_radar ./mvnw spring-boot:run
```

Testcontainers rastgele port kullandığı için entegrasyon testleri bu
çakışmadan etkilenmez.

## Testler

```bash
cd backend
./mvnw test     # tüm testler
./mvnw verify   # testler + JaCoCo kapsam raporu
```

İki tür test var:

- **Birim / web slice** — Docker gerektirmez (`JwtServiceTest`,
  `MetaControllerTest`).
- **Entegrasyon** — `AbstractIntegrationTest`'ten türer. Gerçek PostgreSQL,
  Redis, Elasticsearch ve RabbitMQ container'larını Testcontainers ile ayağa
  kaldırıp her birinin yanıt verdiğini doğrular. **Docker Desktop kapalıysa bu
  testler başarısız olur.**

İmaj etiketleri `TestcontainersConfiguration` içinde sabittir; `latest`
kullanılmaz, aksi halde yeşil bir build tekrar üretilebilir olmaz.

Frontend:

```bash
cd frontend
npm run lint
npm run typecheck
npm run format:check
npm run build
```

## SonarQube

```bash
docker compose --profile quality up -d
# http://localhost:9000 — ilk giriş admin/admin, şifre değiştirmeniz istenir.
# Proje token'ı üretip .env içindeki SONAR_TOKEN alanına yazın.

cd backend
./mvnw verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=$SONAR_TOKEN
```

Kapsam raporu JaCoCo tarafından `verify` fazında üretilir; Sonar onu
`target/site/jacoco/jacoco.xml` üzerinden okur. Frontend tarafının tarayıcı
yapılandırması `frontend/sonar-project.properties` dosyasındadır.

## Yapı

```
price-radar/
├── backend/                       Spring Boot 4.1 · Java 25
│   ├── src/main/java/com/priceradar/
│   │   ├── common/
│   │   │   ├── exception/         404 / 409 için alan hataları
│   │   │   └── web/               ProblemDetail handler + /ping
│   │   ├── config/                Cache, RabbitMQ, OpenAPI, *Properties
│   │   └── security/              SecurityConfig, JWT servisi ve filtresi
│   ├── src/main/resources/
│   │   ├── application.yml        Ortak yapılandırma (env ile override)
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/          Flyway sürümleri
│   ├── src/test/java/             AbstractIntegrationTest + testler
│   └── Dockerfile                 Çok aşamalı, layered jar
├── frontend/                      Next.js 16 · React 19 · Tailwind 4
│   ├── src/app/                   App Router
│   ├── src/components/ui/         shadcn/ui bileşenleri
│   ├── src/lib/api/               Tipli API istemcisi + problem+json
│   ├── src/lib/env.ts             Tek env okuma noktası
│   └── Dockerfile                 standalone çıktı
├── docker-compose.yml             Servisler + app / quality profilleri
└── .github/workflows/             Backend ve frontend CI
```

## Kurulum kararları

**Şemayı Flyway yönetir.** `spring.jpa.hibernate.ddl-auto` her ortamda
`validate`. Şema değişikliği yeni bir `V__*.sql` dosyası demektir; uygulanmış bir
migration asla düzenlenmez. `V1__baseline.sql` yalnızca `pg_trgm` ve `unaccent`
uzantılarını açar — tablolar uygulama geliştikçe eklenir.

**Redis cache JSON serileştirir** ve `price-radar:` ön ekiyle, 10 dakikalık
varsayılan TTL ile çalışır. Polimorfik tip bilgisi `com.priceradar.*` ile
sınırlandırılmıştır. Cache başına TTL gerektiğinde
`RedisCacheManagerBuilderCustomizer` eklenir.

**RabbitMQ topolojisi dead-letter'lı kurulur.** Bir topic exchange, bir DLX ve
bir DLQ hazır. Eklenecek her iş kuyruğu `QueueBuilder.deadLetterExchange(...)`
ile tanımlanmalı — böylece zehirli mesaj sonsuz yeniden teslim yerine DLQ'da
bekler.

**Hatalar RFC 9457 `application/problem+json`.** Frontend'deki `ApiError` bu
gövdeyi taşır; form hataları `fieldErrors` üzerinden okunur.

**Varsayılan olarak her uç kimlik doğrulama ister.** Yalnızca
`/actuator/health`, `/actuator/info`, `/api/v1/ping` ve API dokümantasyonu
açıktır. Yeni kurallar `anyRequest()` satırının üstüne eklenir.

**`@EnableCaching` ve benzeri etkinleştirmeler ilgili `@Configuration`
sınıfında**, ana uygulama sınıfında değil. Ana sınıfta olduklarında web slice
testlerine sızıp o testleri cache yöneticisi sağlamaya zorluyorlar.

**Frontend rewrite'ı build zamanında gömülür.** `next.config.ts` içindeki
`/api/v1/*` → backend yönlendirmesi `next build` sırasında routes manifest'e
yazılır (doğrulandı). Bu yüzden `BACKEND_INTERNAL_URL` Docker imajına **build
arg** olarak geçilir; sadece runtime'da vermek bu yönlendirmeyi değiştirmez.
Server Component'ler aynı değeri `serverEnv` üzerinden runtime'da okur.

## Sıradaki adımlar

- JPA entity'leri ve repository'ler. Eklendikten sonra istenirse
  `@EnableJpaAuditing` açılabilir — şu an metamodel boş olduğu için kapalı.
- Kimlik doğrulama uçları. `JwtService` ve `JwtAuthenticationFilter` hazır;
  controller ve kullanıcı deposu yazılacak.
- Elasticsearch indeks tanımları (`SearchProperties.indexFor` adlandırmayı tutar).
- RabbitMQ tüketicileri ve iş kuyrukları.
- Frontend test altyapısı (Vitest / Playwright).
- Deploy pipeline'ı — CI imajları derler ama hiçbir yere push etmez.
