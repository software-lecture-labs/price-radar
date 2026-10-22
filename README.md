<p align="center">
  <img src="assets/readme/banner.svg" alt="Price Radar: ürün, teklif ve fiyat geçmişi için Spring Boot ve Next.js tabanlı altyapı" width="100%">
</p>

<p align="center">
  <a href="#çözülen-problem">Problem</a> ·
  <a href="#özellikler-ve-modüller">Modüller</a> ·
  <a href="#teknoloji-yığını">Teknolojiler</a> ·
  <a href="#sistem-mimarisi">Mimari</a> ·
  <a href="#kurulum-ve-çalıştırma">Kurulum</a> ·
  <a href="#testler-ve-kalite-kontrolleri">Testler</a> ·
  <a href="#geliştirme-durumu">Durum</a>
</p>

**Price Radar**, farklı satıcılardaki ürün tekliflerini ve fiyat geçmişini tek yerde
toplamayı hedefleyen bir fiyat karşılaştırma platformudur. Depo şu anda bu platformun
**altyapı iskeletini** içerir: Spring Boot backend, Next.js frontend ve PostgreSQL, Redis,
Elasticsearch ve Kafka servisleri kurulu, yapılandırılmış ve birbirine bağlıdır.

> [!NOTE]
> Ürün, teklif ve fiyat geçmişi özellikleri henüz yazılmadı. Çalışan uçlar yalnızca
> bağlantı doğrulama uçlarıdır (`/api/v1/ping`, `/api/v1/hello`). Ayrıntılar:
> [Geliştirme durumu](#geliştirme-durumu).

## Çözülen problem

Aynı ürün birçok satıcıda farklı fiyatlarla satılır ve fiyatlar zamanla değişir. Tek tek
mağaza gezmek hem en uygun teklifi bulmayı hem de bir fiyatın gerçekten düşük olup
olmadığını anlamayı zorlaştırır. Price Radar bu teklifleri tek bir ürün altında toplayıp
fiyat geçmişiyle birlikte sunmayı amaçlar.

Bu hedef koddaki tanımlardan gelir (`pom.xml` açıklaması, OpenAPI başlığı
"Product, offer and price-history API", `next.config.ts` içindeki satıcı logosu notu).
Kurulu altyapı bu hedefin ihtiyaçlarıyla şöyle eşleşir:

| İhtiyaç | Kurulu altyapı |
|---|---|
| Farklı mağazalarda farklı yazılan ürün adlarını eşleştirmek | PostgreSQL `pg_trgm` (benzerlik) ve `unaccent` (aksan duyarsız arama) uzantıları |
| Hızlı ürün araması | Elasticsearch, ortam başına indeks öneki (`price-radar-*`) |
| Sık okunan verileri hafifletmek | Redis cache, 10 dakika varsayılan TTL |
| Fiyat değişimi gibi olayları bağımsız işlemek | Kafka olay topic'i ve işlenemeyen kayıtlar için dead-letter topic |

## Özellikler ve modüller

Backend bir **modüler monolit**tir: tek bir Spring Boot uygulaması olarak derlenip
dağıtılır, ama kod `com.priceradar` altında sınırları test ile korunan modüllere
ayrılmıştır. Kurallar için: [Modül kuralları](#modül-kuralları).

### Backend modülleri

| Modül | Durum | Sorumluluk | Bağımlı olabileceği modüller |
|---|---|---|---|
| `system` | Çalışıyor | `GET /api/v1/ping` servis adı, API sürümü ve sunucu zamanını döner. `GET /api/v1/hello` bir selamlama mesajı döner. İkisi de kimlik doğrulama istemez. | yok |
| `identity` | Çalışıyor | Stateless filtre zinciri, HMAC imzalı JWT üretme ve doğrulama (`JwtService`), `Bearer` başlığını okuyan filtre, BCrypt parola kodlayıcı, CORS. Açık uçlar dışındaki her istek kimlik doğrulama ister. Filtre zinciri `identity.internal` içindedir ve diğer modüllere kapalıdır. | yok |
| `platform` | Çalışıyor | Redis cache (JSON değerler, `price-radar:` öneki, 10 dakika TTL), Kafka topic'leri (`price-radar.events`, `price-radar.dlt`) ve üstel beklemeli 3 denemeden sonra DLT'ye yazan hata işleyici, OpenAPI tanımı. | yok |
| `shared` | Çalışıyor | Ortak hata tipleri (`ResourceNotFoundException` → 404, `BusinessRuleException` → 409) ve tüm hataları RFC 9457 `application/problem+json` olarak döndüren handler. Paylaşılan modüldür; her modül kullanabilir. | yok |
| `catalog` | Planlanan | Kanonik ürünler, markalar, kategoriler, özellikler | yok |
| `merchant` | Planlanan | Mağazalar ve mağaza ürün kayıtları | yok |
| `pricing` | Planlanan | Fiyatlar, indirimler, fiyat geçmişi | yok |
| `ingestion` | Planlanan | Mağaza entegrasyonları, veri toplama, normalizasyon | `merchant`, `pricing` |
| `matching` | Planlanan | Mağaza kaydını kanonik ürüne eşleme | `catalog`, `merchant` |
| `search` | Planlanan | Elasticsearch indeksi ve arama. Şimdilik yalnızca `SearchProperties` var. | `catalog`, `merchant`, `matching`, `pricing` |
| `alert` | Planlanan | Fiyat alarmları ve bildirimler | `pricing`, `matching`, `identity` |
| `favorites` | Planlanan | Favoriler ve takip listeleri | `catalog` |

Planlanan modüllerde yalnızca `package-info.java` var: sorumluluk ve izin verilen
bağımlılıklar şimdiden tanımlı, kod henüz yok. `shared` her modül için ayrıca
listelenmeden izinlidir. Modül haritası, veri modeli ve kararların gerekçeleri
[docs/architecture](docs/architecture/README.md) altındadır.

### Ortak altyapı ve frontend

| Bileşen | Konum | Ne yapar |
|---|---|---|
| Şema yönetimi | `backend/src/main/resources/db/migration/` | Flyway `V1__baseline.sql`: `pg_trgm` ve `unaccent` uzantıları. `V2__event_publication.sql`: Spring Modulith olay kaydı tablosu. Hibernate şemaya dokunmaz (`ddl-auto: validate`). |
| API dokümantasyonu | `backend/.../platform/OpenApiConfig.java` | Swagger UI ve OpenAPI JSON, Bearer şemasıyla. `prod` profilinde Swagger UI kapalıdır. |
| Frontend ana sayfa | `frontend/src/app/page.tsx` | Backend'e sunucu tarafında `ping` ve `hello` isteği atar, bağlantı durumunu ve kurulu katmanları gösterir. |
| API istemcisi | `frontend/src/lib/api/` | Tipli `apiFetch`, sorgu parametreleri, Bearer token, problem+json gövdesini taşıyan `ApiError`. |

### Planlanan

Bu başlıklar için henüz kod yok. Sıra ve kapsam
[geliştirme planında](docs/architecture/README.md#geliştirme-sırası):

1. **Temel domain modeli:** ürün, mağaza, mağaza kaydı, eşleşme ve fiyat gözlemi
   ([ADR 0005](docs/architecture/adr/0005-product-and-price-model.md)).
2. **Veri toplama ve eşleştirme:** önce fiyat toplama yöntemine karar verilmesi
   gerekiyor ([ADR 0006](docs/architecture/adr/0006-price-collection-and-matching.md), açık).
3. **Arama ve performans:** Elasticsearch senkronizasyonu, cache başına TTL.
4. **Olay güdümlü süreçler:** olay sözleşmeleri, Kafka'ya dışsallaştırma, fiyat alarmları.
5. **Production hazırlığı:** rate limiting, gözlemlenebilirlik, yedekleme, yük testi, CI/CD.

Bunlara ek olarak: kimlik doğrulama uçları (OpenAPI açıklaması `POST /api/v1/auth/login`
ucuna atıf yapıyor ama bu uç henüz yok), frontend test altyapısı ve deploy pipeline'ı
(CI imajları derliyor ama hiçbir registry'ye push etmiyor).

## Teknoloji yığını

Sürümler `pom.xml`, `package.json`, `docker-compose.yml` ve çözümlenmiş bağımlılıklardan alınmıştır.

| Katman | Teknoloji | Sürüm |
|---|---|---|
| Backend | Java, Spring Boot | 25 · 4.1.1 |
| Modüler monolit | Spring Modulith (ArchUnit tabanlı doğrulama) | 2.1.1 |
| Frontend | Next.js, React, TypeScript | 16.3.8 · 19.2.8 · 5.9 |
| UI | Tailwind CSS, shadcn/ui (radix-nova), lucide-react | 4.3 · 4.21 |
| Veritabanı | PostgreSQL | 18.6 |
| ORM ve şema | Spring Data JPA, Hibernate, Flyway | Hibernate 7.4.5 · Flyway 12.4.0 |
| Cache | Redis (Lettuce istemcisi) | 8.10 |
| Arama | Elasticsearch, Spring Data Elasticsearch | 9.4.5 · 6.1.1 |
| Mesajlaşma | Apache Kafka (KRaft), Spring Kafka | 4.3.1 · 4.1.1 |
| Kimlik doğrulama | Spring Security, jjwt | 0.13.0 |
| API dokümantasyonu | springdoc-openapi | 3.1.1 |
| Test | JUnit Jupiter, Mockito, Testcontainers | 6.0.3 · 5.23.0 · 2.0.5 |
| Kod kalitesi | SonarQube Community, JaCoCo, ESLint, Prettier | 26.9 · 0.8.15 · 9 · 3.9 |
| Container ve CI | Docker Compose, GitHub Actions | |

## Sistem mimarisi

<p align="center">
  <img src="assets/readme/architecture.svg" alt="Sistem mimarisi: Tarayıcı, Next.js frontend, Spring Boot backend ve PostgreSQL, Redis, Elasticsearch, Kafka servisleri" width="100%">
</p>

- **Tek köken.** Tarayıcı yalnızca Next.js ile konuşur. `next.config.ts` içindeki rewrite
  `/api/v1/*` isteklerini backend'e iletir, böylece tarayıcı tarafında CORS gerekmez.
  Server Component'ler backend'e doğrudan `BACKEND_INTERNAL_URL` üzerinden gider.
- **Rewrite build sırasında sabitlenir.** `next build` yönlendirme hedefini routes
  manifest'e yazar. Bu yüzden Docker imajında `BACKEND_INTERNAL_URL` build argümanı
  olarak verilir; yalnızca çalışma zamanında vermek rewrite'ı değiştirmez.
- **Varsayılan olarak kapalı.** Yalnızca `/actuator/health/**`, `/actuator/info`,
  `/api/v1/ping`, `/api/v1/hello` ve API dokümantasyonu açıktır. Diğer actuator uçları
  `ADMIN` rolü ister, kalan her şey geçerli bir JWT ister.
- **Şemanın tek sahibi Flyway.** Her ortamda `ddl-auto: validate` kullanılır. Şema
  değişikliği yeni bir `V__*.sql` dosyasıdır; uygulanmış migration düzenlenmez.
- **Kafka topic'leri koddan gelir.** Broker'da otomatik topic oluşturma kapalıdır
  (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`); yeni topic bir `NewTopic` bean'i demektir.
  JSON'a çevrilemeyen kayıtlar ham byte olarak DLT'ye yazılır.
- **Veri servisleri bağlı, henüz kullanılmıyor.** Dört servis de entegrasyon testlerinde
  yanıt veriyor, ancak olay kaydı tablosu dışında tablo, indeks, `@Cacheable` metodu veya
  Kafka tüketicisi yok.

### Modül kuralları

- **Modül, paket demektir.** `com.priceradar` altındaki her doğrudan alt paket bir
  modüldür. Modülün adı, açıklaması ve bağımlı olabileceği modüller o paketin
  `package-info.java` dosyasındaki `@ApplicationModule` ile tanımlanır.
- **API üst pakettir.** Bir modülün üst paketindeki public tipler diğer modüllere
  açıktır. Alt paketler (örneğin `identity.internal`) iç ayrıntıdır; başka modül
  bunlara erişemez.
- **Bağımlılıklar beyan edilir.** Bir modül yalnızca `allowedDependencies` içinde
  listelenen modüllere bağımlı olabilir. `shared` her modüle açıktır. Modüller
  arasında döngü yasaktır.
- **Modüller arası iletişim tercihen olaylarla.** Bir modül olay yayınlar, diğeri
  `@ApplicationModuleListener` ile dinler. Spring Modulith olayı işlem commit'lenirken
  `event_publication` tablosuna yazar ve dinleyici başarıyla bitince tamamlandı olarak
  işaretler. Yarıda kalan olaylar tabloda kalır; `IncompleteEventPublications` ile
  yeniden gönderilebilir. Uygulama açılırken otomatik yeniden gönderim kapalıdır
  (varsayılan).
- **Modül içi hexagonal katmanlar.** Her iş modülü `domain` (saf iş modeli ve
  portlar), `application` (kullanım senaryoları), `infrastructure` (JPA,
  Elasticsearch, Kafka, dış API adaptörleri) ve `web` (REST) katmanlarına ayrılır.
  Domain Spring, JPA, Kafka veya Elasticsearch kullanmaz; JPA entity'leri ve Kafka
  dinleyicileri yalnızca `infrastructure` içinde olur
  ([ADR 0002](docs/architecture/adr/0002-hexagonal-modules.md)).
- **Kurallar build'de denetlenir.** `ModularityTests` modüller arası sınırları,
  `HexagonalArchitectureTests` modül içi katmanları her `./mvnw test` çalışmasında
  doğrular; ihlal build'i kırar. `ModularityTests` ayrıca modül diyagramlarını
  (PlantUML) ve modül kartlarını `backend/target/spring-modulith-docs/` altına yazar.

Yeni bir modül eklemek için `com.priceradar` altında bir paket açın ve
`package-info.java` içine `@ApplicationModule(displayName = "...", allowedDependencies = ...)`
yazın. `ModularityTests.allExpectedModulesAreDetected` beklenen modül listesini tutar;
onu da güncelleyin.

### Temel kullanım akışı

Ürün özellikleri olmadığı için bugün uçtan uca çalışan akış, ana sayfanın backend
bağlantısını doğrulamasıdır.

<p align="center">
  <img src="assets/readme/request-flow.svg" alt="İstek akışı: Tarayıcı, Next.js sunucusu, Spring Security ve MetaController arasında ping ve hello çağrıları" width="100%">
</p>

1. Tarayıcı `http://localhost:3000/` adresini ister.
2. Next.js sunucusu `GET /api/v1/ping` ve `GET /api/v1/hello` isteklerini paralel ve
   önbelleksiz (`cache: "no-store"`) gönderir.
3. Spring Security bu iki uca token istemeden izin verir; `MetaController` yanıtlar.
4. Backend `PingResponse` (`service`, `apiVersion`, `timestamp`) ve `HelloResponse`
   (`message`, `service`) döner.
5. Sayfa "Hello World" mesajı ve `connected` rozetiyle render edilir.

Backend'e ulaşılamazsa ya da 2xx dışı yanıt gelirse hata yakalanır. Sayfa yine açılır,
`unreachable` rozetini ve yerel "Hello World" metnini gösterir.

## Kurulum ve çalıştırma

### Gereksinimler

- **JDK 25+.** Maven Wrapper depoda var, ayrıca Maven kurmaya gerek yok.
- **Node.js 24+** (CI ve Docker imajı Node 24 kullanır).
- **Docker** ve Docker Compose. Veri servisleri ve entegrasyon testleri için gerekir.

### Yerel geliştirme

Veri servisleri container'da, backend ve frontend doğrudan makinede çalışır.

```bash
# 1. Ortam değişkenleri (Compose bu dosyayı okur)
cp .env.example .env

# 2. Veri servisleri: PostgreSQL, Redis, Elasticsearch, Kafka
docker compose up -d

# 3. Backend (ayrı terminal)
cd backend
./mvnw spring-boot:run

# 4. Frontend (ayrı terminal)
cd frontend
npm install
npm run dev
```

> [!IMPORTANT]
> `./mvnw spring-boot:run` kök dizindeki `.env` dosyasını **okumaz**. `.env.example`
> PostgreSQL parolasını `change-me` olarak verir, backend ise varsayılan olarak
> `price_radar` parolasını kullanır. İki yoldan birini seçin:
>
> - `.env` içinde `POSTGRES_PASSWORD=price_radar` bırakın, **ya da**
> - backend'i aynı parolayla başlatın: `POSTGRES_PASSWORD=<.env'deki değer> ./mvnw spring-boot:run`
>   (PowerShell: `$env:POSTGRES_PASSWORD="<değer>"; .\mvnw.cmd spring-boot:run`)
>
> PostgreSQL parolayı yalnızca veri dizini ilk oluşturulurken ayarlar. Değeri sonradan
> değiştirdiyseniz `docker compose down -v` ile volume'u silmeniz gerekir; bu işlem
> veritabanındaki tüm veriyi siler.

Frontend için ortam dosyası gerekmez; `frontend/src/lib/env.ts` yerel varsayılanları
kullanır (`BACKEND_INTERNAL_URL=http://localhost:8080`, `NEXT_PUBLIC_API_BASE_PATH=/api/v1`).
Farklı bir değer gerekirse `frontend/.env.local` oluşturun.

`JWT_SECRET` `dev` profilinde boş kalabilir; backend yalnızca geliştirme için tanımlı bir
anahtara düşer. `prod` profili bu değer olmadan açılmaz. Anahtar üretmek için:
`openssl rand -base64 48`.

### Adresler

| Servis | Adres |
|---|---|
| Frontend | http://localhost:3000 |
| Ping | http://localhost:8080/api/v1/ping |
| Hello | http://localhost:8080/api/v1/hello |
| Swagger UI (`prod` profilinde kapalı) | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Actuator health | http://localhost:8080/actuator/health |
| Elasticsearch | http://localhost:9200 |
| Kafka | `localhost:9092` |
| SonarQube (`quality` profili) | http://localhost:9000 |

### Her şeyi container'da çalıştırmak

```bash
docker compose --profile app up -d --build
```

`app` profili backend ve frontend imajlarını derleyip veri servisleri sağlıklı olduktan
sonra başlatır. Compose backend için bağlantı adreslerini kendisi kurar, bu yüzden
yukarıdaki parola uyarısı bu modda geçerli değildir.

### Sorun giderme

<details>
<summary><strong>5432 portu yerel bir PostgreSQL tarafından kullanılıyor</strong></summary>

Makinede kurulu bir PostgreSQL servisi 5432'yi tutuyorsa backend sessizce o servise
bağlanabilir ve `password authentication failed for user "price_radar"` hatasıyla
durur. Yerel servisi durdurun ya da container'ı başka porta alın:

```bash
# .env
POSTGRES_PORT=5433

docker compose up -d postgres
POSTGRES_URL=jdbc:postgresql://localhost:5433/price_radar ./mvnw spring-boot:run
```

Testcontainers rastgele port kullandığı için testler bu çakışmadan etkilenmez.
</details>

<details>
<summary><strong>Linux / macOS: <code>./mvnw: Permission denied</code></strong></summary>

`backend/mvnw` git'te çalıştırılabilir olarak işaretli değil (mod `100644`). Çözüm
olarak `chmod +x backend/mvnw` çalıştırın ya da `sh mvnw ...` kullanın.
</details>

<details>
<summary><strong>Temiz checkout'ta <code>npm run typecheck</code>: <code>Cannot find name 'LayoutProps'</code></strong></summary>

`LayoutProps` tipini Next.js `.next/types` altında üretir; bu dizin `next dev` ya da
`next build` çalışmadan önce yoktur. Typecheck'ten önce tipleri üretin:

```bash
npx next typegen
npm run typecheck
```
</details>

## Klasör yapısı

```text
price-radar/
├── backend/                         Spring Boot 4.1 · Java 25
│   ├── src/main/java/com/priceradar/
│   │   ├── PriceRadarBackendApplication.java   @Modulithic, paylaşılan modül
│   │   ├── system/                  /api/v1/ping ve /api/v1/hello
│   │   ├── identity/                JwtService, SecurityProperties (modül API'si)
│   │   │   └── internal/            SecurityConfig, JWT filtresi (modüle özel)
│   │   ├── platform/                Cache, Kafka, OpenAPI yapılandırması
│   │   ├── shared/                  ortak hata tipleri
│   │   │   └── web/                 problem+json handler
│   │   ├── catalog/  merchant/  pricing/  ingestion/  matching/
│   │   ├── alert/  favorites/       planlanan modüller (package-info.java)
│   │   └── search/                  planlanan modül, SearchProperties
│   ├── src/main/resources/
│   │   ├── application.yml          ortak yapılandırma, ortam değişkenleriyle ezilir
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/            Flyway: V1 uzantılar, V2 olay kaydı
│   ├── src/test/                    modül, birim, web slice ve Testcontainers testleri
│   ├── pom.xml
│   └── Dockerfile                   çok aşamalı, katmanlı jar
├── frontend/                        Next.js 16 · React 19 · Tailwind 4
│   ├── src/app/                     App Router sayfaları
│   ├── src/components/ui/           shadcn/ui bileşenleri
│   ├── src/lib/api/                 tipli API istemcisi ve problem+json tipi
│   ├── src/lib/env.ts               ortam değişkenlerinin okunduğu tek yer
│   ├── sonar-project.properties
│   └── Dockerfile                   standalone çıktı
├── docs/architecture/              mimari genel bakış ve karar kayıtları (ADR)
├── assets/readme/                   README görselleri (SVG)
├── .github/workflows/               backend-ci.yml, frontend-ci.yml
├── docker-compose.yml               veri servisleri + app ve quality profilleri
└── .env.example                     Compose için ortam değişkenleri şablonu
```

## Testler ve kalite kontrolleri

### Mevcut testler

Tüm testler backend'de; frontend'de henüz test yok.

| Test sınıfı | Tür | Test sayısı | Docker gerekir | Kapsam |
|---|---|---|---|---|
| `ModularityTests` | Mimari (Spring Modulith) | 3 | Hayır | Modül sınırları ve izinli bağımlılıklar, beklenen 12 modülün algılanması, modül dokümantasyonu üretimi |
| `HexagonalArchitectureTests` | Mimari (ArchUnit) | 8 | Hayır | Modül içi katman yönü, framework'süz domain, adaptör sızdırmayan modül API'si, JPA entity, Kafka listener ve controller yerleşimi |
| `JwtServiceTest` | Birim | 5 | Hayır | Token üretme ve okuma, yabancı anahtar, yabancı issuer, süresi dolmuş token, boş rol listesi |
| `MetaControllerTest` | Web slice (`@WebMvcTest`) | 2 | Hayır | `/api/v1/ping` ve `/api/v1/hello` yanıt gövdeleri |
| `PriceRadarBackendApplicationTests` | Entegrasyon (Testcontainers) | 7 | Evet | Flyway V1 ve V2, uzantılar, Redis `PING`, Elasticsearch ping, Kafka topic'leri, Kafka'ya JSON olay gönderme, cache yöneticisi, olay kaydının PostgreSQL'de kayıt ve tamamlanma döngüsü |

Entegrasyon testleri `AbstractIntegrationTest`'ten türer ve tek bir Spring context ile
tek bir container setini paylaşır. İmaj etiketleri `TestcontainersConfiguration` içinde
sabittir ve `docker-compose.yml` ile aynıdır.

### Komutlar

```bash
# Backend
cd backend
./mvnw test       # tüm testler
./mvnw verify     # testler + JaCoCo raporu (target/site/jacoco/)

# Frontend
cd frontend
npm run format:check
npm run lint
npm run typecheck   # temiz checkout'ta önce: npx next typegen
npm run build
```

### Son yerel doğrulama

22 Ekim 2026, `feat/modular-monolith` dalı, Windows 11, JDK 26.0.1 (derleme hedefi `release 25`), Docker 29.6.1, Node.js 24.18.0:

| Kontrol | Sonuç |
|---|---|
| `./mvnw -B clean verify` | 25 test, 0 hata, 0 atlanan. `BUILD SUCCESS` |
| `npm run format:check` | Başarılı |
| `npm run lint` | Başarılı |
| `npm run typecheck` | Başarılı (`.next/types` mevcutken) |
| `npm run build` | Başarılı |

### CI

Durum, `main` üzerindeki son çalıştırmalardan alınmıştır (15 Ekim 2026, e30c187).

| İş akışı | Adımlar | Son durum |
|---|---|---|
| `backend-ci.yml` | `./mvnw verify`, test raporlarını yükleme, SonarQube (token varsa), Docker imajı build | **Başarısız**: `./mvnw: Permission denied` |
| `frontend-ci.yml` | `npm ci`, format, lint, typecheck, build, SonarQube (token varsa), Docker imajı build | **Başarısız**: typecheck'te `Cannot find name 'LayoutProps'` |

İki hatanın nedeni yukarıdaki [Sorun giderme](#sorun-giderme) bölümünde. İş akışları
yalnızca ilgili dizin değiştiğinde (`backend/**`, `frontend/**`) çalışır.

### SonarQube

```bash
docker compose --profile quality up -d
# http://localhost:9000 — ilk giriş admin/admin, parola değiştirmeniz istenir.
# Proje token'ı oluşturup kabuğunuzda SONAR_TOKEN olarak tanımlayın.

cd backend
./mvnw verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=$SONAR_TOKEN
```

Sonar kapsamı `target/site/jacoco/jacoco.xml` üzerinden okur. JaCoCo
`platform/**` paketini ve uygulama giriş sınıfını kapsam dışında tutar. Frontend tarama
ayarları `frontend/sonar-project.properties` içindedir ve CI'da
`sonarqube-scan-action` ile çalışır.

## Geliştirme durumu

<p align="center">
  <img src="assets/readme/status.svg" alt="Geliştirme durumu: 13 tamamlanan altyapı başlığı, 7 planlanan başlık ve başarısız CI notu" width="100%">
</p>

**Tamamlandı (altyapı)**

- Spring Boot 4.1.1 backend iskeleti, Java 25
- Modüler monolit: 12 modül, Spring Modulith ile sınır doğrulaması ve olay kaydı
- Modül içi hexagonal katman kuralları ve mimari karar kayıtları (`docs/architecture`)
- PostgreSQL ve Flyway (V1: `pg_trgm`, `unaccent`; V2: olay kaydı tablosu)
- Redis cache yapılandırması
- Kafka topic'leri, yeniden deneme ve dead-letter topic
- Elasticsearch istemci bağlantısı
- Stateless güvenlik: JWT servisi ve filtresi
- RFC 9457 hata yanıtları, OpenAPI ve Swagger UI
- `/api/v1/ping`, `/api/v1/hello` ve frontend bağlantı sayfası
- Docker Compose: veri servisleri, `app` ve `quality` profilleri
- GitHub Actions iş akışları (backend, frontend)
- Testcontainers ile entegrasyon testleri

**Planlanan (uygulama, sıra belirlenmedi):** [Özellikler ve modüller](#planlanan) bölümündeki liste.

**Bilinen sorunlar**

- İki CI iş akışı da şu an başarısız ([CI](#ci)).
- `GlobalExceptionHandler` 401 ve 403 yanıtlarında `type` alanına `not-found` problem
  tipini yazıyor.
