# Kod incelemesi

**Kapsam:** `main` (f69b675) ve üzerindeki **commit'lenmemiş** çalışma ağacı: 153 değişmiş, 40'tan fazla yeni dosya, yaklaşık +3.070 / −1.050 satır. İnceleme 2026-10-26'da yapıldı ve kod değiştirilmedi. Kodla dokümanın çeliştiği yerlerde kod esas alındı.

## 1. Test ve kalite kontrolü sonuçları

| Kontrol | Sonuç | Ayrıntı |
| --- | --- | --- |
| `./mvnw verify` (core) | **Geçti** · 4 dk 11 sn | Surefire 226 test (1 atlandı), Failsafe 130 entegrasyon testi, hata yok. Checkstyle 0 ihlal, SpotBugs 0 bulgu, Spotless temiz, JaCoCo "All coverage checks have been met". 65 test sınıfında 210 `@Test` var (33 birim, 32 IT); parametreli testler sayıyı 356'ya çıkarıyor. |
| `npm run check` (ui) | **Geçti** · ~2 dk 14 sn | `tsc -b`, ESLint (0 uyarı), Prettier ve Vitest: 17 dosyada 61 test. |
| e2e (`e2e`, Playwright) | **Geçti** · 1 test, 45,5 sn (toplam 1,2 dk) | Yeni üretilmiş jar ile 8091/5191 portlarında, `demo` profilinde baştan çalıştırıldı. |
| Son CI çalışması | **Yeşil** | `main` f69b675, 2026-10-23 (core, ui ve e2e job'ları). **Commit'lenmemiş değişiklikler CI'da hiç çalışmadı.** |

Not: Git Bash'te `./mvnw`, Maven dağıtımını `~/.m2/wrapper` altına taşırken izin hatası verdi. Build PowerShell'den `mvnw.cmd` ile çalıştırıldı. Bu bir yerel ortam sorunu, kodla ilgisi yok.

## 2. Bulgular

Kritik bulgu yok. "Yüksek" seviyedeki tek bulgu süreçle ilgili. Kod tarafındaki bulgular çoğunlukla sağlamlık ve okunabilirlik iyileştirmesi. Satır numaraları çalışma ağacına göre.

| # | Önem | Alan | Dosya:satır | Sorun | Neden önemli | Önerilen düzeltme | Süre |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Yüksek | Teslim / git | çalışma ağacı (153 dosya) | Review düzeltmeleri, kaydedilenler özelliği (V10–V11), yeni testler ve dokümanlar commit'lenmemiş. Demo ve testler bu hâlle geçiyor. | Reviewer `main`'e bakar ve bu işi görmez. CI bu değişiklikleri doğrulamadı. "Küçük, anlamlı commit" kuralı (AGENTS.md §6) risk altında. | §5'teki commit planıyla bir branch'e küçük commit'ler halinde alıp PR + yeşil CI ile merge etmek. | 2–3 sa |
| 2 | Orta | Güvenlik / demo | `doping-prescription-core/src/main/resources/application-demo.yml:6,8,11` · `auth/web/DemoLoginController.java:34` | `demo` profilinde şifresiz giriş açık (admin dahil). JWT ve quiz secret'larının varsayılan değerleri herkesin bildiği değerler. | Profil yanlışlıkla dışa açık bir ortamda çalışırsa herkes ADMIN token'ı alabilir ya da üretebilir. Teknoloji CEO'sunun ilk sorusu bu olabilir. | `demo` profili localhost dışına bağlanıyorsa başlangıçta hata verilsin. README ve ADR'ye "yalnız lokal" notu eklensin. | 0,5 sa |
| 3 | Orta | Doğruluk / N+1 | `exam/application/ExamSubmissionService.java:41-48` · `exam/application/ExamService.java:123-139` | `answerWithScenario` her soru için ayrı ayrı kilitli attempt okuması, soru `findById`, cevap `findById` ve `save` yapıyor: yaklaşık 4N sorgu. | "Persona yükle" yolunda çalışıyor. Reviewer'ın gözüne çarpacak bir N+1 kalıbı ve 1 sn'lik reset bütçesini zorluyor. | Attempt bir kez kilitlenip cevaplar toplu okunsun, ardından `saveAll` yapılsın. | 45 dk |
| 4 | Orta | Test kararlılığı | `src/test/.../flow/FeedFlowIT.java:43,108` · `flow/LearnerProfileFlowIT.java:38` | Kafka zinciri `atMost(2s)` ile bekleniyor. | 2 sn kabul kriteri, ama yavaş bir CI runner'ında Testcontainers Kafka gecikmesi testi kırmızıya çevirebilir (flaky). | Kabul ölçümü ayrı, etiketli bir testte kalsın; akış testlerinde süre 10 sn olsun. | 20 dk |
| 5 | Orta | Test anlamı | `src/test/.../flow/ExamSubmissionIT.java:46,170-178` | "Event tam bir kez yayınlandı" kontrolü 3 sn bekleyip bir kez daha poll etmeye dayanıyor. | Garanti zayıf: geç gelen bir kopya pencereyi kaçırabilir. Teste ayrıca ~5 sn ekliyor. | `event_publication` tablosunda tam 1 tamamlanmış kayıt olduğu assert edilsin, Kafka tarafı `endOffsets` ile sayılsın. | 30 dk |
| 6 | — | Sözleşme | `src/test/.../OpenApiContractIT.java:27` | **Geçersiz, bulgu değil.** `OpenApiContractIT`, koddan üretilen dokümanı doğrudan `doping-prescription-ui/openapi.json` ile karşılaştırıyor; sapma `./mvnw verify`'ı ve CI'ı kırıyor. | — | — | — |
| 7 | Orta | Frontend / oturum | `doping-prescription-ui/src/shared/api/client.ts:33-38` | 401 alınınca `window.location.assign('/')` ile tam sayfa yenileme yapılıyor. | Router ve Query önbelleği atlanıyor. Birden fazla eşzamanlı 401 çoklu yönlendirmeye yol açabilir. Demoda beklenmedik bir yenileme olur. | `onUnauthorized` callback'i tanımlansın; `SessionProvider.logout()` + `queryClient.clear()` + `navigate`. | 1 sa |
| 8 | Orta | Ürün / sunum | `admin/application/ContentEffectivenessService.java` (veri) | "Video etkinliği" tablosu reset sonrası boş. Demo sonrası tek satır var: 1 cevap, %100. | Sunumdaki para kaldıracının görseli bu tablo. Tek satır ve %100 ikna etmez, hatta ters etki yapabilir. | Sunum için açıkça "simülasyon" etiketli bir persona/seed (ör. 400 öğrenci × cevap). Bu yapılmazsa slayt yalnızca maket olarak kalsın. | 2 sa |
| 9 | Düşük | Kural 7 kapsamı | `feed/application/FeedService.java:121-145` | `try` yalnızca profil ve sıralamayı sarıyor. `fallback(...)`, `cardViews` (impression yazımı) ve `liveVideos()` kapsam dışında; DB hatasında 500 döner. | Kural metni "akış asla hata vermez" diyor; kod bunu profil ve sıralama hatası olarak uyguluyor. | Kapsam `docs/mimari.md`'ye yazılsın (DB yoksa hiçbir şey çalışmaz). İstenirse fallback yolu da sarılsın. | 30 dk |
| 10 | Düşük | Suistimal | `quiz/application/QuizService.java:99` · `feed/application/InteractionService.java:60-67` | `%70 izlendi` kontrolü istemcinin gönderdiği `watchMs`'e dayanıyor. | İstemci izlemeden soruyu açıp puan toplayabilir. | `watchMs` sunucuda, gösterimden bu yana geçen süre ve video süresiyle sınırlansın. | 1 sa |
| 11 | Düşük | Eşzamanlılık | `quiz/application/QuizService.java:110` | Hız sınırı sayımı transaction ve kilit dışında yapılıyor (TOCTOU). | Çift puan oluşmaz (unique kısıt var), sadece sınır biraz aşılabilir. | Redis `INCR` + TTL kullanılsın ya da kilitli sayım yapılsın. | 1 sa |
| 12 | Düşük | İndeks | `db/migration/V8__quiz_schema.sql:18-25` | `countSince` (`user_id = ? and created_at > ?`) için `(user_id, created_at)` indeksi yok. | Her cevapta çalışan sorgu, satır sayısı arttıkça büyüyen bir tarama yapıyor. | V12: `create index ix_micro_quiz_attempt_user_created on micro_quiz_attempt (user_id, created_at)`. | 10 dk |
| 13 | Düşük | correct_key / log | `exam/domain/ExamGrader.java:79` · `exam/domain/ExamQuestionContent.java:16` · `content/domain/QuestionContent.java:16` | `correctKey` taşıyan record'ların varsayılan `toString()`'i alanı içeriyor. Bugün log'a yazılmıyor. | Kural 5'i ileride tek bir `log.debug` bozabilir. `MicroQuestionCatalog.QuizQuestion` bu durumu zaten engelliyor; tutarlılık için aynısı uygulanmalı. | Aynı `toString` override'ı bu üç record'a da eklensin. | 30 dk |
| 14 | Düşük | correct_key (dolaylı) | `quiz/application/QuizTokens.java:60-69` | Quiz token'ı imzalı ama şifreli değil. Şık karıştırma eşlemesi istemcide çözülebilir. | Doğru cevap sızmıyor, ama eşlemeden ipucu çıkarılabilir. Seed'deki doğru harf dağılımı da dengesiz (D 10, E 4). | Eşleme sunucuda (Redis/DB) tutulsun, token'da yalnız `jti` olsun. Ya da JWE kullanılsın. | 1–2 sa |
| 15 | Düşük | Config (§5.8) | `feed/domain/WeightedLinearRanker.java:11-13` · `quiz/application/QuizService.java:42-43` · `admin/application/DemoControlService.java:41` | `MIN_CONFIDENCE`, `WATCHED_FRESHNESS`, `MAX_DIFFICULTY_STEPS`, `RATE_WINDOW`, `WEAKEST_TOPICS` kodda sabit. | Reviewer "sayısal değerler config'de" kuralının ihlal edildiğini düşünebilir. | Bunlar ya config'e taşınsın ya da kodda "formül sabiti" diye gerekçelendirilip algoritma.md §9'a not düşülsün. | 30 dk |
| 16 | Düşük | Coverage anlamı | `doping-prescription-core/pom.xml:286-324` | IT'ler de aynı `jacoco.exec`'e yazıyor, yani %80 kuralına IT kapsamı da sayılıyor. | "Domain birim testiyle doğrulanır" kuralını build garanti etmiyor. | `domain` paketi için yalnızca birim testi exec'ini kullanan ayrı bir kural eklensin. | 30 dk |
| 17 | Düşük | Test izolasyonu | `src/test/.../TestContainers.java:44-58` | Tüm IT'ler aynı veritabanını paylaşıyor; izolasyon `StudentReset` ile sağlanıyor. | Sabit slug kullanan testler birbirini etkileyebilir, sıraya bağımlılık riski var. | Her IT kendi slug'ını kullansın ya da kural `IntegrationTest` Javadoc'una yazılsın. | 30 dk |
| 18 | Düşük | Frontend / Query | `doping-prescription-ui/src/app/App.tsx:10` | `retry: 1` 4xx hatalarında da tekrar deniyor. | Hata ekranı gecikiyor; 403/404/409'u tekrar denemenin anlamı yok. | `retry: (n, e) => !(e instanceof ApiError && e.status < 500) && n < 1`. | 15 dk |
| 19 | Düşük | Erişilebilirlik | `doping-prescription-ui/src/app/layout/DopingleLayer.tsx:30-48` | Tam ekran katmanda `role="dialog"` ve açılışta odak taşıma yok. Escape global dinleniyor. Filtre `span` + `aria-disabled` ile yapılmış. | Klavye kullanıcısı arka plandaki menüye geçebiliyor. | Açılışta odak katmana verilsin, Escape dinleyicisi kökte olsun, `span` yerine `<button disabled>` kullanılsın. | 1 sa |
| 20 | Düşük | Mutation | `doping-prescription-ui/src/features/dopingle/api.ts:66-77` | `unwrap` sadece yan etkisi için çağrılıyor. İyimser güncellemede `cancelQueries` yok ve geri alma önceki değeri değil `!saved` değerini yazıyor. | Kalıp kanonik değil, niyet okunmuyor. | `assertOk` yardımcısı eklensin; önceki değer saklanıp geri yüklensin. | 30 dk |
| 21 | Düşük | E2E | `e2e/tests/demo-scenario.spec.ts:57,94` · `e2e/playwright.config.ts:7` | `/Sınavı Bitir\|Testi Bitir/` ile `.first()` kullanılıyor, sabit `waitForTimeout` var, jar adı `0.0.1-SNAPSHOT` olarak sabit. | Metin, config ya da sürüm değişince test sessizce bozulur. | Tek ve kesin bir rol adı kullanılsın, bekleme süresi config/env'den türetilsin, jar yolu glob ile bulunsun. | 30 dk |
| 22 | Düşük | Doküman–kod | `doping-prescription-core/src/main/resources/application.yml:73-75` | Yorumda "Spring Security faz 2'ye kadar yapılandırılmadı" yazıyor; artık yapılandırıldı. | Eskimiş yorum. | Yorum güncellensin. | 5 dk |
| 23 | Düşük | Doküman–kod | `docs/algoritma.md:224` · `docs/mimari.md:52` | `SimulationProperties` / `simulation.impact.*` anlatılıyor, ama `simulation` modülü boş. | Doküman, olmayan bir config'i tarif ediyor. | "(ertelendi, `docs/backlog.md`)" notu eklensin. | 5 dk |
| 24 | Düşük | Sunum varlığı | `doping-prescription-ui/public/screenshots/gelisimim_full_ekran.png` | Dosya adı Gelişimim diyor, içerik ise Dopingle akışı (`dopingle_akis.png` ile aynı görüntü). | Sunumda yanlış görsel kullanılabilir. | Yeniden çekilsin. Sunum için `docs/sunum/img/gelisimim.png` üretildi. | 5 dk |

Açık `TODO(spec)` notu: `learner/application/MasteryService.java:126` (mikro-soru adımı decay uygulanmamış değere uygulanıyor). Bu not `docs/ilerleme.md`'de listelenmiş.

## 3. Sunuma kadar önerilen düzeltmeler

Bu işler demoyu riske atmıyor: ya demo yolunun dışında kalıyorlar ya da yalnız test ve dokümanı değiştiriyorlar.

| # | İş | Süre |
| --- | --- | --- |
| 1 | Commit'lenmemiş işi plana göre commit'leyip PR açmak ve yeşil CI ile merge etmek (§5) | 2–3 sa |
| 2 | `demo` profili için localhost koruması ve README notu | 30 dk |
| 4 | Akış IT'lerinde bekleme süresini 10 sn yapmak | 20 dk |
| 5 | Outbox tablosu üzerinden "tam bir kez" assert'i | 30 dk |
| 12 | `(user_id, created_at)` indeksi (V12) | 10 dk |
| 13 | `correctKey` record'larında `toString` override'ı | 30 dk |
| 15 | Sabitler için gerekçe yorumları ve algoritma.md §9 notu | 30 dk |
| 22, 23 | Eskimiş yorum ve simulation doküman notu | 10 dk |
| 24 | Gelişimim ekran görüntüsünü yenilemek | 5 dk |
| **Toplam** | | **≈ 5–6 sa** |

Sunumdan sonraya bırakılabilecekler: 3 (persona yolu demoda çalışıyor; dokunmak riskli), 7 (oturum akışı demoda kullanılıyor), 8 (yeni seed veya persona gerektiriyor), 10, 11, 14, 16–21.

## 4. Kodun en güçlü 5 yanı

1. **Modül sınırları testle zorlanıyor.** Her modül `allowedDependencies` ile yalnız diğerlerinin `api` paketine bağlanabiliyor. `ApplicationModules.verify()` ve ArchUnit kuralları (`ArchitectureTests.java:56-103`) domain saflığını, final domain sınıflarını, `@Transactional`'ın yerini ve `Clock`'suz `now()` yasağını kontrol ediyor. Modüller arasında iç paket import'u yok.
2. **Mikro-soru cevabı "claim-first" kurulmuş.** Unique kısıt, tek transaction ve outbox birlikte kullanılıyor; çakışmada tek retry ve token geri verme var (`quiz/application/QuizAnswering.java:53-99`). Eşzamanlı iki cevabın puanı bir kez yazdığı `QuizIT`'te kanıtlanıyor.
3. **İdempotency her katmanda tutarlı.** `processed_event` + `ON CONFLICT DO NOTHING` aynı transaction'da (`shared/idempotency/ProcessedEvents.java`). `clientEventId` ve puan için `refId` unique kısıtla korunuyor. Redis kuyruğu commit sonrası düşürülüyor (`feed/application/QueueInvalidation.java`).
4. **Sözleşmeden üretilen tipler ve katı TypeScript.** `strict`, `noUncheckedIndexedAccess` ve `exactOptionalPropertyTypes` açık; kodda `any` ya da `!` yok. MSW mock'ları üretilen şemayla tipli. Sorgu anahtarları tek dosyada toplanmış.
5. **Demo senaryosu bir test.** Playwright, sunumdaki 5 adımı gerçek yığın üzerinde her push'ta oynatıyor. Seed soruları cevaptan bağımsız, tam kesirli bir hesaplayıcıyla doğrulanıyor.

## 5. Önerilen commit planı (uygulanmadı)

Branch: `fix/review-round-2`. Her commit'ten önce `./mvnw verify` (ve UI değiştiyse `npm run check`) çalıştırılmalı. Değişiklikler birbirine bağlı olduğundan bazı gruplar birleştirilmek zorunda kalabilir.

1. `refactor(shared): outbox resubmission, cooldown and outbox properties` (shared/messaging, shared/time ve testleri)
2. `refactor(points): streak rule as a pure domain class` (`points/domain/Streak` + `StreakTest`)
3. `refactor(learner): mastery change policy and snapshot loader` (`MasteryChangePolicy`, `LearnerSnapshotLoader`, cache/messaging testleri)
4. `refactor(quiz): answer policy, distinct token secrets and quiz beans` (+ `QuizTokensTest`, `AnswerPolicyTest`)
5. `refactor(exam): question counts and assembler cleanup` (+ `exam/application` testleri)
6. `feat(feed): per-user interaction scope (V10)` (+ `ImpressionQueryIT`)
7. `feat(feed): saved videos (V11)` (`SavedContentService`, `SavedContentQueries`, controller, `SavedContentIT`, openapi.json)
8. `refactor(feed): queue invalidation after commit and page limit` (+ `QueueInvalidationTest`, `FeedPageLimitTest`, `FeedFallbackIT`)
9. `refactor(auth): demo login in its own controller, disabled outside demo` (+ `DemoLoginDisabledIT`)
10. `test: query count guard and movable clock` (`QueryCountIT`, `StatementCounter`, `MovableClock`)
11. `feat(ui): saved videos and Dopingle layer polish` (features/dopingle, app/layout)
12. `fix(ui): focus, loading and error states` (exams, learner, admin, shared/ui)
13. `test(e2e): demo scenario follows the pinned videos`
14. `docs: ADR 0007 and 0008, review-round progress note` (docs, README, AGENTS.md)
15. `docs(review): code review, architecture diagrams and presentation deck` (`docs/review/`, `docs/sunum/`)

## 6. Reviewer'ın sorabileceği en zor 5 soru

1. **"Kafka bir monolitin içinde neden var? Spring'in kendi event'leri yetmez miydi?"**
   Modulith outbox'ı zaten modül içi event'leri taşıyor. Kafka'yı, deneme sisteminin ileride gerçek Doping'de ayrı bir sistem olacağı için kullanıyoruz: topic'ler ve olay sözleşmeleri bugünden o sınırda duruyor. Bedeli bir broker daha; bunu ADR 0002 kabul ediyor.
2. **"Outbox en az bir kez teslim ediyor. Çift mesajda ne olur?"**
   Her tüketici `eventId`'yi aynı transaction'da `processed_event`'e yazıyor, ikinci kopya hiçbir şey yapmıyor. `OutboxResubmission`'ın 1 dakika sonra yeniden göndermesi bu yüzden güvenli. Testi `MasteryServiceIT`'te.
3. **"`%70 izlendi` kuralını istemci atlatabilir mi?"**
   Evet, `watchMs` istemciden geliyor (bulgu 10). Demo kapsamında bu bilinçli bir sadeleştirme; düzeltmesi sunucu tarafında sınırlamak. Puan tavanı ve unique kısıt kötüye kullanımın etkisini sınırlıyor.
4. **"Redis düşerse ne olur?"**
   Akış her istekte veritabanından hesaplanıyor, profil PostgreSQL'den okunuyor, token tekilliğini veritabanı kısıtı sağlıyor. `FeedServiceIT` ve `MasteryServiceIT` bunu Redis olmadan doğruluyor. Kaybedilen tek şey performans.
5. **"Bu kadar iş neden commit'lenmemiş, CI'dan geçmeden 'bitti' denebilir mi?"**
   Denemez. Review düzeltmeleri tek seferde yapıldı; sunumdan önce plana göre küçük commit'ler ve PR ile CI'dan geçirilecek (bulgu 1). Yerelde verify, check ve e2e yeşil.
