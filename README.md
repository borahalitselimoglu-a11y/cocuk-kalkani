# Çocuk Kalkanı — Android ebeveyn kontrolü

Harici kütüphane kullanmayan, tamamen cihaz üzerinde çalışan basit bir ebeveyn kontrol uygulaması.

- **Uygulama kilidi:** Tüm uygulamalar varsayılan olarak kilitlidir; ebeveyn yalnızca çocuğun serbestçe kullanabileceklerini açar. Sonradan yüklenen uygulamalar da kilitli gelir. Listede isimle arama yapılabilir. Gelen arama ve çalan alarm ekranları kilitlenmez.
- **Reklam ve zararlı site engeli:** Telefonun DNS'i, cihaz içi yerel bir VPN ile **AdGuard Aile DNS**'e yönlendirilir (reklam, izleyici, yetişkin içerik, kötü amaçlı siteler engellenir; güvenli arama zorlanır). İstenirse AdGuard Standart veya Cloudflare Aile seçilebilir.
- **Silme koruması:** Cihaz yöneticisi etkinleştirilince uygulama doğrudan silinemez.
- **Koruma önlemleri:** Ebeveyn paneli PIN ister, 5 hatalı denemede 30 sn bekletir, ekran kapanınca açılan kilitler sıfırlanır, telefon yeniden başlayınca DNS koruması kendiliğinden açılır.

Gizlilik: Ekran içeriği okunmaz. VPN yalnızca DNS sorgularını filtreli sunucuya iletir; diğer internet trafiğine dokunmaz ve hiçbir veri geliştiriciye gönderilmez.

---

## APK nasıl elde edilir

### Yol A — GitHub ile (bilgisayara bir şey kurmadan)
1. GitHub'da yeni bir depo açın ve bu klasörün **tüm içeriğini** (gizli `.github` klasörü dahil) yükleyin.
2. Depoda **Actions** sekmesine gidin. "APK Derle" iş akışı kendiliğinden çalışır (çalışmazsa "Run workflow").
3. ~3–5 dakika sonra işin sayfasında **Artifacts › CocukKalkani-apk** dosyasını indirin, zip içindeki `app-debug.apk` sizin APK'nız.

> Not: Her derleme yeni bir hata ayıklama anahtarıyla imzalanır. Güncelleme yüklerken "uygulama yüklenmedi" hatası alırsanız önce eskisini kaldırın.

### Yol B — Android Studio ile
Klasörü **File › Open** ile açın, Gradle eşitlemesini bekleyin, **Build › Build APK(s)**.

---

## Telefonda kurulum (çocuğun telefonunda, ebeveyn yapar)

1. APK'yı yükleyin ("bilinmeyen kaynaklar" izni isteyebilir).
2. Uygulamayı açın, **ebeveyn PIN'i** belirleyin.
3. **Android 13 ve üstü için önemli:** Erişilebilirlik izni gri/kapalı görünürse:
   Ayarlar › Uygulamalar › Çocuk Kalkanı › sağ üst **⋮** › **"Kısıtlanmış ayarlara izin ver"**. Sonra tekrar deneyin.
4. **Uygulama kilidi:** "Erişilebilirlik ayarlarını aç" › İndirilen uygulamalar › Çocuk Kalkanı › Aç.
5. **DNS koruması:** "Korumayı aç" › VPN iznini onaylayın. Bildirim çubuğunda anahtar simgesi görünür.
6. **Kalıcı yapın:** "VPN ayarlarını aç" › Çocuk Kalkanı'nın yanındaki ⚙️ › **Her zaman açık VPN** → açık.
   ⚠️ **"VPN olmadan bağlantıları engelle"** seçeneğini açmayın (bu VPN yalnızca DNS taşıdığı için internet kesilebilir).
7. **Silme koruması:** "Silme korumasını etkinleştir" › Etkinleştir.
8. Listede tüm uygulamalar kilitli gelir. Çocuğun PIN'siz kullanabileceği uygulamaların işaretini kaldırın (üstteki arama kutusuyla bulabilirsiniz). **Ayarlar** ve **Play Store** kilitli kalmalı; yoksa çocuk korumaları kapatabilir. (Panelden açtığınız Ayarlar ekranları sizden tekrar PIN istemez.)

### Test
- Kilitli bir uygulamayı açın → PIN ekranı gelmeli.
- Tarayıcıda reklamlı bir haber sitesine girin → reklam alanlarının boş kaldığını görmelisiniz.
- Bir yetişkin içerik sitesi adresi yazın → sayfa açılmamalı (AdGuard Aile filtresinde).

---

## Sınırlamalar (dürüst liste)

- **DNS filtresi her reklamı yakalayamaz.** Reklamı içerikle aynı sunucudan gelen uygulamalar (ör. YouTube videolarındaki reklamlar) DNS ile engellenemez. Uygulama içi reklamların ve rastgele açılan reklam sayfalarının büyük çoğunluğu engellenir.
- **Başka bir VPN** açılırsa (ör. çocuk bir VPN uygulaması kurarsa) bu koruma devre dışı kalır → Play Store'u kilitleyin.
- **Güvenli mod:** Android'in güvenli modunda üçüncü taraf uygulamalar çalışmaz; bilgili bir çocuk bunu kullanabilir. Tam bir kilit için cihaz sahibi (Device Owner) modu gerekir; Google Family Link ile birlikte kullanmak iyi bir tamamlayıcıdır.
- Bazı üreticiler (Xiaomi, Huawei, Samsung'un bazı modelleri) arka plandaki servisleri agresif kapatır. Ayarlar › Pil › Çocuk Kalkanı → **Kısıtlama yok / Optimize etme** yapın.
- Yalnızca IPv4 DNS sorguları filtrelenir; çoğu mobil ve ev ağında bu yeterlidir.

## Proje yapısı

```
app/src/main/java/com/cocukkalkani/app/
  MainActivity.kt    Ebeveyn paneli (PIN, durumlar, uygulama listesi)
  LockActivity.kt    Kilitli uygulamanın üstüne çıkan PIN ekranı
  AppLockService.kt  Erişilebilirlik servisi: ön plandaki uygulamayı izler
  DnsVpnService.kt   Yerel DNS VPN'i: sorguları AdGuard'a iletir
  Prefs.kt           PIN (tuzlanmış SHA-256), serbest uygulama listesi, DNS seçenekleri
  Receivers.kt       Açılışta başlatma + cihaz yöneticisi
  Apps.kt            Kilitlenebilir uygulama listesi, arama yardımcısı
  Ui.kt              Küçük arayüz yardımcıları
```

minSdk 26 (Android 8.0) · targetSdk 34 · Kotlin · harici bağımlılık yok.
