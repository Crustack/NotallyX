# Kodein → Koin 4.x + Koin Compiler Plugin Migration Reference

> **Setup & minimum versions** — see [`koin-safe-dsl.md`](koin-safe-dsl.md) → "Setup" for the standard Gradle block and Kotlin/Koin/Compiler Plugin minimum versions. This file holds only the source-specific deltas.

## Table of Contents
1. [Progressive Migration](#progressive-migration)
2. [Concept Mapping](#concept-mapping)
3. [Gradle Setup](#gradle-setup)
4. [Module / DI Container](#module-container)
5. [Bindings](#bindings)
6. [Qualifiers / Tags](#qualifiers)
7. [Constructor Parameters](#constructor-parameters)
8. [Retrieval / Injection Sites](#retrieval)
9. [Android Integration](#android)
10. [Compose Integration](#compose)
11. [KMP Considerations](#kmp)
12. [Testing](#testing)

---

## Progressive Migration

Do **not** rewrite existing Kodein modules in place. Instead:

1. Create a new empty Koin module (e.g. `appKoinModule`) and call `startKoin { modules(appKoinModule) }`
2. Keep the existing Kodein `DI` container running for everything not yet migrated
3. Move definitions from Kodein modules into the Koin module **one feature at a time**
4. At each call site, swap `by instance()` / `direct.instance()` for `by inject()` / `get()` only for the bindings already moved
5. Remove the Kodein dependency once the last binding has moved over

This keeps the project building and runnable at every step and limits the blast radius
of each migration commit.

For Kodein-owned bindings consumed from a Koin definition during the bridging
window, write a small Koin-scope helper that resolves from the live `DI`
container — same shape as the `Scope.dagger<T>()` helper in `hilt-to-koin.md`,
just delegating to Kodein's `direct.instance()` instead of `EntryPoints.get()`.

**Co-locate bridge helpers in the consumer module** (Safe DSL `module { }` for
the feature that consumes the Kodein binding) — strict A1 verification then
stays green and no compile-check relaxation is needed. See SKILL.md →
"Bridging with the existing DI" for the full rationale.

---

## Concept Mapping

Mappings target **Safe DSL + Koin Compiler Plugin** (no annotations for Kodein):

| Kodein Construct                     | Koin Safe DSL Equivalent                              |
|--------------------------------------|-------------------------------------------------------|
| `DI { ... }` container               | `startKoin { modules(...) }`                          |
| `DI.Module("name") { ... }`          | `val appModule = module { ... }`                      |
| `bind<T>() with singleton { ... }` / `bindSingleton<T> { }` | `single<T>()`                      |
| `bind<T>() with provider { ... }` / `bindProvider<T> { }`   | `factory<T>()`                     |
| `bindInstance<T> { ... }`            | `single<T> { ... }` (lambda for third-party types)    |
| `bind<T>() with scoped(scope).singleton { }` | `scope<S> { scoped<T> { ... } }`              |
| `bind<T>(tag = "x")`                 | `@Named("x")` on the class + `single<T>()` (reified) — always `@Named` for Kodein migrations |
| `instance()` / `by instance()`       | `get()` / `by inject()`                               |
| `factory<P, T>()` / `bindFactory<P, T> { }` (one runtime arg) | `factory<T> { (p: P) -> T(p, ...) }` — retrieve with `get { parametersOf(p) }` |
| `multiton<P, T>()` (cached factory)  | No direct equivalent — a `single<Map<P, T>>` cache, or `single<T>` per known key |
| `bindings.set { add(...) }` (set bindings) | Manual `List<T>` collected inside a `single<List<T>>` |
| `subDI(parentDI) { ... }`            | Multiple modules in `startKoin { modules(parent, child) }` |
| `DIAware` interface on a class       | `KoinComponent` interface + `by inject()`             |
| `androidXModule(app)`                | `androidContext(this@App)` in `startKoin { }`         |
| `closestDI()` from Android `Context` | `getKoin()` (Android), `KoinContext { }` (Compose)    |

---

## Gradle Setup

```kotlin
// REMOVE
implementation("org.kodein.di:kodein-di:<version>")
implementation("org.kodein.di:kodein-di-framework-android-x:<version>")
implementation("org.kodein.di:kodein-di-framework-compose:<version>")

// ADD
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}

dependencies {
    implementation("io.insert-koin:koin-core:4.2.1")
    implementation("io.insert-koin:koin-annotations:4.2.1")
    implementation("io.insert-koin:koin-android:4.2.1")             // Android
    implementation("io.insert-koin:koin-androidx-compose:4.2.1")    // Compose
}
```


---

## Module Container

**Kodein**

```kotlin
val appModule = DI.Module("app") {
    bind<Repository>() with singleton { RepositoryImpl(instance()) }
    bind<HttpClient>() with singleton { HttpClient() }
}

val di = DI {
    import(appModule)
}
```

**Koin (Safe DSL — the default for Kodein migrations)**

Kodein's `DI { ... }` builder maps cleanly onto Koin's `module { }` lambda.
Safe DSL keeps the same shape while adding reified type parameters and
compile-time verification via the Koin Compiler Plugin. **Do not produce an
annotations-based migration for Kodein** — there are no source annotations to
map from, and the DSL shape is what Kodein users are coming from.

```kotlin
val appModule = module {
    single<RepositoryImpl>().bind<Repository>()  // Safe DSL, ctor auto-resolved
    single<HttpClient>()                         // Safe DSL, no ctor params
    // viewModel<MyViewModel>()
    // factory<Connection>()
}

startKoin {
    modules(appModule)
}
```

**Safe DSL forms** (preferred — compiler resolves constructor params):

- `single<T>()` — no lambda, reified. Compiler resolves every ctor param.
- `single { create(::T) }` — explicit factory-function reference, equivalent to
  the above but more explicit. Use when you want the `::T` reference to be
  visible (e.g. to show intent in a code review).
- `single<Impl>().bind<I>()` — bind an impl to an interface.

**Classic DSL lambda** (`single { T(get()) }`) is a fallback **only** for
third-party types that can't be reached via reified generics (e.g. `Retrofit.Builder()`,
`Room.databaseBuilder()`). Do not use it for classes you own.

Classic DSL lambdas (`single { Retrofit.Builder()... }`) remain a fallback for
third-party types that can't be reached via reified generics. Do not emit
`@Singleton` / `@Factory` / `@Module` class annotations in Kodein migration output.

---

## Bindings

| Kodein                                       | Koin Safe DSL            |
|----------------------------------------------|--------------------------|
| `bind<T>() with singleton { T(instance()) }` | `single<T>()`            |
| `bind<T>() with provider { T() }`            | `factory<T>()`           |
| `bind<I>() with singleton { Impl() }`        | `single<Impl>().bind<I>()` (or `.withOptions { bind<I>() }`) |
| `bind<VM>() with provider { VM() }` (ViewModel) | `viewModel<VM>()`     |
| `bind<W>() with provider { W(...) }` (Worker) | `worker<W>()` — requires `io.insert-koin:koin-androidx-workmanager` |

---

## Qualifiers

**Kodein** uses `tag` to disambiguate:

```kotlin
bind<HttpClient>(tag = "auth") with singleton { HttpClient(authConfig) }
bind<HttpClient>(tag = "public") with singleton { HttpClient(publicConfig) }

val auth: HttpClient by instance(tag = "auth")
```

**Koin Safe DSL** uses `named("...")` (string) or a custom qualifier object
(type-safe) at definition and retrieval sites:

```kotlin
val networkModule = module {
    single<HttpClient>(named("auth"))   { HttpClient(get(named("authConfig"))) }
    single<HttpClient>(named("public")) { HttpClient(get(named("publicConfig"))) }

    single<Service> { Service(client = get(named("auth"))) }
}
```

Kodein's `tag = "x"` maps to Koin's `@Named("x")`. In Safe DSL, the qualifier
lives on the **class declaration** (or a ctor parameter for the consumer side)
— not as an argument to `single<T>()`. The Compiler Plugin reads `@Named` off
the class/param.

```kotlin
// Qualifier on the class itself
@Named("auth")
class AuthHttpClient(val cfg: Config) : HttpClient

@Named("public")
class PublicHttpClient(val cfg: Config) : HttpClient

// Qualifier on a ctor parameter — resolves the matching qualified dependency
class Service(@Named("auth") val client: HttpClient)

// Module — plain reified single<T>(), no qualifier argument
val networkModule = module {
    single<AuthHttpClient>()
    single<PublicHttpClient>()
    single<Service>()
}

// Call site
val auth: HttpClient = get(named("auth"))
```

**Do not write `single<HttpClient>(named("auth")) { ... }`** — that's the
classic-DSL lambda form; it's reserved as a fallback for third-party types that
can't be annotated (e.g. a `Config` with per-qualifier values):

```kotlin
// Legitimate classic-DSL fallback: Config is just a data class, no useful annotations
val networkModule = module {
    single<Config>(named("authConfig"))   { Config("...") }
    single<Config>(named("publicConfig")) { Config("...") }
}
```

**Stay on `@Named` for Kodein migrations** — don't introduce custom `@Qualifier`
annotations. Kodein's string-based `tag` semantics map 1:1 to `@Named`, and
keeping the string form preserves the shape Kodein users already recognize.

---

## Constructor Parameters

**Kodein** uses `factory<P, T>` for one runtime arg, `multiton` for caching:

```kotlin
bind<Connection>() with factory { url: String -> Connection(url) }

val conn: Connection = di.direct.factory<String, Connection>()("http://...")
```

**Koin** uses `@InjectedParam` and `parametersOf(...)`:

```kotlin
@Factory
class Connection(@InjectedParam val url: String)

val conn: Connection = get { parametersOf("http://...") }
```

For multiple runtime args, list each `@InjectedParam` parameter — order matches `parametersOf(...)`.

---

## Retrieval

| Kodein                                  | Koin                            |
|-----------------------------------------|---------------------------------|
| `class X(override val di: DI) : DIAware` | `class X : KoinComponent`      |
| `val r: Repo by instance()`             | `val r: Repo by inject()`       |
| `val r: Repo = di.direct.instance()`    | `val r: Repo = get()`           |
| `val r: Repo by instance(tag = "x")`    | `val r: Repo by inject(named("x"))` |
| `val r: () -> Repo by provider()` (new instance per call when bound to `provider`) | Re-resolve via `get<Repo>()` on each call (`KoinComponent`). `Lazy<Repo>` only matches Kodein `provider()` over a `singleton` binding — semantics differ for `provider` bindings. |
| `val r: Repo? by instanceOrNull()`      | `val r: Repo? by injectOrNull()` (or auto `Repo?` constructor param) |

Prefer **constructor injection** in Koin — declare dependencies as constructor
parameters and let `single<T>()` / `factory<T>()` resolve them via reified
generics. Reserve `KoinComponent` for cases where constructor injection is
impossible (Android `Activity` / `Fragment`, frameworks that own object
construction).

---

## Android

**Kodein**

```kotlin
class App : Application(), DIAware {
    override val di by DI.lazy {
        import(androidXModule(this@App))
        import(appModule)
    }
}

class MyActivity : AppCompatActivity(), DIAware {
    override val di by closestDI()
    private val repo: Repository by instance()
}
```

**Koin**

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(module<AppModule>())
        }
    }
}

class MyActivity : AppCompatActivity() {
    private val repo: Repository by inject()
    private val viewModel: MyViewModel by viewModel()
}
```

---

## Compose

**Kodein**

```kotlin
withDI(di) {
    val repo: Repository by rememberInstance()
}
```

**Koin**

```kotlin
KoinContext {
    val repo: Repository = koinInject()
    val vm: MyViewModel = koinViewModel()
}
```

---

## KMP

Kodein and Koin both support KMP. With the Koin Compiler Plugin:

- One plugin declaration in the root/shared module — no per-target configuration
- Use `expect`/`actual` for platform-specific bindings. Since Kodein migration
  output is Safe DSL only, the typical split is either an `expect fun` / `actual
  fun` factory (shown below) or an `expect class` / `actual class` for the
  platform-specific implementation. Pick the smallest unit that varies per
  platform.
- Same `startKoin { }` entry point on every target

```kotlin
// commonMain
interface PlatformService

expect fun platformService(): PlatformService

val sharedModule = module {
    single<PlatformService> { platformService() }
}

// androidMain
actual fun platformService(): PlatformService = AndroidPlatformService()

// iosMain
actual fun platformService(): PlatformService = IosPlatformService()
```

---

## Testing

**Kodein**

```kotlin
val testDI = DI {
    import(appModule, allowOverride = true)
    bind<Repository>(overrides = true) with singleton { FakeRepository() }
}
```

**Koin**

```kotlin
@get:Rule
val koinTestRule = KoinTestRule.create {
    modules(appModule, module {
        single<FakeRepository>().bind<Repository>()  // overrides previous binding
    })
}

@Test
fun `verify module graph`() = checkModules {
    modules(appModule)
}
```

Prefer the **Koin Compiler Plugin's A1/A2/A3 verification layers** to catch missing
bindings at compile time. `checkModules { }` (runtime) remains useful for DSL-only
modules or as a belt-and-braces check in CI.
