package com.quantumai.customer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantumai.customer.dto.ChatBotRequestDTO;
import com.quantumai.customer.dto.ChatBotResponseDTO;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatBotServiceImpl implements ChatBotService {

    @Autowired private OpenAIService openAIService;
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private ChatBotEmbeddingRepository embeddingRepository;

    // Repositories
    @Autowired private AssetsRepository assetsRepository;
    @Autowired private CompanyCustomerRepository companyCustomerRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private AssetCategoryRepository assetCategoryRepository;
    @Autowired private CompanyCustomerCategoryRepository companyCustomerCategoryRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private BinRepository binRepository;
    @Autowired private AssetCategoryInspectionRepository assetCategoryInspectionRepository;
    @Autowired private AssetCheckInOutRepository assetCheckInOutRepository;
    @Autowired private AssetExtraFieldsRepository assetExtraFieldsRepository;
    @Autowired private CompanyCustomerExtraFieldsRepository companyCustomerExtraFieldsRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ──────────────────────────────────────────────────────────────
    // System prompt that tells the AI how to interpret user queries
    // ──────────────────────────────────────────────────────────────
    private static final String SYSTEM_PROMPT = """
            You are a data query assistant for an asset management application.
            The application manages: Assets, Customers, Users, Asset Categories, Customer Categories, Locations, Bins, Inspections, and Check-In/Check-Out records.
            
            Given a user's natural language query, you must return a JSON object (and NOTHING else) with these fields:
            {
              "entity": "<one of: ASSET, CUSTOMER, USER, ASSET_CATEGORY, CUSTOMER_CATEGORY, LOCATION, BIN, INSPECTION, CHECK_IN_OUT>",
              "operation": "<one of: COUNT, LIST, FIND, SUMMARY>",
              "filters": {
                "<fieldName>": "<value>"
              },
              "limit": <number or null>,
              "explanation": "<brief one-line explanation of what the user wants>"
            }
            
            Rules:
            - entity must be exactly one of the listed values.
            - operation: COUNT = how many/count, LIST = show all/list, FIND = find/search/get specific records, SUMMARY = overview/stats/dashboard.
            - filters: extract any filter conditions. Common fields:
              * ASSET: name, serialNumber, category, customer, status, location
              * CUSTOMER: name, email, phone, category, status, city, state, country
              * USER: firstName, lastName, email, status (active, inActive, invited)
              * ASSET_CATEGORY / CUSTOMER_CATEGORY: name, status
              * LOCATION: name, status
              * BIN: binNumber, status
              * INSPECTION: name, categoryName
              * CHECK_IN_OUT: status (checked in, checked out), assetId
            - If the user says "active" / "inactive" for status, use those exact values.
            - If no specific filters, return empty filters {}.
            - limit: if the user says "top 5" or "first 10", extract the number. Otherwise null.
            - Always return valid JSON only, no markdown, no extra text.
            
            Examples:
            User: "how many assets do I have?"
            {"entity":"ASSET","operation":"COUNT","filters":{},"limit":null,"explanation":"Count all assets"}
            
            User: "find customer with email john@example.com"
            {"entity":"CUSTOMER","operation":"FIND","filters":{"email":"john@example.com"},"limit":null,"explanation":"Find customer by email"}
            
            User: "show all active users"
            {"entity":"USER","operation":"LIST","filters":{"status":"active"},"limit":null,"explanation":"List all active users"}
            
            User: "how many assets are checked out"
            {"entity":"CHECK_IN_OUT","operation":"COUNT","filters":{"status":"checked out"},"limit":null,"explanation":"Count checked out assets"}
            
            User: "list top 5 assets in category Laptop"
            {"entity":"ASSET","operation":"LIST","filters":{"category":"Laptop"},"limit":5,"explanation":"List top 5 assets in Laptop category"}
            
            User: "give me a summary of everything"
            {"entity":"ASSET","operation":"SUMMARY","filters":{},"limit":null,"explanation":"Overall summary of all data"}
            """;

    // ──────────────────────────────────────────────────────────────
    // Seed intent embeddings on startup (once) for semantic fallback
    // ──────────────────────────────────────────────────────────────
    private static final Map<String, String[]> INTENT_SEEDS = Map.ofEntries(
            Map.entry("COUNT_ASSETS", new String[]{"ASSET", "COUNT"}),
            Map.entry("LIST_ASSETS", new String[]{"ASSET", "LIST"}),
            Map.entry("FIND_ASSET", new String[]{"ASSET", "FIND"}),
            Map.entry("COUNT_CUSTOMERS", new String[]{"CUSTOMER", "COUNT"}),
            Map.entry("LIST_CUSTOMERS", new String[]{"CUSTOMER", "LIST"}),
            Map.entry("FIND_CUSTOMER", new String[]{"CUSTOMER", "FIND"}),
            Map.entry("COUNT_USERS", new String[]{"USER", "COUNT"}),
            Map.entry("LIST_USERS", new String[]{"USER", "LIST"}),
            Map.entry("FIND_USER", new String[]{"USER", "FIND"}),
            Map.entry("LIST_CATEGORIES", new String[]{"ASSET_CATEGORY", "LIST"}),
            Map.entry("LIST_LOCATIONS", new String[]{"LOCATION", "LIST"}),
            Map.entry("COUNT_CHECK_IN_OUT", new String[]{"CHECK_IN_OUT", "COUNT"}),
            Map.entry("LIST_INSPECTIONS", new String[]{"INSPECTION", "LIST"}),
            Map.entry("SUMMARY", new String[]{"ASSET", "SUMMARY"})
    );

    private static final Map<String, String> INTENT_PHRASES = Map.ofEntries(
            Map.entry("COUNT_ASSETS", "how many assets do I have"),
            Map.entry("LIST_ASSETS", "show all assets list all assets"),
            Map.entry("FIND_ASSET", "find asset search asset by name serial number"),
            Map.entry("COUNT_CUSTOMERS", "how many customers do I have count customers"),
            Map.entry("LIST_CUSTOMERS", "show all customers list customers"),
            Map.entry("FIND_CUSTOMER", "find customer search customer by email name"),
            Map.entry("COUNT_USERS", "how many users count users team members"),
            Map.entry("LIST_USERS", "show all users list users team"),
            Map.entry("FIND_USER", "find user search user by email"),
            Map.entry("LIST_CATEGORIES", "show all categories list categories"),
            Map.entry("LIST_LOCATIONS", "show all locations list locations"),
            Map.entry("COUNT_CHECK_IN_OUT", "how many assets checked in checked out"),
            Map.entry("LIST_INSPECTIONS", "show all inspections list inspections"),
            Map.entry("SUMMARY", "give me summary overview dashboard stats")
    );

    // Cache embeddings in memory for fast cosine similarity
    private final List<CachedEmbedding> cachedEmbeddings = new ArrayList<>();

    private record CachedEmbedding(String intentKey, String entity, String operation, List<Double> vector) {}

    @PostConstruct
    public void initEmbeddings() {
        if (!openAIService.isConfigured()) {
            log.info("OpenAI not configured — chatbot will use keyword-based fallback only");
            return;
        }

        // Check if embeddings already seeded
        List<ChatBotEmbedding> existing = embeddingRepository.findByCompanyId(0L);
        if (existing.isEmpty()) {
            log.info("Seeding chatbot intent embeddings...");
            seedEmbeddings();
            existing = embeddingRepository.findByCompanyId(0L);
        }

        // Load into memory
        for (ChatBotEmbedding emb : existing) {
            if (emb.getEmbedding() != null && !emb.getEmbedding().isEmpty()) {
                cachedEmbeddings.add(new CachedEmbedding(
                        emb.getIntentKey(), emb.getEntity(), emb.getOperation(), emb.getEmbedding()));
            }
        }
        log.info("Loaded {} intent embeddings into cache", cachedEmbeddings.size());
    }

    private void seedEmbeddings() {
        for (Map.Entry<String, String> entry : INTENT_PHRASES.entrySet()) {
            String intentKey = entry.getKey();
            String phrase = entry.getValue();
            String[] meta = INTENT_SEEDS.get(intentKey);
            List<Double> vector = openAIService.getEmbedding(phrase);
            if (!vector.isEmpty()) {
                ChatBotEmbedding emb = new ChatBotEmbedding();
                emb.setCompanyId(0L);
                emb.setIntentKey(intentKey);
                emb.setPhrase(phrase);
                emb.setEntity(meta[0]);
                emb.setOperation(meta[1]);
                emb.setEmbedding(vector);
                embeddingRepository.save(emb);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //                     MAIN ENTRY POINT
    // ══════════════════════════════════════════════════════════════

    @Override
    public ChatBotResponseDTO processQuery(ChatBotRequestDTO request) {
        String query = request.getQuery();
        Long companyId = request.getCompanyId();

        if (query == null || query.trim().isEmpty()) {
            return new ChatBotResponseDTO("Please ask me something about your data.", null, false, null);
        }

        query = query.trim();
        log.info("ChatBot query [companyId={}]: {}", companyId, query);

        try {
            // ── Step 1: Use AI to parse the intent ──
            ParsedIntent intent = parseIntentWithAI(query);

            // ── Step 2: If AI fails, use embedding similarity fallback ──
            if (intent == null) {
                intent = parseIntentWithEmbeddings(query);
            }

            // ── Step 3: If embeddings fail, use keyword fallback ──
            if (intent == null) {
                intent = parseIntentWithKeywords(query);
            }

            // ── Step 4: If everything fails ──
            if (intent == null) {
                // Last resort: ask AI to just answer as a general assistant
                return handleGeneralQuery(query, companyId);
            }

            log.info("Resolved intent: entity={}, operation={}, filters={}", intent.entity, intent.operation, intent.filters);

            // ── Step 5: Execute the query ──
            ChatBotResponseDTO result = executeIntent(intent, companyId);

            // ── Step 6: If AI is available, polish the answer ──
            if (openAIService.isConfigured() && result.getData() != null) {
                result = polishResponse(query, result);
            }

            return result;

        } catch (Exception e) {
            log.error("ChatBot error: {}", e.getMessage(), e);
            return new ChatBotResponseDTO("Sorry, something went wrong while processing your query. Please try again.",
                    null, false, null);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //              STEP 1: AI-POWERED INTENT PARSING
    // ──────────────────────────────────────────────────────────────

    private record ParsedIntent(String entity, String operation, Map<String, String> filters, Integer limit) {}

    private ParsedIntent parseIntentWithAI(String query) {
        if (!openAIService.isConfigured()) return null;
        try {
            String aiResponse = openAIService.chatCompletion(SYSTEM_PROMPT, query);
            if (aiResponse == null) return null;

            JsonNode json = openAIService.parseJson(aiResponse);
            if (json == null) return null;

            String entity = json.path("entity").asText(null);
            String operation = json.path("operation").asText(null);
            Integer limit = json.has("limit") && !json.get("limit").isNull() ? json.get("limit").asInt() : null;

            Map<String, String> filters = new HashMap<>();
            JsonNode filtersNode = json.path("filters");
            if (filtersNode.isObject()) {
                filtersNode.fields().forEachRemaining(f -> {
                    String val = f.getValue().asText("");
                    if (!val.isBlank()) filters.put(f.getKey(), val);
                });
            }

            if (entity != null && operation != null) {
                return new ParsedIntent(entity, operation, filters, limit);
            }
        } catch (Exception e) {
            log.warn("AI intent parsing failed: {}", e.getMessage());
        }
        return null;
    }

    // ──────────────────────────────────────────────────────────────
    //        STEP 2: EMBEDDING-BASED SEMANTIC SIMILARITY
    // ──────────────────────────────────────────────────────────────

    private ParsedIntent parseIntentWithEmbeddings(String query) {
        if (!openAIService.isConfigured() || cachedEmbeddings.isEmpty()) return null;
        try {
            List<Double> queryVector = openAIService.getEmbedding(query);
            if (queryVector.isEmpty()) return null;

            double bestScore = -1;
            CachedEmbedding bestMatch = null;

            for (CachedEmbedding cached : cachedEmbeddings) {
                double score = cosineSimilarity(queryVector, cached.vector);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = cached;
                }
            }

            if (bestMatch != null && bestScore > 0.75) {
                log.info("Embedding match: {} (score={})", bestMatch.intentKey, bestScore);
                return new ParsedIntent(bestMatch.entity, bestMatch.operation, new HashMap<>(), null);
            }
        } catch (Exception e) {
            log.warn("Embedding fallback failed: {}", e.getMessage());
        }
        return null;
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size() || a.isEmpty()) return 0.0;
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0.0 : dotProduct / denom;
    }

    // ──────────────────────────────────────────────────────────────
    //           STEP 3: KEYWORD-BASED FALLBACK
    // ──────────────────────────────────────────────────────────────

    private ParsedIntent parseIntentWithKeywords(String query) {
        String q = query.toLowerCase();

        String entity = detectEntity(q);
        String operation = detectOperation(q);
        Map<String, String> filters = extractFilters(q, entity);

        if (entity != null && operation != null) {
            return new ParsedIntent(entity, operation, filters, null);
        }
        // If we at least detected an entity, default to LIST
        if (entity != null) {
            return new ParsedIntent(entity, "LIST", filters, null);
        }
        return null;
    }

    private String detectEntity(String q) {
        // Order matters — more specific first
        if (q.contains("check in") || q.contains("check out") || q.contains("checked in") || q.contains("checked out") || q.contains("checkin") || q.contains("checkout")) return "CHECK_IN_OUT";
        if (q.contains("inspection")) return "INSPECTION";
        if (q.contains("customer category") || q.contains("customer categories")) return "CUSTOMER_CATEGORY";
        if (q.contains("asset category") || q.contains("asset categories") || (q.contains("categor") && !q.contains("customer"))) return "ASSET_CATEGORY";
        if (q.contains("location")) return "LOCATION";
        if (q.contains("bin")) return "BIN";
        if (q.contains("asset")) return "ASSET";
        if (q.contains("customer") || q.contains("client")) return "CUSTOMER";
        if (q.contains("user") || q.contains("team") || q.contains("member") || q.contains("staff")) return "USER";
        return null;
    }

    private String detectOperation(String q) {
        if (q.contains("how many") || q.contains("count") || q.contains("total number")) return "COUNT";
        if (q.contains("summary") || q.contains("overview") || q.contains("dashboard") || q.contains("stats") || q.contains("statistics")) return "SUMMARY";
        if (q.contains("find") || q.contains("search") || q.contains("get") || q.contains("who") || q.contains("which") || q.contains("where")) return "FIND";
        if (q.contains("list") || q.contains("show") || q.contains("all") || q.contains("display")) return "LIST";
        return null;
    }

    private Map<String, String> extractFilters(String q, String entity) {
        Map<String, String> filters = new HashMap<>();

        // Status
        if (q.contains("active") && !q.contains("inactive")) filters.put("status", "active");
        else if (q.contains("inactive") || q.contains("in active")) filters.put("status", "inActive");
        else if (q.contains("invited")) filters.put("status", "invited");

        // Checked in/out
        if (q.contains("checked out") || q.contains("checkout")) filters.put("status", "checked out");
        else if (q.contains("checked in") || q.contains("checkin")) filters.put("status", "checked in");

        // Email patterns
        java.util.regex.Matcher emailMatcher = java.util.regex.Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}").matcher(q);
        if (emailMatcher.find()) filters.put("email", emailMatcher.group());

        // "named X" or "name X"
        java.util.regex.Matcher nameMatcher = java.util.regex.Pattern.compile("(?:named?|with name|name is)\\s+[\"']?([\\w\\s]+?)[\"']?(?:\\s|$)").matcher(q);
        if (nameMatcher.find()) filters.put("name", nameMatcher.group(1).trim());

        // "in category X" or "category X"
        java.util.regex.Matcher catMatcher = java.util.regex.Pattern.compile("(?:in category|category is|category)\\s+[\"']?([\\w\\s]+?)[\"']?(?:\\s|$)").matcher(q);
        if (catMatcher.find()) filters.put("category", catMatcher.group(1).trim());

        // "serial number X"
        java.util.regex.Matcher serialMatcher = java.util.regex.Pattern.compile("(?:serial(?:\\s+number)?)\\s+[\"']?([\\w-]+)[\"']?").matcher(q);
        if (serialMatcher.find()) filters.put("serialNumber", serialMatcher.group(1).trim());

        return filters;
    }

    // ──────────────────────────────────────────────────────────────
    //              STEP 5: EXECUTE THE QUERY
    // ──────────────────────────────────────────────────────────────

    private ChatBotResponseDTO executeIntent(ParsedIntent intent, Long companyId) {
        return switch (intent.entity) {
            case "ASSET" -> handleAsset(intent, companyId);
            case "CUSTOMER" -> handleCustomer(intent, companyId);
            case "USER" -> handleUser(intent, companyId);
            case "ASSET_CATEGORY" -> handleAssetCategory(intent, companyId);
            case "CUSTOMER_CATEGORY" -> handleCustomerCategory(intent, companyId);
            case "LOCATION" -> handleLocation(intent, companyId);
            case "BIN" -> handleBin(intent, companyId);
            case "INSPECTION" -> handleInspection(intent, companyId);
            case "CHECK_IN_OUT" -> handleCheckInOut(intent, companyId);
            default -> new ChatBotResponseDTO("I'm not sure which data you're asking about. " +
                    "Try asking about assets, customers, users, categories, locations, or inspections.",
                    null, false, intent.entity);
        };
    }

    // ── ASSET ──
    private ChatBotResponseDTO handleAsset(ParsedIntent intent, Long companyId) {
        if ("SUMMARY".equals(intent.operation)) return buildSummary(companyId);

        List<Assets> assets = queryWithFilters("assets", intent.filters, companyId, Assets.class);
        if (intent.limit != null && assets.size() > intent.limit) {
            assets = assets.subList(0, intent.limit);
        }

        return switch (intent.operation) {
            case "COUNT" -> new ChatBotResponseDTO(
                    "You have " + assets.size() + " asset(s)" + filterDesc(intent.filters) + ".",
                    Map.of("count", assets.size()), true, "ASSET");
            case "FIND" -> {
                if (assets.isEmpty()) yield new ChatBotResponseDTO(
                        "No assets found matching your criteria" + filterDesc(intent.filters) + ".",
                        null, true, "ASSET");
                yield new ChatBotResponseDTO(
                        "Found " + assets.size() + " asset(s)" + filterDesc(intent.filters) + ".",
                        assets, true, "ASSET");
            }
            default -> new ChatBotResponseDTO(
                    "Here are your " + assets.size() + " asset(s)" + filterDesc(intent.filters) + ".",
                    assets, true, "ASSET");
        };
    }

    // ── CUSTOMER ──
    private ChatBotResponseDTO handleCustomer(ParsedIntent intent, Long companyId) {
        List<CompanyCustomer> customers = queryWithFilters("companyCustomer", intent.filters, companyId, CompanyCustomer.class);
        if (intent.limit != null && customers.size() > intent.limit) {
            customers = customers.subList(0, intent.limit);
        }

        return switch (intent.operation) {
            case "COUNT" -> new ChatBotResponseDTO(
                    "You have " + customers.size() + " customer(s)" + filterDesc(intent.filters) + ".",
                    Map.of("count", customers.size()), true, "CUSTOMER");
            case "FIND" -> {
                if (customers.isEmpty()) yield new ChatBotResponseDTO(
                        "No customers found matching your criteria" + filterDesc(intent.filters) + ".",
                        null, true, "CUSTOMER");
                yield new ChatBotResponseDTO(
                        "Found " + customers.size() + " customer(s)" + filterDesc(intent.filters) + ".",
                        customers, true, "CUSTOMER");
            }
            default -> new ChatBotResponseDTO(
                    "Here are your " + customers.size() + " customer(s)" + filterDesc(intent.filters) + ".",
                    customers, true, "CUSTOMER");
        };
    }

    // ── USER ──
    private ChatBotResponseDTO handleUser(ParsedIntent intent, Long companyId) {
        List<Users> users;
        String statusFilter = intent.filters.get("status");

        if (statusFilter != null) {
            try {
                // Repository expects StatusEnum (active, inActive)
                StatusEnum statusEnum = StatusEnum.valueOf(statusFilter);
                users = usersRepository.findByCompanyIdAndStatus(companyId, statusEnum);
            } catch (IllegalArgumentException e) {
                // For statuses not in StatusEnum (e.g., "invited"), fetch all and filter in-memory
                users = usersRepository.findByCompanyId(companyId).stream()
                        .filter(u -> u.getStatus() != null && u.getStatus().name().equalsIgnoreCase(statusFilter))
                        .collect(Collectors.toList());
            }
        } else {
            users = usersRepository.findByCompanyId(companyId);
        }

        // Apply additional filters (email, name)
        String emailFilter = intent.filters.get("email");
        if (emailFilter != null) {
            users = users.stream().filter(u -> emailFilter.equalsIgnoreCase(u.getEmail())).collect(Collectors.toList());
        }
        String nameFilter = intent.filters.get("name");
        if (nameFilter != null) {
            users = users.stream().filter(u ->
                    (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(nameFilter.toLowerCase())) ||
                    (u.getLastName() != null && u.getLastName().toLowerCase().contains(nameFilter.toLowerCase()))
            ).collect(Collectors.toList());
        }

        if (intent.limit != null && users.size() > intent.limit) {
            users = users.subList(0, intent.limit);
        }

        // Sanitize: remove passwords from response
        users.forEach(u -> u.setPassword(null));

        return switch (intent.operation) {
            case "COUNT" -> new ChatBotResponseDTO(
                    "You have " + users.size() + " user(s)" + filterDesc(intent.filters) + ".",
                    Map.of("count", users.size()), true, "USER");
            case "FIND" -> {
                if (users.isEmpty()) yield new ChatBotResponseDTO(
                        "No users found matching your criteria" + filterDesc(intent.filters) + ".",
                        null, true, "USER");
                yield new ChatBotResponseDTO(
                        "Found " + users.size() + " user(s)" + filterDesc(intent.filters) + ".",
                        users, true, "USER");
            }
            default -> new ChatBotResponseDTO(
                    "Here are your " + users.size() + " user(s)" + filterDesc(intent.filters) + ".",
                    users, true, "USER");
        };
    }

    // ── ASSET CATEGORY ──
    private ChatBotResponseDTO handleAssetCategory(ParsedIntent intent, Long companyId) {
        String statusFilter = intent.filters.get("status");
        List<AssetCategory> categories;
        if (statusFilter != null) {
            categories = assetCategoryRepository.findByCompanyIdAndStatus(companyId, statusFilter);
        } else {
            categories = assetCategoryRepository.findByCompanyId(companyId);
        }

        return switch (intent.operation) {
            case "COUNT" -> new ChatBotResponseDTO(
                    "You have " + categories.size() + " asset category(ies)" + filterDesc(intent.filters) + ".",
                    Map.of("count", categories.size()), true, "ASSET_CATEGORY");
            default -> new ChatBotResponseDTO(
                    "Here are your " + categories.size() + " asset category(ies)" + filterDesc(intent.filters) + ".",
                    categories, true, "ASSET_CATEGORY");
        };
    }

    // ── CUSTOMER CATEGORY ──
    private ChatBotResponseDTO handleCustomerCategory(ParsedIntent intent, Long companyId) {
        String statusFilter = intent.filters.get("status");
        List<CompanyCustomerCategory> categories;
        if (statusFilter != null) {
            categories = companyCustomerCategoryRepository.findByCompanyIdAndStatus(companyId, statusFilter);
        } else {
            categories = companyCustomerCategoryRepository.findByCompanyId(companyId);
        }

        return switch (intent.operation) {
            case "COUNT" -> new ChatBotResponseDTO(
                    "You have " + categories.size() + " customer category(ies)" + filterDesc(intent.filters) + ".",
                    Map.of("count", categories.size()), true, "CUSTOMER_CATEGORY");
            default -> new ChatBotResponseDTO(
                    "Here are your " + categories.size() + " customer category(ies)" + filterDesc(intent.filters) + ".",
                    categories, true, "CUSTOMER_CATEGORY");
        };
    }

    // ── LOCATION ──
    private ChatBotResponseDTO handleLocation(ParsedIntent intent, Long companyId) {
        List<Location> locations = locationRepository.findByCompanyId(companyId);

        String nameFilter = intent.filters.get("name");
        if (nameFilter != null) {
            locations = locations.stream()
                    .filter(l -> l.getName() != null && l.getName().toLowerCase().contains(nameFilter.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return switch (intent.operation) {
            case "COUNT" -> new ChatBotResponseDTO(
                    "You have " + locations.size() + " location(s)" + filterDesc(intent.filters) + ".",
                    Map.of("count", locations.size()), true, "LOCATION");
            default -> new ChatBotResponseDTO(
                    "Here are your " + locations.size() + " location(s)" + filterDesc(intent.filters) + ".",
                    locations, true, "LOCATION");
        };
    }

    // ── BIN ──
    private ChatBotResponseDTO handleBin(ParsedIntent intent, Long companyId) {
        Query query = new Query(Criteria.where("companyId").is(companyId));
        List<Bin> bins = mongoTemplate.find(query, Bin.class);

        return switch (intent.operation) {
            case "COUNT" -> new ChatBotResponseDTO(
                    "You have " + bins.size() + " bin(s).",
                    Map.of("count", bins.size()), true, "BIN");
            default -> new ChatBotResponseDTO(
                    "Here are your " + bins.size() + " bin(s).",
                    bins, true, "BIN");
        };
    }

    // ── INSPECTION ──
    private ChatBotResponseDTO handleInspection(ParsedIntent intent, Long companyId) {
        List<AssetCategoryInspection> inspections = assetCategoryInspectionRepository.findByCompanyId(companyId);

        String nameFilter = intent.filters.get("name");
        if (nameFilter != null) {
            inspections = inspections.stream()
                    .filter(i -> i.getName() != null && i.getName().toLowerCase().contains(nameFilter.toLowerCase()))
                    .collect(Collectors.toList());
        }

        String categoryFilter = intent.filters.get("categoryName");
        if (categoryFilter == null) categoryFilter = intent.filters.get("category");
        if (categoryFilter != null) {
            final String catF = categoryFilter;
            inspections = inspections.stream()
                    .filter(data -> {
                        if (data.getCategoryName() == null || data.getCategoryName().isEmpty()) return false;
                        return data.getCategoryName().stream()
                                .filter(Objects::nonNull)
                                .anyMatch(obj -> {
                                    if (obj instanceof Map) {
                                        Object cn = ((Map<?, ?>) obj).get("categoryName");
                                        return cn != null && cn.toString().equalsIgnoreCase(catF);
                                    }
                                    return obj.toString().equalsIgnoreCase(catF);
                                });
                    })
                    .collect(Collectors.toList());
        }

        return switch (intent.operation) {
            case "COUNT" -> new ChatBotResponseDTO(
                    "You have " + inspections.size() + " inspection(s)" + filterDesc(intent.filters) + ".",
                    Map.of("count", inspections.size()), true, "INSPECTION");
            default -> new ChatBotResponseDTO(
                    "Here are your " + inspections.size() + " inspection(s)" + filterDesc(intent.filters) + ".",
                    inspections, true, "INSPECTION");
        };
    }

    // ── CHECK_IN_OUT ──
    private ChatBotResponseDTO handleCheckInOut(ParsedIntent intent, Long companyId) {
        String statusFilter = intent.filters.get("status");

        if ("COUNT".equals(intent.operation) && statusFilter != null) {
            Long count = assetCheckInOutRepository.countByCompanyIdAndStatus(companyId, statusFilter);
            return new ChatBotResponseDTO(
                    "There are " + count + " asset(s) with status '" + statusFilter + "'.",
                    Map.of("count", count, "status", statusFilter), true, "CHECK_IN_OUT");
        }

        List<AssetCheckInOut> records = assetCheckInOutRepository.findByCompanyId(companyId);
        if (statusFilter != null) {
            records = records.stream()
                    .filter(r -> statusFilter.equalsIgnoreCase(r.getStatus()))
                    .collect(Collectors.toList());
        }

        return switch (intent.operation) {
            case "COUNT" -> new ChatBotResponseDTO(
                    "You have " + records.size() + " check-in/out record(s)" + filterDesc(intent.filters) + ".",
                    Map.of("count", records.size()), true, "CHECK_IN_OUT");
            default -> new ChatBotResponseDTO(
                    "Here are your " + records.size() + " check-in/out record(s)" + filterDesc(intent.filters) + ".",
                    records, true, "CHECK_IN_OUT");
        };
    }

    // ──────────────────────────────────────────────────────────────
    //                   SUMMARY / DASHBOARD
    // ──────────────────────────────────────────────────────────────

    private ChatBotResponseDTO buildSummary(Long companyId) {
        Map<String, Object> summary = new LinkedHashMap<>();

        List<Assets> assets = assetsRepository.findByCompanyId(companyId);
        summary.put("totalAssets", assets.size());
        summary.put("activeAssets", assets.stream().filter(a -> "active".equalsIgnoreCase(a.getStatus())).count());

        List<CompanyCustomer> customers = companyCustomerRepository.findByCompanyId(companyId);
        summary.put("totalCustomers", customers.size());
        summary.put("activeCustomers", customers.stream().filter(c -> "active".equalsIgnoreCase(c.getStatus())).count());

        List<Users> users = usersRepository.findByCompanyId(companyId);
        summary.put("totalUsers", users.size());
        summary.put("activeUsers", users.stream().filter(u -> UserStatusEnum.active.equals(u.getStatus())).count());

        summary.put("totalAssetCategories", assetCategoryRepository.findByCompanyId(companyId).size());
        summary.put("totalCustomerCategories", companyCustomerCategoryRepository.findByCompanyId(companyId).size());
        summary.put("totalLocations", locationRepository.findByCompanyId(companyId).size());
        summary.put("totalInspections", assetCategoryInspectionRepository.findByCompanyId(companyId).size());

        Long checkedOut = assetCheckInOutRepository.countByCompanyIdAndStatus(companyId, "checked out");
        Long checkedIn = assetCheckInOutRepository.countByCompanyIdAndStatus(companyId, "checked in");
        summary.put("assetsCheckedOut", checkedOut);
        summary.put("assetsCheckedIn", checkedIn);

        // Build category breakdown for assets
        Map<String, Long> assetsByCategory = assets.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(Assets::getCategory, Collectors.counting()));
        summary.put("assetsByCategory", assetsByCategory);

        String text = String.format(
                "📊 Dashboard Summary:\n" +
                "• Assets: %d total (%d active)\n" +
                "• Customers: %d total (%d active)\n" +
                "• Users: %d total (%d active)\n" +
                "• Categories: %d asset, %d customer\n" +
                "• Locations: %d\n" +
                "• Inspections: %d\n" +
                "• Check-In/Out: %d checked in, %d checked out",
                summary.get("totalAssets"), summary.get("activeAssets"),
                summary.get("totalCustomers"), summary.get("activeCustomers"),
                summary.get("totalUsers"), summary.get("activeUsers"),
                summary.get("totalAssetCategories"), summary.get("totalCustomerCategories"),
                summary.get("totalLocations"),
                summary.get("totalInspections"),
                checkedIn, checkedOut
        );

        return new ChatBotResponseDTO(text, summary, true, "SUMMARY");
    }

    // ──────────────────────────────────────────────────────────────
    //          GENERIC MONGO QUERY BUILDER WITH FILTERS
    // ──────────────────────────────────────────────────────────────

    private <T> List<T> queryWithFilters(String collectionName, Map<String, String> filters, Long companyId, Class<T> clazz) {
        Query query = new Query(Criteria.where("companyId").is(companyId));

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue();
            // Case-insensitive regex match for string fields
            query.addCriteria(Criteria.where(field).regex("^" + java.util.regex.Pattern.quote(value) + "$", "i"));
        }

        return mongoTemplate.find(query, clazz);
    }

    // ──────────────────────────────────────────────────────────────
    //          STEP 6: AI-POLISHED NATURAL LANGUAGE RESPONSE
    // ──────────────────────────────────────────────────────────────

    private ChatBotResponseDTO polishResponse(String originalQuery, ChatBotResponseDTO rawResult) {
        try {
            String dataSnapshot;
            if (rawResult.getData() instanceof Map) {
                dataSnapshot = objectMapper.writeValueAsString(rawResult.getData());
            } else if (rawResult.getData() instanceof List<?> list) {
                // Send at most 10 items to AI to keep token usage low
                List<?> limited = list.size() > 10 ? list.subList(0, 10) : list;
                dataSnapshot = objectMapper.writeValueAsString(limited);
                if (list.size() > 10) {
                    dataSnapshot += "\n... and " + (list.size() - 10) + " more records.";
                }
            } else {
                dataSnapshot = String.valueOf(rawResult.getData());
            }

            String polishPrompt = """
                    You are a friendly data assistant for an asset management application.
                    The user asked: "%s"
                    The system found this data:
                    %s
                    
                    Write a concise, friendly, natural language answer summarizing the data.
                    Use bullet points for lists if more than 3 items.
                    Include key details like names, emails, counts, statuses.
                    Keep it under 300 words. Do not include raw JSON.
                    """.formatted(originalQuery, dataSnapshot);

            String polished = openAIService.chatCompletion(
                    "You are a helpful and concise data assistant. Respond in a friendly tone.",
                    polishPrompt);

            if (polished != null && !polished.isBlank()) {
                rawResult.setAnswerText(polished);
            }
        } catch (Exception e) {
            log.warn("Failed to polish response with AI: {}", e.getMessage());
            // Keep the original answer
        }
        return rawResult;
    }

    // ──────────────────────────────────────────────────────────────
    //         GENERAL QUERY (when no intent could be parsed)
    // ──────────────────────────────────────────────────────────────

    private ChatBotResponseDTO handleGeneralQuery(String query, Long companyId) {
        if (!openAIService.isConfigured()) {
            return new ChatBotResponseDTO(
                    "I couldn't understand your query. Try asking things like:\n" +
                    "• \"How many assets do I have?\"\n" +
                    "• \"List all active customers\"\n" +
                    "• \"Find user with email john@example.com\"\n" +
                    "• \"Show all categories\"\n" +
                    "• \"Give me a summary\"",
                    null, false, null);
        }

        // Provide a brief data context to AI so it can answer
        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("totalAssets", assetsRepository.findByCompanyId(companyId).size());
            context.put("totalCustomers", companyCustomerRepository.findByCompanyId(companyId).size());
            context.put("totalUsers", usersRepository.findByCompanyId(companyId).size());
            context.put("totalLocations", locationRepository.findByCompanyId(companyId).size());
            context.put("totalAssetCategories", assetCategoryRepository.findByCompanyId(companyId).size());

            String contextJson = objectMapper.writeValueAsString(context);

            String generalPrompt = """
                    You are a helpful assistant for an asset management application.
                    The user's company data summary: %s
                    
                    The user asked: "%s"
                    
                    If this is a data-related question, answer it using the context above.
                    If you don't have enough data, tell the user what kinds of questions you CAN answer:
                    assets, customers, users, categories, locations, inspections, check-in/out records.
                    Be concise and friendly.
                    """.formatted(contextJson, query);

            String answer = openAIService.chatCompletion(
                    "You are a helpful asset management assistant.", generalPrompt);

            if (answer != null) {
                return new ChatBotResponseDTO(answer, null, true, "GENERAL");
            }
        } catch (Exception e) {
            log.warn("General query handling failed: {}", e.getMessage());
        }

        return new ChatBotResponseDTO(
                "I couldn't understand your query. Try asking about assets, customers, users, categories, locations, or inspections.",
                null, false, null);
    }

    // ──────────────────────────────────────────────────────────────
    //                     UTILITY
    // ──────────────────────────────────────────────────────────────

    private String filterDesc(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" with ");
        List<String> parts = new ArrayList<>();
        filters.forEach((k, v) -> parts.add(k + "='" + v + "'"));
        sb.append(String.join(", ", parts));
        return sb.toString();
    }
}
