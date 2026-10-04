# Dagger 2 → Koin 4.x + Koin Compiler Plugin Migration Reference

> **Setup & minimum versions** — see [`koin-annotations.md`](koin-annotations.md) → "Setup" for the standard Gradle block, JSR-330 support note, and Kotlin/Koin/Compiler Plugin minimum versions. This file holds only the source-specific deltas.

## Table of Contents
1. [Progressive Migration](#progressive-migration)
2. [Core Concept Mapping](#core-concept-mapping)
3. [Gradle Changes](#gradle-changes)
3. [Component → Module Conversion](#component-to-module)
4. [Subcomponents → Scopes](#subcomponents-to-scopes)
5. [Injection Patterns](#injection-patterns)
6. [Qualifiers](#qualifiers)
7. [Multibindings](#multibindings)
8. [Component Dependencies](#component-dependencies)
9. [Custom Scopes](#custom-scopes)
10. [Lazy and Provider](#lazy-and-provider)

---

## Progressive Migration

Do **not** rewrite existing Dagger components in place. Create a new empty Koin
module alongside the Dagger graph, move definitions one feature at a time, keep
both running side by side until the last `@Component` is gone.

Unlike Hilt, Dagger 2 has no generic entry-point accessor — resolution goes
through your own `@Component` interface methods. This makes the annotation-style
bridge (a `@Singleton fun` inside a `@Module @Configuration` class) the natural
choice; the Safe-DSL `Scope.dagger<T>()` helper from `hilt-to-koin.md` doesn't
translate cleanly.

### Bridge — co-locate `@Singleton fun` in the consumer feature's `@Module`

**Put the bridge function inside the feature module that consumes the Dagger
binding — not in a shared aggregator.** The Koin Compiler Plugin's A1
per-module verification checks each `@Module` in isolation; if the bridge
lives in a sibling module it's invisible to the consumer's A1 pass and you'd
be forced to relax the graph check. Co-location keeps strict A1 green.

Expose each bridged binding as a method on your existing `AppComponent`, retain
the component instance on the Application, and delegate from each feature
module that consumes it:

```kotlin
// Step 1 — your existing Dagger component, augmented with bridge methods
@Component(modules = [NetworkModule::class, DatabaseModule::class])
@Singleton
interface AppComponent {
    fun userDataRepository(): UserDataRepository
    fun analyticsTracker(): AnalyticsTracker
    // ...other bindings that need to bridge into Koin

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}

// Step 2 — Application owns both graphs during migration
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
class App : Application() {
    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(this)   // Dagger first
        startKoin<App> { androidContext(this@App) }                 // Koin after — order matters
    }
}

// Step 3 — each feature module declares the bridges it needs, inline
@Module
@Configuration
@ComponentScan("com.acme.feature.settings")
class SettingsFeatureModule {

    // Bridge function lives in the consumer module — A1-friendly
    @Singleton
    fun userDataRepository(context: Context): UserDataRepository =
        (context.applicationContext as App).appComponent.userDataRepository()
}

@Module
@Configuration
@ComponentScan("com.acme.feature.dashboard")
class DashboardFeatureModule {

    @Singleton
    fun analyticsTracker(context: Context): AnalyticsTracker =
        (context.applicationContext as App).appComponent.analyticsTracker()
}
```

**Ordering:** `DaggerAppComponent.factory().create(this)` must run before
`startKoin<App>`, so the Dagger graph is live when a Koin-managed class first
calls `appComponent.userDataRepository()`.

Track each bridge function as a migration TODO — delete when the target
binding moves into Koin. Remove the component methods from `AppComponent` in
lockstep. When the last bridge function is gone, the `@Component` interface,
the `AppComponent` field on `App`, and the `DaggerAppComponent.create(...)`
call in `onCreate()` all disappear together.

When multiple consumers need the same bridge:
1. **Duplicate** in each consumer (default — cheap).
2. **Migrate the source binding first** — bridges disappear naturally.
3. **Promote** to a `SharedBridgeModule` (`@Module + @Configuration`) consumed
   via `@Module(includes = [SharedBridgeModule::class])`. `includes` keeps A1
   happy; siblings don't. Promote on demand, not pre-emptively.

### Compile-time graph check

With co-located bridges, strict A1 verification stays green — each consumer
module has its bridge function locally declared. You only need to relax the
graph check if you deliberately choose a shared aggregator module pattern
instead (which adds a configuration burden without benefit; prefer
co-location).

### Convention plugin classpath

Same gotcha as Hilt — if you apply the Koin Compiler Plugin via a Gradle
convention plugin that uses `configure<KoinGradleExtension> { ... }`, the
classpath dependency must be `implementation`, not `compileOnly`. See
`hilt-to-koin.md` → "Convention plugin gotcha" for the full snippet, and the
working example at:

- https://github.com/InsertKoinIO/nowinandroid/blob/koin_plugin/build-logic/convention/src/main/kotlin/KoinConventionPlugin.kt

---

## Core Concept Mapping

| Dagger 2 Concept              | Koin Compiler Plugin Equivalent                    |
|-------------------------------|-----------------------------------------------------|
| `@Component`                  | `@Module` + `@KoinApplication` + `startKoin<T>()`  |
| `@Subcomponent`               | `@Scope(T::class)` with `@Scoped` bindings         |
| `@Module` + `@Provides`       | `@Module` class with `@Singleton`/`@Factory` functions |
| `@Module` + `@Binds`          | `@Singleton` on impl (auto-binds to interface)          |
| `@Inject constructor`         | `@Factory` on class (new instance per injection)        |
| `@Singleton`                  | `@Singleton`                                            |
| `@Reusable`                   | `@Singleton` (closest approximation)                   |
| `@Component.Factory`          | Not needed — `startKoin { }` handles init           |
| `@Component.Builder`          | Not needed — module params via `@InjectedParam`     |
| `MembersInjector`             | `by inject()` delegate or `get()` direct            |
| Custom `@Qualifier`           | **Reuse as-is** — works directly with Koin Compiler |
| JSR-330 (`javax.inject.*`)    | **Supported** — `@Inject`, `@Singleton`, `@Named`, `@Qualifier` |

## Gradle Changes

### Step 1: Remove Dagger

```kotlin
// REMOVE from build.gradle.kts
plugins {
    // id("org.jetbrains.kotlin.kapt")  ← REMOVE if only used for Dagger
}

dependencies {
    // REMOVE:
    // implementation("com.google.dagger:dagger:2.x")
    // kapt("com.google.dagger:dagger-compiler:2.x")
    // OR
    // ksp("com.google.dagger:dagger-compiler:2.x")
}
```

### Step 2: Add Koin + Compiler Plugin

```kotlin
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}

dependencies {
    implementation("io.insert-koin:koin-core:4.2.1")
    implementation("io.insert-koin:koin-android:4.2.1")
    implementation("io.insert-koin:koin-annotations:4.2.1")
}
```


## Component to Module

### Simple Component

```kotlin
// BEFORE: Dagger
@Singleton
@Component(modules = [NetworkModule::class, DatabaseModule::class])
interface AppComponent {
    fun inject(activity: MainActivity)
    fun inject(service: SyncService)

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}
```

```kotlin
// AFTER: Koin
@Module(includes = [NetworkModule::class, DatabaseModule::class])
@ComponentScan("com.myapp")
class AppModule

// In Application.onCreate()
startKoin {
    androidContext(this@App)
    modules(module<AppModule>())
}
```

### Module with @Provides

```kotlin
// BEFORE: Dagger
@Module
class NetworkModule {
    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder().client(client).baseUrl(URL).build()
}
```

```kotlin
// AFTER: Koin @Module with annotated functions
@Module
class NetworkModule {
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder().build()

    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder().client(client).baseUrl(URL).build()
}
```

### Module with @Binds

```kotlin
// BEFORE: Dagger
@Module
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindRepo(impl: RepoImpl): Repo
}
```

```kotlin
// AFTER: Koin — annotate the implementation directly
@Singleton
class RepoImpl(
    private val api: ApiService,
    private val dao: UserDao
) : Repo
// Auto-bind: single interface → compiler detects Repo binding automatically
```

## Subcomponents to Scopes

Dagger subcomponents model a lifecycle hierarchy. In Koin, use `@Scope`:

```kotlin
// BEFORE: Dagger
@Subcomponent(modules = [SessionModule::class])
@SessionScope
interface SessionComponent {
    fun inject(fragment: DashboardFragment)

    @Subcomponent.Factory
    interface Factory {
        fun create(): SessionComponent
    }
}

// Parent component exposes factory
@Component(...)
interface AppComponent {
    fun sessionComponentFactory(): SessionComponent.Factory
}
```

```kotlin
// AFTER: Koin
class SessionScopeId  // Scope marker

@Scope(SessionScopeId::class)
class SessionManager(private val api: ApiService)

@Scope(SessionScopeId::class)
class DashboardRepository(private val api: ApiService, private val db: AppDatabase)

// Open scope when session starts
val sessionScope = getKoin().createScope<SessionScopeId>("user_session")

// Inject from scope
val manager = sessionScope.get<SessionManager>()

// Close when session ends
sessionScope.close()
```

## Injection Patterns

### Field Injection → Koin Delegate

```kotlin
// BEFORE: Dagger field injection
class MyActivity : AppCompatActivity() {
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var logger: Logger

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as App).appComponent.inject(this)
        super.onCreate(savedInstanceState)
    }
}
```

```kotlin
// AFTER: Koin
class MyActivity : AppCompatActivity() {
    private val analytics: Analytics by inject()
    private val logger: Logger by inject()

    // No manual injection call needed
}
```

### Constructor Injection

```kotlin
// BEFORE: Dagger
class UserRepository @Inject constructor(
    private val api: ApiService,
    private val db: UserDao
)
```

```kotlin
// AFTER: Koin — @Inject maps to @Factory (new instance per injection)
@Factory
class UserRepository(
    private val api: ApiService,
    private val db: UserDao
)
// Constructor parameters auto-resolved at compile time.
// Use @Singleton if the Dagger class was also @Singleton scoped.
```

## Qualifiers

Custom `@Qualifier` annotations from Dagger can be **reused as-is** with the Koin Compiler.

```kotlin
// BEFORE: Dagger — these custom qualifiers work with Koin Compiler unchanged
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

@Module
object DispatcherModule {
    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

class MyRepo @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher
)
```

```kotlin
// AFTER: Koin — using @Named
@Module
class DispatcherModule {
    @Singleton
    @Named("io")
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Singleton
    @Named("main")
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

@Singleton
class MyRepo(@Named("io") private val dispatcher: CoroutineDispatcher)
```

Or with custom type-safe qualifiers:
```kotlin
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

@Module
class DispatcherModule {
    @Singleton @IoDispatcher
    fun provideIo(): CoroutineDispatcher = Dispatchers.IO
}

@Singleton
class MyRepo(@IoDispatcher private val dispatcher: CoroutineDispatcher)
```

## Multibindings

### Set multibinding

```kotlin
// BEFORE: Dagger
@Module
abstract class PluginModule {
    @Binds @IntoSet
    abstract fun bindA(a: PluginA): Plugin

    @Binds @IntoSet
    abstract fun bindB(b: PluginB): Plugin
}
```

```kotlin
// AFTER: Koin — manual assembly in @Module
@Singleton
class PluginA : Plugin { /* ... */ }

@Singleton
class PluginB : Plugin { /* ... */ }

@Module
class PluginModule {
    @Singleton
    fun providePlugins(a: PluginA, b: PluginB): Set<Plugin> = setOf(a, b)
}
```

### Map multibinding (ViewModel factory pattern)

```kotlin
// BEFORE: Dagger
@Module
abstract class ViewModelModule {
    @Binds @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHome(vm: HomeViewModel): ViewModel
}
```

```kotlin
// AFTER: Koin — just annotate each ViewModel directly
@KoinViewModel
class HomeViewModel(private val repo: HomeRepository) : ViewModel()

@KoinViewModel
class DetailViewModel(private val repo: DetailRepository) : ViewModel()
// No map needed — Koin resolves by type
```

## Component Dependencies

```kotlin
// BEFORE: Dagger — component depends on another
@Component(dependencies = [NetworkComponent::class])
interface FeatureComponent {
    fun inject(activity: FeatureActivity)
}
```

```kotlin
// AFTER: Koin — module includes
@Module(includes = [NetworkModule::class])
@ComponentScan("com.myapp.feature")
class FeatureModule
```

## Custom Scopes

```kotlin
// BEFORE: Dagger
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class ActivityScope

@ActivityScope
@Subcomponent
interface ActivitySubcomponent { ... }
```

```kotlin
// AFTER: Koin
@Scope(MyActivity::class)
class PresenterImpl(private val repo: Repo) : Presenter

@Scope(MyActivity::class)
class ActivityAnalytics(private val tracker: Tracker)

// In Activity
class MyActivity : AppCompatActivity(), KoinScopeComponent {
    override val scope: Scope by activityScope()

    private val presenter: Presenter by inject()
}
```

## Parameter Resolution

All constructor and function parameters are **handled automatically by the Koin Compiler**:
`T`, `T?`, `Lazy<T>`, `List<T>` — just declare the type.

| Dagger                        | Koin                          |
|-------------------------------|-------------------------------|
| `T`                           | `T` — required                |
| `Lazy<T>` (dagger.Lazy)      | `Lazy<T>` (kotlin.Lazy) — deferred |
| `Provider<T>` (javax)         | `List<T>` or `() -> T` lambda |
| `@Nullable T`                | `T?` — optional               |
| `@Named("x") T`              | `@Named("x") T` — qualified  |

## Lazy and Provider

```kotlin
// BEFORE: Dagger
class MyClass @Inject constructor(
    private val lazyService: Lazy<HeavyService>,     // dagger.Lazy
    private val providerService: Provider<LightService>  // javax.inject.Provider
)
```

```kotlin
// AFTER: Koin — Lazy<T> auto-resolved by compiler plugin
@Singleton
class MyClass(
    private val lazyService: Lazy<HeavyService>,       // kotlin.Lazy — auto-resolved
    private val lightServiceProvider: () -> LightService // lambda for factory-like behavior
)

// Note: For Lazy<T>, the Koin compiler plugin validates the inner type at compile time
```
