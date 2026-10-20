# ADR 0003: Açıklanabilir, kural tabanlı öneri algoritması

- **Durum:** Kabul edildi
- **Tarih:** 2026-10-20

## Bağlam

Akışın öğrenciye "neden bu video" açıklaması göstermesi, admin debug ekranının her kararı sayılarla açıklaması ve sonuçların test edilebilir olması gerekiyor. Gerçek kullanım verisi yok; bir modeli eğitecek veri de yok. Proje kapsamı çalışma zamanında LLM kullanımını yasaklıyor.

## Karar

- Ustalık skoru: Elo benzeri artımlı güncelleme ve zamanla unutma (decay).
- Akış: dört aday havuzu, kotalar, ağırlıklı doğrusal skor ve kurallar.
- Etiketleme: anahtar kelime eşleşmesi ve editör onayı.
- "Neden bu video" metinleri: sabit şablonlar.
- Tüm algoritmalar `domain` paketlerinde saf Java ve deterministiktir; tüm katsayılar config'dedir.

## Sonuçlar

- Her karar debug ekranında bileşenleriyle gösterilebilir ve birim testiyle birebir doğrulanabilir.
- Katsayılar sezgiseldir; gerçek veriyle A/B testleri üzerinden ayarlanmalıdır.
- İleride öğrenen bir sıralama modeli, `FeedRanker` arayüzünün yeni bir uygulaması olarak eklenebilir; API ve tablolar değişmez.
