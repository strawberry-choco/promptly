---
mode: overlay
---

> This document overlays project-specific customizations on top of the architecture atom's embedded clean-architecture defaults. Only sections included here differ from the defaults — all other sections remain as-is.

## 1. Layer Responsibilities

| Layer | Responsibility | Depends On | Depended On By |
|---|---|---|---|
| **UI** | Activities, Fragments, ViewModels, state holders (`StateFlow`), Compose layouts | Domain | Nothing (entry point) |
| **Domain** | Use cases, business logic models, repository interfaces, domain services. Pure Kotlin — no Android imports, no framework types | Nothing (innermost) | UI, Data (via interfaces) |
| **Data** | Repository implementations, Room entities (`@Entity`), DAOs, Retrofit APIs, data sources. Maps between data models and domain models | Domain (for interfaces) | Domain UseCases (injected) |

### Typical Directory Mapping

```
app/src/main/java/com/example/promptly/
├── ui/                    # Activities, Fragments, ViewModels, Composables
│   └── ...
├── domain/
│   ├── model/             # Business-logic models (pure Kotlin)
│   ├── usecase/           # One use case per action
│   ├── service/           # Shared domain business logic across use cases
│   └── repository/        # Repository interfaces
└── data/
    ├── repository/        # Repository implementations
    ├── local/             # Room DB, DAOs, entities
    └── remote/            # Retrofit APIs, DTOs
```

Domain Services are pure Kotlin classes in the domain layer encapsulating shared business logic (e.g., `PricingCalculator`, `EligibilityEvaluator`) used by multiple use cases. No I/O, no Android dependencies.

## 3. Per-Layer Rules

### 3.1 UI Layer

**What belongs here:**
- ViewModels with `StateFlow` for state management
- Activities and Fragments for lifecycle handling
- Compose UI or XML layout logic
- Navigation coordination
- UI state models (mapped from domain models)

**What does not belong here:**
- Business rule evaluation
- Direct Room DAO or Retrofit calls
- Domain model construction with business logic

**Common violations:**
- ViewModel calling data sources directly instead of through UseCase
- Business logic conditionals in Compose or XML layout code
- Activity/Fragment containing data-access logic

### 3.2 Domain: UseCases

**What belongs here:**
- One use case class per user action (e.g., `CreateOrderUseCase`)
- Orchestration: call repository interfaces, apply domain services, return results
- Injection of repository interfaces only
- Pure Kotlin — no Android framework imports

**What does not belong here:**
- I/O operations or data-source access
- Android framework types (`Context`, `Parcelable`, `Bundle`)
- Room or Retrofit references

**Common violations:**
- Single use class doing multiple unrelated actions
- UseCase accessing Android APIs
- Anemic use cases that just pass through to repository

### 3.3 Domain: Model

**What belongs here:**
- Business-logic models with behavior (not just data holders)
- Repository interfaces (contracts that Data layer implements)
- Domain services for shared business logic
- Pure Kotlin classes

**What does not belong here:**
- Room `@Entity` annotations or `@Column`, `@PrimaryKey`
- Android framework types (`Context`, `Intent`, `Parcelable`)
- Retrofit annotations or serialization logic

**Common violations:**
- Domain models annotated with Room or Retrofit annotations
- Domain services with I/O or Android dependencies
- Repository interfaces defined in the data layer

### 3.4 Data Layer

**What belongs here:**
- Repository implementations that implement domain repository interfaces
- Room entities (`@Entity`), DAOs
- Retrofit API interfaces and DTOs
- Data source classes for local/remote access
- Mapping between data models and domain models

**What does not belong here:**
- Business logic
- ViewModel or UI imports
- Domain model definitions

**Common violations:**
- Business logic in Repository implementation
- Data models (Room entities, API DTOs) exposed to UI layer
- Android framework types leaking into domain through repository interface

## 4. Command and Query Flows

Both state-changing (commands) and read-only (queries) operations use the same full-stack flow. No shortcut for reads — all operations go through UseCase → Domain → Repository → Data Source.

```
UI (ViewModel)
  → Domain UseCase
    → Domain (model, domain service)
      → Repository Interface
        → Repository Implementation
          → Data Source (Room DAO / Retrofit)
```

### 4.1 Command Flow Example

```kotlin
// domain/repository/OrderRepository
interface OrderRepository {
    suspend fun save(order: Order)
}

// domain/usecase/CreateOrderUseCase
class CreateOrderUseCase(
    private val orderRepo: OrderRepository,
    private val pricing: PricingService
) {
    suspend operator fun invoke(items: List<Item>): Order {
        val total = pricing.calculate(items)
        val order = Order.create(items, total)
        orderRepo.save(order)
        return order
    }
}

// data/repository/OrderRepositoryImpl
class OrderRepositoryImpl(
    private val dao: OrderDao,
    private val mapper: OrderMapper
) : OrderRepository {
    override suspend fun save(order: Order) {
        dao.insert(mapper.toEntity(order))
    }
}

// ui/viewmodel/OrderViewModel
class OrderViewModel(
    private val createOrder: CreateOrderUseCase
) : ViewModel() {
    fun createOrder(items: List<Item>) {
        viewModelScope.launch {
            createOrder(items)
        }
    }
}
```

### 4.2 Query Flow Example

```kotlin
// domain/repository/UserRepository
interface UserRepository {
    suspend fun getById(id: String): User?
    suspend fun listActive(): List<User>
}

// domain/usecase/GetActiveUsersUseCase
class GetActiveUsersUseCase(
    private val userRepo: UserRepository
) {
    suspend operator fun invoke(): List<User> {
        return userRepo.listActive()
    }
}

// data/repository/UserRepositoryImpl
class UserRepositoryImpl(
    private val dao: UserDao,
    private val mapper: UserMapper
) : UserRepository {
    override suspend fun listActive(): List<User> {
        return dao.getActive().map { mapper.toDomain(it) }
    }
}
```

## 5. Example Violations and Fixes

### Business Logic in ViewModel

```kotlin
// BAD: ViewModel contains business rule
class OrderViewModel(
    private val dao: OrderDao
) : ViewModel() {
    fun createOrder(items: List<Item>) {
        val total = items.sumOf { it.price * it.quantity }
        if (total > 10000) throw IllegalArgumentException("Limit exceeded")
        viewModelScope.launch { dao.insert(OrderEntity(...)) }
    }
}

// GOOD: ViewModel delegates to UseCase
class OrderViewModel(
    private val createOrder: CreateOrderUseCase
) : ViewModel() {
    fun createOrder(items: List<Item>) {
        viewModelScope.launch { createOrder(items) }
    }
}
```

### Framework Types in Domain

```kotlin
// BAD: Domain depends on Android framework
class User(
    val context: Context,  // Android framework in domain
    val id: String
)

// GOOD: Domain is pure Kotlin
class User(
    val id: String,
    val name: String
)
```

### Data Models Exposed to UI

```kotlin
// BAD: Room entity used as UI state
fun getUser(userId: String): Flow<UserEntity?>  // UI sees @Entity-annotated type

// GOOD: Domain model mapped to UI state
fun getUser(userId: String): Flow<User?>
```

### God Activity

```kotlin
// BAD: Activity handles navigation, business logic, API, DB
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // navigation
        // business logic
        // API call
        // DB access
    }
}

// GOOD: Decomposed by layer and responsibility
// ui/MainActivity.kt -- lifecycle and navigation only
// domain/usecase/ -- business operations
// data/repository/ -- data access
```

## 6. Validation Checklist

### Layer Placement
- [ ] Business logic is in domain layer (not ViewModel or Activity)
- [ ] Use case orchestration is in domain use cases
- [ ] Android framework types (Context, Parcelable, @Entity) stay in data/UI layers
- [ ] Room/Retrofit details are in data layer only

### Dependency Direction
- [ ] Domain has zero imports from Android framework
- [ ] ViewModels depend on UseCases and domain interfaces, not concrete data sources
- [ ] Data layer implements domain-defined repository interfaces
- [ ] No circular dependencies between layers

### Boundary Integrity
- [ ] Data crossing inward maps to domain models (not Room entities or API DTOs)
- [ ] Data crossing outward maps to UI state models (not domain models directly)
- [ ] Framework annotations (@Entity, @Column, @SerializedName) stay in data layer
- [ ] Both command and query flows cross through all layers (no read shortcut)

### Single Responsibility
- [ ] ViewModel has no direct data-source access (DAO, Retrofit, SharedPreferences)
- [ ] UseCase has no Android imports and orchestrates a single action
- [ ] Repository implementations do not contain business logic

### Testability
- [ ] Domain logic is testable without Robolectric or Android instrumentation
- [ ] ViewModels are testable by mocking UseCases
- [ ] Repository implementations are testable with fake DAOs

---

*Generated for Promptly on 2026-06-14. Mode: overlay.*
*Produced by the architecture-refiner skill.*
