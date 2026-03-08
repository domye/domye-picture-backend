# Research Tools Guide

> **Purpose**: Choose the right MCP tool for research and documentation tasks.

---

## Tool Selection Matrix

| Task Type | Tool | Why |
|-----------|------|-----|
| Web search (news, facts, current events) | **Exa MCP** `web_search_exa` | Real-time web content |
| Website content extraction | **Exa MCP** `crawling_exa` | Full page content from URL |
| Library/API documentation | **Context7 MCP** `query-docs` | Official docs, code examples |
| Code generation patterns | **Context7 MCP** `query-docs` | Framework-specific patterns |
| Configuration help | **Context7 MCP** `query-docs` | Version-specific config |
| Context analysis | **Context7 MCP** `query-docs` | Library context understanding |

---

## Exa MCP - Web Research

### When to Use

- **Web search**: Current information, news, facts, recent developments
- **Content crawling**: Extract content from specific URLs
- **Company research**: Business information, news, insights
- **People search**: Professional profiles
- **Deep research**: Complex questions requiring synthesis

### Key Tools

| Tool | Purpose | Example Use Case |
|------|---------|------------------|
| `web_search_exa` | General web search | "Latest Spring Boot 3.4 features" |
| `web_search_advanced_exa` | Filtered search (date, domain) | Search specific sites, date ranges |
| `crawling_exa` | Extract page content | Get full article from URL |
| `company_research_exa` | Company information | Research a vendor or partner |
| `people_search_exa` | Professional profiles | Find experts in a field |
| `deep_researcher_start` + `deep_researcher_check` | Complex research | Multi-source synthesis |

### Best Practices

```
1. Start with `web_search_exa` for general queries
2. Use `web_search_advanced_exa` when you need filters:
   - Date range (startPublishedDate, endPublishedDate)
   - Domain restriction (includeDomains, excludeDomains)
   - Category filter (news, research paper, etc.)
3. Use `crawling_exa` when you have a specific URL
4. Use `deep_researcher_*` for complex questions (takes 15s-3min)
```

---

## Context7 MCP - Library Documentation

### When to Use

- **Library docs**: Official documentation for any library/framework
- **API reference**: Method signatures, parameters, examples
- **Code patterns**: Framework-specific implementation patterns
- **Configuration**: Version-specific configuration options
- **Migration guides**: Version upgrade information

### Two-Step Process

```
Step 1: Resolve library ID
mcp__context7__resolve-library-id(libraryName: "react", query: "hooks usage")

Step 2: Query documentation
mcp__context7__query-docs(
  libraryId: "/facebook/react",
  query: "How to use useEffect cleanup function"
)
```

### Key Tools

| Tool | Purpose | Example |
|------|---------|---------|
| `resolve-library-id` | Find library by name | "spring boot" → "/spring-projects/spring-boot" |
| `query-docs` | Get relevant documentation | Query specific topics, get code examples |

### Best Practices

```
1. ALWAYS resolve library ID first (required before query-docs)
2. Be specific in your query - include context and goal
3. For version-specific docs, check if multiple versions exist
4. The query parameter should describe your task, not just keywords
```

### Query Examples

```javascript
// Good: Specific task with context
query: "How to configure connection pooling in Spring Boot 3.3 with HikariCP"

// Bad: Vague keywords
query: "connection pool spring"
```

---

## Decision Flowchart

```
Need information?
│
├─ Is it about a specific library/framework?
│  │
│  ├─ YES → Context7 MCP
│  │         1. resolve-library-id
│  │         2. query-docs
│  │
│  └─ NO → Is it web content (news, articles, current info)?
│           │
│           ├─ YES → Exa MCP
│           │         - web_search_exa for search
│           │         - crawling_exa for specific URLs
│           │
│           └─ NO → Consider:
│                     - GitHub code search (gh search code)
│                     - Package registries (npm, PyPI, Maven)
│                     - File system search (Grep, Glob)
```

---

## Common Scenarios

### Scenario 1: "How do I use X library?"

```
1. Context7 resolve-library-id("X library", "what I need")
2. Context7 query-docs(libraryId, "specific question")
```

### Scenario 2: "What's the latest on X topic?"

```
1. Exa web_search_exa("X topic latest developments 2024")
2. If specific URL found, Exa crawling_exa(url) for full content
```

### Scenario 3: "Compare X vs Y approaches"

```
1. Context7 for library-specific docs on each approach
2. Exa web_search_exa for community discussions, comparisons
3. Synthesize findings
```

### Scenario 4: "Find code examples for X pattern"

```
1. Context7 query-docs with specific pattern description
2. Exa web_search_exa with category: "github" for code examples
```

---

## Anti-Patterns

### Don't

```
❌ Using Exa for library documentation (use Context7 instead)
❌ Using Context7 for current news/events (use Exa instead)
❌ Vague queries without context
❌ Skipping resolve-library-id step
❌ Not specifying version when relevant
```

### Do

```
✅ Match tool to information type
✅ Be specific in queries
✅ Follow two-step process for Context7
✅ Use advanced filters in Exa when needed
✅ Combine tools for comprehensive research
```

---

## Quick Reference Card

| I need... | Use this tool |
|-----------|---------------|
| Library docs | Context7 `query-docs` |
| API reference | Context7 `query-docs` |
| Code examples | Context7 `query-docs` or Exa `web_search_exa` |
| Current news | Exa `web_search_exa` |
| Article content | Exa `crawling_exa` |
| Company info | Exa `company_research_exa` |
| Deep research | Exa `deep_researcher_*` |
| Configuration help | Context7 `query-docs` |

---

**Remember**: Context7 for libraries, Exa for web. When in doubt, ask: "Is this about a specific library/framework or general web content?"