# Çocuk Kalkanı — Google Play yayın rehberi

Bu rehber, ORBIT'in bulunduğu mevcut geliştirici hesabından **Play sürümünü** yayınlamak içindir.

> **Önemli:** Play sürümü reklam engellemez ve internet izni yoktur. Reklam kalkanlı tam sürüm yalnızca GitHub'dan dağıtılır; Play'deki uygulamanın içinde ona bağlantı veya indirme yolu **verilmemelidir**.

---

## 0. Hazırlık — elinizde olması gerekenler

| Ne | Nerede |
|---|---|
| Yüklenecek dosya (`.aab`) | GitHub › Actions › son başarılı derleme › Artifacts › **CocukKalkani-play-aab** |
| Telefonda denemek için APK | Aynı yerde **CocukKalkani-play-apk** |
| Uygulama simgesi 512×512 | `docs/store/simge-512.png` |
| Tanıtım görseli 1024×500 | `docs/store/tanitim-1024x500.png` |
| Gizlilik politikası | `https://borahalitselimoglu-a11y.github.io/cocuk-kalkani/gizlilik.html` (bkz. adım 1) |
| Ekran görüntüleri (en az 2) | Play sürümünü telefona kurup çekin (bkz. adım 4) |
| Erişilebilirlik tanıtım videosu | Telefonda ekran kaydı (bkz. adım 6) |

Paket adı: **`com.cocukkalkani.aile`** (Play'de kalıcıdır, sonradan değiştirilemez).

---

## 1. Gizlilik sayfasını yayına alın (bir kez, 1 dakika)

GitHub › `cocuk-kalkani` deposu › **Settings** › **Pages** › *Build and deployment* › Source: **Deploy from a branch** › Branch: **main**, klasör: **/docs** › **Save**.

Birkaç dakika sonra sayfa şu adreste açılır:
`https://borahalitselimoglu-a11y.github.io/cocuk-kalkani/gizlilik.html`

---

## 2. Play Console'da yeni uygulama

**Tüm uygulamalar › Uygulama oluştur**
- Uygulama adı: `Çocuk Kalkanı: Uygulama Kilidi`
- Varsayılan dil: Türkçe – tr-TR
- Uygulama veya oyun: **Uygulama**
- Ücretsiz veya ücretli: **Ücretsiz**
- Beyanları işaretleyin › **Uygulama oluştur**

---

## 3. Mağaza girişi (Ana mağaza girişi)

**Uygulama adı (30):**
```
Çocuk Kalkanı: Uygulama Kilidi
```

**Kısa açıklama (80):**
```
Çocuğunuz yalnızca sizin izin verdiğiniz uygulamaları açabilsin. PIN ile kilit.
```

**Tam açıklama:**
```
Çocuk Kalkanı, çocuğunuzun telefonu kullanırken yalnızca sizin izin verdiğiniz uygulamaları açabilmesini sağlayan, sade ve eğlenceli bir ebeveyn kilidi uygulamasıdır.

🔒 HER ŞEY BAŞTA KİLİTLİ
Kurulumdan sonra tüm uygulamalar kilitli gelir. Çocuğunuzun serbestçe kullanabileceği uygulamaları tek dokunuşla açarsınız. Sonradan yüklenen uygulamalar da otomatik olarak kilitli gelir.

🔑 PIN İLE EBEVEYN KONTROLÜ
Kilitli bir uygulama açıldığında renkli bir PIN ekranı çıkar. PIN'i bilmeyen uygulamayı açamaz. 5 hatalı denemeden sonra 30 saniye bekleme süresi başlar.

📞 ÖNEMLİ EKRANLAR HER ZAMAN AÇIK
Gelen aramalar ve çalan alarmlar kilitlenmez; çocuğunuz sizin aramanızı her zaman açabilir.

🔍 KOLAY YÖNETİM
Uygulamaları isimle arayın, tümünü tek seferde kilitleyin veya serbest bırakın.

🧷 SİLME KORUMASI
İsterseniz cihaz yöneticisi özelliğini açarak uygulamanın çocuk tarafından kolayca silinmesini önleyebilirsiniz.

🌐 GÜVENLİ İNTERNET REHBERİ
Telefonunuzun kendi "Özel DNS" ayarıyla aile filtreli bir DNS kullanmanın adımlarını uygulama içinde anlatır.

🛡️ GİZLİLİĞİNİZE SAYGILI
• Hiçbir kişisel veri toplanmaz, paylaşılmaz veya satılmaz.
• Uygulamanın internet izni yoktur.
• Reklam, analiz veya takip aracı içermez.

ERİŞİLEBİLİRLİK İZNİ HAKKINDA
Çocuk Kalkanı, kilitli bir uygulama açıldığında PIN ekranını gösterebilmek için Android'in Erişilebilirlik hizmetini (AccessibilityService API) kullanır. Bu izinle yalnızca o an ekranda hangi uygulamanın açık olduğu öğrenilir; ekrandaki içerik okunmaz. Bu bilgi yalnızca telefonda kullanılır, kaydedilmez ve hiçbir yere gönderilmez. İzin, uygulama içindeki açıklama okunup onaylandıktan sonra ebeveyn tarafından Android ayarlarından verilir.
```

**Grafikler:** simge (512), tanıtım görseli (1024×500), telefon ekran görüntüleri (en az 2, önerilen 4).

**Kategori:** Uygulama › **Ebeveynlik** · E-posta: Play'de görünecek iletişim adresiniz.

---

## 4. Ekran görüntüleri — önerilen 4 kare

Play sürümünü (CocukKalkani-play-apk) telefona kurun ve şunları çekin:
1. Ana panel (renkli başlık + kartlar)
2. Uygulama listesi (bazıları 🔒 kilitli, bazıları 🔓 serbest)
3. Kilit ekranı (kilitli bir uygulamayı açınca)
4. Güvenli internet (DNS) rehber kartı

> Ekran görüntülerinde başka bir uygulamanın reklamı, kişisel bildirim veya telefon numarası görünmesin.

---

## 5. Uygulama içeriği (Politika › Uygulama içeriği)

| Bölüm | Cevap |
|---|---|
| **Gizlilik politikası** | Yukarıdaki github.io adresi |
| **Reklamlar** | Hayır, uygulamam reklam içermiyor |
| **Uygulama erişimi** | "Tüm işlevler özel erişim gerektirmez" — *Not:* PIN uygulamayı açan kişinin kendisinin belirlediği bir şifredir; incelemeciye bilgi notu olarak "İlk açılışta istediğiniz 4 haneli PIN'i belirleyin" yazabilirsiniz. |
| **İçerik derecelendirmesi** | Anketi doldurun: kategori *Yardımcı program/Üretkenlik/diğer*; şiddet, cinsellik, kumar vb. hepsine **Hayır**; kullanıcılar arası iletişim **Hayır**. |
| **Hedef kitle** | **Yalnızca 18 yaş ve üzeri** seçin (uygulamayı ebeveynler kurar ve yönetir). "Çocukların ilgisini çekebilir mi?" → Hayır. Bu, Aile Politikası gerekliliklerinin gereksiz yere uygulanmasını önler. |
| **Veri güvenliği** | Veri toplanıyor mu? **Hayır**. Veri paylaşılıyor mu? **Hayır**. (İnternet izni olmadığı için cihaz dışına hiçbir veri gidemez.) |
| **Devlet uygulamaları / Finans / Sağlık** | Hayır |

---

## 6. Erişilebilirlik API beyanı (en kritik adım)

**Uygulama içeriği › Erişilebilirlik API'si** (veya "Hassas izinler")
- Uygulamanız bir erişilebilirlik aracı mı? → **Hayır** (`isAccessibilityTool` beyan etmiyoruz)
- Kullanım amacı: **Uygulama işlevselliği** → açıklama:
  ```
  The AccessibilityService is used only to detect which app is in the foreground (package name) so that a parent-set PIN screen can be shown when a child opens an app the parent has locked. Screen content is never read. The information is processed on-device only, never stored, transmitted or shared. The app has no INTERNET permission. Users see a prominent in-app disclosure and must tap "Kabul ediyorum" (I accept) before being sent to Accessibility settings.
  ```
- API ile kişisel veya hassas veri toplanıyor/paylaşılıyor mu? → **Hayır**

**Video (YouTube'da "Liste dışı" yükleyip bağlantısını verin).** Telefonda ekran kaydını başlatın, şu sırayla gösterin:
1. Çocuk Kalkanı'nı açın, PIN girin.
2. **"⚙️ Erişilebilirlik ayarlarını aç"** › açıklama penceresi açılır — metnin tamamı görünsün (gerekirse kaydırın).
3. **"Kabul etmiyorum"**a dokunun › uyarı mesajı görünür.
4. Düğmeye tekrar dokunun › açıklama yeniden açılır › **"Kabul ediyorum"**.
5. Erişilebilirlik ayarlarında Çocuk Kalkanı'nı açın, panele dönün (✅ Kilit AÇIK).
6. Kilitli bir uygulamayı açın › PIN ekranı çıkar › PIN girip açın.

---

## 7. Test ve yayın

1. **Test › Kapalı test** › yeni kanal › test kişilerinin e-posta listesini ekleyin.
2. **Yeni sürüm oluştur** › Play App Signing'i kabul edin › `.aab` dosyasını yükleyin › sürüm notu: `İlk sürüm` › **İncelemeye gönder**.
3. Hesabınız 13 Kasım 2023'ten sonra açılmış bir kişisel hesapsa ve Play Console istiyorsa: **en az 12 test kişisi 14 gün kesintisiz** katılmalı. Ardından **Üretim erişimine başvur**.
4. Üretim (Production) kanalında aynı `.aab` ile sürüm oluşturun › Türkiye (ve istediğiniz ülkeler) › İncelemeye gönder.

**Güncellemeler:** Her yeni sürümde `versionCode` artırılır (ben yaparım); yeni `.aab` dosyasını aynı kanala yüklemeniz yeterli.

---

## Sık red sebepleri ve önlemler

- **Erişilebilirlik videosu eksik/yetersiz** → 6. adımdaki sıralamayı birebir izleyin; açıklama metni okunabilir olsun.
- **Mağaza açıklaması ile uygulama uyuşmuyor** → Açıklamada reklam engelleme vaat etmeyin; Play sürümünde bu özellik yok.
- **Gizlilik politikası açılmıyor** → 1. adımı yaptığınızdan ve bağlantının tarayıcıda açıldığından emin olun.
