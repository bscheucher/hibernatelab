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
    Book["Book<br/><i>persistence</i>"] -->|mapper| BookView["BookView<br/><i>domain</i>"]
    BookView -->|mapper| BookDto["BookDto<br/><i>web</i>"]
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

The mapper is an interface in the same package as its target type:

```java
package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Author;
import com.learning.hibernatelab.persistence.Book;
import com.learning.hibernatelab.persistence.Editor;
import com.learning.hibernatelab.persistence.Publisher;
import com.learning.hibernatelab.persistence.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BookMapper {

    @Mapping(target = "tagNames", source = "tags")
    BookView toView(Book book);

    List<BookView> toViews(List<Book> books);

    // Found by MapStruct whenever it needs one of these conversions.
    AuthorRef toRef(Author author);

    EditorRef toRef(Editor editor);

    PublisherRef toRef(Publisher publisher);

    default String tagName(Tag tag) {
        return tag.getName();
    }
}
```

Points worth noting:

- `id`, `title` and `description` need no annotation — names match, so they are
  mapped automatically.
- `publisher`, `authors` and `editors` need none either. The names match, and
  MapStruct finds the `toRef` methods on this same interface to convert each
  element. Overloading `toRef` is fine — it resolves by parameter type.
- Only `tagNames` is annotated, because the name differs from the source
  property `tags`.
- `toViews` needs no body. Given `toView`, MapStruct writes the loop.
- The `default` method is ordinary Java. MapStruct spots that it needs a
  `Tag` → `String` conversion and calls `tagName` for each element.
- The generated code null-checks `publisher` before calling `toRef`, so a book
  with no publisher yields a null `publisher()` rather than an exception.

Inject it like any bean:

```java
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @Transactional(readOnly = true)
    public Optional<BookView> findById(Long id) {
        return bookRepository.findById(id).map(bookMapper::toView);
    }
}
```

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

Two rules follow:

1. **Call the mapper inside `@Transactional`**, i.e. in the service, never in the
   controller. If the session is closed the lazy collections cannot be read.
2. **Fetch what the mapper will touch**, or you get N+1 queries — one for the
   book, then one per collection, per book. Use an entity graph:

   ```java
   @EntityGraph(attributePaths = {"publisher", "authors", "editors", "tags"})
   Optional<Book> findWithDetailsById(Long id);
   ```

   Note that fetching two `@ManyToMany` collections in one query produces a
   cartesian product. For lists, prefer separate queries or Hibernate's
   `@BatchSize` over one giant join.

MapStruct cannot warn you about either of these — the generated code just calls
getters. The cost is in what those getters do.

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

**Composing mappers** — `uses` lets one mapper delegate to another:

```java
@Mapper(uses = AuthorMapper.class)
public interface BookMapper { ... }
```

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

## Testing a mapper

A mapper needs no database. Instantiate the generated class directly:

```java
class BookMapperTest {

    private final BookMapper mapper = new BookMapperImpl();

    @Test
    void mapsPublisherAndAuthorsToRefs() {
        Publisher publisher = new Publisher();
        publisher.setId(7L);
        publisher.setName("Manning");

        Author author = new Author();
        author.setId(3L);
        author.setName("Christian Bauer");

        Book book = new Book();
        book.setTitle("Hibernate in Action");
        book.setPublisher(publisher);
        book.setAuthors(Set.of(author));

        BookView view = mapper.toView(book);

        assertThat(view.title()).isEqualTo("Hibernate in Action");
        assertThat(view.publisher()).isEqualTo(new PublisherRef(7L, "Manning"));
        assertThat(view.authors()).containsExactly(new AuthorRef(3L, "Christian Bauer"));
    }

    @Test
    void toleratesMissingPublisher() {
        Book book = new Book();
        book.setTitle("Untitled");

        BookView view = mapper.toView(book);

        assertThat(view.publisher()).isNull();
        assertThat(view.authors()).isEmpty();   // never null, thanks to the compact constructor
    }
}
```

Two things are worth a test of their own. **The null-relation case** — a book
with no publisher — because that is where the generated null checks either save
you or surprise you. And **empty collections**, to pin down that the compact
constructor turns MapStruct's `null` into an empty list.

Comparing whole refs with `isEqualTo` works because records get `equals` for
free, which is a quiet argument for using them here.

Ordering, though, is not something to assert on. The source collections are
`HashSet`s, so `authors()` comes back in unspecified order. Either sort in the
mapper or assert with `containsExactlyInAnyOrder`.
