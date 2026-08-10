-- Disable only the legacy demo credentials when their original, publicly
-- known password hashes are still present. This safely preserves any tracks
-- and comments that reference those users; an administrator can later reset
-- the accounts to new, unique passwords through a controlled process.
UPDATE app_user
SET password_hash = 'DISABLED'
WHERE (username = 'admin' AND password_hash = '$2b$10$ZBqfIqFdG669xLHi4yE9/uyMgLtfV3WTBVryt5.vNQ01f8Nzwzd5q')
   OR (username = 'user' AND password_hash = '$2b$10$hnhb5dcNsm6IQXcvlt0YAOQ2GlIugprRuzg66lxZkwCqiBcrBOsDW');
