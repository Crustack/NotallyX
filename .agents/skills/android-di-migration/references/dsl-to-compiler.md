# Koin DSL → Koin 4.x Safe DSL + Koin Compiler Plugin Migration Reference

> **Setup & minimum versions** — see [`koin-safe-dsl.md`](koin-safe-dsl.md) → "Setup" for the standard Gradle block and Kotlin/Koin/Compiler Plugin minimum versions. This file holds only the source-specific deltas.

## Table of Contents
1. [Why Migrate](#why-migrate)
2. [Core DSL Mapping](#core-dsl-mapping)
3. [Gradle Setup](#gradle-setup)
4. [Singleton and Factory](#singleton-and-factory)
5. [ViewModel](#viewmodel)
6. [Qualifiers and Named](#qualifiers-and-named)
7. [Scopes](#scopes)
8. [Bindings (Interface → Implementation)](#bindings)
9. [Parameters](#parameters)
10. [Module Composition](#module-composition)
11. [KMP Modules](#kmp-modules)
12. [Compose Integration](#compose-integration)
13. [Mixing Old and New DSL](#mixing-old-and-new-dsl)
14. [Compile-Time Safety](#compile-time-safety)
15. [Common Pitfalls](#common-pitfalls)

---

## Why Migrate

The Koin Compiler Plugin transforms the **Safe DSL** (`single<T>()`, `factory<T>()`) at
compile time, giving you:

- **Compile-time verification** — missing or circular dependencies caught at build time, not at runtime
- **No manual `get()` wiring** — the compiler resolves constructor parameters automatically from `single<T>()`
- **No generated files** — inline transformation during Kotlin compilation
- **No annotations needed on classes** — your classes stay clean, all DI wiring stays in `module { }` blocks
- **Same module structure** — `val myModule = module { }` stays, just with safer syntax
- **Safer refactoring** — renaming a constructor parameter triggers a compile error, not a runtime crash
- **Full KMP support** — all targets with a single plugin declaration

The key insight: you keep the DSL module approach but replace `singleOf(::Class)` or
`single { Class(get()) }` with `single<Class>()`. The compiler plugin does the rest.

Requires Kotlin ≥ 2.3.20 (K2) and Koin 4.2.1+.

## Core DSL Mapping

| Old Koin DSL (3.x / 4.x manual)                    | New Safe DSL (Compiler Plugin)                       |
|-----------------------------------------------------|------------------------------------------------------|
| `single { MyClass(get()) }`                         | `single<MyClass>()`                                  |
| `single { MyClass(get(), get()) }`                  | `single<MyClass>()`                                  |
| `singleOf(::MyClass)`                               | `single<MyClass>()`                                  |
| `factory { MyClass(get()) }`                        | `factory<MyClass>()`                                 |
| `factoryOf(::MyClass)`                              | `factory<MyClass>()`                                 |
| `viewModel { MyVM(get()) }`                         | `viewModel<MyVM>()`                                  |
| `viewModelOf(::MyVM)`                               | `viewModel<MyVM>()`                                  |
| `scoped { MyClass(get()) }`                         | `scoped<MyClass>()`                                  |
| `scopedOf(::MyClass)`                               | `scoped<MyClass>()`                                  |
| `worker { MyWorker(get(), get(), get()) }`          | `worker<MyWorker>()`                                 |
| `singleOf(::Impl) { bind<Interface>() }`            | `single<Impl>().withOptions { bind<Interface>() }`   |
| `singleOf(::Impl) { named("x") }`                  | `single<Impl>(named("x"))`                           |
| `singleOf(::Impl) { createdAtStart() }`             | `single<Impl>().withOptions { createdAtStart() }`    |
| `single(named("x")) { Dispatchers.IO }`            | `single(named("x")) { Dispatchers.IO }` (unchanged for lambdas with custom logic) |
| `module { includes(other) }`                        | `module { includes(other) }` (unchanged)              |

**Key principle**: `single<T>()` with a reified type parameter tells the compiler plugin
to resolve `T`'s constructor parameters from the DI graph. No `get()` calls needed.

**Note**: `single(::T)` constructor reference syntax is NOT supported. Use `single<T>()`.

## Gradle Setup

### Before (Koin 3.x / manual DSL)

```kotlin
dependencies {
    implementation("io.insert-koin:koin-core:3.5.x")
    implementation("io.insert-koin:koin-android:3.5.x")
    implementation("io.insert-koin:koin-androidx-compose:3.5.x")
}
```

### After (Koin 4.x + Compiler Plugin)

```kotlin
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}

dependencies {
    implementation("io.insert-koin:koin-core:4.2.1")
    implementation("io.insert-koin:koin-android:4.2.1")
    implementation("io.insert-koin:koin-androidx-compose:4.2.1")
}

// Optional configuration
koinCompiler {
    compileSafety = true        // Validate dependency graph at compile time (default: true)
    userLogs = true             // See detected components in build output
    skipDefaultValues = true    // Skip params with default values (default: true)
}
```


### How it works

The Compiler Plugin intercepts `single<T>()` calls during Kotlin compilation and transforms
them into resolved constructor calls:

```kotlin
// What you write:
val myModule = module {
    single<MyService>()
}

// What the compiler plugin transforms it into:
val myModule = module {
    buildSingle(MyService::class, null) { scope, params ->
        MyService(scope.get(), scope.getOrNull())
    }
}
```

No generated files. No annotation processing. Just compile-time transformation.

## Singleton and Factory

### Singleton

```kotlin
// BEFORE: Old DSL
val dataModule = module {
    single { AppDatabase.create(androidContext()) }
    single { get<AppDatabase>().userDao() }
    singleOf(::UserRepository)
}
```

```kotlin
// AFTER: Safe DSL + Compiler Plugin
val dataModule = module {
    // Third-party classes with custom creation logic — keep lambda syntax
    single { AppDatabase.create(androidContext()) }
    single { get<AppDatabase>().userDao() }

    // Classes you own with standard constructors — use reified type
    single<UserRepository>()
    // Compiler resolves: UserRepository(get<ApiService>(), get<UserDao>())
}
```

### Factory

```kotlin
// BEFORE: Old DSL
val presenterModule = module {
    factory { DetailPresenter(get(), get()) }
    factoryOf(::ListPresenter)
}
```

```kotlin
// AFTER: Safe DSL
val presenterModule = module {
    factory<DetailPresenter>()
    factory<ListPresenter>()
}
```

### Created at start

```kotlin
// BEFORE: Old DSL
val appModule = module {
    single(createdAtStart = true) { AnalyticsInitializer(get()) }
}
```

```kotlin
// AFTER: Safe DSL
val appModule = module {
    single<AnalyticsInitializer>().withOptions { createdAtStart() }
}
```

## ViewModel

```kotlin
// BEFORE: Old DSL
val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModel { DetailViewModel(get(), get()) }
    viewModel { SavedStateViewModel(get(), get()) }
}
```

```kotlin
// AFTER: Safe DSL
val viewModelModule = module {
    viewModel<MainViewModel>()
    viewModel<DetailViewModel>()
    viewModel<SavedStateViewModel>()
    // SavedStateHandle is auto-resolved by Koin — no special config needed
}
```

Usage remains the same:
```kotlin
// Activity/Fragment
private val viewModel: MainViewModel by viewModel()

// Compose
val viewModel = koinViewModel<MainViewModel>()
```

### ViewModel with runtime parameters

```kotlin
// BEFORE: Old DSL
val viewModelModule = module {
    viewModel { params -> PlayerViewModel(get(), params.get()) }
}
```

```kotlin
// AFTER: Safe DSL — parameters still work the same way
val viewModelModule = module {
    viewModel<PlayerViewModel>()
    // The compiler knows which params come from DI and which from parametersOf()
    // based on @InjectedParam annotation on the ViewModel constructor
}

// The ViewModel class needs @InjectedParam on the runtime parameter:
class PlayerViewModel(
    private val repo: PlayerRepository,
    @InjectedParam private val playerId: String  // Comes from parametersOf()
) : ViewModel()

// Usage:
val viewModel: PlayerViewModel by viewModel { parametersOf("player_123") }
```

## Qualifiers and Named

### Named qualifier

```kotlin
// BEFORE: Old DSL
val dispatcherModule = module {
    single(named("io")) { Dispatchers.IO as CoroutineDispatcher }
    single(named("main")) { Dispatchers.Main as CoroutineDispatcher }
}

val repoModule = module {
    single { MyRepo(get(named("io"))) }
}
```

```kotlin
// AFTER: Safe DSL
val dispatcherModule = module {
    // Lambda syntax for inline values (no constructor to resolve)
    single(named("io")) { Dispatchers.IO as CoroutineDispatcher }
    single(named("main")) { Dispatchers.Main as CoroutineDispatcher }
}

val repoModule = module {
    // Reified type — qualifier on the constructor param via @Named
    single<MyRepo>(named("io"))
}

// MyRepo class — @Named on the constructor param tells the compiler which qualifier to use
class MyRepo(
    @Named("io") private val dispatcher: CoroutineDispatcher
)
```

### Enum qualifiers (new in 4.x)

```kotlin
enum class DispatcherType { IO, Main, Default }

val dispatcherModule = module {
    single(named(DispatcherType.IO)) { Dispatchers.IO as CoroutineDispatcher }
    single(named(DispatcherType.Main)) { Dispatchers.Main as CoroutineDispatcher }
}
```

## Scopes

### Custom scopes

```kotlin
// BEFORE: Old DSL
val sessionModule = module {
    scope<UserSession> {
        scoped { SessionManager(get()) }
        scoped { UserPreferences(get()) }
    }
}
```

```kotlin
// AFTER: Safe DSL
val sessionModule = module {
    scope<UserSession> {
        scoped<SessionManager>()
        scoped<UserPreferences>()
    }
}
```

Scope lifecycle management remains the same:
```kotlin
val scope = getKoin().createScope<UserSession>("session_id")
val manager = scope.get<SessionManager>()
scope.close()
```

### Android Scope Archetypes

For Android lifecycle scopes, use the scope archetype DSL functions:

```kotlin
// BEFORE: Old DSL with generic scope
val activityModule = module {
    scope<MyActivity> {
        scoped { ActivityPresenter(get()) }
    }
}
```

```kotlin
// AFTER: Safe DSL with scope archetypes
val activityModule = module {
    activityScope {
        scoped<ActivityPresenter>()
    }
    activityRetainedScope {
        scoped<SessionCache>()    // Survives configuration changes
    }
    fragmentScope {
        scoped<FragmentPresenter>()
    }
    viewModelScope {
        scoped<ViewModelHelper>()
    }
}
```

Usage in Activity/Fragment:
```kotlin
class MainActivity : AppCompatActivity(), KoinScopeComponent {
    override val scope: Scope by activityScope()
    private val presenter: ActivityPresenter by inject()
}

class DetailFragment : Fragment(), KoinScopeComponent {
    override val scope: Scope by fragmentScope()
    private val presenter: FragmentPresenter by inject()
}
```

## Bindings

### Single interface binding

```kotlin
// BEFORE: Old DSL
val repoModule = module {
    singleOf(::UserRepositoryImpl) { bind<UserRepository>() }
}
```

```kotlin
// AFTER: Safe DSL
val repoModule = module {
    single<UserRepositoryImpl>().withOptions { bind<UserRepository>() }
}
```

### Multiple interface bindings

```kotlin
// BEFORE: Old DSL
val module = module {
    single { ServiceImpl(get()) } bind ServiceA::class bind ServiceB::class
}
```

```kotlin
// AFTER: Safe DSL
val module = module {
    single<ServiceImpl>().withOptions {
        bind<ServiceA>()
        bind<ServiceB>()
    }
}
```

## Parameters

### Runtime parameters

For classes that need runtime values (not from the DI graph), mark them with `@InjectedParam`:

```kotlin
// The class:
class Notification(
    @InjectedParam private val title: String,
    private val sender: NotificationSender
)

// BEFORE: Old DSL
val module = module {
    factory { params -> Notification(params.get(), get()) }
}

// AFTER: Safe DSL
val module = module {
    factory<Notification>()
}
// Usage: get<Notification> { parametersOf("title") }
```

`@InjectedParam` is the **only annotation needed on classes** in the Safe DSL approach.
It tells the compiler "this parameter comes from `parametersOf()`, not from the graph."

### Nullable dependencies

```kotlin
class MyService(
    private val required: RequiredDep,
    private val optional: OptionalDep?  // Nullable — compiler uses getOrNull()
)

val module = module {
    single<MyService>()  // Compiler: MyService(get(), getOrNull())
}
```

### Parameters with default values

```kotlin
class MyService(
    private val required: RequiredDep,
    private val timeout: Int = 30  // Default value — skipped by compiler (when skipDefaultValues=true)
)

val module = module {
    single<MyService>()  // Compiler: MyService(get()) — uses Kotlin default for timeout
}
```

## Module Composition

Module composition stays the same — just update the inner declarations:

```kotlin
// BEFORE: Old DSL
val networkModule = module {
    single { OkHttpClient.Builder().build() }
    single { Retrofit.Builder().client(get()).baseUrl(URL).build() }
    single { get<Retrofit>().create(ApiService::class.java) }
}

val repositoryModule = module {
    singleOf(::UserRepository)
}

val appModule = module {
    includes(networkModule, repositoryModule)
}

startKoin {
    androidContext(this@App)
    modules(appModule)
}
```

```kotlin
// AFTER: Safe DSL
val networkModule = module {
    // Third-party types with custom creation logic — keep lambda syntax
    single { OkHttpClient.Builder().build() }
    single { Retrofit.Builder().client(get()).baseUrl(URL).build() }
    single { get<Retrofit>().create(ApiService::class.java) }
}

val repositoryModule = module {
    single<UserRepository>()  // Reified — compiler resolves constructor
}

val appModule = module {
    includes(networkModule, repositoryModule)
}

startKoin {
    androidContext(this@App)
    modules(appModule)
}
```

**When to keep lambda syntax vs use reified type:**
- `single<MyClass>()` — for classes you own with standard constructors
- `single { thirdParty.create(...) }` — for third-party classes, builder patterns, or custom creation logic
- Both are validated by the compiler plugin when `compileSafety = true`

## KMP Modules

The Safe DSL works identically across all KMP targets:

```kotlin
// commonMain
val commonModule = module {
    single<CommonRepository>()
    single<CommonUseCase>()
}

// androidMain
val androidModule = module {
    includes(commonModule)
    single { AndroidSqliteDriver(AppDb.Schema, androidContext(), "app.db") as DatabaseDriver }
}

// iosMain
val iosModule = module {
    includes(commonModule)
    single { NativeSqliteDriver(AppDb.Schema, "app.db") as DatabaseDriver }
}
```

### Platform-specific with expect/actual

```kotlin
// commonMain
expect fun platformModule(): Module

// androidMain
actual fun platformModule() = module {
    single { AndroidPlatformService() as PlatformService }
}

// iosMain
actual fun platformModule() = module {
    single { IosPlatformService() as PlatformService }
}
```

## Compose Integration

No changes on the Compose side — the Safe DSL produces the same Koin modules:

```kotlin
@Composable
fun UserScreen() {
    val viewModel = koinViewModel<UserViewModel>()
    val service = koinInject<MyService>()
}
```

The only change is inside the `module { }` blocks.

## Mixing Old and New DSL

Migration can be incremental. Old and new DSL syntax coexist in the same module:

```kotlin
val myModule = module {
    // Already migrated — reified type (compiler-validated)
    single<UserRepository>()
    factory<DetailPresenter>()
    viewModel<MainViewModel>()

    // Not yet migrated — old lambda syntax (still works)
    single { OkHttpClient.Builder().build() }
    single { Retrofit.Builder().client(get()).baseUrl(URL).build() }
}
```

Recommended migration order:
1. Start with leaf classes (simple constructors, no custom logic)
2. Move to ViewModels (biggest compile-safety win)
3. Then repositories and data layer
4. Keep lambda syntax for third-party types and builder patterns (these can't use reified type)

## Parameter Resolution

All constructor and function parameters are **handled automatically by the Koin Compiler**:
`T`, `T?`, `Lazy<T>`, `List<T>` — just declare the type, the compiler does the rest.

Use `@InjectedParam` for runtime values, `@Named("x")` for qualified dependencies.
Parameters with default values are skipped (uses Kotlin default).

```kotlin
// All parameter types handled by the Koin Compiler:
class MyService(
    private val api: ApiService,              // T — required
    private val logger: Logger?,              // T? — optional
    private val heavyDep: Lazy<HeavyService>, // Lazy<T> — deferred
    private val plugins: List<Plugin>,        // List<T> — all matching
    @Named("io") private val dispatcher: CoroutineDispatcher,  // qualified
    private val timeout: Int = 30             // default value — skipped
)

val module = module {
    single<MyService>()  // Compiler handles all the wiring
}
```

## Compile-Time Safety

The Compiler Plugin validates your Safe DSL at 3 levels:

### What gets validated

| Scenario                                   | Result                                          |
|--------------------------------------------|-------------------------------------------------|
| Non-nullable param, no definition          | **ERROR** at compile time                       |
| Nullable param (`T?`), no definition       | OK — uses `getOrNull()`                         |
| Param with default value, no definition    | OK — uses Kotlin default (when `skipDefaultValues=true`) |
| `@InjectedParam`, no definition            | OK — provided at runtime via `parametersOf()`   |
| `Lazy<T>`, no definition for T            | **ERROR** — validates inner type                |
| `@Named("x")` param, no matching qualifier | **ERROR** — with hint if unqualified exists     |
| Scoped dependency from wrong scope         | **ERROR**                                       |

### Error messages

Missing dependencies produce standard Kotlin compiler errors:

```
e: Missing definition for type 'ApiService' required by 'UserRepository'
```

### Runtime verification (optional safety net)

```kotlin
@Test
fun verifyKoinModules() {
    koinApplication {
        modules(appModule)
        checkModules()
    }
}
```

## Common Pitfalls

### 1. Using constructor reference syntax instead of reified type

```kotlin
// WRONG — constructor reference syntax is NOT supported by the compiler plugin
val module = module {
    single(::MyService)  // Will NOT be transformed
}

// RIGHT — use reified type parameter
val module = module {
    single<MyService>()
}
```

### 2. Forgetting @InjectedParam on runtime parameters

```kotlin
// WRONG — compiler treats `itemId` as a dependency to resolve from the graph
class DetailViewModel(
    private val repo: ItemRepository,
    private val itemId: String  // Will fail: no String binding in graph
) : ViewModel()

// RIGHT — mark runtime params with @InjectedParam
class DetailViewModel(
    private val repo: ItemRepository,
    @InjectedParam private val itemId: String  // Comes from parametersOf()
) : ViewModel()
```

### 3. Using reified type for third-party classes with no public constructor

```kotlin
// WRONG — Retrofit has no public constructor the compiler can resolve
val module = module {
    single<Retrofit>()  // Compiler error: can't resolve constructor
}

// RIGHT — use lambda syntax for custom creation logic
val module = module {
    single { Retrofit.Builder().client(get()).baseUrl(URL).build() }
}
```

### 4. Forgetting the compiler plugin in build.gradle.kts

```kotlin
// WRONG — single<T>() without the plugin is just a regular Koin call
// It will compile but won't have compile-time validation
dependencies {
    implementation("io.insert-koin:koin-core:4.2.1")
}

// RIGHT — add the plugin for compile-time transformation
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}
```

### 5. Mixing scopes incorrectly

```kotlin
// WRONG — viewModel<T>() inside a scope block
val module = module {
    scope<UserSession> {
        viewModel<SessionViewModel>()  // ViewModels should be top-level
    }
}

// RIGHT — use scoped<T>() inside scope blocks, viewModel<T>() at top level
val module = module {
    viewModel<SessionViewModel>()  // Top-level
    scope<UserSession> {
        scoped<SessionManager>()
    }
}
```

### 6. Missing qualification on same-type params

```kotlin
// WRONG — two CoroutineDispatcher params, compiler can't distinguish
class MyRepo(
    private val ioDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher
)

// RIGHT — use @Named to distinguish same-type params
class MyRepo(
    @Named("io") private val ioDispatcher: CoroutineDispatcher,
    @Named("main") private val mainDispatcher: CoroutineDispatcher
)
```
