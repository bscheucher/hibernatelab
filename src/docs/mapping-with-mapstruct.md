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
        String publisherName,
        List<String> authorNames,
        List<String> tagNames) {}
```

The mapper is an interface in the same package as its target type:

```java
package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Author;
import com.learning.hibernatelab.persistence.Book;
import com.learning.hibernatelab.persistence.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BookMapper {

    @Mapping(target = "publisherName", source = "publisher.name")
    @Mapping(target = "authorNames",   source = "authors")
    @Mapping(target = "tagNames",      source = "tags")
    BookView toView(Book book);

    List<BookView> toViews(List<Book> books);

    // Used implicitly to turn Set<Author> into List<String>.
    default String authorName(Author author) {
        return author.getName();
    }

    default String tagName(Tag tag) {
        return tag.getName();
    }
}
```

Points worth noting:

- `id`, `title` and `description` need no annotation — names match, so they are
  mapped automatically.
- `publisher.name` is **nested source navigation**. MapStruct null-checks the
  intermediate `publisher`, which matters here because `publisher_id` is
  nullable.
- `toViews` needs no body. Given `toView`, MapStruct writes the loop.
- The `default` methods are ordinary Java. MapStruct spots that it needs an
  `Author` → `String` conversion and calls `authorName` for each element.

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
   @EntityGraph(attributePaths = {"publisher", "authors", "tags"})
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
    void mapsPublisherAndAuthorNames() {
        Publisher publisher = new Publisher();
        publisher.setName("Manning");

        Author author = new Author();
        author.setName("Christian Bauer");

        Book book = new Book();
        book.setTitle("Hibernate in Action");
        book.setPublisher(publisher);
        book.setAuthors(Set.of(author));

        BookView view = mapper.toView(book);

        assertThat(view.title()).isEqualTo("Hibernate in Action");
        assertThat(view.publisherName()).isEqualTo("Manning");
        assertThat(view.authorNames()).containsExactly("Christian Bauer");
    }

    @Test
    void toleratesMissingPublisher() {
        Book book = new Book();
        book.setTitle("Untitled");

        assertThat(mapper.toView(book).publisherName()).isNull();
    }
}
```

Keep at least one test for the null-relation case — that is where nested source
navigation either saves you or surprises you.
