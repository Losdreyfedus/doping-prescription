# Algoritma

v1 makine öğrenmesi ve LLM kullanmaz. Her karar açıklanabilir olmalı: debug ekranı her kartın neden orada olduğunu sayılarla gösterebilmeli. Tüm sayısal değerler §9'daki config anahtarlarından okunur.

## 1. Ustalık (mastery) güncellemesi

Her konu için ilk değer `mastery = 0.5`, `confidence = 0`, `attempts = 0`.

Her sinyal, mastery'yi sonuca doğru bir adım çeker:

```
m_yeni = m + alpha * w * (y - m)
```

- `m`: mevcut mastery
- `y`: sonuç (doğru = 1, yanlış veya boş = 0)
- `w`: sinyal ağırlığı (aşağıdaki tablo)
- `alpha`: öğrenme hızı (`mastery.alpha`, varsayılan 0.3)

| Sinyal | w | Mastery'yi etkiler mi? |
| --- | --- | --- |
| EXAM_CORRECT / EXAM_WRONG | 1.0 | Evet |
| EXAM_BLANK | 0.8 | Evet (y = 0) |
| QUIZ_CORRECT / QUIZ_WRONG | 0.4 | Evet |
| VIDEO_COMPLETED | — | Hayır; yalnızca `last_served_at` (v1'de içerik kalite skoru statiktir, seed'den gelir) |
| VIDEO_SKIPPED | — | Hayır; yalnızca oturum içi erteleme kuralı |

Mastery'yi etkileyen her sinyalde `attempts` 1 artar, doğruysa `correct` da artar ve:

```
confidence = 1 - exp(-attempts / mastery.confidence_k)     (k varsayılan 5)
```

Bir denemedeki sinyaller soru sırasıyla, tek transaction içinde uygulanır. Deneme işlenirken konu başına `last_exam_attempt_id`, `last_exam_total`, `last_exam_correct` de yazılır.

**Unutma (decay):** Okuma anında uygulanır, tabloya yazılmaz:

```
m_etkin = m * exp(-mastery.decay_lambda * gün)     (gün = now - last_signal_at, gün cinsinden)
```

**Hızlı cevap:** `responseMs < quiz.min_response_ms` olan mikro-soru cevapları sinyal üretmez ve puan almaz.

**Hassasiyet:** `mastery` ve `confidence` hesapta ve veritabanında (`double precision`) tam hassasiyetle tutulur; ara değerler yuvarlanmaz, çünkü yuvarlanıp saklanan değerler her sinyalde hata biriktirir. 3 ondalığa yuvarlama (`RoundingMode.HALF_UP`) yalnızca API yanıtında ve gösterimde yapılır.

### Ustalık bandı

Tek bir tanım vardır (`domain` paketinde `MasteryBand`) ve hem Eksiklerim ekranındaki renk bandı hem akışın `WEAK_TOPIC` havuzu bunu kullanır. Böylece "kırmızı konu = akışta reçete olarak gelen konu" kuralı her zaman tutarlıdır. Bant, decay uygulanmış `m_etkin` üzerinden hesaplanır.

| Bant | Koşul |
| --- | --- |
| `UNKNOWN` | Konuda mastery'yi etkileyen hiç sinyal yok (`attempts = 0`) |
| `WEAK` | `m_etkin < learner.mastery.band.weak-threshold` (0.6) |
| `MEDIUM` | `m_etkin < learner.mastery.band.strong-threshold` (0.8) |
| `STRONG` | Diğer durumlar |

### Örnek (demo senaryosu, varsayılan config)

Deniz, Olasılık konusundaki 3 deneme sorusunu yanlış yapar:

| Adım | Hesap | mastery (saklanan) | API'de |
| --- | --- | --- | --- |
| Başlangıç | | 0.5 | 0.500 |
| 1. yanlış | 0.5 + 0.3·1·(0 − 0.5) | 0.35 | 0.350 |
| 2. yanlış | 0.35 + 0.3·(0 − 0.35) | 0.245 | 0.245 |
| 3. yanlış | 0.245 + 0.3·(0 − 0.245) | 0.1715 | 0.172 |
| Mikro-soru doğru | 0.1715 + 0.3·0.4·(1 − 0.1715) | 0.27092 | 0.271 |

`confidence` 3 denemeden sonra 0.4512 (API'de 0.451), mikro-sorudan sonra 0.5507 (API'de 0.551) olur. Ekranda "%17 → %27" görünür. Birim testi saklanan değerleri `1e-9` toleransla, API değerlerini 3 ondalık olarak birebir doğrular.

## 2. Aday havuzları

Akış sayfası (`feed.page_size`, varsayılan 10) dört havuzdan doldurulur. Akışa yalnızca `status = LIVE` videolar girer; bir videonun konusu, `content_topic_map` içinde en yüksek `weight`'e sahip konudur.

| Havuz | Seçim kuralı | Kota |
| --- | --- | --- |
| `WEAK_TOPIC` | Bandı `WEAK` olan konular (§1, `MasteryBand`), `m_etkin` artan sırada ilk `feed.weak_topic_count` (5) konu; bu konuların videoları | 0.60 |
| `REVIEW_DUE` | Bandı `MEDIUM` veya `STRONG` olan ve son sinyalin üzerinden `feed.review_after_days` (7) gün geçmiş konuların videoları | 0.15 |
| `EXPLORE` | Hiç sinyali olmayan konular, `exam_weight` azalan sırada; bu konuların videoları | 0.15 |
| `POPULAR` | Tüm LIVE videolar, `quality_score` azalan sırada | 0.10 |

Kart sayıları **en büyük kalan yöntemiyle** dağıtılır: her havuz `floor(kota · sayfa)` kart alır, kalan kartlar kesir kısmı en büyük olan havuzlara birer birer verilir; eşitlikte havuz sırası `WEAK_TOPIC, REVIEW_DUE, EXPLORE, POPULAR` geçerlidir. 10 kartlık sayfada sonuç 6 / 2 / 1 / 1'dir. Bir havuzda yeterli aday yoksa eksik kota sırayla `WEAK_TOPIC → EXPLORE → POPULAR` havuzlarına aktarılır.

**Hiç sinyali olmayan öğrenci:** `EXPLORE` %70, `POPULAR` %30; yanıt `personalized: false`.

**Kişiselleştirme kapalı:** Tüm LIVE videolardan, `sessionId` ile tohumlanan karıştırma; havuz `FALLBACK`; yanıt `personalized: false`.

## 3. Sıralama

Her aday için 0–1 arasında bileşenler hesaplanır:

| Bileşen | Hesap |
| --- | --- |
| weakness | Hedef konu için `(1 − m_etkin) · max(confidence, 0.3)`; `EXPLORE` ve `POPULAR` adaylarında 0 |
| examImportance | `topic.exam_weight` |
| quality | `content_item.quality_score` |
| freshness | Öğrenci videoyu hiç izlememişse 1.0, izlemişse 0.2 |
| difficultyFit | `1 − abs((difficulty − 1) / 4 − m_etkin)` |

```
skor = 0.45·weakness + 0.20·examImportance + 0.15·quality + 0.10·freshness + 0.10·difficultyFit
```

Ağırlıklar `feed.rank_weights` içindedir. `users.exam_date` 60 günden yakınsa examImportance ağırlığı `feed.exam_near_weight` (0.30) olur.

Havuz kotalarıyla seçilen adaylar skor azalan sırada dizilir, sonra §4'teki kurallar açgözlü (greedy) biçimde uygulanır: sıradaki aday bir kuralı bozuyorsa atlanır ve bir sonraki denenir. Atlanan aday, ceza bilgisiyle `score_breakdown.diversityPenalty` alanına yazılır.

Demo kontrolü: senaryodaki deneme sonrası Olasılık için weakness = (1 − 0.1715) · 0.4512 ≈ 0.374, Permütasyon-Kombinasyon (1 doğru + 1 boş, mastery ≈ 0.494, confidence ≈ 0.330) için ≈ 0.167 olur; ilk kart Olasılık videosudur.

## 4. Kurallar

1. Aynı konudan art arda en fazla `feed.max_same_topic_in_row` (2) kart.
2. Son `feed.rewatch_block_days` (14) gün içinde tamamlanmış video tekrar gösterilmez; `REVIEW_DUE` havuzu hariç.
3. Aynı oturumda aynı konudan `feed.skip_defer_count` (3) video ilk 3 saniyede geçildiyse, o konu oturumun geri kalanında bir sonraki sayfaya ertelenir.
4. Aynı sayfada aynı video iki kez yer almaz.

Akış Redis'te kullanıcı başına bir kuyruk olarak tutulur (`feed:queue:{userId}`, en fazla `feed.queue_size` (30) kart, ömrü `feed.queue_ttl` (30m)). Kuyrukta `feed.queue_refill_threshold` (5) karttan az kalırsa yeniden doldurulur. `learner.mastery-changed` event'i geldiğinde kuyruk silinir.

**Oturum bütünlüğü:** Oturumda gösterilen içerik id'leri Redis'te `feed:session:{sessionId}` altında bir set olarak tutulur. Kuyruk oturum ortasında silinip yeniden kurulduğunda (örn. mikro-soru cevabından sonra) bu setteki videolar aday dışı bırakılır; böylece aynı oturumda bir video iki kez gelmez. `cursor`, `sessionId` ve sonraki pozisyonu taşır. Redis yoksa set, `feed_impression` kayıtlarından (`session_id`) okunur.

## 5. Kural tabanlı etiketleme

**Normalizasyon:** Türkçe küçük harfe çevir (`toLocaleLowerCase('tr')`), `ç→c, ğ→g, ı→i, ö→o, ş→s, ü→u`, noktalama işaretlerini boşluğa çevir, birden fazla boşluğu teke indir.

**Eşleşme (kelime başı):** Türkçe eklemeli bir dil olduğu için tam kelime eşleşmesi kullanılmaz ("kume" → "kumeler", "olasilik" → "olasiligin" eşleşmeli). Normalize edilmiş anahtar kelime token'lara bölünür; anahtar kelimenin her token'ı, metindeki **ardışık** token'ların başı (prefix) olmalıdır. Örn. `tanim kumesi`, "tanım kümesini bulma" metnine eşleşir. Ünsüz yumuşaması da kabul edilir: p/t/k ile biten bir token'ın b/d/g ile biten biçimi de eşleşir (`olasilik` → "olasılığın", `kitap` → "kitabı"). 4 karakterden kısa token'lar tam eşleşmelidir (örn. `us`, "ustalık" kelimesine eşleşmez, "üs" kelimesine eşleşir). Bu kurallar parametreli birim testleriyle doğrulanır.

Anahtar kelime başlıkta eşleşiyorsa `weight · 1.0`, yalnızca açıklamada eşleşiyorsa `weight · 0.5` eklenir. Konu skoru 1 ile sınırlıdır. Negatif anahtar kelime eşleşirse konu skoru 0 olur.

**Sonuç:** Skoru `content.tagging.min-confidence` (0.5) ve üstündeki konular `content_topic_map`'e `tag_source = KEYWORD_RULE`, `weight = skor`, `confidence = skor` ile yazılır, video `TAGGED` olur. Hiç eşleşme yoksa video `DRAFT` kalır. Admin onayıyla (`POST /api/admin/content/:id/tags`) etiket kesinleşir ve video `LIVE` olur.

## 6. "Neden bu video?" şablonları

| Havuz | Koşul | Şablon |
| --- | --- | --- |
| WEAK_TOPIC | Konuda son deneme verisi var | `Son denemende {konu}: {total} soruda {correct} doğru` |
| WEAK_TOPIC | Deneme verisi yok (mikro-sorulardan zayıf) | `{konu} sorularında zorlanıyorsun` |
| REVIEW_DUE | | `{konu} konusunu {gün} gündür tekrar etmedin` |
| EXPLORE | | `TYT'de sık çıkan konu: {konu}` |
| POPULAR | | `Bu hafta en çok izlenenlerden` |
| FALLBACK | | `Dopingle'da yeni` |

Türkçe ek uyumu gerektiren kalıplardan kaçınıldı; şablonlara ek ekleme.

## 7. Puan kuralları

| Kural | Config | Varsayılan |
| --- | --- | --- |
| Doğru cevap | `points.correct` | 10 |
| `WEAK_TOPIC` kartındaki soruya doğru cevap | `points.weak_topic_correct` | 15 |
| Art arda doğru serisi bonusu | `points.streak_length` / `points.streak_bonus` | 5 doğruda +20 |
| Yanlış cevap | | 0, seri sıfırlanır |
| Günlük üst sınır | `points.daily_cap` | 200 |
| Aynı soruya ikinci cevap | | 0 |
| Hızlı cevap (`quiz.min_response_ms` altı) | | 0, sinyal yok |

Kartta görünen `pointsAvailable`, havuz `WEAK_TOPIC` ise 15, değilse 10'dur. Günlük sınıra ulaşan öğrenci soru çözmeye devam edebilir; mastery güncellenir, sadece puan yazılmaz. "Gün" `prescription.timezone` (Europe/Istanbul) saat dilimine göre hesaplanır.

Seri `points` modülünün kendi defterinde tutulur (`points_streak`). Quiz modülü her cevapta `PointsLedger`'a sonucu (doğru/yanlış, havuz, `refId`) iletir; yanıt olarak kazanılan puanı ve güncel seriyi alır.

Suistimal önlemleri: soru yalnız `quiz.min_watch_ratio` izlendikten sonra açılır (izleme süresi `feed_interaction` kayıtlarından doğrulanır); `quizToken` tek kullanımlıktır ve kullanıcıya bağlıdır; şıklar her gösterimde karıştırılır; puan yazma `ref_id` ile idempotenttir.

## 8. Etki simülasyonu

Etki paneli, gerçek veri olmadığı için **açıkça etiketlenmiş bir simülasyon** gösterir. Amaç, "ölçüm böyle yapılacak" sorusunu görsel olarak cevaplamaktır, etki iddia etmek değil.

- `simulation.impact.student-count` (400) sentetik öğrenci, `simulation.impact.seed` (42) ile deterministik üretilir; yarısı kontrol, yarısı deney grubu.
- Her öğrencinin 10 konuda gizli bir "gerçek beceri" değeri vardır (0.2–0.9 arası rastgele).
- 4 hafta boyunca her hafta: 12 soruluk bir deneme (doğru olasılığı = beceri), ardından `simulation.impact.videos-per-week` (15) video izleme.
- Deney grubu videolarını mevcut algoritmayla seçer (simüle mastery üzerinden); kontrol grubu rastgele seçer.
- Bir video, izlendiği konunun becerisini `simulation.impact.gain-per-video` (0.01) kadar artırır, en fazla 0.95'e kadar.
- Çıktı: haftalık ortalama net (iki çizgi), başlangıçtaki en zayıf 2 konudaki doğru oranı değişimi (iki çubuk), grup büyüklükleri.

Ekranda ve API yanıtında "Simülasyon verisi; sonuçlar model varsayımlarından gelir, gerçek kullanıcı etkisi değildir" notu bulunur.

## 9. Config

Değerler `doping-prescription-core/src/main/resources/application.yml` içinde, Spring'in kebab-case biçimiyle yazılır. **Her modül kendi grubunu, kendi paketindeki `@Validated @ConfigurationProperties` record'uyla bağlar** (`prescription.<modül>.*`): başka bir modülün config'ini okuyan modül yoktur, config de modül sınırlarına uyar. Geçersiz değerde (örn. toplamı 1 olmayan kotalar) uygulama başlamaz. Ortam değişkeniyle ezilebilir. Süreler `java.time.Duration` tipindedir ve yml'de `30m`, `5m` biçiminde yazılır. Algoritma sınıfları config record'unu değil, kendi ihtiyaç duyduğu değerleri taşıyan küçük parametre nesnelerini alır; böylece domain paketi Spring'e bağımlı olmaz.

Altyapı ayarları `shared` modülüne aittir ve modül adı taşımaz: `prescription.timezone` (`shared.time.TimeProperties`) ve `prescription.health.kafka.timeout` (`shared.messaging.KafkaHealthProperties`, varsayılan `2s`).

Bu dokümandaki kısa adlar (örn. `feed.page_size`) aşağıdaki tam anahtarlara karşılık gelir.

| Modül | Record | Anahtar (`prescription.` önekiyle) | Varsayılan |
| --- | --- | --- | --- |
| shared | `TimeProperties` | `timezone` | Europe/Istanbul |
| learner | `LearnerProperties` | `learner.mastery.alpha` | 0.3 |
| learner | | `learner.mastery.initial` | 0.5 |
| learner | | `learner.mastery.confidence-k` | 5 |
| learner | | `learner.mastery.decay-lambda` | 0.01 |
| learner | | `learner.mastery.signal-weights` | EXAM 1.0, EXAM_BLANK 0.8, QUIZ 0.4 |
| learner | | `learner.mastery.change-event-threshold` | 0.05 |
| learner | | `learner.mastery.band.weak-threshold` / `strong-threshold` | 0.6 / 0.8 |
| feed | `FeedProperties` | `feed.page-size` / `feed.page-size-max` | 10 / 20 |
| feed | | `feed.queue-size` / `feed.queue-ttl` | 30 / 30m |
| feed | | `feed.queue-refill-threshold` | 5 |
| feed | | `feed.quota` | WEAK 0.60, REVIEW 0.15, EXPLORE 0.15, POPULAR 0.10 |
| feed | | `feed.cold-start-quota` | EXPLORE 0.70, POPULAR 0.30 |
| feed | | `feed.weak-topic-count` | 5 |
| feed | | `feed.review-after-days` | 7 |
| feed | | `feed.rank-weights` | 0.45 / 0.20 / 0.15 / 0.10 / 0.10 |
| feed | | `feed.exam-near-days` / `feed.exam-near-weight` | 60 / 0.30 |
| feed | | `feed.max-same-topic-in-row` | 2 |
| feed | | `feed.rewatch-block-days` | 14 |
| feed | | `feed.skip-defer-count` | 3 |
| feed | | `feed.demo-video-speed` | 4 (akış yanıtıyla web'e iletilir) |
| quiz | `QuizProperties` | `quiz.min-watch-ratio` | 0.70 |
| quiz | | `quiz.time-limit-sec` | 60 |
| quiz | | `quiz.token-ttl` | 5m |
| quiz | | `quiz.min-response-ms` | 2000 |
| quiz | | `quiz.rate-limit-per-min` | 30 |
| points | `PointsProperties` | `points.correct` / `points.weak-topic-correct` | 10 / 15 |
| points | | `points.streak-length` / `points.streak-bonus` | 5 / 20 |
| points | | `points.daily-cap` | 200 |
| content | `ContentProperties` | `content.tagging.min-confidence` | 0.5 |
| simulation | `SimulationProperties` | `simulation.impact.student-count` / `simulation.impact.seed` | 400 / 42 |
| simulation | | `simulation.impact.videos-per-week` / `simulation.impact.gain-per-video` | 15 / 0.01 |

Bant eşikleri yalnızca `learner` modülündedir; `feed` modülü bandı `LearnerProfileQuery` üzerinden hazır alır, eşikleri kendisi okumaz (tek kaynak, §1).
