# ADR 0002: Kafka ve transactional outbox

- **Durum:** Kabul edildi
- **Tarih:** 2026-10-20

## Bağlam

Deneme sonucu, öğrenci profilini ve akışı güncellemeli; deneme modülü bu tüketicilerden habersiz olmalı. Gerçek Doping'de deneme, video ve puan sistemleri ayrı ekiplerin elinde; bu projedeki event sözleşmeleri ileride o sistemlerle entegrasyonun temeli olacak. Bir event'in kaybolması (veritabanına yazılıp broker'a gitmemesi) öğrencinin akışının hiç güncellenmemesi demek.

## Karar

- Modüller arası domain event'leri için **Apache Kafka** (KRaft, tek node).
- Yayınlamada **transactional outbox**: Spring Modulith event publication registry, event'i iş transaction'ında `event_publication` tablosuna yazar ve commit sonrası Kafka'ya dışsallaştırır.
- Mesaj anahtarı `userId`: bir öğrencinin event'leri aynı partition'da, sıralı işlenir.
- Tüketicilerde idempotency `processed_event` tablosuyla; hatada 3 yeniden deneme ve `.DLT` topic'i.

## Değerlendirilen seçenekler

- **Yalnızca uygulama içi event'ler:** En basit; ama "ileride ayrı sistemler" senaryosunu göstermez ve uygulama çökerse event kaybolur.
- **RabbitMQ:** Kuyruk semantiği bu iş için yeterli; ancak öğrenci profili sinyal geçmişinden yeniden hesaplanabilir olmalı ve Kafka'nın saklanan, yeniden oynatılabilir log'u bu ihtiyaca daha uygun. Partition anahtarıyla kullanıcı bazında sıralama da doğrudan geliyor.

## Sonuçlar

- Yerel ortamda Kafka konteyneri gerekiyor (Docker Compose).
- Deneme gönderimi ile akışın güncellenmesi arasında birkaç yüz milisaniyelik gecikme oluşur; arayüz bunu "Eksiklerin analiz ediliyor…" durumuyla gösterir.
- Entegrasyon testleri Testcontainers Kafka ile çalışır.
