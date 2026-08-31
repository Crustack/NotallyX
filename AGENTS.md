# AGENTS.md - NotallyX Development Guidelines for AI Agents

## Project Overview & Purpose

**NotallyX** is an open-source, minimalistic, yet feature-rich note-taking Android application built with Kotlin. It provides users with a distraction-free experience for creating text notes, task lists, rich-text formatting, media attachments (images, audio, files), labels, reminders, and encrypted backups.

- **Target Platform**: Android (Min SDK: 21, Compile/Target SDK: 36, JVM Target: 1.8).
- **Core Architecture**: MVVM (Model-View-ViewModel) architecture backed by Android Jetpack components (Room, LiveData, ViewModel, Navigation Component, WorkManager) with ViewBinding and DataBinding.
- **Key Tenets**: User privacy, offline-first reliability, non-destructive data handling, robust backup/restore mechanisms, and clean Kotlin idioms.

---

## Tech Stack & Conventions

### Language & Tooling
- **Language**: Kotlin 1.9.0 (Standard Kotlin Idioms, Coroutines, Serialization).
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`), Android Gradle Plugin 8.7.3, KSP (`com.google.devtools.ksp`) for Room compiler.
- **Code Formatter**: `ktfmt` with Kotlin standard style (`kotlinLangStyle`).

### UI Framework & Conventions
- **UI System**: Android Views (XML layouts) with `MaterialComponents` / Material 3 styling.
- **View Binding**: ViewBinding and DataBinding enabled (`buildFeatures { viewBinding = true; dataBinding = true }`).
- **Navigation**: Jetpack Navigation Component with NavHostFragment and XML navigation graphs (`navigation.xml`).
- **Lists**: `RecyclerView` using custom ViewHolders (`*VH`), ListAdapters / custom Adapters (`*Adapter`), and `ItemTouchHelper` for drag-and-drop / swipe interactions.

### State Management & Concurrency
- **State Holders**: `AndroidViewModel` subclasses (`BaseNoteModel`, `NotallyModel`) handling business logic and exposing observable state.
- **Observables**: `LiveData`, `MutableLiveData`, and custom `NotNullLiveData`.
- **Async & Concurrency**: Kotlin Coroutines (`viewModelScope`, `CoroutineScope`).
- **Dispatchers**:
  - `Dispatchers.Main`: UI interactions and LiveData updates.
  - `Dispatchers.IO`: Database operations, disk I/O, file exports/imports, compression, encryption.
  - Always handle coroutine exceptions using `CoroutineExceptionHandler` or try-catch blocks around suspend calls.

### Database & Storage Patterns
- **Database**: Room Database (`NotallyDatabase`, versioned with KSP schema exports in `app/schemas/`).
- **Encryption**: Optional SQLCipher database encryption (`net.zetetic:sqlcipher-android`) and AndroidX Security EncryptedSharedPreferences for sensitive settings.
- **Storage Strategy**:
  - Internal storage for private app data and default SQLite database.
  - Optional external storage storage mode (`dataInPublicFolder`).
  - Strict WAL checkpointing (`pragma wal_checkpoint(FULL)`) before any database export, backup, or replacement.
- **Type Converters**: `Converters.kt` converts complex models (spans, attachments, folders, reminders, labels) to/from JSON strings.

---

## Repository Directory Map

```
NotallyX/
├── app/
│   ├── schemas/com.philkes.notallyx.data.NotallyDatabase/  # Room schema exports (v1..vN JSON)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml                        # Manifest, permissions, activities, receivers
│   │   │   ├── java/com/philkes/notallyx/
│   │   │   │   ├── NotallyXApplication.kt                 # Application entry point, crash handling
│   │   │   │   ├── data/                                  # Data Layer: Room DB, DAOs, Entities, Importers
│   │   │   │   │   ├── NotallyDatabase.kt                 # Room database definition & migrations
│   │   │   │   │   ├── DatabaseManager.kt                 # Database instance lifecycle & encryption manager
│   │   │   │   │   ├── dao/                               # Room DAOs (BaseNoteDao, LabelDao, CommonDao)
│   │   │   │   │   ├── model/                             # Data entities (BaseNote, Label, Attachment, etc.)
│   │   │   │   │   └── imports/                           # Importers (Google Keep, Evernote, Quillpad, JSON, TXT)
│   │   │   │   ├── presentation/                          # Presentation Layer: UI & ViewModels
│   │   │   │   │   ├── activity/                          # Activities (MainActivity, EditNoteActivity, EditListActivity)
│   │   │   │   │   ├── fragment/                          # Fragments (NotesFragment, SettingsFragment, etc.)
│   │   │   │   │   ├── view/                              # ViewHolders, Adapters, custom UI components
│   │   │   │   │   └── viewmodel/                         # ViewModels (BaseNoteModel, NotallyModel, Preferences)
│   │   │   │   └── utils/                                 # Utilities: Backup, security, formatting, media, spans
│   │   │   └── res/                                       # Resources (layouts, drawables, navigation, values)
│   │   └── test/                                          # Unit & Robolectric test suite
│   │       ├── java/ / kotlin/com/philkes/notallyx/
│   │       │   ├── data/                                  # Database, migration & converter tests
│   │       │   ├── imports/                               # Importer parsing tests
│   │       │   ├── recyclerview/                          # ListManager & adapter logic tests
│   │       │   └── utils/                                 # Security, compression, and utility tests
│   │       └── resources/                                 # Test fixture files (exports, sample backups)
│   └── build.gradle.kts                                   # App-level build configurations & dependencies
├── build.gradle.kts                                       # Root build configuration
└── settings.gradle.kts                                    # Project & repository settings
```

---

## Agent Guidelines & Constraints

### Do's
- **Preserve Data Integrity**: Always safeguard user notes. Never drop tables destructively without valid migration paths.
- **Run Checkpoints Before DB Copy**: When exporting, backing up, or copying SQLite files, call `database.checkpointOrThrow()` to ensure the WAL is flushed.
- **Use ViewBinding**: Inflate views using generated ViewBinding bindings (e.g., `ActivityEditBinding.inflate(layoutInflater)`).
- **Offload Heavy Work**: Perform file I/O, parsing (JSON/Markdown/HTML), and database queries on `Dispatchers.IO`.
- **Format with ktfmt**: Ensure Kotlin files comply with `ktfmt` by running `./gradlew ktfmtFormat`.
- **Follow Existing Patterns**: Match existing naming conventions (`*Activity`, `*Fragment`, `*Model`, `*VH`, `*Adapter`, `*Dao`).
- **Write Unit & Robolectric Tests**: Add tests under `app/src/test/` whenever introducing business logic, migration steps, or parser utilities.

### Don'ts
- **NO Blocking Main Thread**: Never invoke synchronous database queries, heavy regex, or file operations on the main thread.
- **NO Raw SQL Injections**: Use Room DAO annotations (`@Query`, `@Insert`, `@Update`, `@Delete`) or properly parameterised `SupportSQLiteQuery`.
- **NO Hardcoded Strings**: Use string resources (`R.string.*`) for all UI-visible text to preserve localization support across 30+ languages.
- **NO Direct Database File Edits Without Manager**: Do not manipulate SQLite files without going through `DatabaseManager` and `NotallyDatabase`.
- **NO Unapproved Third-Party Libraries**: Avoid adding heavy external dependencies unless explicitly requested.

---

## Common Tasks & Workflows

### 1. Adding a New Screen / Feature
1. **Layout**: Create an XML layout in `app/src/main/res/layout/` with ViewBinding support.
2. **ViewModel**: Expose `LiveData` or `NotNullLiveData` state in an existing or new `ViewModel` (`AndroidViewModel`).
3. **Activity/Fragment**:
   - Create the Activity in `presentation/activity/` or Fragment in `presentation/activity/main/fragment/`.
   - Bind views using `ViewBinding`.
   - Observe ViewModel `LiveData` in `onCreate()` / `onViewCreated()`.
4. **Navigation**: If part of the main navigation, declare the fragment/destination in `app/src/main/res/navigation/navigation.xml` and register menu actions in `MainActivity`.
5. **Manifest**: Register new Activities in `app/src/main/AndroidManifest.xml`.

#### Example ViewModel Pattern:
```kotlin
class FeatureViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableLiveData<FeatureUiState>(FeatureUiState.Loading)
    val uiState: LiveData<FeatureUiState> = _uiState

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = fetchData()
                _uiState.postValue(FeatureUiState.Success(data))
            } catch (e: Exception) {
                _uiState.postValue(FeatureUiState.Error(e.message ?: "Unknown error"))
            }
        }
    }
}
```

#### Example Activity ViewBinding Pattern:
```kotlin
class FeatureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeatureBinding
    private val viewModel: FeatureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeatureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        observeViewModel()
        viewModel.loadData()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is FeatureUiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is FeatureUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.textViewContent.text = state.data
                }
                is FeatureUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showToast(state.message)
                }
            }
        }
    }
}
```

---

### 2. Creating a Database Migration
When modifying Room entities (`BaseNote`, `Label`, etc.):
1. Update entity classes in `com.philkes.notallyx.data.model`.
2. Increment the `version` number in `@Database(entities = [...], version = NEW_VERSION)` in `NotallyDatabase.kt`.
3. Define the `Migration` object in `NotallyDatabase.kt`:
   ```kotlin
   val MIGRATION_11_12 = object : Migration(11, 12) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE notes ADD COLUMN new_feature_column TEXT DEFAULT NULL")
       }
   }
   ```
4. Add the migration object to `MIGRATIONS` array in `NotallyDatabase.kt`.
5. Update `DatabaseManager.kt` migration lists if relevant.
6. Run `./gradlew kspDebugKotlin` to regenerate the Room schema JSON in `app/schemas/`.
7. Write migration verification tests in `app/src/test/kotlin/com/philkes/notallyx/data/`.

---

### 3. Writing and Running Tests

The test suite contains both pure unit tests and Robolectric tests (`testOptions.unitTests.isIncludeAndroidResources = true`).

#### Running Tests via CLI:
```bash
# Run all unit tests
./gradlew test

# Run debug unit tests specifically
./gradlew testDebugUnitTest

# Reformat code with ktfmt
./gradlew ktfmtFormat

# Check code formatting without applying
./gradlew ktfmtCheck
```

#### Example Unit / Robolectric Test Pattern:
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteHelperTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun testNoteTransformation() {
        val note = BaseNote(
            id = 1L,
            title = "Test Title",
            body = "Test Body",
            type = Type.NOTE,
            folder = Folder.NOTES
        )

        val result = note.toSearchResult()
        assertThat(result.title).isEqualTo("Test Title")
    }
}
```

---

## Code Style & Quality Rules

1. **Formatting**: All Kotlin source code must follow `ktfmt` Kotlin style (2-space indent, strict import ordering). Run `./gradlew ktfmtFormat` before committing.
2. **Naming Conventions**:
   - Entities & Models: PascalCase data classes (`BaseNote`, `Label`, `Attachment`).
   - DAOs: Interface suffix `*Dao` (`BaseNoteDao`, `LabelDao`).
   - ViewModels: `*Model` or `*ViewModel` (`BaseNoteModel`, `NotallyModel`).
   - Activities & Fragments: `*Activity`, `*Fragment` (`EditNoteActivity`, `NotesFragment`).
   - Adapters & ViewHolders: `*Adapter`, `*VH` (`BaseNoteAdapter`, `BaseNoteVH`).
   - Preferences: Wrapper properties in `NotallyXPreferences`.
3. **Error Handling**:
   - Catch specific exceptions rather than swallowing generic `Throwable`.
   - Provide user feedback using `showToast`, `showSnackbar`, or error dialogs.
   - For background jobs, update progress observers (`Progress.ProgressUpdate`, `MigrationProgress`, etc.).
