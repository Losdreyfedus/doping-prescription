# API ve event'ler

Tüm endpoint'ler `/api` altındadır. Kimlik doğrulama `Authorization: Bearer <JWT>` ile yapılır; `userId` hiçbir zaman body'den okunmaz, token'dan alınır. `/api/admin/*` yalnızca `ADMIN` rolüne açıktır.

Request ve response DTO'ları Bean Validation anotasyonlu Java record'larıdır. OpenAPI belgesi koddan üretilir ve frontend tipleri bu belgeden üretilir (bkz. `docs/adr/0004-openapi-sozlesmesi.md`). Aşağıdaki JSON örnekleri sözleşmenin referansıdır; alan adları camelCase'tir.

Hata formatı RFC 9457 `ProblemDetail`, ek alan `code`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Soruyu açmak için videonun en az %70'ini izlemelisin.",
  "code": "VIDEO_NOT_WATCHED",
  "instance": "/api/feed/cards/7f1c.../quiz"
}
```

`detail` Türkçedir ve doğrudan kullanıcıya gösterilebilir. Doğrulama hatalarında ayrıca `errors: [{ "field": "...", "message": "..." }]` alanı bulunur.

## Endpoint listesi

| Metot | Yol | Modül | Amaç |
| --- | --- | --- | --- |
| GET | `/actuator/health` | — | postgres, redis ve kafka durumu (Spring Boot Actuator) |
| GET | `/api/auth/demo-users` | auth | Giriş ekranı için kullanıcı listesi (token gerekmez; yalnızca `prescription.auth.demo-login-enabled`, yani `demo` profili açıkken, aksi halde `404`) |
| POST | `/api/auth/demo-login` | auth | `{ "slug": "deniz" }` → `{ token, user }` (token gerekmez; koşulu yukarıdakiyle aynı) |
| GET | `/api/me` | auth | Oturumdaki kullanıcı |
| GET | `/api/exams` | exam | Denemeler ve son sonuç özeti |
| POST | `/api/exams/:examId/attempts` | exam | Denemeyi başlat, soruları al |
| GET | `/api/exam-attempts/:attemptId` | exam | Devam eden denemeyi soruları ve kayıtlı cevaplarıyla getir (çözme ekranı yenilenince) |
| PUT | `/api/exam-attempts/:attemptId/answers/:questionId` | exam | Cevap kaydet veya boşalt |
| POST | `/api/exam-attempts/:attemptId/submit` | exam | Denemeyi bitir |
| GET | `/api/exam-attempts/:attemptId/result` | exam | Sonuç |
| GET | `/api/feed` | feed | Kişiselleştirilmiş akış |
| POST | `/api/feed/interactions` | feed | İzleme, geçme, beğenme (toplu) |
| PUT | `/api/feed/saved/:contentId` | feed | Videoyu kaydet (karttaki yer imi); tekrar kaydetmek bir şey değiştirmez, `204` |
| DELETE | `/api/feed/saved/:contentId` | feed | Videoyu kaydedilenlerden çıkar; `204` |
| GET | `/api/feed/cards/:impressionId/quiz` | quiz | Video sonrası mikro-soru |
| POST | `/api/quiz/:microQuestionId/answer` | quiz | Cevap gönder |
| GET | `/api/learner/profile` | learner | Eksikler ve güçlü konular |
| PATCH | `/api/learner/settings` | learner | Kişiselleştirme aç/kapa, sınav tarihi (**MVP sonrası**, uygulanmadı; bugün seed'den gelir) |
| GET | `/api/points` | points | Toplam puan, seri, son hareketler |
| GET | `/api/admin/users` | admin | Tüm kullanıcılar ve özet durum |
| GET | `/api/admin/demo/students` | admin | Demo öğrencileri: puan ve denemesi olup olmadığı (demo kontrol ekranı) |
| GET | `/api/admin/demo/personas` | admin | Yüklenebilir persona adları |
| POST | `/api/admin/demo/reset` | admin | `{ "slug": "deniz" }` demo öğrencisini sıfırla |
| POST | `/api/admin/demo/persona` | admin | `{ "slug", "persona" }` hazır profil yükle |
| GET | `/api/admin/demo/answer-key` | admin | Mini Deneme 1'in doğru ve senaryo cevapları (sunum cevap anahtarı) |
| GET | `/api/admin/feed-debug?slug=deniz` | admin | Akışın skor dökümü |
| GET | `/api/admin/content-effectiveness` | admin | Video başına mikro-soru cevap sayısı ve doğruluk oranı |
| GET | `/api/admin/compare?left=deniz&right=selin` | admin | İki öğrencinin akış önizlemesi (kayıt bırakmaz) (**MVP sonrası**) |
| GET | `/api/admin/content?status=TAGGED` | admin | İçerik listesi (**MVP sonrası**) |
| POST | `/api/admin/content` | admin | Yeni video ekle (etiketleme otomatik çalışır) (**MVP sonrası**) |
| POST | `/api/admin/content/:contentId/tags` | admin | Önerilen etiketleri onayla veya düzelt (**MVP sonrası**) |
| POST | `/api/admin/content/:contentId/micro-questions` | admin | Mikro-soru ekle (**MVP sonrası**) |
| GET / POST / DELETE | `/api/admin/topic-keywords` | admin | Anahtar kelime sözlüğü (**MVP sonrası**) |
| GET | `/api/admin/impact` | admin | Etki paneli (simülasyon) (**MVP sonrası**) |

## Deneme

### POST /api/exams/:examId/attempts

Yanıt `201`:

```json
{
  "attemptId": "3a1f...",
  "examId": 1,
  "title": "TYT Matematik Mini Deneme 1",
  "durationMin": 20,
  "startedAt": "2026-10-19T10:00:00+03:00",
  "questions": [
    {
      "questionId": 101,
      "orderNo": 1,
      "stem": "...",
      "options": [{ "key": "A", "text": "..." }, { "key": "B", "text": "..." }]
    }
  ]
}
```

Soru nesnesinde `correctKey`, `explanation` ve `topic` **yoktur**. Aynı denemede `IN_PROGRESS` bir attempt varsa yenisi açılmaz, mevcut olan cevaplarıyla döner.

### PUT /api/exam-attempts/:attemptId/answers/:questionId

```json
{ "selectedKey": "C", "timeSpentSec": 42 }
```

`selectedKey: null` cevabı boşaltır. Yanıt `204`. Gönderilmiş attempt'e yazma `409 ATTEMPT_ALREADY_SUBMITTED`.

### POST /api/exam-attempts/:attemptId/submit

Tek transaction'da sonuçları hesaplar, `exam_attempt` ve `exam_answer` kayıtlarını günceller, `ExamResultPublished` event'ini outbox'a ekler (commit sonrası Kafka'ya `exam.result-published` topic'ine gider). Yanıt `200` → `ExamResult`:

```json
{
  "attemptId": "3a1f...",
  "correct": 8,
  "wrong": 3,
  "blank": 1,
  "net": 7.25,
  "byTopic": [
    { "topicId": 9, "name": "Olasılık", "total": 3, "correct": 0, "wrong": 3, "blank": 0 },
    { "topicId": 8, "name": "Permütasyon-Kombinasyon", "total": 2, "correct": 1, "wrong": 0, "blank": 1 }
  ],
  "questions": [
    { "questionId": 101, "orderNo": 1, "selectedKey": "C", "correctKey": "C", "result": "CORRECT", "explanation": "..." }
  ],
  "profileUpdate": { "status": "PENDING" }
}
```

`correctKey` ve `explanation` yalnızca gönderilmiş attempt'in sonucunda döner. `profileUpdate.status` `PENDING` ise web, `GET /api/learner/profile` yanıtındaki `lastExamAttemptId` bu attempt'e eşit olana kadar 500 ms aralıkla en fazla 10 kez sorgular, sonra "akışın güncellendi" bandını gösterir.

## Akış

### GET /api/feed

| Parametre | Tip | Varsayılan | Açıklama |
| --- | --- | --- | --- |
| cursor | string | — | Önceki yanıtın `nextCursor`'ı; opak, `sessionId` ve sonraki pozisyonu taşır |
| limit | int | `feed.page-size` (10) | En çok `feed.page-size-max` (20); aralık dışı `400 INVALID_REQUEST` |
| topicId | int | — | Yalnız bu konunun videoları (Gelişimim → Çalış); bkz. `docs/arayuz.md` |
| refresh | bool | false | true ise yeni oturum açılır ve kuyruk yeniden kurulur |
| saved | bool | false | Yalnız öğrencinin kaydettiği videolar, en son kaydedilen önce (Kaydedilen); `topicId`'den önceliklidir |

Yanıt `200`:

```json
{
  "sessionId": "c0a8...",
  "personalized": true,
  "algoVersion": "v1",
  "demoVideoSpeed": 4,
  "nextCursor": "eyJwIjoxMH0",
  "cards": [
    {
      "impressionId": "7f1c...",
      "position": 0,
      "content": {
        "contentId": 33,
        "title": "Olasılığın temeli: istenen bölü tüm durumlar",
        "teacherName": "Ayşe Öğretmen",
        "videoUrl": null,
        "durationSec": 58,
        "contentKind": "CONCEPT"
      },
      "topic": { "topicId": 9, "code": "TYT.MAT.OLAS", "name": "Olasılık" },
      "pool": "WEAK_TOPIC",
      "reasonText": "Son denemende Olasılık: 3 soruda 0 doğru",
      "hasMicroQuiz": true,
      "pointsAvailable": 15,
      "saved": false
    }
  ]
}
```

`personalized: false` → fallback akış (kişiselleştirme kapalı, hiç sinyal yok veya hata). Akış her zaman `200` döner. `saved` kartın videosunun öğrenci tarafından kaydedilip kaydedilmediğini söyler.

### PUT / DELETE /api/feed/saved/:contentId

Karttaki yer imi. `PUT` videoyu kaydeder, `DELETE` çıkarır; ikisi de idempotenttir ve `204` döner. Olmayan bir video için `PUT` `404 NOT_FOUND` döner. Kaydedilen videolar `GET /api/feed?saved=true` ile, en son kaydedilen önce, normal kartlar olarak (izlenim, mikro-soru ve puan dahil) gelir.

Kuyruk oturum ortasında yeniden kurulsa bile (örn. mikro-soru cevabından sonra gelen `learner.mastery-changed`) aynı `sessionId` içinde daha önce gösterilmiş bir video tekrar dönmez (bkz. `docs/algoritma.md` §4, oturum bütünlüğü).

### POST /api/feed/interactions

Web event'leri biriktirip 5 saniyede bir, sekme değişince ve sayfa kapanırken gönderir.

```json
{
  "events": [
    { "clientEventId": "a1b2", "impressionId": "7f1c...", "event": "VIEW_COMPLETE", "watchMs": 57200, "clientTs": "2026-10-19T10:05:12+03:00" }
  ]
}
```

Yanıt `202`. Tekrarlanan `clientEventId` yok sayılır.

### GET /api/feed/cards/:impressionId/quiz

Yanıt `200` → `MicroQuiz`:

```json
{
  "microQuestionId": 37,
  "impressionId": "7f1c...",
  "stem": "Bir zar atılıyor. Gelen sayının çift olduğu bilindiğine göre 4 olma olasılığı kaçtır?",
  "options": [
    { "key": "A", "text": "1/6" }, { "key": "B", "text": "1/3" },
    { "key": "C", "text": "1/2" }, { "key": "D", "text": "2/3" }
  ],
  "timeLimitSec": 60,
  "pointsAvailable": 15,
  "quizToken": "eyJhbGciOi..."
}
```

Şıklar her istekte karıştırılır; harfler yeniden atanır ve eşleme token'ın içinde taşınır. Videonun `quiz.min_watch_ratio` kadarı izlenmediyse `409 VIDEO_NOT_WATCHED`. Videoya bağlı soru yoksa `204`. Öğrenci bu soruyu daha önce cevapladıysa aynı videonun başka sorusu, o da yoksa `204`.

`quizToken`: `{ userId, impressionId, microQuestionId, optionMap, exp }` içeren, `PRESCRIPTION_QUIZ_TOKEN_SECRET` ile imzalı (oturum JWT'sinden ayrı anahtar), `jti` ve `aud: "quiz"` alanlı, `prescription.quiz.token-ttl` (varsayılan `5m`) ömürlü token. Anahtarı oturum anahtarıyla aynı olamaz (uygulama açılmaz). Cevapta `microQuestionId` pozitif, `responseMs` en çok 300000 (token ömrü) olmalıdır.

### POST /api/quiz/:microQuestionId/answer

```json
{ "quizToken": "eyJhbGciOi...", "selectedKey": "B", "responseMs": 14300 }
```

Yanıt `200` → `QuizResult`:

```json
{
  "isCorrect": true,
  "correctKey": "B",
  "explanation": "Çift sayılar 2, 4, 6; bunlardan biri 4 olduğu için 1/3.",
  "pointsAwarded": 15,
  "totalPoints": 135,
  "streak": { "current": 1, "bonusAt": 5 },
  "masteryChange": { "topicId": 9, "name": "Olasılık", "before": 0.172, "after": 0.271 }
}
```

Kullanıcı sonucu aynı yanıtta görmeli, bu yüzden bu akış senkrondur (`docs/mimari.md` §4). Quiz modülü tek bir transaction içinde: cevabı `micro_quiz_attempt`'e yazar, `learner.api.LearnerSignalRecorder` ile sinyali kaydedip önceki/sonraki ustalık değerini alır, `points.api.PointsLedger`'a sonucu (doğru/yanlış, havuz, `refId`) iletip kazanılan puanı ve güncel seriyi alır ve `QuizAnswered` event'ini outbox'a ekler. Unique kısıt ihlali (aynı soruya ikinci cevap veya eşzamanlı iki istek) yakalanır ve `pointsAwarded: 0` sonucuna çevrilir.

## Öğrenci profili

### GET /api/learner/profile

```json
{
  "userId": "…",
  "personalizationEnabled": true,
  "lastExamAttemptId": "3a1f...",
  "updatedAt": "2026-10-19T10:04:01+03:00",
  "topics": [
    {
      "topicId": 9,
      "code": "TYT.MAT.OLAS",
      "name": "Olasılık",
      "mastery": 0.271,
      "confidence": 0.551,
      "band": "WEAK",
      "trend": "UP",
      "lastSignalAt": "2026-10-19T10:06:12+03:00",
      "readyContentCount": 6
    }
  ]
}
```

`topics` mastery'ye göre artan sıralıdır; hiç sinyali olmayan konular `band: "UNKNOWN"` ile en sonda gelir. `band` tek kaynaktan, domain'deki `MasteryBand`'den gelir (bkz. `docs/algoritma.md` §1): decay uygulanmış mastery < 0,6 `WEAK`, < 0,8 `MEDIUM`, diğerleri `STRONG`. `WEAK` konular akışta `WEAK_TOPIC` havuzunu besleyen konulardır. `mastery` ve `confidence` yanıtta 3 ondalığa yuvarlanır (HALF_UP). `trend`: konunun son ustalık sinyalinin yönü (`UP`, `DOWN`, `FLAT`; bkz. `docs/algoritma.md` §1).

## Puan

### GET /api/points

```json
{ "total": 135, "today": 15, "dailyCap": 200, "streak": { "current": 1, "best": 4 }, "recent": [ { "amount": 15, "reason": "QUIZ_CORRECT_WEAK", "createdAt": "…" } ] }
```

## Admin

### POST /api/admin/demo/reset

`{ "slug": "deniz" }`: öğrencinin attempt, cevap, sinyal, mastery, impression, etkileşim, quiz ve puan kayıtlarını siler, Redis anahtarlarını temizler. `deniz` sıfırlanırken `selin` de `SCRIPTED_EXAM` personasıyla yeniden hazırlanır (bkz. `docs/demo-senaryosu.md`). Yanıt `204`.

### POST /api/admin/demo/persona

`persona`: `NEW` (her şey boş), `WEAK_PROBLEMS` (Mert'in profili), `SCRIPTED_EXAM` (demo senaryosundaki deneme sonucu), `SCRIPTED_IN_PROGRESS` (Mini Deneme 1 başlatılmış, 12 cevap senaryo cevaplarıyla işaretli, gönderilmemiş; sunumda Deniz için). Persona'lar gerçek API akışıyla uygulanır: attempt oluşturulur, cevaplar yazılır, gönderilir (`SCRIPTED_IN_PROGRESS`'te gönderme adımı yapılmaz); böylece event zinciri de test edilmiş olur. Senaryo cevapları `exam_question.scenario_answer` kolonundan okunur.

### GET /api/admin/content-effectiveness

Video başına mikro-soru cevap sayısı, doğru sayısı ve doğruluk oranı (`correctRate`, 0–1); doğruluk oranına göre azalan, eşitlikte cevap sayısı fazla olan önce. Yalnızca en az bir cevabı olan videolar. Kaynak `micro_quiz_attempt` (quiz modülü, `QuizStatistics`) ve soru → video eşlemesi (content modülü, `MicroQuestionCatalog`).

### GET /api/admin/feed-debug?slug=deniz

Öğrencinin en zayıf 5 konusu, son akış oturumundaki her kartın `pool`, `score`, `score_breakdown` (`weakness`, `examImportance`, `quality`, `freshness`, `difficultyFit`, `diversityPenalty`) ve uygulanan kurallar (örn. "aynı konu 3. kez → ertelendi").

### GET /api/admin/impact

Simülasyon sonuçları (bkz. `docs/algoritma.md` §8). Yanıtta `"isSimulation": true` alanı zorunludur.

## Hata kodları

| HTTP | Kod | Ne zaman |
| --- | --- | --- |
| 400 | INVALID_REQUEST | Şema doğrulaması başarısız, eksik parametre veya okunamayan gövde |
| 401 | UNAUTHORIZED | Token yok veya geçersiz |
| 403 | FORBIDDEN | Rol yetersiz |
| 403 | QUIZ_TOKEN_INVALID | Token başka kullanıcıya ait, imza bozuk ya da soru id'si uyuşmuyor |
| 404 | NOT_FOUND | Kayıt veya endpoint yok, ya da kayıt kullanıcıya ait değil |
| 405 | METHOD_NOT_ALLOWED | Endpoint bu HTTP metodunu desteklemiyor (`Allow` başlığıyla) |
| 415 | UNSUPPORTED_MEDIA_TYPE | Gövde JSON değil |
| 409 | ATTEMPT_ALREADY_SUBMITTED | Gönderilmiş denemeye yazma |
| 409 | VIDEO_NOT_WATCHED | Video yeterince izlenmeden soru istendi |
| 410 | QUIZ_EXPIRED | Token süresi doldu |
| 429 | RATE_LIMITED | Dakikada `quiz.rate_limit_per_min`'den fazla cevap |

Spring MVC'nin kendi hataları da (eksik parametre, yanlış metot, okunamayan gövde) aynı `ProblemDetail` biçiminde ve `code` alanıyla döner. Admin isteklerinde `slug` en çok 32, `persona` en çok 64 karakterdir.

Aynı soruya ikinci cevap hata değildir: `200` döner, `pointsAwarded: 0`.

## Event'ler

Modüller arası event'ler Kafka üzerinden, transactional outbox ile yayınlanır (bkz. `docs/mimari.md` §3, `docs/adr/0002-kafka-ve-outbox.md`). Tüm event'ler JSON'dur ve `eventId`, `schemaVersion`, `occurredAt`, `userId` alanlarını taşır. Mesaj anahtarı `userId`'dir. Tüketiciler `processed_event` tablosuyla idempotenttir; 3 başarısız denemeden sonra mesaj `<topic>-dlt` topic'ine gider (Spring Kafka 4 adlandırması).

| Topic | Java event'i | Yayınlayan | Dinleyen | Partition |
| --- | --- | --- | --- | --- |
| `exam.result-published` | `ExamResultPublished` | exam | learner | 3 |
| `learner.mastery-changed` | `MasteryChanged` | learner | feed (kuyruğu geçersiz kıl) | 3 |
| `feed.interaction-recorded` | `InteractionRecorded` | feed | learner (yalnız `VIEW_COMPLETE` ve `SKIP` → `last_served_at`; feed zaten learner API'sine bağlı olduğu için learner bu event'i kendi yerel kaydıyla okur, modül döngüsü oluşmaz) | 3 |
| `quiz.answered` | `QuizAnswered` | quiz | — (analitik için yayınlanır; learner dinlemez, sinyal senkron yazılır) | 3 |

`ContentCreated` yalnızca `content` modülü içinde kullanılır (otomatik etiketleme) ve Kafka'ya gitmez.

Topic'ler uygulama başlangıcında `NewTopic` bean'leriyle oluşturulur; demo için replikasyon 1.

### exam.result-published

```json
{
  "eventId": "b7d1...",
  "schemaVersion": 1,
  "occurredAt": "2026-10-19T10:04:00+03:00",
  "userId": "…",
  "attemptId": "3a1f...",
  "examId": 1,
  "answers": [
    { "questionId": 101, "topicId": 9, "result": "WRONG", "timeSpentSec": 95 }
  ]
}
```

### feed.interaction-recorded

```json
{
  "eventId": "c41b...",
  "schemaVersion": 1,
  "occurredAt": "2026-10-19T10:05:12+03:00",
  "userId": "…",
  "impressionId": "7f1c...",
  "contentId": 33,
  "topicId": 9,
  "interaction": "VIEW_COMPLETE",
  "watchMs": 57200
}
```

### quiz.answered

```json
{
  "eventId": "e912...",
  "schemaVersion": 1,
  "occurredAt": "2026-10-19T10:06:12+03:00",
  "userId": "…",
  "quizAttemptId": "…",
  "impressionId": "7f1c...",
  "microQuestionId": 37,
  "topicId": 9,
  "isCorrect": true,
  "responseMs": 14300
}
```

### learner.mastery-changed

```json
{
  "eventId": "f03a...",
  "schemaVersion": 1,
  "occurredAt": "2026-10-19T10:06:12+03:00",
  "userId": "…",
  "trigger": "QUIZ_CORRECT",
  "changes": [ { "topicId": 9, "before": 0.172, "after": 0.271, "confidence": 0.551 } ]
}
```

Yalnızca en az bir konuda değişim `prescription.learner.mastery.change-event-threshold` değerini geçerse yayınlanır. Feed modülü bu event'te kullanıcının Redis akış kuyruğunu siler; bir sonraki `GET /api/feed` kuyruğu yeniden kurar.
