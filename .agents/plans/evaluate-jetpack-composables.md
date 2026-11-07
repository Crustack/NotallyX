# Jetpack Compose Evaluation Template

This document serves as an evaluation template for every `@Composable` function found in the NotallyX codebase against the best practices defined in the `/evaluate-jetpack-composables` skill.

Each Composable includes checkboxes for the 6 core evaluation categories:
1. **Modifiers Usage & Application**
2. **Compose Previews Completeness**
3. **State Hoisting & Parameter Hygiene**
4. **Function Complexity & Splitting Boundaries**
5. **Package Structure & Component Locality**
6. **Performance & State Best Practices**

> [!NOTE]
> Checkboxes are left blank (`[ ]`) intentionally to serve as a progressive analysis template. Do not modify source code.

---

## Evaluation Checklist by File & Composable

### 1. `core/common/error/ErrorScreen.kt`
#### `ErrorScreen`
- [x] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [x] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [x] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [x] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [x] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [x] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ErrorScreenPreview`
- [x] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [x] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [x] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [x] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [x] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [x] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 2. `core/ui/components/BaseNoteCard.kt`
#### `BaseNoteCard`
- [x] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [x] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [x] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [x] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [x] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [x] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `parseNoteColor`
- [x] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [x] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [x] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [x] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [x] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [x] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `LoremIpsumNotePreview`
- [x] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [x] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [x] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [x] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [x] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [x] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ListNotePreview`
- [x] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [x] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [x] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [x] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [x] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [x] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `RichTextNotePreview`
- [x] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [x] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [x] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [x] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [x] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [x] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `GeneratedListNotePreview`
- [x] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [x] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [x] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [x] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [x] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [x] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `LoremIpsumNoteSelectedPreview`
- [x] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [x] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [x] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [x] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [x] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [x] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `LoremIpsumNoteSelectedDarkPreview`
- [x] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [x] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [x] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [x] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [x] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [x] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 3. `core/ui/components/ColorSwatchItem.kt`
#### `ColorSwatchItem`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ColorSwatchItemPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 4. `core/ui/components/ErrorStateCard.kt`
#### `ErrorStateCard`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ErrorStateCardPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 5. `core/ui/components/LabelChip.kt`
#### `LabelChip`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `LabelChipPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 6. `core/ui/components/MenuItemRow.kt`
#### `MenuItemRow`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `MenuItemRowPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 7. `core/ui/components/ReminderChip.kt`
#### `ReminderChip` (overload 1)
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ReminderChip` (overload 2)
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 8. `core/ui/components/SectionHeaderItem.kt`
#### `SectionHeaderItem`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `SectionHeaderItemPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 9. `core/ui/dialog/ColorPickerDialog.kt`
#### `ColorPickerDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 10. `core/ui/dialog/ConfirmDialog.kt`
#### `ConfirmDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 11. `core/ui/dialog/ErrorDialog.kt`
#### `ErrorDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 12. `core/ui/dialog/ProgressDialog.kt`
#### `ProgressDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 13. `core/ui/dialog/SelectionDialog.kt`
#### `SingleSelectionDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 14. `core/ui/dialog/TextInputDialog.kt`
#### `TextInputDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `DualTextInputDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 15. `core/ui/theme/NotallyXTheme.kt`
#### `NotallyXTheme`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 16. `core/ui/views/TriStateCheckBox.kt`
#### `TriStateMultiChoiceContent`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `TriStateChoiceItemRow`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `TriStateChoiceItemRowPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 17. `feature/editor/EditActivity.kt`
#### `RenderScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 18. `feature/editor/EditListActivity.kt`
#### `RenderScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 19. `feature/editor/EditNoteActivity.kt`
#### `RenderScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 20. `feature/editor/EditTextPlainActivity.kt`
#### `RenderScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 21. `feature/editor/bottomsheet/ActionModalBottomSheet.kt`
#### `ActionBottomSheetContent`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ActionModalBottomSheet`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 22. `feature/editor/formatting/TextFormattingBar.kt`
#### `TextFormattingBar`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `TextFormattingBarPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 23. `feature/editor/screens/ChecklistItemRow.kt`
#### `ChecklistItemRow`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ChecklistItemRowPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 24. `feature/editor/screens/EditScreen.kt`
#### `ActionIconButton`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `EditScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `EditNoteScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `EditListScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `EditNoteScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `EditListScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 25. `feature/editor/screens/FileAttachmentPreviewChip.kt`
#### `FileAttachmentPreviewChip`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ImagePreviewCard`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `FileAttachmentPreviewChipPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ImagePreviewCardPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 26. `feature/labels/LabelItemRow.kt`
#### `LabelItemRow`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `SelectableLabelRow`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `LabelItemRowPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `SelectableLabelRowPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 27. `feature/labels/LabelsScreen.kt`
#### `LabelsScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `LabelsScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 28. `feature/labels/SelectLabelsScreen.kt`
#### `SelectLabelsScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `SelectLabelsScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 29. `feature/main/DrawerHeaderView.kt`
#### `DrawerHeaderView`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `DrawerHeaderViewPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 30. `feature/main/MainScreen.kt`
#### `MainScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ActionModeActions`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `MainScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 31. `feature/main/NotesGridScreen.kt`
#### `NotesGridScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `NotesGridScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 32. `feature/main/PickNoteScreen.kt`
#### `PickNoteScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `PickNoteScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 33. `feature/media/audio/AudioItemRow.kt`
#### `AudioItemRow`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `AudioItemRowPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 34. `feature/media/audio/PlayAudioScreen.kt`
#### `PlayAudioScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `PlayAudioScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 35. `feature/media/audio/RecordAudioScreen.kt`
#### `RecordAudioScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `RecordingTimer`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `RecordAudioScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 36. `feature/media/image/ViewImageScreen.kt`
#### `ViewImageScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ViewImageScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 37. `feature/migration/backup/DatabaseTransitionScreen.kt`
#### `DatabaseTransitionScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `DatabaseTransitionScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 38. `feature/reminder/ReminderChipItem.kt`
#### `ReminderChipItem`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `NoteReminderRow`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ReminderChipItemPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `NoteReminderRowPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 39. `feature/reminder/ReminderRepetitionDialog.kt`
#### `ReminderRepetitionDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `CustomRepetitionDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 40. `feature/reminder/RemindersScreen.kt`
#### `RemindersScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `RemindersScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 41. `feature/search/SearchScreen.kt`
#### `SearchScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `SearchScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 42. `feature/settings/PreferenceRow.kt`
#### `PreferenceRow`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `PreferenceRowPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 43. `feature/settings/PreferenceToggleDialog.kt`
#### `PreferenceBooleanDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `PreferenceEnumWithToggleDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 44. `feature/settings/SeekBarPreferenceRow.kt`
#### `SeekBarPreferenceRow`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `SeekBarPreferenceRowPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `SeekBarPreferenceRowValuePreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 45. `feature/settings/SettingsScreen.kt`
#### `SettingsScreen`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `SettingsScreenPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 46. `feature/settings/StepperPreferenceRow.kt`
#### `StepperPreferenceRow`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `repeatingClickable` (Modifier Composable)
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `StepperPreferenceRowPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 47. `feature/settings/ToggleOptionItem.kt`
#### `ToggleOptionItem`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

#### `ToggleOptionItemPreview`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 48. `feature/settings/dialogs/DateTimeFormatDialog.kt`
#### `DateTimeFormatDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 49. `feature/settings/dialogs/ImportNotesDialog.kt`
#### `ImportNotesDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).

---

### 50. `feature/settings/dialogs/NotesSortDialog.kt`
#### `NotesSortDialog`
- [ ] **Modifiers**: Accepts optional `modifier: Modifier = Modifier` as first optional parameter and applies to root layout element with strict default and proper chaining.
- [ ] **Previews**: Has `@Preview` defined, passes mock/sample values for required parameters, handles event callbacks, and uses theme (`NotallyXTheme`).
- [ ] **State Hoisting**: Avoids parameter bloat (>5 primitives), bundles UI state if needed, separates stateless/stateful concerns, and adheres to UDF.
- [ ] **Complexity**: Maintains single responsibility, appropriate function length, and judicious splitting.
- [ ] **Package Locality**: Placed in correct core/common package with appropriate visibility scoping.
- [ ] **Performance**: Expensive calculations remembered, lazy keys stable, uses `derivedStateOf` / lambda modifiers where applicable, no backwards writes, observable collections, and lifecycle-aware collection (`collectAsStateWithLifecycle`).
