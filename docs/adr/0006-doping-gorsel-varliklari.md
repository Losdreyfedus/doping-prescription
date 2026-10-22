# ADR 0006: Doping Hafıza'nın kendi görsel varlıklarının kullanılması

- **Durum:** Kabul edildi
- **Tarih:** 2026-10-22

## Bağlam

AGENTS.md §5.11 internetten görsel ve logo indirilmesini yasaklıyordu; Ana Sayfa bu yüzden CSS çizimleriyle kuruldu. Sunum yönetime yapılacağı için ekranın Doping Hafıza'nın gerçek arayüzüne birebir benzemesi istendi. Ürün sahibi, Doping Hafıza'nın kendi test sitesindeki (online-education-ui-uat.dopingtech.net) görselleri projeye ekledi.

## Karar

1. Doping Hafıza'nın **kendi** görsel varlıkları (logo, menü ikonları, puan ve gün sayacı ikonları, rozet, Günlük Görevler halkası, duyuru görseli, YKS yolculuğu arka planı, Bire Bir Uygulamalar logoları) kullanılabilir. Dosyalar `doping-prescription-ui/public/doping/` altında, içerik özetli (hash) ad olmadan tutulur ve repoya commit edilir.
2. Bileşenler bu dosyalara yalnızca `src/shared/ui/dopingAssets.ts` haritası üzerinden erişir.
3. Üçüncü taraf telifli içerik yasağı sürer: video, soru ve başka kurumların görselleri indirilmez. Video kapağı olarak kullanılan fotoğraf (Doping'in test sitesinde de kullanılıyor) Unsplash lisanslıdır.
4. Kullanılmayan büyük dosyalar (giriş arka planı, bağlantı hatası görseli) repoya eklenmez.

## Sonuçlar

- Ana Sayfa, üst çubuk ve menü gerçek arayüze görsel olarak çok yaklaşır.
- Proje dışarıya (açık kaynak vb.) verilecekse `public/doping/` içeriği Doping'in izniyle sınırlıdır; bu durumda dosyalar çıkarılır ve bileşenler CSS çizimlerine döner.

## Değerlendirilen seçenekler

- **CSS çizimleri (önceki durum):** Lisans sorunu yok, ama sunumda "gerçek ürün" hissi zayıf.
- **Görselleri yerelde tutup `.gitignore`'a eklemek:** CI ve temiz kurulumda farklı görünüm üretir; demo tekrarlanabilirliğini bozar.
