# Letterly Motion & Micro-Interaction Rules
*Physics-based feel untuk Android — terinspirasi Apple Design Awards*

> **Filosofi utama:** Setiap elemen punya *bobot*. Tombol terasa seperti bisa dipegang. Kartu terasa seperti kertas. Amplop terasa seperti bisa diangkat. Bukan animasi dekoratif — tapi respons fisik.

---

## 1. Prinsip Dasar

| Prinsip | Penjelasan |
|---|---|
| **Responsive, bukan dekoratif** | Animasi hanya muncul sebagai respons terhadap aksi user, bukan autoplay |
| **Physics over linear** | Selalu gunakan `spring()` atau `tween()` dengan easing — **jangan pernah** `LinearEasing` untuk UI |
| **Instant feedback** | Respons visual harus terasa dalam **< 16ms** (1 frame) setelah tap |
| **Consistent weight** | Elemen yang "lebih berat" secara visual bergerak lebih lambat dan memantul lebih sedikit |
| **Interruptible** | Semua animasi harus bisa di-interrupt — user bisa tap lagi sebelum animasi selesai |

---

## 2. Spring Presets

Gunakan konstanta ini di seluruh app. **Jangan hardcode nilai spring secara manual.**

```kotlin
// core/util/LetterlySpring.kt

object LetterlySpring {

    /**
     * SNAPPY — untuk tombol, chip, toggle kecil
     * Respons cepat, pantulan minimal
     */
    val Snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,  // 0.5f
        stiffness = Spring.StiffnessHigh,                 // 10_000f
    )

    /**
     * BOUNCY — untuk FAB, card tap, stamp
     * Efek "muncul" yang playful
     */
    val Bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,      // 0.2f
        stiffness = Spring.StiffnessMedium,               // 400f
    )

    /**
     * GENTLE — untuk modal, bottom sheet, page transition
     * Berasa seperti kertas diletakkan pelan-pelan
     */
    val Gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,   // 0.5f
        stiffness = Spring.StiffnessMediumLow,            // 200f
    )

    /**
     * STIFF — untuk error shake, confirmation pulse
     * Gerakan tegas tanpa pantulan
     */
    val Stiff = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,       // 1.0f
        stiffness = Spring.StiffnessHigh,
    )

    /**
     * ENVELOPE — khusus animasi amplop/kertas
     * Berat, lambat, terasa seperti kertas fisik
     */
    val Envelope = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 180f,
    )
}
```

---

## 3. Rules per Komponen

### 3.1 Tombol (LetterlyButton)

**Behavior:** Tombol "tenggelam" saat ditekan dan "muncul" saat dilepas. Pop-art shadow mengikuti press state.

```
IDLE    → PRESSED  : scale 1.0 → 0.94, shadow offset 3dp → 0dp  (Snappy)
PRESSED → RELEASED : scale 0.94 → 1.02 → 1.0              (Bouncy)
```

```kotlin
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.94f else 1.0f,
    animationSpec = if (isPressed) LetterlySpring.Snappy else LetterlySpring.Bouncy,
)
val shadowOffset by animateDpAsState(
    targetValue = if (isPressed) 0.dp else 3.dp,
    animationSpec = LetterlySpring.Snappy,
)

Box(modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
```

**Rule:** Jangan gunakan ripple. Tidak ada ripple di Letterly — semua feedback adalah scale + shadow.

---

### 3.2 Kartu Amplop (EnvelopeCard)

**Behavior:** Amplop terasa seperti diangkat dari tumpukan saat hover/long-press. Saat di-tap, sedikit miring sebelum navigate.

```
IDLE    → HOVER/FOCUS : scale 1.0 → 1.02, elevation naik  (Gentle)
TAP     : slight rotate -2deg → 0deg + scale pulse         (Snappy)
SWIPE   : follow finger dengan resistance 0.7x             (realtime)
```

```kotlin
val cardScale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1.0f,
    animationSpec = LetterlySpring.Snappy,
)
val cardRotation by animateFloatAsState(
    targetValue = if (isPressed) -1.5f else 0f,
    animationSpec = LetterlySpring.Snappy,
)

Modifier.graphicsLayer {
    scaleX = cardScale
    scaleY = cardScale
    rotationZ = cardRotation
}
```

---

### 3.3 FAB (Write Button)

**Behavior:** FAB adalah elemen paling playful. Bounce saat pertama muncul. Scale lebih dramatis saat ditekan.

```
ENTER   : scale 0.0 → 1.1 → 1.0  dengan delay 300ms setelah screen load  (Bouncy)
IDLE    → PRESSED : scale 1.0 → 0.88                                       (Snappy)
PRESSED → RELEASED : scale 0.88 → 1.12 → 1.0                              (Bouncy)
```

```kotlin
var isVisible by remember { mutableStateOf(false) }
LaunchedEffect(Unit) {
    delay(300)
    isVisible = true
}
val fabScale by animateFloatAsState(
    targetValue = if (isVisible) 1f else 0f,
    animationSpec = LetterlySpring.Bouncy,
)
```

---

### 3.4 Bottom Sheet & Modal

**Behavior:** Muncul dari bawah seperti kertas diletakkan. Dismiss dengan drag atau tap backdrop.

```
ENTER   : translateY 100% → 0%    (Gentle, 380ms)
EXIT    : translateY 0% → 100%    (Stiff, 250ms — lebih cepat dari enter)
DRAG    : follow finger 1:1, dengan resistance mulai di 80% threshold
SNAP    : jika drag > 40% height → dismiss; jika tidak → snap back (Bouncy)
```

**Rule:** Sheet masuk lebih lambat dari keluar. Ini memberi kesan "berat" saat datang, "ringan" saat pergi.

```kotlin
val sheetOffset by animateFloatAsState(
    targetValue = if (isVisible) 0f else 1f,
    animationSpec = if (isVisible) LetterlySpring.Gentle else LetterlySpring.Stiff,
)
```

---

### 3.5 Stamp (Pilo / Stamp Collection)

**Behavior:** Stamp punya bounce paling dramatis karena ini elemen koleksi yang ingin terasa rewarding.

```
TAP/SELECT  : scale 1.0 → 1.3 → 1.0  (Bouncy, stiffness lebih rendah)
COLLECT NEW : scale 0 → 1.4 → 1.0  dengan particle burst  (Bouncy)
HOVER       : slight wobble ±3deg, bergantian              (loop, Gentle)
```

```kotlin
val stampScale by animateFloatAsState(
    targetValue = if (isSelected) 1.25f else 1.0f,
    animationSpec = spring(
        dampingRatio = 0.3f,  // lebih bouncy dari preset
        stiffness = 300f,
    ),
)
```

---

### 3.6 Surat Terbuka (OpenLetter Screen)

**Behavior:** Surat "terbuka" seperti amplop fisik. Konten surat masuk dengan stagger per baris.

```
AMPLOP → SURAT : amplop scale down + fade, surat scale up dari center (Envelope, 500ms)
KONTEN SURAT   : Dear line → body text → signature, stagger 80ms per elemen
CLOSE / BACK   : surat fold kembali ke amplop (reverse, Gentle)
```

```kotlin
// Stagger konten surat
LaunchedEffect(isLetterOpen) {
    if (isLetterOpen) {
        delay(100); showDearLine = true
        delay(80);  showBody = true
        delay(80);  showSignature = true
        delay(80);  showStamp = true
    }
}

val letterScale by animateFloatAsState(
    targetValue = if (isLetterOpen) 1f else 0.85f,
    animationSpec = LetterlySpring.Envelope,
)
val letterAlpha by animateFloatAsState(
    targetValue = if (isLetterOpen) 1f else 0f,
    animationSpec = tween(300, easing = EaseOutCubic),
)
```

---

### 3.7 Font Picker & Template Picker

**Behavior:** Item dalam picker muncul dengan stagger. Item yang dipilih "lock in" dengan scale pulse.

```
ENTER LIST ITEM : translateX -20dp → 0dp + fadeIn, stagger 40ms per item  (Snappy)
SELECT ITEM     : scale 1.0 → 1.05 → 1.0 + border animates in            (Bouncy)
DESELECT ITEM   : scale 1.05 → 1.0                                        (Snappy)
```

```kotlin
items.forEachIndexed { index, item ->
    val itemVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 40L)
        itemVisible = true
    }
    val offsetX by animateDpAsState(
        targetValue = if (itemVisible) 0.dp else (-20).dp,
        animationSpec = LetterlySpring.Snappy,
    )
    val alpha by animateFloatAsState(
        targetValue = if (itemVisible) 1f else 0f,
        animationSpec = tween(200),
    )
}
```

---

### 3.8 Navigation Tab

**Behavior:** Active indicator "meluncur" antar tab dengan spring. Icon active scale up.

```
TAB CHANGE : indicator translateX antar posisi    (Snappy)
ICON       : inactive scale 1.0 → active scale 1.2 (Bouncy)
```

```kotlin
val indicatorOffset by animateDpAsState(
    targetValue = activeTabOffset,
    animationSpec = LetterlySpring.Snappy,
)
val iconScale by animateFloatAsState(
    targetValue = if (isActive) 1.2f else 1.0f,
    animationSpec = LetterlySpring.Bouncy,
)
```

---

### 3.9 Error / Shake

**Behavior:** Input field atau elemen yang error "menggeleng" seperti kepala bilang tidak.

```
ERROR : translateX  0 → -8dp → 8dp → -6dp → 6dp → -4dp → 4dp → 0  (Stiff, 400ms total)
```

```kotlin
val shakeOffset = remember { Animatable(0f) }

suspend fun triggerShake() {
    shakeOffset.animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
            durationMillis = 400
            (-8f) at 50  using LetterlySpring.Stiff
            8f   at 100 using LetterlySpring.Stiff
            (-6f) at 150 using LetterlySpring.Stiff
            6f   at 200 using LetterlySpring.Stiff
            (-4f) at 250 using LetterlySpring.Stiff
            4f   at 300 using LetterlySpring.Stiff
            0f   at 400
        }
    )
}

Modifier.offset(x = shakeOffset.value.dp)
```

---

### 3.10 Pull to Refresh / Mood Delivery Banner

**Behavior:** Banner pagi/malam masuk dari atas seperti tirai turun. Mood icon berotasi saat loading.

```
BANNER ENTER : translateY -100% → 0%   (Gentle, 500ms delay setelah login)
MOOD ICON    : rotate 0 → 360deg loop  saat loading letters (linear, 1200ms)
```

```kotlin
val bannerOffset by animateFloatAsState(
    targetValue = if (isBannerVisible) 0f else -1f,
    animationSpec = LetterlySpring.Gentle,
)
val iconRotation by animateFloatAsState(
    targetValue = if (isLoading) 360f else 0f,
    animationSpec = if (isLoading) infiniteRepeatable(
        tween(1200, easing = LinearEasing), RepeatMode.Restart
    ) else LetterlySpring.Snappy,
)
```

---

## 4. Hal yang Dilarang

| ❌ Jangan | ✅ Gantinya |
|---|---|
| `LinearEasing` untuk UI interaksi | Spring atau `EaseOutCubic` |
| Material `ripple` / `indication` default | Scale + shadow shift |
| Durasi > 600ms untuk respons tap | Max 350ms untuk feedback langsung |
| Animasi yang tidak bisa di-interrupt | Semua pakai `animateXxxAsState` (auto-interruptible) |
| `AnimationConstants.DefaultDurationMillis` (300ms linear) | Spring preset dari `LetterlySpring` |
| Animasi yang sama untuk semua komponen | Bobot berbeda = spring berbeda |
| Alpha fade saja untuk enter/exit | Selalu kombinasikan alpha + translate/scale |

---

## 5. Reduced Motion

Selalu hormati `LocalReduceMotion`. User yang punya motion sensitivity tidak boleh diabaikan.

```kotlin
// core/util/MotionUtils.kt

@Composable
fun rememberIsReducedMotion(): Boolean {
    return LocalReduceMotion.current.enabled
}

// Penggunaan:
val isReducedMotion = rememberIsReducedMotion()

val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.94f else 1.0f,
    animationSpec = if (isReducedMotion) snap() else LetterlySpring.Bouncy,
)
```

**Rule:** Jika reduced motion aktif, ganti spring dengan `snap()` — elemen masih berubah state, tapi tanpa animasi.

> **Catatan implementasi:** Di codebase ini, `rememberIsReducedMotion()` (lihat `core/util/MotionUtils.kt`) dibaca dari system setting `Settings.Global.ANIMATOR_DURATION_SCALE` alih-alih `LocalReduceMotion`, karena signature `LocalReduceMotion` bisa berubah antar versi Compose UI — pendekatan ini stabil lintas versi untuk preferensi yang sama.

---

## 6. Referensi Timing Cepat

| Aksi | Durasi | Spring |
|---|---|---|
| Tap feedback | < 100ms | Snappy |
| Button press/release | 150–200ms | Bouncy |
| Card tap | 200ms | Snappy |
| Sheet masuk | 350–400ms | Gentle |
| Sheet keluar | 200–250ms | Stiff |
| Screen transition | 300–380ms | Gentle |
| Stamp collect | 400–500ms | Bouncy |
| Error shake | 400ms | Stiff (keyframes) |
| Stagger per item | 40–80ms | Snappy |
| FAB entrance | 300ms delay + 350ms anim | Bouncy |

---

*Letterly Motion Rules v1.0 · Jetpack Compose · Physics-based Android UI*
