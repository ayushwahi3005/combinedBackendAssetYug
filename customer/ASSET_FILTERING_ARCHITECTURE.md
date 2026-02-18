# Asset Advanced Filtering with Pagination and Custom Fields

## Overview
This document describes the optimized architecture for fetching assets with pagination, advanced filtering (predefined and custom fields), and sorting in a MongoDB-backed Spring Boot application.

## Architecture

### Components

#### 1. **DTOs (Data Transfer Objects)**

##### `AssetAdvancedFilterDTO`
- Contains filter criteria for both predefined and custom fields
- Fields:
  - `assetId`: Filter by asset ID
  - `name`: Filter by asset name (case-insensitive, partial match)
  - `customer`: Filter by customer name
  - `serialNumber`: Filter by serial number
  - `category`: Filter by category
  - `location`: Filter by location (location:ID or bin:ID format)
  - `status`: Filter by asset status
  - `email`: Filter by email
  - `companyId`: Company ID (required)
  - `pageNumber`: Page number (default: 0)
  - `pageSize`: Page size (default: 10)
  - `sortField`: Field to sort by (defaults to "updatedAt" if null/empty)
  - `sortDirection`: ASC or DESC (default: DESC)
  - `customFields`: Map of custom field filters (e.g., {"fieldName": "value"})

- Methods:
  - `getEffectiveSortField()`: Returns sortField if provided, otherwise "updatedAt"
  - `hasCustomFieldFilters()`: Checks if custom field filters exist

##### `AssetWithCustomFieldsDTO`
- Response DTO containing both predefined and custom field data
- Fields:
  - All predefined asset fields (id, name, serialNumber, category, etc.)
  - `customFields`: Map of custom field name-value pairs

##### `PaginatedAssetResponseDTO`
- Wrapper response DTO for paginated data
- Fields:
  - `assets`: List of AssetWithCustomFieldsDTO
  - `totalElements`: Total count of matching assets
  - `totalPages`: Total number of pages
  - `currentPage`: Current page number
  - `pageSize`: Page size
  - `hasNext`: Boolean indicating if next page exists
  - `hasPrevious`: Boolean indicating if previous page exists

#### 2. **Repository Layer**

##### `AssetRepositoryCustomAdvanced` (Interface)
Defines custom MongoDB query methods:
```java
PaginatedAssetResponseDTO getAssetsWithAdvancedFilter(AssetAdvancedFilterDTO filter);
long countAssetsWithAdvancedFilter(AssetAdvancedFilterDTO filter);
Page<Assets> findByCompanyIdWithSort(Long companyId, String sortField, String sortDirection, Pageable pageable);
```

##### `AssetRepositoryCustomAdvancedImpl` (Implementation)
- Uses MongoDB aggregation pipeline for optimized querying
- Key features:
  - **Single Query**: Combines asset and custom field data in one aggregation
  - **Efficient Filtering**: Applies filters at database level
  - **Sorting**: Sorts by effective sort field before pagination
  - **Lookup Join**: Uses MongoDB `$lookup` stage to join with AssetExtraFields collection
  - **Pagination**: Uses `$skip` and `$limit` stages for efficient pagination

#### 3. **Service Layer**

##### `AssetsService` Interface
New method:
```java
PaginatedAssetResponseDTO getAssetsWithAdvancedFilter(AssetAdvancedFilterDTO filter);
```

##### `AssetsServiceImpl` Implementation
- Delegates to custom repository for optimized queries
- Enriches location information (converts location:ID to location name)
- Caches location and bin names for efficiency
- Logs all operations for debugging

#### 4. **Controller Layer**

##### `AssetAPI` Controller
New endpoint:
```
POST /assets/advancedFilter/optimized
```

**Request Body:**
```json
{
  "assetId": "1",
  "name": "Asset1",
  "customer": "Alex",
  "serialNumber": "10000",
  "category": "MYCAT",
  "location": "location:696dd8895e32c53cf616e018",
  "status": "active",
  "email": null,
  "companyId": 100001,
  "pageNumber": 0,
  "pageSize": 10,
  "sortField": "updatedAt",
  "sortDirection": "DESC",
  "customFields": {
    "new": "12122",
    "fieldName": "fieldValue"
  }
}
```

**Response:**
```json
{
  "assets": [
    {
      "id": "696dd34e5e32c53cf616e00c",
      "assetId": 1,
      "name": "Asset1",
      "serialNumber": "10000",
      "category": "MYCAT",
      "customer": "Alex",
      "customerId": "696dd34e5e32c53cf616e00a",
      "location": "location:696dd8895e32c53cf616e018",
      "locationName": "Main Warehouse",
      "status": "active",
      "email": null,
      "image": "",
      "companyId": 100001,
      "updatedAt": "2026-01-21T23:47:15.539266100",
      "customFields": {
        "new": "2e2",
        "new22": "2e22222"
      }
    }
  ],
  "totalElements": 150,
  "totalPages": 15,
  "currentPage": 0,
  "pageSize": 10,
  "hasNext": true,
  "hasPrevious": false
}
```

## How It Works

### Query Flow

1. **Client Request**: Frontend sends filter criteria with custom fields and pagination params
2. **Controller**: Validates and sets defaults (pageNumber=0, pageSize=10, sortDirection=DESC)
3. **Service**: Delegates to repository and enriches location names
4. **Repository**: 
   - Builds MongoDB aggregation pipeline
   - Stage 1: Match predefined field filters
   - Stage 2: Sort by effective sort field (defaults to updatedAt)
   - Stage 3: Lookup and join with AssetExtraFields collection
   - Stage 4-5: Apply pagination (skip and limit)
5. **Response**: Converts MongoDB results to DTO objects with combined data

### Aggregation Pipeline (MongoDB)

```javascript
[
  {
    $match: {
      companyId: 100001,
      customer: /.*Alex.*/i,
      status: /.*active.*/i,
      location: "location:696dd8895e32c53cf616e018"
    }
  },
  {
    $sort: { updatedAt: -1 }  // DESC order
  },
  {
    $lookup: {
      from: "assetExtraFields",
      localField: "_id",
      foreignField: "assetId",
      as: "extraFields"
    }
  },
  { $skip: 0 },
  { $limit: 10 }
]
```

## Performance Optimizations

1. **Single Aggregation Query**: Combines all filtering, joining, and pagination in one database operation
2. **Index Usage**: Leverage MongoDB indexes on:
   - `companyId`
   - `assetId` (in AssetExtraFields)
   - `updatedAt`
3. **Regex Escaping**: Escapes special characters in filter values to prevent injection
4. **Location Caching**: Caches all locations and bins to avoid repeated lookups
5. **Lazy Filtering**: Custom field filtering happens after asset filtering
6. **Null Safety**: Handles null values gracefully

## Usage Examples

### Example 1: Simple Filter with Default Sorting
```json
{
  "companyId": 100001,
  "customer": "Alex",
  "pageNumber": 0,
  "pageSize": 10
}
// Sorts by updatedAt DESC by default
```

### Example 2: Filter by Custom Field
```json
{
  "companyId": 100001,
  "customFields": {
    "new": "12122",
    "warranty": "5years"
  },
  "pageNumber": 0,
  "pageSize": 10
}
```

### Example 3: Sort by Custom Field Name
```json
{
  "companyId": 100001,
  "sortField": "name",
  "sortDirection": "ASC",
  "pageNumber": 0,
  "pageSize": 20
}
```

### Example 4: Complex Filter with Multiple Criteria
```json
{
  "companyId": 100001,
  "customer": "Alex",
  "status": "active",
  "location": "location:696dd8895e32c53cf616e018",
  "category": "MYCAT",
  "customFields": {
    "new": "12122"
  },
  "sortField": "updatedAt",
  "sortDirection": "DESC",
  "pageNumber": 0,
  "pageSize": 10
}
```

## Handling Large Datasets

The architecture is optimized for handling large-scale data:

1. **Pagination**: Limits database results to pageSize records
2. **Aggregation Pipeline**: Processes data at database level, not application level
3. **Efficient Sorting**: Applies sorting before pagination
4. **Index Usage**: Leverages MongoDB indexes for fast filtering
5. **Memory Efficient**: Doesn't load entire collection into memory

### Recommended Indexes

```javascript
// In MongoDB
db.assets.createIndex({ companyId: 1, updatedAt: -1 })
db.assets.createIndex({ companyId: 1, customer: 1 })
db.assets.createIndex({ companyId: 1, status: 1 })
db.assetExtraFields.createIndex({ assetId: 1, name: 1 })
db.assetExtraFields.createIndex({ companyId: 1, name: 1 })
```

## Backward Compatibility

The existing `advanceFilter` method has been updated to:
- Default sortField to "updatedAt" if null or empty
- Maintain the same behavior with enhanced sorting logic
- Continue supporting the existing endpoint

## Fallback/Default Behavior

| Parameter | Default | Behavior |
|-----------|---------|----------|
| pageNumber | 0 | First page |
| pageSize | 10 | 10 items per page |
| sortField | "updatedAt" | Sort by last updated time if null/empty |
| sortDirection | "DESC" | Newest first |
| customFields | empty | No custom field filtering |

## Error Handling

- Invalid pageNumber/pageSize: Defaults to valid values
- Null companyId: Returns empty result
- Invalid filter values: Treated as no filter
- Database errors: Logged and returns empty response

## Notes

- All string filters use case-insensitive partial matching (regex)
- Location filter requires exact match in format "location:ID" or "bin:ID"
- Custom field filters are case-insensitive and use partial matching
- Response includes both predefined fields and custom fields in single object
- Location names are resolved from IDs in the enrichment step

