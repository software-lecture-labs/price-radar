# Price Radar

Farklı satıcılardaki ürün fiyatlarını tek yerde toplayıp karşılaştırmayı amaçlayan
bir web uygulaması. Backend Spring Boot, frontend Next.js ile yazılıyor.

Java 25 ve Node.js 24 gerekir. Backend `.env` dosyasındaki Neon bağlantısını
kullanır; yerel PostgreSQL kurulumu gerekmez.

## Veritabanı

Ekipte herkes kendi Neon branch'ini kullanır; aynı veritabanına birden fazla kişi
bağlanmaz. Flyway, backend her açıldığında bekleyen migration'ları uygular. Ortak
bir veritabanında iki kişi aynı sürüm numarasıyla farklı migration yazarsa ya da
henüz merge edilmemiş bir migration değişirse, diğerlerinin backend'i Flyway
doğrulama hatasıyla açılmaz veya şema başkasının dalına göre değişir.

1. Neon konsolunda projenin ana branch'inden kendine bir branch aç
   (Branches → New branch, ör. `dev-adin`).
2. Bu branch'in bağlantı bilgilerini `.env` dosyana yaz.

Entegrasyon testleri için bundan da ayrı bir branch kullan (bkz. [Testler](#testler)).

## Çalıştırma

`.env` dosyan yoksa `.env.example` dosyasını `.env` olarak kopyala.
`POSTGRES_URL`, `POSTGRES_USER` ve `POSTGRES_PASSWORD` değerlerini kendi
Neon branch'ine göre düzenle.

Proje kökünden backend'i başlat:

```powershell
# Windows (PowerShell)
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run-backend.ps1
```

```bash
# macOS / Linux
./tools/run-backend.sh
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

Birim testleri veritabanı veya ağ bağlantısı gerektirmez:

```powershell
# Windows (PowerShell)
cd backend
.\mvnw.cmd test
```

```bash
# macOS / Linux
cd backend
./mvnw test
```

Entegrasyon testleri için uygulama veritabanından ayrı bir Neon test branch'i veya
veritabanı kullan. Eklentileri yalnızca bu test veritabanında hazırla:

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
```

`backend` dizinindeki terminalde test bağlantısını ayarla:

```powershell
# Windows (PowerShell)
$env:TEST_POSTGRES_URL = 'jdbc:postgresql://YOUR_TEST_HOST.neon.tech/neondb?sslmode=require'
$env:TEST_POSTGRES_USER = 'YOUR_TEST_USER'
$env:TEST_POSTGRES_PASSWORD = 'YOUR_TEST_PASSWORD'
.\mvnw.cmd -Pintegration verify
```

```bash
# macOS / Linux
export TEST_POSTGRES_URL='jdbc:postgresql://YOUR_TEST_HOST.neon.tech/neondb?sslmode=require'
export TEST_POSTGRES_USER='YOUR_TEST_USER'
export TEST_POSTGRES_PASSWORD='YOUR_TEST_PASSWORD'
./mvnw -Pintegration verify
```

CI bu testleri her PR'da runner üzerinde açılan geçici bir PostgreSQL ile
çalıştırır; bunun için secret gerekmez.

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
