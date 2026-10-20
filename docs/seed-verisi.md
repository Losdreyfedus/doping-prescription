# Seed verisi

Seed içeriği okunabilir olsun diye `doping-prescription-core/src/main/resources/seed/*.json` dosyalarında tutulur ve yalnızca `demo` profilinde çalışan bir `DemoDataSeeder` (`ApplicationRunner`) tarafından yüklenir. Şema seed'e dahil değildir, Flyway'den gelir. Seed idempotenttir: tekrar çalıştırıldığında `slug` / `code` / sabit id üzerinden upsert yapar, veriyi çoğaltmaz. Her modül kendi verisini kendi seeder'ıyla yükler (modül sınırları seed'de de geçerlidir); `DemoDataSeeder` yalnızca sırayı yönetir.

Soruları, video başlıklarını ve açıklamalarını sen yazacaksın. Kurallar:

- Her seed JSON dosyasının başına `"_note": "Editör kontrolünden geçmedi"` alanı koy.
- Sorular TYT Matematik seviyesinde, kısa ve tek doğru cevaplı olsun. Her sorunun cevabını yazdıktan sonra kendin çöz ve doğrula.
- Bir seed doğrulama testi yaz: her sorunun `correct_key` değeri `options` içinde var; deneme sorularında 5, mikro-sorularda 4 şık var; şık metinleri tekrar etmiyor.
- Kişi, öğretmen ve öğrenci isimleri kurgusaldır.

## Kullanıcılar

| slug | display_name | role | personalization_enabled | Başlangıç durumu |
| --- | --- | --- | --- | --- |
| deniz | Deniz | STUDENT | true | Persona `NEW` |
| mert | Mert | STUDENT | true | Persona `WEAK_PROBLEMS` |
| selin | Selin | STUDENT | false | Persona `SCRIPTED_EXAM` |
| admin | Admin | ADMIN | — | — |

Öğrencilerin `exam_date` değeri seed çalıştırıldığı günden 240 gün sonrası.

## Konular (TYT Matematik)

| id | code | name | exam_weight | Anahtar kelimeler (weight) |
| --- | --- | --- | --- | --- |
| 1 | TYT.MAT.TEMEL | Temel Kavramlar | 0.90 | temel kavramlar (1.0), tek cift sayilar (0.6), ardisik sayilar (0.6) |
| 2 | TYT.MAT.SAYIBAS | Sayı Basamakları | 0.50 | basamak (0.8), sayi basamaklari (1.0), cozumleme (0.6) |
| 3 | TYT.MAT.BOLUNME | Bölme ve Bölünebilme | 0.50 | bolunebilme (1.0), bolme (0.6), kalan (0.5) |
| 4 | TYT.MAT.RASYONEL | Rasyonel Sayılar | 0.60 | rasyonel (1.0), kesir (0.6), ondalik (0.5) |
| 5 | TYT.MAT.USLU | Üslü ve Köklü Sayılar | 0.60 | uslu (1.0), koklu (1.0), us (0.5), karekok (0.8) |
| 6 | TYT.MAT.PROBLEM | Problemler | 1.00 | problem (0.8), yas problemi (1.0), yuzde (0.7), hiz problemi (1.0) |
| 7 | TYT.MAT.KUMELER | Kümeler | 0.40 | kume (1.0), kesisim (0.6), birlesim (0.6) |
| 8 | TYT.MAT.PERKOM | Permütasyon-Kombinasyon | 0.50 | permutasyon (1.0), kombinasyon (1.0), sayma (0.6), faktoriyel (0.7) |
| 9 | TYT.MAT.OLAS | Olasılık | 0.50 | olasilik (1.0), zar (0.5), para atisi (0.5), kosullu (0.7) |
| 10 | TYT.MAT.FONK | Fonksiyonlar | 0.60 | fonksiyon (1.0), tanim kumesi (0.7), bileske (0.8) |

Anahtar kelimeler normalize edilmiş biçimde yazılmıştır (`docs/algoritma.md` §5). `exam_weight` değerleri demo için verilmiş yaklaşık değerlerdir; gerçek ürüne geçerken geçmiş sınav istatistikleriyle güncellenmelidir.

## Videolar

Toplam 40 video: konu başına 4. Bunların 36'sı `LIVE` ve `MANUAL` etiketli (`weight 1.0`, `approved_by = admin`); 4'ü `DRAFT` ve etiketsiz (admin etiketleme demosu için; seed bunları kaydeder, sonra `content.created` event'i ile otomatik etiketleme çalışır ve `TAGGED` olurlar).

- `DRAFT` olacak videolar: konu 2, 5, 7 ve 10'un 4. videoları. Başlıklarında konunun ilk anahtar kelimesi geçsin ki etiket önerisi çıksın.
- `duration_sec` 35–85 arası; `difficulty` her konuda 1, 2, 3, 4; `content_kind` her konuda en az bir `CONCEPT`, bir `SOLVED_EXAMPLE`, bir `TIP`.
- `quality_score` 0.55–0.95 arası, konu içinde farklı.
- `teacher_name`: Ayşe Öğretmen, Burak Öğretmen, Can Öğretmen, Derya Öğretmen (döngüsel).
- `published_at`: son 60 gün içine yayılmış.
- `video_url`: hepsi `null` (simüle oynatıcı).

Demo senaryosunda en çok görünecek iki konunun videoları:

| Konu | Başlık | Tür | Zorluk | Süre (sn) |
| --- | --- | --- | --- | --- |
| Olasılık | Olasılığın temeli: istenen bölü tüm durumlar | CONCEPT | 1 | 58 |
| Olasılık | Zar ve para atışında olasılık, 3 örnek | SOLVED_EXAMPLE | 2 | 72 |
| Olasılık | Koşullu olasılığı 60 saniyede çöz | TIP | 3 | 60 |
| Olasılık | Bağımsız olaylar ve "ve / veya" kuralı | SOLVED_EXAMPLE | 4 | 80 |
| Permütasyon-Kombinasyon | Sayma kuralı: çarpma mı toplama mı? | CONCEPT | 1 | 55 |
| Permütasyon-Kombinasyon | Faktöriyel ve sıralama soruları | SOLVED_EXAMPLE | 2 | 70 |
| Permütasyon-Kombinasyon | Kombinasyonda seçim mi dizilim mi? | TIP | 3 | 50 |
| Permütasyon-Kombinasyon | Tekrarlı permütasyon, 2 zor örnek | SOLVED_EXAMPLE | 4 | 85 |

Diğer konuların başlık ve açıklamalarını aynı üslupla sen yaz.

## Mikro-sorular

- Her `LIVE` videoya en az 1 mikro-soru; Olasılık ve Permütasyon-Kombinasyon videolarına 2'şer.
- Soru, videonun başlığındaki fikri doğrudan ölçsün ve 60 saniyede çözülebilsin.
- 4 şık (A–D), 1–2 cümlelik açıklama.
- Olasılık 1. videonun ilk mikro-sorusu `docs/api.md` içindeki örnek olsun: zar, çift olduğu biliniyor, 4 olma olasılığı, doğru cevap 1/3.

## Mini Deneme 1 (sunum denemesi)

`exam.id = 1`, "TYT Matematik Mini Deneme 1", 20 dakika, 12 soru, 5 şık (A–E). Soruların konu ve doğru cevap harfleri **sabittir**; soruları bu harflere uyacak şekilde yaz.

| Soru | Konu | Doğru cevap | Senaryoda verilecek cevap |
| --- | --- | --- | --- |
| 1 | Temel Kavramlar | B | B (doğru) |
| 2 | Sayı Basamakları | D | D (doğru) |
| 3 | Bölme ve Bölünebilme | A | A (doğru) |
| 4 | Olasılık | C | A (yanlış) |
| 5 | Olasılık | E | B (yanlış) |
| 6 | Olasılık | B | D (yanlış) |
| 7 | Permütasyon-Kombinasyon | D | D (doğru) |
| 8 | Permütasyon-Kombinasyon | A | boş |
| 9 | Rasyonel Sayılar | C | C (doğru) |
| 10 | Üslü ve Köklü Sayılar | E | E (doğru) |
| 11 | Problemler | B | B (doğru) |
| 12 | Kümeler | D | D (doğru) |

Sonuç: 8 doğru, 3 yanlış, 1 boş, net 7,25. Fonksiyonlar bilerek denemede yok; böylece senaryo sonrası akışta bir `EXPLORE` kartı (Fonksiyonlar) görünür. Soru 7'nin 8'den önce gelmesi önemlidir: mastery hesabı soru sırasıyla yapılır (bkz. `docs/algoritma.md` §3'teki kontrol değerleri).

Bu tablo admin "Sunum cevap anahtarı" kartında gösterilir ve Playwright testi bu cevapları kullanır.

## Mini Deneme 2

`exam.id = 2`, "TYT Matematik Mini Deneme 2", 12 soru: Problemler 4 soru, Temel Kavramlar, Rasyonel Sayılar, Üslü ve Köklü Sayılar, Kümeler, Fonksiyonlar, Bölme ve Bölünebilme, Sayı Basamakları, Olasılık birer soru. Doğru cevap harflerini dengeli dağıt.

## Persona'lar

Persona'lar gerçek servis akışıyla uygulanır (attempt aç, cevapları yaz, gönder). Geçmiş tarihli kayıt gereken yerlerde persona uygulayıcı, ilgili modülün `api` metodunu `Clock.offset(clock, Duration.ofDays(-10))` gibi bir saatle çağırır; global saat değiştirilmez (bkz. `docs/mimari.md` §9).

| Persona | Uygulama |
| --- | --- |
| `NEW` | Öğrencinin tüm verisi silinir. |
| `SCRIPTED_EXAM` | Mini Deneme 1, yukarıdaki "senaryoda verilecek cevap" sütunuyla çözülür ve gönderilir. |
| `WEAK_PROBLEMS` | 10 gün önce: Mini Deneme 1, Soru 11 yanlış (A), diğerleri doğru. 2 gün önce: Mini Deneme 2, Problemler sorularından 3'ü yanlış, 1'i doğru, diğer sorular doğru. Ayrıca 2 gün önce Problemler videolarından 2 mikro-soru yanlış cevaplanmış. |

`WEAK_PROBLEMS` sonrasında Mert'in en zayıf konusu Problemler olmalı ve 10 gün önceki denemede doğru yaptığı konular `REVIEW_DUE` havuzuna girebilmeli (mastery ≥ 0.6 ve son sinyal ≥ 7 gün). Bunu bir entegrasyon testiyle doğrula.
