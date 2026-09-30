# iOS Auth & Onboarding Flow — Unsealed (Internal)

Dokumen ini menjelaskan **urutan lengkap** dari Login/Sign-Up hingga masuk ke tab utama, termasuk di mana TOS acceptance harus terjadi. Dibuat sebagai referensi implementasi SwiftUI, mirror langsung dari Android (`AuthViewModel.kt`, `OnboardingTosScreen.kt`, `OnboardingScaffold.kt`).

> **PENTING:** TOS acceptance (`PATCH /auth/me` dengan `tos_accepted_at`) adalah **hard prerequisite** — backend mengembalikan 403 untuk semua endpoint `POST /letters` dan `POST /penpals/match` sampai field ini terisi. Layar TOS **tidak boleh di-skip**.

---

## 1. State Machine (`AuthState`)

Mirror `AuthUiState` dari `feature/auth/AuthViewModel.kt:42-55`:

```swift
enum AuthState: Equatable {
    case loading
    case unauthenticated                // belum ada Firebase user → tampilkan LoginView
    case needsRegister                  // Firebase user ada, backend record belum → OnboardingView
    case needsOnboarding(User)          // backend record ada, nickname/continent kosong → OnboardingView
    case authenticated(User)            // selesai — masuk TabView
    case error(String)                  // error tidak recoverable, balik ke LoginView
}
```

**Bedanya dengan Android:** `needsOnboarding` di Android mengecek `tosAcceptedAt`, `gender`, `birthday`, dan `interests` (`AuthRepository.kt:49-50`). Di iOS MVP, cukup cek `nickname`/`continent` untuk routing ke `OnboardingView` — **TOS ditaruh sebagai step pertama di dalam `OnboardingView`**, bukan sebagai state tersendiri. Ini menyederhanakan onboarding, tapi TOS tetap wajib diklik sebelum user bisa melanjutkan.

---

## 2. Flow Lengkap — Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  App Launch                                                                  │
│  ↓ cek Auth.auth().currentUser (lokal, bukan network)                        │
└──────────────────────────────┬──────────────────────────────────────────────┘
                               │
              ┌────────────────┴──────────────────┐
              │ nil                               │ ada Firebase user
              ▼                                   ▼
     ┌─────────────────┐              ┌───────────────────────┐
     │   LoginView     │              │  GET /auth/me (1×)    │
     │                 │              └──────────┬────────────┘
     │ ① Google Sign-In│                         │
     │ ② Email/Password│         ┌───────────────┼───────────────┐
     └────────┬────────┘         │               │               │
              │               404 (NR)       200 (cek)        401 (SO)
              │                  │               │               │
              │                  ▼           nick/cont       sign-out diam
              │      ┌─────────────────┐    kosong? ya          │
              │      │  OnboardingView  │◄──────────            │
              │      │                 │                        ▼
              │      │ FASE 1: TOS     │              ┌─────────────────┐
              │      │  ☐ checkbox     │              │   LoginView     │
              │      │  PATCH /auth/me │              └─────────────────┘
              │      │  tos_accepted_at│
              │      │                 │
              │      │  (jika nick/cont│
              │      │   masih kosong) │
              │      │                 │
              │      │ FASE 2: Profile │
              │      │  Nickname field │
              │      │  Continent pick │
              │      │  POST /register │
              │      │  atau PATCH /me │
              │      └────────┬────────┘
              │               │ sukses
              └───────────────┼─────────────────
                              ▼
              ┌───────────────────────────────┐
              │  TabView (authenticated)       │
              │  Tab default: PenPalsFeedView  │
              └───────────────────────────────┘
```

**Legenda:** NR = NeedsRegister, cek = cek field, SO = sign-out diam

---

## 3. Urutan Panggilan API yang Benar

| Langkah | Kondisi | Yang dilakukan |
|---|---|---|
| **1** | App start | Cek `Auth.auth().currentUser` (lokal, **bukan** network). Jika `nil` → `.unauthenticated` → tampilkan `LoginView`. |
| **2** | Ada Firebase user | Panggil `GET /auth/me` **satu kali saja**. |
| **2a** | `GET /auth/me` → 404 | → `.needsRegister` → tampilkan `OnboardingView`. |
| **2b** | `GET /auth/me` → 200, tapi `nickname`/`continent` kosong | → `.needsOnboarding` → tampilkan `OnboardingView` yang sama. |
| **2c** | `GET /auth/me` → 200, lengkap | → `.authenticated` → masuk `TabView`, tab default PenPals. |
| **2d** | `GET /auth/me` → 401 | Sign-out diam-diam (token Firebase basi), balik ke `.unauthenticated`. |
| **2e** | Error lain | → `.error(message)`, tampilkan banner, tetap di `LoginView`. |
| **3** | Google/Email sign-in atau sign-up sukses | Ulangi langkah 2 (satu `GET /auth/me`). Firebase sign-in sendiri **tidak pernah** langsung set `.authenticated`. |
| **4** | `POST /auth/register` sukses | **Jangan** fetch ulang `/auth/me`. Pakai response body langsung untuk tentukan state selanjutnya. |

---

## 4. TOS Acceptance — Detail

### Posisi dalam Flow

TOS ada di dalam `OnboardingView` sebagai **fase pertama**, sebelum form nickname/continent. Tidak bisa dilewati.

Android: `OnboardingTosScreen.kt` — step 1 dari 7, tanpa tombol Skip.
iOS: fase 1 dari 2 di dalam `OnboardingView`, tanpa tombol Skip.

### Endpoint

```
PATCH /api/v1/auth/me
Body: { "tos_accepted_at": "2026-09-20T13:11:51Z" }
```

Swift: `Date().ISO8601Format()` → string yang tepat.

### Routing setelah TOS diterima

Mirror dari `OnboardingTosScreen.kt:192-199`:

```swift
// Setelah PATCH /auth/me sukses
func afterTosAccepted(user: User) {
    let hasNickname = !(user.nickname ?? "").isEmpty
    let hasContinent = !(user.continent ?? "").isEmpty
    if hasNickname && hasContinent {
        // Existing user yang hanya butuh TOS → langsung masuk TabView
        authState = .authenticated(user)
    } else {
        // User baru → lanjut ke fase 2 (nickname + continent)
        showNicknameForm = true
    }
}
```

### Kenapa tidak bisa di-skip

1. Backend 403 pada `POST /letters` dan `POST /penpals/match` jika `tos_accepted_at` null.
2. Android: checkbox TOS tidak ada tombol Skip — `onSkip = null` di `OnboardingScaffold`.
3. iOS: tombol "Lanjutkan" di-disable sampai checkbox dicentang.

---

## 5. `OnboardingView` — Dua Fase

### Fase 1: TOS (tidak bisa dilewati)

```
┌──────────────────────────────────────────────┐
│  ● ○                               [no skip] │  ← step dots, TANPA tombol Skip
│                                              │
│  Sebelum mulai,                              │
│  baca dulu ya 📜                             │
│  Pastikan kamu setuju sebelum lanjut.        │
│                                              │
│                          (konten kosong)     │
│                                              │
│  ☐  Saya setuju dengan **Syarat Layanan**    │
│     dan **Kebijakan Privasi**                │
│                                              │
│  ┌──────────────────────────────────────┐    │
│  │            Lanjutkan                 │    │  ← disabled sampai ☑
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘
```

- "Syarat Layanan" dan "Kebijakan Privasi" → link tappable → buka `SafariViewController`
- Tap di bagian teks lain → toggle checkbox
- On submit: `PATCH /auth/me { tos_accepted_at }`

### Fase 2: Nickname + Continent

```
┌──────────────────────────────────────────────┐
│  ○ ●                               [no skip] │
│                                              │
│  Siapa kamu?                                 │
│  Pilih nama panggilanmu dan asalmu.          │
│                                              │
│  Nickname                                    │
│  ┌──────────────────────────────────────┐    │
│  │  Nama Panggilanmu...                 │    │
│  └──────────────────────────────────────┘    │
│                                              │
│  Benua                                       │
│  ┌──────────────────────────────────────┐    │
│  │  Pilih benua  ▼                      │    │
│  └──────────────────────────────────────┘    │
│                                              │
│  ┌──────────────────────────────────────┐    │
│  │             Mulai! 🚀                │    │
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘
```

- On submit: `POST /auth/register` jika state `.needsRegister`, `PATCH /auth/me` jika `.needsOnboarding`
- Pakai response body langsung → jika `nickname`/`continent` terisi → `.authenticated`

**7 opsi Continent:** `ASIA`, `EUROPE`, `NORTH_AMERICA`, `SOUTH_AMERICA`, `AFRICA`, `OCEANIA`, `ANTARCTICA`

---

## 6. Skeleton SwiftUI

### `AuthViewModel.swift`

```swift
import SwiftUI
import FirebaseAuth

@MainActor
class AuthViewModel: ObservableObject {
    @Published var state: AuthState = .loading

    init() { Task { await checkAuthState() } }

    func checkAuthState() async {
        state = .loading
        guard let _ = Auth.auth().currentUser else {
            state = .unauthenticated
            return
        }
        await fetchMeAndRoute()
    }

    private func fetchMeAndRoute() async {
        do {
            let user = try await APIClient.shared.getMe()
            if (user.nickname ?? "").isEmpty || (user.continent ?? "").isEmpty {
                state = .needsOnboarding(user)
            } else {
                state = .authenticated(user)
            }
        } catch APIError.notFound {
            state = .needsRegister
        } catch APIError.unauthorized {
            try? Auth.auth().signOut()
            state = .unauthenticated
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    func signInWithGoogle() async {
        state = .loading
        // ... Google Sign-In logic ...
        await fetchMeAndRoute()  // selalu lewat /auth/me
    }

    func signInWithEmail(_ email: String, password: String) async {
        state = .loading
        do {
            try await Auth.auth().signIn(withEmail: email, password: password)
            await fetchMeAndRoute()  // selalu lewat /auth/me
        } catch {
            state = .unauthenticated
            // surfacing error ke form, bukan ke state global
        }
    }

    func acceptTos() async {
        let timestamp = Date().ISO8601Format()
        do {
            let user = try await APIClient.shared.patchMe(tosAcceptedAt: timestamp)
            // Cek apakah nickname/continent sudah ada (existing user)
            if !(user.nickname ?? "").isEmpty && !(user.continent ?? "").isEmpty {
                state = .authenticated(user)  // langsung masuk TabView
            }
            // else: caller akan pindah ke fase 2 (nickname/continent)
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    func register(nickname: String, continent: String) async {
        state = .loading
        do {
            let user = try await APIClient.shared.register(nickname: nickname, continent: continent)
            // Jangan fetch ulang /auth/me — pakai response langsung
            state = .authenticated(user)
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    func patchMe(nickname: String, continent: String) async {
        state = .loading
        do {
            let user = try await APIClient.shared.patchMe(nickname: nickname, continent: continent)
            state = .authenticated(user)
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    func signOut() {
        try? Auth.auth().signOut()
        state = .unauthenticated
    }
}
```

### `RootView.swift`

```swift
struct RootView: View {
    @StateObject private var authVM = AuthViewModel()

    var body: some View {
        Group {
            switch authVM.state {
            case .loading:
                ProgressView()

            case .unauthenticated, .error:
                LoginView()
                    .environmentObject(authVM)

            case .needsRegister, .needsOnboarding:
                OnboardingView()
                    .environmentObject(authVM)

            case .authenticated(let user):
                MainTabView(user: user)
                    .environmentObject(authVM)
            }
        }
    }
}
```

### `OnboardingView.swift`

```swift
struct OnboardingView: View {
    @EnvironmentObject var authVM: AuthViewModel
    @State private var phase: Phase = .tos
    @State private var tosChecked = false
    @State private var nickname = ""
    @State private var continent = ""
    @State private var legalURL: URL? = nil

    enum Phase { case tos, profile }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Step dots — 2 step, tanpa Skip button
            StepDots(current: phase == .tos ? 1 : 2, total: 2)
                .padding(.top, 16)

            if phase == .tos {
                tosPhase
            } else {
                profilePhase
            }
        }
        .padding(.horizontal, 24)
        .sheet(item: $legalURL) { url in
            SafariView(url: url)
        }
    }

    // MARK: - Fase 1: TOS
    var tosPhase: some View {
        VStack(alignment: .leading, spacing: 0) {
            Spacer().frame(height: 32)
            Text("Sebelum mulai,\nbaca dulu ya 📜")
                .font(.title2.bold())
            Text("Pastikan kamu setuju sebelum lanjut.")
                .font(.body).foregroundStyle(.secondary)
                .padding(.top, 6)

            Spacer()

            HStack(alignment: .top, spacing: 12) {
                Toggle("", isOn: $tosChecked).labelsHidden()

                // "Saya setuju dengan [Syarat Layanan] dan [Kebijakan Privasi]"
                // Implementasi teks dengan link — gunakan AttributedString
                tosLabel
            }
            .padding(.bottom, 24)

            Button("Lanjutkan") {
                Task { await submitTos() }
            }
            .frame(maxWidth: .infinity)
            .buttonStyle(.borderedProminent)
            .disabled(!tosChecked || authVM.state == .loading)

            Spacer().frame(height: 24)
        }
    }

    func submitTos() async {
        await authVM.acceptTos()
        // Jika setelah TOS state masih needsOnboarding → pindah ke fase 2
        if case .needsOnboarding = authVM.state {
            phase = .profile
        }
        // Jika .authenticated → RootView auto-route ke TabView
    }

    // MARK: - Fase 2: Nickname + Continent
    var profilePhase: some View {
        VStack(alignment: .leading, spacing: 0) {
            Spacer().frame(height: 32)
            Text("Siapa kamu?")
                .font(.title2.bold())
            Text("Pilih nama panggilanmu dan asalmu.")
                .font(.body).foregroundStyle(.secondary)
                .padding(.top, 6)
            Spacer().frame(height: 28)

            Text("Nickname").font(.headline)
            TextField("Nama Panggilanmu...", text: $nickname)
                .textFieldStyle(.roundedBorder)
                .padding(.top, 8)

            Spacer().frame(height: 24)

            Text("Benua").font(.headline)
            Picker("Pilih benua", selection: $continent) {
                Text("Pilih benua").tag("")
                ForEach(continents, id: \.self) { c in Text(c).tag(c) }
            }
            .pickerStyle(.menu)
            .padding(.top, 8)

            Spacer()

            Button("Mulai! 🚀") {
                Task { await submitProfile() }
            }
            .frame(maxWidth: .infinity)
            .buttonStyle(.borderedProminent)
            .disabled(nickname.isEmpty || continent.isEmpty || authVM.state == .loading)

            Spacer().frame(height: 24)
        }
    }

    func submitProfile() async {
        if case .needsRegister = authVM.state {
            await authVM.register(nickname: nickname, continent: continent)
        } else {
            await authVM.patchMe(nickname: nickname, continent: continent)
        }
    }

    let continents = ["ASIA", "EUROPE", "NORTH_AMERICA", "SOUTH_AMERICA",
                      "AFRICA", "OCEANIA", "ANTARCTICA"]
}
```

---

## 7. Perbedaan Android vs iOS — Ringkasan

| Aspek | Android | iOS MVP |
|---|---|---|
| TOS step | Step 1 dari 7 (`OnboardingTosScreen`) — layar tersendiri | Fase 1 dari 2 di dalam `OnboardingView` |
| `needsOnboarding()` cek | `tosAcceptedAt == null \|\| gender == null \|\| birthday == null \|\| interests.isEmpty()` | Cukup `nickname == null \|\| continent == null` |
| Gender + Birthday | Step 3 dari 7 (wajib, age gate Play Store) | **Dipotong** |
| Interests | Step 4 dari 7 | **Dipotong** |
| Language/Bio | Step 5 dari 7 | **Dipotong** |
| Photo | Step 6 dari 7 | **Dipotong** |
| Notify Hour | Step 7 dari 7 | **Dipotong** |
| Register → routing | Setelah register → cek `needsOnboarding()` → bisa route ke onboarding | Setelah register → pakai response body langsung → `.authenticated` (nickname+continent sudah terisi di request) |
| `/auth/me` calls per sesi | Bisa 4-5x (bug terdokumentasi di `ios-mvp-spec.md §2b`) | 1x saja via shared `AuthViewModel` + `@EnvironmentObject` |

---

## 8. Checklist Implementasi

- [ ] `AuthState` enum dengan 6 kasus
- [ ] `AuthViewModel` (`ObservableObject`) dengan `checkAuthState()` di `init`
- [ ] `RootView` routing berdasarkan `authVM.state`
- [ ] `LoginView` — Google Sign-In + Email/Password form
- [ ] `OnboardingView` — 2 fase:
  - [ ] **Fase 1 TOS**: `StepDots(current:1, total:2)` tanpa Skip, checkbox dengan teks link tappable, tombol disabled sampai checked, `PATCH /auth/me { tos_accepted_at }`
  - [ ] Auto-route: jika nickname/continent sudah ada → `.authenticated`; jika belum → pindah ke fase 2
  - [ ] **Fase 2 Profile**: `StepDots(current:2, total:2)` tanpa Skip, nickname TextField + continent Picker (7 opsi), `POST /auth/register` atau `PATCH /auth/me`
- [ ] `MainTabView` — 3 tab: PenPals | Mailbox | Profile
- [ ] `ProfileView` baca data dari `authVM.state`, **bukan** memanggil `GET /auth/me` sendiri

---

## 9. Referensi File Android

| File iOS yang akan dibuat | Mirror dari Android |
|---|---|
| `AuthViewModel.swift` | `feature/auth/AuthViewModel.kt` |
| `OnboardingView.swift` (fase TOS) | `ui/screens/onboarding/OnboardingTosScreen.kt` |
| `OnboardingView.swift` (fase profile) | `ui/screens/onboarding/OnboardingScaffold.kt` |
| `StepDots` view | `OnboardingScaffold.kt:128-154` (fungsi `StepDots`) |
| `AuthRepository.swift` | `feature/auth/data/AuthRepository.kt` |
| `needsOnboarding` logic | `feature/auth/data/AuthRepository.kt:49-50` |
