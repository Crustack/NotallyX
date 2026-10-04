# Hilt → Koin 4.x + Koin Compiler Plugin Migration Reference

> **Setup & minimum versions** — see [`koin-annotations.md`](koin-annotations.md) → "Setup" for the standard Gradle block, JSR-330 support note, and Kotlin/Koin/Compiler Plugin minimum versions. This file holds only the source-specific deltas.

## Table of Contents
1. [Progressive Migration with koin-android-dagger](#progressive-migration)
2. [Annotation Mapping](#annotation-mapping)
3. [Gradle Changes](#gradle-changes)
3. [Module Conversion](#module-conversion)
4. [Component/Entry Point Conversion](#component-conversion)
5. [ViewModel Migration](#viewmodel-migration)
6. [Scope Mapping](#scope-mapping)
7. [Assisted Inject](#assisted-inject)
8. [Multibindings](#multibindings)
9. [WorkManager](#workmanager)
10. [Compose Integration](#compose-integration)
11. [Navigation Compose Scoping](#navigation-compose-scoping)

---

## Progressive Migration

Do **not** rewrite existing Hilt/Dagger modules in place. Instead:

1. Create a new empty Koin module (e.g. `appKoinModule`) and call `startKoin { modules(appKoinModule) }`
2. Add the **`koin-android-dagger`** bridge so Koin and Hilt/Dagger can coexist
3. Move definitions from Hilt modules into the Koin module one feature at a time
4. Inject from either container at the call sites until migration is complete
5. Remove Hilt entry points, modules, and dependencies only after every binding has moved

### Option A — library bridge

```kotlin
implementation("io.insert-koin:koin-android-dagger:<version>")
```

The bridge lets Hilt-generated entry points resolve dependencies that have already moved
into Koin, and lets Koin definitions request bindings that still live in Hilt — keeping
the app buildable and runnable at every step.

### Option B — manual bridge, co-located in the consumer's `@Module` (recommended)

Put the bridge function inside the feature's own `@Module`, not a shared
aggregator. A1 per-module verification only sees the module's own bindings;
sibling aggregators are invisible.

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserDataEntryPoint {
    fun userDataRepository(): UserDataRepository
}

@Module
@Configuration
@ComponentScan("com.acme.feature.settings")
class SettingsFeatureModule {
    @Singleton
    fun userDataRepository(context: Context): UserDataRepository =
        EntryPointAccessors.fromApplication(context, UserDataEntryPoint::class.java)
            .userDataRepository()
}

@KoinViewModel
class SettingsViewModel(private val userData: UserDataRepository) : ViewModel()
```

When the binding migrates into Koin, delete the bridge function and the
`@EntryPoint` interface from this module — blast radius stays local.

When multiple consumers need the same bridge:
1. **Duplicate** in each consumer (default — cheap).
2. **Migrate `UserDataRepository` first** — bridges disappear naturally.
3. **Promote** to a `SharedBridgeModule` (`@Module + @Configuration`) consumed
   via `@Module(includes = [SharedBridgeModule::class])`. `includes` keeps A1
   happy; siblings don't. Promote on demand, not pre-emptively.

### Option C — `Scope.dagger<T>()` helper (for Safe DSL modules)

If your migration output style is Safe DSL (`module { }` blocks) rather than
annotations, a scope helper is cleaner than a function bridge:

```kotlin
inline fun <reified T> Scope.dagger(): T =
    EntryPoints.get(androidContext().applicationContext, T::class.java)
```

Used inside the feature's own Koin `module { }` (same co-location principle):

```kotlin
val settingsModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(dagger<SomeHiltBinding>()) }
}
```

Requires an `@EntryPoint` interface exposing the binding, and `androidContext(...)`
configured in `startKoin { }`. Every `dagger<T>()` call is a migration TODO —
remove when the target moves into Koin. Keep `dagger<T>()` calls inside the
consumer module to stay friendly with A1 verification.

### Compile-time graph check during bridging

**With co-located bridges (Option B or C as described), strict A1 verification
stays green — you don't need to relax anything.** Every consumer module has
its bridge function locally declared, so A1's per-module check finds it.

Only relax the graph check if you explicitly choose an aggregator pattern
(shared `HiltBridgeModule` feeding multiple consumers) — which is a
configuration you're taking on deliberately, not a migration requirement.

---

## Annotation Mapping

| Hilt Annotation              | Koin Compiler Plugin Equivalent                       |
|------------------------------|-------------------------------------------------------|
| `@HiltAndroidApp`           | `@KoinApplication` + `startKoin<App>()`               |
| `@AndroidEntryPoint`        | Remove — use `by inject()` or `by viewModel()`       |
| `@Inject constructor(...)`  | `@Factory` on the class (new instance per injection)  |
| `@Module`                   | `@Module` class with `@ComponentScan` or `includes`  |
| `@InstallIn(SingletonComponent::class)` | `@Singleton` on each binding              |
| `@InstallIn(ActivityComponent::class)` | `@Scope(Activity::class)` on bindings     |
| `@InstallIn(ViewModelComponent::class)` | `@KoinViewModel` on ViewModel class      |
| `@Provides`                 | `@Singleton` / `@Factory` on a function in `@Module`  |
| `@Binds`                    | `@Singleton` on impl class (auto-binds to interface)  |
| `@Singleton`                | `@Singleton`                                           |
| `@ViewModelScoped`          | `@KoinViewModel` (inherently scoped)                  |
| `@ActivityScoped`           | `@Scope(Activity::class)` + `@Scoped`                |
| `@Qualifier` / `@Named`    | **Reuse as-is** — custom `@Qualifier` annotations work directly |
| `@ViewModelScoped`          | `@KoinViewModel` (inherently scoped)                   |
| `@AssistedInject`           | `@InjectedParam` on runtime parameters                 |
| `@HiltWorker`               | `@KoinWorker`                                          |


## Gradle Changes

### Step 1: Remove Hilt dependencies

```kotlin
// REMOVE from build.gradle.kts (app)
plugins {
    // id("dagger.hilt.android.plugin")  ← REMOVE
    // id("com.google.devtools.ksp")     ← REMOVE if only used for Hilt
}

dependencies {
    // REMOVE all of these:
    // implementation("com.google.dagger:hilt-android:2.x")
    // ksp("com.google.dagger:hilt-compiler:2.x")
    // implementation("androidx.hilt:hilt-navigation-compose:1.x")
    // implementation("androidx.hilt:hilt-work:1.x")
    // ksp("androidx.hilt:hilt-compiler:1.x")
}
```

### Step 2: Add Koin + Compiler Plugin

```kotlin
// ADD to build.gradle.kts (app)
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}

dependencies {
    implementation("io.insert-koin:koin-core:4.2.1")
    implementation("io.insert-koin:koin-android:4.2.1")
    implementation("io.insert-koin:koin-annotations:4.2.1")

    // If using Compose:
    implementation("io.insert-koin:koin-androidx-compose:4.2.1")

    // If using WorkManager:
    implementation("io.insert-koin:koin-androidx-workmanager:4.2.1")
}
```


## Module Conversion

### Hilt @Provides module → Koin @Module

```kotlin
// BEFORE: Hilt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .client(client)
            .baseUrl(BASE_URL)
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
```

```kotlin
// AFTER: Koin Compiler Plugin
@Module
class NetworkModule {
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder().build()

    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .client(client)
            .baseUrl(BASE_URL)
            .build()

    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
```

### Hilt @Binds module → Koin annotation with binds

```kotlin
// BEFORE: Hilt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
```

```kotlin
// AFTER: Koin — annotate the implementation directly
@Singleton
class UserRepositoryImpl(
    private val api: ApiService,
    private val dao: UserDao
) : UserRepository
// Auto-bind: implements single interface → binding auto-detected by compiler.
// No explicit binds needed. No separate module class needed — @ComponentScan discovers this.
```

### Hilt @Inject constructor → Koin @Factory

`@Inject` in Hilt means "this class can be created by the DI framework" — it creates a new instance
each time by default. This maps to `@Factory` in Koin. Use `@Singleton` only when Hilt also has `@Singleton`.

```kotlin
// BEFORE: Hilt — @Inject without @Singleton = new instance each time
class UserRepository @Inject constructor(
    private val api: ApiService,
    private val dao: UserDao
)
```

```kotlin
// AFTER: Koin — @Inject maps to @Factory
@Factory
class UserRepository(
    private val api: ApiService,
    private val dao: UserDao
)
// Dependencies auto-resolved from the Koin graph at compile time.
// If the original Hilt class had @Singleton, use @Singleton instead of @Factory.
```

## Parameter Resolution

All constructor and function parameters are **handled automatically by the Koin Compiler**:
`T`, `T?`, `Lazy<T>`, `List<T>` — just declare the type, the compiler does the rest.

Use `@InjectedParam` for runtime values, `@Named("x")` for qualified dependencies.

```kotlin
// All parameter types handled by the Koin Compiler:
@Singleton
class MyService(
    private val api: ApiService,              // T — required
    private val logger: Logger?,              // T? — optional
    private val heavyDep: Lazy<HeavyService>, // Lazy<T> — deferred
    private val plugins: List<Plugin>,        // List<T> — all matching
    @Named("io") private val dispatcher: CoroutineDispatcher  // qualified
)
```

## Component Conversion

Hilt components are implicit (generated). Koin uses `@Module` composition + `@KoinApplication`:

```kotlin
// BEFORE: Hilt — component hierarchy is auto-generated
// @HiltAndroidApp generates SingletonComponent → ActivityComponent → FragmentComponent
@HiltAndroidApp
class App : Application()
```

```kotlin
// AFTER: Koin — auto-discovery via the @Module + @Configuration + @ComponentScan triad
// Each Gradle module declares its own module class with the triad; the app just needs
// @KoinApplication. No modules(...) call required.

@Module
@Configuration
@ComponentScan("com.acme.app")
class AppModule

// Application — @KoinApplication + reified startKoin<App>
import org.koin.plugin.module.dsl.startKoin   // NOT org.koin.core.context.startKoin

@KoinApplication
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin<App> {
            androidContext(this@App)
            androidLogger()
        }
    }
}
```

### Dual-container Application (Hilt + Koin coexisting during migration)

While migration is in progress, the `Application` class owns both graphs:

```kotlin
import org.koin.plugin.module.dsl.startKoin

@HiltAndroidApp
@KoinApplication
class App : Application() {
    override fun onCreate() {
        super.onCreate()                              // Hilt graph ready after this
        startKoin<App> { androidContext(this@App) }   // Koin after — order matters
    }
}
```

**Ordering is critical:** `super.onCreate()` must run first so the Hilt graph is
live by the time Koin starts. Any `@Singleton fun` bridge function (Option B
above) calling `EntryPointAccessors.fromApplication(...)` needs a fully-initialized
Hilt graph underneath.

### Convention plugin gotcha

If you apply the Koin Compiler Plugin through a Gradle convention plugin that
uses `configure<KoinGradleExtension> { ... }`, the classpath dependency must be
`implementation`, **not** `compileOnly`:

```kotlin
// convention plugin's build.gradle.kts — WRONG
dependencies {
    compileOnly(libs.koin.compiler.gradlePlugin)
}
// Fails at runtime: NoClassDefFoundError: org/koin/compiler/plugin/KoinGradleExtension

// RIGHT
dependencies {
    implementation(libs.koin.compiler.gradlePlugin)
}
```

The extension class needs to be on the runtime classpath of the convention
plugin itself because the `configure { }` block executes at build configuration time.

**Working example:** the Koin team maintains a Now-in-Android fork on a `koin_plugin`
branch that includes a fully-wired `KoinConventionPlugin`. Use it as the
reference when porting a multi-module Android/KMP project's convention-plugin
setup:

- https://github.com/InsertKoinIO/nowinandroid/blob/koin_plugin/build-logic/convention/src/main/kotlin/KoinConventionPlugin.kt

### Entry Points

```kotlin
// BEFORE: Hilt
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var analytics: Analytics
    private val viewModel: MainViewModel by viewModels()
}
```

```kotlin
// AFTER: Koin
class MainActivity : AppCompatActivity() {
    private val analytics: Analytics by inject()
    private val viewModel: MainViewModel by viewModel()
}
```

## ViewModel Migration

```kotlin
// BEFORE: Hilt
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: ItemRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel()

// Usage in Activity:
private val viewModel: DetailViewModel by viewModels()
// Usage in Compose:
val viewModel: DetailViewModel = hiltViewModel()
```

```kotlin
// AFTER: Koin Compiler Plugin
@KoinViewModel
class DetailViewModel(
    private val repo: ItemRepository,
    private val savedStateHandle: SavedStateHandle  // Auto-resolved by Koin
) : ViewModel()

// Usage in Activity/Fragment:
private val viewModel: DetailViewModel by viewModel()

// Usage in Compose:
val viewModel = koinViewModel<DetailViewModel>()
```

### ViewModel with runtime parameters

```kotlin
// BEFORE: Hilt — needs @AssistedInject + @AssistedFactory
@HiltViewModel(assistedFactory = PlayerViewModel.Factory::class)
class PlayerViewModel @AssistedInject constructor(
    private val repo: PlayerRepository,
    @Assisted private val playerId: String
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(playerId: String): PlayerViewModel
    }
}
```

```kotlin
// AFTER: Koin — much simpler with @InjectedParam
@KoinViewModel
class PlayerViewModel(
    private val repo: PlayerRepository,
    @InjectedParam private val playerId: String
) : ViewModel()

// Usage:
val viewModel: PlayerViewModel by viewModel { parametersOf("player_123") }
// Or in Compose:
val viewModel = koinViewModel<PlayerViewModel> { parametersOf("player_123") }
```

## Scope Mapping

Koin provides **scope archetype annotations** that map directly to Android lifecycle scopes:

| Hilt Scope                   | Koin Scope Archetype Annotation                        | Generated DSL                    |
|------------------------------|--------------------------------------------------------|----------------------------------|
| `SingletonComponent`         | `@Singleton` (application-wide)                           | `single { ... }`                 |
| `ActivityRetainedComponent`  | `@ActivityRetainedScope`                               | `activityRetainedScope { scoped { ... } }` |
| `ActivityComponent`          | `@ActivityScope`                                       | `activityScope { scoped { ... } }` |
| `FragmentComponent`          | `@FragmentScope`                                       | `fragmentScope { scoped { ... } }` |
| `ViewModelComponent`         | `@ViewModelScope`                                      | `viewModelScope { scoped { ... } }` |
| `ServiceComponent`           | `@Scope(Service::class)` or `@Factory`                 | `scope<Service> { scoped { ... } }` |
| Custom `@DefineComponent`    | `@Scope(CustomType::class)`                            | `scope<CustomType> { scoped { ... } }` |

### Scope Archetype Imports

```kotlin
import org.koin.android.annotation.ActivityScope         // Activity lifecycle
import org.koin.android.annotation.ActivityRetainedScope  // Survives config changes
import org.koin.android.annotation.FragmentScope          // Fragment lifecycle
import org.koin.core.annotation.ViewModelScope            // ViewModel lifecycle
```

### Scope Archetype Examples

```kotlin
// BEFORE: Hilt ActivityComponent
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {
    @Provides @ActivityScoped
    fun providePresenter(repo: UserRepository): Presenter = PresenterImpl(repo)
}
```

```kotlin
// AFTER: Koin — @ActivityScope archetype
@ActivityScope
class PresenterImpl(private val repo: UserRepository) : Presenter

// In Activity — scope tied to Activity lifecycle
class MainActivity : AppCompatActivity(), KoinScopeComponent {
    override val scope: Scope by activityScope()
    private val presenter: Presenter by inject()
}
```

```kotlin
// BEFORE: Hilt ViewModelComponent
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    @Provides @ViewModelScoped
    fun provideUseCase(repo: UserRepository): GetUsersUseCase = GetUsersUseCase(repo)
}
```

```kotlin
// AFTER: Koin — @ViewModelScope archetype
@ViewModelScope
class GetUsersUseCase(private val repo: UserRepository)

// The use case is scoped to the ViewModel's lifecycle
```

```kotlin
// Activity-retained scope (survives configuration changes)
@ActivityRetainedScope
class SessionCache(private val api: ApiService)
```

```kotlin
// Fragment scope
@FragmentScope
class FragmentPresenter(private val repo: Repository)
```

### Custom Scope Example

```kotlin
// BEFORE: Hilt custom scope
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthScope

@DefineComponent(parent = SingletonComponent::class)
interface AuthComponent

@DefineComponent.Builder
interface AuthComponentBuilder {
    fun build(): AuthComponent
}
```

```kotlin
// AFTER: Koin — much simpler
class AuthScopeId  // Scope marker class

@Scope(AuthScopeId::class)
class AuthTokenProvider(private val api: ApiService)

@Scope(AuthScopeId::class)
class AuthRepository(private val api: ApiService, private val db: AppDatabase)

// Open/close scope manually
val authScope = getKoin().createScope<AuthScopeId>("auth_session")
val tokenProvider = authScope.get<AuthTokenProvider>()
// When done:
authScope.close()
```

## Assisted Inject

```kotlin
// BEFORE: Hilt Assisted Inject
class PlayerViewModel @AssistedInject constructor(
    private val repo: PlayerRepository,
    @Assisted private val playerId: String
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(playerId: String): PlayerViewModel
    }
}
```

```kotlin
// AFTER: Koin — @InjectedParam replaces @Assisted entirely
@KoinViewModel
class PlayerViewModel(
    private val repo: PlayerRepository,
    @InjectedParam private val playerId: String
) : ViewModel()

// Usage — parametersOf at the call site
val viewModel: PlayerViewModel by viewModel { parametersOf("player_123") }
```

## Multibindings

Hilt multibindings (Set/Map) don't have a direct annotation equivalent.
Use a `@Module` with a factory function:

```kotlin
// BEFORE: Hilt
@Module
@InstallIn(SingletonComponent::class)
abstract class InterceptorModule {
    @Binds @IntoSet
    abstract fun bindLogging(impl: LoggingInterceptor): Interceptor

    @Binds @IntoSet
    abstract fun bindAuth(impl: AuthInterceptor): Interceptor
}
```

```kotlin
// AFTER: Koin
@Singleton
class LoggingInterceptor : Interceptor { /* ... */ }

@Singleton
class AuthInterceptor(private val tokenProvider: AuthTokenProvider) : Interceptor { /* ... */ }

@Module
class InterceptorModule {
    @Singleton
    fun provideInterceptors(
        logging: LoggingInterceptor,
        auth: AuthInterceptor
    ): Set<Interceptor> = setOf(logging, auth)
}
```

## WorkManager

```kotlin
// BEFORE: Hilt
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ApiService
) : CoroutineWorker(context, params)
```

```kotlin
// AFTER: Koin
@KoinWorker
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val api: ApiService
) : CoroutineWorker(context, params)

// Application setup
startKoin {
    androidContext(this@App)
    workManagerFactory()
    modules(module<AppModule>())
}
```

## Compose Integration

```kotlin
// BEFORE: Hilt + Compose
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
        }
    }
}
```

```kotlin
// AFTER: Koin + Compose
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KoinContext {
                val viewModel = koinViewModel<MainViewModel>()
            }
        }
    }
}
```

## Navigation Compose Scoping

```kotlin
// BEFORE: Hilt — scoped to nav backstack entry
val viewModel: DetailViewModel = hiltViewModel()

// AFTER: Koin — scoped to nav backstack entry
val viewModel = koinViewModel<DetailViewModel>(
    viewModelStoreOwner = LocalViewModelStoreOwner.current!!
)
```

For nested navigation graphs with shared ViewModels:
```kotlin
val parentEntry = remember(navBackStackEntry) {
    navController.getBackStackEntry("parent_route")
}
val sharedViewModel = koinViewModel<SharedViewModel>(
    viewModelStoreOwner = parentEntry
)
```
