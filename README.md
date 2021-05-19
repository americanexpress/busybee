# BusyBee - Tell Espresso when it needs to be patient because your app is busy 🐝

<img src="images/busybee.png" width=100>

BusyBee is an alternative API for [IdlingResource][]s in [Espresso][] tests. You can use BusyBee instead of
[CountingIdlingResource][] to get better log messages and improve your ability to debug problems related to
[IdlingResource][]s.

BusyBee is meant to be used with [Espresso][]. You use BusyBee inside the "app under test". It allows the "app under
test" to tell Espresso when it is `busyWith` an operation and, conversely, allows the app to tell Espresso when the
operation is `completed`. Tracking `busyWith`/`completed` helps your **Espresso tests be fast and reliable**.

If you write [Espresso][] tests, proper use of the [IdlingResource][] API is critical for ensuring that your tests are
fast and reliable. IdlingResource can be hard to use correctly and it can be hard to understand what is happening with
your IdlingResources when you are debugging problems with your tests. That is where BusyBee comes in.

## Comparison with [CountingIdlingResource][]

In some ways, BusyBee is similar to [CountingIdlingResource][], but it does have some notable advantages:

- Rather than track only the _number_ of operations in progress, BusyBee keeps track of the set of operations currently
  in progress. In progress operations are represented by a Java object, which could be a string, request object, etc.
  This allows for easier debugging, as it allows you to inspect the set of in progress operations across the whole app.
- When Espresso times out because the app is busy, your logs can show the list of in progress operations.
- BusyBee lets you separately enable/disable tracking of specific categories of operations (e.g. `NETWORK` operations)
- The `BusyBee#completed(thing)` method is idempotent (`CountingIdlingResource#decrement` is not). This is useful when
  you have unreliable/multiple signals (e.g. WebView) to tell you that an operation has completed. Also, you can
  `completed(thing)` even if you never were `busyWith(thing)`

**Trade-off:** While there are a number of advantages listed above, the downside of `BusyBee` (and
`CountingIdlingResource`) is that you are modifying your app under test for purely testing purposes.

# How to use BusyBee

Include the BusyBee dependencies in your `build.gradle` files. When tests are not running, the **no-op** implementation
is automatically used to minimize overhead of BusyBee (since it is only needed during tests).

[ ![Latest Version](https://api.bintray.com/packages/americanexpress/maven/io.americanexpress.busybee/images/download.svg) ](https://bintray.com/americanexpress/maven/io.americanexpress.busybee/_latestVersion)

_Required_: For Android modules:

```gradle
    implementation 'io.americanexpress.busybee:busybee-android:$version'
```

_Optional_: Only needed, if you want to use BusyBee in a non-Android module:

```gradle
    implementation 'io.americanexpress.busybee:busybee-core:$version'
```

```gradle
    repositories {
        jcenter() // for release builds
        maven { url 'https://oss.jfrog.org/artifactory/libs-snapshot/' } // `-SNAPSHOT` builds from `main`
    }    
```

Inside your _app_, tell `BusyBee` what operations your app is `busyWith`, and when that operation is `completed`.

```java
    class BackgroundProcessor {
        private final BusyBee busyBee = BusyBee.singleton();

        void processThing(Thing thing){
            // Espresso will wait
            busyBee.busyWith(thing);
            try {
                thing.process();
            } finally {
                // Espresso will continue
                busyBee.completed(thing);
            }
        }
    }
```

That's all! Now Espresso will wait until your app is not busy before executing its actions and assertions.

## Categories

Assigning a `Category` to your operations is an advanced feature of `BusyBee`. By default, all operations are in the
`GENERAL` category. But, you can also add operations in other categories such as `NETWORK`. You can toggle tracking for
any category with `payAttentionToCategory`/`ignoreCategory`. When a category is being "ignored" then Espresso will not
wait for operations in that category.

For example, you might want to perform actions on your UI or assert things about your UI while a network request is
still in progress. In this case, you don't want Espresso to wait for the network requests to complete, but you still
want Espresso to wait for other operations in your app. To accomplish this, you would use
`busyBee.ignoreCategory(NETWORK)`, then perform actions and assertions on your UI, then call
`busybee.payAttentionToCategory(NETWORK)` so Espresso will again wait for network operations to complete.

## BusyBeeExecutorWrapper

If you have an executor and you need Espresso to know the app is "busy" anytime that executor is executing something,
then you can wrap the `Executor` with `BusyBeeExecutorWrapper`. Operations executed with the wrapped `Executor` will
cause `BusyBee` to be "busy" while they are in progress.

```java
   Executor backgroundTasks;
   Executor busyBeeBackgroundTasks =
                         BusyBeeExecutorWrapper.with(busyBee)
                                               .wrapExecutor(backgroundTasks)
                                               .build();
    busyBeeBackgroundTasks.execute(operation);
```

## Why is this written in Java and not Kotlin?

We wanted to get an initial release out that didn't depend on the Kotlin standard library, but we plan on converting the
implementation to 100% Kotlin.

## Contributing

We welcome Your interest in the American Express Open Source Community on Github. Any Contributor to any Open Source
Project managed by the American Express Open Source Community must accept and sign an Agreement indicating agreement to
the terms below. Except for the rights granted in this Agreement to American Express and to recipients of software
distributed by American Express, You reserve all right, title, and interest, if any, in and to Your Contributions.
Please [fill out the Agreement][].

## License

Any contributions made under this project will be governed by the [Apache License 2.0][].

The Android™ robot is reproduced or modified from work created and shared by Google and used according to terms
described in the Creative Commons 3.0 Attribution License. Android is a trademark of Google Inc.

## Code of Conduct

This project adheres to the [American Express Community Guidelines][]. By participating, you are expected to honor these
guidelines.

[espresso]: https://developer.android.com/training/testing/espresso
[idlingresource]: https://developer.android.com/reference/androidx/test/espresso/IdlingResource
[countingidlingresource]: https://developer.android.com/reference/androidx/test/espresso/idling/CountingIdlingResource
[fill out the agreement]: https://cla-assistant.io/americanexpress/busybee
[apache license 2.0]: https://github.com/americanexpress/busybee/blob/main/LICENSE.txt
[american express community guidelines]: https://github.com/americanexpress/busybee/blob/main/CODE_OF_CONDUCT.md
