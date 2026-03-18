# `equals()` and `hashCode()` in LearnSphere Entities

This document explains why the entity classes in LearnSphere use Lombok's `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` and what can go wrong if we do not define equality correctly.

The entities that use this pattern are:

- [Course.java](../course-service/src/main/java/com/ashique/courseservice/entity/Course.java)
- [Module.java](../course-service/src/main/java/com/ashique/courseservice/entity/Module.java)
- [Lesson.java](../course-service/src/main/java/com/ashique/courseservice/entity/Lesson.java)
- [User.java](../identity-service/src/main/java/com/ashique/identity_service/entity/User.java)

## What `equals()` and `hashCode()` do

Java uses these two methods to decide whether two objects should be treated as the same logical value.

- `equals()` answers: "Are these two objects logically equal?"
- `hashCode()` answers: "Which bucket should this object go into in a hash-based collection?"

Collections such as `HashSet`, `HashMap`, and `LinkedHashSet` depend on both methods.

Example:

```java
Set<Course> courses = new HashSet<>();
courses.add(course1);
courses.add(course2);
```

If `course1.equals(course2)` is `true`, the set treats them as the same element.

## What this project is doing

In these entities, Lombok generates `equals()` and `hashCode()` using only the `id` field:

```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;
}
```

The same pattern appears in `Module`, `Lesson`, and `User`.

That means:

- Two objects are considered equal if their `id` values are equal.
- Other fields like `title`, `email`, `description`, or `role` do not affect equality.

## Why this is usually the right choice for JPA entities

These classes are database entities, not plain DTOs. Their identity is the primary key, not all of their fields.

Using only `id` has several benefits:

- A course does not become a "different" object just because its title changes.
- A user does not stop being the same user after their email or role is updated.
- Collections like `Set` can behave predictably when the entity is loaded more than once from the database.
- Avoids pulling in relationship fields such as `course`, `module`, or `lessons`, which can cause recursion or lazy-loading problems.

## What can go wrong if we do not use it

### 1. Duplicate objects in collections

Without a correct `equals()`/`hashCode()`, two objects that represent the same row can look different to Java.

Example:

```java
Course a = courseRepository.findById(id).orElseThrow();
Course b = courseRepository.findById(id).orElseThrow();

Set<Course> set = new HashSet<>();
set.add(a);
set.add(b);
```

If equality is based on object identity instead of entity identity, the set may contain both objects even though they point to the same database row.

### 2. `HashMap` and `HashSet` lookups fail

Hash-based collections first use `hashCode()` and then `equals()`.

If the two methods are inconsistent, you may not be able to find an object that was already inserted.

Typical symptoms:

- `set.contains(entity)` returns `false` even though the entity is inside the set.
- `map.get(entity)` returns `null` for a key that was previously inserted.

### 3. Infinite recursion with bidirectional relationships

These entities are connected in both directions:

- `Course -> modules`
- `Module -> course`
- `Module -> lessons`
- `Lesson -> module`

If Lombok includes all fields in equality, it can walk across the object graph:

- `Course.equals()` checks `modules`
- each `Module.equals()` checks `course`
- which checks `modules` again

That can lead to stack overflow errors or extremely expensive comparisons.

### 4. Lazy-loading side effects

JPA relationships are often `LAZY`.

If `equals()` or `hashCode()` touches a lazy field, Hibernate may need to load data from the database just to compare two objects. That can cause:

- unexpected SQL queries
- slower performance
- `LazyInitializationException` when the session is already closed

Using only `id` avoids that risk.

### 5. Wrong equality when fields change

If equality uses mutable fields such as `title`, `email`, or `position`, changing one of those fields after putting the entity into a `HashSet` can break the collection.

Example:

```java
Set<User> users = new HashSet<>();
users.add(user);

user.setEmail("new@email.com");
```

If `email` were part of `hashCode()`, the object could "move" to a different hash bucket logically, and `contains()` or `remove()` could stop working.

## Important caveat with `id`-only equality

There is one subtle issue to understand.

These entities use generated IDs:

```java
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

Before an entity is saved, `id` is usually `null`.

That means two brand-new unsaved entities can compare as equal if they both have `null` IDs. In practice, this matters when:

- you put transient entities into a `Set`
- you compare entities before persistence
- you use entity instances as `Map` keys before the ID exists

So the rule of thumb is:

- use entity equality mainly for persisted objects
- avoid relying on `equals()` for brand-new objects that have not been saved yet

## Why not include every field

Including all fields is usually a bad fit for JPA entities because:

- entities have relationships that can create recursion
- fields can be lazy-loaded
- mutable fields can break hash-based collections
- equality becomes slower and less predictable

For entities, the primary key is the stable identity. That is why this project uses `id` only.

## Practical summary for this codebase

For [Course.java](../course-service/src/main/java/com/ashique/courseservice/entity/Course.java), [Module.java](../course-service/src/main/java/com/ashique/courseservice/entity/Module.java), [Lesson.java](../course-service/src/main/java/com/ashique/courseservice/entity/Lesson.java), and [User.java](../identity-service/src/main/java/com/ashique/identity_service/entity/User.java):

- equality is based on the database ID
- object graphs are kept out of equality checks
- hash-based collections behave more predictably
- JPA proxy and lazy-loading issues are reduced

## Short version

`equals()` and `hashCode()` tell Java when two objects should be treated as the same.

In LearnSphere entities, using only `id` is a deliberate design choice because these classes represent database rows, not value objects.

If we do not define them carefully, we can get:

- duplicate entities in sets
- failed map lookups
- recursion in bidirectional relationships
- lazy-loading problems
- inconsistent behavior after field updates

