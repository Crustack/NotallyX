---
name: livedata-to-stateflow
description: Provides a structured workflow for migrating an Android/Kotlin codebase from LiveData-based state and observation to Kotlin Flow and StateFlow. This skill covers inventory and classification, state-holder migration, lifecycle-aware UI collection, Flow transformations, stateIn/sharing policies, repository and DAO boundaries, interop, validation, and manual-review cases. Use this skill when migrating LiveData, MutableLiveData, Transformations, MediatorLiveData, LiveData coroutine builders, or LiveData UI observation to Flow/StateFlow while preserving behavior, lifecycle semantics, data ownership, and existing architecture.
metadata:
   author: OpenAI
   last-updated: '2026-10-03'
   keywords:
      - Android
      - Kotlin
      - LiveData
      - MutableLiveData
      - StateFlow
      - MutableStateFlow
      - Kotlin Flow
      - lifecycle
      - repeatOnLifecycle
      - stateIn
      - SharingStarted
      - migration
      - ViewModel
      - MediatorLiveData
      - Transformations
      - flatMapLatest
      - combine
      - repository
      - Room
      - incremental migration
   scope: Android/Kotlin application architecture and UI state
   primary-goal: Preserve observable-state and lifecycle behavior while replacing LiveData with Flow/StateFlow
   migration-style: Incremental and behavior-preserving
   primary-targets:
      - ViewModel UI state
      - UI lifecycle collection
      - repository and DAO stream boundaries
      - LiveData transformations
      - LiveData/Flow interop
   validation:
      - Compile affected modules
      - Run relevant unit tests
      - Run lifecycle/UI tests when applicable
      - Re-scan for unintended LiveData usage
---

# LiveData → StateFlow Migration Skill

## Purpose

Use this skill to perform an incremental, behavior-preserving migration from Android `LiveData` to Kotlin `StateFlow`.

The migration target is **StateFlow for UI state** and Kotlin `Flow` for asynchronous streams and transformations. The skill is intentionally conservative: do not rewrite architecture just because Flow makes a different design possible. Preserve existing ownership, threading, state shape, public APIs, and behavior unless the repository already has a clear convention that should be followed.

This skill is based on the supplied sources:

- `livedata.md.md` — Android LiveData documentation supplied with the task.
- `Migrating from LiveData to Kotlin’s Flow _ by Jose Alcérreca _ Android Developers _ Medium.html` — the supplied migration article.

The article is historical and its exact dependency/API versions may be outdated. Treat its migration principles as the source of truth for this skill, but verify version-sensitive APIs, signatures, and project compatibility against the repository's actual Gradle dependencies and current local documentation before applying changes.

See [references/migration-patterns.md](references/migration-patterns.md) for the detailed mapping and review rules.

## Core invariants

Preserve these invariants unless the code clearly demonstrates that they are intentionally changing:

1. **State ownership stays out of Activities/Fragments.**
   The supplied Android documentation places UI-facing `LiveData` in the `ViewModel`; the migration should keep state ownership in the `ViewModel` or existing state-holder layer.

2. **Public UI state is read-only.**
   Replace the `LiveData`/`MutableLiveData` public-boundary pattern with a public `StateFlow` (and use kotlin explicit backing fields for private `MutableStateFlow` value) wherever that matches the current API.

3. **StateFlow requires an initial value.**
   Every `StateFlow` has a current value. Do not silently invent a fake default just to make the type compile. Choose an initial state that represents an existing, meaningful state contract, or introduce an explicit loading/empty/uninitialized state when the domain already supports it.

4. **Lifecycle handling moves from the observable to the collector.**
   `LiveData.observe(owner, observer)` supplied lifecycle awareness automatically. `StateFlow` collection is explicit, so UI collection must be lifecycle-aware. Prefer the `repeatOnLifecycle` pattern described by the supplied migration article.

5. **Do not keep upstream work hot accidentally.**
   When a `ViewModel` exposes a transformed/observed flow as `StateFlow`, use the repository/project's established `SharingStarted` policy. The supplied article recommends `WhileSubscribed` with a timeout for continuously observed streams and explains why this matters for upstream resource usage.

6. **Preserve transformation semantics.**
   A migration is not complete when types change; the resulting Flow graph must represent the same source relationships and switching/combining behavior.

7. **Do not convert one-shot events into persistent state without review.**
   `StateFlow` always holds and replays the latest value. If the existing `LiveData` is actually being used as a transient event channel, stop and classify it before changing semantics. The supplied sources focus on UI state, not a complete event architecture.

8. **Do not broaden the migration unnecessarily.**
   Avoid changing domain models, repository boundaries, navigation architecture, or unrelated coroutine code unless required to complete the LiveData → Flow migration safely.

## Workflow

### Phase 1 — Inventory before editing

Search the repository for all relevant patterns, including:

- `LiveData<`
- `MutableLiveData<`
- `.observe(`
- `observeForever(`
- `.setValue(`
- `.postValue(`
- `Transformations.map`
- `Transformations.switchMap`
- `MediatorLiveData`
- `liveData {`
- `emitSource`
- `asLiveData()`
- LiveData-returning DAO/repository methods
- `lifecycleScope.launch`
- `launchWhenCreated`
- `launchWhenStarted`
- `launchWhenResumed`
- existing `Flow`, `StateFlow`, `MutableStateFlow`, `stateIn`, `combine`, `flatMapLatest`, `repeatOnLifecycle`

Create a migration inventory. For each LiveData, record:

| Item | Record |
|---|---|
| Owner | ViewModel / repository / DAO / UI / other |
| Type | state / derived state / stream / event-like / interop |
| Initial state | unset / nullable / existing default / wrapper state |
| Writers | `setValue`, `postValue`, transformation, callback, etc. |
| Readers | Activities, Fragments, services, tests, other layers |
| Transformations | `map`, `switchMap`, `MediatorLiveData`, etc. |
| Lifecycle behavior | ordinary observe / view lifecycle / `observeForever` |
| Threading | main-thread writes / worker-thread writes |
| Interop | `asLiveData`, Java callers, data binding, other |
| Risk | low / medium / high |

Do not edit code until the highest-impact dependencies of each observable are understood.

### Phase 2 — Classify each observable

Classify every LiveData occurrence into one of these categories:

#### A. UI state

Use `StateFlow`.

Typical signs:

- ViewModel exposes current screen data.
- UI always wants the latest value.
- Existing code observes it repeatedly across lifecycle changes.
- It conceptually represents a current condition such as loading/content/error.

Target shape:

```kotlin
val uiState : StateFlow<UiState>
   field = MutableStateFlow(initialState)
```

Match the project's existing style.

#### B. Derived state

Prefer Flow operators and convert to `StateFlow` at the ViewModel/UI-state boundary when the result is persistent UI state.

Examples include:

- `map`
- `combine`
- `flatMapLatest`
- `transformLatest`

Do not reproduce a LiveData transformation graph mechanically if an equivalent Flow expression is simpler and preserves behavior.

#### C. Stream from another layer

Prefer a regular `Flow` in the lower layer and expose the UI-facing result as `StateFlow` when it is UI state.

The supplied Android documentation specifically discourages putting LiveData in repositories/data-layer classes and suggests Kotlin Flow for streams outside the UI/ViewModel boundary.

#### D. Event-like or command-like LiveData

Do not automatically replace it with `StateFlow`.

Because `StateFlow` always has a value and replays the latest value to new collectors, blindly converting event-like behavior can cause repeat delivery or stale-event behavior.

Instead:

- preserve the existing semantics,
- inspect how producers and consumers use it,
- determine whether it is actually state,
- make a deliberate design choice,
- and flag it for manual review if the sources do not fully specify the required event semantics.

#### E. Interop-only LiveData

A temporary bridge can remain when an external API requires LiveData. Keep the bridge narrow and document why it exists.

The supplied Android documentation describes converting Flow to LiveData with `asLiveData()` when a layer still requires LiveData.

### Phase 3 — Migrate the state holder

For the common ViewModel pattern:

```kotlin
val name: LiveData<String>
   field = MutableLiveData<String>()
```

migrate toward:

```kotlin
val name: StateFlow<String>
   field = MutableStateFlow(initialName)
```

Then map writes:

```kotlin
name.value = newValue
```

For compound updates, prefer the project's established atomic/update idiom when required by concurrency semantics.

Do not expose `MutableStateFlow` merely to make the migration easy. Keep mutation private unless the existing architecture explicitly requires external mutation.

### Phase 4 — Replace transformations

Use these semantic mappings:

| LiveData | Flow/StateFlow target |
|---|---|
| `Transformations.map` / `.map` | `Flow.map` |
| `switchMap` | `flatMapLatest` |
| `MediatorLiveData` merging multiple sources | `combine` where the desired result depends on the latest values of multiple sources |
| more custom multi-source transformation | `combine`, `combineTransform`, `zip`, `transformLatest`, or explicit Flow logic as dictated by the existing semantics |
| `liveData {}` one-shot result | a Flow/state pipeline, often `stateIn` when the consumer needs state |

The important part of `switchMap` migration is cancellation/switching behavior: when the input changes, the current downstream source should be replaced by the latest source. Use `flatMapLatest` for that semantic.

Do not mechanically replace every `MediatorLiveData` with `combine`. Inspect whether the original behavior depends on:

- latest value from each source,
- emission from any source,
- source add/remove dynamics,
- ordering,
- missing initial values,
- explicit mutation performed inside the mediator.

If the existing behavior is not equivalent to a simple `combine`, preserve it with the Flow operator structure that matches the real semantics.

### Phase 5 — Convert cold Flow to StateFlow only where needed

A regular `Flow` becomes a `StateFlow` with `stateIn(...)` when the consumer needs state semantics.

Follow the supplied article's model:

```kotlin
val uiState: StateFlow<UiState> =
   upstream.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = initialState,
   )
```

Treat `5000` as an example from the supplied article, not as a universal constant.

Before choosing `started`, decide:

- Is this a one-shot operation?
- Is this a continuously observed stream?
- Does the upstream hold resources such as database/sensor/network work?
- Should upstream stop when there are no collectors?
- Should the latest value remain cached across a short lifecycle gap?
- Should the replay cache eventually reset to `initialValue`?

The supplied article documents `Lazily`, `Eagerly`, and `WhileSubscribed`, and specifically recommends `WhileSubscribed` with a timeout for continuously observed streams.

Never select a sharing policy merely because it compiles.

### Phase 6 — Migrate UI collection

Replace lifecycle-aware LiveData observation with lifecycle-aware Flow collection.

For a Fragment whose UI is tied to its view lifecycle, follow the article's pattern:

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
   viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.uiState.collect { state ->
         render(state)
      }
   }
}
```

For an Activity, use the corresponding Activity lifecycle scope/owner appropriate to the project.

The key behavioral requirement is:

- collection begins at the chosen lifecycle state,
- collection stops when the lifecycle falls below that state,
- UI work is not performed while the UI is stopped,
- the state holder itself does not accidentally keep upstream work alive forever.

Do not replace `repeatOnLifecycle` with `lifecycleScope.launch` or `launchWhenX` without a repository-specific reason. The supplied article explicitly distinguishes suspending a collector from cancelling upstream work and recommends `repeatOnLifecycle`.

When several flows must update the UI, collect them inside the same lifecycle block unless the codebase has a clearer established pattern.

### Phase 7 — Handle old LiveData-specific behavior

Review these cases individually:

#### Uninitialized LiveData

The supplied LiveData documentation allows a `LiveData` to begin without a set value. `StateFlow` does not have that shape.

Choose one of:

- an explicit initial UI state,
- a nullable state if `null` is already meaningful,
- an explicit sealed/wrapper state such as `Loading`/`Success`/`Error` when this matches the existing contract.

Do not invent a domain value that could be mistaken for real data.

#### `setValue` vs `postValue`

Translate the *state update intent*, not the method name.

The old `postValue()` often indicates a background-thread producer; ensure the new coroutine/Flow implementation uses an appropriate coroutine context and state-update mechanism rather than blindly changing it to `state.value = ...` in an unsafe place.

#### `observeForever`

This has no direct `StateFlow.observeForever` equivalent. Determine why the observer was intentionally detached from a lifecycle. Preserve the owning coroutine scope and lifetime deliberately.

#### Transformations that were lazy

The LiveData documentation notes that transformations are calculated only while their returned LiveData has an active observer. When moving to Flow, decide where collection starts and where sharing is introduced so that laziness/resource behavior is not lost.

#### Repository/data-layer LiveData

Prefer to move the stream abstraction to `Flow` at the lower layer and let the ViewModel produce the `StateFlow` needed by the UI.

Do not put lifecycle concerns into repositories merely to make the Flow version compile.

### Phase 8 — Tests and verification

After each migration unit:

1. Compile the affected modules.
2. Run the relevant unit tests.
3. Run UI/instrumentation tests when lifecycle behavior is involved.
4. Check that the initial state is correct.
5. Check that updates happen exactly when expected.
6. Check configuration-change behavior.
7. Check stopped/background behavior.
8. Check cancellation of upstream work where relevant.
9. Check multiple collectors.
10. Search again for the old LiveData API and confirm remaining usages are intentional.

Use targeted diff review. A successful compile is not sufficient evidence of behavioral equivalence.

## Definition of done

A migration unit is complete when all of the following are true:

- The UI-facing state is a `StateFlow` where it represents persistent UI state.
- Mutable state is not unnecessarily exposed.
- Every `StateFlow` has a deliberate initial value/state.
- LiveData lifecycle observation has been replaced with lifecycle-aware Flow collection.
- `switchMap`/`MediatorLiveData` semantics have been deliberately mapped, not textually replaced.
- Resource-sharing behavior has been reviewed.
- Event-like LiveData has not been accidentally converted into replayed state.
- Repository/data-layer streams use Flow where migration is appropriate.
- Tests cover the changed lifecycle/state behavior.
- No unnecessary architecture changes were introduced.

## Escalate for manual review

Stop and flag the case rather than guessing when:

- the LiveData is event-like but the event contract is unclear;
- initial-state semantics are undefined;
- `observeForever` has an intentional long-lived lifetime that is not obvious;
- `MediatorLiveData` performs nontrivial source management;
- Java consumers depend on the exact old type/API;
- data binding or another generated integration depends on LiveData;
- `asLiveData()` is present at an integration boundary and the required downstream API is unknown;
- coroutine scopes/dispatchers are unclear;
- changing to `StateFlow` would alter public API contracts used outside the immediate feature;
- the project dependency versions do not support the proposed Flow/lifecycle API.

## Output expectations for an agent

When applying this skill, produce changes in this order:

1. Inventory the relevant LiveData usage.
2. State the migration classification for each affected observable.
3. Make the smallest coherent code changes.
4. Update lifecycle collection.
5. Update tests.
6. Run/build the affected scope.
7. Re-scan for accidental remaining or newly introduced LiveData patterns.
8. Summarize any manual-review items and any behavior that could not be proven equivalent.

Do not claim the migration is behavior-preserving merely because the project compiles. Explicitly call out any semantic assumption that could not be verified from code/tests.
