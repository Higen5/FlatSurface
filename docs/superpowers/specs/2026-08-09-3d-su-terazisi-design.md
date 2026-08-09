# 3D Su Terazisi + Pusula — Tasarım

Tarih: 2026-08-09
Durum: Onay bekliyor

## Amaç

Android telefonda çalışan, tek ekranlı bir su terazisi (dijital hizalayıcı) uygulaması.
Ekranın ortasında cam kubbe içinde bir kabarcık telefonun eğimini gösterir; kubbenin
çevresindeki halka pusula kadranı olarak döner. Amaç masa, raf, tablo gibi yüzeylerin
düz olup olmadığını ve bakılan yönü tek bakışta okumak.

Kullanıcı kendi telefonuna USB kablo ile kuracak.

## Yığın

- **Dil:** Kotlin 2.x. Projede `.java` dosyası yok. (JDK yalnızca derleme için gerekli,
  Android Studio kendi JBR'ını getirir.)
- **UI:** Jetpack Compose + Material 3.
- **Build:** Gradle Kotlin DSL + version catalog (`libs.versions.toml`) — yeni proje
  şablonunun varsayılanı.
- **SDK:** `compileSdk 36`, `targetSdk 36`, `minSdk 26`.
- **Üçüncü parti bağımlılık: yok.** Sensör Android framework'ünden, çizim Compose
  `Canvas`'tan geliyor.

### Neden Kotlin/Compose (Flutter veya React Native değil)

Makinede hiçbir geliştirme aracı kurulu değil, dolayısıyla kurulum yükü seçimin parçası:

| Seçenek | Kurulum | 3. parti bağımlılık |
|---|---|---|
| Kotlin + Compose | Yalnız Android Studio (JDK + SDK + adb dahil) | 0 |
| Flutter | Flutter SDK + Android SDK + JDK ayrı ayrı | `sensors_plus`, `flutter_compass` |
| React Native / Expo | Node + Expo + USB kurulum için yine Android SDK | `expo-sensors`, skia/svg |

Aynı sonuç için en az kurulum ve en az bağımlılık Kotlin tarafında. Ayrıca sensör
akışı 50 Hz civarında; JS köprüsü olmayan yol daha akıcı.

Değerlendirilip elenen: `DeviceOrientation` API ile tek dosyalık web sayfası — sıfır
kurulum, ama Android Chrome'da mutlak pusula yönü güvenilmez ve USB ile kurulum
gereksinimi karşılanmıyor.

## Ekran

Tek ekran, birleşik yerleşim. Koyu arka plan sabit (tema anahtarı yok).

```
        ┌─────────────────┐
        │    ╭─ K ─╮      │   Dış halka: pusula kadranı
        │  ╱  ·  ·  ╲     │   -azimut kadar döner, K/D/G/B + çentikler
        │ B    ◯     D    │
        │ │  ╭───╮   │    │   İç kubbe: radyal gradyan + iç gölge = cam etkisi
        │ │  │ ● │   │    │   ● kabarcık: (roll, pitch) → x,y; kubbe kenarına kırpılır
        │  ╲ ╰───╯  ╱     │      ±0,5° içinde yeşile döner
        │    ╰─ G ─╯      │
        │   X 1,2° Y -0,4°│   Altta sayısal eğim, üstte derece + yön (örn. 148° GD)
        └─────────────────┘
```

### Tasarım revizyonu (2026-08-09): gerçek 3D

İlk sürüm katmanlı 2D çizimle sahte derinlik veriyordu. Kullanıcı isteğiyle gerçek 3B
sahneye geçildi. Yöntem: **perspektif projeksiyon**, hesap Kotlin'de, çizim yine tek
`Canvas` üzerinde. OpenGL ES ve AGSL shader değerlendirilip elendi (biri belirgin
şekilde fazla kod, diğeri Android 13+ gerektirip ikinci bir çizim yolu doğuruyordu).

Sahne fiziksel olarak modellenir:

- **Pusula kadranı dünyaya sabittir.** Gerçek pusulalardaki yataylanan kadranın
  karşılığı: yatay düzlemde durur, kuzeye hizalıdır. Telefon eğildikçe perspektifte
  elipse döner, döndükçe kendi ekseninde döner.
- **Cam kubbe cihaza sabittir.** Alet gövdesine takılı vial gibi davranır, bu yüzden
  ekranda hep daire kalır; derinlik gradyan ve kenar gölgesiyle verilir.
- **Su yüzeyi dünya yatayındadır.** Yatay düzlemin küreyi kestiği çember, kubbenin
  içinde eğildikçe kayan ve yatan bir elips olarak görünür. Asıl 3B ipucu budur.
- **Kabarcık dünya yukarısı yönündedir.** Kubbe iç yüzeyinde `up` vektörü yönüne
  oturur; yani daima yükselen tarafa gider. Bu, işaret tahmini ihtiyacını ortadan
  kaldırır — davranış fizikten çıkar.

Gerçek eğim görsel olarak `gain` katsayısıyla büyütülür (gerçek vial'ın büyük eğrilik
yarıçapının karşılığı) ve `maxTilt` ile kubbe kenarında sınırlanır.

Fizikli sıvı çalkalanması kapsam dışı: su düzlemi her an tam yataydır, sönümlenen
salınım yoktur.

## Mimari

İki Kotlin dosyası. Katman, DI, ViewModel yok — tek ekranlı tek state.

### `MainActivity.kt`

- `ComponentActivity`, `setContent { ... }`, edge-to-edge.
- Tek `SensorEventListener`, tek sensör: `TYPE_ROTATION_VECTOR`.
  Bu sensör ivmeölçer + jiroskop + manyetometreyi cihaz tarafında birleştirir, yani
  eğim ve pusula yönü tek kaynaktan gelir. (Alternatif olan `ACCELEROMETER` +
  `MAGNETIC_FIELD` ikilisi iki dinleyici, elle füzyon ve daha fazla gürültü demekti.)
- `SensorManager.getRotationMatrix` → `remapCoordinateSystem` (ekran yönü için) →
  `getOrientation` → azimut, pitch, roll (radyan) → dereceye çevir.
- Yumuşatma: alçak geçiren filtre, `v += (yeni - v) * 0.15f`. Azimut için
  360°/0° sınırında geri sarmayan sürüm kullanılır.
- `onResume`/`onPause` içinde dinleyici kaydı/kaldırması. Örnekleme:
  `SENSOR_DELAY_GAME`.
- Ekran kapanmasın (`keepScreenOn`), dikey yön kilidi (manifest'te
  `screenOrientation="portrait"`).

### `Dial.kt`

Tek `@Composable fun Dial(azimuth: Float, pitch: Float, roll: Float)` — saf çizim,
state tutmaz, tüm değerleri parametre alır. İçinde tek `Canvas`.

Saf yardımcı fonksiyonlar (aynı dosyada, test edilebilir):

- `bubbleOffset(pitch, roll, radius, maxAngle): Offset` — açıyı piksel konumuna
  çevirir, kubbe yarıçapını aşarsa kenara kırpar.
- `smoothAngle(prev, next, alpha): Float` — 359° → 1° geçişini geri sarmadan yumuşatır.
- `isLevel(pitch, roll, tolerance = 0.5f): Boolean`

## Hata durumu

Tek gerçek hata yolu: cihazda `TYPE_ROTATION_VECTOR` sensörü yok
(`getDefaultSensor` `null` döner). Bu durumda ekranda "Bu cihazda gerekli sensör yok"
metni gösterilir, çizim yapılmaz. Sensör okuma yolu istisna fırlatmaz, try/catch yok.

## Test

Tek JVM unit testi (`src/test/`), framework kurulumu yok, sade `assert`'ler.
Kapsam yalnızca saf açı matematiği:

1. `bubbleOffset` — 0°/0° merkezdedir; büyük açı kubbe kenarında kırpılır.
2. `smoothAngle` — 359° → 1° arasında kısa yoldan geçer, 358° geriye sarmaz.
3. `isLevel` — tolerans sınırında doğru sonuç verir.

Compose çizimi ve sensör dinleyicisi test edilmez; ikisi de framework davranışı.

## Kurulum planı (geliştirme makinesi)

Mevcut durum: makinede yalnızca `git` ve `winget` var. Java, Node, Android SDK yok.

1. `winget install Google.AndroidStudio` — JDK (JBR), SDK Manager, `adb` ve emülatör
   tek pakette gelir.
2. İlk açılış sihirbazı: SDK Platform 36, build-tools, platform-tools indirilir (~7 GB).
3. `ANDROID_HOME` ortam değişkeni + PATH'e `platform-tools` (komut satırından `adb`
   kullanabilmek için).

Windows'un yerleşik USB sürücüsü çoğu cihazda yeterli; çalışmazsa SDK Manager'dan
Google USB Driver eklenir (yalnız gerekirse).

## Hedef cihaz

Redmi Note 13 Pro, Android 16, yapı numarası `BP2A.250605.031.A3`.

Bu yapı numarası AOSP/Pixel formatında; Xiaomi'nin kendi yapıları `OS2.0.x.x.XXXXXXX`
biçimindedir ve bu modele resmî Android 16 güncellemesi verilmedi. Dolayısıyla cihazda
büyük olasılıkla özel bir ROM (LineageOS/crDroid vb.) çalışıyor. Bu, Xiaomi'nin
"Güvenlik ayarları + Mi hesabı" zorunluluğunu ortadan kaldırır.

## Telefon tarafı (USB ile kurulum)

**Özel ROM (beklenen durum):**

1. Ayarlar → Telefon hakkında → **Yapı numarası**na 7 kez dokun.
2. Ayarlar → Sistem → **Geliştirici seçenekleri**ni aç.
3. **USB hata ayıklama**yı aç.
4. Kabloyu tak → telefondaki "USB hata ayıklamaya izin ver?" uyarısında
   "Bu bilgisayara her zaman izin ver" işaretle → İzin Ver.
5. USB bağlantı modunu **Dosya aktarımı (MTP)** yap; "yalnızca şarj" modunda bazı
   cihazlarda `adb` cihazı görmez.

**Stok HyperOS çıkarsa** (Geliştirici seçenekleri "Ek ayarlar" altındaysa):

- 1. adımda Yapı numarası yerine **OS sürümü**ne 7 kez dokunulur.
- USB hata ayıklamaya ek olarak "**USB üzerinden hata ayıklama (Güvenlik ayarları)**"
  da açılmalıdır; bu seçenek SIM kartlı bir Mi hesabı ile giriş ister, yoksa
  `adb install` reddedilir.

## Açık risk: manyetometre

Redmi Note 13 Pro'da manyetometre (e-pusula) bulunduğu doğrulanmadı; Xiaomi bu
segmentte sensörü bazen çıkarıyor. Manyetometre yoksa `TYPE_ROTATION_VECTOR` mutlak
yön veremez ve pusula halkası anlamsız kalır (eğim/kabarcık kısmı etkilenmez).

Doğrulama, araçlar kurulup telefon bağlandıktan sonra tek komutla yapılır:

```
adb shell dumpsys sensorservice | findstr /i "magnet rotation"
```

`Magnetic Field` ve `Rotation Vector` görünüyorsa tasarım aynen geçerlidir. Görünmezse
pusula kapsamdan çıkarılır. Bu ihtimal için şimdiden koda yedek yol yazılmaz.

## Kapsam dışı

- Kalibrasyon ("şu an düz kabul et") butonu — telefon kasası eğriyse sonradan eklenir.
- Dikey/şahmerdan modu (tek eksenli tüp terazi).
- Sekmeli navigasyon, ayar ekranı.
- Fizikli sıvı simülasyonu (çalkalanma, sönümlenen dalga).
- Play Store yayını, imzalı release APK.
- iOS.
