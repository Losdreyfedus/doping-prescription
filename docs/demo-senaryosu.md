# Demo senaryosu

Ürünün hedefi bu senaryodur. Senaryoda görünmeyen bir özellik için zaman harcama; senaryodaki her adım ise kusursuz çalışmalı. Bu senaryo `e2e/` klasöründe Playwright testi olarak da çalışır (`cd e2e && npm test`).

Ana mesaj: **"Öğrenci hiçbir şey yapmadı; sistem denemeden eksiğini kendisi anladı ve akışını buna göre kurdu."**

## Kişiler

| Kod | İsim | Durum | Kullanım |
| --- | --- | --- | --- |
| `deniz` | Deniz | Hiç denemesi yok, kişiselleştirme açık | Sunumun ana öğrencisi |
| `mert` | Mert | Geçmişte 2 deneme çözmüş, Problemler konusunda zayıf | Yedek; hazır profil göstermek için |
| `selin` | Selin | Deniz ile aynı deneme sonucu, kişiselleştirme **kapalı** | Karşılaştırma (kontrol grubu) |

İsimler kurgusaldır. Kişilerin seed verisi `docs/seed-verisi.md` içindedir.

## Hazırlık (sunumdan önce, izleyici görmez)

1. Admin panelinde `/admin/demo` ekranını aç.
2. Deniz için **Sıfırla**'ya bas: denemeleri, sinyalleri, ustalık skorları, akış geçmişi ve puanları silinir. Selin, Deniz'in sunumda yapacağı deneme sonucuyla hazır yüklenir.
3. Deniz için **Persona yükle** → `SCRIPTED_IN_PROGRESS`: Mini Deneme 1 başlatılmış, 12 cevabın hepsi senaryo cevaplarıyla işaretli, ama deneme **gönderilmemiş** durumda hazırlanır. Deniz'in hâlâ hiç sinyali yoktur, bu yüzden 1. adımdaki akış kişiselleştirilmemiş kalır.
4. Öğrenci uygulamasını ayrı bir sekmede aç, Deniz ile giriş yap.

Sunumun tamamı 10–12 dakikadır; deneme canlı çözülmez, böylece demo bölümü kısa ve hatasız kalır.

## Senaryo

### Adım 1: Akış bugün böyle (yaklaşık 20 sn)

- Deniz sol menüden Dopingle'ı açar.
- Akışta farklı konulardan videolar vardır. Kart etiketleri genel ifadelerdir: "TYT'de sık çıkan konu", "Bu hafta en çok izlenenlerden".
- Anlatım: "Bugün Dopingle herkese benzer videolar gösteriyor. Sistem öğrencinin neyi bilmediğini bilmiyor."

### Adım 2: Öğrenci sadece denemesini bitirir (yaklaşık 10 sn)

- Deniz sol menüden Testler ve Denemeler'e geçer ve "TYT Matematik Mini Deneme 1"i açar (12 soru). Deneme hazırlıkta başlatılmıştır; ekran mevcut denemeyi açar.
- Cevap kâğıdında cevaplar zaten işaretlidir (`SCRIPTED_IN_PROGRESS` personası, cevap anahtarı `docs/seed-verisi.md` içindedir):
    - 3 Olasılık sorusu: **yanlış**
    - 1 Permütasyon-Kombinasyon sorusu: **boş**
    - Diğer 8 soru: doğru
- Sunucu cevap kâğıdını kısaca gösterir ve **Sınavı Bitir**'e basar; onay penceresinde ("1 soru boş…") bitirmeyi onaylar.
- Sonuç ekranı: 8 doğru, 3 yanlış, 1 boş, 7,25 net. Konu tablosunda Olasılık ve Permütasyon kırmızıdır.
- Sonuç ekranının altında bir bant görünür: "Dopingle akışın bu sonuca göre güncellendi." ve **Reçeteni gör** butonu.
- Anlatım: "Öğrenci bir şey yüklemedi, bir şey seçmedi. Sadece denemesini çözdü."

### Adım 3: Reçete (yaklaşık 30 sn)

- **Reçeteni gör** Dopingle'ı açar.
- İlk kart Olasılık videosudur. Etiketi: "Son denemende Olasılık: 3 soruda 0 doğru".
- İlk 5 kartın en az 3'ü Olasılık veya Permütasyon-Kombinasyon konusundandır.
- Aynı konudan art arda en fazla 2 kart gelir (Olasılık, Olasılık, Permütasyon, Permütasyon, …).
- İlk 10 kartta en az bir "TYT'de sık çıkan konu: Fonksiyonlar" kartı vardır; Fonksiyonlar denemede olmadığı için keşif havuzundan gelir.
- Anlatım: "Instagram ilgini bilir; Doping eksiğini biliyor."

### Adım 4: Kontrol ve puan (yaklaşık 30 sn)

- İlk video oynar (simüle oynatıcı, demo hızında yaklaşık 15 sn).
- Video bitince mikro-soru penceresi açılır: tek soru, 4 şık, 60 sn süre.
- Deniz doğru cevaplar.
- Pencerede: yeşil onay, kısa çözüm açıklaması, **+15 puan** (zayıf konu bonusu), seri sayacı ve "Olasılık ustalığın %17 → %27" (varsayılan config ile beklenen değerler; hesap `docs/algoritma.md` içindeki örnekte).
- Anlatım: "İzledi, anladığını kanıtladı, ödülünü aldı."

### Adım 5: İyileşme (yaklaşık 15 sn)

- Deniz sol menüden Eksiklerim'e geçer.
- Olasılık en üstte, kırmızı bölgededir; çubuğu az önceki artışla birlikte animasyonla yükselir ve yanında yukarı ok vardır.
- Her konunun yanında "Bu konudan 6 video hazır" gibi bir bilgi ve **Çalış** butonu vardır.

### Adım 6: Karşılaştırma (isteğe bağlı, yaklaşık 20 sn)

- `/admin/compare` ekranı: yan yana iki koyu akış sütunu, solda Deniz, sağda Selin.
- İkisi aynı deneme sonucuna sahiptir; Deniz'in akışı Olasılık ağırlıklı, Selin'inki (kişiselleştirme kapalı) karışıktır.

### Adım 7: Nasıl çalışıyor? (soru gelirse)

- `/admin/feed-debug?slug=deniz`: her kartın skoru ve bileşenleri (zayıflık, sınav önemi, kalite, tazelik, zorluk uyumu), kartın hangi havuzdan geldiği ve Deniz'in en zayıf 5 konusu.
- `/admin/impact`: simüle edilmiş 400 öğrenciyle kontrol ve deney grubunun 4 haftalık karşılaştırması. Ekranda her zaman "Simülasyon verisi" yazar.

## Senaryonun teknik şartları

| Şart | Değer |
| --- | --- |
| Deneme gönderme → Eksiklerim güncel | En geç 2 sn |
| Deneme gönderme → yeni akış hazır | En geç 2 sn |
| Akış isteği yanıt süresi | p95 < 150 ms (lokalde) |
| Video demo hızı | `feed.demo-video-speed` config değeri, varsayılan 4 |
| Sıfırla işlemi | Tekrarlanabilir, 1 sn altında |
| Toplam demo süresi | 3 dakikanın altında |
