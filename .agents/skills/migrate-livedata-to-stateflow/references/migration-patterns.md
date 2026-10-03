# LiveData → StateFlow migration patterns

This reference is deliberately narrower than a general Kotlin Flow guide. It captures the migration relationships supported by the supplied Android documentation and the supplied Jose Alcérreca migration article.

## 1. State-holder boundary

### Existing pattern

```kotlin
private val name: LiveData<String>
    field = MutableLiveData<String>()
```

### Target shape

```kotlin
val name: StateFlow<String>
   field = MutableStateFlow(initialName)
```

The important migration rule is not the property names. The rule is:

- keep mutation on the ViewModel/state holder side;
- expose a read-only state type;
- Always use kotlin explicit backing fields for mutable state (`field` keyword)
- make the initial value explicit because StateFlow always has a current value.

The supplied article describes StateFlow as the Flow type closest to LiveData and says it always has a value, supports multiple observers, and replays the latest value to a new subscription.

## 2. `setValue` / `postValue`

### Existing pattern

```kotlin
currentName.value = newName
```

or:

```kotlin
currentName.postValue(newName)
```

### Target

```kotlin
name.value = newName
```

Do not treat this as a mechanical replacement when the old code used `postValue()` from a worker thread. First identify the coroutine/context that owns the update and preserve the intended threading semantics.

## 3. `map`

### Existing

```kotlin
val userName: LiveData<String> = userLiveData.map {
    user -> "${user.name} ${user.lastName}"
}
```

### Target

```kotlin
val userName: Flow<String> = userFlow.map {
    user -> "${user.name} ${user.lastName}"
}
```

If `userName` is UI state that must be exposed as a `StateFlow`, share/state-ify the final pipeline deliberately rather than changing every intermediate Flow into a StateFlow.

## 4. `switchMap`

### Existing

```kotlin
val user = userId.switchMap { id ->
    getUser(id)
}
```

### Target

```kotlin
val user = userIdFlow.flatMapLatest { id ->
    getUser(id)
}
```

The supplied migration article explicitly presents `flatMapLatest` as the Flow equivalent for switching to the latest downstream source.

This mapping is semantic: a new input invalidates/replaces the previous downstream subscription.

## 5. Stream observed with parameters

When the current application uses a parameterized LiveData stream, prefer to model the parameter and the observed stream as Flows:

```kotlin
val userId: Flow<String> = ...
val user = userId.flatMapLatest { id ->
    repository.observeItem(id)
}
```

If the UI needs current state rather than a cold stream, convert the final result to `StateFlow` with `stateIn`.

Do not retain `asLiveData()` in the middle of the pipeline just because the old implementation had it.

## 6. `MediatorLiveData`

### Existing mental model

Several LiveData sources feed one mutable observable.

### Flow model

When the intended behavior is “derive a new value from the latest values of multiple sources”:

```kotlin
val combined =
    combine(firstFlow, secondFlow) { first, second ->
        derive(first, second)
    }
```

The supplied article names this mapping as:

`MediatorLiveData` → `Flow.combine`

It also mentions `combineTransform` and `zip` as other options.

Do not assume all MediatorLiveData implementations are a simple `combine`. Read the source-management logic first.

## 7. `stateIn`

Use `stateIn` at the point where a cold Flow must become shared current state.

Shape:

```kotlin
val uiState: StateFlow<UiState> =
    upstream.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialState,
    )
```

The supplied article explains:

- `scope` controls the coroutine scope in which sharing is started;
- `started` controls when sharing starts/stops;
- `initialValue` is the initial state value;
- `Lazily` starts with the first subscriber;
- `Eagerly` starts immediately;
- `WhileSubscribed` stops upstream work when there are no collectors.

The article recommends `WhileSubscribed` with a timeout for continuously observed streams, and uses five seconds as its example timeout.

The timeout is a policy decision, not a magic migration constant.

## 8. `WhileSubscribed`

The supplied article explains why a stop timeout can be useful:

- lifecycle changes can briefly remove collectors, such as during recreation;
- a short timeout avoids immediately cancelling upstream work;
- when the app is backgrounded, upstream work can stop after the timeout;
- the latest StateFlow value remains cached by default.

`replayExpirationMillis` can be used when the application explicitly wants the replay cache to expire and the StateFlow to reset to its initial value.

Only introduce replay expiration when the desired stale-data behavior is understood and tested.

## 9. UI collection

### Avoid

```kotlin
lifecycleScope.launch {
    viewModel.uiState.collect { render(it) }
}
```

as a replacement for ordinary lifecycle-aware `LiveData.observe`, when the resulting collector remains active outside the intended UI lifecycle state.

Also do not blindly use `launchWhenStarted`/`launchWhenResumed`. The supplied article warns that these patterns suspend collection rather than cancelling the coroutine/upstream work in the same way.

### Preferred pattern from the supplied article

For a Fragment view lifecycle:

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            render(state)
        }
    }
}
```

The collection state and the `stateIn` sharing policy should be considered together.

## 10. Flow in lower layers

The supplied Android documentation says LiveData is intended to communicate with lifecycle owners and should not be used as the generic asynchronous stream type in repositories. It suggests Kotlin Flow for streams in other layers, converting to LiveData at the ViewModel boundary when needed.

For a full migration, prefer:

```text
DAO / repository
      │
      ▼
    Flow<T>
      │
      ▼
 ViewModel transformation
      │
      ▼
 StateFlow<UiState>
      │
      ▼
 lifecycle-aware UI collector
```

Do not add lifecycle ownership to repository code simply to preserve old LiveData behavior.

## 11. LiveData-to-Flow interop

If a downstream API still requires LiveData, keep the conversion at the boundary:

```kotlin
val legacyLiveData: LiveData<UiState> = uiState.asLiveData()
```

The supplied Android documentation explicitly describes `asLiveData()` as the bridge from Flow to LiveData.

Do not reintroduce LiveData into the entire pipeline just because one integration point still needs it.

## 12. Event-like LiveData

This skill intentionally does **not** prescribe a universal event migration pattern.

Why:

- StateFlow always has a value;
- StateFlow replays its latest value to a new subscriber;
- event semantics are not equivalent to persistent state semantics.

For a LiveData used for navigation, snackbar/toast commands, one-time callbacks, or other transient effects:

1. inspect producer and all consumers;
2. determine the intended delivery/replay semantics;
3. avoid blindly converting it to StateFlow;
4. choose a dedicated event mechanism only after those semantics are explicit.

## 13. High-risk legacy cases

Flag these before automatic migration:

- `observeForever`;
- custom `LiveData` subclasses with `onActive()` / `onInactive()`;
- complex `MediatorLiveData`;
- LiveData used in repositories with significant lazy behavior;
- Data Binding or generated code tied to LiveData;
- Java callers that expect the old public API;
- `postValue` combined with cross-thread mutation;
- nested transformations whose cancellation behavior is not obvious.

## Source traceability

Key supplied-source ideas used by this skill:

- LiveData is lifecycle-aware and notifies active observers; the Android documentation describes observers as active at `STARTED` or `RESUMED`.
- The Android documentation recommends holding UI-facing LiveData in ViewModels rather than Activities/Fragments.
- The Android documentation describes `MutableLiveData.setValue()` and `postValue()` and the mutable/private versus immutable/public pattern.
- The Android documentation warns against using LiveData as the repository/data-layer stream abstraction and points to Kotlin Flow for streams in other layers.
- The Android documentation describes `map`, `switchMap`, and `MediatorLiveData`.
- The supplied migration article maps Mutable state holders to `(Mutable)StateFlow`, `switchMap` to `flatMapLatest`, `MediatorLiveData` to `Flow.combine`, Flow-to-StateFlow conversion to `stateIn`, and lifecycle collection to `repeatOnLifecycle`.
- The supplied migration article recommends `WhileSubscribed` with a timeout for continuously observed streams and discusses `replayExpirationMillis`.

Do not treat historical dependency versions or the article's exact API-era recommendations as universal. The migration agent should verify the actual project's dependency set before editing build files or relying on version-specific APIs.
