# 0001. Modüler monolit ve modül sınırları

- **Durum:** Kabul edildi, kuruldu
- **Tarih:** 2026-10-15

## Bağlam

Price Radar birbirinden farklı hızlarda değişen iş alanlarından oluşur: ürün kataloğu,
mağazalar, fiyatlar, veri toplama, eşleştirme, arama, kullanıcılar, alarmlar. Ekip
küçük, trafik henüz yok ve hangi alanın bağımsız ölçeklenmesi gerekeceği bilinmiyor.

Her alanı ilk günden ayrı mikroservis yapmak dağıtık işlem, servisler arası sözleşme,
servis keşfi ve ayrı deploy hattı maliyetini şimdiden getirir. Sınırsız tek bir
uygulama ise zamanla her şeyin her şeye bağlı olduğu bir yapıya dönüşür.

## Karar

Backend tek bir Spring Boot uygulaması olarak kalır ve **Spring Modulith 2.1.1** ile
modüllere ayrılır.

- `com.priceradar` altındaki her doğrudan alt paket bir modüldür. Araya `modules/`
  gibi bir ara paket konmaz: Spring Modulith modülleri uygulama sınıfının doğrudan alt
  paketlerinden algılar; ara paket bütün modülleri tek modül yapar.
- Modülün API'si üst paketindeki public tiplerdir. Alt paketler iç ayrıntıdır.
- Her modül bağımlı olabileceği modülleri `package-info.java` içinde
  `@ApplicationModule(allowedDependencies = ...)` ile beyan eder.
- `shared` paylaşılan modüldür (`@Modulithic(sharedModules = "shared")`); her modül
  listelemeden kullanabilir. Yalnızca iş anlamı taşımayan ortak tipler içerir.
- Bir modül başka modülün tablosuna, JPA repository'sine veya iç sınıfına erişmez.
  Modüller ya karşı modülün API'sindeki arayüzü çağırır ya da yayınladığı olayı dinler.

### Modüller

| Modül | Sorumluluk | Bağımlı olabileceği modüller |
|---|---|---|
| `catalog` | Kanonik ürünler, markalar, kategoriler, özellikler | yok |
| `merchant` | Mağazalar ve mağaza ürün kayıtları | yok |
| `pricing` | Fiyatlar, indirimler, fiyat geçmişi | yok |
| `ingestion` | Mağaza entegrasyonları, veri toplama, normalizasyon | `merchant`, `pricing` |
| `matching` | Mağaza kaydını kanonik ürüne eşleme | `catalog`, `merchant` |
| `search` | Elasticsearch indeksi ve arama | `catalog`, `merchant`, `matching`, `pricing` |
| `identity` | Kullanıcılar, oturumlar, yetkilendirme | yok |
| `alert` | Fiyat alarmları ve bildirimler | `pricing`, `matching`, `identity` |
| `favorites` | Favoriler ve takip listeleri | `catalog` |
| `shared` | Ortak hata tipleri ve problem+json | yok (herkese açık) |
| `platform` | Cache, Kafka, OpenAPI yapılandırması | yok |
| `system` | `/api/v1/ping`, `/api/v1/hello` | yok |

Bağımlılık yönünün gerekçesi:

- **`catalog`, `merchant`, `pricing` hiçbir iş modülüne bağımlı değil.** Esas veri
  bunlarda; başka modüllerin değişmesi bunları etkilemez. Birbirlerine yalnızca
  kimlik (ID) değeriyle atıf yaparlar.
- **`merchant` kanonik ürünü bilmez.** Hangi mağaza kaydının hangi ürün olduğuna
  `matching` karar verir. Böylece eşleştirme yöntemi değişse de `merchant` değişmez.
- **`ingestion` yazar ama sahip değildir.** Mağaza kaydını `merchant` API'si, fiyat
  gözlemini `pricing` API'si üzerinden yazar.
- **`search` ve `alert` uçtaki okuyuculardır.** Diğer modüllerin olaylarını dinler,
  kimse onlara bağımlı değildir.

Önerilen ilk listeye göre iki uyarlama yapıldı:

- `config/` yerine mevcut `platform` modülü kullanılır; Spring Modulith'te o da bir
  modüldür ve sınırları aynı şekilde denetlenir.
- Fiyat geçmişi ayrı modül değil, `pricing` içindedir.

## Sonuçlar

- `ModularityTests` her build'de döngüleri, iç pakete erişimi ve beyan edilmemiş
  bağımlılıkları yakalar. Kural ihlali build'i kırar.
- Uygulama tek parça deploy edilir. Bir modül ayrı süreç olarak çalıştırılmak
  istendiğinde sınırları zaten çizilmiştir; modülün olayları Kafka'ya
  dışsallaştırılır ([ADR 0003](0003-event-driven-integration.md)).
- Ürün detay sayfası gibi birden fazla modülün verisini birleştiren okumaların nerede
  yapılacağı açık bir sorudur. Seçenekler: `search` okuma modelinin bu veriyi de
  taşıması ([ADR 0004](0004-cqrs-lite.md)) ya da web katmanında birleştirme yapan
  ayrı bir okuma modülü. İlk ürün detay ucu yazılırken karar verilecek.
- Kubernetes, API Gateway ve servis keşfi eklenmez.
