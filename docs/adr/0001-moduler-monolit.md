# ADR 0001: Modüler monolit

- **Durum:** Kabul edildi
- **Tarih:** 2026-10-20

## Bağlam

Sistemin dokuz iş alanı var (içerik, deneme, profil, akış, mikro-soru, puan, admin, simülasyon, kimlik). Proje bir sunum demosu; tek kişi ve kısa süreyle geliştiriliyor, ama kodu deneyimli geliştiriciler inceleyecek ve ileride gerçek Doping sistemine taşınması mümkün olmalı.

## Karar

Tek bir Spring Boot uygulaması, Spring Modulith ile zorlanan modül sınırları. Her iş alanı bir modül; modüller yalnızca birbirlerinin `api` paketlerini ve event'lerini kullanır.

## Değerlendirilen seçenekler

- **Mikroservisler:** Bağımsız dağıtım avantajı bu ölçekte kullanılmıyor; ağ, dağıtık transaction ve operasyon maliyeti demo süresini tüketir.
- **Katmanlı tek uygulama (controller/service/repository):** Hızlı ama iş alanları arası bağımlılıklar kontrolsüz büyür; incelemede en çok eleştirilecek yapı.

## Sonuçlar

- Modül sınırları test ile doğrulanır; ihlal build'i kırar.
- Bir modül ileride ayrı servise çıkarılmak istenirse, Kafka event'leri ve `api` arayüzleri hazır sözleşme olur.
- Tek veritabanı kullanılır, ama her modül yalnızca kendi tablolarına yazar.
