# Backlog (MVP sonrası)

Hedef, 2. aşamadan itibaren demo senaryosunun ana yolunu (adım 1–5, Deniz) en kısa sürede uçtan uca çalıştırmaktır (AGENTS.md §7). Aşağıdaki işler bu yüzden ertelendi. Hepsi `docs/mimari.md` §10'daki genişleme noktaları veya mevcut tablolar üzerinden, var olan kodu değiştirmeden eklenebilecek şekilde bırakılır. 1. aşamada yapılanlar (etiketleme, Mini Deneme 2, 48 mikro-soru, Mert ve Selin kullanıcıları) kodda durur.

| İş | Neden ertelendi | Sonra nasıl eklenir |
| --- | --- | --- |
| **Etki paneli** (eski 8. aşama: simülasyon, A/B grafikleri) | Demo ana yolunda yok; "ölçüm nasıl yapılır" sorusu sözlü cevaplanabilir. En pahalı ekranlardan biri. | `simulation` modülü boş durur; `FeedRanker` ve `CandidatePool` arayüzleri simülasyonun aynı algoritmayı çağırmasına izin verir. |
| **Admin etiket onay ekranı ve anahtar kelime API'si** | Otomatik etiketleme 1. aşamada çalışıyor; onay arayüzü sunumun ana mesajına katkı vermiyor. 4 `TAGGED` video onaylanmadığı için şimdilik akışa girmez (akışa yalnızca `LIVE` girer). | `ContentTopic.approve(...)` ve `ContentItem` durum geçişi hazır; admin modülüne yalnızca endpoint ve ekran eklenir. |
| **Karşılaştırma ekranı** (`/admin/compare`, senaryo adım 6) | İsteğe bağlı adım; Selin personasına bağlı. | `FeedPreview` (feed api) ve `ScriptedExamPersona` ile eklenir. |
| **Feed debug ekranının arayüzü** | Debug bilgisi MVP'de `GET /api/admin/feed-debug` API'si olarak verilir (soru gelirse Swagger UI'dan gösterilir). | Aynı API'yi kullanan bir admin sayfası. |
| **Puanlarım ekranı** | Senaryoda kullanılmıyor. Ana Sayfa görünüm amaçlı olarak var (giriş sonrası açılır); puan üst çubukta ve mikro-soru panelinde görünür. | `GET /api/points` ve profil API'leri bu ekranları besleyecek şekilde tasarlanır; yalnızca yeni route ve sayfa eklenir. |
| **Mert ve Selin persona'ları** (`WEAK_PROBLEMS`, `SCRIPTED_EXAM`) | Karşılaştırma ve yedek öğrenci senaryoları MVP'de yok. Kullanıcılar seed'de durur. | Yeni `DemoPersona` bean'leri; persona seçimi isimle yapıldığı için mevcut kod değişmez. |
| **kafka-ui** | Geliştirme kolaylığı; demoyu etkilemiyor. Compose dosyasında `tools` profiliyle isteğe bağlı durur; MVP doğrulamalarında ve CI'da kullanılmaz, üzerine iş yapılmaz. | Gerekirse sürüm güncellemesi ve README notu. |
