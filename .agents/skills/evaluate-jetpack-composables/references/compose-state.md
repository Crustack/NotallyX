# State in Jetpack Compose

State in Jetpack Compose represents any value that can change over time. Updating state triggers recomposition of the affected composables.

## 1. Composition and Recomposition

* **Composition**: Description of the UI built by Compose when executing composables.
* **Initial Composition**: Creating the Composition the first time composables run.
* **Recomposition**: Re-running composables to update the Composition when observed state changes.

## 2. Managing State in Composables

Use `remember` to store values in memory across recompositions. Combine with `mutableStateOf` to create observable state (`MutableState<T>`).

```kotlin
var name by remember { mutableStateOf("") }
```

### Preserving State Across Configuration Changes
Use `rememberSaveable` to retain state across Activity creation or process death (configuration changes such as rotation).

```kotlin
var name by rememberSaveable { mutableStateOf("") }
```

### Avoiding Unobservable Collections
> [!CAUTION]
> Avoid standard mutable collections like `ArrayList` or `mutableListOf()` as Compose state without wrappers. They are non-observable and will not trigger recomposition when modified. Use immutable lists (`List<T>`) wrapped in `MutableState<List<T>>` or `snapshots.SnapshotStateList` (`mutableStateListOf()`).

## 3. State Hoisting & Unidirectional Data Flow (UDF)

State hoisting moves state to a composable's caller, making the component stateless and reusable.

Replace internal state with two parameters:
* `value: T`: The current state value to display.
* `onValueChange: (T) -> Unit` (or specific event lambdas): Callbacks for requesting state changes.

```kotlin
@Composable
fun HelloScreen() {
    var name by rememberSaveable { mutableStateOf("") }
    HelloContent(name = name, onNameChange = { name = it })
}

@Composable
fun HelloContent(name: String, onNameChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Hello, $name")
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Name") })
    }
}
```

### Key Rules for State Hoisting
1. **Lowest Common Parent**: Hoist state to at least the lowest common parent of all composables reading that state.
2. **Highest Write Level**: Hoist state to at least the highest level where it is modified.
3. **Co-located States**: Hoist states together if they change in response to the same event.

## 4. Converting External Observable Streams

Before reading external observable sources in Compose, convert them to Compose `State<T>`:
* **Flow**: `collectAsStateWithLifecycle()` (Android lifecycle-aware) or `collectAsState()` (multiplatform).
* **LiveData**: `observeAsState()`.

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

## 5. Re-evaluating `remember` with Keys

Pass keys into `remember(key1, key2)` so that cached calculations or state holder instances are recalculated when key inputs change.

```kotlin
val brush = remember(avatarRes) {
    ShaderBrush(
        BitmapShader(
            ImageBitmap.imageResource(res, avatarRes).asAndroidBitmap(),
            Shader.TileMode.REPEAT,
            Shader.TileMode.REPEAT
        )
    )
}
```

## 6. Saving Complex Objects

For objects that cannot be automatically stored in a `Bundle` via `rememberSaveable`:
* **`@Parcelize`**: Annotate data classes with `@Parcelize` (`Parcelable`).
* **`mapSaver`**: Convert objects into a key-value `Map`.
* **`listSaver`**: Convert objects into an indexed `List`.
