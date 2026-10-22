# 0002. Modül içinde hexagonal katmanlar

- **Durum:** Kabul edildi, kuralları kuruldu
- **Tarih:** 2026-10-15

## Bağlam

Ürün eşleştirme, fiyat değişimi tespiti ve alarm koşulları projenin en değerli iş
kurallarıdır. Bu kurallar PostgreSQL'e, Kafka'ya veya Elasticsearch'e doğrudan
bağlanırsa hem test edilmeleri zorlaşır hem de altyapı değiştiğinde kuralların da
değişmesi gerekir.

## Karar

Her iş modülü Ports & Adapters düzeninde dört katmana ayrılır:

| Katman | Paket | İçerik | Bağımlı olabileceği katmanlar |
|---|---|---|---|
| Domain | `<modül>.domain` | İş modeli, değer nesneleri, kurallar, çıkış portları (ör. `ProductRepository` arayüzü) | yok |
| Application | `<modül>.application` | Kullanım senaryoları (`command/`, `query/`), giriş portları | domain |
| Infrastructure | `<modül>.infrastructure` | Çıkış adaptörleri: JPA, Elasticsearch, Kafka, dış mağaza API'leri | domain, application |
| Web | `<modül>.web` | Giriş adaptörü: REST controller'lar ve DTO'lar | application, domain |

Modülün üst paketi modül API'sidir: diğer modüllerin çağırdığı arayüzler (ör.
`CatalogQueries`) ve yayınlanan olaylar (ör. `PriceChanged`) burada durur.
Uygulamaları `application` içindedir.

### Kurallar

`HexagonalArchitectureTests` bu kuralları her build'de denetler:

1. `domain` başka hiçbir katmana bağımlı olmaz.
2. `domain` Spring, JPA/Hibernate, Kafka, Elasticsearch, Lettuce ve Jackson
   kullanmaz.
3. `application` adaptörlere ve altyapı kütüphanelerine (JPA, Spring Data
   JPA/Elasticsearch/Redis, Kafka, Elasticsearch istemcisi, Lettuce, Spring Web)
   bağımlı olmaz. Spring'in bağımlılık enjeksiyonu, `@Transactional` ve Spring Modulith olay
   anotasyonları serbesttir.
4. Modül API'si (modülün üst paketi) `web` ve `infrastructure` tiplerine bağımlı
   olmaz; diğer modüller adaptörleri hiçbir yoldan görmez.
5. `web` ile `infrastructure` birbirine bağımlı olmaz; ikisi de application
   üzerinden konuşur.
6. `@Entity` sınıfları yalnızca `infrastructure` içinde olur.
7. `@KafkaListener` metotları yalnızca `infrastructure` içinde olur.
8. `@RestController` sınıfları domain, application veya infrastructure içinde
   olmaz.

Kurallar paket adına göre çalışır ve tüm modüllere otomatik uygulanır. Boş katman
kuralı bozmaz; katman klasörü içine ilk sınıf yazıldığında açılır.

### Önerilen şablondan farklar

- **`api/` yerine `web/`.** Spring Modulith'te "API" modülün diğer modüllere açtığı
  yüzü anlatır. REST katmanına da `api` demek iki farklı kavramı aynı isimle
  karıştırır.
- **`domain/repository/` yerine `domain/port/`.** Repository arayüzleri port
  rolündedir. Mağaza API istemcisi veya bildirim gönderici gibi repository olmayan
  çıkış portları da burada durur.
- **JPA entity'leri domain modelinden ayrıdır.** Domain modeli saf Java'dır;
  `infrastructure/persistence` içindeki entity'lerle eşleme yapılır. Bu, her kalıcı
  model için bir eşleme sınıfı maliyeti getirir. Karşılığında iş kuralları veritabanı
  olmadan birim testle denenebilir.

## Sonuçlar

- İş kuralları Docker gerektirmeyen birim testlerle sınanır. Adaptörler
  Testcontainers ile ayrı sınanır.
- Mevcut altyapı kodu bu şablonun dışında kalır: `identity` modülündeki
  `JwtService` ve güvenlik zinciri, `platform` ve `system`. Bunlar iş kuralı
  içermez. `identity`, kullanıcı alanı yazılırken bu şablona geçer.
- Kurallar `HexagonalArchitectureTests` içinde ArchUnit ile yazılmıştır. ArchUnit
  Spring Modulith üzerinden zaten projeye gelir; ek bağımlılık eklenmedi.
