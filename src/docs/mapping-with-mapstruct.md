# Mapping with MapStruct

MapStruct generates the boilerplate that converts one object into another. You
declare an interface describing *what* should be converted; an annotation
processor writes the implementation at compile time.

Because the code is generated during `compileJava`, there is no reflection at
runtime and no surprises in production: if a field cannot be mapped you find out
from the compiler, not from a null value in a JSON response.

## Why this project needs it

The layers each have their own representation of a book:

```mermaid
flowchart LR
    Book["Book<br/><i>persistence</i>"] -->|BookMapper| BookView["BookView<br/><i>domain</i>"]
    BookView -->|DtoMapper| BookDto["BookDto<br/><i>web</i>"]
    BookDto -->|Jackson| JSON["JSON response"]
```

Keeping them separate is what stops a JPA entity from reaching the web layer.
That matters for two concrete reasons:

- A column rename in `Book` does not silently change the REST contract.
- Jackson never serializes a Hibernate proxy. Serializing `Book` directly would
  walk `authors` → `books` → `authors` and recurse forever, or blow up with
  `LazyInitializationException` once the transaction has closed.

## Build setup

Already configured in `build.gradle.kts`:

```kotlin
extra["mapstructVersion"] = "1.6.3"

dependencies {
    implementation("org.mapstruct:mapstruct:${property("mapstructVersion")}")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstructVersion")}")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Amapstruct.defaultComponentModel=spring")
    options.compilerArgs.add("-Amapstruct.defaultInjectionStrategy=constructor")
}
```

Three things are easy to get wrong:

| Line | Why it matters |
|------|----------------|
| `mapstruct` on `implementation` | The annotations (`@Mapper`, `@Mapping`) are needed at compile time and referenced by generated code. |
| `mapstruct-processor` on `annotationProcessor` | On `implementation` javac never runs it — **no implementations are generated** and the mapper fails at runtime with `Cannot find implementation`. |
| `lombok-mapstruct-binding` | Our entities get their getters/setters from Lombok. Without the binding, MapStruct may run before Lombok and see a bean with no properties, producing empty mappers. |

The `defaultComponentModel=spring` argument makes every generated mapper a
`@Component`, so mappers can be constructor-injected like any other bean. Without
it you would need `Mappers.getMapper(BookMapper.class)` or
`@Mapper(componentModel = "spring")` on each interface.

`defaultInjectionStrategy=constructor` decides how a mapper receives *other*
mappers it delegates to via `uses`. MapStruct's default is an `@Autowired` field,
so the generated class keeps a public no-arg constructor that leaves the delegate
null — fine under Spring, a `NullPointerException` anywhere else. With this line
the delegate becomes a constructor parameter instead: the usual Spring
convention, and the compiler now refuses to build a mapper without its delegates
rather than letting one exist half-initialised.

If you ever declare a `@Mapper` in *test* sources, mirror the processor entries
onto `testAnnotationProcessor` as well.

## Writing a mapper

Views and DTOs are best modelled as records — immutable, and MapStruct maps them
through the canonical constructor.

```java
package com.learning.hibernatelab.domain;

public record BookView(
        Long id,
        String title,
        String description,
        PublisherRef publisher,
        List<AuthorRef> authors,
        List<EditorRef> editors,
        List<String> tagNames
) {
    public BookView {
        if (authors == null) {
            authors = List.of();
        } else {
            authors = List.copyOf(authors);
        }

        // ... same for editors and tagNames
    }
}
```

Relations resolve to small **reference records** rather than to entities or to
other views:

```java
public record AuthorRef(Long id, String name) {}
public record BookRef(Long id, String title) {}
```

A ref holds only scalars, never a collection. That is what keeps the views
acyclic — `Author` → `Book` → `Author` would otherwise recurse forever, in the
mapper and again in Jackson. Carrying the `id` matters because `author.name`,
`editor.name` and `publisher.name` are all deliberately non-unique (migrations
`V5` and `V6`), so a bare name cannot identify a row. `tagNames` is the
exception: `tag.name` *is* unique (`V4`), so plain strings lose nothing.

That `public BookView {` with no parameter list is a **compact constructor**, a
record-only form. Java declares the parameters for you from the record header,
and assigns each field from its parameter once the body finishes — so assigning
to the bare `authors` inside the body changes what lands in the field.
(Writing `this.authors = …` there is a compile error.)

The normalisation is worth the lines. Records are only *shallowly* immutable, so
without `List.copyOf` a caller could mutate the list after construction and
change what the view reports. And the `List.of()` branch replaces the `null` that
MapStruct passes for an empty relation, so consumers never have to null-check
`authors()`. Refs need no compact constructor — they hold only scalars.

Every view mapper needs at least one entity-to-ref conversion, so they live in
one interface rather than being repeated:

```java
package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Author;
import com.learning.hibernatelab.persistence.Book;
import com.learning.hibernatelab.persistence.Editor;
import com.learning.hibernatelab.persistence.Publisher;
import org.mapstruct.Mapper;

@Mapper
public interface RefMapper {

    BookRef toRef(Book book);

    AuthorRef toRef(Author author);

    EditorRef toRef(Editor editor);

    PublisherRef toRef(Publisher publisher);
}
```

Overloading `toRef` is fine — MapStruct resolves by parameter type.

A mapper is then an interface in the same package as its target type, pulling in
`RefMapper` with `uses`:

```java
package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Book;
import com.learning.hibernatelab.persistence.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = RefMapper.class)
public interface BookMapper {

    @Mapping(target = "tagNames", source = "tags")
    BookView toView(Book book);

    List<BookView> toViews(List<Book> books);

    default String tagName(Tag tag) {
        return tag.getName();
    }
}
```

Points worth noting:

- `id`, `title` and `description` need no annotation — names match, so they are
  mapped automatically.
- `publisher`, `authors` and `editors` need none either. The names match, and
  MapStruct finds the `toRef` methods on `RefMapper` to convert each element.
- Only `tagNames` is annotated, because the name differs from the source
  property `tags`.
- `toViews` needs no body. Given `toView`, MapStruct writes the loop.
- The `default` method is ordinary Java. MapStruct spots that it needs a
  `Tag` → `String` conversion and calls `tagName` for each element.
- A book with no publisher yields a null `publisher()` rather than an exception —
  but note *where* that null check lives. The generated code calls
  `refMapper.toRef(book.getPublisher())` unconditionally; it is `toRef` itself
  that returns null for a null argument.
- The list relations come back in **unspecified order**. The entity side is a
  `HashSet`, and the generated loop just iterates it, so `authors()` has no
  stable order from one run to the next. If a caller needs one — a JSON response
  that should not churn, say — sort in the mapper or in the service; do not
  assume the order you happen to observe.

The four remaining mappers are the same shape without the `tagNames` line:

```java
@Mapper(uses = RefMapper.class)
public interface AuthorMapper {
    AuthorView toView(Author author);
    List<AuthorView> toViews(List<Author> authors);
}
```

Inject it like any bean:

```java
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @Transactional(readOnly = true)
    public Optional<BookView> findById(Long id) {
        return bookRepository.findWithPublisherById(id).map(bookMapper::toView);
    }
}
```

Two things in those four lines carry the whole next section: the mapper call is
inside the transaction, and the repository method was chosen for what the mapper
is about to touch.

### Why declare the refs at all?

It is tempting to leave the `toRef` methods out entirely — and everything still
compiles if you do. That is the trap. When no declared method converts `Book` to
`BookRef`, MapStruct silently generates a `protected bookToBookRef` into *every*
implementation that needs one. Four mappers, four private copies of the same
conversion, none of them callable, testable, or overridable.

Declaring `RefMapper` turns that invisible generated code into one named thing
with one place to change. The rule of thumb: if two mappers need the same
conversion, give it a home.

## The JPA trap: map inside the transaction

This is the mistake to expect in this project. `Book.authors`, `Book.tags` and
`Book.editors` are `@ManyToMany` and therefore **lazy by default**. The mapper
touches all three.

```java
// BROKEN — no transaction, so the proxies are detached
public BookView findById(Long id) {
    Book book = bookRepository.findById(id).orElseThrow();
    return bookMapper.toView(book);   // LazyInitializationException
}
```

Two rules follow.

### 1. Call the mapper inside `@Transactional`

Mapping belongs in the service, never in the controller. If the session is
closed the lazy collections cannot be read. This is why every service method
that returns a view is annotated, `readOnly = true` for the queries, and why the
controllers receive finished views and never touch an entity, a repository or a
transaction.

### 2. Fetch what the mapper will touch

Otherwise you get N+1 — one query for the book, then one per collection, per
book. The right tool depends on how many relations the view needs, and this
project has both shapes.

**One collection: use an entity graph.** `AuthorView`, `EditorView`, `TagView`
and `PublisherView` each need only `books`, so the repository join-fetches it.
One collection is one join, and there is nothing for it to multiply against:

```java
@Override
@EntityGraph(attributePaths = "books")
List<Author> findAll();

@EntityGraph(attributePaths = "books")
Optional<Author> findWithBooksById(Long id);
```

**Several collections: do not.** `BookView` needs `publisher`, `authors`,
`editors` and `tags`. One graph covering all four would join three many-to-manys
at once and hand Hibernate the cartesian product of them. So `BookRepository`
fetches only the `@ManyToOne`:

```java
@Override
@EntityGraph(attributePaths = "publisher")
List<Book> findAll();
```

and the three collections are batch-fetched on the entity instead:

```java
@BatchSize(size = 50)
@ManyToMany(mappedBy = "books")
private Set<Author> authors = new HashSet<>();
```

Fetching the `@ManyToOne` is not optional busywork: `@ManyToOne` is **EAGER by
default**, so without that graph a list of books costs one extra query per book.

### What that actually produces

`GET /api/books` over six books, read from the SQL log with
`logging.level.org.hibernate.SQL: DEBUG`:

```sql
-- 1. books + publisher, one query (the entity graph)
select b1_0.id, b1_0.description, p1_0.id, p1_0.name, b1_0.title
from book b1_0 left join publisher p1_0 on p1_0.id=b1_0.publisher_id

-- 2-4. one batched query per collection (@BatchSize)
select ... from book_tag    t1_0 join tag    t1_1 ... where t1_0.book_id = any (?)
select ... from author_book a1_0 join author a1_1 ... where a1_0.book_id = any (?)
select ... from editor_book e1_0 join editor e1_1 ... where e1_0.book_id = any (?)
```

**Four queries, and the count does not grow with the number of books** — six or
fifty, still four, because the batch size is 50. The `= any (?)` is the tell:
Hibernate collected the ids of every book in the session and fetched each
collection for all of them at once.

The four is measured. The counterfactual is arithmetic: strip `@BatchSize` and
each collection falls back to one query per book, so the same request becomes
1 + 3 × 6 = 19, and it grows with every book added. Drop the entity graph too
and the eager `publisher` adds six more.

Note also `left join`, not an inner join, so a book with no publisher still
appears — `publisher_id` is nullable, and V5 made it `ON DELETE SET NULL`.

MapStruct cannot warn you about any of this — the generated code just calls
getters. The cost is in what those getters do, and the only way to know is to
turn on the SQL log and count.

## Mapping to the web layer

The domain view is not the REST contract. `DtoMapper` converts one to the other,
and the DTOs mirror the views — including the reference records:

```java
public record BookRefDto(Long id, String title) {}
public record AuthorRefDto(Long id, String name) {}
```

The duplication is the point. `BookDto` is published to clients; `BookView` is
internal. Renaming a field in the view becomes a mapper change here rather than
a breaking change for everyone consuming the API.

Unlike `domain`, this layer has **one mapper for everything**:

```java
@Mapper
public interface DtoMapper {

    AuthorDto toDto(AuthorView view);
    BookDto toDto(BookView view);
    // ...

    List<AuthorDto> toAuthorDtos(List<AuthorView> views);
    List<BookDto> toBookDtos(List<BookView> views);
    // ...

    BookRefDto toDto(BookRef ref);
    AuthorRefDto toDto(AuthorRef ref);
}
```

That is not a contradiction of *Why declare the refs at all?*. `RefMapper` exists
in `domain` because five mappers needed the same conversion and would otherwise
have got five private copies. Here there is only one mapper, so the ref
conversions already have exactly one home.

Two details worth knowing:

- The list methods need **distinct names**. Erasure makes `List<AuthorView>` and
  `List<BookView>` the same signature, so `toDto` cannot be overloaded for them
  the way it can for the single-object methods.
- The DTOs need **no compact constructor**. The views they are mapped from have
  already replaced nulls with empty lists, so `authors()` on a `BookDto` is never
  null either — normalising twice would only hide where it happens.

The result over the wire, for a book with nothing attached:

```json
{ "id": 80, "title": "Untitled Draft", "description": null,
  "publisher": null, "authors": [], "editors": [], "tagNames": [] }
```

`publisher` is null because that is a real state; the collections are `[]`
because the view's compact constructor made them so. And nothing recurses: a
`BookRefDto` inside an `AuthorDto` holds only scalars, so Jackson cannot walk
back to the authors again.

Where each *write* goes — which service may change which relation, and why the
endpoints are shaped the way they are — is a separate concern from mapping. That
is [service-and-web-layers.md](service-and-web-layers.md).

## Common cases

**Renaming and ignoring**

```java
@Mapping(target = "name", source = "title")
@Mapping(target = "internalNotes", ignore = true)
BookSummary toSummary(Book book);
```

**Constants and expressions**

```java
@Mapping(target = "source", constant = "hibernatelab")
@Mapping(target = "slug", expression = "java(book.getTitle().toLowerCase())")
```

**Updating an existing entity** — for `PUT`/`PATCH` handlers, where you want to
mutate a managed entity rather than build a new one:

```java
@Mapping(target = "id", ignore = true)
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void updateBookFromDto(BookDto dto, @MappingTarget Book book);
```

`@MappingTarget` writes into the passed instance. Always `ignore` the `id`, and
never map incoming collections onto an entity's collections this way — replacing
the `Set` instance breaks Hibernate's dirty tracking. Handle relations
explicitly in the service.

**Composing mappers** — `uses` lets one mapper delegate to another, which is how
every view mapper here reaches `RefMapper`:

```java
@Mapper(uses = RefMapper.class)
public interface BookMapper { ... }
```

The generated implementation receives the delegate as a constructor-injected
Spring bean and calls it — `refMapper.toRef(author)` — rather than rolling its
own copy. It takes several: `uses = {RefMapper.class, DateMapper.class}`.

## Catching mistakes at compile time

By default an unmapped target property is only a warning, which is easy to miss.
Tighten it per mapper while you are learning:

```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
```

Or globally in `build.gradle.kts`:

```kotlin
options.compilerArgs.add("-Amapstruct.unmappedTargetPolicy=ERROR")
```

Note that records make this stricter automatically: every constructor component
must be supplied, so a forgotten field is a compile error regardless.

### What no policy will catch

`unmappedTargetPolicy` inspects the *target*. A mapper whose **source** type is
wrong is still a perfectly valid mapper, so it compiles in silence:

```java
// Compiles. Generates a useless TagView → TagView copy.
TagView toView(TagView tagView);

// Compiles. Generates a useless EditorRef → EditorRef copy.
EditorRef toRef(EditorRef editor);
```

What makes this hard to spot is that the mapping you *meant* still appears in the
implementation — as a private helper, generated so that `toViews` has something
to call — so the generated file looks broadly correct at a glance. Meanwhile the
public method you would actually call does nothing useful, and there is no way to
map a single entity at all.

Nothing in the build warns about either one, and nothing will until real calling
code exists. What catches it is a service that tries to hand the method an
entity — `bookRepository.findById(id).map(tagMapper::toView)` does not compile
against `toView(TagView)`. Until a caller exists, read the generated impl: a
public method whose parameter type is a `domain` record, sitting next to a
private helper that takes the entity, is the signature bug in plain sight.

## Reading the generated code

The generated implementations are plain, readable Java — the fastest way to
understand what a mapper actually does:

```bash
./gradlew compileJava
find build/generated/sources/annotationProcessor -name "*Impl.java"
cat build/generated/sources/annotationProcessor/java/main/com/learning/hibernatelab/domain/BookMapperImpl.java
```

If a mapper is missing from that directory, the processor did not run — check
the `annotationProcessor` entries above. If it is present but its methods return
empty objects, the target's properties were invisible at processing time, which
usually means the Lombok binding is missing.

Once mappers delegate, two greps tell you whether the wiring took:

```bash
cd build/generated/sources/annotationProcessor/java/main/com/learning/hibernatelab/domain

grep -n "refMapper" *Impl.java    # conversions delegated to RefMapper
grep -n "protected " *Impl.java   # conversions generated locally instead
```

In the second list, a `protected bookToBookRef` is the smell from *Why declare
the refs at all?* — some conversion has no declared method. The
`protected bookSetToBookRefList` loops are a different thing and are expected:
MapStruct always inlines the collection loop into the mapper that needs it, and
each iteration calls `refMapper.toRef`. Sharing removes the duplicated element
conversion, not the loops.
