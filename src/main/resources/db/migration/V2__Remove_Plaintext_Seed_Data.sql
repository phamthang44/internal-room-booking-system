-- Remove the dummy booking to remove foreign key dependencies
DELETE FROM bookings;
-- Remove the plain text users
DELETE FROM users;
