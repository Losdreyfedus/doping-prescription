# Kod standartları

Bu proje kod incelemesinden geçecek. Aşağıdaki kurallar her commit için geçerlidir. Her aşamanın sonunda §7'deki kontrol listesiyle kendi kodunu incele ve `docs/ilerleme.md` içine kısa bir öz-değerlendirme yaz.

## 1. Genel ilkeler

- Okunabilirlik zekâdan önce gelir: açık isimler, kısa metotlar, tek sorumluluk.
- Önce doğru davranış ve testi, sonra optimizasyon.
- Kullanılmayan kod, yorum satırına alınmış kod, `System.out`, `console.log` bırakılmaz.
- Yorumlar "ne" değil "neden" anlatır. Algoritma sınıflarında formülün kaynağı (`docs/algoritma.md` bölüm numarası) yorumda belirtilir.
- Bağımlılık eklerken gerekçesini commit mesajında yaz; aynı işi yapan iki kütüphane olmaz.

## 2. Backend (Java)

### Dil ve stil

- Java 21 özellikleri bilinçli kullanılır: DTO'lar, event'ler, value object'ler ve config için `record`; kapalı hiyerarşiler için `sealed`; `switch` ifadeleri ve pattern matching.
- Biçimlendirme Spotless (palantir-java-format) ile otomatik; Checkstyle uyarısı kalmaz.
- **Lombok kullanılmaz.** Record'lar ve açık constructor'lar yeterlidir.
- Varsayılan görünürlük package-private; yalnızca gerekenler `public`.
- Alanlar `final`; değiştirilebilir durum yalnızca JPA entity'lerinde ve kontrollü metotlarla.
- `null` döndürülmez: tekil sonuç için `Optional` (yalnızca dönüş tipi olarak), koleksiyonlar için boş koleksiyon.
- Sihirli sayı yok; sabitler ya config'de ya isimlendirilmiş `static final` alanlarda.

### Spring

- Yalnızca constructor injection; alan enjeksiyonu (`@Autowired` alan) yok.
- `@Transactional` yalnızca `application` servislerinin public metotlarında; okuma metotlarında `readOnly = true`. Controller ve repository'de transaction tanımı yok.
- Controller'lar ince: doğrulama (`@Valid`), servis çağrısı, yanıt olarak servisin okuma modeli (ADR 0008); entity'den DTO'ya eşleme gereken yerde MapStruct. İş kuralı yok.
- Config: `@ConfigurationProperties(prefix = "prescription")` ile `@Validated` record; değerler `application.yml`'de.
- Zamana bağlı her şey `Clock` bean'i üzerinden.

### Kalıcılık

- Şema yalnızca Flyway migration'larıyla değişir: `V{n}__{aciklama}.sql`. Uygulanmış migration düzenlenmez, yenisi eklenir. `spring.jpa.hibernate.ddl-auto=validate`.
- Sorgulanan her kolon ve her foreign key için indeks migration'da tanımlanır.
- `FetchType.EAGER` yok; ihtiyaç olan yerde fetch join veya `@EntityGraph`. Akış ve profil endpoint'leri için sorgu sayısı bir testle sınırlanır (N+1 olmadığı kanıtlanır).
- Enum'lar `@Enumerated(EnumType.STRING)`.
- Eşzamanlılık: `learner_topic_mastery` için `@Version` ile iyimser kilitleme ve yeniden deneme; mikro-soru ve puanda tekillik veritabanı unique kısıtıyla sağlanır, ihlal yakalanıp "0 puan" sonucuna çevrilir.
- Entity'ler API'den dışarı çıkmaz; her zaman DTO.

### Domain kodu

- `domain` paketleri Spring, JPA, Jackson bağımlılığı içermez (ArchUnit testi zorlar).
- Değişmezler (invariant) constructor'da korunur: örneğin `Mastery` 0–1 dışı değer kabul etmez.
- Algoritma sınıfları saf fonksiyon gibi davranır: aynı girdi, aynı çıktı. Rastgelelik gerekiyorsa `RandomGenerator` dışarıdan verilir.

### Hata yönetimi ve log

- Genel `catch (Exception e)` yok; yakalanan her istisna ya anlamlı bir domain hatasına çevrilir ya da yeniden fırlatılır. İstisna yutulmaz.
- Log'lar SLF4J parametreli biçimde (`log.info("Exam submitted attemptId={}", attemptId)`), string birleştirme yok.
- Seviyeler: iş olayları `INFO`, kurtarılabilir sorunlar `WARN`, müdahale gerektirenler `ERROR`. Döngü içinde `INFO` log yok.
- Log'da kişisel veri, token veya `correct_key` yok.

## 3. Frontend (React + TypeScript)

### Yapı

```
doping-prescription-ui/src/
├── app/            Router (iç içe layout route'ları), provider'lar, layout'lar (AppShell, DopingleLayer, PanelLayout)
├── features/       Özellik bazlı klasörler (bileşenler, hook'lar, api.ts)
│   ├── auth/
│   ├── home/
│   ├── dopingle/   Player, mikro-soru paneli, useQuizFlow, useImpression, useFeedNavigation, useQuizTimer…
│   ├── exams/
│   ├── learner/
│   └── admin/
├── shared/
│   ├── api/        Üretilmiş OpenAPI tipleri, fetch sarmalayıcısı, query key fabrikası, puan ve profil sorguları
│   ├── session/    Oturum context'i ve useSession (provider app/session'da)
│   ├── ui/         Paylaşılan bileşenler ve hook'lar: Skeleton, useFocusTrap, useMenuNavigation…
│   ├── i18n/       tr.ts: tüm arayüz metinleri tek yerde
│   └── lib/        Saf yardımcılar
└── styles/         globals.css (tasarım değişkenleri)
```

Bir özellik başka bir özelliğin iç dosyalarını ve `app` katmanını import etmez; ortak olan `shared`'a taşınır (ESLint `import/no-restricted-paths` ile zorlanır; özellik listesi `src/features` klasörlerinden okunur).

### Kurallar

- TypeScript `strict`; `any`, `as` ile tip zorlaması ve `@ts-ignore` yok.
- Sunucu verisi yalnızca TanStack Query ile; veri çekmek için `useEffect` kullanılmaz. Query key'ler tek bir fabrikadan gelir; mutation'lar ilgili query'leri geçersiz kılar.
- İstemci durumu yerel (`useState`, `useReducer`); global durum yalnızca oturum bilgisi için (React context).
- Türetilebilen durum saklanmaz, render sırasında hesaplanır.
- İş mantığı bileşenlerden özel hook'lara çıkarılır: `useSimulatedPlayer`, `useInteractionBatcher`, `useQuizTimer`, `useProfilePolling`.
- Bileşenler küçük ve tek amaçlı; 150 satırı geçen bileşen bölünür.
- Renkler yalnızca tasarım değişkenleri üzerinden (Tailwind temasına bağlı); bileşen içinde sabit hex değer yok.
- Erişilebilirlik: tıklanabilir öğeler `button`/`a`; ikon butonlarda `aria-label`; klavye ile kullanılabilirlik (Dopingle ↑/↓/Space, cevap kâğıdı); odak görünür.
- Her veri ekranında yükleme, boş ve hata durumu; beklenmeyen hatalar için route seviyesinde error boundary.
- Formlar React Hook Form + zod. (MVP'de serbest metin girişi olan form yok; cevap kâğıdı ve mikro-soru tek tıklık seçimlerdir. Kütüphane ilk form ekranıyla eklenir.)

## 4. Test stratejisi

| Seviye | Ne | Araç | Nerede |
| --- | --- | --- | --- |
| Birim | Domain algoritmaları (ustalık, sıralama, kurallar, etiketleme, puan, şablonlar) | JUnit 5, AssertJ, parametreli testler | `domain` paketleri |
| Modül | Bir modülün use case'leri, gerçek veritabanıyla | `@ApplicationModuleTest`, Testcontainers | Her modül |
| Entegrasyon | Uçtan uca event zinciri: deneme → Kafka → profil → akış | `@SpringBootTest`, Testcontainers (PG, Redis, Kafka), Awaitility | `doping-prescription-core/src/test/.../flow` |
| Mimari | Modül sınırları, domain saflığı, katman kuralları | Spring Modulith `verify()`, ArchUnit | `ModularityTests`, `ArchitectureTests` |
| API | Her endpoint'in başarılı ve hata yanıtları, yetki | MockMvc veya RestAssured | `web` paketleri |
| Sözleşme | Üretilen OpenAPI'nin repodaki `openapi.json` ile aynı olması | JUnit | `OpenApiContractTest` |
| Frontend bileşen | Mikro-soru paneli, cevap kâğıdı, neden hapı, ustalık çubuğu | Vitest, RTL, MSW | `*.test.tsx` |
| Frontend hook | Simüle oynatıcı, etkileşim toplayıcı, profil yoklama | Vitest (sahte zamanlayıcılar) | `*.test.ts` |
| E2E | Demo senaryosu | Playwright | `e2e/` |

Kurallar:

- Testler Given / When / Then yapısında; isimler davranışı anlatır (`returnsZeroPointsWhenQuestionAlreadyAnswered`).
- `Thread.sleep` yok; asenkron bekleme Awaitility ile.
- Testler birbirinden bağımsız; sabit `Clock` ve sabit rastgelelik tohumu.
- `docs/algoritma.md` içindeki örnek tablolar birebir test vakasıdır.
- Mock yalnızca modül sınırlarında; domain sınıfları mock'lanmaz.

## 5. Kalite kapıları

`./mvnw verify` ve `npm run check` aşağıdakilerin hepsini çalıştırır; biri başarısızsa build kırılır.

| Kontrol | Eşik |
| --- | --- |
| Spotless, Checkstyle | 0 ihlal |
| SpotBugs | 0 bulgu (gerekçeli istisnalar `spotbugs-exclude.xml` içinde, yorumlu) |
| JaCoCo | `domain` ve `application` paketlerinde satır kapsamı en az %80 |
| Spring Modulith, ArchUnit | Tüm kurallar geçer |
| ESLint | 0 hata, 0 uyarı |
| `tsc --noEmit` | 0 hata |
| Vitest | Tümü geçer |

GitHub Actions bu adımları her push ve pull request'te çalıştırır; e2e testi ayrı bir job'da Docker Compose ile çalışır.

## 6. README

README şunları içerir:

1. Projenin bir paragraflık özeti ve demo senaryosuna bağlantı
2. Gereksinimler (Java 21, Node.js 24 LTS, Docker) ve kurulum komutları
3. Mimari diyagramı (Mermaid) ve modül tablosu, `docs/mimari.md` bağlantısı
4. Event akışı ve Kafka topic'leri
5. Test ve kalite komutları
6. ADR listesi
7. Bilinen sınırlamalar ve "üretime geçerken yapılacaklar" listesi

## 7. Review kontrol listesi

Her aşama sonunda bu listeyle kendi kodunu incele.

**Mimari**
- [ ] Modül sınırları ihlal edilmemiş; başka modülün yalnız `api` paketi kullanılmış.
- [ ] Domain mantığı `domain` paketinde ve framework'ten bağımsız.
- [ ] Controller'da iş kuralı yok.
- [ ] Senkron/asenkron seçimi `docs/mimari.md` §4 ile uyumlu.

**Doğruluk**
- [ ] Transaction sınırları doğru; event'ler outbox üzerinden yayınlanıyor.
- [ ] Tüketiciler idempotent; aynı event iki kez gelince sonuç değişmiyor.
- [ ] Eşzamanlı isteklerde çift kayıt veya çift puan oluşmuyor.
- [ ] Kaynak sahipliği kontrol ediliyor (bir öğrenci başkasının verisine erişemiyor).
- [ ] `correct_key` hiçbir öğrenci yanıtında, log'da veya event'te yok.

**Performans**
- [ ] N+1 sorgu yok; sorgulanan kolonlarda indeks var.
- [ ] Akış isteği Redis kuyruğundan dönüyor; Redis yokken de çalışıyor.

**Okunabilirlik**
- [ ] İsimler alan dilini yansıtıyor (`Mastery`, `CandidatePool`, `ReasonTemplate`).
- [ ] Metotlar kısa, tek iş yapıyor; iç içe koşullar en fazla iki seviye.
- [ ] Sihirli sayı yok; değerler config'de.

**Test**
- [ ] Yeni davranışın testi aynı commit'te.
- [ ] Hata yolları test edilmiş, yalnızca mutlu yol değil.
- [ ] Testler deterministik.

**Frontend**
- [ ] `any` ve tip zorlaması yok; API tipleri üretilmiş tiplerden.
- [ ] Yükleme, boş ve hata durumları var.
- [ ] Klavye ile kullanılabilir; ikon butonlarda `aria-label` var.
- [ ] Metinler `i18n/tr.ts` içinde.

**Teslim**
- [ ] Commit'ler küçük ve Conventional Commits biçiminde.
- [ ] Önemli kararlar ADR olarak yazılmış.
- [ ] README ve `docs/ilerleme.md` güncel.
