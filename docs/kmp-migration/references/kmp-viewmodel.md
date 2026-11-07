[//]: # (title: Multiplatform ViewModel)

The Android [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) allows you to connect
the business logic of your app with the UI components.
With Compose Multiplatform, you can also use ViewModels in common code.

This page walks you through setting up and working with ViewModels in a multiplatform project:

* [Set up dependencies](#set-up-dependencies).
* [Use ViewModel in common code](#using-viewmodel-in-common-code).
* [Scope ViewModels to navigation destinations](#viewmodel-scoping-with-navigation-3).
* [Inject dependencies with Koin or Metro](#viewmodel-and-dependency-injection).
* [Choose how much of your ViewModel and UI code to share](#levels-of-code-sharing):
  from a fully shared approach to sharing only the repository or data layer.

## Set up dependencies

To share ViewModels and UI across platforms:

1. Define the dependencies in a Gradle version catalog file:

    ```toml
    [versions]
    androidx-viewmodel = "2.10.0"
    
    [libraries]
    androidx-lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-viewmodel" }
    androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "androidx-viewmodel" }
    ``` 

   > You can track changes to the multiplatform ViewModel implementation in our [What's new](https://www.jetbrains.com/help/kotlin-multiplatform-dev/whats-new-compose.html)
   > or follow EAP releases in the [Compose Multiplatform changelog](https://github.com/JetBrains/compose-multiplatform/blob/master/CHANGELOG.md).
>
   {style="tip"}
2. In the `build.gradle.kts` file of the KMP module, add the following dependencies to the `commonMain` source set:

    ```kotlin
    kotlin {
       // ...
       sourceSets {
           // ...
           commonMain.dependencies {
               implementation(libs.androidx.lifecycle.viewmodel.compose)
               implementation(libs.androidx.lifecycle.viewmodel.navigation3)
           }
           // ...
       }
    }
    ```
   {initial-collapse-state="collapsed" collapsible="true" collapsed-title="implementation(libs.androidx.lifecycle.viewmodel.compose)"}

> Dependencies may differ depending on your code-sharing approach. See [](#levels-of-code-sharing) for details.
>
{style="note"}

If you have a desktop target, add the `kotlinx-coroutines-swing` dependency as well.
When running coroutines in a `ViewModel`, `ViewModel.viewModelScope` is tied to `Dispatchers.Main.immediate`,
which might be unavailable on desktop by default. The Kotlinx Coroutines Swing library makes ViewModel coroutines
work correctly with Compose Multiplatform.

1. In the Gradle version catalog:

    ```toml
    [versions]
    kotlinx-coroutines = "1.10.2"
    
    [libraries]
    kotlinx-coroutines-swing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinx-coroutines" }
    ```

2. In the `build.gradle.kts` file:

    ```kotlin
    kotlin {
       // ...
       sourceSets {
           // ...
           jvmMain.dependencies {
               implementation(libs.kotlinx.coroutines.swing)
           }
           // ...
       }
    }
    ```
   {initial-collapse-state="collapsed" collapsible="true" collapsed-title="implementation(libs.kotlinx.coroutines.swing)"}

   See the [`Dispatchers.Main` documentation](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-main.html) for details.

## Using ViewModel in common code

Compose Multiplatform provides a common `ViewModelStoreOwner` implementation, so using the `ViewModel` class
in common code is not much different from [Android best practices](https://developer.android.com/topic/libraries/architecture/viewmodel#best-practices).

However, there is an important difference on non-JVM platforms, where type reflection for instantiating objects is not available.
You cannot call the `viewModel()` function without parameters in common code.
Every time you create a `ViewModel` instance, you need to provide at least an initializer as an argument.

If only an initializer is provided, Compose Multiplatform creates a default factory under the hood.
However, you can implement your own factories and call more explicit versions of the common `viewModel()` function,
just like [with Jetpack Compose](https://developer.android.com/topic/libraries/architecture/viewmodel#jetpack-compose).

Let's define a ViewModel and wire it into a composable:

1. Define a simple `OrderViewModel` class that manages the UI state, including the quantity and price of the ordered item:

   ```kotlin
   data class OrderUiState(val quantity: Int = 0, val price: String = "$0.00")

   class OrderViewModel : ViewModel() {
      val uiState: StateFlow<OrderUiState>
          field = MutableStateFlow(OrderUiState())

      fun setQuantity(n: Int) {
          uiState.update { it.copy(quantity = n, price = "$${n * 2}.00") }
      }
   }
   ```

   > This example uses [explicit backing fields](https://kotlinlang.org/docs/properties.html#explicit-backing-fields),
   > stabilized in Kotlin 2.4.0. When using earlier versions, add
   > the `-Xexplicit-backing-fields` compiler option or use the old backing fields pattern with `.asStateFlow()` instead.
>
   {style="note"}

2. Add the custom ViewModel to your composable function using the common `viewModel()` function with an initializer:

    ```kotlin
    import com.example.ui.OrderViewModel
    
    @Composable
    fun CupcakeApp(
       viewModel: OrderViewModel = viewModel { OrderViewModel() },
    ) {
       // ...
    }
    ```

## ViewModel scoping with Navigation 3

When using ViewModels with Navigation 3 in common code,
ViewModels are not automatically scoped to navigation entries by default.
Without explicit scoping, each ViewModel will be tied to the `Activity` rather than the screen,
even after the user has navigated away.

To scope ViewModels and saveable Compose state per navigation entry,
pass the Navigation 3 entry decorators to `NavDisplay` when defining the navigation destinations:

```kotlin
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

//...

NavDisplay(
   entryDecorators = listOf(
       // Saves Compose state per entry
       rememberSaveableStateHolderNavEntryDecorator(),
       // Scopes ViewModel per entry
       rememberViewModelStoreNavEntryDecorator()
   ),
   backStack = backStack,
   entryProvider = entryProvider { }
)
```

## ViewModel and dependency injection

A dependency injection (DI) framework allows you to inject different dependencies into components
based on the current environment or target platform.
To manage ViewModels, you can use Koin, Metro, or any other DI framework that supports Kotlin Multiplatform.

For an advanced example of dependency injection usage,
see the [Share data access layer](multiplatform-ktor-sqldelight.md) tutorial.

### Koin

Koin is a runtime DI framework that provides either a DSL or annotations for configuring your dependencies.
To use Koin with Compose ViewModels, add the `koin-compose-viewmodel` dependency.

You can then inject a ViewModel into a Composable function using `koinViewModel()`:

```kotlin
@Composable
fun CupcakeApp(
   viewModel: UserViewModel = koinViewModel()
) {
   // ...
}
```

For details, see the Koin documentation on [ViewModel support](https://insert-koin.io/docs/reference/koin-core/viewmodel)
and [injecting ViewModels in Compose](https://insert-koin.io/docs/reference/koin-compose/compose-viewmodel).

### Metro

Metro is a compile-time DI framework implemented as a Kotlin compiler plugin.
To use Metro with Compose ViewModels, add the `metrox-viewmodel-compose` dependency.

Then you can inject a ViewModel into a Composable function using `metroViewModel()`:

```kotlin
@Composable
fun CupcakeApp(
   viewModel: UserViewModel = metroViewModel()
) {
   // ...
}
```

For details, see the MetroX documentation on [ViewModel integration](https://zacsweers.github.io/metro/latest/metrox-viewmodel/)
and [accessing ViewModels in Compose](https://zacsweers.github.io/metro/latest/metrox-viewmodel-compose/).

## Levels of code sharing

You can choose which parts of your code to share and which to keep platform-specific:

* To share both UI and business logic between platforms,
  see the [shared logic and UI tutorial](compose-multiplatform-new-project.md).
* To share some code without sharing UI implementation,
  see the [shared logic tutorial](multiplatform-upgrade-app.md).

The following examples show how to use ViewModel at different levels of code sharing.
All examples are based on the `OrderViewModel` class introduced above.

### Shared ViewModel and UI

In this approach, everything, including the `ViewModel` and the UI, is shared via Compose Multiplatform.
You write your app's UI code once, and it will work on all platforms.

```kotlin
@Composable
fun CupcakeApp(
   viewModel: OrderViewModel = viewModel { OrderViewModel() }
) {
   val uiState by viewModel.uiState.collectAsState()
    
   Column(modifier = Modifier.padding(16.dp)) {
       Text("Quantity: ${uiState.quantity}")
       Text("Price: ${uiState.price}")

       Button(onClick = { viewModel.setQuantity(6) }) {
           Text("Set Quantity to '6'")
       }
   }
}
```

### Shared ViewModel and platform-specific UI

In this approach, the `ViewModel` (business logic) is shared, but platforms have native UI implementations.
Learn more in [Set up ViewModel for Kotlin Multiplatform](https://developer.android.com/kotlin/multiplatform/viewmodel).

Since the UI is not shared in this case, you can switch from the Compose Multiplatform version of the ViewModel library to the `androidx.lifecycle` library.

1. Update the dependencies in the Gradle version catalog:

    ```toml
    [versions]
    androidx-viewmodel = "2.10.0"
    
    [libraries]
    androidx-lifecycle-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel", version.ref = "androidx-viewmodel" }
    ```

2. In the `build.gradle.kts` file, declare the dependency as `api`, as it needs to be exported to the binary framework:

    ```kotlin
    kotlin {
       // ...
       sourceSets {
           // ...
           commonMain.dependencies {
               api(libs.androidx.lifecycle.viewmodel)
           }
           // ...
       }
    }
    ```
   {initial-collapse-state="collapsed" collapsible="true" collapsed-title="api(libs.androidx.lifecycle.viewmodel)"}

#### Android implementation

On Android, Jetpack Compose automatically finds the `ViewModelStoreOwner`
provided by the `Activity` and supplies the `OrderViewModel`.

```kotlin
@Composable
fun AndroidCupcakeApp(
   viewModel: OrderViewModel = viewModel { OrderViewModel() }
) {
   val uiState by viewModel.uiState.collectAsState()

   Column {
       Text("Quantity: ${uiState.quantity}")
       Text("Price: ${uiState.price}")
       Button(onClick = { viewModel.setQuantity(6) }) {
           Text("Set Quantity to '6'")
       }
   }
}
```

#### iOS implementation

On iOS, there is no built-in `ViewModelStoreOwner`, so the ViewModel's lifecycle must be tied to SwiftUI manually.
We recommend using the [KMP-ObservableViewModel](https://klibs.io/project/rickclephas/KMP-ObservableViewModel) library,
which lets SwiftUI observe Kotlin Multiplatform ViewModels directly and handles the required ViewModel lifecycle/store-owner boilerplate for iOS.

1. Export ViewModel APIs for access from Swift:

   ```kotlin
   listOf(
      iosArm64(),
      iosSimulatorArm64(),
   ).forEach {
      it.binaries.framework {
         export(libs.androidx.lifecycle.viewmodel)
         baseName = "shared"
      }
   }
   ```

2. Define your ViewModel in `commonMain` using KMP-ObservableViewModel's ViewModel base class
   and the `@NativeCoroutinesState` annotation:

   ```kotlin
    import com.rickclephas.kmp.observableviewmodel.ViewModel
    import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.asStateFlow
     
    class OrderViewModel : ViewModel() {
        private val _uiState = MutableStateFlow(OrderUiState())

        @NativeCoroutinesState
        val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

        fun setQuantity(n: Int) {
            _uiState.value = _uiState.value.copy(quantity = n)
        }
    }
   ```

3. Use the ViewModel in the iOS UI entry point:

   ```swift
    import SwiftUI
    import shared
    import KMPObservableViewModelSwiftUI

    @main
    struct iOSCupcakeApp: App {
        var body: some Scene {
            WindowGroup {
                CupcakeView()
            }
        }
    }

    struct CupcakeView: View {
        @StateViewModel private var viewModel = OrderViewModel()

        var body: some View {
            VStack {
                Text("Quantity: \(viewModel.uiState.quantity)")
                Text("Price: \(viewModel.uiState.price)")

                Button("Set Quantity to '6'") {
                    viewModel.setQuantity(n: 6)
                }
            }
        }
    }
   ```

### Shared repo/data layer, platform-specific ViewModels and UI

Another option is to only share the data and repository layer while using platform-specific ViewModel implementations.
This allows you to use native patterns on each platform, such as Hilt for Android dependency injection,
or `ObservableObject` with Combine for iOS.

1. Create a shared repository class with the data logic:

    ```kotlin
    class OrderRepository {
       fun calculatePrice(quantity: Int) = "$${quantity * 2}.00"
    }
    ```

2. Implement platform-specific ViewModels.

    * On Android, use the standard Android ViewModel and inject the repository:

        ```kotlin
        class AndroidOrderViewModel(
         private val repo: OrderRepository
        ) : ViewModel() {
     
          val uiState: StateFlow<OrderUiState>
             field = MutableStateFlow(OrderUiState())
     
          fun setQuantity(n: Int) {
             uiState.update {
                it.copy(quantity = n, price = repo.calculatePrice(n))
             }
          }
        }
        ```

    * On iOS, implement the ViewModel natively in Swift using `ObservableObject`:

        ```swift
        import shared
     
        class IOSOrderViewModel: ObservableObject {
           private let repo: OrderRepository
           @Published var uiState: OrderUiState = OrderUiState()
     
           init(repo: OrderRepository) {
               self.repo = repo
           }
     
           func setQuantity(n: Int32) {
               uiState = OrderUiState(quantity: n, price: repo.calculatePrice(quantity: n))
           }
        }
        ```

3. Implement platform-specific UI.

    * On Android:

        ```kotlin
        @Composable
        fun AndroidCupcakeApp(
           viewModel: AndroidOrderViewModel = viewModel { AndroidOrderViewModel(OrderRepository()) }
        ) {
           val uiState by viewModel.uiState.collectAsState()
     
           Column {
               Text("Quantity: ${uiState.quantity}")
               Text("Price: ${uiState.price}")
               Button(onClick = { viewModel.setQuantity(6) }) {
                   Text("Set Quantity to '6'")
               }
           }
        }
        ```

    * On iOS:

        ```swift
        struct IOSCupcakeApp: App {
           @StateObject var viewModel = IOSOrderViewModel(repo: OrderRepository())
     
           var body: some View {
               VStack {
                   Text("Quantity: \(viewModel.uiState.quantity)")
                   Text("Price: \(viewModel.uiState.price)")
                   Button("Set Quantity to '6'") {
                       viewModel.setQuantity(n: 6)
                   }
               }
           }
        }
        ```

## What's next

* Check out the [full sample](https://github.com/JetBrains/compose-multiplatform/tree/master/examples/nav_cupcake).
* See [Set up ViewModel for Kotlin Multiplatform](https://developer.android.com/kotlin/multiplatform/viewmodel) for additional Android-focused guidance.
* Learn how to [integrate Compose Multiplatform with SwiftUI](compose-swiftui-integration.md) when using shared ViewModels with native UI.