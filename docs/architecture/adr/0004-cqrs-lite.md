# 0004. CQRS Lite: PostgreSQL yazma, Elasticsearch okuma

- **Durum:** Kabul edildi, uygulanmadı
- **Tarih:** 2026-10-15

## Bağlam

Kullanıcı tarafındaki yük büyük ölçüde okumadır: metin araması, kategori ve özellik
filtreleri, fiyata göre sıralama. Bu sorgular ilişkisel şemada pahalıdır ve
Elasticsearch için doğal iştir. Öte yandan fiyat geçmişi ve eşleştirme kararları
işlem güvencesi ister ve tek bir doğru kaynağa ihtiyaç duyar.

Ayrı olay deposu, ayrı okuma veritabanları ve olay kaynaklı (event-sourced) modeller
içeren tam CQRS bu aşamada gereğinden ağırdır.

## Karar

- **PostgreSQL esas veri kaynağıdır.** Ürünler, mağaza kayıtları, eşleştirmeler,
  fiyat gözlemleri, kullanıcılar ve alarmlar yalnızca burada yazılır.
- **Elasticsearch yeniden oluşturulabilir bir okuma modelidir.** Arama ve listeleme
  buradan okunur. İndeks olaylarla güncel tutulur
  ([ADR 0003](0003-event-driven-integration.md)) ve her an PostgreSQL'den baştan
  kurulabilir. Elasticsearch'te bulunup PostgreSQL'de olmayan bir veri yoktur.
- **İndeks sürümlü tutulur.** Uygulama bir alias üzerinden okur
  (`price-radar-products`). Yeniden kurulum yeni bir indekse yapılır
  (`price-radar-products-v2`), tamamlanınca alias çevrilir. İndeks adlarını
  `SearchProperties.indexFor` üretir.
- **Ürün detayı ve güncel teklifler PostgreSQL'den okunur.** Fiyatın anlık doğru
  olması gereken yerde indeksteki gecikme kabul edilmez.
- **Komut ve sorgu kodu modül içinde ayrılır.** `application/command` yazar,
  `application/query` okur. Ayrı veritabanı veya ayrı servis yoktur.
- **Redis önbelleği okuma tarafındadır ve veri türüne göre ayarlanır.** Tek bir 10
  dakikalık TTL yerine cache başına TTL tanımlanır. İlk öneri:

  | Cache | TTL | Geçersizleştirme |
  |---|---|---|
  | Kategori ağacı | 1 saat | Kategori değişikliği olayıyla |
  | Ürün detayı | 10 dakika | Ürün değişikliği olayıyla |
  | Güncel teklifler ve fiyatlar | 1 dakika | `PriceChanged` olayıyla |
  | Arama sonuçları | Önbelleğe alınmaz | Elasticsearch zaten hızlı; filtre kombinasyonu çok |

  Değerler ölçümle güncellenecek. TTL'ler `CacheConfig` içinde
  `RedisCacheManagerBuilderCustomizer` ile tanımlanacak.

## Sonuçlar

- Arama sonuçları PostgreSQL'in birkaç saniye gerisinde olabilir. Arayüz fiyatı
  detay sayfasında PostgreSQL'den gösterdiği için kullanıcı satın alma kararını
  güncel fiyatla verir.
- İndeksi baştan kuran bir yönetim işi gerekir. Aşama 3'te yazılacak.
- Elasticsearch kaybedilirse veri kaybı olmaz; yalnızca arama, indeks yeniden
  kurulana kadar çalışmaz.
