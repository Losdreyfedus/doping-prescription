# Doping Prescription

Bu dosya, projeyi sıfırdan kuracak kodlama agent'ı için ana brief'tir. Önce bu dosyayı baştan sona oku, sonra `docs/` altındaki ilgili dokümanı aç. Bu dosya ile `docs/` çelişirse bu dosya geçerlidir.

**Rolün:** Kıdemli (senior) bir Java + React full stack geliştirici gibi çalış. Yazdığın kod, sunumdan önce deneyimli geliştiriciler tarafından **kod incelemesinden (code review) geçirilecek**. Çalışan bir demo tek başına yeterli değil: mimari, okunabilirlik, test kalitesi ve commit geçmişi de değerlendirilecek.

## 1. Problem

Doping Hafıza'da Dopingle (reels mantığında kısa video akışı), deneme sınavları, soru çözümü ve puan sistemi birbirinden kopuk çalışıyor:

- Öğrenci Dopingle'ı açtığında videolar rastgele geliyor.
- Öğrenci denemede matematikte olasılık sorularını kaçırsa bile akışına olasılık videosu düşmüyor.
- Doping'in elinde çok güçlü bir veri var (deneme sonuçları, soru performansı, izleme geçmişi), ama bu veri öğrencinin karşısına kişiselleştirilmiş bir deneyim olarak çıkmıyor.

Instagram, kullanıcı YDS'ye çalışıyorsa ona YDS videosu gösterir, çünkü ilgi verisine sahiptir. Doping bundan daha iyisini yapabilir, çünkü öğrencinin **neyi bilmediğini** biliyor.

## 2. Ne istiyoruz

Öğrenciden hiçbir ekstra işlem istemeden eksiklerini tespit edip Dopingle akışını bu eksiklere göre kuran bir sistem. Sistem dört adımda çalışır:

1. **Teşhis:** Öğrenci denemeyi her zamanki gibi çözer. Sonuç gönderildiği anda sistem konu bazlı eksikleri otomatik çıkarır. Öğrenci hiçbir şey yüklemez veya seçmez.
2. **Reçete:** Dopingle akışı, eksik konulardan kısa videolarla dolar. Her kartta "neden bu video" açıklaması bulunur.
3. **Kontrol:** Video bittiğinde öğrenciye o konudan tek bir mikro-soru sorulur. Doğru cevap puan kazandırır.
4. **İyileşme:** Her cevap öğrencinin konu ustalık skorunu günceller; "Eksiklerim" ekranında ilerleme görünür.

**Bu proje bir sunum demosudur.** Amaç, şirket yönetimine (C-level) bu fikrin uçtan uca çalıştığını canlı göstermektir. Gerçek Doping sistemlerine bağlanılmaz; deneme, video ve puan altyapısı bu projenin içinde kurulur. Mimari ise ileride gerçek sisteme taşınabilecek şekilde, üretim kalitesinde kurulur.

Başarı ölçütleri:

- `docs/demo-senaryosu.md` içindeki senaryo, temiz bir kurulumdan sonra baştan sona hatasız ve 3 dakikanın altında oynatılabilir.
- Kod, `docs/kod-standartlari.md` içindeki review kontrol listesinden geçer.

## 3. Kapsam

**Kapsam içi:**

- Tek sınav ve tek ders: **TYT Matematik**, 10 konu
- Öğrenci uygulaması (Doping Hafıza'nın mevcut web arayüzüne benzeyen masaüstü web; bkz. `docs/arayuz.md`): Ana Sayfa, Dopingle, Testler ve Denemeler, Eksiklerim, Puanlarım
- Deneme modülü: hazır mini denemeler, çözme, gönderme, sonuç
- Öğrenci profili: konu bazlı ustalık skoru
- Kişiselleştirilmiş akış ve "neden bu video" açıklaması
- Mikro-soru ve puan
- Admin paneli: demo kontrolü, "neden bu akış" debug ekranı, kural tabanlı içerik etiketleme
- Etki paneli: simüle edilmiş öğrenci verisiyle A/B karşılaştırması (her yerde "Simülasyon" etiketiyle)

**Kapsam dışı:**

- LLM, üretken yapay zeka veya makine öğrenmesi modeli (çalışma zamanında hiçbir şekilde)
- Gerçek Doping sistemlerine entegrasyon, gerçek kullanıcı verisi
- Şifreli giriş, kayıt, ödeme
- Native mobil uygulama, mobil uyumlu tasarım
- Soru bankası modülü (v2)
- Mikroservislere bölme (bkz. `docs/adr/0001-moduler-monolit.md`)

## 4. Teknoloji

| Katman | Seçim |
| --- | --- |
| Backend dili | **Java 21 (LTS)** |
| Backend framework | **Spring Boot** (güncel kararlı sürüm), Spring Web MVC, Spring Security, Spring Data JPA, Spring Data Redis, Spring Kafka |
| Modül sınırları | **Spring Modulith** (modüller arası bağımlılıklar testle doğrulanır, event publication registry = outbox) |
| Build | **Maven** (`./mvnw` wrapper ile) |
| Veritabanı | PostgreSQL 16, şema **Flyway** migration'larıyla |
| Önbellek | Redis 7 (akış kuyruğu, profil önbelleği) |
| Message broker | **Apache Kafka** (KRaft, tek node), modüller arası domain event'leri için (bkz. `docs/adr/0002-kafka-ve-outbox.md`) |
| Eşleme | MapStruct (entity ↔ DTO) |
| API sözleşmesi | springdoc-openapi ile üretilen OpenAPI 3; frontend tipleri bundan üretilir |
| Backend test | JUnit 5, AssertJ, Mockito, **Testcontainers** (PostgreSQL, Redis, Kafka), Spring Modulith test desteği, ArchUnit |
| Kod kalitesi (backend) | Spotless (palantir-java-format), Checkstyle, SpotBugs, JaCoCo |
| Frontend | **React 18 + TypeScript (strict) + Vite**, React Router, TanStack Query, Tailwind CSS, React Hook Form + zod, Recharts |
| Frontend API istemcisi | `openapi-typescript` ile OpenAPI'den üretilen tipler + ince bir `fetch` sarmalayıcısı |
| Frontend test | Vitest, React Testing Library, MSW |
| Kod kalitesi (frontend) | ESLint (typescript-eslint strict, react-hooks, jsx-a11y), Prettier |
| E2E | Playwright (demo senaryosu) |
| CI | GitHub Actions: backend build + test + kalite, frontend lint + typecheck + test |
| Çalıştırma | Docker Compose (postgres, redis, kafka, isteğe bağlı kafka-ui) |

Repo düzeni:

```
doping-prescription/                 Repo kökü (tek git reposu)
├── doping-prescription-core/        Backend: Spring Boot uygulaması (tek Maven modülü, paketler = Modulith modülleri)
├── doping-prescription-ui/          Frontend: React + Vite uygulaması
├── e2e/                             Playwright testleri (demo senaryosu)
├── docs/                            Bu dokümanlar ve ADR'ler
├── docker-compose.yml               postgres, redis, kafka, kafka-ui
├── .github/workflows/               CI: core ve ui için ayrı job'lar
├── AGENTS.md
├── CLAUDE.md
└── README.md
```

Proje kimlikleri:

| | Değer |
| --- | --- |
| Maven `groupId` / `artifactId` | `com.doping` / `doping-prescription-core` |
| Java kök paketi | `com.doping.prescription` |
| Ana sınıf | `com.doping.prescription.PrescriptionApplication` |
| npm paket adı | `doping-prescription-ui` |

Kurulum (repo kökünden; uygulamalar ayrı terminallerde):

```bash
docker compose up -d
```

```bash
cd doping-prescription-core && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

```bash
cd doping-prescription-ui && npm ci && npm run dev
```

API `http://localhost:8080`, web `http://localhost:5173`, OpenAPI arayüzü `http://localhost:8080/swagger-ui.html`. `demo` profili başlangıçta seed verisini yükler. `doping-prescription-core` içinde `./mvnw verify` tüm backend testlerini ve kalite kontrollerini, `doping-prescription-ui` içinde `npm run check` frontend kontrollerini, `e2e` içinde `npm test` demo senaryosunu çalıştırır.

## 5. Kesin kurallar

1. **Çalışma zamanında LLM veya üretken yapay zeka kullanma.** Etiketleme anahtar kelime kurallarıyla, "neden bu video" metinleri şablonlarla üretilir.
2. **Modül sınırlarına uy.** Modüller: `auth`, `content`, `exam`, `learner`, `feed`, `quiz`, `points`, `admin`, `simulation`, ortak kod için `shared`. Bir modül başka bir modülün yalnızca `api` paketindeki public arayüzleri ve event'leri kullanabilir; repository, entity ve iç servislerine erişemez. `ApplicationModules.of(...).verify()` testi bunu zorlar. Ayrıntı: `docs/mimari.md`.
3. **Asenkron iş akışları event ile.** Deneme sonucu → profil → akış zinciri Kafka üzerinden event'lerle akar. Senkron modül çağrısına yalnızca kullanıcının anında görmesi gereken sonuçlarda izin var (mikro-soru cevabında puan ve ustalık değişimi; bkz. `docs/mimari.md` §4).
4. **Domain mantığı framework'ten bağımsız.** Ustalık hesabı, akış sıralaması, etiketleme ve puan kuralları Spring'e bağımlı olmayan saf Java sınıflarıdır ve birim testleriyle doğrulanır.
5. **Doğru cevap sızmaz.** `correct_key` hiçbir öğrenci yanıtında, log'da veya event'te yer almaz; cevap kontrolü her zaman sunucuda yapılır.
6. **İdempotent yazma.** Event'lerde `eventId`, etkileşimlerde `clientEventId`, puanda `refId` tekrar işlenmez.
7. **Akış asla hata vermez.** Profil veya sıralama hata verirse kullanıcıya `personalized: false` ile fallback akış döner.
8. **Sayısal değerler config'de.** `docs/algoritma.md` §9'daki her değer `application.yml` içindeki `prescription.*` altından, doğrulanmış bir `@ConfigurationProperties` record'u ile okunur.
9. **Arayüz dili Türkçe**, kod, tablo, kolon, endpoint, commit mesajı ve kod yorumları İngilizce.
10. **Belirsizlikte sessiz tahmin yok.** Varsayım yaptığın yere `TODO(spec):` yorumu bırak ve iş sonunda liste halinde raporla. Önemli bir teknik karar alırsan `docs/adr/` altına yeni bir ADR yaz.
11. **Telifli içerik kullanma.** İnternetten video, görsel, logo veya soru indirme. Videolar için `docs/arayuz.md` içindeki simüle oynatıcıyı kullan.
12. **Seed içerikleri doğru olsun.** Soruları sen yazacaksın; her sorunun tek doğru cevabı olmalı ve matematiksel olarak doğru olmalı. Seed dosyalarının başına "Editör kontrolünden geçmedi" notu koy.
13. **Kod kalitesi pazarlık konusu değil.** `docs/kod-standartlari.md` içindeki kurallar her commit'te geçerlidir; `./mvnw verify` ve `npm run check` kırmızıyken commit atma.

## 6. Git akışı

- Her aşama ayrı bir branch'te (`feat/03-exam-module` gibi) ve küçük, anlamlı commit'lerle ilerler. Commit mesajları [Conventional Commits](https://www.conventionalcommits.org/) biçiminde (`feat(exam): submit attempt and publish result event`).
- Bir commit tek bir mantıksal değişiklik içerir; kod ve onun testi aynı commit'tedir.
- Aşama bittiğinde `main`'e merge edilir; merge öncesi CI yeşil olmalı.

## 7. Uygulama sırası

Her aşama bir öncekine dayanır. Bir aşamanın kabul kriterleri geçmeden sonrakine geçme. Her aşama sonunda testleri çalıştır ve kısa bir ilerleme notu yaz (`docs/ilerleme.md`).

0. **Kalite altyapısı ve iskelet**
    - Repo düzeni, Docker Compose, Spring Boot ve Vite uygulamaları, `GET /actuator/health`, Spotless, Checkstyle, SpotBugs, JaCoCo, ESLint, Prettier, GitHub Actions. `docs/adr/` altındaki dört ADR hazırdır; onlarla çelişen bir karar alırsan yeni ADR yaz, eskisini "Yerini aldı" olarak işaretle.
    - Kabul: kurulum komutları temiz bir makinede çalışır; health endpoint'i postgres, redis ve kafka durumunu raporlar; CI yeşil; `ApplicationModules.verify()` testi boş modüllerle geçer.
1. **Veri modeli ve seed**
    - `docs/veri-modeli.md` içindeki tüm tablolar (Flyway), JPA entity'leri, `docs/seed-verisi.md` içindeki içerik.
    - Kabul: migration'lar Testcontainers üzerinde temiz veritabanına uygulanır; seed tekrar çalıştırıldığında veriyi çoğaltmaz; seed sonrası 10 konu, 40 video, 2 mini deneme ve 4 kullanıcı vardır.
2. **Kimlik ve uygulama kabuğu**
    - Demo giriş (`auth` modülü, JWT), Spring Security yapılandırması, rol bazlı yetki; Doping benzeri uygulama kabuğu (sol menü, üst çubuk), tasarım değişkenleri, yazı tipleri, API istemcisi üretimi.
    - Kabul: öğrenci seçilince sol menülü uygulama açılır; admin endpoint'leri öğrenciye 403 döner; sayfa yenilenince oturum korunur.
3. **Deneme modülü**
    - Deneme listesi, çözme ekranı, gönderme, sonuç ekranı, `exam.result-published` event'i (outbox üzerinden Kafka'ya).
    - Kabul: deneme gönderildiğinde event tam bir kez yayınlanır (entegrasyon testi); sonuç ekranında konu bazlı doğru/yanlış/boş görünür.
4. **Öğrenci profili**
    - Kafka consumer, sinyal işleme, ustalık hesabı, decay, Redis önbelleği, `learner.mastery-changed`, Eksiklerim ekranı.
    - Kabul: `docs/algoritma.md` §1'deki örnek değerler birim testinde birebir çıkar; deneme gönderildikten sonra en geç 2 saniye içinde Eksiklerim ekranı güncellenir; aynı event iki kez gelince skor değişmez.
5. **Kişiselleştirilmiş akış**
    - Aday havuzları, sıralama, kurallar, "neden bu video" şablonları, Redis kuyruğu, fallback, Dopingle ekranı, simüle oynatıcı, etkileşim kaydı.
    - Kabul: demo senaryosunun 3. adımında akışın ilk 5 kartından en az 3'ü zayıf konudandır; aynı konudan art arda en fazla 2 kart gelir; kişiselleştirme kapalı öğrenci rastgele akış görür; Redis kapalıyken akış yine döner.
6. **Mikro-soru ve puan**
    - Quiz token'ı, mikro-soru penceresi, cevap, açıklama, puan, seri, Puanlarım ekranı, suistimal kuralları.
    - Kabul: `docs/api.md` hata kodları tablosundaki her durum için entegrasyon testi vardır; aynı soruya ikinci cevap 0 puan alır; eşzamanlı iki cevap isteği çift puan üretmez.
7. **Admin paneli**
    - Demo kontrolü (sıfırla, persona yükle), "neden bu akış" debug ekranı, karşılaştırma, anahtar kelime ve etiket onay ekranı.
    - Kabul: "Sıfırla" sonrası demo senaryosu baştan oynatılabilir; debug ekranı her kartın skor bileşenlerini gösterir.
8. **Etki paneli (simülasyon)**
    - Sentetik öğrenci üretimi, kontrol ve deney grupları, karşılaştırma grafikleri.
    - Kabul: ekranın başlığında ve her grafikte "Simülasyon verisi" yazar; üretim deterministiktir (sabit seed).
9. **Demo cilası, e2e ve dokümantasyon**
    - Demo senaryosunu Playwright testine dök, yükleme ve boş durumlar, animasyonlar, README.
    - Kabul: e2e testi senaryonun tamamını geçer; README `docs/kod-standartlari.md` §6'daki içeriği kapsar.

## 8. Dokümanlar

| Dosya | İçerik |
| --- | --- |
| `docs/demo-senaryosu.md` | Sunumda adım adım ne olacağı; ürünün hedefi budur |
| `docs/mimari.md` | Modüller, paket yapısı, event akışı, senkron/asenkron kararları |
| `docs/kod-standartlari.md` | Backend ve frontend kod kuralları, test stratejisi, review kontrol listesi |
| `docs/adr/` | Mimari karar kayıtları |
| `docs/veri-modeli.md` | Tablolar ve kolonlar |
| `docs/api.md` | Endpoint'ler, DTO'lar, hata formatı, Kafka topic'leri |
| `docs/algoritma.md` | Ustalık skoru, akış sıralaması, kurallar, şablonlar, config |
| `docs/arayuz.md` | Ekranlar, tasarım değişkenleri, bileşenler, simüle oynatıcı |
| `docs/seed-verisi.md` | Konular, videolar, denemeler, mikro-sorular, demo öğrenciler |

## 9. Bitti tanımı

- Kurulum komutları temiz bir makinede çalışır.
- `./mvnw verify`, `npm run check` ve e2e testleri geçer; CI yeşil.
- Domain paketlerinde satır kapsamı en az %80 (JaCoCo kuralı build'i kırar).
- Demo senaryosu elle oynatıldığında her adımda beklenen ekran görünür.
- README ve ADR'ler günceldir.
- Tüm `TODO(spec):` notları listelenmiştir.
