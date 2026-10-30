# Jetpack Compose Best Practices

Follow these performance and optimization rules for Jetpack Compose.

## 1. Use `remember` to Minimize Expensive Calculations

Composable functions can run very frequently (as often as every frame during an animation). Minimize calculation inside the body of a composable function. Store calculation results with `remember`.

```kotlin
@Composable
fun ContactList(
    contacts: List<Contact>,
    comparator: Comparator<Contact>,
    modifier: Modifier = Modifier
) {
    val sortedContacts = remember(contacts, comparator) {
        contacts.sortedWith(comparator)
    }

    LazyColumn(modifier = modifier) {
        items(sortedContacts) { contact ->
            // ...
        }
    }
}
```

*Note: Where possible, move heavy calculations outside the composable altogether (e.g. into the ViewModel).*

## 2. Use Stable Keys in Lazy Layouts

Lazy layouts (`LazyColumn`, `LazyRow`, `LazyVerticalGrid`) efficiently reuse items. Provide a stable, unique key for each item so Compose can track moved items across recompositions without re-creating unchanged items.

```kotlin
@Composable
fun NotesList(notes: List<Note>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(
            items = notes,
            key = { note -> note.id }
        ) { note ->
            NoteRow(note)
        }
    }
}
```

## 3. Use `derivedStateOf` to Limit Recompositions

When state changes faster than the UI needs to update (e.g., scroll position offsets in a list), wrap derived state calculations in `derivedStateOf` to trigger recomposition only when the computed threshold or output changes.

```kotlin
val listState = rememberLazyListState()

LazyColumn(state = listState) {
    // ...
}

val showButton by remember {
    derivedStateOf {
        listState.firstVisibleItemIndex > 0
    }
}

AnimatedVisibility(visible = showButton) {
    ScrollToTopButton()
}
```

## 4. Defer State Reads as Long as Possible

Defer state reads to the lowest possible node in the Composition tree, or pass state values as lambda providers (`() -> T`) to defer execution to the layout or draw phase.

### Using Lambda Modifiers to Skip Composition Phase
When updating layout offset, size, or draw properties based on rapidly changing state, use lambda versions of modifiers (e.g., `Modifier.offset { ... }` or `Modifier.drawBehind { ... }`) to execute changes in the layout/draw phase directly without triggering recompositions.

```kotlin
@Composable
private fun Title(snack: Snack, scrollProvider: () -> Int) {
    Column(
        modifier = Modifier
            .offset { IntOffset(x = 0, y = scrollProvider()) }
    ) {
        // ...
    }
}

@Composable
fun AnimatedBox(colorProvider: () -> Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(colorProvider())
            }
    )
}
```

## 5. Avoid Backwards Writes

Compose assumes state is never modified after it has been read during the same composition pass. Writing to state during composition pass causes backwards writes, resulting in recomposition loops or infinite rendering passes.

* **Rule**: Never mutate state directly in the body of a `@Composable`. Always perform state writes inside event callbacks (e.g. `onClick`, `onValueChange`) or side-effect handlers (`LaunchedEffect`, `SideEffect`).

```kotlin
// BAD - Backwards write during composition
@Composable
fun BadComposable() {
    var count by remember { mutableIntStateOf(0) }
    Text("$count")
    count++ // DO NOT DO THIS
}

// GOOD - State write triggered inside event handler
@Composable
fun GoodComposable() {
    var count by remember { mutableIntStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Count: $count")
    }
}
```
