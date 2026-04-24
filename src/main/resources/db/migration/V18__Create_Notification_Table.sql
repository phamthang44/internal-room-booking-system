-- ==========================================
-- CREATE NOTIFICATIONS TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS public.notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    related_id VARCHAR(255),
    
    -- Auditing fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES public.users(id)
);

-- Index for fast lookup of a user's notifications sorted by time
CREATE INDEX IF NOT EXISTS idx_notification_user_created ON public.notifications (user_id, created_at DESC);

-- Index for unread counts
CREATE INDEX IF NOT EXISTS idx_notification_unread ON public.notifications (user_id) WHERE is_read = FALSE;
