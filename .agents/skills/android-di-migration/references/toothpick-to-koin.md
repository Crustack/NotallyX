# Toothpick → Koin 4.x + Koin Compiler Plugin Migration Reference

> **Setup & minimum versions** — see [`koin-annotations.md`](koin-annotations.md) → "Setup" for the standard Gradle block, JSR-330 support note, and Kotlin/Koin/Compiler Plugin minimum versions. This file holds only the source-specific deltas.

## Table of Contents
1. [Progressive Migration](#progressive-migration)
2. [Core Concept Mapping](#core-concept-mapping)
3. [Gradle Changes](#gradle-changes)
3. [Scope Tree → Koin Scopes](#scope-tree)
4. [Module Conversion](#module-conversion)
5. [Injection Patterns](#injection-patterns)
6. [Scope Lifecycle](#scope-lifecycle)
7. [Providers and Lazy](#providers-and-lazy)
8. [ViewModel Migration](#viewmodel-migration)
9. [Testing](#testing)
10. [Migration Checklist](#migration-checklist)

---

## Progressive Migration

Do **not** rewrite Toothpick scopes in place. Create a new Koin module alongside
the existing Toothpick scope tree, move bindings one feature/scope at a time,
keep both running until the root `Toothpick.openRootScope()` is gone.

For Toothpick-owned bindings consumed from a Koin definition during the bridging
window, write a `Scope.toothpick<T>()` helper that delegates to the live
Toothpick scope (`Toothpick.openScope(scope).getInstance(T::class.java)`) — same
pattern as `Scope.dagger<T>()` in `hilt-to-koin.md`.

**Co-locate bridge helpers in the consumer module** (or its `@Module @Configuration`
class for annotations output) — strict A1 verification then stays green and no
compile-check relaxation is needed. See SKILL.md → "Bridging with the existing DI"
for the full rationale.

---

## Core Concept Mapping

| Toothpick Concept                  | Koin Compiler Plugin Equivalent                     |
|------------------------------------|------------------------------------------------------|
| `KTP.openScope(name)`             | `getKoin().createScope<T>(id)`                       |
| `KTP.closeScope(name)`            | `scope.close()`                                      |
| `scope.getInstance(Class)`        | `scope.get<Class>()` or `by inject()`                |
| `@Inject` field injection         | `by inject()` delegate                               |
| `@Inject` constructor injection   | `@Factory` on the class (new instance per injection)    |
| `@Singleton`                      | `@Singleton`                                            |
| `@ProvidesSingleton`              | `@Singleton` on `@Module` function                      |
| `@ScopeAnnotation`                | `@Scope(T::class)` + `@Scoped`                       |
| `Module` (Toothpick)              | `@Module` class with annotated functions              |
| `bind(X).to(Y)`                   | `@Singleton` on Y implementing X (auto-bind)            |
| `bind(X).toInstance(obj)`          | `@Singleton` function in `@Module` returning obj         |
| `bind(X).toProvider(P)`           | `@Factory` function in `@Module`                      |
| `bind(X).withName(Named("n"))`    | `@Named("n")` on binding                             |
| `Toothpick.inject(this, scope)`   | Remove — use `by inject()` delegate                  |
| `@InjectConstructor`              | `@Factory` on the class (new instance per injection)    |
| `ScopeSingleton`                  | `@Scope(T::class)` + class annotation                |
| `Releasable`                      | Not needed — `scope.close()` handles cleanup          |
| `smoothie` (Android lifecycle)    | Koin scope archetypes: `@ActivityScope`, `@FragmentScope` |
| Custom `@Qualifier`               | **Reuse as-is** — works directly with Koin Compiler |
| JSR-330 (`javax.inject.*`)        | **Supported** — `@Inject`, `@Singleton`, `@Named`, `@Qualifier` work as-is |


## Gradle Changes

### Step 1: Remove Toothpick

```kotlin
// REMOVE from build.gradle.kts
plugins {
    // id("org.jetbrains.kotlin.kapt")  ← REMOVE if only used for Toothpick
}

dependencies {
    // REMOVE all of these:
    // implementation("com.github.stephanenicolas.toothpick:toothpick-runtime:3.x")
    // implementation("com.github.stephanenicolas.toothpick:smoothie-androidx:3.x")
    // kapt("com.github.stephanenicolas.toothpick:toothpick-compiler:3.x")
    // testImplementation("com.github.stephanenicolas.toothpick:toothpick-testing-junit5:3.x")
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

    // If using Compose:
    implementation("io.insert-koin:koin-androidx-compose:4.2.1")

    // Testing:
    testImplementation("io.insert-koin:koin-test-junit5:4.2.1")
}
```


## Scope Tree

Toothpick's distinguishing feature is its hierarchical scope tree.
Each scope can see bindings from its parent scopes. In Koin, this maps to
`@Singleton` for global bindings + `@Scope` for lifecycle-bound bindings.

### Toothpick scope tree

```kotlin
// BEFORE: Toothpick scope hierarchy
// Application scope (root)
//   └── Activity scope
//         └── Fragment scope

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val appScope = KTP.openRootScope()
        appScope.installModules(AppModule())
    }
}

class MainActivity : AppCompatActivity() {
    @Inject lateinit var analytics: Analytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scope = KTP.openScopes(application, this)
        scope.installModules(ActivityModule())
        Toothpick.inject(this, scope)
    }

    override fun onDestroy() {
        super.onDestroy()
        KTP.closeScope(this)
    }
}

class DetailFragment : Fragment() {
    @Inject lateinit var presenter: DetailPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scope = KTP.openScopes(application, activity, this)
        scope.installModules(FragmentModule())
        Toothpick.inject(this, scope)
    }

    override fun onDestroy() {
        super.onDestroy()
        KTP.closeScope(this)
    }
}
```

### Koin equivalent with annotations

```kotlin
// AFTER: Koin — annotated classes + scoping

// Application-level singletons (replaces root scope)
@Singleton
class Analytics(private val context: Application)

@Singleton
class ApiService(/* ... */)

@Singleton
class AppDatabase(private val context: Application)

// Activity-scoped bindings — use @ActivityScope archetype
@ActivityScope
class ActivityNavigator(private val analytics: Analytics)

// Fragment-scoped bindings — use @FragmentScope archetype
@FragmentScope
class DetailPresenter(private val api: ApiService, private val db: AppDatabase)

// Module for auto-discovery
@Module
@ComponentScan("com.myapp")
class AppModule

// Application setup
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(module<AppModule>())
        }
    }
}

// Activity — no manual inject call needed
class MainActivity : AppCompatActivity() {
    private val analytics: Analytics by inject()
}

// Fragment — scoped injection
class DetailFragment : Fragment(), KoinScopeComponent {
    override val scope: Scope by fragmentScope()
    private val presenter: DetailPresenter by inject()
}
```

Key differences:
- Toothpick's hierarchical lookup (child sees parent bindings) → Koin `@Singleton` bindings are globally visible
- Scoped bindings use `@Scope(T::class)` and are only accessible within that scope
- No manual `inject(this, scope)` call — Koin delegates handle injection automatically
- No manual `closeScope()` — `KoinScopeComponent` ties scope to Android lifecycle

## Module Conversion

### Toothpick Module with bind()

```kotlin
// BEFORE: Toothpick
class NetworkModule : Module() {
    init {
        bind(OkHttpClient::class.java)
            .toInstance(OkHttpClient.Builder().build())
        bind(Retrofit::class.java)
            .toProviderInstance(RetrofitProvider())
            .providesSingleton()
        bind(ApiService::class.java)
            .to(ApiServiceImpl::class.java)
            .singleton()
    }
}

class RetrofitProvider @Inject constructor(
    private val client: OkHttpClient
) : Provider<Retrofit> {
    override fun get(): Retrofit =
        Retrofit.Builder()
            .client(client)
            .baseUrl(BASE_URL)
            .build()
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
        Retrofit.Builder()
            .client(client)
            .baseUrl(BASE_URL)
            .build()
}

// ApiServiceImpl can be annotated directly since you own it
@Singleton
class ApiServiceImpl(private val retrofit: Retrofit) : ApiService
// Auto-bind: single interface → compiler detects ApiService binding automatically

// RetrofitProvider class is no longer needed — @Module function replaces it
```

### Named bindings

```kotlin
// BEFORE: Toothpick
class DispatcherModule : Module() {
    init {
        bind(CoroutineDispatcher::class.java)
            .withName(Named("io"))
            .toInstance(Dispatchers.IO)
        bind(CoroutineDispatcher::class.java)
            .withName(Named("main"))
            .toInstance(Dispatchers.Main)
    }
}
```

```kotlin
// AFTER: Koin
@Module
class DispatcherModule {
    @Singleton @Named("io")
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Singleton @Named("main")
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
```

### Factory bindings (non-singleton)

```kotlin
// BEFORE: Toothpick
class PresenterModule : Module() {
    init {
        bind(DetailPresenter::class.java)  // default is non-singleton
            .to(DetailPresenterImpl::class.java)
    }
}
```

```kotlin
// AFTER: Koin — annotate the implementation
@Factory
class DetailPresenterImpl(private val repo: ItemRepository) : DetailPresenter
// Auto-bind: single interface → compiler detects DetailPresenter binding automatically
```

## Injection Patterns

### Field injection → Koin delegate

```kotlin
// BEFORE: Toothpick
class ProfileActivity : AppCompatActivity() {
    @Inject lateinit var userRepo: UserRepository
    @Inject lateinit var imageLoader: ImageLoader
    @Inject @Named("main") lateinit var dispatcher: CoroutineDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scope = KTP.openScopes(application, this)
        Toothpick.inject(this, scope)
    }
}
```

```kotlin
// AFTER: Koin
class ProfileActivity : AppCompatActivity() {
    private val userRepo: UserRepository by inject()
    private val imageLoader: ImageLoader by inject()
    private val dispatcher: CoroutineDispatcher by inject(named("main"))
}
```

### Constructor injection

```kotlin
// BEFORE: Toothpick — @InjectConstructor = factory behavior (new instance)
@InjectConstructor
class UserRepository(
    private val api: ApiService,
    private val dao: UserDao,
    @Named("io") private val dispatcher: CoroutineDispatcher
)
```

```kotlin
// AFTER: Koin — @InjectConstructor maps to @Factory
@Factory
class UserRepository(
    private val api: ApiService,
    private val dao: UserDao,
    @Named("io") private val dispatcher: CoroutineDispatcher
)
```

## Scope Lifecycle

### Activity-scoped pattern

```kotlin
// BEFORE: Toothpick — manual scope open/close
class ChatActivity : AppCompatActivity() {
    @Inject lateinit var chatManager: ChatManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scope = KTP.openScopes(application, this)
        scope.installModules(object : Module() {
            init {
                bind(ChatManager::class.java).singleton()
            }
        })
        Toothpick.inject(this, scope)
    }

    override fun onDestroy() {
        super.onDestroy()
        KTP.closeScope(this)
    }
}
```

```kotlin
// AFTER: Koin — scope tied to Activity lifecycle automatically
@Scope(ChatActivity::class)
class ChatManager(private val api: ApiService)

class ChatActivity : AppCompatActivity(), KoinScopeComponent {
    override val scope: Scope by activityScope()
    private val chatManager: ChatManager by inject()
    // No onDestroy cleanup needed — activityScope() handles it
}
```

### Custom scope (e.g., user session)

```kotlin
// BEFORE: Toothpick
object ScopeNames {
    const val USER_SESSION = "USER_SESSION"
}

// Open on login
val sessionScope = KTP.openScope(ScopeNames.USER_SESSION)
sessionScope.installModules(SessionModule())

// Close on logout
KTP.closeScope(ScopeNames.USER_SESSION)
```

```kotlin
// AFTER: Koin
class UserSessionScope  // Scope marker

@Scope(UserSessionScope::class)
class SessionManager(private val api: ApiService)

@Scope(UserSessionScope::class)
class UserPreferences(private val db: AppDatabase)

// Open on login
val sessionScope = getKoin().createScope<UserSessionScope>("user_session")

// Access scoped instances
val session = sessionScope.get<SessionManager>()

// Close on logout
sessionScope.close()
```

## Providers and Lazy

```kotlin
// BEFORE: Toothpick
class MyService @Inject constructor(
    private val heavyDep: Lazy<HeavyDependency>,     // toothpick Lazy
    private val providerDep: Provider<LightDependency> // javax Provider
) {
    fun doWork() {
        val heavy = heavyDep.get()        // resolved on first access
        val light = providerDep.get()     // new instance each call
    }
}
```

```kotlin
// AFTER: Koin — Lazy<T> natively supported by compiler plugin
@Singleton
class MyService(
    private val heavyDep: Lazy<HeavyDependency>,    // kotlin.Lazy — auto-resolved
    private val lightDepFactory: () -> LightDependency  // lambda for factory
) {
    fun doWork() {
        val heavy = heavyDep.value       // kotlin.Lazy
        val light = lightDepFactory()    // new instance each call
    }
}
// The compiler plugin validates Lazy<T> inner types at compile time
```

## ViewModel Migration

Toothpick doesn't have built-in ViewModel support. Projects typically use a custom
ViewModelProvider.Factory:

```kotlin
// BEFORE: Toothpick + manual factory
class MainViewModel @Inject constructor(
    private val repo: UserRepository
) : ViewModel()

class ViewModelFactory @Inject constructor(
    private val viewModelProviders: Map<Class<out ViewModel>, Provider<ViewModel>>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        viewModelProviders[modelClass]?.get() as T
}

// In Activity
class MainActivity : AppCompatActivity() {
    @Inject lateinit var factory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KTP.openScopes(application, this)
        Toothpick.inject(this, KTP.openScope(this))
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }
}
```

```kotlin
// AFTER: Koin — no factory needed, just annotate the ViewModel
@KoinViewModel
class MainViewModel(
    private val repo: UserRepository
) : ViewModel()

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModel()
}

// Or in Compose:
val viewModel = koinViewModel<MainViewModel>()
```

## Testing

```kotlin
// BEFORE: Toothpick test
class UserRepositoryTest {
    @Rule @JvmField
    val toothpickRule = ToothPickRule(this, "test")

    @Inject lateinit var repo: UserRepository

    @Before
    fun setUp() {
        val scope = KTP.openScope("test")
        scope.installTestModules(object : Module() {
            init {
                bind(ApiService::class.java).toInstance(mockk())
                bind(UserDao::class.java).toInstance(mockk())
            }
        })
        Toothpick.inject(this, scope)
    }

    @After
    fun tearDown() {
        KTP.closeScope("test")
    }
}
```

```kotlin
// AFTER: Koin test
class UserRepositoryTest : KoinTest {
    private val mockApi = mockk<ApiService>()
    private val mockDao = mockk<UserDao>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(module {
            single<ApiService> { mockApi }
            single<UserDao> { mockDao }
            single<UserRepository>()
        })
    }

    private val repo: UserRepository by inject()

    @Test
    fun `fetches users`() = runTest {
        coEvery { mockApi.getUsers() } returns listOf(User("Alice"))
        assertEquals(1, repo.getUsers().size)
    }
}
```

## Migration Checklist

- [ ] Remove all `KTP.openScope()` / `KTP.openScopes()` calls
- [ ] Remove all `KTP.closeScope()` calls
- [ ] Remove all `Toothpick.inject(this, scope)` calls
- [ ] Remove all `@InjectConstructor` annotations → replace with `@Factory` (or `@Singleton` if was scoped singleton)
- [ ] Remove all `@Inject` field annotations → replace with `by inject()`
- [ ] Remove all Toothpick `Module` subclasses → replace with `@Module` classes
- [ ] Remove custom `ViewModelProvider.Factory` → use `@KoinViewModel`
- [ ] Remove `smoothie` dependency if used for lifecycle scoping
- [ ] Remove `@ScopeAnnotation` annotations → replace with `@Scope(T::class)`
- [ ] Remove `Provider<T>` usage → replace with lambda `() -> T` or Koin `get()`
- [ ] Remove `Releasable` implementations → `scope.close()` handles cleanup
- [ ] Verify no remaining imports from `toothpick.*` packages
- [ ] Koin compiler plugin verifies dependency graph at compile time
