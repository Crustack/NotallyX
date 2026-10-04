# Koin Safe DSL + Compiler Plugin — Reference

Concrete cheat sheet for the Safe DSL output used by Kodein / classic Koin DSL / Koin 3.x migrations. Cross-link target for the per-source references — they map source constructs to entries here.

For depth, see https://insert-koin.io.

## Setup (shared by every migration to Safe DSL output)

**Minimum versions** (also enforced by SKILL.md rule #3):

| Component             | Minimum     |
|-----------------------|-------------|
| Kotlin                | `2.3.20`    |
| Koin                  | `4.2.1`     |
| Koin Compiler Plugin  | `1.0.0-RC1` |

Always try to resolve newer versions at Step 3 — minimums, not targets.

**Standard Gradle block** (per-source references add only the deltas):

```kotlin
// build.gradle.kts
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}

dependencies {
    implementation(platform("io.insert-koin:koin-bom:4.2.1"))
    implementation("io.insert-koin:koin-core")
    implementation("io.insert-koin:koin-android")              // Android
    implementation("io.insert-koin:koin-androidx-compose")     // Compose
    implementation("io.insert-koin:koin-androidx-workmanager") // Workers
}
```

Note: `koin-annotations` is **not** required for Safe DSL output unless you mix
in `@InjectedParam` / `@Named` / `@Qualifier` on classes — which most Safe DSL
migrations do. When in doubt, include it.

## Imports

**Safe DSL with the Koin Compiler Plugin lives in a single package** —
`org.koin.plugin.module.dsl`. One import covers most of the surface.

| Symbol                                                                                              | Import                                       | Notes                                                          |
|-----------------------------------------------------------------------------------------------------|----------------------------------------------|----------------------------------------------------------------|
| `single<T>()`, `factory<T>()` (on `Module`)                                                         | `org.koin.plugin.module.dsl.*`               | **Safe DSL — reified, no lambda. Compiler resolves ctor.**     |
| `scoped<T>()`, `factory<T>()` (on `ScopeDSL`)                                                       | `org.koin.plugin.module.dsl.*`               | Scope-bound reified definitions                                |
| `viewModel<T>()`                                                                                    | `org.koin.plugin.module.dsl.*`               | From `koin-core-viewmodel`                                     |
| `worker<T>()`                                                                                       | `org.koin.plugin.module.dsl.*`               | From `koin-androidx-workmanager`                               |
| `Scope.create(::T)`                                                                                 | `org.koin.plugin.module.dsl.create`          | Explicit factory-function reference (alternative to `single<T>()`) |
| `module { }` (builder lambda)                                                                       | `org.koin.dsl.module`                        | Module declaration block                                       |
| `startKoin { modules(...) }`                                                                        | `org.koin.core.context.startKoin`            | **Classic startKoin — use this for Safe DSL output.** The reified `startKoin<App>()` from `org.koin.plugin.module.dsl` is for `@KoinApplication` (annotations path) only. |
| `.bind<T>()` on `KoinDefinition<*>`                                                                 | `org.koin.core.module.dsl.bind` *(extension)* | Interface binding (preferred shorthand). Verify with IDE auto-import |
| `.withOptions { ... }`                                                                              | `org.koin.core.module.dsl.withOptions`       | Combine binding with other options                             |
| `singleOf(::T)`, `factoryOf(::T)`, `viewModelOf(::T)`                                               | `org.koin.core.module.dsl.*`                 | **Legacy — fallback only. Prefer reified `single<T>()` etc.**  |
| `named("x")`, `Qualifier`                                                                           | `org.koin.core.qualifier.*`                  | String + custom qualifiers                                     |
| `parametersOf(...)`                                                                                 | `org.koin.core.parameter.parametersOf`       | Pass runtime params at retrieval                               |
| `androidContext`, `androidLogger`                                                                   | `org.koin.android.ext.koin.*`                | Android startKoin DSL                                          |
| `KoinContext`, `koinInject()`                                                                       | `org.koin.compose.*`                         | Compose injection                                              |
| `koinViewModel()` (Compose retrieval)                                                               | `org.koin.androidx.compose.koinViewModel`    | Compose ViewModel                                              |
| `workManagerFactory()` (startKoin DSL)                                                              | `org.koin.androidx.workmanager.koin.workManagerFactory` | WorkManager startKoin setup                       |
| `KoinComponent`, `inject()`, `get()`                                                                | `org.koin.core.component.*`                  | For classes Koin doesn't construct — prefer constructor injection |

## Definitions — form preference

For classes you own, always use the **reified** form. The Koin Compiler Plugin
resolves every constructor parameter automatically — no lambda needed.

| Form                              | Use when                                           |
|-----------------------------------|----------------------------------------------------|
| `single<T>()`                     | Shared instance, class you own                     |
| `factory<T>()`                    | New instance per resolution, class you own         |
| `viewModel<T>()`                  | Android ViewModel, class you own                   |
| `worker<T>()`                     | WorkManager Worker — reified Safe DSL form (needs `koin-androidx-workmanager`) |
| `single { create(::T) }`          | Explicit factory-function reference — equivalent to `single<T>()`, use when intent should be visible in review |
| `single { T(get(), get()) }`      | **Fallback** — only for third-party types that can't be reached via reified generics (`Retrofit.Builder()`, `Room.databaseBuilder()`, `OkHttpClient.Builder()`) |
| `singleOf(::T)` / `factoryOf(::T)`| Legacy form — prefer `single<T>()` / `factory<T>()` |

```kotlin
val appModule = module {
    // Classes you own — reified Safe DSL
    single<HttpClient>()
    single<Repository>()                           // auto-resolves ctor from graph
    factory<RequestId>()
    viewModel<HomeViewModel>()

    // Third-party types — lambda fallback is fine
    single<Retrofit> {
        Retrofit.Builder()
            .client(get())
            .baseUrl(BuildConfig.BASE_URL)
            .build()
    }

    // WorkManager
    worker<SyncWorker>()                           // Safe DSL — reified, ctor auto-resolved
    // Lambda form is fallback only:
    // worker { SyncWorker(androidContext(), get(), get()) }
}
```

## Interface binding

```kotlin
val appModule = module {
    single<RepositoryImpl>().bind<Repository>()                  // preferred
    single<RepositoryImpl>().withOptions { bind<Repository>() }  // equivalent — use when combining with other options

    // Multiple interfaces
    single<RepositoryImpl>()
        .bind<Repository>()
        .bind<Persistable>()
}
```

## Qualifiers

**`@Named` is the canonical Safe DSL qualifier.** Declare it on the **class
declaration** or on a **constructor parameter**, then use the plain reified
`single<T>()` in the module. The Compiler Plugin reads `@Named` off the class/param
and registers the binding with that qualifier.

`@Qualifier` (custom annotation) works the same way and is the type-safe alternative.

**Do not write `single<T>(named("x")) { ... }` in Safe DSL output** — that's
classic DSL (a lambda-backed overload). The Safe DSL form has no qualifier
argument on `single<T>()`; the qualifier lives on the class.

### `@Named` — the default pattern

```kotlin
// Qualifier on the class declaration — this is what the Compiler Plugin reads
@Named("auth")
class AuthHttpClient(val cfg: Config) : HttpClient

@Named("public")
class PublicHttpClient(val cfg: Config) : HttpClient

// Qualifier on a constructor parameter — resolves the matching qualified dependency
class Service(@Named("auth") val client: HttpClient)

val networkModule = module {
    single<AuthHttpClient>()     // registered with qualifier named("auth")
    single<PublicHttpClient>()   // registered with qualifier named("public")
    single<Service>()            // @Named("auth") on ctor param wires AuthHttpClient
}
```

For types you can't annotate (third-party classes), drop to classic-DSL lambda
form — that's the only place `single<T>(named("x")) { ... }` appears legitimately:

```kotlin
val networkModule = module {
    // Classic DSL lambda — third-party type, qualifier supplied here
    single<Config>(named("authConfig"))   { Config("...") }
    single<Config>(named("publicConfig")) { Config("...") }
}
```

### `@Qualifier` — for type-safe qualifiers

```kotlin
@Qualifier annotation class Auth
@Qualifier annotation class Public

@Auth   class AuthHttpClient(val cfg: Config) : HttpClient
@Public class PublicHttpClient(val cfg: Config) : HttpClient

class Service(@Auth val client: HttpClient)

val networkModule = module {
    single<AuthHttpClient>()
    single<PublicHttpClient>()
    single<Service>()
}
```

### `@Property`

```kotlin
class ApiClient(@Property("api.url") val url: String)

val appModule = module {
    single<ApiClient>()
}
// load via startKoin { properties(mapOf("api.url" to "https://...")) }
```

### Call-site lookup

```kotlin
val auth: HttpClient   = get(named("auth"))
val publicCli          = get<HttpClient>(Public)
```

## Parameter resolution

The compiler resolves every constructor parameter from the graph. Modifiers on
the parameter type control the resolution:

| Parameter type       | Resolves to                                    |
|----------------------|------------------------------------------------|
| `T`                  | Required — compile error if missing            |
| `T?`                 | Optional — `null` if no definition             |
| `List<T>`            | All definitions of `T`                         |
| `Lazy<T>`            | Deferred — resolved on first access            |

For runtime parameters, the **preferred form is `@InjectedParam` on the
constructor** — the Compiler Plugin reads it whether the module uses annotations
or Safe DSL:

```kotlin
// Class side — @InjectedParam marks the runtime-supplied param
class SessionToken(@InjectedParam val userId: String, val api: AuthApi)
class DetailViewModel(val repo: Repo, @InjectedParam val itemId: String) : ViewModel()

// Module side — Safe DSL, no lambda needed
val appModule = module {
    factory<SessionToken>()
    viewModel<DetailViewModel>()
}

// Call site — identical to annotations style
val token: SessionToken = get { parametersOf(userId) }
val vm: DetailViewModel by viewModel { parametersOf(itemId) }
```

Lambda-destructuring is a fallback only for classes you can't annotate
(third-party types with runtime params):

```kotlin
val appModule = module {
    factory { (url: String) -> ThirdPartyConnection(url, get()) }
}
```

## Scopes

```kotlin
val appModule = module {
    // Android scope archetypes
    activityScope { scoped<TabRouter>() }
    fragmentScope { scoped<FormState>() }
    viewModelScope { scoped<PageState>() }

    // Custom scope
    scope<SessionScope> {
        scoped<SessionToken>()
        scoped<AuthService>()
    }
}
```

## App entry point

**For Safe DSL output, use the classic `startKoin` — not the reified Compiler-Plugin variant.** The reified one is for `@KoinApplication` auto-discovery only.

```kotlin
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            androidLogger()
            modules(appModule, networkModule, dataModule)
        }
    }
}
```

Mixing Safe DSL modules with annotated `@Module @Configuration` classes:

```kotlin
startKoin {
    androidContext(this@App)
    modules(appModule, module<AnnotatedModule>())
}
```

## Retrieval

From an Android entry point:

```kotlin
class MyActivity : AppCompatActivity() {
    private val repo: Repository by inject()
    private val vm: HomeViewModel by viewModel()
}
```

From Compose:

```kotlin
@Composable fun Screen() {
    val repo = koinInject<Repository>()
    val vm   = koinViewModel<HomeViewModel>()
}
```

From a non-Koin-constructed class — prefer constructor injection via `single<T>()`
so the compiler resolves dependencies. Use `KoinComponent` only when constructor
injection is impossible:

```kotlin
class LegacyHelper : KoinComponent {
    private val repo: Repository by inject()
}
```

## Testing

```kotlin
class RepositoryTest : KoinTest {
    private val repo: Repository by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(appModule, module {
            single<FakeApiService>().bind<ApiService>()   // override production binding
        })
    }

    @Test
    fun `graph check`() = checkModules { modules(appModule) }
}
```

## Classic DSL fallback — when it's OK

Lambda form (`single { T(get()) }` / `singleOf(::T)`) is a **fallback only**. Legitimate uses:

- Third-party builder types (`Retrofit`, `Room`, `OkHttpClient`, `Firebase*`)
- Qualified bindings where multiple qualifier variants need different ctor args:
  ```kotlin
  single<HttpClient>(Auth) { HttpClient(get(Auth)) }
  ```
- Runtime parameters: `factory { (id: String) -> ... }`
- Test overrides with hand-built mocks

Do **not** use classic DSL for ordinary classes you own — the compiler handles
them via `single<T>()`.

## What this skill does NOT cover

- Full Koin feature set — see https://insert-koin.io
- Annotations output — see `references/koin-annotations.md`
- Ktor / server / Jetpack Compose Multiplatform integration — see Koin docs
