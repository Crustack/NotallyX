# Koin KSP Annotations → Koin Compiler Plugin Migration Reference

> **Setup & minimum versions** — see [`koin-annotations.md`](koin-annotations.md) → "Setup" for the standard Gradle block, JSR-330 support note, and Kotlin/Koin/Compiler Plugin minimum versions. This file holds only the source-specific deltas.

## Table of Contents
1. [Why Migrate](#why-migrate)
2. [What Changes Overview](#what-changes-overview)
3. [Step-by-Step Migration](#step-by-step-migration)
4. [Gradle Migration](#gradle-migration)
5. [Annotation Changes](#annotation-changes)
6. [DSL Syntax Changes](#dsl-syntax-changes)
7. [Module Declaration Changes](#module-declaration-changes)
8. [ViewModel Changes](#viewmodel-changes)
9. [New Features](#new-features)
10. [KMP Migration](#kmp-migration)
11. [Multi-Module Projects](#multi-module-projects)
12. [Compile-Time Safety](#compile-time-safety)
13. [Configuration Options](#configuration-options)
14. [Common Pitfalls](#common-pitfalls)
15. [Verification Checklist](#verification-checklist)

---

## Why Migrate

Projects using `koin-annotations` + `koin-ksp-compiler` (KSP-based) benefit from the Koin Compiler Plugin:

- **No KSP dependency** — removes `com.google.devtools.ksp` Gradle plugin entirely
- **No generated files** — inline transformation at compile time, no `build/generated/ksp/` files
- **Faster builds** — runs as part of standard Kotlin compilation, no separate KSP pass
- **Full KMP support** — all targets (JVM, JS, WASM, iOS, macOS, Linux, Windows) without per-target KSP boilerplate
- **Better compile-time safety** — 3-layer validation (per-module, config groups, full graph)
- **Top-level function definitions** — annotate standalone functions, not just classes
- **Cleaner DSL** — `single<T>()` with reified type parameters instead of `singleOf(::T)`
- **K2 native** — integrated with the modern Kotlin compiler


## What Changes Overview

| Aspect                       | KSP-based (Before)                              | Compiler Plugin (After)                          |
|------------------------------|--------------------------------------------------|--------------------------------------------------|
| Gradle plugin                | `com.google.devtools.ksp`                        | `io.insert-koin.compiler.plugin`                 |
| Compiler dependency          | `ksp("io.insert-koin:koin-ksp-compiler")`        | Included via plugin                              |
| Annotations library          | `io.insert-koin:koin-annotations:1.x/2.x`       | `io.insert-koin:koin-annotations:4.2.x`         |
| Generated files              | `build/generated/ksp/*.kt` files                 | **None** — inline transformation                 |
| DSL style                    | `singleOf(::MyClass)`                            | `single<MyClass>()`                              |
| Module import                | `import org.koin.ksp.generated.module`            | No special import needed                         |
| KMP setup                    | Per-target `ksp(...)` + task hacks               | Single plugin declaration                        |
| Kotlin version               | K1 and K2                                        | **K2 only** (Kotlin ≥ 2.3.20)                     |
| Top-level functions          | Not supported                                    | Fully supported with `@ComponentScan`            |
| Compile safety               | Partial (KSP-time)                               | Full 3-layer validation (A1/A2/A3)               |
| @KoinViewModel import        | `org.koin.android.annotation.KoinViewModel`      | `org.koin.core.annotation.KoinViewModel`         |

### What stays the same (no code changes)

- `@Singleton`, `@Singleton`, `@Factory`, `@Scoped`
- `@KoinViewModel` (just import path changes)
- `@Named("qualifier")`, `@Qualifier`
- `@Scope(ScopeType::class)`
- `@InjectedParam`, `@Property("key")`, `@PropertyValue`
- `@Module`, `@Module(includes = [...])`
- `@ComponentScan("package")`
- `binds = [Interface::class]` parameter
- `createdAtStart = true` parameter
- `.module` extension property on `@Module` classes

## Step-by-Step Migration

### Step 1 — Verify Prerequisites

1. **Kotlin version**: Must be 2.3.x or higher (K2 compiler)
   ```bash
   # Check your Kotlin version in gradle.properties or build.gradle.kts
   grep -r "kotlin" gradle.properties | grep version
   ```
2. **Koin version**: Must upgrade to 4.2.1 or higher
3. If you cannot upgrade Kotlin to 2.3.x, **stay on KSP** — the compiler plugin requires K2

### Step 2 — Update Gradle Configuration

**In each module's `build.gradle.kts`:**

```kotlin
// REMOVE these:
plugins {
    // id("com.google.devtools.ksp") version "..."  ← REMOVE (if only used for Koin)
}

dependencies {
    // ksp("io.insert-koin:koin-ksp-compiler:...")  ← REMOVE
}

// REMOVE the ksp { } block:
// ksp {
//     arg("KOIN_USE_COMPOSE_VIEWMODEL", "true")
//     arg("KOIN_DEFAULT_MODULE", "false")
//     arg("KOIN_CONFIG_CHECK", "true")
// }

// ADD these:
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}

dependencies {
    implementation("io.insert-koin:koin-core:4.2.1")
    implementation("io.insert-koin:koin-annotations:4.2.1")
    // Keep all other koin-android, koin-compose dependencies — update to 4.2.1
}
```

### Step 3 — Update Annotation Imports

```kotlin
// BEFORE: KSP-based
import org.koin.android.annotation.KoinViewModel
import org.koin.ksp.generated.module

// AFTER: Compiler Plugin
import org.koin.core.annotation.KoinViewModel
// Koin Compiler Plugin provides the module<T>() reified helper
```

### Step 4 — Update DSL Syntax (Optional but Recommended)

The compiler plugin introduces reified type parameter syntax:

```kotlin
// BEFORE: constructor reference style
val myModule = module {
    singleOf(::UserRepository)
    factoryOf(::DetailPresenter)
    viewModelOf(::MainViewModel)
}

// AFTER: reified type parameter style (preferred with compiler plugin)
val myModule = module {
    single<UserRepository>()
    factory<DetailPresenter>()
    viewModel<MainViewModel>()
}
```

With qualifiers:
```kotlin
// BEFORE
singleOf(::MyService) { named("production") }

// AFTER
single<MyService>(named("production"))
```

With options:
```kotlin
// BEFORE
singleOf(::MyService) {
    named("production")
    bind<Service>()
    createdAtStart()
}

// AFTER
single<MyService>(named("production")).withOptions {
    bind<Service>()
    createdAtStart()
}
```

### Step 5 — Remove Generated File References

1. Delete `build/generated/ksp/` directories
2. Remove any manual source set configuration for KSP:
   ```kotlin
   // REMOVE these lines from build.gradle.kts:
   // sourceSets.named("commonMain").configure {
   //     kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
   // }
   ```
3. Remove KSP task dependency hacks:
   ```kotlin
   // REMOVE:
   // tasks.matching { it.name.startsWith("ksp") ... }.configureEach { ... }
   ```

### Step 6 — Clean Build and Verify

```bash
./gradlew clean build
```

Fix any compilation errors (see Common Pitfalls section below).

### Step 7 — Enable Compile-Time Safety (Optional)

```kotlin
// build.gradle.kts
koinCompiler {
    compileSafety = true    // Default: true — validates dependency graph
    userLogs = true         // See what components are detected
}
```

## Gradle Migration

### JVM/Android — Before (KSP)

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.1.21-2.0.2"
}

dependencies {
    implementation("io.insert-koin:koin-core:3.5.x")
    implementation("io.insert-koin:koin-android:3.5.x")
    implementation("io.insert-koin:koin-annotations:1.4.x")
    ksp("io.insert-koin:koin-ksp-compiler:1.4.x")
    implementation("io.insert-koin:koin-androidx-compose:3.5.x")
}

ksp {
    arg("KOIN_USE_COMPOSE_VIEWMODEL", "true")
    arg("KOIN_DEFAULT_MODULE", "false")
}
```

### JVM/Android — After (Compiler Plugin)

```kotlin
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}

dependencies {
    implementation("io.insert-koin:koin-core:4.2.1")
    implementation("io.insert-koin:koin-android:4.2.1")
    implementation("io.insert-koin:koin-annotations:4.2.1")
    implementation("io.insert-koin:koin-androidx-compose:4.2.1")
}
```

### KMP — Before (KSP) — Complex!

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.1.21-2.0.2"
}

kotlin {
    sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}

dependencies {
    commonMainImplementation("io.insert-koin:koin-core:3.5.x")
    commonMainImplementation("io.insert-koin:koin-annotations:1.4.x")
    // Per-target KSP — every target needs this!
    add("kspCommonMainMetadata", "io.insert-koin:koin-ksp-compiler:1.4.x")
    add("kspAndroid", "io.insert-koin:koin-ksp-compiler:1.4.x")
    add("kspIosX64", "io.insert-koin:koin-ksp-compiler:1.4.x")
    add("kspIosArm64", "io.insert-koin:koin-ksp-compiler:1.4.x")
    add("kspIosSimulatorArm64", "io.insert-koin:koin-ksp-compiler:1.4.x")
}

// Task dependency hack
tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }
    .configureEach { dependsOn("kspCommonMainKotlinMetadata") }
```

### KMP — After (Compiler Plugin) — Simple!

```kotlin
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}

dependencies {
    commonMainImplementation("io.insert-koin:koin-core:4.2.1")
    commonMainImplementation("io.insert-koin:koin-annotations:4.2.1")
    // No per-target configuration needed!
}
```

## Annotation Changes

### Unchanged annotations (no modifications)

```kotlin
// All of these work identically:
@Singleton
class UserRepository(private val api: ApiService, private val dao: UserDao)

@Singleton
class AppDatabase(private val context: Application)

@Singleton(createdAtStart = true)
class AnalyticsInitializer(private val context: Application)

@Singleton(binds = [UserRepository::class])
class UserRepositoryImpl(private val api: ApiService) : UserRepository

@Factory
class DetailPresenter(private val repo: ItemRepository)

@Scoped
class SessionManager(private val api: ApiService)

@Scope(UserSession::class)
class UserPreferences(private val db: AppDatabase)

@Singleton
class MyRepo(@Named("io") private val dispatcher: CoroutineDispatcher)

@Module
@ComponentScan("com.myapp.data")
class DataModule

@Module(includes = [NetworkModule::class, DatabaseModule::class])
class AppModule
```

### Import path change: @KoinViewModel

```kotlin
// BEFORE (KSP)
import org.koin.android.annotation.KoinViewModel

// AFTER (Compiler Plugin)
import org.koin.core.annotation.KoinViewModel

// Usage is identical:
@KoinViewModel
class MainViewModel(private val repo: UserRepository) : ViewModel()
```

### Import path change: generated .module

```kotlin
// BEFORE (KSP) — needed explicit import
import org.koin.ksp.generated.module

startKoin {
    modules(module<AppModule>())
}

// AFTER (Compiler Plugin) — no special import needed
startKoin {
    modules(module<AppModule>())
}
```

## DSL Syntax Changes

The compiler plugin transforms reified type parameter calls into constructor resolution:

| KSP Style (Before)                             | Compiler Plugin Style (After)               |
|-------------------------------------------------|----------------------------------------------|
| `singleOf(::MyService)`                        | `single<MyService>()`                        |
| `factoryOf(::MyRepo)`                          | `factory<MyRepo>()`                          |
| `viewModelOf(::MyVM)`                          | `viewModel<MyVM>()`                          |
| `scopedOf(::MyScoped)`                         | `scoped<MyScoped>()`                         |
| `workerOf(::MyWorker)`                         | `worker<MyWorker>()`                         |
| `singleOf(::Impl) { bind<Interface>() }`       | `single<Impl>().withOptions { bind<Interface>() }` |
| `singleOf(::Impl) { named("x") }`             | `single<Impl>(named("x"))`                  |

**Note**: The old `singleOf(::Class)` syntax still works if you prefer not to change DSL code immediately. The compiler plugin supports both styles.

## Module Declaration Changes

```kotlin
// BEFORE (KSP): Module with generated module extension
@Module
@ComponentScan("com.myapp.network")
class NetworkModule

// startKoin with KSP import
import org.koin.ksp.generated.module
startKoin {
    modules(module<NetworkModule>())
}

// AFTER (Compiler Plugin): Same structure, no special import
@Module
@ComponentScan("com.myapp.network")
class NetworkModule

startKoin {
    modules(module<NetworkModule>())
}
```

## ViewModel Changes

```kotlin
// BEFORE (KSP)
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DetailViewModel(
    private val repo: ItemRepository,
    @InjectedParam private val itemId: String
) : ViewModel()

// AFTER (Compiler Plugin) — only import changes
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DetailViewModel(
    private val repo: ItemRepository,
    @InjectedParam private val itemId: String
) : ViewModel()

// Usage in Compose — identical:
val viewModel = koinViewModel<DetailViewModel> { parametersOf("item_123") }

// Usage in Fragment — identical:
private val viewModel: DetailViewModel by viewModel { parametersOf("item_123") }
```

## New Features

### Top-Level Function Definitions

The compiler plugin supports annotations on top-level functions (not possible with KSP):

```kotlin
// Top-level functions — discovered by @ComponentScan
@Singleton
fun provideDatabase(): DatabaseService = PostgresDatabase()

@Factory
fun provideCache(db: DatabaseService): CacheService = RedisCache(db)

@Singleton
@Named("http")
fun provideHttpClient(): NetworkClient = OkHttpClient()

// Module scans the package
@Module
@ComponentScan("com.example")
class AppModule
```

### @KoinApplication for Automatic Module Injection

```kotlin
// Define application entry point with module list
@KoinApplication(modules = [AppModule::class, NetworkModule::class])
object MyApp

// Modules are auto-injected — no manual module list
fun main() {
    startKoin<MyApp> {
        printLogger()
    }
}
```

### @Configuration for Cross-Module Discovery

```kotlin
// In feature module — marks module as discoverable
@Module
@ComponentScan
@Configuration
class FeatureModule

// In app module — auto-discovers @Configuration modules
@KoinApplication
object MyApp

startKoin<MyApp>()  // FeatureModule automatically included
```

### @Monitor for Kotzilla Observability

```kotlin
@Monitor
@Singleton
class UserRepository(private val api: ApiService) {
    fun getUsers(): List<User> { ... }
}
// Compiler generates a monitoring proxy wrapping all public methods
```

## KMP Migration

The biggest improvement: no per-target KSP boilerplate.

```kotlin
// commonMain — works on ALL Kotlin targets
@Module
@ComponentScan("com.myapp.shared")
class SharedModule

@Singleton
class CommonRepository(private val api: ApiClient)

// Platform modules with expect/actual
// commonMain
@Module
expect class PlatformModule

// androidMain
@Module
actual class PlatformModule {
    @Singleton
    fun providePlatform(): Platform = AndroidPlatform()
}

// iosMain
@Module
actual class PlatformModule {
    @Singleton
    fun providePlatform(): Platform = IosPlatform()
}
```

## Multi-Module Projects

### Before (KSP): Every module needed KSP config

```kotlin
// feature/build.gradle.kts
plugins {
    id("com.google.devtools.ksp")
}
dependencies {
    implementation("io.insert-koin:koin-annotations:1.4.x")
    ksp("io.insert-koin:koin-ksp-compiler:1.4.x")
}
```

### After (Compiler Plugin): Just apply the plugin

```kotlin
// feature/build.gradle.kts
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}
dependencies {
    implementation("io.insert-koin:koin-annotations:4.2.1")
}
```

### Cross-Module Discovery with @Configuration

```kotlin
// :feature-auth module
@Module
@ComponentScan("com.myapp.auth")
@Configuration("auth")
class AuthModule

// :feature-profile module
@Module
@ComponentScan("com.myapp.profile")
@Configuration("profile")
class ProfileModule

// :app module — discovers all @Configuration modules
@KoinApplication(configurations = ["auth", "profile"])
object MyApp
```

## Compile-Time Safety

The compiler plugin validates your dependency graph at 3 levels:

### A1: Per-Module Validation
Each `@Module` is validated against its own definitions + included modules.

### A2: Configuration Group Validation
Modules with the same `@Configuration` tag are validated together.

### A3: Full-Graph Validation
At the `startKoin<T>()` or `@KoinApplication` entry point, the entire graph is validated.

### What Gets Validated

| Scenario                                   | Result                                          |
|--------------------------------------------|-------------------------------------------------|
| Non-nullable param, no definition          | **ERROR**                                       |
| Nullable param (`T?`), no definition       | OK — uses `getOrNull()`                         |
| Param with default value, no definition    | OK — uses Kotlin default (when `skipDefaultValues=true`) |
| `@InjectedParam`, no definition            | OK — provided at runtime via `parametersOf()`   |
| `@Property("key")` param                  | OK — property injection                         |
| `@Provided` type, no definition            | OK — externally provided                        |
| `List<T>` param                           | OK — `getAll()` returns empty if none           |
| `Lazy<T>`, no definition for T            | **ERROR** — validates inner type                |
| `@Named("x")` param, no matching qualifier | **ERROR** — with hint if unqualified exists     |
| Scoped dependency from wrong scope         | **ERROR**                                       |

## Configuration Options

```kotlin
// build.gradle.kts
koinCompiler {
    // Enable user-facing logs (component detection, DSL interceptions)
    userLogs = true                 // Default: false

    // Enable debug logs (internal plugin processing details)
    debugLogs = true                // Default: false

    // Enable unsafe DSL checks (validates create() usage)
    unsafeDslChecks = true          // Default: true

    // Skip injection for parameters with default values
    skipDefaultValues = true        // Default: true

    // Enable compile-time dependency safety checks
    compileSafety = true            // Default: true
}
```

## Common Pitfalls

### 1. Using Kotlin < 2.3.x

```
Error: The Koin Compiler Plugin requires Kotlin ≥ 2.3.20 (K2 compiler)
```
You must upgrade Kotlin first. If you can't, stay on KSP.

### 2. Forgetting to update @KoinViewModel import

```kotlin
// WRONG — old import will not resolve
import org.koin.android.annotation.KoinViewModel

// RIGHT
import org.koin.core.annotation.KoinViewModel
```

### 3. Leftover KSP generated imports

```kotlin
// WRONG — KSP import no longer needed
import org.koin.ksp.generated.module

// RIGHT — module<T>() is a reified helper from the Koin Compiler Plugin
```

### 4. Keeping KSP plugin when not needed

```kotlin
// WRONG — both plugins, conflicts possible
plugins {
    id("com.google.devtools.ksp") version "..."     // Remove if only Koin used it
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}

// RIGHT — only keep KSP if OTHER processors need it (Room, Moshi, etc.)
plugins {
    id("io.insert-koin.compiler.plugin") version "1.0.0-RC1"
}
```

### 5. Version mismatch

```kotlin
// WRONG — old annotations with new plugin
dependencies {
    implementation("io.insert-koin:koin-annotations:1.4.0")  // Old!
}

// RIGHT — use matching versions
dependencies {
    implementation("io.insert-koin:koin-annotations:4.2.1")
}
```

### 6. KMP per-target KSP declarations left behind

```kotlin
// REMOVE all of these:
dependencies {
    add("kspCommonMainMetadata", "io.insert-koin:koin-ksp-compiler:...")
    add("kspAndroid", "io.insert-koin:koin-ksp-compiler:...")
    add("kspIosX64", "io.insert-koin:koin-ksp-compiler:...")
}

// REMOVE source set hacks:
// sourceSets.named("commonMain").configure {
//     kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
// }
```

## Verification Checklist

- [ ] Kotlin upgraded to 2.3.x+ (K2)
- [ ] Koin upgraded to 4.2.1+
- [ ] `com.google.devtools.ksp` plugin removed (or only kept for non-Koin processors)
- [ ] `ksp("io.insert-koin:koin-ksp-compiler")` dependency removed
- [ ] `ksp { }` block removed or cleaned of Koin-specific args
- [ ] `io.insert-koin.compiler.plugin` plugin added
- [ ] `koin-annotations` version updated to 4.2.x
- [ ] `@KoinViewModel` import changed to `org.koin.core.annotation`
- [ ] `import org.koin.ksp.generated.module` removed
- [ ] Per-target KSP declarations removed (KMP projects)
- [ ] KSP source set hacks removed (KMP projects)
- [ ] `build/generated/ksp/` directory deleted
- [ ] Clean build succeeds: `./gradlew clean build`
- [ ] `checkModules()` passes in tests (optional safety net)
- [ ] ViewModel injection verified in Activities, Fragments, Composables
- [ ] Scoped bindings verified
