-- Example of Flyway handling DATA INSERTION, not just schema DDL: a small,
-- fixed set of known-bad reference entities that should be blacklisted in
-- every environment from day one (rather than requiring someone to call
-- POST /api/v1/fraud/blacklist manually after every fresh deployment).
-- Real entries added later via the API live alongside these -- Flyway only
-- owns what's seeded at migration time, not the table's ongoing contents.
INSERT INTO blacklist_entries (id, entity_ref, reason, added_at) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ACC-SANCTIONED-DEMO-1', 'Seeded demo: OFAC-style sanctions list match', CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000002', 'ACC-SANCTIONED-DEMO-2', 'Seeded demo: known fraud ring reference', CURRENT_TIMESTAMP);
