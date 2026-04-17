# R5 Rate Limiting - Assessment Logic Explained

## How the Fix Determines if Code Needs Rate Limiting

### Decision Flow

```
For each file:
  ┌─────────────────────────────────────┐
  │ 1. Does file have rate limiting?    │
  │    (@RateLimiter, RateLimiterConfig,│
  │     rate-limit:, rateLimit, throttle)│
  └──────────────┬──────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
       YES               NO
        │                 │
        ▼                 ▼
   ┌─────────┐   ┌──────────────────────────┐
   │  PASS   │   │ 2. Does file have API    │
   │ (Skip)  │   │    endpoints/controllers? │
   └─────────┘   └──────────┬───────────────┘
                            │
                   ┌────────┴────────┐
                   │                 │
                  YES               NO
                   │                 │
                   ▼                 ▼
          ┌─────────────────┐  ┌─────────┐
          │ FAIL            │  │  PASS   │
          │ Add Finding:    │  │ (Skip)  │
          │ "API endpoints  │  └─────────┘
          │  without rate   │
          │  limiting"      │
          └─────────────────┘
```

## Phase 1: Check for Existing Rate Limiting

The checker first looks for rate limiting patterns in the file:

```python
patterns = [
    r'@RateLimiter',              # Resilience4j annotation
    r'RateLimiterConfig',         # Configuration class
    r'rate-limit:',               # YAML config
    r'rateLimit',                 # Property/config
    r'throttle',                  # Throttling implementation
]
```

**If found:** ✅ File has rate limiting → PASS (no violation)

## Phase 2: Check if Rate Limiting is Needed

**Only if Phase 1 finds NO rate limiting**, the checker determines if the file NEEDS it by looking for API endpoints:

```python
endpoint_patterns = [
    # Spring Boot REST Controllers
    r'@RestController',           # REST controller class
    r'@Controller',               # MVC controller class
    r'@RequestMapping',           # Request mapping
    r'@GetMapping',               # GET endpoint
    r'@PostMapping',              # POST endpoint
    r'@PutMapping',               # PUT endpoint
    r'@DeleteMapping',            # DELETE endpoint
    r'@PatchMapping',             # PATCH endpoint
    
    # Express.js (Node.js)
    r'app\.get\(',                # Express GET route
    r'app\.post\(',               # Express POST route
    r'router\.',                  # Express router
    
    # JAX-RS (Java)
    r'@Path\(',                   # JAX-RS path annotation
    
    # Generic
    r'@Route\(',                  # Generic route annotation
]
```

**If endpoints found:** ❌ File needs rate limiting but doesn't have it → FAIL (add finding)
**If no endpoints:** ✅ File doesn't need rate limiting → PASS (not applicable)

## Why This Logic?

### Rationale: API Endpoints Need Rate Limiting

**Rate limiting is needed for:**
- ✅ REST API controllers (expose HTTP endpoints to external clients)
- ✅ Web service endpoints (can be called repeatedly)
- ✅ Public-facing APIs (vulnerable to abuse/overload)

**Rate limiting is NOT needed for:**
- ❌ Service classes (internal business logic)
- ❌ Repository classes (database access)
- ❌ Configuration classes (setup code)
- ❌ Domain models (data structures)
- ❌ Utility classes (helper functions)

### Example: VesselOperationsController.java

```java
@RestController                              // ← API Controller detected!
@RequestMapping("/api/v1/vessel-operations")
public class VesselOperationsController {
    
    @PostMapping("/containers/load")        // ← Endpoint detected!
    public ResponseEntity<LoadContainerResponse> loadContainer(...) {
        // No @RateLimiter annotation
        // No rate limiting configuration
    }
    
    @PostMapping("/vessels/{vesselId}/depart")  // ← Endpoint detected!
    public ResponseEntity<VesselDepartureResponse> departVessel(...) {
        // No rate limiting
    }
}
```

**Assessment:**
1. ❌ No rate limiting patterns found (Phase 1)
2. ✅ Has `@RestController` → API controller detected
3. ✅ Has `@PostMapping` → Endpoints detected
4. **Result:** FAIL - "API endpoints detected without rate limiting protection"

### Example: Service Class (No Violation)

```java
@Service
public class LoadContainerOnVesselUseCase {
    
    public LoadContainerResult execute(LoadContainerCommand command) {
        // Business logic
        // No HTTP endpoints
    }
}
```

**Assessment:**
1. ❌ No rate limiting patterns found (Phase 1)
2. ❌ No endpoint patterns found (Phase 2)
3. **Result:** PASS - Not an API controller, rate limiting not needed

## What Files Get Flagged?

### ✅ Files That WILL Be Flagged (Need Rate Limiting)

1. **Spring Boot REST Controllers**
   ```java
   @RestController
   public class MyController {
       @GetMapping("/api/data")
       public Data getData() { ... }
   }
   ```

2. **Express.js Routes**
   ```javascript
   app.get('/api/users', (req, res) => {
       // Handler
   });
   ```

3. **JAX-RS Resources**
   ```java
   @Path("/api/orders")
   public class OrderResource {
       @GET
       public List<Order> getOrders() { ... }
   }
   ```

### ❌ Files That Will NOT Be Flagged (Don't Need Rate Limiting)

1. **Service/Business Logic Classes**
   ```java
   @Service
   public class OrderService {
       public Order processOrder(OrderRequest req) { ... }
   }
   ```

2. **Repository Classes**
   ```java
   @Repository
   public interface OrderRepository extends JpaRepository<Order, Long> {
   }
   ```

3. **Configuration Classes**
   ```java
   @Configuration
   public class AppConfig {
       @Bean
       public RestTemplate restTemplate() { ... }
   }
   ```

4. **Domain Models**
   ```java
   @Entity
   public class Order {
       private Long id;
       private String status;
   }
   ```

## Limitations and Considerations

### Current Approach: File-Level Detection

**Pros:**
- ✅ Simple and fast
- ✅ Catches most common cases
- ✅ Works across multiple frameworks
- ✅ No false positives for non-API code

**Cons:**
- ⚠️ Doesn't detect if rate limiting is configured at:
  - API Gateway level (Kong, AWS API Gateway, etc.)
  - Load balancer level (NGINX, HAProxy)
  - Application-wide middleware
  - Framework-level interceptors

### Potential Improvements

1. **Check Configuration Files**
   ```yaml
   # application.yml
   resilience4j:
     ratelimiter:
       instances:
         default:
           limitForPeriod: 100
   ```

2. **Check API Gateway Configs**
   ```yaml
   # kong.yml
   plugins:
     - name: rate-limiting
       config:
         minute: 100
   ```

3. **Check Middleware/Interceptors**
   ```java
   @Configuration
   public class WebConfig implements WebMvcConfigurer {
       @Override
       public void addInterceptors(InterceptorRegistry registry) {
           registry.addInterceptor(new RateLimitInterceptor());
       }
   }
   ```

## Summary

**The fix assesses if code needs rate limiting by:**

1. **Checking if rate limiting exists** (annotations, config, middleware)
2. **If not, checking if the file exposes API endpoints** (controllers, routes)
3. **Only flagging files that have endpoints but lack rate limiting**

**This ensures:**
- ✅ Only API-facing code is checked
- ✅ Internal business logic is not flagged
- ✅ Violations are specific and actionable
- ✅ Developers know exactly which files need fixing

**The assessment is based on the principle:**
> "If your code exposes HTTP endpoints to external clients, it needs rate limiting to prevent abuse and overload."