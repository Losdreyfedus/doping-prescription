# Veri modeli

Tüm sistem tek bir ortak dile dayanır: **konu** (`topic`). Deneme sorusu, video ve mikro-soru aynı `topic_id`'ye bağlandığı için veriler birbirini görür.

Genel kurallar:

- Şema Flyway migration'larıyla oluşturulur (`doping-prescription-core/src/main/resources/db/migration`), her modülün tabloları ayrı migration dosyalarında. JPA `ddl-auto=validate`.
- Tüm tablolarda `created_at timestamptz not null default now()`; değişen tablolarda ayrıca `updated_at`.
- Enum'lar veritabanında `varchar` + `CHECK` kısıtı, Java'da `enum` (`EnumType.STRING`).
- Aşağıdaki tablolarda `Decimal(p,s)` PostgreSQL `numeric(p,s)` demektir; Java tarafında `BigDecimal`. Ustalık skorları `numeric(4,3)` (0,000–1,000). Domain hesapları `double` ile yapılır ve kalıcılaştırılırken 3 ondalığa yuvarlanır (`RoundingMode.HALF_UP`).
- `uuid` id'ler uygulamada üretilir (`UUID.randomUUID()`), `serial`/`bigserial` id'ler veritabanında.
- Foreign key'ler yalnızca aynı modülün tabloları arasında tanımlanır. Başka modüle ait id'ler (örn. `learner_signal.topic_id`) foreign key'siz, indeksli kolonlardır; modüller arası bütünlük uygulama katmanında sağlanır.
- Her `user_id`, `topic_id`, `content_id` kolonu ve sık sorgulanan bileşik kolonlar indekslidir.
- `learner_topic_mastery` iyimser kilitleme için `version bigint not null default 0` kolonu taşır.
- Silme işlemi yok; içerik `status` ile emekliye ayrılır. İstisna: admin "Sıfırla" işlemi demo öğrencisinin verilerini fiziksel olarak siler.
- Üretimde `learner_signal` ve `feed_interaction` partition'lanır; demoda gerekmez, ama migration'da bir yorumla belirt.
- Spring Modulith'in outbox tablosu `event_publication` kendi şemasıyla, ayrı bir migration'da oluşturulur (aşağıdaki listeye dahil değildir).

17 tablo, sahibi olan modüle göre gruplanmıştır. `processed_event` tablosu `shared` modülüne aittir.

## auth

### `users`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | uuid PK | |
| slug | varchar(32) unique | Demo girişi için: `deniz`, `mert`, `selin`, `admin` |
| display_name | varchar(100) | |
| role | enum `STUDENT`, `ADMIN` | |
| target_exam | enum `TYT` | Demoda tek değer; enum ileride genişler |
| exam_date | date null | Sınav yakınlığı ağırlığı için |
| personalization_enabled | bool default true | false ise akış fallback modunda çalışır |

## content

### `topic`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | serial PK | |
| code | varchar(32) unique | Örn. `TYT.MAT.OLAS` |
| name | varchar(100) | "Olasılık" |
| exam_type | enum `TYT` | |
| subject | varchar(50) | "Matematik" |
| exam_weight | Decimal(4,3) | Konunun sınavdaki önemi, 0–1 (seed'de verilir) |
| sort_order | int | |

### `topic_keyword`

Kural tabanlı etiketleme sözlüğü.

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | serial PK | |
| topic_id | int FK → topic | |
| keyword | varchar(100) | Normalize edilmiş ifade (bkz. `docs/algoritma.md` §5) |
| weight | Decimal(3,2) | Eşleşme skoruna katkı, 0–1 |
| is_negative | bool default false | true ise eşleşme bu konuyu dışlar |
| | unique | (topic_id, keyword) |

### `content_item`

Dopingle'daki her kısa video.

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | serial PK | |
| title | varchar(150) | Kartta görünen başlık |
| description | text | Etiketleme için kullanılır |
| teacher_name | varchar(100) | Kurgusal öğretmen adı |
| video_url | varchar(300) null | Varsa gerçek dosya; yoksa simüle oynatıcı |
| duration_sec | int | 30–90 |
| difficulty | smallint | 1–5 |
| content_kind | enum `CONCEPT`, `SOLVED_EXAMPLE`, `TIP` | |
| quality_score | Decimal(4,3) | Seed'de verilir; ileride tamamlanma oranından hesaplanır |
| status | enum `DRAFT`, `TAGGED`, `LIVE`, `RETIRED` | Akışa yalnız `LIVE` girer |
| published_at | timestamptz | Tazelik hesabı için |

### `content_topic_map`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| content_id | int FK | PK (content_id, topic_id) |
| topic_id | int FK | |
| weight | Decimal(3,2) | Videonun bu konuyu kapsama oranı |
| tag_source | enum `MANUAL`, `KEYWORD_RULE` | |
| confidence | Decimal(3,2) | Anahtar kelime eşleşme skoru; `MANUAL` için 1 |
| approved_by | uuid null FK → users | `KEYWORD_RULE` etiketi onaylanmadan video `LIVE` olmaz |
| approved_at | timestamptz null | |

### `micro_question`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | serial PK | |
| content_id | int FK | Bağlı olduğu video; video başına en az 1 |
| topic_id | int FK | |
| stem | text | Soru metni |
| options | jsonb | `[{"key":"A","text":"..."}, ...]`, 4 şık |
| correct_key | char(1) | Öğrenciye asla dönmez |
| explanation | text | Cevaptan sonra gösterilen 1–2 cümlelik çözüm |
| difficulty | smallint | 1–5 |
| status | enum `DRAFT`, `LIVE`, `RETIRED` | |

## exam

### `exam`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | serial PK | |
| title | varchar(150) | "TYT Matematik Mini Deneme 1" |
| exam_type | enum `TYT` | |
| duration_min | int | |
| is_active | bool | |

### `exam_question`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | serial PK | |
| exam_id | int FK | |
| order_no | smallint | 1'den başlar |
| topic_id | int FK | |
| stem | text | |
| options | jsonb | 5 şık (A–E), TYT formatı |
| correct_key | char(1) | Öğrenciye sonuç ekranına kadar dönmez |
| explanation | text | Sonuç ekranında gösterilir |
| difficulty | smallint | |
| | unique | (exam_id, order_no) |

### `exam_attempt`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | uuid PK | |
| user_id | uuid FK | |
| exam_id | int FK | |
| status | enum `IN_PROGRESS`, `SUBMITTED` | |
| started_at | timestamptz | |
| submitted_at | timestamptz null | |
| correct_count / wrong_count / blank_count | smallint | Gönderimde hesaplanır |
| net | Decimal(5,2) | doğru − yanlış / 4 |

Bir öğrencinin aynı denemede aynı anda tek `IN_PROGRESS` denemesi olabilir. Demo öğrencisi aynı denemeyi tekrar çözebilir (her seferinde yeni attempt).

### `exam_answer`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| attempt_id | uuid FK | PK (attempt_id, question_id) |
| question_id | int FK | |
| selected_key | char(1) null | null = boş |
| result | enum `CORRECT`, `WRONG`, `BLANK` null | Gönderimde hesaplanır |
| time_spent_sec | int null | |

## learner

### `learner_signal`

Append-only sinyal günlüğü. Ustalık skoru her zaman bu tablodan yeniden hesaplanabilir.

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | bigserial PK | |
| user_id | uuid | |
| topic_id | int | |
| signal_type | enum `EXAM_CORRECT`, `EXAM_WRONG`, `EXAM_BLANK`, `QUIZ_CORRECT`, `QUIZ_WRONG`, `VIDEO_COMPLETED`, `VIDEO_SKIPPED` | |
| source_ref | varchar(64) | attempt id, quiz attempt id veya impression id |
| occurred_at | timestamptz | |
| idempotency_key | varchar(100) unique | Örn. `exam:{attemptId}:{questionId}` |

### `learner_topic_mastery`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| user_id | uuid | PK (user_id, topic_id) |
| topic_id | int | |
| mastery | Decimal(4,3) | 0 = bilmiyor, 1 = hakim; ilk değer 0,5 |
| confidence | Decimal(4,3) | Kaç sinyale dayandığı |
| attempts | int | |
| correct | int | |
| last_signal_at | timestamptz | Decay için |
| last_served_at | timestamptz null | Bu konudan en son ne zaman video gösterildi |
| last_exam_attempt_id | uuid null | "Neden bu video" şablonu için |
| last_exam_total / last_exam_correct | smallint null | Son denemede bu konudaki soru ve doğru sayısı |

Sıcak kopya Redis'te `mastery:{userId}` hash'inde tutulur; her güncellemede ikisi birlikte yazılır.

## shared

### `processed_event`

Kafka tüketicilerinin idempotency'si için.

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| consumer | varchar(64) | PK (consumer, event_id) |
| event_id | uuid | |
| processed_at | timestamptz | |

## feed ve quiz

### `feed_impression`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | uuid PK | Client bu id ile etkileşim gönderir |
| user_id | uuid | |
| session_id | uuid | |
| content_id | int | |
| position | smallint | |
| pool | enum `WEAK_TOPIC`, `REVIEW_DUE`, `EXPLORE`, `POPULAR`, `FALLBACK` | Kartın geldiği havuz |
| target_topic_id | int null | |
| score | Decimal(6,4) | |
| score_breakdown | jsonb | Debug ekranı için bileşenler |
| reason_text | varchar(200) | Kartta görünen açıklama |
| algo_version | varchar(16) | |
| served_at | timestamptz | |

### `feed_interaction`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | bigserial PK | |
| impression_id | uuid FK | |
| user_id | uuid | |
| client_event_id | varchar(64) unique | |
| event | enum `VIEW_START`, `VIEW_COMPLETE`, `SKIP`, `LIKE`, `QUIZ_SHOWN`, `QUIZ_DISMISSED` | |
| watch_ms | int null | |
| client_ts | timestamptz | |

### `micro_quiz_attempt`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | uuid PK | |
| user_id | uuid | |
| micro_question_id | int FK | |
| impression_id | uuid FK | |
| selected_key | char(1) | |
| is_correct | bool | Sunucuda hesaplanır |
| response_ms | int | |
| points_awarded | int | |
| | unique | (user_id, micro_question_id) — ikinci cevap kaydedilmez, 0 puan döner |

## points

### `points_ledger`

| Kolon | Tip | Açıklama |
| --- | --- | --- |
| id | bigserial PK | |
| user_id | uuid | |
| amount | int | |
| reason | enum `QUIZ_CORRECT`, `QUIZ_CORRECT_WEAK`, `STREAK_BONUS` | |
| ref_id | varchar(64) unique | Örn. `quiz:{quizAttemptId}`, `streak:{quizAttemptId}` |

Toplam puan = `sum(amount)`; seri bilgisi `micro_quiz_attempt` üzerinden hesaplanır.
