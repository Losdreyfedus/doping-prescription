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

## 1. aşama: Veri modeli ve seed

**Branch:** `feat/01-data-model-and-seed` · **Tarih:** 2026-10-21

### Yapılanlar

- **Şema (V1–V9):** 18 iş tablosu ve Modulith'in `event_publication` outbox tablosu; her modülün tabloları ayrı migration'da. Modüller arası FK yok (ADR 0005), enum'lar `CHECK` kısıtlı, sorgulanan `*_id` kolonları indeksli.
- **Veritabanı güvenceleri:** tek `IN_PROGRESS` deneme (kısmi unique indeks), `exam_attempt` / `learner_topic_mastery` / `points_streak` için `@Version`, aynı soruya ikinci cevap / aynı puan / aynı sinyal için unique kısıtlar, ustalık için `double precision` + 0–1 `CHECK`.
- **Entity'ler:** her modülün `persistence` paketinde; birbirine id ile referans veriyor (JPA ilişkisi yok); zaman damgaları `Clock`'tan.
- **Etiketleme:** `TextNormalizer` ve `KeywordTagger` saf Java (kelime başı eşleşmesi, ünsüz yumuşaması, kısa token'larda tam eşleşme, şapkalı ünlüler); `ContentCreated` modül içi event'i ve asenkron `ContentTagging` dinleyicisi.
- **Seed:** `shared.api.DemoDataLoader` + `admin` modülündeki `DemoDataSeeder` (yalnızca `demo` profili). 4 kullanıcı, 10 konu, 34 anahtar kelime, 40 video (36 LIVE + 4 otomatik etiketlenen), 48 mikro-soru, 2 mini deneme (24 soru). Upsert kuralları docs/seed-verisi.md'deki gibi.
- **Soru doğrulama:** her sorunun cevaptan bağımsız `check` ifadesi, test tarafında tam kesirli bir hesaplayıcıyla hesaplanıyor; doğru şıkla eşitlik ve diğer şıklardan farklılık kontrol ediliyor. Hesaplayıcının kendi birim testleri var; bozuk bir cevap anahtarıyla testin kırıldığı doğrulandı.
- **Test altyapısı:** container'lar yalnızca ihtiyaç duyulunca başlıyor; veritabanı testleri yalnızca PostgreSQL kullanıyor.

### Kabul kriterleri

| Kriter | Durum |
| --- | --- |
| Migration'lar Testcontainers üzerinde temiz veritabanına uygulanır | Geçti (`MigrationIT`, `SchemaConventionsIT`; Hibernate `validate` dahil) |
| Seed tekrar çalıştırıldığında veriyi çoğaltmaz | Geçti (`DemoSeedIT` ve yükleyici başına IT'ler; Compose'a karşı iki başlatmada elle de doğrulandı) |
| Seed sonrası 10 konu, 40 video, 2 mini deneme ve 4 kullanıcı | Geçti (`DemoSeedIT`) |

### Öz-değerlendirme (docs/kod-standartlari.md §7)

**Mimari**
- [x] Modül sınırları: her modül yalnızca kendi tablolarına yazıyor (seed dahil); `verify()` ve ArchUnit geçiyor. `admin` seed'i yalnızca `shared.api` arayüzü üzerinden yönetiyor.
- [x] Domain saflığı: etiketleme, `AttemptScore`, `ServedCard` vb. Spring/JPA/Jackson içermiyor.
- [x] Controller yok (bu aşamada API yok).
- [x] Senkron/asenkron: içerik etiketleme modül içi asenkron event ile; Kafka henüz kullanılmıyor.

**Doğruluk**
- [x] Event'ler outbox üzerinden: `ContentCreated` Modulith event publication registry'ye yazılıyor.
- [x] Tüketiciler idempotent: etiketleme yalnızca `DRAFT` videoda çalışıyor; tekrar oynatılan event'in sonucu değiştirmediği test edildi.
- [x] Eşzamanlılık: kilitleme ve unique kısıtlar persistence testleriyle doğrulandı.
- [ ] Kaynak sahipliği ve `correct_key`'in sızmaması: API olmadığı için 2.–6. aşamalarda.

**Performans**
- [x] Sorgulanan kolonlarda indeks (otomatik kontrol). N+1 için entity'ler arası JPA ilişkisi kullanılmadı.

**Okunabilirlik**
- [x] Sihirli sayı yok: etiketleme eşiği config'de (`prescription.content.tagging.min-confidence`), algoritma sabitleri isimli ve algoritma.md'ye referanslı.
- [x] Alan dili isimlerde: `CandidatePool`, `ServedCard`, `ScoreBreakdown`, `KeywordTagger`, `TopicMastery`.

**Test**
- [x] Her davranışın testi aynı commit'te; hata yolları (kısıt ihlalleri, geçersiz config, bozuk ifade) test edildi.
- [x] Deterministik: sabit zaman damgaları; paylaşılan veritabanında testler kendi verisini temizliyor ya da seed verisine dayanıklı.
- [x] Arka arkaya 3 tam `verify` yeşil (container başlatma düzeltmesinden sonra).

**Teslim**
- [x] Küçük, Conventional Commits biçiminde commit'ler; şema kararları dokümanlara işlendi.

### Bu aşamada alınan kararlar ve sapmalar

- **Ünsüz yumuşaması:** onaylanan kelime başı kuralı algoritma.md'nin kendi örneğini (`olasilik` → "olasılığın") karşılamıyordu; p/t/k → b/d/g kuralı eklendi ve dokümana işlendi.
- **Şapkalı ünlüler:** `â, î, û` da normalize ediliyor ("kâr" bölünmesin diye).
- **`points_ledger.created_at` uygulamadan:** günlük puan sınırı `Clock`'a göre hesaplanabilsin diye.
- **Modulith Documenter hatası:** Javadoc'ta çift tırnak varsa modül dokümanı üretimi çöküyor; Javadoc'larda çift tırnak kullanılmıyor.
- **SpotBugs istisnaları:** JPA entity'leri için `CT_CONSTRUCTOR_THROW`, `URF_UNREAD_FIELD`, `UWF_UNWRITTEN_FIELD` yalnızca `persistence` paketlerinde, gerekçeli.
- **Seed'deki editör id'si:** content seed'i, onaylayan admin için users.json'daki sabit id'yi kullanıyor (content modülü auth tablosunu okuyamaz).

### TODO(spec) notları

Açık `TODO(spec)` notu yok.

### Açık işler

- Health ayrıntılarının ADMIN'e bağlanması (2. aşama, yukarıda).
- Persona'lar (`WEAK_PROBLEMS`, `SCRIPTED_EXAM`, `SCRIPTED_IN_PROGRESS`) 7. aşamada; şu an tüm kullanıcılar boş durumda seed'leniyor.
- Seed sorularının editör kontrolü: testler hesabı ve tek doğru cevabı doğruluyor, sorunun metninin ifadeyle aynı şeyi sorduğunu doğrulayamaz.

## 2. aşama: Kimlik ve uygulama kabuğu

**Branch:** `feat/02-auth-and-shell` · **Tarih:** 2026-10-21

- Demo giriş (`/api/auth/demo-users`, `/api/auth/demo-login`, `/api/me`), HS256 JWT (Spring Security resource server), `/api/admin/**` yalnız ADMIN, 401/403 dahil tüm hatalar `ProblemDetail` + `code`, `X-Request-Id`.
- `auth.api.CurrentUser` (token'dan çözülür) ve `UserDirectory`; ilk admin uç noktası `GET /api/admin/users`. Health ayrıntıları artık ADMIN token'ıyla görünür (0. aşamadan kalan açık iş kapandı).
- OpenAPI sözleşmesi `doping-prescription-ui/openapi.json`; `OpenApiContractIT` sapmayı yakalar. Alanlar varsayılan zorunlu, yalnız `@Schema(nullable = true)` olanlar boş olabilir (OpenAPI 3.0).
- UI: üretilen tipler + `openapi-fetch`, `sessionStorage` oturumu, giriş ekranı, sol menülü kabuk (Dopingle, Testler ve Denemeler, Eksiklerim), giriş sonrası Dopingle.
- `ContentTagger` genişleme noktası; `KeywordTagger` onu uyguluyor.
- Kabul: öğrenci seçilince sol menülü uygulama açılır (Vitest), admin uç noktası öğrenciye 403 (`AuthIT`), yenilemede oturum korunur (Vitest).

## 3. aşama: Deneme modülü

**Branch:** `feat/03-exam-module` · **Tarih:** 2026-10-21

- `ExamGrader` (saf Java): doğru/yanlış/boş, net = doğru − yanlış/4, konu dökümü; senaryo 8/3/1, 7,25 birim testinde.
- API: liste, başlat/devam et, deneme getir (yeni: çözme ekranı yenilenince), cevap kaydet/boşalt, gönder, sonuç. Cevap anahtarı yalnız gönderilmiş denemenin sonucunda; başkasının denemesi 404; gönderilmiş denemeye yazma ve ikinci (eşzamanlı dahil) gönderim 409.
- `ExamResultPublished` outbox üzerinden `exam.result-published`'a (anahtar `userId`). `ExamSubmissionIT` gerçek Kafka'da tam bir kez yayınlandığını ve cevap anahtarı taşımadığını doğruluyor.
- UI: deneme listesi, çözme ekranı (cevap kâğıdı, bitirme onayı), sonuç ekranı (konu analizi, soru inceleme).
- Kararlar: Modulith olayı kendisi JSON'a çeviriyor, producer değeri `ByteArraySerializer` ile olduğu gibi yazıyor; uygulama katmanının okuma modelleri (`ExamViews`) doğrudan API gövdesi olarak kullanılıyor (entity'ler dışarı çıkmıyor); modül diyagramı artık `-Dmodulith.docs=true` ile isteğe bağlı üretiliyor.

## 4. aşama: Öğrenci profili ve Eksiklerim

**Branch:** `feat/04-learner-profile` · **Tarih:** 2026-10-21

- `MasteryCalculator` (saf Java): §1 örnekleri birim testinde birebir (0,35 → 0,245 → 0,1715; API'de 0,172 / 0,451; PerKom 0,494 / 0,330). API yuvarlaması iki adımlı (`MasteryRounding`).
- `ExamResultListener` (`learner` grubu) sonucu soru sırasıyla sinyallere çeviriyor; olay (`processed_event`) ve soru (`exam:{attemptId}:{questionId}`) düzeyinde idempotent. Eşik ≥ 0,05 olan değişimler `learner.mastery-changed` olarak outbox'a yazılıyor. Hatalarda 3 deneme, sonra `<topic>-dlt`.
- Redis önbelleği (`mastery:{userId}`, decay uygulanmamış snapshot): okuyucu `SETNX`, yazan commit sonrası `SET`; böylece eski değer yeni değerin üzerine yazılamıyor. Redis yoksa PostgreSQL'den okunuyor (`MasteryServiceIT` Redis'siz context'te).
- `GET /api/learner/profile`; UI: Eksiklerim ekranı ve sonuç ekranında profil sorgulamalı güncelleme bandı.
- Kabul: `LearnerProfileFlowIT` gerçek Kafka + Redis ile gönderimden sonra 2 sn içinde profili doğruluyor; aynı olay iki kez gelince skor değişmiyor (`MasteryServiceIT`).

### TODO(spec) notları

Üç not da (trend = son sinyalin yönü, decay tam gün, Redis'te öğrenci başına tek JSON değeri) onaylandı; `docs/algoritma.md` §1, `docs/api.md` ve `docs/mimari.md` §5 güncellendi. Açık not yok.

### Açık işler

- Eksiklerim'de "son ziyaretten beri artan konu" vurgusu 8. aşamadaki cilaya bırakıldı.

## 5. aşama: Kişiselleştirilmiş akış ve Dopingle ekranı

**Branch:** `feat/05-personalized-feed` · **Tarih:** 2026-10-21

- Saf domain: havuzlar (`CandidatePool` arayüzü; WEAK_TOPIC, REVIEW_DUE, EXPLORE, POPULAR), en büyük kalan yöntemiyle kota ve eksik kotanın WEAK → EXPLORE → POPULAR zinciri, `WeightedLinearRanker`, açgözlü "art arda en fazla 2" kuralı, oturumda gösterilen / son 14 günde tamamlanan / oturumda çok geçilen konuların dışlanması, `ReasonTemplate` şablonları, oturum tohumlu fallback. Havuzlar, sıralayıcı ve şablonlar bean (genişleme noktaları).
- Kotalar sayfa başına uygulanıyor ve kuyruk 3 sayfa olarak kuruluyor; böylece keşif kartı ilk 10'da yer alıyor (demo adım 3).
- `GET /api/feed`: imleçli oturum, `topicId`, `refresh`; kişiselleştirme hata verirse fallback (`personalized: false`), Redis yoksa sayfa her istekte hesaplanıyor ve oturum `feed_impression`'dan okunuyor. `POST /api/feed/interactions` `clientEventId` ile idempotent ve `feed.interaction-recorded` yayınlıyor; `learner.mastery-changed` kuyruğu siliyor.
- UI: Dopingle katmanında 9:16 simüle oynatıcı (konu rengi, neden hapı, konu çipi, puan rozeti), klavye/tekerlek/ok gezintisi, 3 kart kala sonraki sayfa, etkileşim olayları 5 sn'de bir.
- Kabul: `FeedFlowIT` (gerçek Kafka + Redis) ilk kartın Olasılık olduğunu, ilk 5'in en az 3'ünün zayıf konudan geldiğini, art arda 3 aynı konu olmadığını ve Fonksiyonlar keşif kartını doğruluyor; `FeedServiceIT` Redis kapalıyken akışı ve kişiselleştirmesi kapalı öğrencinin fallback akışını doğruluyor.

### Kararlar

- Learner, `feed.interaction-recorded`'ı feed'in Java tipini import etmeden kendi kaydıyla okuyor (feed → learner bağımlılığı varken döngü olmasın diye).
- `topicId` ile gelen akış kuyruğa girmiyor; konunun bandına göre havuz etiketi alıyor (WEAK → WEAK_TOPIC, UNKNOWN → EXPLORE, diğer → POPULAR).
