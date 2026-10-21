# ADR 0005: Modül sınırlarının netleştirilmesi

- **Durum:** Kabul edildi
- **Tarih:** 2026-10-20

## Bağlam

ADR 0001 modülleri ve "yalnızca `api` paketi" kuralını tanımladı. Uygulamaya başlamadan önce yapılan doküman incelemesinde bu kuralla çelişen üç nokta bulundu:

1. Veri modelinde modül sınırını aşan foreign key'ler vardı (`exam_attempt.user_id → users`, `exam_question.topic_id → topic`, `content_topic_map.approved_by → users`, `micro_quiz_attempt.micro_question_id → micro_question`, `micro_quiz_attempt.impression_id → feed_impression`).
2. Seri (streak) `points` modülüne aitti ama `quiz` modülünün `micro_quiz_attempt` tablosundan hesaplanacağı yazıyordu. `quiz` zaten `points`'i çağırdığı için bu, döngüsel bir bağımlılık ya da başka modülün tablosunu okumak demekti.
3. `shared` modülündeki hata modeli (`shared.error`) ve altyapı tipleri `api` paketi dışında, ama tüm modüller tarafından kullanılacak. Kural olduğu gibi uygulansaydı `ApplicationModules.verify()` her modülde ihlal raporlardı.

## Karar

1. **Modüller arası foreign key yok.** Başka modüle ait id'ler FK'siz ve indeksli kolonlardır; bütünlük uygulama katmanında, ilgili modülün `api` arayüzüyle sağlanır. FK'ler yalnızca aynı modülün tabloları arasında tanımlanır.
2. **Seri `points` modülünün kendi verisidir** (`points_streak` tablosu). `quiz`, her cevapta `PointsLedger`'a sonucu (doğru/yanlış, havuz, `refId`) iletir; yanıt olarak kazanılan puanı ve güncel seriyi alır. Bağımlılık tek yönlüdür: `quiz → points`.
3. **`shared` açık (OPEN) bir modüldür** (`@ApplicationModule(type = OPEN)`). İçeriği beş alanla sınırlıdır: hata modeli, event zarfı (`DomainEvent`), idempotency (`processed_event`), saat ve mesajlaşma altyapısı (Kafka yapılandırması ve health, `shared.messaging`). İş mantığı `shared`'a girmez; bu kural kod incelemesinde ve bir ArchUnit kuralıyla (`shared` başka bir modüle bağımlı olamaz) korunur.

## Değerlendirilen seçenekler

- **Modüller arası FK'leri korumak:** Veritabanı bütünlüğü kazanılır, ama modüller şema düzeyinde birbirine bağlanır. Bir modülün ileride ayrı servise çıkarılması (ADR 0001) şema ayrıştırması gerektirir, `admin` sıfırlamasında silme sırası modüller arasında koordinasyon ister.
- **Seriyi `quiz` içinde tutmak:** Bağımlılık sorunu çözülür, ama puanla ilgili kural (seri bonusu) iki modüle bölünür; `GET /api/points` seriyi göstermek için `quiz`'e bağımlı olur.
- **`shared` tiplerini `shared.api` altına taşımak:** OPEN gerektirmez, ama hata modeli ve saat gibi altyapı tiplerini "modül API'si" gibi gösterir ve paket adlarını yapay biçimde uzatır.

## Sonuçlar

- Veritabanı, modüller arası yetim kayıtları engellemez. Riski düşük, çünkü demo dışında silme yok ve "Sıfırla" her modülde `UserDataEraser` ile yapılıyor.
- `points` modülü seri için iyimser kilitleme (`version`) ve `refId` idempotency'si uygular; eşzamanlı iki cevap seriyi iki kez artırmaz.
- `shared`'ın OPEN olması bir istisnadır; büyümemesi incelemede takip edilir.
