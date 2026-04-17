# Semantic Analysis Enhancement Guide for R1-R23 Resilience Checker

## Overview

The current R1-R23 Resilience Checker uses **regex pattern matching** for static analysis. This document outlines how to enhance it with **semantic analysis** using Abstract Syntax Tree (AST) parsing for deeper code understanding.

## Current Limitations vs. Semantic Analysis

| Capability | Current (Regex) | With Semantic Analysis |
|------------|----------------|------------------------|
| Detect annotations | ✅ Yes | ✅ Yes |
| Verify method exists | ❌ No | ✅ Yes |
| Check method signature | ❌ No | ✅ Yes |
| Validate configuration | ❌ Basic | ✅ Deep |
| Understand code flow | ❌ No | ✅ Yes |
| Detect commented code | ❌ No | ✅ Yes |
| Cross-reference checks | ❌ No | ✅ Yes |

## Enhancement Approach

### Phase 1: Add AST Parsing for Java (Recommended)

Use **Tree-sitter** or **javalang** for Java AST parsing.

#### Option A: Tree-sitter (Multi-language support)

**Advantages:**
- Supports multiple languages (Java, Python, JavaScript, Go, etc.)
- Fast and incremental parsing
- Battle-tested (used by GitHub, Atom)
- Language-agnostic approach

**Installation:**
```bash
pip install tree-sitter tree-sitter-java tree-sitter-python
```

**Example Implementation:**
```python
from tree_sitter import Language, Parser
import tree_sitter_java as tsjava

class SemanticCircuitBreakerCheck(ResilienceCheck):
    """Enhanced R1: Circuit Breaker with Semantic Analysis"""
    
    def __init__(self):
        super().__init__(
            "R1",
            "Circuit Breaker Pattern (Semantic)",
            "Fault Tolerance",
            "CRITICAL",
            "Verify circuit breaker is correctly implemented with valid fallback."
        )
        # Initialize parser
        JAVA_LANGUAGE = Language(tsjava.language())
        self.parser = Parser(JAVA_LANGUAGE)
    
    def check(self, file_path: str, content: str) -> bool:
        if not file_path.endswith('.java'):
            self.passed = True
            return True
        
        # Parse Java code into AST
        tree = self.parser.parse(bytes(content, "utf8"))
        root_node = tree.root_node
        
        # Find all methods with @CircuitBreaker annotation
        circuit_breaker_methods = self._find_circuit_breaker_methods(root_node)
        
        for method_node, annotation_node in circuit_breaker_methods:
            # Extract fallback method name from annotation
            fallback_name = self._extract_fallback_name(annotation_node)
            
            if fallback_name:
                # Verify fallback method exists
                if not self._fallback_method_exists(root_node, fallback_name):
                    self.add_finding(
                        file_path,
                        f"Fallback method '{fallback_name}' not found",
                        self._get_line_number(method_node)
                    )
                    self.passed = False
                    return False
                
                # Verify fallback method signature matches
                if not self._verify_fallback_signature(root_node, method_node, fallback_name):
                    self.add_finding(
                        file_path,
                        f"Fallback method '{fallback_name}' signature doesn't match",
                        self._get_line_number(method_node)
                    )
                    self.passed = False
                    return False
        
        self.passed = True
        return True
    
    def _find_circuit_breaker_methods(self, root_node):
        """Find all methods with @CircuitBreaker annotation"""
        methods = []
        
        def traverse(node):
            # Look for method declarations
            if node.type == 'method_declaration':
                # Check if it has @CircuitBreaker annotation
                for child in node.children:
                    if child.type == 'modifiers':
                        for modifier in child.children:
                            if modifier.type == 'marker_annotation':
                                annotation_name = modifier.child_by_field_name('name')
                                if annotation_name and 'CircuitBreaker' in annotation_name.text.decode():
                                    methods.append((node, modifier))
            
            for child in node.children:
                traverse(child)
        
        traverse(root_node)
        return methods
    
    def _extract_fallback_name(self, annotation_node):
        """Extract fallback method name from annotation"""
        # Look for fallbackMethod = "methodName"
        for child in annotation_node.children:
            if child.type == 'annotation_argument_list':
                text = child.text.decode()
                match = re.search(r'fallbackMethod\s*=\s*"([^"]+)"', text)
                if match:
                    return match.group(1)
        return None
    
    def _fallback_method_exists(self, root_node, fallback_name):
        """Check if fallback method exists in the class"""
        def traverse(node):
            if node.type == 'method_declaration':
                name_node = node.child_by_field_name('name')
                if name_node and name_node.text.decode() == fallback_name:
                    return True
            
            for child in node.children:
                if traverse(child):
                    return True
            return False
        
        return traverse(root_node)
    
    def _verify_fallback_signature(self, root_node, original_method, fallback_name):
        """Verify fallback method has compatible signature"""
        # Get original method return type
        original_return = self._get_return_type(original_method)
        
        # Find fallback method
        fallback_method = self._find_method_by_name(root_node, fallback_name)
        if not fallback_method:
            return False
        
        # Get fallback return type
        fallback_return = self._get_return_type(fallback_method)
        
        # Return types should match
        return original_return == fallback_return
    
    def _get_return_type(self, method_node):
        """Extract return type from method declaration"""
        type_node = method_node.child_by_field_name('type')
        if type_node:
            return type_node.text.decode()
        return None
    
    def _find_method_by_name(self, root_node, method_name):
        """Find method by name"""
        def traverse(node):
            if node.type == 'method_declaration':
                name_node = node.child_by_field_name('name')
                if name_node and name_node.text.decode() == method_name:
                    return node
            
            for child in node.children:
                result = traverse(child)
                if result:
                    return result
            return None
        
        return traverse(root_node)
    
    def _get_line_number(self, node):
        """Get line number from AST node"""
        return node.start_point[0] + 1
```

#### Option B: javalang (Java-specific)

**Advantages:**
- Pure Python, no external dependencies
- Java-specific, better Java support
- Easier to use for Java-only projects

**Installation:**
```bash
pip install javalang
```

**Example Implementation:**
```python
import javalang

class SemanticCircuitBreakerCheck(ResilienceCheck):
    """Enhanced R1: Circuit Breaker with Semantic Analysis"""
    
    def check(self, file_path: str, content: str) -> bool:
        if not file_path.endswith('.java'):
            self.passed = True
            return True
        
        try:
            # Parse Java code
            tree = javalang.parse.parse(content)
            
            # Find all methods with @CircuitBreaker
            for path, node in tree.filter(javalang.tree.MethodDeclaration):
                circuit_breaker_annotation = self._find_circuit_breaker_annotation(node)
                
                if circuit_breaker_annotation:
                    # Extract fallback method name
                    fallback_name = self._extract_fallback_name(circuit_breaker_annotation)
                    
                    if fallback_name:
                        # Verify fallback method exists
                        if not self._fallback_exists(tree, fallback_name):
                            self.add_finding(
                                file_path,
                                f"Fallback method '{fallback_name}' not found",
                                node.position.line if node.position else None
                            )
                            self.passed = False
                            return False
                        
                        # Verify signature compatibility
                        fallback_method = self._find_method(tree, fallback_name)
                        if not self._signatures_compatible(node, fallback_method):
                            self.add_finding(
                                file_path,
                                f"Fallback method '{fallback_name}' signature incompatible",
                                node.position.line if node.position else None
                            )
                            self.passed = False
                            return False
            
            self.passed = True
            return True
            
        except javalang.parser.JavaSyntaxError as e:
            # If parsing fails, fall back to regex
            return super().check(file_path, content)
    
    def _find_circuit_breaker_annotation(self, method_node):
        """Find @CircuitBreaker annotation on method"""
        if not method_node.annotations:
            return None
        
        for annotation in method_node.annotations:
            if annotation.name == 'CircuitBreaker':
                return annotation
        return None
    
    def _extract_fallback_name(self, annotation):
        """Extract fallback method name from annotation"""
        if not annotation.element:
            return None
        
        # Handle @CircuitBreaker(fallbackMethod = "methodName")
        if isinstance(annotation.element, list):
            for element in annotation.element:
                if hasattr(element, 'name') and element.name == 'fallbackMethod':
                    return element.value.value.strip('"')
        
        return None
    
    def _fallback_exists(self, tree, fallback_name):
        """Check if fallback method exists"""
        for path, node in tree.filter(javalang.tree.MethodDeclaration):
            if node.name == fallback_name:
                return True
        return False
    
    def _find_method(self, tree, method_name):
        """Find method by name"""
        for path, node in tree.filter(javalang.tree.MethodDeclaration):
            if node.name == method_name:
                return node
        return None
    
    def _signatures_compatible(self, original_method, fallback_method):
        """Check if fallback method signature is compatible"""
        if not fallback_method:
            return False
        
        # Check return types match
        if original_method.return_type != fallback_method.return_type:
            return False
        
        # Fallback should have same parameters + Exception parameter
        original_params = len(original_method.parameters) if original_method.parameters else 0
        fallback_params = len(fallback_method.parameters) if fallback_method.parameters else 0
        
        # Fallback should have one extra parameter (Exception)
        return fallback_params == original_params + 1
```

### Phase 2: Enhanced Configuration Validation

Use **PyYAML** with schema validation for YAML files.

**Installation:**
```bash
pip install pyyaml jsonschema
```

**Example: Kubernetes Resource Validation**
```python
import yaml
from jsonschema import validate, ValidationError

class SemanticKubernetesResourceCheck(ResilienceCheck):
    """Enhanced R16: Kubernetes Resources with Semantic Validation"""
    
    RESOURCE_SCHEMA = {
        "type": "object",
        "properties": {
            "resources": {
                "type": "object",
                "properties": {
                    "limits": {
                        "type": "object",
                        "properties": {
                            "cpu": {"type": "string"},
                            "memory": {"type": "string"}
                        },
                        "required": ["cpu", "memory"]
                    },
                    "requests": {
                        "type": "object",
                        "properties": {
                            "cpu": {"type": "string"},
                            "memory": {"type": "string"}
                        },
                        "required": ["cpu", "memory"]
                    }
                },
                "required": ["limits", "requests"]
            }
        },
        "required": ["resources"]
    }
    
    def check(self, file_path: str, content: str) -> bool:
        if not file_path.endswith(('.yaml', '.yml')):
            self.passed = True
            return True
        
        try:
            # Parse YAML
            docs = yaml.safe_load_all(content)
            
            for doc in docs:
                if not doc or doc.get('kind') != 'Deployment':
                    continue
                
                # Extract container specs
                containers = doc.get('spec', {}).get('template', {}).get('spec', {}).get('containers', [])
                
                for container in containers:
                    # Validate resource schema
                    try:
                        validate(instance=container, schema=self.RESOURCE_SCHEMA)
                    except ValidationError as e:
                        self.add_finding(file_path, f"Invalid resource configuration: {e.message}")
                        self.passed = False
                        return False
                    
                    # Semantic validation: requests <= limits
                    resources = container.get('resources', {})
                    if not self._validate_resource_bounds(resources):
                        self.add_finding(
                            file_path,
                            "Resource requests must be less than or equal to limits"
                        )
                        self.passed = False
                        return False
            
            self.passed = True
            return True
            
        except yaml.YAMLError:
            return super().check(file_path, content)
    
    def _validate_resource_bounds(self, resources):
        """Validate that requests <= limits"""
        limits = resources.get('limits', {})
        requests = resources.get('requests', {})
        
        # Parse CPU values
        cpu_limit = self._parse_cpu(limits.get('cpu', '0'))
        cpu_request = self._parse_cpu(requests.get('cpu', '0'))
        
        if cpu_request > cpu_limit:
            return False
        
        # Parse memory values
        mem_limit = self._parse_memory(limits.get('memory', '0'))
        mem_request = self._parse_memory(requests.get('memory', '0'))
        
        if mem_request > mem_limit:
            return False
        
        return True
    
    def _parse_cpu(self, cpu_str):
        """Parse CPU string to millicores"""
        if cpu_str.endswith('m'):
            return int(cpu_str[:-1])
        return int(float(cpu_str) * 1000)
    
    def _parse_memory(self, mem_str):
        """Parse memory string to bytes"""
        units = {
            'Ki': 1024,
            'Mi': 1024**2,
            'Gi': 1024**3,
            'K': 1000,
            'M': 1000**2,
            'G': 1000**3
        }
        
        for unit, multiplier in units.items():
            if mem_str.endswith(unit):
                return int(mem_str[:-len(unit)]) * multiplier
        
        return int(mem_str)
```

### Phase 3: Cross-File Analysis

Track state across files to detect missing implementations.

**Example: Circuit Breaker Configuration Check**
```python
class CrossFileCircuitBreakerCheck(ResilienceCheck):
    """Cross-file validation for circuit breaker configuration"""
    
    def __init__(self):
        super().__init__(
            "R1-CF",
            "Circuit Breaker Configuration Consistency",
            "Fault Tolerance",
            "HIGH",
            "Verify circuit breakers in code have corresponding configuration."
        )
        self.circuit_breakers_in_code = set()
        self.circuit_breakers_in_config = set()
    
    def check(self, file_path: str, content: str) -> bool:
        # Phase 1: Collect circuit breaker names from Java files
        if file_path.endswith('.java'):
            self._collect_circuit_breakers_from_code(content)
        
        # Phase 2: Collect circuit breaker names from config files
        if file_path.endswith(('.yml', '.yaml')):
            self._collect_circuit_breakers_from_config(content)
        
        return True  # Don't fail yet, wait for finalize
    
    def finalize(self):
        """Called after all files are processed"""
        # Find circuit breakers in code but not in config
        missing_config = self.circuit_breakers_in_code - self.circuit_breakers_in_config
        
        if missing_config:
            for cb_name in missing_config:
                self.add_finding(
                    "configuration",
                    f"Circuit breaker '{cb_name}' used in code but not configured"
                )
            self.passed = False
            return False
        
        self.passed = True
        return True
    
    def _collect_circuit_breakers_from_code(self, content):
        """Extract circuit breaker names from @CircuitBreaker annotations"""
        pattern = r'@CircuitBreaker\s*\(\s*name\s*=\s*"([^"]+)"'
        matches = re.findall(pattern, content)
        self.circuit_breakers_in_code.update(matches)
    
    def _collect_circuit_breakers_from_config(self, content):
        """Extract circuit breaker names from configuration"""
        try:
            config = yaml.safe_load(content)
            cb_config = config.get('resilience4j', {}).get('circuitbreaker', {}).get('instances', {})
            self.circuit_breakers_in_config.update(cb_config.keys())
        except:
            pass
```

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [ ] Add tree-sitter or javalang dependency
- [ ] Create semantic check base class
- [ ] Implement semantic Circuit Breaker check (R1)
- [ ] Add unit tests for semantic checks

### Phase 2: Core Patterns (Week 3-4)
- [ ] Implement semantic Retry check (R2)
- [ ] Implement semantic Timeout check (R3)
- [ ] Add configuration validation for Resilience4j
- [ ] Add cross-file validation

### Phase 3: Infrastructure (Week 5-6)
- [ ] Enhance Kubernetes resource validation (R16-R18)
- [ ] Add Terraform semantic validation (R21)
- [ ] Implement OpenAPI schema validation (R22)
- [ ] Add Helm chart semantic checks (R23)

### Phase 4: Advanced Features (Week 7-8)
- [ ] Add control flow analysis
- [ ] Implement data flow tracking
- [ ] Add complexity metrics
- [ ] Create detailed reports with fix suggestions

## Benefits of Semantic Analysis

### 1. **Deeper Validation**
```java
// Current checker: ✅ PASS (sees @CircuitBreaker)
// Semantic checker: ❌ FAIL (fallback method missing)
@CircuitBreaker(name = "service", fallbackMethod = "handleFailure")
public String call() {
    return restTemplate.getForObject(url, String.class);
}
// Missing: public String handleFailure(Exception e) { ... }
```

### 2. **Configuration Consistency**
```yaml
# Current checker: ✅ PASS (sees circuit breaker config)
# Semantic checker: ❌ FAIL (requests > limits)
resources:
  limits:
    cpu: 500m      # Limit is 500m
    memory: 512Mi
  requests:
    cpu: 1000m     # Request is 1000m (INVALID!)
    memory: 256Mi
```

### 3. **Cross-File Validation**
```java
// Code: Uses circuit breaker "externalService"
@CircuitBreaker(name = "externalService")
public String call() { ... }
```

```yaml
# Config: Missing configuration for "externalService"
resilience4j:
  circuitbreaker:
    instances:
      otherService:  # Wrong name!
        failureRateThreshold: 50
```

**Semantic checker:** ❌ FAIL - Circuit breaker "externalService" not configured

## Performance Considerations

| Aspect | Regex | Semantic Analysis |
|--------|-------|-------------------|
| Speed | Fast (ms) | Slower (seconds) |
| Memory | Low | Higher |
| Accuracy | 70-80% | 95-99% |
| False Positives | High | Low |
| False Negatives | Medium | Low |

**Recommendation:** Use both approaches:
- **Regex checks:** Fast pre-screening (current implementation)
- **Semantic checks:** Deep validation for critical patterns (new enhancement)

## Migration Strategy

### Option 1: Gradual Migration (Recommended)
1. Keep existing regex checks
2. Add semantic checks alongside
3. Run both in parallel
4. Compare results
5. Gradually replace regex with semantic

### Option 2: Hybrid Approach
1. Use regex for simple patterns (health checks, logging)
2. Use semantic for complex patterns (circuit breaker, retry)
3. Best of both worlds: speed + accuracy

### Option 3: Full Replacement
1. Replace all regex checks with semantic
2. Higher accuracy but slower
3. Requires more maintenance

## Recommended Tools

| Language | Tool | Pros | Cons |
|----------|------|------|------|
| Java | tree-sitter-java | Fast, multi-language | Learning curve |
| Java | javalang | Pure Python, easy | Java-only |
| Python | ast (built-in) | No dependencies | Python-only |
| YAML | PyYAML + jsonschema | Standard, reliable | Basic validation |
| Terraform | python-hcl2 | Native HCL support | Limited validation |

## Next Steps

1. **Evaluate tools:** Test tree-sitter vs javalang with sample code
2. **Prototype:** Implement semantic Circuit Breaker check
3. **Benchmark:** Compare performance vs accuracy
4. **Decide:** Choose migration strategy
5. **Implement:** Roll out semantic checks incrementally

## Conclusion

Semantic analysis will significantly improve the R1-R23 Resilience Checker by:
- ✅ Reducing false positives
- ✅ Catching more real issues
- ✅ Providing better error messages
- ✅ Enabling cross-file validation
- ✅ Validating configuration correctness

The trade-off is increased complexity and slower execution, but the accuracy gains are worth it for critical resilience patterns.

---

**Related Documentation:**
- [Capabilities & Limitations](RESILIENCE-CHECKER-CAPABILITIES.md)
- [R1-R23 Checklist](R1-R20-RESILIENCE-CHECKLIST.md)
- [Technical Guide](RESILIENCE-CHECKER-TECHNICAL-GUIDE.md)