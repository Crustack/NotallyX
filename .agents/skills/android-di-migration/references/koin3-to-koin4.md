# Koin 3.x → Koin 4.x Safe DSL + Koin Compiler Plugin Upgrade Reference

> **Setup & minimum versions** — see [`koin-safe-dsl.md`](koin-safe-dsl.md) → "Setup" for the standard Gradle block and Kotlin/Koin/Compiler Plugin minimum versions. This file holds only the source-specific deltas.

## Table of Contents
1. [Key Changes Overview](#key-changes)
2. [Gradle Migration](#gradle-migration)
3. [Compiler Plugin Setup](#compiler-plugin)
4. [Migrating to Safe DSL](#migrating-to-safe-dsl)
5. [API Changes](#api-changes)
6. [Scope Changes](#scope-changes)
7. [Compose Changes](#compose-changes)
8. [KMP Changes](#kmp-changes)
9. [Testing Changes](#testing-changes)
10. [Kotzilla Integration](#kotzilla-integration)

---

## Key Changes

Koin 4.x introduces:
- **Koin Compiler Plugin** (`io.insert-koin.compiler.plugin`) for compile-time dependency verification
- **Safe DSL** — `single<T>()`, `factory<T>()`, `viewModel<T>()` with reified type parameters, transformed by the compiler plugin
- **Zero generated files** — compiler plugin transforms inline during Kotlin compilation
- **3-layer compile safety** — A1 (per-module), A2 (config groups), A3 (full graph)
- **KMP-first** architecture — unified API across Android, iOS, Desktop, Server
- **Simplified Compose** integration via `KoinContext`, `koinViewModel()`, `koinInject()`
- **Scope archetypes** — `activityScope { }`, `fragmentScope { }`, `viewModelScope { }` DSL functions
- **Kotzilla** observability hooks built into the runtime

Your existing `module { }` structure is preserved — you just upgrade the syntax inside.

## Gradle Migration

### Before (Koin 3.x)
```kotlin
dependencies {
    implementation("io.insert-koin:koin-android:3.5.x")
    implementation("io.insert-koin:koin-androidx-compose:3.5.x")
    // Optional KSP annotations
    implementation("io.insert-koin:koin-annotations:1.x")
    ksp("io.insert-koin:koin-ksp-compiler:1.x")
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

    // REMOVE if previously using KSP:
    // implementation("io.insert-koin:koin-annotations:1.x")
    // ksp("io.insert-koin:koin-ksp-compiler:1.x")
}
```


**If you can't upgrade to Kotlin ≥ 2.3.20**, use Koin 4.x without the compiler plugin:
```kotlin
dependencies {
    implementation(platform("io.insert-koin:koin-bom:4.x.x"))
    implementation("io.insert-koin:koin-android")
    implementation("io.insert-koin:koin-androidx-compose")
    implementation("io.insert-koin:koin-core")
}
// You keep singleOf(::Class) syntax — no compile-time safety, but 4.x API changes apply
```

## Compiler Plugin Setup

```kotlin
// build.gradle.kts
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}

// Optional configuration
koinCompiler {
    compileSafety = true        // Validate dependency graph at compile time (default: true)
    userLogs = true             // See detected components in build output
    skipDefaultValues = true    // Skip params with defaults (default: true)
}
```

### What the compiler plugin does
- Transforms `single<T>()`, `factory<T>()`, `viewModel<T>()` calls into resolved constructor calls
- Validates the dependency graph at compile time (3 layers: A1/A2/A3)
- No generated files — all transformations happen inline during Kotlin compilation
- Works on all KMP targets without per-target configuration

### How transformation works

```kotlin
// What you write:
val myModule = module {
    single<UserRepository>()
}

// What the compiler plugin transforms it into:
val myModule = module {
    buildSingle(UserRepository::class, null) { scope, params ->
        UserRepository(scope.get(), scope.get())
    }
}
```

No `get()` calls to write. No annotation processing. Just compile-time transformation.

## Migrating to Safe DSL

### Step 1: Convert singleton declarations

```kotlin
// BEFORE: Koin 3.x
val networkModule = module {
    single { OkHttpClient.Builder().build() }
    single { Retrofit.Builder().client(get()).baseUrl(URL).build() }
    singleOf(::UserRepository) { bind<Repository>() }
}
```

```kotlin
// AFTER: Koin 4.x Safe DSL
val networkModule = module {
    // Third-party types with custom logic — keep lambda syntax
    single { OkHttpClient.Builder().build() }
    single { Retrofit.Builder().client(get()).baseUrl(URL).build() }

    // Classes you own — use reified type (compiler resolves constructor)
    single<UserRepository>().withOptions { bind<Repository>() }
}
```

### Step 2: Convert factory declarations

```kotlin
// BEFORE: Koin 3.x
val presenterModule = module {
    factory { DetailPresenter(get(), get()) }
    factoryOf(::ListPresenter)
    factory { params -> DataSync(get(), params.get()) }
}
```

```kotlin
// AFTER: Koin 4.x Safe DSL
val presenterModule = module {
    factory<DetailPresenter>()
    factory<ListPresenter>()
    factory<DataSync>()
}

// DataSync needs @InjectedParam on its runtime parameter:
class DataSync(
    private val repo: Repository,
    @InjectedParam private val syncId: String  // from parametersOf()
)
```

### Step 3: Convert ViewModels

```kotlin
// BEFORE: Koin 3.x
val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModel { DetailViewModel(get(), get()) }
    viewModel { params -> PlayerViewModel(get(), params.get()) }
}
```

```kotlin
// AFTER: Koin 4.x Safe DSL
val viewModelModule = module {
    viewModel<MainViewModel>()
    viewModel<DetailViewModel>()
    viewModel<PlayerViewModel>()
}

// PlayerViewModel needs @InjectedParam for its runtime param:
class PlayerViewModel(
    private val repo: PlayerRepository,
    @InjectedParam private val playerId: String
) : ViewModel()
```

### Step 4: Convert module composition

```kotlin
// BEFORE: Koin 3.x — flat module list
startKoin {
    modules(networkModule, databaseModule, repoModule, viewModelModule)
}
```

```kotlin
// AFTER: Koin 4.x — hierarchical includes (preferred)
val appModule = module {
    includes(networkModule, databaseModule, repoModule, viewModelModule)
}

startKoin {
    androidContext(this@App)
    modules(appModule)
}
```

Module structure stays the same: `val myModule = module { }`.

### Step 5: Convert qualifiers

```kotlin
// BEFORE: Koin 3.x
val dispatcherModule = module {
    single(named("io")) { Dispatchers.IO as CoroutineDispatcher }
    single(named("main")) { Dispatchers.Main as CoroutineDispatcher }
}
val repoModule = module {
    single { MyRepo(get(named("io"))) }
}
```

```kotlin
// AFTER: Koin 4.x Safe DSL
val dispatcherModule = module {
    // Lambda syntax for inline values (no constructor to resolve)
    single(named("io")) { Dispatchers.IO as CoroutineDispatcher }
    single(named("main")) { Dispatchers.Main as CoroutineDispatcher }
}
val repoModule = module {
    single<MyRepo>()
}

// MyRepo class uses @Named on the constructor param:
class MyRepo(
    @Named("io") private val dispatcher: CoroutineDispatcher
)
```

**Note**: `@Named` on constructor parameters is the one annotation needed on classes
for qualifier resolution. `@InjectedParam` is the other for runtime parameters.
Otherwise classes stay annotation-free.

### Step 6: Convert binding options

```kotlin
// BEFORE: Koin 3.x
val module = module {
    singleOf(::UserRepositoryImpl) { bind<UserRepository>() }
    singleOf(::ServiceImpl) {
        bind<ServiceA>()
        bind<ServiceB>()
        createdAtStart()
    }
}
```

```kotlin
// AFTER: Koin 4.x Safe DSL
val module = module {
    single<UserRepositoryImpl>().withOptions { bind<UserRepository>() }
    single<ServiceImpl>().withOptions {
        bind<ServiceA>()
        bind<ServiceB>()
        createdAtStart()
    }
}
```

## Parameter Resolution

All constructor and function parameters are **handled automatically by the Koin Compiler**:
`T`, `T?`, `Lazy<T>`, `List<T>` — just declare the type, the compiler does the rest.

| 3.x (manual wiring)              | 4.x (Koin Compiler handles it)           |
|-----------------------------------|-------------------------------------------|
| `single { MyClass(get()) }`      | `single<MyClass>()` — `T` auto-resolved  |
| `single { MyClass(getOrNull()) }`| `single<MyClass>()` — `T?` auto-resolved |
| `single { MyClass(lazy { get() }) }` | `single<MyClass>()` — `Lazy<T>` auto-resolved |
| `single { MyClass(getAll()) }`   | `single<MyClass>()` — `List<T>` auto-resolved |
| `single { MyClass(get(named("x"))) }` | `single<MyClass>()` — `@Named("x")` on param |
| `factory { p -> MyClass(p.get()) }` | `factory<MyClass>()` — `@InjectedParam` on param |

No more writing `get()`, `getOrNull()`, `getAll()`, `get(named(...))` etc.

## API Changes

### DSL syntax comparison table

| Koin 3.x                                       | Koin 4.x Safe DSL                                    |
|-------------------------------------------------|-------------------------------------------------------|
| `single { MyClass(get()) }`                     | `single<MyClass>()`                                   |
| `singleOf(::MyClass)`                           | `single<MyClass>()`                                   |
| `factory { MyClass(get()) }`                    | `factory<MyClass>()`                                  |
| `factoryOf(::MyClass)`                          | `factory<MyClass>()`                                  |
| `viewModel { MyVM(get()) }`                     | `viewModel<MyVM>()`                                   |
| `viewModelOf(::MyVM)`                           | `viewModel<MyVM>()`                                   |
| `singleOf(::Impl) { bind<Iface>() }`           | `single<Impl>().withOptions { bind<Iface>() }`        |
| `singleOf(::Impl) { named("x") }`              | `single<Impl>(named("x"))`                            |
| `scope(named<MyActivity>()) { scoped {...} }`   | `scope<MyActivity> { scoped<MyClass>() }`             |
| `single { thirdParty.create() }`               | `single { thirdParty.create() }` (unchanged)          |

### Enum qualifiers (new in 4.x)

```kotlin
enum class DispatcherType { IO, Main, Default }
single(named(DispatcherType.IO)) { Dispatchers.IO }
```

## Scope Changes

### Basic scope syntax

```kotlin
// 3.x
scope(named<MyActivity>()) {
    scoped { Presenter(get()) }
}

// 4.x Safe DSL
scope<MyActivity> {
    scoped<Presenter>()
}
```

### Android Scope Archetypes (new in 4.x)

Koin 4.x introduces scope archetype DSL functions that bind to real Android lifecycle scopes:

```kotlin
// BEFORE: Koin 3.x — generic scope
val activityModule = module {
    scope<MyActivity> {
        scoped { ActivityPresenter(get()) }
    }
}
```

```kotlin
// AFTER: Koin 4.x — scope archetypes
val activityModule = module {
    // Activity-scoped (tied to Activity lifecycle)
    activityScope {
        scoped<ActivityPresenter>()
    }

    // Activity-retained (survives configuration changes)
    activityRetainedScope {
        scoped<SessionCache>()
    }

    // Fragment-scoped
    fragmentScope {
        scoped<FragmentPresenter>()
    }

    // ViewModel-scoped
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

### Custom scopes

```kotlin
// 3.x
val sessionModule = module {
    scope<UserSession> {
        scoped { SessionManager(get()) }
    }
}

// 4.x Safe DSL
val sessionModule = module {
    scope<UserSession> {
        scoped<SessionManager>()
    }
}

// Lifecycle unchanged:
val scope = getKoin().createScope<UserSession>("session_id")
val manager = scope.get<SessionManager>()
scope.close()
```

## Compose Changes

### ViewModel in Compose

```kotlin
// 3.x
import org.koin.androidx.compose.getViewModel
val viewModel = getViewModel<MainViewModel>()

// 4.x
import org.koin.androidx.compose.koinViewModel
val viewModel = koinViewModel<MainViewModel>()
```

### Compose application setup

```kotlin
// 3.x
@Composable
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        MainScreen()
    }
}

// 4.x
@Composable
fun App() {
    KoinContext {
        MainScreen()
    }
}
// Note: startKoin {} is called in Application.onCreate()
// KoinContext provides the Koin instance to the Compose tree
```

### Inject in Composables

```kotlin
// 3.x
@Composable
fun MyScreen() {
    val service = get<MyService>()  // Direct resolution
}

// 4.x
@Composable
fun MyScreen() {
    val service = koinInject<MyService>()  // Compose-aware resolution
}
```

## KMP Changes

Koin 4.x is KMP-first. The compiler plugin works on all targets with a single declaration:

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

## Testing Changes

```kotlin
// 3.x and 4.x — similar test setup
class MyTest : KoinTest {
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(testModule)
    }
}

// 4.x — compile-time verification replaces runtime checks
// The compiler plugin catches missing deps at build time.
// Runtime checkModules() still available as safety net:
@Test
fun verifyModules() {
    koinApplication {
        modules(appModule)
        checkModules()
    }
}
```

## Kotzilla Integration

Koin 4.x includes hooks for Kotzilla observability:

```kotlin
dependencies {
    implementation("io.kotzilla:kotzilla-sdk:x.x.x")
}

startKoin {
    androidContext(this@App)
    kotzilla("YOUR_PROJECT_KEY")
    modules(appModule)
}
```

Kotzilla automatically instruments:
- Module resolution times
- Dependency graph visualization
- ViewModel lifecycle tracking
- Scope creation/destruction events
- Anomaly detection on injection patterns
