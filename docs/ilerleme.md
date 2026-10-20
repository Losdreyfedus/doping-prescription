# İlerleme

## 0. aşama: Kalite altyapısı ve iskelet

**Branch:** `feat/00-quality-and-skeleton` · **Tarih:** 2026-10-20

### Yapılanlar

- Repo hijyeni: `.gitignore`, `.gitattributes` (LF zorunlu), `.editorconfig`.
- `docker-compose.yml`: PostgreSQL 16.15, Redis 7.4.11, Kafka 4.3.2 (KRaft, tek node), isteğe bağlı kafka-ui (`tools` profili). Üç servis de healthcheck ile `healthy` durumuna geliyor.
- Backend iskeleti: Spring Boot 4.1.1 ve Spring Modulith 2.1.1 BOM olarak; 10 modül paketi (`shared` OPEN); `Clock` bean'i `prescription.timezone` ile.
- `GET /actuator/health`: db, redis ve özel `KafkaHealthIndicator` ile kafka. Hem Testcontainers entegrasyon testinde hem gerçek Compose yığınında `UP` doğrulandı.
- Kalite kapıları `verify` fazında: Spotless (palantir), Checkstyle, SpotBugs, JaCoCo (`domain` + `application` %80), Spring Modulith `verify()`, ArchUnit.
- Frontend iskeleti: React 19, Vite 8, TypeScript 6.0 strict, Tailwind 4 ve tüm tasarım değişkenleri, şirket logosu, ESLint/Prettier/Vitest, `npm run check`.
- GitHub Actions: `core` ve `ui` job'ları.

### Kabul kriterleri

| Kriter | Durum |
| --- | --- |
| Kurulum komutları temiz bir makinede çalışır | Bu makinede doğrulandı: `docker compose up -d`, `./mvnw verify`, `npm ci && npm run check`. `demo` profili henüz seed yüklemiyor (1. aşama). |
| Health endpoint'i postgres, redis ve kafka durumunu raporlar | Geçti (`HealthEndpointIT` + jar ile Compose'a karşı elle kontrol) |
| CI yeşil | Henüz çalıştırılmadı: branch push edilmedi |
| `ApplicationModules.verify()` boş modüllerle geçer | Geçti; ayrıca 10 modülün tamamının algılandığı test ediliyor |

### Öz-değerlendirme (docs/kod-standartlari.md §7)

**Mimari**
- [x] Modül sınırları: modüllerde henüz kod yok; `verify()` ve ArchUnit kuralları ilk koddan itibaren devrede.
- [x] Domain saflığı: ArchUnit kuralı hazır (`allowEmptyShould` ile boşken de etkin).
- [x] Controller'da iş kuralı yok (henüz controller yok).
- [ ] Senkron/asenkron seçimi: bu aşamada uygulanabilir değil.

**Doğruluk**
- [ ] Transaction, outbox, idempotency, eşzamanlılık, sahiplik, `correct_key`: bu aşamada uygulanabilir değil.

**Performans**
- [ ] Bu aşamada uygulanabilir değil.

**Okunabilirlik**
- [x] Sihirli sayı yok: Kafka health timeout'u config'de (`prescription.health.kafka.timeout`), Checkstyle `MagicNumber` açık.
- [x] Kısa metotlar, iç içe koşul en fazla iki seviye (Checkstyle zorluyor).

**Test**
- [x] Yeni davranışın testi aynı commit'te (Clock, Kafka health, health endpoint).
- [x] Hata yolu test edildi: erişilemeyen broker için DOWN ve timeout süresi; eksik timezone ile uygulamanın başlamaması.
- [x] Testler deterministik; zaman ölçen tek test geniş bir üst sınır (5 sn) kullanıyor.

**Frontend**
- [x] `any`, tip zorlaması ve `@ts-ignore` ESLint'te hata; kural çalıştığı geçici bir dosyayla doğrulandı.
- [x] Özellik klasörleri arası import yasağı doğrulandı.
- [x] Metinler `shared/i18n/tr.ts` içinde.
- [ ] Yükleme, boş ve hata durumları: henüz veri ekranı yok.

**Teslim**
- [x] Commit'ler küçük ve Conventional Commits biçiminde.
- [x] Kararlar ADR 0005'te; bu aşamanın kararları aşağıda.
- [x] README iskeleti ve bu not güncel.

### Bu aşamada alınan kararlar ve sapmalar

- **Spring Boot parent yerine BOM import:** Boot ve Modulith sürümleri pom'da property olarak tutuluyor. Bunun bedeli, parent'ın sağladığı şeylerin (eklenti sürümleri, `-parameters`, `repackage`, failsafe `classesDirectory`) açıkça yazılması oldu.
- **Kafka health indicator'ı `shared.messaging` içinde:** ADR 0005'teki "event zarfı" alanını "event/mesajlaşma altyapısı" olarak yorumladım.
- **TypeScript 6.0 ve ESLint 9:** typescript-eslint TypeScript'i < 6.1 ile, jsx-a11y ise ESLint'i 9 ile sınırlıyor.
- **ArchUnit 1.4.2:** Hiçbir BOM yönetmiyor; Modulith'in getirdiği sürümle hizalandı.
- **Commit sırası:** Docker motoru başta çalışmadığı için health commit'i (planda 4.) kalite kapıları, ArchUnit ve UI commit'lerinden sonra atıldı.
- **JDK:** Bu makinede Oracle JDK 21.0.12 kurulu; CI Temurin 21 kullanıyor.

### TODO(spec) notları

1. `shared/time/TimeProperties.java`: algoritma.md §9 tek bir `PrescriptionProperties` record'u tarif ediyor. Modül sınırlarını korumak için her modülün kendi `prescription.*` grubunu bağlamasını öneriyorum; 1. aşamadan önce karar gerekiyor.
2. `application.yml`: health ayrıntıları (`show-details: always`) herkese açık; 2. aşamada Spring Security ile ADMIN'e kısıtlanmalı.
