# Arayüz

Öğrenci uygulaması, **Doping Hafıza'nın mevcut web arayüzüne benzemelidir**. Sunumu izleyen yöneticiler bu ekranları tanıyor; demo "yeni bir uygulama" gibi değil, "Doping'e eklenmiş bir özellik" gibi görünmeli. Bu dokümandaki renkler ve düzen, Doping Hafıza test ortamının ekran kaydından ölçülerek alınmıştır.

Öncelik sırası: senaryodaki akışın akıcılığı, Doping'e benzerlik, okunabilirlik, sonra cila. Tüm metinler Türkçe.

## Genel

- React + TypeScript + Vite, React Router, Tailwind, TanStack Query; sunucu verisi yalnızca API'den gelir. Klasör yapısı ve kurallar `docs/kod-standartlari.md` §3'tedir.
- Bu dokümandaki tekrar eden görsel öğeler `shared/ui` altında yeniden kullanılabilir bileşen olur: `AnswerBubble` (A–E balonu; deneme, cevap kâğıdı ve mikro-soru aynı bileşeni kullanır), `ReasonPill`, `TopicChip`, `MasteryBar`, `StatCard`, `IconTile`, `Skeleton`.
- **Masaüstü web düzeni** (hedef çözünürlük 1920×1080, en az 1366×768 düzgün görünmeli). Mobil uyum gerekmez.
- Açık tema; Dopingle sayfası koyu.
- Her veri yüklemesinde iskelet (skeleton) görünümü, her listede boş durum metni.
- Gövde metni Doping Hafıza ile aynı, 14 px; ölçüler gerçek sayfalarla birebir eşleşir (yansıtıcıda gerekirse tarayıcı yakınlaştırması kullanılır).

### Renkler

`globals.css` içinde CSS değişkeni olarak tanımla ve Tailwind temasına bağla. Değerler ekran kaydından ölçüldü.

| Değişken | Değer | Kullanım |
| --- | --- | --- |
| `--brand-blue` | `#3167be` | Logo, birincil vurgular |
| `--brand-blue-ink` | `#375f99` | Logo yazısının mavi kısmı, koyu mavi metin |
| `--brand-red` | `#eb353d` | Logo yazısının kırmızı kısmı |
| `--accent-pink` | `#f14a8b` | Üst çubuktaki yuvarlak vurgu butonu |
| `--navy` | `#2b3e51` | Ana sayfadaki profil kartı (hafif dikey degrade, `#2b3e51` → `#25384b`) |
| `--navy-deep` | `#0b304c` | Sonuç ekranının üst bölümü |
| `--ink` | `#1b2129` | Seçili kategori kartı, koyu butonlar, başlık metni |
| `--select` | `#04a4ed` | Seçili şık balonu, "7 Soru Çözüldü" gibi vurgular |
| `--select-row` | `#d4f3fe` | Cevap kâğıdında aktif soru satırı |
| `--success` | `#1abc9c` | Doğru kartı, başarı halkası |
| `--success-strong` | `#16b58e` | "Sınavı Bitir" butonu |
| `--danger` | `#f84f4f` | Yanlış kartı, başarı halkasının kırmızı kısmı |
| `--danger-strong` | `#ff4e52` | "Testi Bitir" butonu |
| `--orange` | `#f38d1d` | Sonuç ekranındaki birincil buton |
| `--pin` | `#ff693e` | "Tekrar İncele" iğne ikonu |
| `--surface` | `#ffffff` | Sayfa ve kart arka planı |
| `--surface-muted` | `#edf2f3` | Arama kutusu, ikincil butonlar, aktif menü öğesi arka planı (`#f3f8f9`) |
| `--surface-result` | `#e0ecf7` | Sonuç ekranının alt bölümü |
| `--header-tint` | `#e9ebf6` → `#e3eafb` | Bölüm başlık bantları (soldan sağa lavanta → açık mavi degrade) |
| `--dopingle-bg` | `#111111` | Dopingle sayfa arka planı |

Ustalık bantları için: `WEAK` = `--danger`, `MEDIUM` = `--orange`, `STRONG` = `--success`, `UNKNOWN` = `#c0c7ca`.

### Tipografi ve şekil

- Gövde: geometrik, yuvarlak hatlı bir sans-serif (Google Fonts'tan **Plus Jakarta Sans** uygun).
- Sayfa başlıkları ("Testler ve Denemeler" gibi): kalın ve dar bir yazı tipi (**Barlow Semi Condensed** 700), 40–44 px.
- Kartlar ve butonlar belirgin yuvarlak köşeli: kartlar 16–20 px, butonlar 10–12 px, şık balonları tam daire.
- Gölgeler çok hafif; ayrım çoğunlukla açık gri arka plan ve kenarlıklarla yapılır.

### Logo ve görseller

- Logo, şirketin kendi sağladığı `doping-prescription-ui/public/brand/logo.svg` dosyasıdır; sol üstte bu dosya, altında küçük gri "Prescription demo" yazısı gösterilir (`shared/ui/BrandLogo`). Dopingle kartındaki filigran da aynı dosyayı kullanır. Başka logo veya görsel internetten indirilmez.
- Öğrenci avatarı: baş harfli yuvarlak rozet (renkli arka plan). 3B avatar çizme.

## Uygulama kabuğu

Ekran kaydındaki Doping Hafıza düzeni:

- **Sol menü** (yaklaşık 300 px, beyaz): üstte logo; altında her biri **pastel renkli, yuvarlak kare ikon karosu** + metin olan menü öğeleri. Aktif öğenin arka planı `--surface-muted` ve yuvarlak köşeli. Dopingle katmanında menü yalnızca ikonlara daralır (~92 px) ve üzerine gelince ipucu (tooltip) gösterir.
- Menü öğeleri Doping Hafıza'nın gerçek menüsüyle aynıdır (Ana Sayfa, Dersler, Testler ve Denemeler, WAY Evreni, Rehberlik Videoları, İstatistikler, Akıllı Değerlendirme Paneli, Bire Bir Uygulamalar, Yardım Merkezi, Canlı Etkinlikler, Öğrencilerimizden Dinleyin) ve Testler ve Denemeler'in altına **Eksiklerim** eklenir. Yalnızca **Ana Sayfa**, **Testler ve Denemeler** ve **Eksiklerim** ekran açar; diğerleri tıklanmayan görünüm öğeleridir. Dopingle menüde yoktur.
- **Üst çubuk:** solda geniş, `--surface-muted` arka planlı, yuvarlak köşeli arama kutusu ("Ders, ünite ya da konu ara" yer tutucusu; demoda işlevsiz olabilir); sağda **Dopingle'ı açan** `--accent-pink` yuvarlak şimşek butonu, görünüm amaçlı mesaj, takvim ve bildirim ikonları, avatar + öğrenci adı.

## Sayfalar

| Yol | Ekran |
| --- | --- |
| `/` | Demo giriş: öğrenci kartları (Deniz, Mert, Selin) ve küçük "Admin" bağlantısı |
| `/app` | Ana Sayfa |
| `/app/dopingle` | Dopingle akışı |
| `/app/denemeler` | Testler ve Denemeler listesi |
| `/app/denemeler/[attemptId]` | Deneme çözme |
| `/app/denemeler/[attemptId]/sonuc` | Deneme sonucu |
| `/app/eksiklerim` | Konu ustalıkları |
| `/app/puan` | Puan ve seri |
| `/admin/demo` | Demo kontrolü |
| `/admin/compare` | Yan yana iki öğrencinin akışı |
| `/admin/feed-debug?slug=deniz` | Akış skor dökümü |
| `/admin/content` | İçerik, etiket onayı, anahtar kelimeler |
| `/admin/impact` | Etki paneli (simülasyon) |

## Ana Sayfa

Giriş sonrası açılan ilk ekran; Doping Hafıza'nın ana sayfasıyla aynı düzende. Yalnızca öğrenci adı canlıdır; diğer kartlar (profil kartı rozetleri, Günlük Görevlerim, Günlük Çalışma Performansı, Duyurular, Son Aktivitelerim, sağ sütunda haftalık takvim ve "YKS Yolculuğunu Başlat!" kartı) görünüm amaçlıdır. Görseller Doping Hafıza'nın kendi varlıklarıdır (`public/doping/`, ADR 0006); bileşenler bunlara `dopingAssets` haritasıyla erişir. Canlı olanlar: öğrenci adı, toplam puan, seri sayacı ve hafta takvimi (oklar ve Bugün).

## Dopingle

Sağ üstteki şimşek butonuyla açılan **tam ekran katmandır** (`/app/dopingle`); kapat butonu veya Escape, açıldığı sayfaya döndürür.

Ekran kaydındaki Dopingle sayfasının birebir benzeri olmalı:

- Tam sayfa koyu arka plan (`--dopingle-bg`), sol menü ikon moduna daralmış.
- Ortada **dikey 9:16 video kartı** (yükseklik ekranın ~%90'ı), 16 px yuvarlak köşe. Kartın sağ üst köşesinde küçük Doping logosu filigranı.
- **Videonun sağında, dikey sırada yuvarlak yarı saydam aksiyon butonları:** beğen (kalp, altında sayı), kaydet (yer imi), ses (hoparlör).
- **Sayfanın sağ kenarında** üst üste iki yuvarlak buton: önceki video (↑) ve sonraki video (↓).
- **Sol üstte:** gri küçük ipucu metni "↑ ↓ Video değiştir | Space: Duraklat"; altında yarı saydam koyu hap butonlar **Filtre** ve **Kaydedilen** (demoda işlevsiz olabilir).
- **Sağ üstte:** iki küçük anahtar (toggle): otomatik sonraki video ve parlaklık; mavi aktif durum.
- **Sol altta:** kalın beyaz video başlığı, altında gri yarı saydam hap içinde öğretmen adı.
- **Bu projeye özel eklemeler** (ekran kaydında yok, aynı görsel dille ekle):
    - Video kartının alt kısmında, başlığın hemen üstünde **"neden bu video" hapı**: konu renginde küçük nokta + metin ("Son denemende Olasılık: 3 soruda 0 doğru"). Akışın kişisel olduğunu gösteren ana öğe budur; okunaklı olmalı.
    - Video kartının sol üstünde konu çipi ve sağ altta puan rozeti ("+15 puan", `--orange`).
- Klavye: ↑/↓ video değiştirir, Space durdurur/devam ettirir. Fare tekerleği ve sağ kenardaki oklar da çalışır.
- Akış ilk açılışta veya `?refresh=1` ile yeni oturum başlatır. Sona 3 kart kala sonraki sayfa yüklenir.
- `?topicId=` parametresiyle yalnız o konunun videoları gösterilir (Eksiklerim'deki **Çalış** butonu). API tarafında `GET /api/feed?topicId=9` desteklenir.

### Simüle oynatıcı

Gerçek video dosyası olmadığında (`videoUrl: null`) kullanılır; demo bunun üzerine kuruludur.

- 9:16 kart içinde konunun renginde koyu degrade arka plan; ortada büyük beyaz konu adı ve video başlığı (Doping videolarındaki gibi kalın, gölgeli altyazı stili); ortada büyük bir öğretmen baş harfi rozeti.
- Kartın altında ince ilerleme çubuğu ve sağında süre (`00:14 / 00:58`). Süre `durationSec / feed.demo-video-speed` saniyede dolar; ekranda gerçek video süresi gösterilir.
- Kart ekrana gelince otomatik başlar; Space veya tıklama ile durur/devam eder.
- `videoUrl` doluysa aynı arayüz HTML5 `<video>` ile çalışır (dosyalar `doping-prescription-ui/public/videos` altında, git'e eklenmez).
- Oynatıcı mantığı `useSimulatedPlayer` hook'unda (`play`, `pause`, `progress`, `onComplete`), görünüm ayrı bileşende; hook sahte zamanlayıcılarla test edilir.

### Etkileşim olayları

| Olay | Ne zaman |
| --- | --- |
| VIEW_START | Oynatma başladığında |
| SKIP | Oynatma başladıktan sonra 3 saniye (duvar saati) dolmadan video değiştirilirse |
| VIEW_COMPLETE | İlerleme %100 olduğunda |
| LIKE | Beğen |
| QUIZ_SHOWN / QUIZ_DISMISSED | Mikro-soru penceresi açılınca / kapatılınca |

`watchMs` her zaman gerçek video süresi cinsinden gönderilir (demo hızı çarpanı geri alınarak), böylece sunucudaki %70 kontrolü tutarlı olur.

## Mikro-soru penceresi

Görsel dili deneme çözme ekranıyla aynı olmalı ki öğrenci tanıdık bulsun.

- İlerleme %70'i geçince video kartının altında **Soruyu çöz** butonu belirir; video bitince pencere kendiliğinden açılır.
- Pencere, video kartının yanında (sağında) açılan beyaz, 20 px yuvarlak köşeli bir panel; arka plan koyu kalır, video kartı soluklaşır.
- İçerik: üstte konu çipi ve sağda 60 sn geri sayım; soru metni; 4 şık, her biri **solunda harf balonu** olan satır (balon: ince gri kenarlıklı daire, seçilince `--select` dolgu, beyaz harf).
- Şıkka tıklamak cevabı gönderir (onay adımı yok).
- Sonuç durumu: doğruysa seçilen balon `--success` olur, panelde yeşil onay, "+15 puan" yukarı süzülen animasyonla üst çubuktaki puana eklenir. Yanlışsa seçilen balon `--danger`, doğru şık `--success` işaretlenir. Her iki durumda kısa çözüm metni ve **ustalık çubuğu**: önceki değerden yeni değere animasyonla ilerler ("Olasılık %17 → %27").
- **Devam** butonu (`--ink` dolgu, beyaz yazı) pencereyi kapatır ve sonraki videoya geçer.

## Testler ve Denemeler

Ekran kaydındaki liste sayfasına benzer:

- Üstte büyük başlık "Testler ve Denemeler" ve altında gri alt başlık "Tüm testler ve deneme sınavları burada!".
- **Solda kategori kartları** dikey listesi: seçili olan `--ink` dolgulu, beyaz metinli yuvarlak kart ("Mini Denemeler / Tüm mini denemeler burada!"); diğerleri beyaz, ikonlu. Demoda tek kategori yeterli: **Mini Denemeler**.
- **Sağda** `--header-tint` degradeli bir başlık bandı (kategori adı + alt başlık); altında filtre açılır menüleri (Ders, Ünite; demoda tek değer), "2 adet deneme bulunmaktadır." bilgisi ve deneme satırları.
- Her satır: deneme adı, soru sayısı, son çözümün neti ve tarihi (varsa), sağda **Başla** veya **Tekrar Çöz** butonu.

## Deneme çözme

Ekran kaydındaki test çözme ekranının benzeri:

- Üst çubukta solda "← Deneme adı", sağda kırmızı **Testi Bitir** butonu (`--danger-strong`, beyaz yazı, çıkış ikonu).
- **Sol ana alan:** soru metni (büyük, kalın), altında 5 şık; her şıkın solunda harf balonu (A–E), seçilince `--select` dolgu.
- Sorunun solunda dar dikey araç çubuğu (koyu, yuvarlak köşeli): soru numarası ve küçük ikonlar.
- **Sağ panel (~430 px, `#f8fbfe` arka plan): optik cevap kâğıdı.** Üstte konu adı ve "12 Soru / 7 Soru Çözüldü" (çözülen sayısı `--select` renginde). Her satırda soru numarası ve A–E balonları; işaretli balon `--select` dolgulu; aktif sorunun satırı `--select-row` arka planlı. Balona tıklamak da cevabı işaretler, satıra tıklamak o soruya gider.
- **Alt çubuk:** solda **Önceki Soru**, ortada küçük araç butonları ve "Tekrar İncele" (turuncu iğne ikonu, sorunun işaretlenmesi), sağda **Sonraki Soru**; son soruda bunun yerine yeşil **Sınavı Bitir** (`--success-strong`).
- Bitirme onay penceresi: "1 soru boş. Denemeyi bitirmek istiyor musun?"

## Deneme sonucu

Ekran kaydındaki "Testi Tamamladın" ekranının benzeri:

- **Üst bölüm** `--navy-deep` arka planlı, ortalanmış: yuvarlak avatar, "Tebrikler Deniz 🎉 **Denemeyi Tamamladın!**" (beyaz, büyük), altında deneme adı, ortada turuncu (`--orange`) **Sonuçları İncele** butonu (aşağıdaki listeye kaydırır).
- **Alt bölüm** `--surface-result` arka planlı: ortalanmış "Başarı Durumun" başlığı ve yan yana dört kare kart:
    - beyaz kart: "Başarı Durumun" ve yeşil/kırmızı halka grafik, ortada yüzde;
    - `--success` dolgulu kart: "Doğru", büyük sayı, "Soru";
    - `--danger` dolgulu kart: "Yanlış", büyük sayı, "Soru";
    - beyaz kart: "Boş", büyük sayı, "Soru".
- Altında **Konu Analizi** kartı: konu, soru, doğru, yanlış, boş sütunlu tablo; zayıf konuların satırı açık kırmızı.
- **Güncelleme bandı** (bu projeye özel, en kritik öğe): Konu Analizi'nin üstünde, tam genişlikte, `--navy` arka planlı yuvarlak bant. Profil güncellenene kadar "Eksiklerin analiz ediliyor…" ve dönen ikon; güncellenince "Dopingle akışın bu sonuca göre güncellendi. Olasılık ve Permütasyon-Kombinasyon için 8 video hazır." ve turuncu **Reçeteni gör** butonu (`/app/dopingle?refresh=1`).
- En altta soru inceleme listesi: soru, seçilen ve doğru şık balonları, açıklama.

## Eksiklerim

Ekran kaydında karşılığı yok; aynı görsel dille tasarla.

- Sayfa başlığı "Eksiklerim", alt başlık "Deneme ve soru sonuçlarına göre konu durumun".
- Konular mastery'ye göre artan sırada, her biri beyaz yuvarlak kart satırı: konu adı, bant renginde yatay çubuk, yüzde, trend oku, "Bu konudan 6 video hazır" ve **Çalış** butonu (`--ink` dolgu).
- Verisi olmayan konular gri ve en altta: "Henüz veri yok, bir deneme çöz".
- Bant renkleri API'deki `band` alanından gelir, arayüzde eşik hesaplanmaz. Kırmızı (`WEAK`) bant, akışta reçete olarak gelen konularla birebir aynıdır (bkz. `docs/algoritma.md` §1).
- Sayfa açılışında çubuklar 0'dan değerlerine animasyonla dolar; son ziyaretten beri artan konu kısa bir vurgu animasyonu alır.

## Puanlarım

`--navy` üst kart: toplam puan (büyük), bugünkü puan / günlük sınır çubuğu, mevcut ve en iyi seri. Altında son 10 puan hareketi listesi.

## Admin

Öğrenci uygulamasından ayrı, sade bir masaüstü düzeni (aynı renkler, sol menü). Admin `admin` slug'ı ile giriş yapar.

- **Demo kontrolü:** her öğrenci için kart (son deneme, toplam puan, sinyal sayısı) ve **Sıfırla**, **Persona yükle** (açılır menü) butonları. Ayrıca bir **Sunum cevap anahtarı** kartı: Mini Deneme 1'in doğru cevapları ve senaryoda verilecek cevaplar (bkz. `docs/seed-verisi.md`).
- **Karşılaştırma:** iki öğrenci seçici ve yan yana iki koyu sütunda salt okunur kart listeleri (oynatıcı yok; başlık, konu çipi, neden hapı). Üstte her öğrencinin kişiselleştirme durumu.
- **Akış debug:** öğrenci seçici; solda en zayıf 5 konu (mastery, confidence), sağda son akış oturumunun tablosu: sıra, video, havuz, skor ve bileşenleri, uygulanan kural notları.
- **İçerik:** `DRAFT`, `TAGGED`, `LIVE` filtreleri; yeni video ekleme formu (başlık, açıklama, süre, zorluk, tür); `TAGGED` videolarda önerilen konular ve skorları, **Onayla** / konu değiştir; ayrı bir bölümde anahtar kelime tablosu (ekle, sil).
- **Etki paneli:** en üstte sarı bant "Simülasyon verisi"; iki grubun haftalık ortalama net çizgi grafiği, zayıf konulardaki doğru oranı değişimi çubuk grafiği, grup büyüklükleri. Grafikler için Recharts.
