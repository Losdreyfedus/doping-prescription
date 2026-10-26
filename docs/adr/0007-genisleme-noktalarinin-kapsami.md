# ADR 0007: Genişleme noktalarının MVP'deki gerçek kapsamı

- **Durum:** Kabul edildi
- **Tarih:** 2026-10-23

## Bağlam

`docs/mimari.md` §10, ertelenen özelliklerin "mevcut kodu değiştirmeden, yeni bir bean ile" eklenebileceğini söylüyordu. Sunum öncesi kod incelemesi, beş genişleme noktasının bu iddiayı aynı ölçüde karşılamadığını gösterdi:

- `DemoPersona` ve `ReasonTemplate`: gerçekten açık. Uygulamalar `List<...>` olarak alınır, ada veya havuz türüne göre seçilir; yeni bir uygulama yalnızca yeni bir bean'dir.
- `CandidatePool`: arayüz ve bean'ler var, ama yeni bir havuz türü bugün şunlara da dokunur: `PoolType` enum'u, `prescription.feed.quota` alanları (`FeedProperties.Quota`), `QuotaAllocator`'daki havuz sırası ve eksik kota zinciri, `feed_impression.pool` kontrol kısıtı (yeni migration). `REVIEW_DUE` için tekrar izleme istisnası `FeedComposer`'da, "zayıflık" bileşenini hangi havuzların alacağı `WeightedLinearRanker`'da türe göre yazılıdır.
- `FeedRanker` ve `ContentTagger`: arayüzün tek bir bean'i vardır ve tekil olarak enjekte edilir; ikinci bir uygulama eklemek seçim kuralı (config) gerektirir.
- `simulation` modülü için öngörülen `FeedPreview` ve dışarıya açık sıralama arayüzü yoktur; `FeedRanker` ve `CandidatePool` `feed.domain` içinde, modül dışına kapalıdır.

## Karar

1. MVP'de bu durum bilinçli olarak korunur. Demo yolunda yeni havuz, ikinci sıralayıcı veya simülasyon yoktur; genelleştirmek bugün kullanılmayan bir esneklik için kodu karmaşıklaştırırdı.
2. Dokümanlar gerçeği söyler: `docs/mimari.md` §1 ve §10 bu ADR'ye göre güncellendi.
3. Ucuz bir koruma eklendi: şablonu olmayan bir havuz `FeedComposer` kurulurken reddedilir. Böylece eksik bir şablon her istekte sessizce fallback akışa düşmek yerine uygulamanın açılışında görünür.
4. Bir genişleme gerçekten gerektiğinde yapılacaklar:
    - **Yeni havuz:** `CandidatePool`'a `order()` ve havuza özgü kural bayrakları (tekrar izleme istisnası, zayıflık bileşeni) eklenir; kota `Map<PoolType, Double>` olarak config'den bağlanır; eksik kota zinciri config'den okunur. `PoolType` enum'u ve DB kısıtı kalır (havuz adı denetlenen bir değerdir).
    - **İkinci sıralayıcı:** `prescription.feed.ranker` ile `List<FeedRanker>` içinden `version()` ile seçim (A/B).
    - **Simülasyon / karşılaştırma:** `feed` modülüne `FeedPreview` (`feed.api`) eklenir; simülasyon sıralamayı bu arayüz üzerinden çağırır, `feed.domain`'e erişmez.

## Sonuçlar

- "Open-closed" iddiası abartılı değil; her noktanın maliyeti ve izlenecek yol yazılı.
- `DemoPersona` ve `ReasonTemplate` örnek olarak gösterilebilir: Mert ve Selin persona'ları yalnızca yeni bean'lerdir.
- Yeni bir havuz bugün yaklaşık yarım günlük bir değişikliktir; 4. madde bunu bir kerelik refactor'a çevirir.
