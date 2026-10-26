# ADR 0008: Uygulama katmanının okuma modelleri API yanıtıdır

- **Durum:** Kabul edildi
- **Tarih:** 2026-10-23

## Bağlam

AGENTS.md §4 eşleme için MapStruct'ı, `docs/kod-standartlari.md` §2 ve `docs/mimari.md` §2 ise controller'ların sonucu "MapStruct ile response DTO'ya çevirmesini" öngörüyordu. Uygulamada servisler entity döndürmüyor: her modülün `application` paketi, entity'lerden derlenmiş değişmez okuma modelleri (`ExamViews`, `FeedViews`, `QuizViews`, profil ve admin görünümleri) üretiyor ve controller'lar bunları doğrudan döndürüyor. MapStruct yalnızca `auth` modülünde, entity'den DTO'ya eşleme gereken tek yerde kullanılıyor. Sonuç olarak OpenAPI açıklamaları (`@Schema`) `application` paketindeki record'larda duruyor.

## Karar

- `application` paketindeki okuma modeli record'ları API yanıt sözleşmesidir. Controller bunları dönüştürmeden döndürür; aynı alanlara sahip ikinci bir web DTO'su ve aradaki MapStruct eşlemesi yazılmaz.
- MapStruct, entity'den DTO'ya eşleme gereken yerde (bugün `auth`) kullanılır. Entity'ler hiçbir zaman API'den dışarı çıkmaz; bu kural değişmez.
- Request DTO'ları web katmanında kalır (Bean Validation anotasyonlarıyla).
- `@Schema` anotasyonlarının `application` record'larında bulunması kabul edilir; bunlar yalnızca belge üretir, davranış değiştirmez.

## Değerlendirilen seçenekler

- **Her modülde web DTO'su + MapStruct:** Katman ayrımı daha keskin olur, ama bugün her record'un alan alan bir kopyası yazılır ve OpenAPI sözleşmesi (ADR 0004) aynı kalır. Kod miktarı artar, davranış değişmez.
- **Servislerin entity döndürmesi ve controller'da MapStruct:** Entity'leri modül sınırının dışına (web katmanına) taşır ve tembel yükleme (lazy loading) hatalarına açık bırakır.

## Sonuçlar

- Bir modül ileride ayrı servise çıkarılırsa ya da API sözleşmesi iç modelden ayrışmak zorunda kalırsa, o modüle web DTO'ları eklenir; OpenAPI sözleşme testi (`OpenApiContractIT`) farkı gösterir.
- `docs/kod-standartlari.md` §2 ve `docs/mimari.md` §2 bu karara göre güncellendi.
