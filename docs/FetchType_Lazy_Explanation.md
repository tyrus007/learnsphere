# `FetchType.LAZY` in LearnSphere Entities

This document explains what lazy loading means in JPA and why the LearnSphere entities use `FetchType.LAZY`.

The relevant places in the code are:

- [Course.java](../course-service/src/main/java/com/ashique/courseservice/entity/Course.java)
- [Module.java](../course-service/src/main/java/com/ashique/courseservice/entity/Module.java)
- [Lesson.java](../course-service/src/main/java/com/ashique/courseservice/entity/Lesson.java)

## What lazy loading means

`FetchType.LAZY` tells JPA not to load the related object immediately.

Instead, JPA loads the relation only when your code actually accesses it.

Example:

```java
Module module = moduleRepository.findById(id).orElseThrow();
```

If `module.course` is lazy, JPA may return a proxy object first and fetch the real `Course` only when `module.getCourse()` is accessed.

## Where this project uses it

### `@ManyToOne(fetch = FetchType.LAZY)`

In this codebase, these relationships are lazy:

- `Module.course`
- `Lesson.module`

This is important because `@ManyToOne` is often eager by default in JPA, so explicitly setting `LAZY` avoids loading the parent automatically every time.

### `@OneToMany(fetch = FetchType.LAZY)`

These collections are also lazy:

- `Course.modules`
- `Module.lessons`

This is the default behavior for `@OneToMany`, but the code makes the intent explicit.

## Why lazy loading is useful

Lazy loading prevents JPA from loading the whole object graph when you only need one entity.

That matters here because the model is hierarchical:

- `Course` has many `Module`
- `Module` has many `Lesson`
- each `Lesson` points back to a `Module`
- each `Module` points back to a `Course`

If everything were loaded eagerly, fetching one course could pull in a large amount of data immediately.

Benefits of lazy loading:

- less SQL on the initial query
- smaller object graphs in memory
- better performance for endpoints that only need summary data
- reduced risk of circular loading problems

## What happens if we do not use it

If relationships are eager, JPA may load much more data than needed.

That can cause:

- slow queries
- too many joins
- large payloads in memory
- repeated database access for nested graphs
- recursion problems during serialization if the object graph is exposed directly

This becomes especially expensive in a structure like courses, modules, and lessons, where the tree can grow large.

## The main tradeoff

Lazy loading saves work up front, but it only works while the persistence context is still open.

If you try to access a lazy relation after the session is closed, you can get:

- `LazyInitializationException`

Typical example:

```java
Course course = courseRepository.findById(id).orElseThrow();
// if the transaction/session is already closed here:
course.getModules().size();
```

If `modules` was not initialized earlier, JPA may not be able to fetch it anymore.

## How to work with lazy loading safely

Common approaches are:

- access needed relations inside a transactional service method
- map entities to DTOs before returning them
- use fetch joins or entity graphs when a query really needs the related data

The important idea is: only load what you need, when you need it.

## Why this project uses it

For LearnSphere, lazy loading helps keep the model efficient because:

- course, module, and lesson graphs can be deep
- most API calls do not need the full tree
- bidirectional relationships exist
- it reduces unnecessary database traffic

## Short version

`FetchType.LAZY` means "do not load this relation until I actually use it."

In LearnSphere, that is a good fit because the entity graph is nested and bidirectional.

Without lazy loading, you can get heavier queries, more memory usage, and unnecessary database work.

The tradeoff is that you must access lazy relations while the JPA session is still open, or map them to DTOs first.

