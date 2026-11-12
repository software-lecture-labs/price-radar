# Price Radar

Java 25 ve Node.js 24 gerekir. Backend mevcut `.env` dosyasındaki Neon bağlantısını
kullanır; yerel PostgreSQL kurulumu gerekmez.

`.env` dosyan yoksa `.env.example` dosyasını `.env` olarak kopyala.
`POSTGRES_URL`, `POSTGRES_USER` ve `POSTGRES_PASSWORD` değerlerini kendi
Neon bağlantına göre düzenle.

Proje kökünden backend'i başlat:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run-backend.ps1
```

İkinci terminalde frontend'i başlat:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Frontend `http://localhost:3000`, backend `http://localhost:8080` adresindedir.
Birim testleri veritabanı veya ağ bağlantısı gerektirmez:

```powershell
cd backend
.\mvnw.cmd test
```

Entegrasyon testleri için uygulama veritabanından ayrı bir Neon test branch'i veya
veritabanı kullan. Eklentileri yalnızca bu test veritabanında hazırla:

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
```

`backend` dizinindeki terminalde test bağlantısını ayarla:

```powershell
$env:TEST_POSTGRES_URL = 'jdbc:postgresql://YOUR_TEST_HOST.neon.tech/neondb?sslmode=require'
$env:TEST_POSTGRES_USER = 'YOUR_TEST_USER'
$env:TEST_POSTGRES_PASSWORD = 'YOUR_TEST_PASSWORD'
.\mvnw.cmd -Pintegration verify
```
