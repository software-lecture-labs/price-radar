# Price Radar

Farklı satıcılardaki ürün fiyatlarını tek yerde toplayıp karşılaştırmayı amaçlayan
bir web uygulaması. Backend Spring Boot, frontend Next.js ile yazılıyor.

## Çalıştırma

Java 25, Node.js 24 ve Docker gerekir.

Proje kökünden PostgreSQL, Redis, Elasticsearch ve Kafka'yı başlat:

```bash
docker compose up -d
```

`.env` dosyası oluşturduysan `POSTGRES_PASSWORD` değerini `price_radar` yap. Backend
`.env` dosyasını okumaz, veritabanına varsayılan `price_radar` parolasıyla bağlanır.

Backend'i başlat:

```powershell
# Windows (PowerShell)
cd backend
.\mvnw.cmd spring-boot:run
```

```bash
# macOS / Linux
cd backend
./mvnw spring-boot:run
```

İkinci terminalde frontend'i başlat:

```powershell
# Windows (PowerShell)
cd frontend
npm.cmd ci
npm.cmd run dev
```

```bash
# macOS / Linux
cd frontend
npm ci
npm run dev
```

Frontend `http://localhost:3000`, backend `http://localhost:8080` adresindedir.

## Testler

Backend'in entegrasyon testleri Testcontainers kullanır, Docker açık olmalı:

```powershell
# Windows (PowerShell)
cd backend
.\mvnw.cmd verify
```

```bash
# macOS / Linux
cd backend
./mvnw verify
```

## Dal akışı

`dev` entegrasyon dalı, `main` teslim edilen sürümdür.

- Her iş `dev`'den açılan ayrı bir dalda yapılır (`feat/...`, `fix/...`, `chore/...`,
  `docs/...`) ve PR ile `dev`'e merge edilir.
- `dev` çalışır ve testleri geçer durumdayken `dev → main` PR'ı ile `main`'e alınır.
- `dev` ile `main`'i eşitleyen PR'lar (`dev → main`, `main → dev`) "Create a merge commit" ile
  birleştirilir, squash ile değil. Squash dalın commit'lerini diğer dalın geçmişine taşımaz;
  bir sonraki eşitleme PR'ında aynı değişiklikler tekrar gelir ve conflict çıkar.
- `main` ve `dev`'e GitHub web arayüzünden de olsa doğrudan commit atılmaz. Her
  değişiklik PR ile girer ve en az bir onay ister.
- Bir PR tek bir işi kapsar. Birbiriyle ilgisi olmayan değişiklikler ayrı PR'larda açılır.
