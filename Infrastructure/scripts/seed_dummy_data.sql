-- =============================================================================
-- EventTicketingPlatform — dummy data
-- =============================================================================
-- Pipe into the Postgres container AFTER Liquibase has created the tables
-- (start the Spring Boot server once, or wait for it to finish booting):
--
--   docker exec -i eventticketing-postgres \
--     psql -U postgres -d eventticketing -v ON_ERROR_STOP=1 \
--     < scripts/seed_dummy_data.sql
--
-- Or from the repo root:
--
--   ./scripts/seed.sh
--
-- Safe to re-run: previous dummy rows (matched by email) are removed first.
-- Real accounts that do not use the demo emails are left alone.
--
-- Demo logins (password for organizers + customers: Password123!)
--   admin@example.com              ADMIN      Admin12345678Admin
--   nader.farouk@nileevents.eg     ORGANIZER  Password123!
--   salma.khalil@cairolive.eg      ORGANIZER  Password123!
--   yasmine.nabil@pyramidarts.eg   ORGANIZER  Password123!
--   layla.hassan@demo.local        CUSTOMER   Password123!
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Wipe a previous dummy run (FK order)
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE _demo_user_emails (email text PRIMARY KEY);
INSERT INTO _demo_user_emails (email) VALUES
  ('nader.farouk@nileevents.eg'),
  ('salma.khalil@cairolive.eg'),
  ('yasmine.nabil@pyramidarts.eg'),
  ('layla.hassan@demo.local'),
  ('omar.said@demo.local'),
  ('noor.adel@demo.local'),
  ('karim.mostafa@demo.local'),
  ('hana.fouad@demo.local'),
  ('tamer.rashad@demo.local'),
  ('dina.magdy@demo.local'),
  ('youssef.ibrahim@demo.local');

CREATE TEMP TABLE _demo_app_emails (email text PRIMARY KEY);
INSERT INTO _demo_app_emails (email)
SELECT email FROM _demo_user_emails
UNION ALL
SELECT * FROM (VALUES
  ('rania.lotfy@newwave.eg'),
  ('hassan.omar@deltaevents.eg'),
  ('tarek.shawky@midnight.eg')
) AS extra (email);

CREATE TEMP TABLE _demo_users AS
SELECT u.id
FROM users u
JOIN _demo_user_emails d ON d.email = u.email;

DELETE FROM ticket_attendee
WHERE customer_id IN (SELECT id FROM _demo_users)
   OR ticket_id IN (
        SELECT t.uuid
        FROM tickets t
        WHERE t.user_owner_uuid IN (SELECT id FROM _demo_users)
           OR t.evnt IN (SELECT e.id FROM events e WHERE e.organizer_id IN (SELECT id FROM _demo_users))
      );

DELETE FROM tickets
WHERE user_owner_uuid IN (SELECT id FROM _demo_users)
   OR evnt IN (SELECT e.id FROM events e WHERE e.organizer_id IN (SELECT id FROM _demo_users));

DELETE FROM booking
WHERE user_id IN (SELECT id FROM _demo_users)
   OR event_id IN (SELECT e.id FROM events e WHERE e.organizer_id IN (SELECT id FROM _demo_users));

DELETE FROM seat_categories
WHERE event_id IN (SELECT e.id FROM events e WHERE e.organizer_id IN (SELECT id FROM _demo_users));

DELETE FROM events
WHERE organizer_id IN (SELECT id FROM _demo_users);

DELETE FROM venues
WHERE requested_by IN (SELECT id FROM _demo_users);

DELETE FROM organizer_applications
WHERE email IN (SELECT email FROM _demo_app_emails);

DELETE FROM users
WHERE id IN (SELECT id FROM _demo_users);

-- ---------------------------------------------------------------------------
-- 2. Admin — keep the existing row if Spring already created it
-- ---------------------------------------------------------------------------
INSERT INTO users (id, name, email, phone, password, role, created_at)
SELECT
  'a0000000-0000-4000-8000-000000000001',
  'Platform Admin',
  'admin@example.com',
  '+201000000001',
  '$2a$10$ab.ljpAEnauIcrdGv75mTOKDO1M29bN2Cx3wvI1miRtIMw2j63TXS', -- Admin12345678Admin
  'ADMIN',
  now() - interval '40 days'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@example.com');

-- ---------------------------------------------------------------------------
-- 3. Organizers + customers
--    BCrypt hash below is Password123! (Spring BCryptPasswordEncoder, cost 10)
-- ---------------------------------------------------------------------------
INSERT INTO users (id, name, email, phone, password, role, created_at) VALUES
  ('a0000000-0000-4000-8000-000000000011', 'Nader Farouk',    'nader.farouk@nileevents.eg',   '+201011112221', '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'ORGANIZER', now() - interval '35 days'),
  ('a0000000-0000-4000-8000-000000000012', 'Salma Khalil',    'salma.khalil@cairolive.eg',    '+201011112222', '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'ORGANIZER', now() - interval '34 days'),
  ('a0000000-0000-4000-8000-000000000013', 'Yasmine Nabil',   'yasmine.nabil@pyramidarts.eg', '+201011112223', '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'ORGANIZER', now() - interval '33 days'),
  ('a0000000-0000-4000-8000-000000000021', 'Layla Hassan',    'layla.hassan@demo.local',      '+201012223331', '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'CUSTOMER',  now() - interval '20 days'),
  ('a0000000-0000-4000-8000-000000000022', 'Omar Said',       'omar.said@demo.local',         '+201012223332', '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'CUSTOMER',  now() - interval '19 days'),
  ('a0000000-0000-4000-8000-000000000023', 'Noor Adel',       'noor.adel@demo.local',         '+201012223333', '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'CUSTOMER',  now() - interval '18 days'),
  ('a0000000-0000-4000-8000-000000000024', 'Karim Mostafa',   'karim.mostafa@demo.local',     '+201012223334', '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'CUSTOMER',  now() - interval '17 days'),
  ('a0000000-0000-4000-8000-000000000025', 'Hana Fouad',      'hana.fouad@demo.local',        '+201012223335', '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'CUSTOMER',  now() - interval '16 days'),
  ('a0000000-0000-4000-8000-000000000026', 'Tamer Rashad',    'tamer.rashad@demo.local',      '+201012223336', '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'CUSTOMER',  now() - interval '15 days'),
  ('a0000000-0000-4000-8000-000000000027', 'Dina Magdy',      'dina.magdy@demo.local',        '+201012223337', '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'CUSTOMER',  now() - interval '14 days'),
  ('a0000000-0000-4000-8000-000000000028', 'Youssef Ibrahim', 'youssef.ibrahim@demo.local',   '+201012223338', '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'CUSTOMER',  now() - interval '13 days');

-- ---------------------------------------------------------------------------
-- 4. Organizer applications (approved / pending / rejected)
-- ---------------------------------------------------------------------------
INSERT INTO organizer_applications (
  id, name, email, phone, password_hash, organization_name, reason,
  status, submitted_at, reviewed_at, reviewed_by, rejection_reason
) VALUES
  ('aa000000-0000-4000-8000-000000000011', 'Nader Farouk', 'nader.farouk@nileevents.eg', '+201011112221',
   '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'Nile Events Co',
   'We produce large-scale sports and music events across Greater Cairo.',
   'APPROVED', now() - interval '36 days', now() - interval '35 days',
   (SELECT id FROM users WHERE email = 'admin@example.com'), NULL),
  ('aa000000-0000-4000-8000-000000000012', 'Salma Khalil', 'salma.khalil@cairolive.eg', '+201011112222',
   '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'Cairo Live Productions',
   'Theatre and live-music promoter based in Zamalek.',
   'APPROVED', now() - interval '35 days', now() - interval '34 days',
   (SELECT id FROM users WHERE email = 'admin@example.com'), NULL),
  ('aa000000-0000-4000-8000-000000000013', 'Yasmine Nabil', 'yasmine.nabil@pyramidarts.eg', '+201011112223',
   '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'Pyramid Arts Collective',
   'Cultural programmes at heritage sites around Giza.',
   'APPROVED', now() - interval '34 days', now() - interval '33 days',
   (SELECT id FROM users WHERE email = 'admin@example.com'), NULL),
  ('aa000000-0000-4000-8000-000000000014', 'Rania Lotfy', 'rania.lotfy@newwave.eg', '+201015556661',
   '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'New Wave Festivals',
   'Independent music festivals across the Delta — applying to list our autumn circuit.',
   'PENDING', now() - interval '2 days', NULL, NULL, NULL),
  ('aa000000-0000-4000-8000-000000000015', 'Hassan Omar', 'hassan.omar@deltaevents.eg', '+201015556662',
   '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'Delta Sports Agency',
   'We want to host amateur football cups and youth tournaments.',
   'PENDING', now() - interval '1 day', NULL, NULL, NULL),
  ('aa000000-0000-4000-8000-000000000016', 'Tarek Shawky', 'tarek.shawky@midnight.eg', '+201015556663',
   '$2a$10$fznQmnwjeSQDaoytiqCf3.ronpSEbfyAHca10jaWY0JVMTW4S9ro2', 'Midnight Parties LLC',
   'Nightclub takeover series in unused industrial halls.',
   'REJECTED', now() - interval '12 days', now() - interval '10 days',
   (SELECT id FROM users WHERE email = 'admin@example.com'),
   'Does not meet venue safety and licensing requirements.');

-- ---------------------------------------------------------------------------
-- 5. Venues
-- ---------------------------------------------------------------------------
INSERT INTO venues (id, name, address, capacity, requested_by, status, reviewed_at, reviewed_by) VALUES
  ('b0000000-0000-4000-8000-000000000001', 'Cairo International Stadium', 'Nasr City, Cairo', 75000,
   'a0000000-0000-4000-8000-000000000011', 'APPROVED', now() - interval '30 days',
   (SELECT id FROM users WHERE email = 'admin@example.com')),
  ('b0000000-0000-4000-8000-000000000002', 'Cairo Opera House', 'Zamalek, Cairo', 1200,
   'a0000000-0000-4000-8000-000000000012', 'APPROVED', now() - interval '30 days',
   (SELECT id FROM users WHERE email = 'admin@example.com')),
  ('b0000000-0000-4000-8000-000000000003', 'AUC New Cairo Campus', 'Fifth Settlement, New Cairo', 800,
   'a0000000-0000-4000-8000-000000000013', 'APPROVED', now() - interval '29 days',
   (SELECT id FROM users WHERE email = 'admin@example.com')),
  ('b0000000-0000-4000-8000-000000000004', 'Al Manara International Conference Center', 'New Cairo', 2500,
   'a0000000-0000-4000-8000-000000000011', 'APPROVED', now() - interval '29 days',
   (SELECT id FROM users WHERE email = 'admin@example.com')),
  ('b0000000-0000-4000-8000-000000000005', 'Sound and Light Theatre', 'Pyramids Road, Giza', 1500,
   'a0000000-0000-4000-8000-000000000013', 'APPROVED', now() - interval '28 days',
   (SELECT id FROM users WHERE email = 'admin@example.com')),
  ('b0000000-0000-4000-8000-000000000006', 'Cairo Festival City Arena', 'New Cairo', 5000,
   'a0000000-0000-4000-8000-000000000012', 'PENDING', NULL, NULL),
  ('b0000000-0000-4000-8000-000000000007', 'Bibliotheca Alexandrina Great Hall', 'Chatby, Alexandria', 1800,
   'a0000000-0000-4000-8000-000000000011', 'REJECTED', now() - interval '15 days',
   (SELECT id FROM users WHERE email = 'admin@example.com'));

-- ---------------------------------------------------------------------------
-- 6. Events (dates are relative so they stay in the future)
-- ---------------------------------------------------------------------------
INSERT INTO events (
  id, title, description, category, event_date, event_time, status,
  venue_id, organizer_id, created_at, updated_at
) VALUES
  ('c0000000-0000-4000-8000-000000000001', 'Cairo Jazz Nights',
   'An evening of contemporary Egyptian and international jazz on the Opera House main stage, featuring the Cairo Jazz Project and guest soloists.',
   'MUSIC', CURRENT_DATE + 18, TIME '21:00', 'PUBLISHED',
   'b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000012',
   now() - interval '20 days', now() - interval '10 days'),

  ('c0000000-0000-4000-8000-000000000002', 'Nile Beats Festival',
   'Open-air electronic and alternative music festival with three stages, food trucks and late-night sets looking out over Nasr City.',
   'MUSIC', CURRENT_DATE + 32, TIME '19:00', 'PUBLISHED',
   'b0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000011',
   now() - interval '18 days', now() - interval '9 days'),

  ('c0000000-0000-4000-8000-000000000003', 'Cairo Derby: Al Ahly vs Zamalek',
   'The Cairo Derby returns to Cairo International Stadium. Limited hospitality boxes and general admission on sale.',
   'SPORTS', CURRENT_DATE + 14, TIME '19:30', 'PUBLISHED',
   'b0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000011',
   now() - interval '22 days', now() - interval '8 days'),

  ('c0000000-0000-4000-8000-000000000004', 'Egypt Super Cup Final',
   'Season-opening Super Cup final under the lights at Cairo International Stadium.',
   'SPORTS', CURRENT_DATE + 45, TIME '20:00', 'PUBLISHED',
   'b0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000011',
   now() - interval '12 days', now() - interval '6 days'),

  ('c0000000-0000-4000-8000-000000000005', 'Cairo Tech Summit 2026',
   'Two keynote halls and a startup expo covering AI, fintech and climate tech across the MENA region.',
   'CONFERENCE', CURRENT_DATE + 21, TIME '09:00', 'PUBLISHED',
   'b0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000011',
   now() - interval '25 days', now() - interval '11 days'),

  ('c0000000-0000-4000-8000-000000000006', 'FinTech Africa Forum',
   'A focused day of panels and workshops on payments, open banking and digital identity, hosted at AUC New Cairo.',
   'CONFERENCE', CURRENT_DATE + 28, TIME '09:30', 'PUBLISHED',
   'b0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000013',
   now() - interval '16 days', now() - interval '7 days'),

  ('c0000000-0000-4000-8000-000000000007', 'Aida at the Opera',
   'Verdi''s Aida in a new production by the Cairo Opera Company, with full orchestra and chorus.',
   'THEATRE', CURRENT_DATE + 12, TIME '20:00', 'PUBLISHED',
   'b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000012',
   now() - interval '24 days', now() - interval '12 days'),

  ('c0000000-0000-4000-8000-000000000008', 'Hamlet in Arabic',
   'A contemporary Arabic-language staging of Hamlet, directed by a visiting ensemble from Alexandria.',
   'THEATRE', CURRENT_DATE + 40, TIME '19:30', 'PUBLISHED',
   'b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000012',
   now() - interval '14 days', now() - interval '5 days'),

  ('c0000000-0000-4000-8000-000000000009', 'Pyramids Sound & Light',
   'The classic Sound and Light show on the Giza plateau — narration, projection and live score against the pyramids.',
   'OTHER', CURRENT_DATE + 7, TIME '19:30', 'PUBLISHED',
   'b0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000013',
   now() - interval '10 days', now() - interval '4 days'),

  ('c0000000-0000-4000-8000-000000000010', 'Community Iftar Night',
   'Draft community iftar with live oud, shared tables and a short heritage talk. Seat map still being finalized.',
   'OTHER', CURRENT_DATE + 50, TIME '18:30', 'DRAFT',
   'b0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000011',
   now() - interval '3 days', now() - interval '1 day'),

  ('c0000000-0000-4000-8000-000000000011', 'Indie Showcase (Cancelled)',
   'Cancelled due to a last-minute venue conflict. Refunds are being processed for confirmed bookings.',
   'MUSIC', CURRENT_DATE + 10, TIME '20:00', 'CANCELLED',
   'b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000012',
   now() - interval '15 days', now() - interval '2 days');

-- ---------------------------------------------------------------------------
-- 7. Seat categories
--    available_seats = total minus PENDING + CONFIRMED quantities below
--    seating_capacity = max seats one customer may hold in that category
-- ---------------------------------------------------------------------------
INSERT INTO seat_categories (
  id, event_id, venue_id, name, price, total_seats, available_seats, seating_capacity
) VALUES
  -- Jazz (opera)
  ('d0000000-0000-4000-8000-000000000101', 'c0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002', 'VIP',      2500.00,    80,    77, 4),
  ('d0000000-0000-4000-8000-000000000102', 'c0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002', 'Regular',   450.00,   400,   397, 6),
  ('d0000000-0000-4000-8000-000000000103', 'c0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002', 'Student',   200.00,   100,    98, 2),
  -- Nile Beats (stadium)
  ('d0000000-0000-4000-8000-000000000201', 'c0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000001', 'VIP',      1800.00,   500,   498, 6),
  ('d0000000-0000-4000-8000-000000000202', 'c0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000001', 'General',   350.00,  8000,  7996, 8),
  -- Derby (stadium)
  ('d0000000-0000-4000-8000-000000000301', 'c0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000001', 'VIP Box',  3500.00,   200,   198, 8),
  ('d0000000-0000-4000-8000-000000000302', 'c0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000001', 'Lower Tier',650.00, 15000, 14994, 6),
  ('d0000000-0000-4000-8000-000000000303', 'c0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000001', 'Upper Tier',220.00, 30000, 29997, 8),
  -- Super Cup (stadium)
  ('d0000000-0000-4000-8000-000000000401', 'c0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000001', 'Lower Tier',700.00, 15000, 14998, 6),
  ('d0000000-0000-4000-8000-000000000402', 'c0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000001', 'VIP Box',  4000.00,   200,   200, 8),
  ('d0000000-0000-4000-8000-000000000403', 'c0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000001', 'Upper Tier',250.00, 30000, 30000, 8),
  -- Tech summit (manara)
  ('d0000000-0000-4000-8000-000000000501', 'c0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000004', 'Premium',  1200.00,   200,   199, 2),
  ('d0000000-0000-4000-8000-000000000502', 'c0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000004', 'General',   350.00,   800,   798, 2),
  -- FinTech (auc)
  ('d0000000-0000-4000-8000-000000000601', 'c0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000003', 'Premium',   900.00,   100,    99, 2),
  ('d0000000-0000-4000-8000-000000000602', 'c0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000003', 'General',   250.00,   400,   399, 2),
  -- Aida (opera)
  ('d0000000-0000-4000-8000-000000000701', 'c0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000002', 'Box',      2200.00,    40,    38, 4),
  ('d0000000-0000-4000-8000-000000000702', 'c0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000002', 'Orchestra', 850.00,   500,   498, 4),
  ('d0000000-0000-4000-8000-000000000703', 'c0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000002', 'Balcony',   380.00,   400,   396, 4),
  -- Hamlet (opera)
  ('d0000000-0000-4000-8000-000000000801', 'c0000000-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000002', 'Orchestra', 650.00,   500,   498, 4),
  ('d0000000-0000-4000-8000-000000000802', 'c0000000-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000002', 'Box',      1600.00,    40,    40, 4),
  ('d0000000-0000-4000-8000-000000000803', 'c0000000-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000002', 'Balcony',   280.00,   400,   400, 4),
  -- Sound & Light (pyramids)
  ('d0000000-0000-4000-8000-000000000901', 'c0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000005', 'Front',     450.00,   200,   198, 6),
  ('d0000000-0000-4000-8000-000000000902', 'c0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000005', 'Standard',  220.00,   800,   798, 6),
  ('d0000000-0000-4000-8000-000000000903', 'c0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000005', 'Family',    700.00,   150,   146, 6),
  -- Iftar draft (manara)
  ('d0000000-0000-4000-8000-000000001001', 'c0000000-0000-4000-8000-000000000010', 'b0000000-0000-4000-8000-000000000004', 'Open Seating',150.00, 400, 400, 8),
  ('d0000000-0000-4000-8000-000000001002', 'c0000000-0000-4000-8000-000000000010', 'b0000000-0000-4000-8000-000000000004', 'VIP Table',   800.00,  40,  40, 8),
  -- Cancelled indie (opera)
  ('d0000000-0000-4000-8000-000000001101', 'c0000000-0000-4000-8000-000000000011', 'b0000000-0000-4000-8000-000000000002', 'Regular',   200.00,   300,   300, 4);

-- ---------------------------------------------------------------------------
-- 8. Bookings
--    PENDING reservations expire 2 hours from now so the 60s scheduler
--    does not wipe them the moment the backend starts.
-- ---------------------------------------------------------------------------
INSERT INTO booking (
  id, user_id, event_id, seat_category_id, quantity, status, total_price,
  created_at, confirmed_at, cancelled_at, expires_at
) VALUES
  -- Jazz
  ('e0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000021', 'c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000101',
   2, 'CONFIRMED', 5000.00, now() - interval '4 days',  now() - interval '4 days' + interval '3 minutes', NULL, now() - interval '4 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000023', 'c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000102',
   3, 'CONFIRMED', 1350.00, now() - interval '3 days',  now() - interval '3 days' + interval '3 minutes', NULL, now() - interval '3 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000024', 'c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000102',
   2, 'CANCELLED',  900.00, now() - interval '5 days',  now() - interval '5 days' + interval '3 minutes', now() - interval '1 day', now() - interval '5 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000025', 'c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000103',
   2, 'CONFIRMED',  400.00, now() - interval '2 days',  now() - interval '2 days' + interval '3 minutes', NULL, now() - interval '2 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000022', 'c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000101',
   1, 'PENDING',   2500.00, now() - interval '2 minutes', NULL, NULL, now() + interval '2 hours'),
  ('e0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000026', 'c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000103',
   1, 'EXPIRED',    200.00, now() - interval '1 day', NULL, NULL, now() - interval '1 day' + interval '5 minutes'),

  -- Derby
  ('e0000000-0000-4000-8000-000000000007', 'a0000000-0000-4000-8000-000000000027', 'c0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000302',
   4, 'CONFIRMED', 2600.00, now() - interval '6 days', now() - interval '6 days' + interval '3 minutes', NULL, now() - interval '6 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000008', 'a0000000-0000-4000-8000-000000000028', 'c0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000301',
   2, 'CONFIRMED', 7000.00, now() - interval '6 days', now() - interval '6 days' + interval '3 minutes', NULL, now() - interval '6 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000009', 'a0000000-0000-4000-8000-000000000021', 'c0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000303',
   3, 'CONFIRMED',  660.00, now() - interval '5 days', now() - interval '5 days' + interval '3 minutes', NULL, now() - interval '5 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000010', 'a0000000-0000-4000-8000-000000000022', 'c0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000302',
   2, 'PENDING',   1300.00, now() - interval '1 minute', NULL, NULL, now() + interval '2 hours'),
  ('e0000000-0000-4000-8000-000000000011', 'a0000000-0000-4000-8000-000000000024', 'c0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000303',
   4, 'EXPIRED',    880.00, now() - interval '2 days', NULL, NULL, now() - interval '2 days' + interval '5 minutes'),

  -- Tech summit
  ('e0000000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000023', 'c0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000501',
   1, 'CONFIRMED', 1200.00, now() - interval '8 days', now() - interval '8 days' + interval '3 minutes', NULL, now() - interval '8 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000025', 'c0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000502',
   2, 'CONFIRMED',  700.00, now() - interval '7 days', now() - interval '7 days' + interval '3 minutes', NULL, now() - interval '7 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000014', 'a0000000-0000-4000-8000-000000000026', 'c0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000502',
   1, 'CANCELLED',  350.00, now() - interval '3 days', NULL, now() - interval '3 days' + interval '2 minutes', now() - interval '3 days' + interval '5 minutes'),

  -- Aida
  ('e0000000-0000-4000-8000-000000000015', 'a0000000-0000-4000-8000-000000000021', 'c0000000-0000-4000-8000-000000000007', 'd0000000-0000-4000-8000-000000000702',
   2, 'CONFIRMED', 1700.00, now() - interval '9 days', now() - interval '9 days' + interval '3 minutes', NULL, now() - interval '9 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000016', 'a0000000-0000-4000-8000-000000000027', 'c0000000-0000-4000-8000-000000000007', 'd0000000-0000-4000-8000-000000000701',
   2, 'CONFIRMED', 4400.00, now() - interval '9 days', now() - interval '9 days' + interval '3 minutes', NULL, now() - interval '9 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000017', 'a0000000-0000-4000-8000-000000000022', 'c0000000-0000-4000-8000-000000000007', 'd0000000-0000-4000-8000-000000000703',
   4, 'CONFIRMED', 1520.00, now() - interval '8 days', now() - interval '8 days' + interval '3 minutes', NULL, now() - interval '8 days' + interval '5 minutes'),

  -- Sound & Light
  ('e0000000-0000-4000-8000-000000000018', 'a0000000-0000-4000-8000-000000000024', 'c0000000-0000-4000-8000-000000000009', 'd0000000-0000-4000-8000-000000000903',
   4, 'CONFIRMED', 2800.00, now() - interval '2 days', now() - interval '2 days' + interval '3 minutes', NULL, now() - interval '2 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000019', 'a0000000-0000-4000-8000-000000000028', 'c0000000-0000-4000-8000-000000000009', 'd0000000-0000-4000-8000-000000000901',
   2, 'CONFIRMED',  900.00, now() - interval '2 days', now() - interval '2 days' + interval '3 minutes', NULL, now() - interval '2 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000020', 'a0000000-0000-4000-8000-000000000023', 'c0000000-0000-4000-8000-000000000009', 'd0000000-0000-4000-8000-000000000902',
   2, 'PENDING',    440.00, now() - interval '3 minutes', NULL, NULL, now() + interval '2 hours'),

  -- Nile Beats / FinTech / Hamlet / Super Cup / cancelled indie
  ('e0000000-0000-4000-8000-000000000021', 'a0000000-0000-4000-8000-000000000025', 'c0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000202',
   4, 'CONFIRMED', 1400.00, now() - interval '1 day', now() - interval '1 day' + interval '3 minutes', NULL, now() - interval '1 day' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000022', 'a0000000-0000-4000-8000-000000000027', 'c0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000201',
   2, 'PENDING',   3600.00, now() - interval '4 minutes', NULL, NULL, now() + interval '2 hours'),
  ('e0000000-0000-4000-8000-000000000023', 'a0000000-0000-4000-8000-000000000026', 'c0000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000602',
   1, 'CONFIRMED',  250.00, now() - interval '4 days', now() - interval '4 days' + interval '3 minutes', NULL, now() - interval '4 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000024', 'a0000000-0000-4000-8000-000000000028', 'c0000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000601',
   1, 'CONFIRMED',  900.00, now() - interval '4 days', now() - interval '4 days' + interval '3 minutes', NULL, now() - interval '4 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000025', 'a0000000-0000-4000-8000-000000000022', 'c0000000-0000-4000-8000-000000000008', 'd0000000-0000-4000-8000-000000000801',
   2, 'CONFIRMED', 1300.00, now() - interval '3 days', now() - interval '3 days' + interval '3 minutes', NULL, now() - interval '3 days' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000026', 'a0000000-0000-4000-8000-000000000021', 'c0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000401',
   2, 'CONFIRMED', 1400.00, now() - interval '1 day', now() - interval '1 day' + interval '3 minutes', NULL, now() - interval '1 day' + interval '5 minutes'),
  ('e0000000-0000-4000-8000-000000000027', 'a0000000-0000-4000-8000-000000000024', 'c0000000-0000-4000-8000-000000000011', 'd0000000-0000-4000-8000-000000001101',
   2, 'CANCELLED',  400.00, now() - interval '10 days', now() - interval '10 days' + interval '3 minutes', now() - interval '2 days', now() - interval '10 days' + interval '5 minutes');

-- ---------------------------------------------------------------------------
-- 9. Tickets — one per confirmed (or later-cancelled-confirmed) booking
--    ticket_code is 27 chars from ABCDEFGHJKLMNPQRSTUVWXYZ23456789
-- ---------------------------------------------------------------------------
INSERT INTO tickets (
  uuid, ticket_code, creation_date, booking_id, seat, evnt, venue,
  user_owner_uuid, quantity, total_price, status, checked_in_at
) VALUES
  ('f0000000-0000-4000-8000-000000000001', 'DEMAJAZZLAYLA23456789ABCDEF', now() - interval '4 days',
   'e0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000101',
   'c0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000021', 2, 5000.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000002', 'DEMAJAZZNUR23456789ABCDEFGH', now() - interval '3 days',
   'e0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000102',
   'c0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000023', 3, 1350.00, 'CHECKED_IN', now() - interval '2 hours'),

  ('f0000000-0000-4000-8000-000000000003', 'DEMAJAZZKARM23456789ABCDEFG', now() - interval '5 days',
   'e0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000102',
   'c0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000024', 2, 900.00, 'CANCELLED', NULL),

  ('f0000000-0000-4000-8000-000000000004', 'DEMAJAZZHANA23456789ABCDEFG', now() - interval '2 days',
   'e0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000103',
   'c0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000025', 2, 400.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000005', 'DEMAAHLYDYNA23456789ABCDEFG', now() - interval '6 days',
   'e0000000-0000-4000-8000-000000000007', 'd0000000-0000-4000-8000-000000000302',
   'c0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000001',
   'a0000000-0000-4000-8000-000000000027', 4, 2600.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000006', 'DEMAAHLYYUSF23456789ABCDEFG', now() - interval '6 days',
   'e0000000-0000-4000-8000-000000000008', 'd0000000-0000-4000-8000-000000000301',
   'c0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000001',
   'a0000000-0000-4000-8000-000000000028', 2, 7000.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000007', 'DEMAAHLYLAYL23456789ABCDEFG', now() - interval '5 days',
   'e0000000-0000-4000-8000-000000000009', 'd0000000-0000-4000-8000-000000000303',
   'c0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000001',
   'a0000000-0000-4000-8000-000000000021', 3, 660.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000008', 'DEMATECHNUR23456789ABCDEFGH', now() - interval '8 days',
   'e0000000-0000-4000-8000-000000000012', 'd0000000-0000-4000-8000-000000000501',
   'c0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000004',
   'a0000000-0000-4000-8000-000000000023', 1, 1200.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000009', 'DEMATECHHANA23456789ABCDEFG', now() - interval '7 days',
   'e0000000-0000-4000-8000-000000000013', 'd0000000-0000-4000-8000-000000000502',
   'c0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000004',
   'a0000000-0000-4000-8000-000000000025', 2, 700.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000010', 'DEMAVERDLAYL23456789ABCDEFG', now() - interval '9 days',
   'e0000000-0000-4000-8000-000000000015', 'd0000000-0000-4000-8000-000000000702',
   'c0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000021', 2, 1700.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000011', 'DEMAVERDDYNA23456789ABCDEFG', now() - interval '9 days',
   'e0000000-0000-4000-8000-000000000016', 'd0000000-0000-4000-8000-000000000701',
   'c0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000027', 2, 4400.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000012', 'DEMAVERDUMAR23456789ABCDEFG', now() - interval '8 days',
   'e0000000-0000-4000-8000-000000000017', 'd0000000-0000-4000-8000-000000000703',
   'c0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000022', 4, 1520.00, 'CHECKED_IN', now() - interval '30 minutes'),

  ('f0000000-0000-4000-8000-000000000013', 'DEMASNDLKARM23456789ABCDEFG', now() - interval '2 days',
   'e0000000-0000-4000-8000-000000000018', 'd0000000-0000-4000-8000-000000000903',
   'c0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000005',
   'a0000000-0000-4000-8000-000000000024', 4, 2800.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000014', 'DEMASNDLYUSF23456789ABCDEFG', now() - interval '2 days',
   'e0000000-0000-4000-8000-000000000019', 'd0000000-0000-4000-8000-000000000901',
   'c0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000005',
   'a0000000-0000-4000-8000-000000000028', 2, 900.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000015', 'DEMABEATHANA23456789ABCDEFG', now() - interval '1 day',
   'e0000000-0000-4000-8000-000000000021', 'd0000000-0000-4000-8000-000000000202',
   'c0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000001',
   'a0000000-0000-4000-8000-000000000025', 4, 1400.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000016', 'DEMAFNTCTAMR23456789ABCDEFG', now() - interval '4 days',
   'e0000000-0000-4000-8000-000000000023', 'd0000000-0000-4000-8000-000000000602',
   'c0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000003',
   'a0000000-0000-4000-8000-000000000026', 1, 250.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000017', 'DEMAFNTCYUSF23456789ABCDEFG', now() - interval '4 days',
   'e0000000-0000-4000-8000-000000000024', 'd0000000-0000-4000-8000-000000000601',
   'c0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000003',
   'a0000000-0000-4000-8000-000000000028', 1, 900.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000018', 'DEMAHAMLUMAR23456789ABCDEFG', now() - interval '3 days',
   'e0000000-0000-4000-8000-000000000025', 'd0000000-0000-4000-8000-000000000801',
   'c0000000-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000022', 2, 1300.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000019', 'CUPLAYLALOW23456789ABCDEFGH', now() - interval '1 day',
   'e0000000-0000-4000-8000-000000000026', 'd0000000-0000-4000-8000-000000000401',
   'c0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000001',
   'a0000000-0000-4000-8000-000000000021', 2, 1400.00, 'ISSUED', NULL),

  ('f0000000-0000-4000-8000-000000000020', 'DEMAXSHWKARM23456789ABCDEFG', now() - interval '10 days',
   'e0000000-0000-4000-8000-000000000027', 'd0000000-0000-4000-8000-000000001101',
   'c0000000-0000-4000-8000-000000000011', 'b0000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000024', 2, 400.00, 'CANCELLED', NULL);

-- ---------------------------------------------------------------------------
-- 10. Ticket attendees (owner + extras up to booking.quantity)
-- ---------------------------------------------------------------------------
INSERT INTO ticket_attendee (uuid, ticket_id, customer_id) VALUES
  -- Jazz VIP x2: Layla + Omar
  ('ab000000-0000-4000-8000-000000000001', 'f0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000021'),
  ('ab000000-0000-4000-8000-000000000002', 'f0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000022'),
  -- Jazz Regular x3: Noor + Hana + Dina
  ('ab000000-0000-4000-8000-000000000003', 'f0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000023'),
  ('ab000000-0000-4000-8000-000000000004', 'f0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000025'),
  ('ab000000-0000-4000-8000-000000000005', 'f0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000027'),
  -- Jazz cancelled Regular x2: Karim
  ('ab000000-0000-4000-8000-000000000006', 'f0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000024'),
  -- Jazz Student x2: Hana + Tamer
  ('ab000000-0000-4000-8000-000000000007', 'f0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000025'),
  ('ab000000-0000-4000-8000-000000000008', 'f0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000026'),
  -- Derby Lower x4
  ('ab000000-0000-4000-8000-000000000009', 'f0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000027'),
  ('ab000000-0000-4000-8000-000000000010', 'f0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000021'),
  ('ab000000-0000-4000-8000-000000000011', 'f0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000022'),
  ('ab000000-0000-4000-8000-000000000012', 'f0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000023'),
  -- Derby VIP x2
  ('ab000000-0000-4000-8000-000000000013', 'f0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000028'),
  ('ab000000-0000-4000-8000-000000000014', 'f0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000024'),
  -- Derby Upper x3
  ('ab000000-0000-4000-8000-000000000015', 'f0000000-0000-4000-8000-000000000007', 'a0000000-0000-4000-8000-000000000021'),
  ('ab000000-0000-4000-8000-000000000016', 'f0000000-0000-4000-8000-000000000007', 'a0000000-0000-4000-8000-000000000025'),
  ('ab000000-0000-4000-8000-000000000017', 'f0000000-0000-4000-8000-000000000007', 'a0000000-0000-4000-8000-000000000026'),
  -- Tech Premium x1 / General x2
  ('ab000000-0000-4000-8000-000000000018', 'f0000000-0000-4000-8000-000000000008', 'a0000000-0000-4000-8000-000000000023'),
  ('ab000000-0000-4000-8000-000000000019', 'f0000000-0000-4000-8000-000000000009', 'a0000000-0000-4000-8000-000000000025'),
  ('ab000000-0000-4000-8000-000000000020', 'f0000000-0000-4000-8000-000000000009', 'a0000000-0000-4000-8000-000000000027'),
  -- Aida
  ('ab000000-0000-4000-8000-000000000021', 'f0000000-0000-4000-8000-000000000010', 'a0000000-0000-4000-8000-000000000021'),
  ('ab000000-0000-4000-8000-000000000022', 'f0000000-0000-4000-8000-000000000010', 'a0000000-0000-4000-8000-000000000028'),
  ('ab000000-0000-4000-8000-000000000023', 'f0000000-0000-4000-8000-000000000011', 'a0000000-0000-4000-8000-000000000027'),
  ('ab000000-0000-4000-8000-000000000024', 'f0000000-0000-4000-8000-000000000011', 'a0000000-0000-4000-8000-000000000023'),
  ('ab000000-0000-4000-8000-000000000025', 'f0000000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000022'),
  ('ab000000-0000-4000-8000-000000000026', 'f0000000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000024'),
  ('ab000000-0000-4000-8000-000000000027', 'f0000000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000025'),
  ('ab000000-0000-4000-8000-000000000028', 'f0000000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000026'),
  -- Sound & Light
  ('ab000000-0000-4000-8000-000000000029', 'f0000000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000024'),
  ('ab000000-0000-4000-8000-000000000030', 'f0000000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000027'),
  ('ab000000-0000-4000-8000-000000000031', 'f0000000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000021'),
  ('ab000000-0000-4000-8000-000000000032', 'f0000000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000022'),
  ('ab000000-0000-4000-8000-000000000033', 'f0000000-0000-4000-8000-000000000014', 'a0000000-0000-4000-8000-000000000028'),
  ('ab000000-0000-4000-8000-000000000034', 'f0000000-0000-4000-8000-000000000014', 'a0000000-0000-4000-8000-000000000023'),
  -- Nile Beats x4
  ('ab000000-0000-4000-8000-000000000035', 'f0000000-0000-4000-8000-000000000015', 'a0000000-0000-4000-8000-000000000025'),
  ('ab000000-0000-4000-8000-000000000036', 'f0000000-0000-4000-8000-000000000015', 'a0000000-0000-4000-8000-000000000026'),
  ('ab000000-0000-4000-8000-000000000037', 'f0000000-0000-4000-8000-000000000015', 'a0000000-0000-4000-8000-000000000027'),
  ('ab000000-0000-4000-8000-000000000038', 'f0000000-0000-4000-8000-000000000015', 'a0000000-0000-4000-8000-000000000028'),
  -- FinTech / Hamlet / Super Cup / indie
  ('ab000000-0000-4000-8000-000000000039', 'f0000000-0000-4000-8000-000000000016', 'a0000000-0000-4000-8000-000000000026'),
  ('ab000000-0000-4000-8000-000000000040', 'f0000000-0000-4000-8000-000000000017', 'a0000000-0000-4000-8000-000000000028'),
  ('ab000000-0000-4000-8000-000000000041', 'f0000000-0000-4000-8000-000000000018', 'a0000000-0000-4000-8000-000000000022'),
  ('ab000000-0000-4000-8000-000000000042', 'f0000000-0000-4000-8000-000000000018', 'a0000000-0000-4000-8000-000000000021'),
  ('ab000000-0000-4000-8000-000000000043', 'f0000000-0000-4000-8000-000000000019', 'a0000000-0000-4000-8000-000000000021'),
  ('ab000000-0000-4000-8000-000000000044', 'f0000000-0000-4000-8000-000000000019', 'a0000000-0000-4000-8000-000000000024'),
  ('ab000000-0000-4000-8000-000000000045', 'f0000000-0000-4000-8000-000000000020', 'a0000000-0000-4000-8000-000000000024');

COMMIT;

-- Summary
SELECT 'users' AS entity, count(*) FROM users
UNION ALL SELECT 'organizer_applications', count(*) FROM organizer_applications
UNION ALL SELECT 'venues', count(*) FROM venues
UNION ALL SELECT 'events', count(*) FROM events
UNION ALL SELECT 'seat_categories', count(*) FROM seat_categories
UNION ALL SELECT 'booking', count(*) FROM booking
UNION ALL SELECT 'tickets', count(*) FROM tickets
UNION ALL SELECT 'ticket_attendee', count(*) FROM ticket_attendee
ORDER BY 1;
