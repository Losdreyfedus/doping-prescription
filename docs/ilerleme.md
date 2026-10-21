# İlerleme

## 0. aşama: Kalite altyapısı ve iskelet

**Branch:** `feat/00-quality-and-skeleton` · **Tarih:** 2026-10-20

### Yapılanlar

- Repo hijyeni: `.gitignore`, `.gitattributes` (LF zorunlu), `.editorconfig`.
- `docker-compose.yml`: PostgreSQL 16.15, Redis 7.4.11, Kafka 4.3.2 (KRaft, tek node), isteğe bağlı kafka-ui (`tools` profili). Üç servis de healthcheck ile `healthy` durumuna geliyor.
- Backend iskeleti: Spring Boot 4.1.1 (`spring-boot-starter-parent`) ve Spring Modulith 2.1.1 (BOM); 10 modül paketi (`shared` OPEN); `Clock` bean'i `prescription.timezone` ile.
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

- **`spring-boot-starter-parent`:** İlk denemede Boot BOM olarak import edildi; parent'ın sağladığı eklenti sürümlerini ve ayarları elle tekrar yazmak gerektiği için parent'a dönüldü. pom'da yalnızca gerçek özelleştirmeler var: Modulith BOM, toolchain enforcer kuralı ve parent'ın yönetmediği kalite eklentileri. Çalıştırılabilir jar elle doğrulandı.
- **Kafka health indicator'ı `shared.messaging` içinde:** ADR 0005'e `shared`'ın beşinci alanı olarak "mesajlaşma altyapısı (Kafka yapılandırması ve health)" eklendi.
- **Config modül başına:** Her modül kendi `prescription.<modül>.*` grubunu kendi `@ConfigurationProperties` record'uyla bağlar (algoritma.md §9). `shared` altyapı anahtarları (`prescription.timezone`, `prescription.health.*`) modül adı taşımaz.
- **TypeScript 6.0 ve ESLint 9:** typescript-eslint TypeScript'i < 6.1 ile, jsx-a11y ise ESLint'i 9 ile sınırlıyor.
- **ArchUnit 1.4.2:** Hiçbir BOM yönetmiyor; Modulith'in getirdiği sürümle hizalandı.
- **Commit sırası:** Docker motoru başta çalışmadığı için health commit'i (planda 4.) kalite kapıları, ArchUnit ve UI commit'lerinden sonra atıldı.
- **JDK:** Doğrulamalar Temurin 21.0.12 ile yapıldı; CI da Temurin 21 kullanıyor.

### TODO(spec) notları

Açık `TODO(spec)` notu yok. Config yapısı (eski 1. not) karara bağlandı.

### Açık işler (sonraki aşamalara devredilen)

- **Health ayrıntıları ADMIN'e bağlanmalı (2. aşama):** `application.yml` şu an `show-details: when-authorized` ve `roles: ADMIN`. Spring Security henüz olmadığı için kimse yetkili değil; herkese yalnızca toplam durum (`status`) görünür. 2. aşamada Security yapılandırılınca ADMIN token'ıyla ayrıntıların göründüğü, öğrenci token'ıyla görünmediği testlenmeli ve `HealthEndpointIT`'deki `show-details=always` override'ı kaldırılmalı.
