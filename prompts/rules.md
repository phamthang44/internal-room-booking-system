### DOMAIN RULES (MUST FOLLOW):

1. The system MUST support bilingual content (Vietnamese and English).
   - Do NOT remove or simplify i18n-related structures.
   - Existing translation tables or fields must be preserved.

2. Role management MUST remain in the database:
   - Keep the roles table and role_id relationship
   - Do NOT replace it with a simple string field if it already exists

3. Simplification should NOT remove domain features:
   - Only reduce technical complexity
   - Do NOT remove business capabilities

4. This is a university-style internal system:
   - Users are students/staff
   - Email-based registration is allowed
   - No need for strict identity verification

5. Avoid IAM-level complexity:
   - No wildcard permissions
   - No dynamic permission system
   - Keep RBAC simple