# 3D Su Terazisi + Pusula — Uygulama Planı

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redmi Note 13 Pro üzerinde çalışan, tek ekranda cam kubbeli su terazisi ve dönen pusula kadranı gösteren Android uygulaması.

**Architecture:** Tek `Activity`, tek sensör dinleyicisi (`TYPE_ROTATION_VECTOR`), tek `Canvas` çizimi. Açı matematiği Compose'dan bağımsız saf fonksiyonlarda tutulur, böylece JVM unit testi ile doğrulanabilir. Katman, DI, ViewModel, navigasyon yok.

**Tech Stack:** Kotlin 2.x, Jetpack Compose (Material 3'ten yalnızca `Text`), Gradle Kotlin DSL + version catalog, Android SDK 36. Üçüncü parti bağımlılık yok.

## Global Constraints

- Dil yalnızca **Kotlin**. Projede `.java` kaynak dosyası bulunmayacak.
- **Üçüncü parti bağımlılık eklenmeyecek.** Yeni proje şablonunun getirdiği AndroidX/Compose bağımlılıkları dışında `build.gradle.kts` dosyasına satır eklenmez.
- `compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`. (Spec 36 diyordu; sihirbaz AGP 9.3.1 ile 37 üretti, şablonla kavga edilmedi.)
- Sihirbazın ürettiği sürümler: AGP 9.3.1, Kotlin 2.2.10, Compose BOM 2026.02.01. Bu değerler elle değiştirilmez.
- Paket adı: `com.ahmet.suterazisi`. Uygulama adı: **Su Terazisi**.
- Arayüz metinleri Türkçe.
- Dikey yön kilidi (manifest), ekran açık kalır (`FLAG_KEEP_SCREEN_ON`).
- Ponytail aktif: en yalın çalışan çözüm. Şablonun ürettiği kullanılmayan dosyalar silinir.
- Kapsam dışı (spec'ten): kalibrasyon butonu, şahmerdan modu, sekmeler, ayar ekranı, gerçek 3D motoru, fizikli sıvı, release imzalama, iOS.

## Dosya Yapısı

| Dosya | Sorumluluk |
|---|---|
| `app/src/main/java/com/ahmet/suterazisi/Angles.kt` | Saf açı matematiği. Android/Compose tipi kullanmaz, dolayısıyla düz JVM testinden çağrılabilir. |
| `app/src/main/java/com/ahmet/suterazisi/Dial.kt` | Tek `Canvas` çizimi: pusula halkası, cam kubbe, kabarcık. State tutmaz, tüm değerleri parametre alır. |
| `app/src/main/java/com/ahmet/suterazisi/MainActivity.kt` | Activity yaşam döngüsü, sensör dinleyicisi, state, ekran yerleşimi. |
| `app/src/test/java/com/ahmet/suterazisi/AnglesTest.kt` | `Angles.kt` için tek unit test dosyası. |
| `app/src/main/AndroidManifest.xml` | Dikey kilit, uygulama adı. |

Spec "iki dosya" diyordu; `Angles.kt` üçüncü dosya olarak ayrıldı çünkü saf fonksiyonlar `Dial.kt` içinde kalsaydı test JVM'de Compose sınıflarını yüklemeye çalışırdı. Ayrım maliyeti bir dosya, karşılığı çalışan bir test.

Şablonun ürettiği `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt` silinir — uygulama sabit koyu renkler kullanıyor, tema altyapısı kullanılmıyor.

---

### Task 1: Geliştirme ortamı ve cihaz doğrulama

**Durum: TAMAMLANDI (2026-08-09).** Android Studio 2026.1.3.7 kuruldu, SDK indi, `adb` cihazı `device` olarak görüyor. Sensör kontrolü geçti: `qmc6308` (manyetometre), `lsm6dso_acc`, `lsm6dso_gyro` ve AOSP `Rotation Vector Sensor` (type 11) mevcut. Cihaz `23117RA68G` / Android 16 / `BP2A.250605.031.A3` — AOSP tabanlı özel ROM, Mi hesabı gerekmiyor.

Kod yok. Çıktısı: `adb` telefonu görüyor ve gereken sensörlerin varlığı kanıtlanmış oluyor. Manyetometre çıkmazsa pusula kapsamdan çıkar, bu yüzden bu görev diğer her şeyden önce gelir.

**Files:** yok (ortam kurulumu)

**Interfaces:**
- Consumes: —
- Produces: Çalışan `adb`; `Rotation Vector` ve `Magnetic Field` sensörlerinin var/yok bilgisi.

- [ ] **Step 1: Android Studio ilk açılış sihirbazını tamamla**

Başlat menüsünden Android Studio'yu aç. Sihirbazda **Standard** kurulumu seç ve lisansları kabul et. Bu adım SDK Platform, build-tools ve platform-tools'u indirir (~7 GB).

- [ ] **Step 2: SDK'nın indiğini doğrula**

```powershell
Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
```
Beklenen: `True`. `False` ise Android Studio → Settings → Languages & Frameworks → Android SDK → SDK Tools sekmesinden **Android SDK Platform-Tools**'u işaretleyip uygula.

- [ ] **Step 3: ANDROID_HOME ve PATH ayarla**

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdk, "User")
$p = [Environment]::GetEnvironmentVariable("Path", "User")
if ($p -notlike "*platform-tools*") { [Environment]::SetEnvironmentVariable("Path", "$p;$sdk\platform-tools", "User") }
```
Ardından **yeni bir terminal aç** (mevcut oturum eski PATH'i taşır).

- [ ] **Step 4: Telefonda geliştirici seçeneklerini aç**

1. Ayarlar → Telefon hakkında → **Yapı numarası**na 7 kez dokun.
2. Ayarlar → Sistem → **Geliştirici seçenekleri**.
3. **USB hata ayıklama** → aç.
4. Kabloyu tak → "USB hata ayıklamaya izin ver?" → **"Bu bilgisayara her zaman izin ver"** işaretle → İzin Ver.
5. Bildirim panelinden USB modunu **Dosya aktarımı (MTP)** yap.

Geliştirici seçenekleri "Sistem" yerine **"Ek ayarlar"** altındaysa cihaz stok HyperOS çalıştırıyor demektir; o durumda 1. adımda Yapı numarası yerine **OS sürümü**ne dokunulur ve listede ayrıca "**USB üzerinden hata ayıklama (Güvenlik ayarları)**" açılmalıdır (Mi hesabı ister).

- [ ] **Step 5: Bağlantıyı doğrula**

```powershell
adb devices
```
Beklenen: cihaz seri numarası ve yanında `device`. `unauthorized` yazıyorsa telefondaki izin penceresi onaylanmamıştır. Liste boşsa USB modunu ve kabloyu (bazı kablolar yalnızca şarj eder) kontrol et.

- [ ] **Step 6: Sensörleri doğrula — bu görevin asıl çıktısı**

```powershell
adb shell dumpsys sensorservice | Select-String -Pattern "Rotation Vector", "Magnetic"
```
Beklenen: her ikisi de listede. **`Magnetic Field` çıkmazsa dur ve bildir** — cihazda manyetometre yok, pusula halkası uygulanamaz, plan revize edilmelidir. `Rotation Vector` çıkmazsa uygulama hiç çalışmaz.

---

### Task 2: Proje iskeleti ve telefonda çalışan boş uygulama

**Files:**
- Create: Android Studio sihirbazı ile `app/` modülü ve Gradle dosyaları
- Modify: `app/src/main/AndroidManifest.xml`
- Delete: `app/src/main/java/com/ahmet/suterazisi/ui/theme/` (3 dosya)

**Interfaces:**
- Consumes: Task 1'den çalışan SDK ve `adb`
- Produces: `com.ahmet.suterazisi.MainActivity`; telefonda kurulabilen çalışır bir APK

**Not (2026-08-09):** Sihirbaz `G:\Projects\Mobile app test` klasörünü boş olmadığı için reddetti; proje `G:\Projects\suterazisi-tmp` içinde oluşturulup dosyalar depoya taşındı. `suterazisi-tmp` yedek olarak duruyor, Step 5 doğrulandıktan sonra silinebilir.

- [x] **Step 1: Projeyi oluştur**

Android Studio → **New Project** → **Empty Activity** (Compose şablonu). Ayarlar:

| Alan | Değer |
|---|---|
| Name | `Su Terazisi` |
| Package name | `com.ahmet.suterazisi` |
| Save location | `G:\Projects\Mobile app test` |
| Language | Kotlin |
| Minimum SDK | API 26 ("Android 8.0") |
| Build configuration language | Kotlin DSL + Gradle Version Catalogs |

Klasörde zaten `.git` ve `docs/` var; sihirbaz bunları silmez, yanına ekler. Sihirbaz kendi `.gitignore` dosyasını oluşturur.

- [ ] **Step 2: İlk Gradle senkronizasyonunu bekle ve derle**

```bash
./gradlew assembleDebug
```
Beklenen: `BUILD SUCCESSFUL`. İlk çalıştırma Gradle dağıtımını indirdiği için birkaç dakika sürer.

- [ ] **Step 3: Manifest'i düzenle**

`app/src/main/AndroidManifest.xml` içinde `<activity android:name=".MainActivity"` etiketine şu iki özniteliği ekle:

```xml
android:screenOrientation="portrait"
android:configChanges="orientation|screenSize|keyboardHidden"
```

- [ ] **Step 4: Kullanılmayan tema dosyalarını sil**

```bash
rm -r "app/src/main/java/com/ahmet/suterazisi/ui"
```
`MainActivity.kt` içindeki `SuTeraziTheme { ... }` sarmalayıcısını ve `import com.ahmet.suterazisi.ui.theme.*` satırlarını kaldır; içindeki `Scaffold`/`Greeting` gövdesini şimdilik tek satır bırak:

```kotlin
setContent {
    Text("kuruldu")
}
```
`androidx.compose.material3.Text` import edilmeli.

- [ ] **Step 5: Telefonda çalıştır**

Android Studio'da cihazı seçip **Run** (Shift+F10). Beklenen: telefonda uygulama açılır ve "kuruldu" yazar. Bu, tüm zincirin (derleme → imzalama → `adb install` → başlatma) çalıştığının kanıtıdır.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: Android proje iskeleti, telefonda calisan bos uygulama"
```

---

### Task 3: Açı matematiği (TDD)

**Files:**
- Create: `app/src/main/java/com/ahmet/suterazisi/Angles.kt`
- Test: `app/src/test/java/com/ahmet/suterazisi/AnglesTest.kt`

**Interfaces:**
- Consumes: —
- Produces:
  - `fun angleDelta(from: Float, to: Float): Float` — `-180..180`
  - `fun smoothAngle(prev: Float, next: Float, alpha: Float = 0.15f): Float` — sonuç `0..360`
  - `fun smooth(prev: Float, next: Float, alpha: Float = 0.15f): Float`
  - `fun bubbleOffset(pitchDeg: Float, rollDeg: Float, radius: Float, maxAngle: Float = 30f): Pair<Float, Float>`
  - `fun isLevel(pitchDeg: Float, rollDeg: Float, tolerance: Float = 0.5f): Boolean`
  - `fun headingLabel(azimuthDeg: Float): String`

- [ ] **Step 1: Başarısız testi yaz**

`app/src/test/java/com/ahmet/suterazisi/AnglesTest.kt`:

```kotlin
package com.ahmet.suterazisi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnglesTest {

    @Test
    fun `angleDelta kisa yoldan gider`() {
        assertEquals(2f, angleDelta(359f, 1f), 0.001f)
        assertEquals(-2f, angleDelta(1f, 359f), 0.001f)
        assertEquals(90f, angleDelta(0f, 90f), 0.001f)
    }

    @Test
    fun `smoothAngle 360 sinirinda geri sarmaz`() {
        // 359 -> 1 arasi ileri 2 derece; alpha 0.5 ile tam ortasi = 0 derece
        assertEquals(0f, smoothAngle(359f, 1f, 0.5f), 0.001f)
        // sonuc her zaman 0..360 araliginda
        val r = smoothAngle(1f, 359f, 0.5f)
        assertTrue("0..360 disinda: $r", r in 0f..360f)
        assertEquals(0f, r, 0.001f)
    }

    @Test
    fun `bubbleOffset merkez ve kirpma`() {
        assertEquals(0f to 0f, bubbleOffset(0f, 0f, 100f))
        // 90 derece egim yaricapi cok asar, kenara kirpilmali
        val (x, y) = bubbleOffset(90f, 0f, 100f, 30f)
        assertEquals(0f, x, 0.001f)
        assertEquals(100f, y, 0.001f)
        // capraz egimde de yaricap asilmaz
        val (dx, dy) = bubbleOffset(90f, 90f, 100f, 30f)
        assertEquals(100f, Math.hypot(dx.toDouble(), dy.toDouble()).toFloat(), 0.01f)
    }

    @Test
    fun `isLevel tolerans sinirinda`() {
        assertTrue(isLevel(0.4f, -0.3f))
        assertFalse(isLevel(0.6f, 0f))
        assertFalse(isLevel(0f, -0.6f))
    }

    @Test
    fun `headingLabel sekiz yon`() {
        assertEquals("K", headingLabel(0f))
        assertEquals("K", headingLabel(350f))
        assertEquals("KD", headingLabel(45f))
        assertEquals("D", headingLabel(90f))
        assertEquals("G", headingLabel(180f))
        assertEquals("B", headingLabel(270f))
    }
}
```

- [ ] **Step 2: Testi çalıştır, başarısız olduğunu gör**

```bash
./gradlew :app:testDebugUnitTest --tests "com.ahmet.suterazisi.AnglesTest"
```
Beklenen: derleme hatası — `Unresolved reference: angleDelta`.

- [ ] **Step 3: Asgari uygulamayı yaz**

`app/src/main/java/com/ahmet/suterazisi/Angles.kt`:

```kotlin
package com.ahmet.suterazisi

import kotlin.math.abs
import kotlin.math.hypot

/** İki açı arasındaki en kısa farkı -180..180 aralığında verir. */
fun angleDelta(from: Float, to: Float): Float {
    var d = (to - from) % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}

/** Pusula açısı için alçak geçiren filtre. 360/0 sınırında geri sarmaz, sonuç 0..360. */
fun smoothAngle(prev: Float, next: Float, alpha: Float = 0.15f): Float {
    val r = prev + angleDelta(prev, next) * alpha
    return (r % 360f + 360f) % 360f
}

/** Eğim açıları için düz alçak geçiren filtre. */
fun smooth(prev: Float, next: Float, alpha: Float = 0.15f): Float = prev + (next - prev) * alpha

/**
 * Eğimi kubbe içindeki (x, y) piksel kaymasına çevirir.
 * y yukarı yönde pozitiftir; çizim tarafında ekran koordinatına çevrilir.
 * maxAngle: kabarcığın kubbe kenarına dayandığı eğim.
 */
fun bubbleOffset(
    pitchDeg: Float,
    rollDeg: Float,
    radius: Float,
    maxAngle: Float = 30f
): Pair<Float, Float> {
    var x = rollDeg / maxAngle * radius
    var y = pitchDeg / maxAngle * radius
    val d = hypot(x, y)
    if (d > radius) {
        x = x / d * radius
        y = y / d * radius
    }
    return x to y
}

fun isLevel(pitchDeg: Float, rollDeg: Float, tolerance: Float = 0.5f): Boolean =
    abs(pitchDeg) <= tolerance && abs(rollDeg) <= tolerance

/** 0..360 dereceyi sekiz yönlü Türkçe kısaltmaya çevirir. */
fun headingLabel(azimuthDeg: Float): String {
    val names = arrayOf("K", "KD", "D", "GD", "G", "GB", "B", "KB")
    val norm = (azimuthDeg % 360f + 360f) % 360f
    return names[((norm / 45f + 0.5f).toInt()) % 8]
}
```

- [ ] **Step 4: Testi çalıştır, geçtiğini gör**

```bash
./gradlew :app:testDebugUnitTest --tests "com.ahmet.suterazisi.AnglesTest"
```
Beklenen: `BUILD SUCCESSFUL`, 5 test geçer.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ahmet/suterazisi/Angles.kt app/src/test/java/com/ahmet/suterazisi/AnglesTest.kt
git commit -m "feat: aci matematigi ve unit testleri"
```

---

### Task 4: Sensör bağlantısı ve ham değerlerin ekranda gösterimi

Çizimden önce sensörün doğru veri ürettiğini telefonda gözle doğrulamak için ara adım. Bu görevin sonunda ekranda üç sayı akıyor olacak.

**Files:**
- Modify: `app/src/main/java/com/ahmet/suterazisi/MainActivity.kt` (Task 2'den kalan tüm gövde değişir)

**Interfaces:**
- Consumes: `smoothAngle`, `smooth`, `isLevel`, `headingLabel` (Task 3)
- Produces: `MainActivity` içinde `azimuth`, `pitch`, `roll` (hepsi `Float`, derece); Task 5 bunları `Dial`'a geçirir

Spec `remapCoordinateSystem` çağrısından söz ediyordu; ekran dikey kilitli olduğu için gerekmiyor ve plandan çıkarıldı. Yatay mod ileride açılırsa geri eklenmelidir.

- [ ] **Step 1: MainActivity'yi yaz**

`app/src/main/java/com/ahmet/suterazisi/MainActivity.kt` içeriğini tamamen şununla değiştir:

```kotlin
package com.ahmet.suterazisi

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensors: SensorManager
    private var rotationSensor: Sensor? = null

    private var azimuth by mutableFloatStateOf(0f)
    private var pitch by mutableFloatStateOf(0f)
    private var roll by mutableFloatStateOf(0f)

    private val rm = FloatArray(9)
    private val orient = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sensors = getSystemService(SensorManager::class.java)
        rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF06080B))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (rotationSensor == null) {
                    Text(
                        "Bu cihazda gerekli sensör yok",
                        color = Color(0xFFFF6B6B),
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        "${azimuth.roundToInt()}°  ${headingLabel(azimuth)}",
                        color = Color(0xFFD9B863),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "X %.1f°   Y %.1f°".format(roll, pitch),
                        color = if (isLevel(pitch, roll)) Color(0xFF3ED87F) else Color(0xFF9AA6B2),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let {
            sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensors.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rm, event.values)
        SensorManager.getOrientation(rm, orient)
        azimuth = smoothAngle(azimuth, Math.toDegrees(orient[0].toDouble()).toFloat())
        pitch = smooth(pitch, Math.toDegrees(orient[1].toDouble()).toFloat())
        roll = smooth(roll, Math.toDegrees(orient[2].toDouble()).toFloat())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
```

- [ ] **Step 2: Telefonda çalıştır ve elle doğrula**

Run (Shift+F10). Telefonu elinde tutarak sırayla kontrol et:

| Hareket | Beklenen |
|---|---|
| Telefonu düz masaya koy | X ve Y ≈ 0,0°, yazı yeşile döner |
| Kuzeye çevir | Üstteki derece ≈ 0°, etiket **K** |
| Doğuya çevir | ≈ 90°, etiket **D** |
| Sağ kenarı kaldır | X değeri işaret değiştirir |
| Üst kenarı kaldır | Y değeri işaret değiştirir |
| Yavaşça döndür | Sayılar akıcı değişir, zıplamaz |

Kuzey yönü 90° veya 180° kayık geliyorsa telefonu havada 8 çizerek manyetometreyi kalibre et; hâlâ kayıksa yakında mıknatıs/metal olup olmadığını kontrol et.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ahmet/suterazisi/MainActivity.kt
git commit -m "feat: rotation vector sensoru ve sayisal aci gosterimi"
```

---

### Task 5: Kubbe, kabarcık ve pusula kadranı çizimi

**Files:**
- Create: `app/src/main/java/com/ahmet/suterazisi/Dial.kt`
- Modify: `app/src/main/java/com/ahmet/suterazisi/MainActivity.kt` (yalnızca `setContent` gövdesine `Dial` eklenir)

**Interfaces:**
- Consumes: `bubbleOffset`, `isLevel` (Task 3); `azimuth`, `pitch`, `roll` (Task 4)
- Produces: `@Composable fun Dial(azimuth: Float, pitch: Float, roll: Float, modifier: Modifier = Modifier)`

- [ ] **Step 1: Dial.kt'yi yaz**

`app/src/main/java/com/ahmet/suterazisi/Dial.kt`:

```kotlin
package com.ahmet.suterazisi

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val Gold = Color(0xFFD9B863)
private val North = Color(0xFFFF6B6B)
private val Glass = Color(0xFF11151C)
private val LevelGreen = Color(0xFF3ED87F)
private val BubbleBlue = Color(0xFF7BC0FF)

@Composable
fun Dial(azimuth: Float, pitch: Float, roll: Float, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = minOf(size.width, size.height) / 2f - 6.dp.toPx()
        val domeR = outer * 0.60f
        val bubbleR = 18.dp.toPx()
        val tint = if (isLevel(pitch, roll)) LevelGreen else BubbleBlue

        drawRing(c, outer, azimuth, measurer)
        drawDome(c, domeR)
        drawTarget(c, bubbleR + 5.dp.toPx(), tint)

        val (bx, by) = bubbleOffset(pitch, roll, domeR - bubbleR)
        // y yukari pozitif; ekran koordinatinda asagi pozitif oldugu icin isaret cevriliyor
        drawBubble(Offset(c.x + bx, c.y - by), bubbleR, tint)
    }
}

private fun DrawScope.drawRing(c: Offset, r: Float, azimuth: Float, m: TextMeasurer) {
    drawCircle(Color(0xFF0C0F14), r, c)
    drawCircle(Gold.copy(alpha = 0.30f), r, c, style = Stroke(1.5.dp.toPx()))

    rotate(-azimuth, c) {
        for (deg in 0 until 360 step 15) {
            val major = deg % 45 == 0
            val a = Math.toRadians(deg.toDouble() - 90.0)
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            val len = if (major) 13.dp.toPx() else 6.dp.toPx()
            drawLine(
                color = if (major) Gold else Gold.copy(alpha = 0.35f),
                start = Offset(c.x + ca * r, c.y + sa * r),
                end = Offset(c.x + ca * (r - len), c.y + sa * (r - len)),
                strokeWidth = if (major) 2.5.dp.toPx() else 1.dp.toPx()
            )
        }

        val labelR = r - 32.dp.toPx()
        listOf(0 to "K", 90 to "D", 180 to "G", 270 to "B").forEach { (deg, txt) ->
            val a = Math.toRadians(deg.toDouble() - 90.0)
            val layout = m.measure(
                txt,
                TextStyle(
                    color = if (deg == 0) North else Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                layout,
                topLeft = Offset(
                    c.x + cos(a).toFloat() * labelR - layout.size.width / 2f,
                    c.y + sin(a).toFloat() * labelR - layout.size.height / 2f
                )
            )
        }
    }
}

private fun DrawScope.drawDome(c: Offset, r: Float) {
    drawCircle(Glass, r, c)
    // sol ustten gelen isik: camin hacim hissi
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(c.x - r * 0.35f, c.y - r * 0.35f),
            radius = r * 1.1f
        ),
        radius = r,
        center = c
    )
    // ic golge halkasi: kubbe kenarinin derinligi
    drawCircle(Color.Black.copy(alpha = 0.55f), r, c, style = Stroke(6.dp.toPx()))
    drawCircle(Gold.copy(alpha = 0.25f), r, c, style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawTarget(c: Offset, r: Float, tint: Color) {
    drawCircle(tint.copy(alpha = 0.55f), r, c, style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.drawBubble(p: Offset, r: Float, tint: Color) {
    drawCircle(
        Color.Black.copy(alpha = 0.45f),
        r,
        Offset(p.x + 2.dp.toPx(), p.y + 3.dp.toPx())
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.90f), tint, tint.copy(alpha = 0.65f)),
            center = Offset(p.x - r * 0.35f, p.y - r * 0.35f),
            radius = r * 1.6f
        ),
        radius = r,
        center = p
    )
    // spekuler nokta: cam kabarcik hissinin tamamlayicisi
    drawCircle(
        Color.White.copy(alpha = 0.85f),
        r * 0.18f,
        Offset(p.x - r * 0.38f, p.y - r * 0.40f)
    )
}
```

`rememberTextMeasurer` veya `drawText` için derleyici deneysel API uyarısı verirse `Dial` fonksiyonunun üstüne `@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)` ekle.

- [ ] **Step 2: MainActivity'ye bağla**

`MainActivity.kt` içinde, iki `Text` çağrısının **arasına** `Dial`'ı ekle:

```kotlin
Dial(
    azimuth = azimuth,
    pitch = pitch,
    roll = roll,
    modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .padding(vertical = 20.dp)
)
```

Şu iki import satırını ekle:

```kotlin
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
```

- [ ] **Step 3: Derle ve testlerin hâlâ geçtiğini doğrula**

```bash
./gradlew :app:testDebugUnitTest assembleDebug
```
Beklenen: `BUILD SUCCESSFUL`, 5 test geçer.

- [ ] **Step 4: Telefonda çalıştır ve görsel doğrulama yap**

| Hareket | Beklenen |
|---|---|
| Düz masaya koy | Kabarcık tam merkezde, yeşil, hedef çemberinin içinde |
| Sağ kenarı kaldır | Kabarcık **sağa** kayar |
| Üst kenarı kaldır | Kabarcık **yukarı** kayar |
| Telefonu döndür | Halka ters yönde döner, **K** hep kuzeyi gösterir |
| Dik tut | Kabarcık kubbe kenarına dayanır, dışarı taşmaz |

Kabarcık ters yönde gidiyorsa `Dial.kt`'de `bubbleOffset(pitch, roll, ...)` çağrısındaki argümanların işaretini çevir (`-pitch`, `-roll`). Gerçek su terazisinde kabarcık **yükselen** tarafa gider; hangi kuralı tercih ettiğine karar verip sabitle.

Kabarcık hareketi çok hızlı/hassas geliyorsa `bubbleOffset`'in `maxAngle` varsayılanını 30f'ten büyüt (daha sakin) veya küçült (daha hassas).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: cam kubbe, kabarcik ve donen pusula kadrani cizimi"
```

---

## Bitiş kontrolü

Plan tamamlandığında şunlar doğrulanmış olmalı:

- [ ] `./gradlew :app:testDebugUnitTest` → 5 test geçiyor
- [ ] `./gradlew assembleDebug` → `BUILD SUCCESSFUL`
- [ ] Uygulama telefonda açılıyor, ekran uykuya geçmiyor, yatay dönmüyor
- [ ] Düz yüzeyde kabarcık merkezde ve yeşil
- [ ] Pusula halkası doğru yönü gösteriyor
- [ ] Projede `.java` dosyası yok: `git ls-files "*.java"` boş çıktı veriyor
- [ ] `app/build.gradle.kts` içindeki `dependencies` bloğuna elle satır eklenmemiş
