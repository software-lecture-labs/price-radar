# 0006. Fiyat toplama yöntemi ve ürün eşleştirme

- **Durum:** Açık, karar bekliyor
- **Tarih:** 2026-10-15

Price Radar'ın uzun vadede kaç mağazaya ölçeklenebileceğini bu karar belirler. Mimari
her iki seçeneğe de hazır (`ingestion` ve `matching` modülleri, sınırları ve olay
akışı tanımlı). Ancak ilk entegrasyon yazılmadan önce aşağıdaki sorular
cevaplanmalı.

## Fiyat toplama

| Yöntem | Artı | Eksi |
|---|---|---|
| Mağazanın resmi API'si | Yapılandırılmış, güvenilir, izinli | Her mağazanın API'si farklı; erişim anlaşma gerektirebilir |
| Ürün feed'i (XML/CSV, ör. Google Merchant formatı) | Tek formatla çok mağaza; GTIN genellikle dolu | Güncelleme sıklığı mağazaya bağlı (çoğu günlük) |
| Affiliate ağları | Çok mağazaya tek sözleşmeyle erişim | Kapsam ağa bağlı, veri gecikmeli olabilir |
| Mağazanın veriyi bize göndermesi (push) | En güncel veri | Mağazanın entegrasyon yapmasını gerektirir |
| Web sayfasından veri kazıma | Anlaşma gerektirmez | Kullanım koşulları ve hukuki risk, kırılgan, engellenebilir |

**Öneri:** İzinli kaynaklarla başlamak: önce ürün feed'leri ve resmi API'ler. Veri
kazıma yalnızca mağazanın kullanım koşulları açıkça izin veriyorsa ve hukuki görüş
alındıktan sonra değerlendirilir.

Mimari açıdan her kaynak `ingestion` içinde bir adaptördür
(`ingestion/infrastructure/<kaynak>`). Hepsi aynı normalize kayıt biçimine çevrilir;
sonraki adımlar kaynağı bilmez.

## Ürün eşleştirme

Önerilen sıra (ilk uyan karar verir):

1. **GTIN/EAN tam eşleşmesi.** Kanonik üründe aynı GTIN varsa otomatik onaylanır.
2. **Daha önce onaylanmış mağaza SKU'su.** Aynı mağazanın aynı SKU'su daha önce bir
   ürüne eşlendiyse aynı ürüne bağlanır.
3. **Marka + model + varyant özellikleri.** Renk, depolama kapasitesi gibi
   özelliklerin tamamı uyuşuyorsa yüksek güvenli aday olur. Otomatik onay eşiği
   ölçümle belirlenir.
4. **Yalnızca isim benzerliği.** Asla otomatik onaylanmaz. Aday olarak inceleme
   kuyruğuna düşer. PostgreSQL'deki `pg_trgm` uzantısı aday bulmak için hazırdır.

Hiçbir aday yoksa yeni kanonik ürün oluşturulması önerilir (otomatik mi, inceleme ile
mi: aşağıdaki sorulara bakın).

## Cevaplanması gereken sorular

1. İlk hedef mağazalar hangileri ve hangilerinin API'si ya da feed'i var?
2. Fiyatlar hangi sıklıkla güncellenmeli: saatlik mi, günlük mü, mağazaya göre mi?
3. İnceleme kuyruğuna düşen eşleşmeleri kim onaylayacak: yönetim paneli gerekecek mi?
4. GTIN'i olmayan ürünlerde yeni kanonik ürün otomatik mi oluşturulmalı?
5. Birden fazla uygulama örneği çalıştığında zamanlanmış toplama işlerinin aynı anda
   iki kez çalışmaması nasıl sağlanacak (ör. ShedLock veya veritabanı kilidi)?
6. Toplama işleri ana uygulamayla mı çalışacak, yoksa baştan ayrı bir süreç mi
   olacak?

Bu sorular cevaplandığında bu kayıt "Kabul edildi" durumuna geçer ve Aşama 2 başlar.
