# Koin Annotations + Compiler Plugin — Reference

Concrete cheat sheet for the annotations output used by Hilt / Dagger / Toothpick / Koin-KSP migrations. Cross-link target for the per-source references — they map source constructs to entries here, this file holds the Koin side.

For depth, see https://insert-koin.io.

## Setup (shared by every migration to annotations output)

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
    implementation("io.insert-koin:koin-annotations")          // BOM resolves correct version
    implementation("io.insert-koin:koin-android")              // Android
    implementation("io.insert-koin:koin-androidx-compose")     // Compose
    implementation("io.insert-koin:koin-androidx-workmanager") // Workers
}
```

**Never pin `koin-annotations` to a 2.x version** — the 2.x line is the
KSP-processed annotations artifact and is **incompatible** with the Compiler
Plugin path. Let the BOM resolve it. Likewise, the `com.google.devtools.ksp`
plugin and `koin-ksp-compiler` dependency must be **absent** — the Compiler
Plugin reads annotations directly, no KSP processor is involved.

**JSR-330 support**: the Koin Compiler also handles `jakarta.inject.*` and
`javax.inject.*` directly — `@Inject`, `@Singleton`, `@Named`, `@Qualifier`
all work as-is. Custom `@Qualifier` annotations from Dagger / Hilt / Toothpick
can be **reused without changes** in Koin annotation output.

## Imports — lookup table

| Symbol                                                                                              | Import                                                | Notes                                                            |
|-----------------------------------------------------------------------------------------------------|-------------------------------------------------------|------------------------------------------------------------------|
| `@Module`, `@Configuration`, `@ComponentScan`                                                       | `org.koin.core.annotation.*`                          | Module declaration triad                                         |
| `@Singleton`, `@Single`, `@Factory`, `@KoinViewModel`, `@KoinWorker`                                | `org.koin.core.annotation.*`                          | Binding annotations                                              |
| `@KoinApplication`                                                                                  | `org.koin.core.annotation.*`                          | Application entry point                                          |
| `@InjectedParam`, `@Named`, `@Property`, `@Qualifier`                                               | `org.koin.core.annotation.*`                          | Parameter / qualifier annotations                                |
| `@Scope`, `@Scoped`                                                                                 | `org.koin.core.annotation.*`                          | Custom scope annotations                                         |
| `@ActivityScope`, `@ActivityRetainedScope`, `@FragmentScope`, `@ViewModelScope`                     | `org.koin.core.annotation.*`                          | Android scope archetypes                                         |
| `startKoin<App>`, `koinApplication<App>`, `koinConfiguration<App>`                                  | `org.koin.plugin.module.dsl.*`                        | **Compiler-Plugin entry points** — reified, trigger auto-discovery. NOT `org.koin.core.context.startKoin` |
| `KoinApplication.module<T>()`, `KoinApplication.modules(vararg KClass)`                             | `org.koin.plugin.module.dsl.*`                        | **Annotations-only escape hatch** — use when *not* relying on `@Configuration` auto-discovery (variant modules, conditional loading, test overrides). With `@Configuration` this is unnecessary. Not for Safe DSL output. |
| `koinViewModel()`                                                                                   | `org.koin.androidx.compose.koinViewModel`             | Compose ViewModel injection                                      |
| `koinInject()`                                                                                      | `org.koin.compose.koinInject`                         | Compose generic injection                                        |
| `KoinContext`                                                                                       | `org.koin.compose.KoinContext`                        | Wraps the Compose tree                                           |
| `androidContext`, `androidLogger`                                                                   | `org.koin.android.ext.koin.*`                         | Android startKoin DSL                                            |
| `workManagerFactory`                                                                                | `org.koin.androidx.workmanager.koin.workManagerFactory` | WorkManager startKoin DSL                                      |

**Avoid:** `org.koin.android.annotation.KoinViewModel` (legacy KSP location).
In Koin 4.x every annotation lives under `org.koin.core.annotation.*`.

## Bindings

| Annotation                          | Use for                                              |
|-------------------------------------|------------------------------------------------------|
| `@Singleton` (preferred)            | Shared instance for the whole graph (JSR-330 form)   |
| `@Single`                           | Same as `@Singleton`; emit `@Singleton` instead      |
| `@Factory`                          | New instance per resolution                          |
| `@KoinViewModel`                    | Android ViewModel (lifecycle-managed)                |
| `@KoinWorker`                       | WorkManager Worker (requires `koin-androidx-workmanager`) |
| `@Scoped`                           | Lifetime tied to a Koin scope (see Scopes)           |

```kotlin
@Singleton class HttpClient(val config: HttpConfig)
@Factory   class RequestId
@KoinViewModel class HomeViewModel(repo: Repository)
@KoinWorker class SyncWorker(ctx: Context, params: WorkerParameters, repo: Repo) : CoroutineWorker(ctx, params)
```

**Auto-bind**: when a `@Singleton` / `@Factory` class implements a single
interface, the binding is registered automatically — do not emit explicit
`bind`. Only set `binds` when the class implements multiple interfaces and
you want to expose a specific subset:

```kotlin
@Singleton class RepositoryImpl : Repository, Persistable
// → not auto-bound to either interface — explicit binds needed:
@Singleton(binds = [Repository::class]) class RepositoryImpl : Repository, Persistable
```

## Modules

| Annotation                                | Use for                                       |
|-------------------------------------------|-----------------------------------------------|
| `@Module`                                 | Marks a class as a Koin module                |
| `@Configuration`                          | Marks a module as a discovery root for `@KoinApplication` (not needed on modules pulled in via `includes`) |
| `@ComponentScan("com.example.pkg")`       | Auto-discover annotated classes in a package  |
| `@Module(includes = [Other::class])`      | Compose modules explicitly                    |

**The triad** (required for cross-module discovery in multi-Gradle-module projects):

```kotlin
@Module
@Configuration
@ComponentScan("com.acme.feature.foo")
class FooFeatureModule {
    @Singleton fun analytics(): Analytics = Analytics()  // third-party / factory function
}
```

Missing any of the three silently breaks discovery (no compile error, runtime
`NoDefinitionFoundException` at first injection).

**What `@Configuration` actually marks:** a **root** of a module subtree
discovered by `@KoinApplication`. Not every `@Module` class needs it — only
the ones you want `@KoinApplication` to pick up directly. Modules composed
via `@Module(includes = [...])` are pulled in transitively and **don't need
`@Configuration`**:

```kotlin
@Module                                // included module — @Configuration NOT needed
@ComponentScan("com.acme.api")
class ApiModule

@Module
@Configuration                         // root — discovered by @KoinApplication
@ComponentScan("com.acme.feature.foo")
@Module(includes = [ApiModule::class]) // pulls ApiModule into this subtree
class FooFeatureModule
```

Rule of thumb: **every Gradle module's top-level `@Module` class gets the full
triad.** Internal helper/sub-modules composed via `includes` need only `@Module`
(plus `@ComponentScan` if they own a package to scan).

## App entry point

```kotlin
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin<App> {
            androidContext(this@App)
            androidLogger()
            // no modules(...) — Compiler Plugin auto-discovers triad-tagged modules
        }
    }
}
```

The reified `<App>` on `startKoin<T>()` is what triggers cross-module
auto-discovery. Without it (or with the classic `import org.koin.core.context.startKoin`),
the compiler plugin's discovery wiring is bypassed.

## Parameters & qualifiers

| Annotation                | Where applied                  | Replaces                              |
|---------------------------|--------------------------------|---------------------------------------|
| `@InjectedParam`          | Constructor param              | Runtime values via `parametersOf(...)` at call site |
| `@Named("auth")`          | Class or param                 | String qualifier                      |
| `@Qualifier`              | On your own annotation         | Custom type-safe qualifier            |
| `@Property("api.url")`    | Constructor param              | Inject Koin property value            |

```kotlin
@Qualifier annotation class Auth
@Singleton @Auth class AuthClient(@Property("api.url") val url: String) : HttpClient

@Factory class Connection(@InjectedParam val url: String)
// at call site: get<Connection> { parametersOf("https://...") }
```

## Scopes

| Annotation                              | Use for                                          |
|-----------------------------------------|--------------------------------------------------|
| `@Scope(MyScope::class)` + `@Scoped`    | Custom scope                                     |
| `@ActivityScope`                        | Tied to Android Activity lifecycle               |
| `@ActivityRetainedScope`                | Survives configuration changes                   |
| `@FragmentScope`                        | Tied to Fragment lifecycle                       |
| `@ViewModelScope`                       | Tied to ViewModel (cleared with VM)              |

```kotlin
@ActivityScope class TabRouter
@FragmentScope class FormState
@Scope(name = "session") @Scoped class SessionToken(@InjectedParam val userId: String)
```

## Retrieval

From an Android entry point that Koin doesn't construct (Activity / Fragment / Service):

```kotlin
class MyActivity : AppCompatActivity() {
    private val repo: Repository by inject()
    private val viewModel: HomeViewModel by viewModel()
}
```

From Compose:

```kotlin
@Composable fun Screen() {
    val repo = koinInject<Repository>()
    val vm = koinViewModel<HomeViewModel>()
}
```

From inside another Koin-managed class — **don't use `KoinComponent`**, use
constructor injection. The compiler resolves every parameter:

```kotlin
@Singleton
class FeatureCoordinator(
    private val repo: Repository,
    private val analytics: Analytics,
    private val maybeFlag: FeatureFlag?,        // optional — null if no definition
    private val plugins: List<Plugin>,          // all definitions
    private val deferred: Lazy<HeavyService>    // resolved on first access
)
```

## Manual module retrieval (escape hatch)

```kotlin
import org.koin.ksp.generated.module    // generated extension property — NOT for normal use

startKoin {
    modules(module<MyModule>())   // reified helper — for conditional/test wiring
}
```

Default flow goes through `@KoinApplication` + `@Configuration` and never needs
this. Use only when you have variant-specific or test-only modules to inject
manually.

## What this skill does NOT cover

- Full Koin DSL — see `references/koin-safe-dsl.md` (when added) or the official docs
- Multiplatform setup beyond what migrations need — see https://insert-koin.io/docs/quickstart/kmp
- Property file loading, dynamic plugins, ktor integration — see https://insert-koin.io
