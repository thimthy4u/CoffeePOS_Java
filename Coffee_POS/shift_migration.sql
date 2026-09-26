-- This script modifies the 'staff_shifts' table for the new shift management system.
-- Execute this in your 'java_pos' MySQL database.

-- 1. Drop the old timestamp columns
ALTER TABLE staff_shifts
DROP COLUMN shift_start,
DROP COLUMN shift_end;

-- 2. Add the new columns for shift type and date
ALTER TABLE staff_shifts
ADD COLUMN shift_type VARCHAR(50) NOT NULL,
ADD COLUMN shift_date DATE NULL;
