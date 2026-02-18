# Quick Reference - Asset Advanced Filtering

## Endpoint
```
POST /assets/advancedFilter/optimized
Content-Type: application/json
Authorization: Bearer <token>
```

## Minimal Request (Uses All Defaults)
```json
{
  "companyId": 100001
}
```
**Result**: All assets for company 100001, sorted by updatedAt DESC, first 10 records

## Common Use Cases

### 1. Filter by Customer with Pagination
```json
{
  "companyId": 100001,
  "customer": "Alex",
  "pageNumber": 0,
  "pageSize": 20
}
```

### 2. Filter by Multiple Predefined Fields
```json
{
  "companyId": 100001,
  "customer": "Alex",
  "status": "active",
  "category": "Electronics",
  "pageNumber": 0,
  "pageSize": 10
}
```

### 3. Filter by Custom Field
```json
{
  "companyId": 100001,
  "pageNumber": 0,
  "pageSize": 10,
  "customFields": {
    "warranty": "5years"
  }
}
```

### 4. Sort by Custom Field (if numeric)
```json
{
  "companyId": 100001,
  "sortField": "customFieldName",
  "sortDirection": "ASC",
  "pageNumber": 0,
  "pageSize": 10
}
```

### 5. Complex Filter with Custom Fields
```json
{
  "companyId": 100001,
  "customer": "Alex",
  "status": "active",
  "location": "location:696dd8895e32c53cf616e018",
  "pageNumber": 0,
  "pageSize": 10,
  "sortField": "updatedAt",
  "sortDirection": "DESC",
  "customFields": {
    "warranty": "5years",
    "department": "IT"
  }
}
```

## Response Fields Explanation

```json
{
  "assets": [...],           // Array of AssetWithCustomFieldsDTO
  "totalElements": 150,      // Total matching records
  "totalPages": 15,          // Total number of pages
  "currentPage": 0,          // Current page number (0-indexed)
  "pageSize": 10,            // Items per page
  "hasNext": true,           // More pages available?
  "hasPrevious": false       // Previous page available?
}
```

## Predefined Fields (Case-Insensitive Partial Match)
- `assetId` - Integer asset ID
- `name` - Asset name
- `customer` - Customer name
- `serialNumber` - Serial number
- `category` - Asset category
- `status` - Asset status (exact match)
- `email` - Email address

## Special Fields (Exact Match)
- `location` - Format: "location:ID" or "bin:ID"
- `companyId` - Company identifier (required)

## Custom Fields
- Any custom field name can be added to `customFields` map
- Values use partial matching (case-insensitive)
- Multiple custom fields can be combined (OR logic)

## Sorting Rules
```
sortField = null/empty  →  Defaults to "updatedAt"
sortField = "name"      →  Sort by name field
sortField = "<custom>"  →  Sort by custom field
sortDirection = "DESC"  →  Highest/newest first (default)
sortDirection = "ASC"   →  Lowest/oldest first
```

## Common Issues & Solutions

### Empty Results
- Check if `companyId` is correct
- Verify filter values exist in database
- Try removing or broadening custom field filters

### Wrong Sort Order
- Ensure `sortDirection` is "ASC" or "DESC" (case-sensitive)
- Verify `sortField` exists in your data
- Check if field contains numeric or string values

### Performance Issues
- Reduce `pageSize` if response is slow
- Narrow down filters to specific fields
- Use `location` filter (exact match) instead of custom fields

### No Custom Fields Returned
- Verify custom field names match exactly (case-sensitive)
- Check that custom fields exist for the assets
- Ensure custom field values are set (not null)

## Permission Requirements
- User must have `view` permission for assets (CustomRoleType.view)

## Error Handling
All errors return proper HTTP status codes:
- `200 OK` - Success (may contain empty asset list)
- `401 Unauthorized` - Missing/invalid authentication
- `403 Forbidden` - Insufficient permissions
- `400 Bad Request` - Invalid request format
- `500 Internal Server Error` - Server error (check logs)

## Rate Limiting
Same as existing API endpoints - check rate limit headers

---

**Example cURL Command**:
```bash
curl -X POST http://localhost:8080/assets/advancedFilter/optimized \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "companyId": 100001,
    "customer": "Alex",
    "status": "active",
    "pageNumber": 0,
    "pageSize": 10,
    "sortField": "updatedAt",
    "sortDirection": "DESC"
  }'
```

---

For detailed architecture documentation, see: `ASSET_FILTERING_ARCHITECTURE.md`

