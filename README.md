# Doping Prescription

Doping Hafıza için bir sunum demosu: öğrenci denemesini her zamanki gibi çözer, sistem sonuçtan konu bazlı eksikleri kendisi çıkarır ve Dopingle video akışını bu eksiklere göre "reçete" olarak kurar. Video sonrası mikro-soru, puan ve konu ustalık skoru döngüyü tamamlar. Hedef senaryo: [docs/demo-senaryosu.md](docs/demo-senaryosu.md).

> Bu README 0. aşamada iskelet olarak oluşturuldu; mimari diyagramı, event akışı ve "üretime geçerken yapılacaklar" 9. aşamada tamamlanacak ([docs/kod-standartlari.md](docs/kod-standartlari.md) §6).

## Gereksinimler

- Java 21
- Node.js 24 LTS (npm ile)
- Docker (Docker Desktop veya eşdeğeri; Compose ve Testcontainers için)

Maven kurmaya gerek yok; `./mvnw` wrapper'ı kullanılır.

## Kurulum

Repo kökünden, her uygulama ayrı bir terminalde:

```bash
docker compose up -d
```

```bash
cd doping-prescription-core && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

```bash
cd doping-prescription-ui && npm ci && npm run dev
```

| Adres | İçerik |
| --- | --- |
| http://localhost:5173 | Web uygulaması |
| http://localhost:8080/actuator/health | Sağlık durumu; postgres, redis ve kafka ayrıntıları yalnızca ADMIN'e görünür (2. aşamadan itibaren) |
| http://localhost:8090 | Kafka UI (isteğe bağlı: `docker compose --profile tools up -d`) |

## Test ve kalite komutları

| Komut | Nerede | Ne yapar |
| --- | --- | --- |
| `./mvnw verify` | `doping-prescription-core` | Birim + Testcontainers entegrasyon testleri, Spotless, Checkstyle, SpotBugs, JaCoCo, Spring Modulith ve ArchUnit kuralları |
| `./mvnw spotless:apply` | `doping-prescription-core` | Java kodunu biçimlendirir |
| `npm run check` | `doping-prescription-ui` | `tsc`, ESLint (0 uyarı), Prettier kontrolü, Vitest |
| `npm run format` | `doping-prescription-ui` | Frontend kodunu biçimlendirir |

## Dokümanlar

- [AGENTS.md](AGENTS.md): proje brief'i ve aşama planı
- [docs/mimari.md](docs/mimari.md): modüller, event akışı, senkron/asenkron kararları
- [docs/moduller/](docs/moduller/): testlerden üretilen modül diyagramları
- [docs/ilerleme.md](docs/ilerleme.md): aşama notları ve öz-değerlendirmeler

## ADR'ler

| No | Karar |
| --- | --- |
| [0001](docs/adr/0001-moduler-monolit.md) | Modüler monolit (Spring Modulith) |
| [0002](docs/adr/0002-kafka-ve-outbox.md) | Kafka ve transactional outbox |
| [0003](docs/adr/0003-aciklanabilir-kural-tabanli-algoritma.md) | Açıklanabilir, kural tabanlı öneri algoritması |
| [0004](docs/adr/0004-openapi-sozlesmesi.md) | OpenAPI ile backend–frontend sözleşmesi |
| [0005](docs/adr/0005-modul-sinirlari-netlestirme.md) | Modül sınırlarının netleştirilmesi |
