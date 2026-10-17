# Submodule merges and NexAlloy integration

Read this document when merging `morphe-patches` or `morphe-patches-library` and reviewing the results. Both submodules' extension sources participate directly in the main app build; their existing NexAlloy adaptations are part of the integration.

## Which diffs to review before and after merging

For each submodule, record the old fork commit, old upstream baseline, new upstream target, and merge result. Identify the upstream baseline from the merge parents; do not substitute the previous root repository sync commit or an arbitrary nearby tag.

| Comparison | Purpose |
| --- | --- |
| Old upstream baseline → old fork commit | Identify local adaptations that must be preserved or adjusted |
| Old upstream baseline → new upstream target | Find the full scope of upstream additions, deletions, and modifications |
| New upstream target → merge result | Check which local adaptations remain and whether they are still needed |
| Old fork commit → merge result | Review the changes actually introduced by this sync and their supporting adjustments |

Review the conflict's base, ours, and theirs alongside these diffs. Incorporate upstream type, signature, and behavior changes while adapting the existing local connections. Resolve conflicts by meaning rather than choosing one side's entire file. Even automatic merges without textual conflicts need a check that the interfaces remain connected.

## Connections between extensions and the root repository

Empty methods, default return values, or placeholder members marked for filling during patching may depend on offline APK modification to work. Check how the corresponding upstream patch fills them and which local callback, hook, or adapter performs that work.

Preserve existing runtime callback forwarding, dynamic filter registration, and reflection/interface bridges during the merge. When upstream changes parameter types, return types, fields, or interfaces, update the extension side, root repository finders, proxies, and all callback consumers together. Check parameter meaning and call timing. Compiling the Java/Kotlin signatures is insufficient if upstream empty implementations remain in place.

When upstream depends on adding host methods, interfaces, or fields, or changing class inheritance, those structures will not automatically exist in the original host APK. Preserve the corresponding local implementations using `createProxy`, `bindProxy`, reflection, composition, and lifecycle hooks, and connect new upstream behavior to these runtime entry points. For new extension constant returns or checks for whether a patch is included, check whether local entry points such as `setExtensionIsPatchIncluded` must supply the value.

## Resources, settings, and build information

- Preserve module resource injection and lookup: `ResourceUtils.fallbackPackageName` points to the module resource package, which is searched before falling back to the host. Check the dependency order for context, Activity resources, and classloader initialization.
- Preserve runtime Preference construction: build settings pages from locally collected builders, set the correct SharedPreferences name, and assign properties before running attachment logic that depends on them. Coordinate upstream XML initialization code with this flow.
- Preserve build values supplied through `BuildConfig.PATCH_VERSION`, `BuildConfig.DEBUG`, and local app information. Upstream empty or default values awaiting replacement during patching must not overwrite these sources.
- For upstream settings moves, icon/layout changes, and new resource directories, check local screen definitions, Preference models, and entry points under `app/src/main/res`. Resource copying is selective; adding a resource in a submodule does not mean the app build includes it.

These rules preserve the existing architecture, without requiring every old implementation to remain. When upstream removes or replaces a mechanism, check whether its adapters still have callers.

## Supporting changes in the root repository

As required by the actual changes, review extension/proto source sets, Java exclusions, string generation and resource copying tasks in `app/build.gradle.kts`, and dependency version declarations in the root repository. Adjust them only where this integration requires it.

Changes to shared extension interfaces or public methods may require updates under `app/src/main/java/io/github/nexalloy/morphe/shared` and to consumers in multiple apps. When the compatibility layer lacks a lookup capability required by this sync, inspect the semantics of `FingerprintCompat.kt`, `InstructionFilter.kt`, `Utils.kt`, and existing helpers, add reusable support, and check affected callers.

The final result should make the connections traceable: where upstream changes enter the local runtime, which consumers were updated, and which original placeholders are still handled locally. For unimplemented features, retain upstream comments, complete but disabled Preferences, and corresponding TODOs. Do not hide gaps by deleting declarations, swallowing exceptions, or excluding required source files.
