# Mimari şeması (koddan çıkarıldı)

Kaynak: çalışma ağacının 2026-10-26 hali (`main` f69b675 + commit'lenmemiş değişiklikler). Modül bağımlılıkları `*/package-info.java` içindeki `allowedDependencies`'ten, topic'ler `@Externalized` / `@KafkaListener` tanımlarından, endpoint'ler controller'lardan alındı.

PNG'ler aynı klasörde: `mimari-genel.png`, `mimari-genel-ayrintili.png`, `demo-akisi.png`, `frontend.png`, `frontend-ayrintili.png` (`.mmd` kaynaklarıyla). Yeniden üretmek için:

```bash
npx -y @mermaid-js/mermaid-cli -i docs/review/<ad>.mmd -o docs/review/<ad>.png -w 1920 -b white
```

## a) Genel mimari (sunum sürümü)

```mermaid
flowchart LR
    UI["React<br/>uygulaması"]
    EXAM["exam<br/>deneme"]
    LEARNER["learner<br/>ustalık skoru"]
    FEED["feed<br/>kişisel akış"]
    QUIZ["quiz<br/>mikro-soru"]
    POINTS["points<br/>puan"]
    STORE[("PostgreSQL: veri + outbox<br/>Redis: önbellek")]

    UI -->|"Sınavı Bitir"| EXAM
    EXAM ==>|"Kafka<br/>sonuç yayınlandı"| LEARNER
    LEARNER ==>|"Kafka<br/>ustalık değişti"| FEED
    FEED -->|"reçete"| UI
    UI -->|"cevap"| QUIZ
    QUIZ -->|"anında"| LEARNER
    QUIZ -->|"anında"| POINTS
    LEARNER -.- STORE
```

Okuma: ağır işler (deneme sonucu → profil → akış) Kafka üzerinden arka planda akar; öğrencinin anında görmesi gereken tek şey (mikro-soru sonucu) doğrudan çağrıyla. `auth`, `content`, `admin`, `shared` sunum sürümünde gizlendi.

## a') Genel mimari (ayrıntılı, yedek slayt)

```mermaid
flowchart LR
    UI["React 19 + Vite"]
    AUTH["auth<br/>JWT"]
    CONTENT["content<br/>video, konu"]
    EXAM["exam"]
    LEARNER["learner"]
    FEED["feed"]
    QUIZ["quiz"]
    POINTS["points"]
    ADMIN["admin<br/>demo, feed-debug,<br/>video etkinliği"]
    T1(["exam.result-published"])
    T2(["learner.mastery-changed"])
    T3(["feed.interaction-recorded"])
    T4(["quiz.answered<br/>(tüketicisi yok)"])
    PG[("PostgreSQL 16<br/>outbox = event_publication")]
    REDIS[("Redis 7<br/>profil, kuyruk, token")]

    UI -->|"REST + JWT"| AUTH
    EXAM -. outbox .-> T1 --> LEARNER
    LEARNER -. outbox .-> T2 --> FEED
    FEED -. outbox .-> T3 --> LEARNER
    QUIZ -. outbox .-> T4
    QUIZ -->|senkron| LEARNER
    QUIZ -->|senkron| POINTS
    QUIZ -->|senkron| FEED
    FEED -->|senkron profil| LEARNER
    FEED -->|kart puanı| POINTS
    EXAM --> CONTENT
    FEED --> CONTENT
    ADMIN -->|"yalnız api"| FEED
    LEARNER --- PG
    FEED --- REDIS
```

Modül bağımlılıkları (`allowedDependencies`, hepsi yalnız `api` paketine):

| Modül | İzinli bağımlılıklar |
| --- | --- |
| auth, content | shared |
| exam | auth, content, shared |
| learner | auth, content, exam (event tipi), shared |
| points | auth, shared |
| feed | auth, content, learner, points, shared |
| quiz | auth, content, feed, learner, points, shared |
| admin | auth, content, exam, feed, learner, points, quiz, shared |
| simulation | yok (boş, MVP sonrası) |

Not: `learner`, `feed.interaction-recorded`'ı feed'in Java tipini import etmeden kendi kaydıyla okur (`learner/messaging/InteractionListener.java`), böylece feed → learner bağımlılığıyla döngü oluşmaz.

## b) Demo akışı (sequence diagram)

```mermaid
sequenceDiagram
    autonumber
    actor Ö as Öğrenci
    participant UI as React
    participant EX as exam
    participant DB as PostgreSQL<br/>(outbox)
    participant K as Kafka
    participant LE as learner
    participant FE as feed
    participant QZ as quiz
    participant PT as points

    Ö->>UI: "Sınavı Bitir"
    UI->>EX: POST /api/exam-attempts/{id}/submit
    EX->>DB: notlandır + ExamResultPublished (aynı transaction)
    EX-->>UI: 8 D · 3 Y · 1 B · 7,25 net
    DB-->>K: outbox → exam.result-published
    K->>LE: tüketir (eventId ile idempotent)
    LE->>LE: ustalık hesabı (Olasılık %17)
    LE->>DB: skor + MasteryChanged (aynı transaction)
    DB-->>K: learner.mastery-changed
    K->>FE: tüketir → Redis kuyruğunu sil
    UI->>LE: GET /api/learner/profile (bant: "akışın güncellendi")
    Ö->>UI: "Reçeteni gör"
    UI->>FE: GET /api/feed
    FE->>LE: profili oku (Redis önbellekli)
    FE-->>UI: kartlar + "neden bu video"
    Note over UI: video izlenir (≥ %70)
    UI->>QZ: GET /api/feed/cards/{id}/quiz
    QZ-->>UI: soru + imzalı token (cevap yok)
    Ö->>UI: cevap
    UI->>QZ: POST /api/quiz/{id}/answer
    rect rgb(235, 243, 255)
        Note over QZ,PT: tek transaction, senkron
        QZ->>LE: sinyal yaz → %17 → %27
        QZ->>PT: +15 puan (refId ile tekil)
    end
    QZ-->>UI: Doğru · +15 · %17 → %27
    Ö->>UI: Gelişimim
    UI->>LE: GET /api/learner/profile
```

## c) Frontend yapısı (sunum sürümü)

```mermaid
flowchart TB
    OAS["openapi.json (backend sözleşmesi)"] -->|"npm run gen:api"| SCHEMA["Üretilen TypeScript tipleri"]
    SCHEMA --> CLIENT["API istemcisi<br/>openapi-fetch + TanStack Query"]
    CLIENT --> FEAT["features/<br/>home · exams · dopingle<br/>learner · admin · auth"]
    SHARED["shared/ui · i18n"] --> FEAT
    FEAT --> APPL["app/: rotalar · menü · oturum"]
    CLIENT <-->|"REST + JWT"| API["Spring Boot API"]
```

## c') Frontend yapısı (ayrıntılı)

```mermaid
flowchart TB
    OAS["openapi.json<br/>(backend springdoc'tan,<br/>OpenApiContractIT sapmayı yakalar)"]
    GEN["npm run gen:api<br/>openapi-typescript"]
    SCHEMA["shared/api/generated/schema.ts"]
    CLIENT["shared/api/client.ts<br/>openapi-fetch + JWT + ProblemDetail"]

    subgraph APPL["app/"]
        ROUTES["routes.tsx · App.tsx"]
        LAYOUT["layout/ (menü, üst çubuk)"]
        SESSION["session/"]
    end

    subgraph FEAT["features/ (birbirini import edemez)"]
        HOME["home<br/>Ana Sayfa"]
        EXAMS["exams<br/>liste · çözme · sonuç"]
        DOP["dopingle<br/>akış · oynatıcı · mikro-soru"]
        LEARN["learner<br/>Gelişimim"]
        ADM["admin<br/>demo kontrolü · video etkinliği"]
        AUTHF["auth<br/>giriş"]
    end

    SHAREDUI["shared/ui · i18n · session · lib"]

    OAS --> GEN --> SCHEMA --> CLIENT
    ROUTES --> FEAT
    LAYOUT --> SHAREDUI
    FEAT --> CLIENT
    FEAT --> SHAREDUI
    CLIENT -->|"TanStack Query"| API["Spring Boot API"]
```
