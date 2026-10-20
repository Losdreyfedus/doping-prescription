# ADR 0004: OpenAPI ile backend–frontend sözleşmesi

- **Durum:** Kabul edildi
- **Tarih:** 2026-10-20

## Bağlam

Backend Java, frontend TypeScript. DTO'ların iki tarafta elle tekrar yazılması zamanla birbirinden kopan tiplere ve çalışma zamanı hatalarına yol açar.

## Karar

- Backend, springdoc-openapi ile OpenAPI 3 belgesini koddan üretir. Request/response DTO'ları Java record'larıdır ve Bean Validation anotasyonları taşır.
- Belge `doping-prescription-ui/openapi.json` olarak repoya kaydedilir (`./mvnw verify` sırasında bir test, üretilen belgenin repodakiyle aynı olduğunu kontrol eder).
- Frontend tipleri `openapi-typescript` ile bu dosyadan üretilir (`npm run gen:api`); elle tip yazılmaz.

## Sonuçlar

- API değişikliği frontend'de derleme hatası olarak görünür.
- Sözleşme değişiklikleri pull request'te `openapi.json` farkı olarak incelenebilir.
