# `orphanRemoval` in LearnSphere Entities

This document explains why the LearnSphere entities use `orphanRemoval = true` on their child collections, and what would happen if we did not use it.

The two places where this appears are:

- [Course.java](../course-service/src/main/java/com/ashique/courseservice/entity/Course.java)
- [Module.java](../course-service/src/main/java/com/ashique/courseservice/entity/Module.java)

## What `orphanRemoval` means

`orphanRemoval = true` tells JPA:

- if a child entity is removed from its parent's collection
- and that child is no longer referenced by any parent
- then delete that child row from the database

In this project:

- removing a `Module` from `Course.modules` deletes that `Module`
- removing a `Lesson` from `Module.lessons` deletes that `Lesson`

Example:

```java
Course course = courseRepository.findById(id).orElseThrow();
course.getModules().remove(module);
courseRepository.save(course);
```

If the entity is managed and the persistence context is flushed, JPA treats the removed module as an orphan and deletes it.

## Why this is useful here

`Course`, `Module`, and `Lesson` form a parent-child hierarchy:

- one course has many modules
- one module has many lessons

If a module is removed from a course, the module should not stay behind as an unused row in the database.
If a lesson is removed from a module, the lesson should not remain as dead data.

So `orphanRemoval` matches the business meaning of the model:

- child records belong to one parent
- if the parent no longer owns them, they should be deleted

## What happens if we do not use it

Without `orphanRemoval = true`, removing a child from the collection does not automatically delete it.

That can cause several problems:

- stale rows remain in the database
- deleted modules or lessons may still appear in queries later
- data becomes inconsistent with the object graph in memory

In this codebase, the foreign keys are `nullable = false`:

- `Module.course` is required
- `Lesson.module` is required

So if `orphanRemoval` is off and the child is removed from the collection, JPA may try to null out the foreign key instead of deleting the row. Because the FK is not nullable, this can fail with a constraint violation.

## `orphanRemoval` is not the same as `cascade = CascadeType.REMOVE`

These two settings are related but not identical.

- `cascade = CascadeType.REMOVE` means deleting the parent also deletes the children.
- `orphanRemoval = true` means removing the child from the parent's collection deletes the child.

In this project, both are useful together:

- delete a course, and its modules and lessons should disappear
- remove a module from a course, and that module should disappear too

## What problem it prevents

Without orphan removal, you can easily end up with:

- orphaned modules that no longer belong to a course
- orphaned lessons that no longer belong to a module
- database rows that no longer match the in-memory domain model
- extra cleanup code in service methods

## Practical rule for this codebase

Use `orphanRemoval = true` when:

- the child belongs to exactly one parent
- the child should not exist on its own
- removing it from the parent should delete it

That is exactly the case for:

- `Course -> Module`
- `Module -> Lesson`

## Short version

`orphanRemoval = true` means "if I remove this child from the parent collection, delete it from the database."

In LearnSphere, this is the correct behavior because modules belong to a course, and lessons belong to a module.

Without it, you risk stale rows, constraint errors, and data that no longer matches the object graph.

