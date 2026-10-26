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

## 6. aşama: Mikro-soru ve puan

**Branch:** `feat/06-micro-quiz-and-points` · **Tarih:** 2026-10-21

- `GET /api/feed/cards/:impressionId/quiz`: videonun %70'i izlenmeden `409 VIDEO_NOT_WATCHED`; şıklar karıştırılıp yeniden harflendiriliyor, eşleme oturum anahtarından ayrı bir anahtarla imzalanan token'da (`PRESCRIPTION_QUIZ_TOKEN_SECRET`, demo profilinde varsayılan değer). Soru kalmadıysa `204`.
- `POST /api/quiz/:id/answer`: cevap sunucuda kontrol ediliyor; tek transaction'da cevap (`micro_quiz_attempt`), senkron sinyal (`LearnerSignalRecorder`, "%17 → %27"), puan (`PointsLedger`) ve `quiz.answered`. İkinci veya eşzamanlı cevap unique kısıta takılıp `pointsAwarded: 0` dönüyor; token Redis'te tek kullanımlık (`quiz:token-used:{jti}`), Redis yoksa yalnız veritabanı kısıtı koruyor. 2 sn'den hızlı cevap sinyal ve puan üretmiyor.
- Puan kuralları saf domain (`PointsRules`): zayıf konu 15, diğer 10, 5'li seri +20, günlük 200 sınırı (ödül ve bonus birlikte sınırlanıyor). `GET /api/points` üst çubuğu besliyor.
- UI: "Soruyu çöz" butonu, video bitince kendiliğinden açılan panel, tek tıkla cevap, çözüm, puan, seri ve eski değerden yeniye büyüyen ustalık çubuğu; üst çubukta ve Ana Sayfa'da toplam puan.
- Kabul: `QuizIT` api.md'deki quiz hata kodlarının hepsini (400, 403 QUIZ_TOKEN_INVALID, 404, 409 VIDEO_NOT_WATCHED, 410, 429), ikinci cevabın 0 puan aldığını ve eşzamanlı iki cevabın puanı bir kez yazdığını doğruluyor; 401, 403 FORBIDDEN ve 409 ATTEMPT_ALREADY_SUBMITTED önceki aşamaların testlerinde.

## 7. aşama: Admin (minimum)

**Branch:** `feat/07-admin-demo-control` · **Tarih:** 2026-10-21

- Her modül kendi tablolarını ve Redis anahtarlarını `shared.api.UserDataEraser` ile siliyor; `POST /api/admin/demo/reset` hepsini çağırıyor (admin hiçbir tabloya yazmıyor).
- `DemoPersona` bean'leri: `NEW` ve `SCRIPTED_IN_PROGRESS` (Mini Deneme 1 başlatılıyor, senaryo cevapları exam modülünün `ExamSubmission` API'siyle, öğrencinin kullandığı servis yolundan işaretleniyor; gönderilmiyor). Bilinmeyen persona `400`.
- `GET /api/admin/demo/answer-key`, `/students`, `/personas`; `GET /api/admin/feed-debug?slug=` en zayıf 5 konu ve son oturumun kartlarını skor bileşenleriyle döndürüyor (Swagger UI'dan admin token'ıyla).
- UI: `/admin` demo kontrolü (öğrenci kartları, Sıfırla, Persona yükle) ve sunum cevap anahtarı.
- Kabul: `DemoControlIT` sıfırlamanın tüm modüllerin verisini 1 sn altında sildiğini, `SCRIPTED_IN_PROGRESS` sonrası 11 cevabın işaretli olduğunu ve henüz sinyal olmadığını, feed debug API'sinin bileşenleri döndürdüğünü doğruluyor. Senaryo (sıfırla → persona → Deniz → deneme → Dopingle → mikro-soru → Eksiklerim) tarayıcıda baştan sona oynatıldı.
- Test düzeltmesi: `ContentTaggingIT` artık yalnız kendi outbox olaylarını bekliyor; Kafka'sız context'te diğer testlerin olayları tamamlanmadığı için sıra bağımlı kırılıyordu (6. aşamanın ilk CI koşusu buna takıldı).

## 8. aşama: E2E ve README

**Branch:** `feat/e2e-demo-scenario` · **Tarih:** 2026-10-22

- Playwright testi (`e2e/tests/demo-scenario.spec.ts`) demo senaryosunun ana yolunu gerçek yığında oynatıyor: hazırlık (sıfırla, `SCRIPTED_IN_PROGRESS`), deneme gönderimi, reçete, mikro-soru ve Gelişimim. Yerelde çalışan uygulamayı kullanıyor, yoksa jar'ı ve Vite'ı kendisi başlatıyor; CI'da ayrı bir `e2e` job'u (Ubuntu 24.04, Docker Compose).
- README `docs/kod-standartlari.md` §6'daki başlıkları kapsıyor.

## Aşama sonrası işler (2026-10-22 / 23)

Kapsam MVP'de tutuldu; aşağıdakiler sunumun görünüşü ve "işe yarıyor mu?" sorusu için eklendi.

- **Doping Hafıza görselleri ve kabuk** (ADR 0006): Ana Sayfa, menü, üst çubuk, hesap menüsü; Doping Hafıza'nın ölçüleri.
- **Gelişimim:** Eksiklerim ekranı Akıllı Değerlendirme Paneli tonlarında Gelişimim oldu (`/app/gelisimim`; eski adres yönleniyor). AGENTS.md ve dokümanlar yeni adla güncellendi.
- **Video etkinliği:** `GET /api/admin/content-effectiveness`, video başına mikro-soru doğruluk oranı (backlog: ileride `quality_score`'u besleyebilir).
- **Gerçek demo videoları:** ürün sahibinin hazırladığı sekiz sesli video Git LFS ile `public/videos/` altında; dosya yoksa simüle oynatıcı. `demo` profili dördünü kişiselleştirilmiş oturumun başına sabitliyor (`prescription.feed.pinned-content-ids`, docs/algoritma.md §2).

## Sunum öncesi kod incelemesi düzeltmeleri

**Tarih:** 2026-10-23. Bir iç kod incelemesinin "Yüksek" önemdeki bulguları düzeltildi.

**Backend**

- **Outbox yeniden gönderimi:** `republish-outstanding-events-on-restart` açık; uygulama çalışırken tamamlanmamış yayınlar `OutboxResubmission` ile periyodik olarak yeniden gönderiliyor (`prescription.messaging.outbox`). Testlerde ikisi de kapalı: testler tek veritabanını paylaşıyor ve Kafka'sız bağlamların tamamlanmamış olayları başka testlere karışmamalı.
- **Mikro-soru cevabının dayanıklılığı:**
    - Yalnız `uq_micro_quiz_attempt_user_question` ihlali "zaten cevaplandı" (0 puan) sayılıyor.
    - Ustalık ve seri satırı cevap sırasında `SELECT … FOR UPDATE` ile kilitleniyor; satır oluşturma ya da version çakışmasında cevap bir kez yeniden deneniyor.
    - Saklanamayan bir cevap token'ı geri bırakıyor.
    - `feed.interaction-recorded`'dan yalnız `VIEW_COMPLETE` ve `SKIP` `last_served_at`'e yazılıyor (mimari.md §4), mikro-soruyla aynı satıra gereksiz yazım yok.
- **Art arda en fazla 2 kural:** kuyruk oturum ortasında yeniden kurulduğunda kural oturumun son sunulan kartlarından devam ediyor (`FeedContext.recentTopics`).
- **Sıfırlama:** tüm modüller tek transaction'da siliyor; Redis temizliği commit'ten sonra. Persona yükleme de tek transaction.
- **Tüketiciler:** `MasteryChangedListener` işini transaction'lı `QueueInvalidation`'a devrediyor (`processed_event` ile aynı transaction, kuyruk commit sonrası siliniyor).
- **N+1:**
    - Id'si uygulamada atanan entity'ler `save()`'de önce SELECT yapmıyor (`Persistable` veya wrapper `Long` version).
    - Insert'ler batch'leniyor.
    - Deneme listesi deneme sayısından bağımsız 3 sorgu.
    - Sınav sonucu işlenirken idempotency anahtarları tek sorguda okunuyor.
- **Genişleme noktaları ve eşleme:** ADR 0007 (gerçek kapsam; eksik "neden bu video" şablonu artık açılışta hata veriyor) ve ADR 0008 (okuma modelleri API yanıtı, MapStruct yalnız entity eşlemesinde).

**Frontend**

- **Sorgulama sayacı:** profil sorgulaması (`useProfilePolling`) sayacı sayfanın açıldığı andan sayıyor; aynı sekmede ikinci provada bant "analiz ediliyor"da kalmıyor.
- **Player bölündü:** Dopingle `Player` bileşeni hook'lara (`useQuizFlow`, `useImpression`, `useFeedNavigation`, `useAutoNext`) ve `ActionRail`'e bölündü.
- **İki hata düzeltildi:** mikro-soru bir kartta en fazla bir kez isteniyor; `VIEW_START` StrictMode'da iki kez kaydedilmiyor.
- **Vitest bekleme süresi:** Testing Library'nin bekleme süresi 3 sn. Paralel jsdom açılışında her dosyanın ilk render'ı 1 sn'yi aşabiliyordu.

**Testler**

- **Backend:**
    - `QueryCountIT`: akış ve profil endpoint'lerinin SQL sayısı sabit; 5 ve 20 kartlık sayfa aynı sayıda ifade çalıştırıyor.
    - `FeedFallbackIT`: profil hatasında fallback akış dönüyor.
    - `QueueInvalidationTest`, `InteractionListenerTest` (learner'ın event kopyası feed'in event'iyle uyumlu), `MasteryServiceIT` (`recordServed` filtresi ve tekrar teslim).
    - `QuizIT`: aynı token iki kez gönderilince; iki farklı soruya eşzamanlı cevap.
    - `FeedComposerTest`: oturum ortası yeniden kurulum; şablonsuz havuz.
- **E2E:** adım 1 (genel akış), sonuç ekranında 8/3/1 ve 7,25 net, ilk 5 ve ilk 10 kart kuralları, Gelişimim'de "N video hazır" ve Çalış eklendi; deneme adıyla seçiliyor.

### Orta ve Düşük önemdeki bulgular

**Backend: doğruluk ve güvenlik**

- **Eşzamanlı deneme:**
    - Gönderim ve cevap kaydı denemeyi satır kilidiyle okuyor. Eşzamanlı ikinci gönderim 500 yerine `409 ATTEMPT_ALREADY_SUBMITTED` alıyor.
    - Çift "başla" aynı denemeyi paylaşıyor (`insert … on conflict do nothing`).
    - Sonuç ekranı kayıtlı sonuçları gösteriyor, yeniden notlandırmıyor.
- **Demo girişi:** yalnızca `prescription.auth.demo-login-enabled` açıkken (`demo` profili) var; aksi halde `404`. CSRF'in neden kapalı olduğu yorumda.
- **Hata modeli:** Spring MVC hataları da `ProblemDetail` ve `code` ile dönüyor. Yeni kodlar: `METHOD_NOT_ALLOWED` (405), `UNSUPPORTED_MEDIA_TYPE` (415).
- **Quiz token'ı:** `aud: quiz` taşıyor ve doğrulanıyor. Oturum ve quiz anahtarları aynıysa uygulama açılmıyor.
- **Girdi doğrulama:** `responseMs` en çok 300000, `microQuestionId` pozitif, admin `slug`/`persona` uzunluk sınırlı.
- **Akış ve etkileşim kapsamı:**
    - `clientEventId` öğrenci başına tekil (V10).
    - Oturum okumaları öğrenciyle sınırlı; Redis oturum anahtarı `feed:session:{userId}:{sessionId}`.
    - Tekrar izleme engeli istemci saati yerine sunucunun kayıt anını kullanıyor.
- **Kafka:** okunamayan mesajlar yeniden denenmeden `-dlt`'ye gidiyor.
- **Redis kesintisi:** bir hatadan sonra kısa süre Redis denenmiyor (`store-retry-after`, `cache-retry-after`). Profil önbelleği okuması DB transaction'ı dışında.
- **Akış kuralları:** sayfa boyutu config'den (`page-size`, `page-size-max`). Ertelenen konu yalnızca yeniden kurulan ilk sayfadan çıkıyor; ilk sayfa artık eksik kalmıyor.

**Backend: domain ve kod düzeni**

- Seri kuralı `points.domain.Streak`, çok hızlı cevap kuralı `quiz.domain.AnswerPolicy`, anlamlı değişim eşiği `learner.domain.MasteryChangePolicy` oldu.
- Modüller arası sözleşmeler tipli: `LearnerProfileQuery.Band`/`Trend` enum'ları, `ImpressionView.weakTopic`. Quiz artık `"WEAK_TOPIC"` metnini karşılaştırmıyor.
- Rastgelelik dışarıdan veriliyor: quiz için `RandomGenerator` bean'i, fallback akış için oturum tohumlu üreteç.
- Kayıt bileşenlerinde `Optional` yerine `@Nullable`.
- `ContentEffectivenessService` sıfıra bölme ve eksik konu durumlarına karşı korumalı.

**Mimari testler**

- `@Transactional` yalnızca `application` (ve seed) paketlerinde ve public metotlarda.
- web → persistence yasak (`AuthWebMapper` istisnası yorumlu).
- Alan enjeksiyonu yasak; `Clock`'suz `now()` yasak; domain sınıfları final.
- Her modülün izinli bağımlılıkları `allowedDependencies` ile yazılı. `feed → points` bağımlılığı mimari.md'ye eklendi.

**Testler**

- **Yeni testler:** `ExamServiceIT`, `DemoLoginDisabledIT`, `QuizTokensTest`, `ImpressionQueryIT`, `KafkaConsumerConfigurationTest`, Redis geri çekilme testleri.
- **Düzeltilen testler:**
    - `AuthIT` tüm admin endpoint'lerinde 403'ü deniyor.
    - `QuizIT` geçersiz harf durumunu deniyor.
    - `DemoControlIT`'te süre ölçümü ayrı ve sıcak uygulamada.
    - `OpenApiContractIT` dosya yoksa kırılıyor.
- **Test log'u:** Kafka'sız test bağlamları event dışsallaştırmayı kapatıyor; log'daki gürültü kalktı.

**Frontend**

- **Yarış:** deneme cevap kaydı ve gönderim aynı mutation kapsamında, sırayla.
- **Odak ve klavye:**
    - Bitirme penceresinde odak tuzağı ve Escape.
    - Hesap menüsünde ok tuşları.
    - Mikro-soru paneli odağı alıyor; açıkken Escape katmanı kapatmıyor.
- **Ekran durumları:** yükleme, boş ve hata durumları (cevap anahtarı, video etkinliği, Gelişimim, Ana Sayfa, akış için "Tekrar dene").
- **Mimari:**
    - Oturum context'i `shared/session`'da; özellikler `app` katmanını import edemiyor.
    - Özellik listesi klasörlerden okunuyor.
    - Layout'lar iç içe route'larla seçiliyor.
- **Davranış:**
    - Puan ve profil sorguları sekmeye dönünce yenileniyor.
    - "Reçeteni gör" ve "Çalış" kapatılınca açıldıkları sayfaya dönüyor.
    - `useQuizTimer`: şık seçilince süre duruyor.
- **Test:** `useInteractionRecorder` testi; MSW mock'ları üretilen tiplerle; mikro-soru zaman aşımı ve 410/429 durumları.
- **Temizlik:**
    - Ölü export ve CSS değişkenleri silindi.
    - Sahte beğeni sayacı kaldırıldı.
    - Ekran okuyucu metinleri `sr-only`.
    - Arama kutusunda görünür odak.

**Bilerek bırakılanlar**

- Doping'in marka turuncusu üzerindeki beyaz yazının kontrastı (≈2,4:1) değiştirilmedi.
- Seed yükleyicileri `@Transactional`'ı kendi paketlerinde tutuyor (ArchUnit istisnası yorumlu).
- Geçmişe ait iki eleştiri düzeltilemez: büyük commit'ler ve CI bitmeden yapılan merge'ler. Bundan sonra merge'ler PR ve yeşil CI ile.

### TODO(spec) notları

- `MasteryService.applyQuizAnswer`: mikro-soru güncellemesi algoritma.md §1'deki "mevcut mastery"ye, yani decay uygulanmamış saklı değere uygulanıyor. Günlerce ara verildiyse paneldeki "önce → sonra" farkı decay'in geri alınmasını da içerir. Aynı gün oynanan demoda etkisi yok; doküman netleşirse değişebilir.
