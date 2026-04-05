CREATE TABLE notifications (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       user_id UUID NOT NULL,
       type VARCHAR(50) NOT NULL,
       title VARCHAR(200) NOT NULL,
       message TEXT,
       is_read BOOLEAN NOT NULL DEFAULT false,
       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);