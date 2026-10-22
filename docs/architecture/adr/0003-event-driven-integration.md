# 0003. Modüller arası olaylar ve transactional outbox

- **Durum:** Kabul edildi, olay kaydı kuruldu; Kafka'ya dışsallaştırma ilk dış tüketiciyle eklenecek
- **Tarih:** 2026-10-15

## Bağlam

Bir fiyat değiştiğinde arama indeksi, fiyat geçmişi görünümü ve alarmlar
güncellenmelidir. Bunları fiyatı yazan istek içinde yapmak isteği yavaşlatır ve
`pricing` modülünü bu modüllerin hepsine bağımlı kılar.

Olayı veritabanı işleminden ayrı göndermek ise tutarsızlık yaratır: fiyat
PostgreSQL'e yazılıp olay gönderilemezse arama indeksinde eski fiyat kalır. Olay
gönderilip işlem geri alınırsa da var olmayan bir fiyat için bildirim gider.

## Karar

**Modüller arası yan etkiler olayla tetiklenir.** Olayı yayınlayan modül, tüketen
modülleri bilmez.

**Outbox olarak Spring Modulith olay kaydı kullanılır.** Ayrı bir outbox tablosu ve
yayınlayıcı yazılmaz.

- Olay, yayınlandığı veritabanı işlemiyle aynı işlemde `event_publication`
  tablosuna yazılır (`V2__event_publication.sql`).
- Dinleyici `@ApplicationModuleListener` ile tanımlanır. Bu, işlem commit'lendikten
  sonra, ayrı bir thread'de ve kendi işleminde çalışır; API isteği beklemez.
- Dinleyici başarıyla bittiğinde kayıt tamamlandı olarak işaretlenir. Hata alan
  veya uygulama kapanırken yarıda kalan olaylar tabloda kalır ve
  `IncompleteEventPublications` ile yeniden gönderilir.

**Teslim en az bir kezdir (at-least-once).** Aynı olay iki kez işlenebilir. Bu
yüzden her dinleyici idempotent yazılır: olayın kimliğini veya doğal anahtarını
(ör. listing + gözlem zamanı) kontrol eder, ikinci işlemede sonucu değiştirmez.

**Olay sözleşmeleri modül API'sindedir.** Olaylar yayınlayan modülün üst paketinde
değişmez `record` olarak tanımlanır. Adı geçmiş zaman kipindedir (`PriceChanged`,
`ListingMatched`). Yalnızca tüketicinin ihtiyaç duyduğu alanları ve kimlikleri
taşır; JPA entity'si taşımaz.

**Kafka, süreç sınırını aşan olaylar içindir.** Uygulama içindeki modüller olayları
doğrudan kayıt üzerinden alır. Bir tüketici ayrı bir süreçte çalışmaya başladığında
(ör. arama indeksleyici veya bildirim işçisi) ilgili olay `@Externalized` ile
Kafka'ya dışsallaştırılır. Bunun için o gün `spring-modulith-events-kafka`
eklenir. Dışsallaştırma da aynı kayıt üzerinden yapıldığı için outbox garantisi
korunur.

**Topic'ler olay türüne göre ayrılır.** Mevcut genel `price-radar.events` topic'i
yalnızca altyapı denemesi içindir. Dışsallaştırılan her olay türü kendi topic'ini
alır (`price-radar.<modül>.<olay>`, ör. `price-radar.pricing.price-changed`).
Anahtar, sıranın korunması gereken varlığın kimliğidir (ör. listing kimliği). Mevcut
yeniden deneme ve dead-letter yapılandırması (`KafkaConfig`) tüm tüketicilere
uygulanır.

## Sonuçlar

- Olay kaydı ve tamamlanma döngüsü PostgreSQL üzerinde entegrasyon testiyle
  doğrulanıyor (`PriceRadarBackendApplicationTests`).
- Tamamlanan kayıtlar varsayılan olarak tabloda kalır (`completion-mode: update`).
  Tablo büyüdükçe eski kayıtların temizlenmesi gerekecek; `CompletedEventPublications`
  ile zamanlanmış bir temizlik işi Aşama 4'te eklenecek.
- Uygulama açılırken yarıda kalan olayları otomatik yeniden gönderme kapalıdır
  (varsayılan). Birden fazla uygulama örneği aynı olayı yeniden gönderebileceği için
  yeniden gönderimin nasıl tetikleneceğine Aşama 4'te karar verilecek.
