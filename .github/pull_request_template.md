## Summary

<!-- Briefly describe what this PR does and why -->

## Type of change

- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Infrastructure / configuration change
- [ ] Documentation update

## Checklist

- [ ] I have added/updated unit tests for the changed code
- [ ] All existing tests pass locally (`mvn verify`)
- [ ] I have checked the Flyway migration scripts against the JPA entities if DB schema changed
- [ ] I have not committed any secrets, API keys, or credentials
- [ ] For security-sensitive changes: I have requested a review from `@security-team`
- [ ] For Terraform/Helm changes: I have run `terraform validate` / `helm template` locally

## Security impact (fill in if applicable)

- Does this change any auth/JWT/HMAC/encryption behavior? If so, describe.
- Does this change affect PII handling (e.g., nationalId field, card data)?
- Does this change any service-to-service trust boundaries?

## Testing

Describe what you tested and how:

## Screenshots / logs (if applicable)
